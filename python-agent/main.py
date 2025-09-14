import cv2
import sys
import json
import time
from modules.face_detector import FaceDetector
from modules.screen_capture import CameraFrameCapture

def create_event(activity, status, details):
    """
    Helper function to create and send a standardized JSON event to the parent Java process.
    Includes robust error handling for broken communication pipes.
    """
    try:
        event = {
            "timestamp": int(time.time() * 1000),
            "activity": activity,
            "status": status,
            "details": details
        }
        print(json.dumps(event))
        sys.stdout.flush()
    except (OSError, BrokenPipeError):
        # THE FIX: If the pipe is broken, exit the script cleanly.
        sys.exit(0)

def debug_print(message):
    """
    Helper function to print debug messages to stderr.
    Includes robust error handling for broken communication pipes.
    """
    try:
        print(f"[PYTHON-DEBUG] {message}", file=sys.stderr)
        sys.stderr.flush()
    except (OSError, BrokenPipeError):
        # THE FIX: If the pipe is broken, exit the script cleanly.
        sys.exit(0)

def main():
    """
    Main function for the proctoring vision agent. This version includes enhanced
    resilience and internal debugging to ensure stable operation.
    """
    cap = None  # Initialize cap to None for the finally block
    try:
        # Initialize the camera.
        cap = cv2.VideoCapture(0)
        if not cap.isOpened():
            create_event("agent_status", "error", {"message": "Could not open webcam. Check camera permissions and connections."})
            return

        # Initialize monitoring modules.
        face_detector = FaceDetector()
        frame_capture = CameraFrameCapture()
        
        # State tracking variables
        last_confirmed_face_count = -1
        pending_face_count = -1
        change_confirm_frames = 0
        last_frame_capture_time = 0
        
        # Configuration
        FRAME_CAPTURE_INTERVAL = 60
        CONFIRMATION_THRESHOLD = 3

        create_event("agent_status", "ok", {"message": "Python agent started successfully."})
        debug_print("Main monitoring loop started.")

        while True:
            # Robustness Check: Ensure camera is still connected on each loop.
            if not cap.isOpened():
                create_event("agent_status", "error", {"message": "Webcam connection was lost."})
                break

            success, frame = cap.read()
            if not success:
                time.sleep(0.5)
                continue

            # --- Face Detection with error handling ---
            try:
                current_face_count = face_detector.detect_faces(frame)
            except Exception as e:
                debug_print(f"Error during face detection: {e}")
                time.sleep(1) # Pause briefly if detection is failing
                continue

            # --- Smart "Debounce" Logic to prevent false alerts ---
            if current_face_count != last_confirmed_face_count:
                if current_face_count != pending_face_count:
                    pending_face_count = current_face_count
                    change_confirm_frames = 1
                else:
                    change_confirm_frames += 1
            else:
                pending_face_count = -1
                change_confirm_frames = 0
            
            # If the change has been consistent for long enough, send an official event.
            if change_confirm_frames >= CONFIRMATION_THRESHOLD:
                debug_print(f"State change confirmed: {last_confirmed_face_count} -> {current_face_count} faces. Sending event.")
                details = {"faceCount": current_face_count}
                if current_face_count > 1:
                    status = "violation"
                    details["message"] = "Multiple faces detected."
                elif current_face_count == 0:
                    status = "suspicious"
                    details["message"] = "No face detected."
                else:
                    status = "ok"
                    details["message"] = "Single face detected."
                
                create_event("faceDetection", status, details)
                
                last_confirmed_face_count = current_face_count
                pending_face_count = -1
                change_confirm_frames = 0

            # --- Periodic Camera Frame Capture ---
            current_time = time.time()
            if current_time - last_frame_capture_time > FRAME_CAPTURE_INTERVAL:
                debug_print("Capturing periodic camera frame.")
                base64_frame = frame_capture.get_base64_frame(frame)
                if base64_frame:
                    create_event("frameCapture", "info", {"format": "jpeg", "data": base64_frame})
                    last_frame_capture_time = current_time

            time.sleep(0.5) # Loop roughly twice per second.

    except Exception as e:
        # Catch any unexpected top-level errors and report them.
        create_event("agent_status", "error", {"message": f"A critical error occurred in the Python agent: {str(e)}"})
        debug_print(f"CRITICAL ERROR: {e}")
    finally:
        # Ensure resources are always cleaned up.
        debug_print("Shutdown sequence initiated.")
        if cap and cap.isOpened():
            cap.release()
        # Send one final shutdown message.
        create_event("agent_status", "shutdown", {"message": "Python agent is shutting down."})

if __name__ == "__main__":
    main()
