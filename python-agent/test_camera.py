import cv2
from modules.face_detector import FaceDetector

print("--- Starting Camera and Face Detection Test ---")
print("A window should appear showing your webcam feed.")
print("Press 'q' to quit.")

try:
    # Initialize the face detector. This will also test if the model file is found.
    detector = FaceDetector()
    print("Face detector loaded successfully.")

    # Initialize the camera.
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("ERROR: Could not open webcam. Please check your camera.")
        exit()

    while True:
        # Read a frame from the camera.
        success, frame = cap.read()
        if not success:
            print("Failed to grab frame.")
            break

        # Get the rectangles for any detected faces.
        face_rects = detector.get_face_rectangles(frame)

        # Draw a green rectangle around each detected face.
        for (x, y, w, h) in face_rects:
            cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)
        
        # Display the resulting frame in a window.
        cv2.imshow('Face Detection Test', frame)

        # Wait for the 'q' key to be pressed to exit.
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

except Exception as e:
    print(f"An error occurred: {e}")

finally:
    # Clean up when done.
    print("Shutting down...")
    if 'cap' in locals() and cap.isOpened():
        cap.release()
    cv2.destroyAllWindows()
