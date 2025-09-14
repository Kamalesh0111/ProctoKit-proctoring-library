import cv2
import os
import sys

def resource_path(relative_path):
    """
    Get the absolute path to a resource, works for dev and for PyInstaller.
    """
    try:
        base_path = sys._MEIPASS
    except Exception:
        base_path = os.path.abspath(".")
    return os.path.join(base_path, relative_path)

class FaceDetector:
    """
    A stateless class that uses a Haar Cascade model from OpenCV to detect faces.
    """
    def __init__(self, cascade_file_name="haarcascade_frontalface_default.xml"):
        model_path = resource_path(cascade_file_name)
        if not os.path.exists(model_path):
            raise FileNotFoundError(f"Haar Cascade model not found at the resolved path: {model_path}")
        self.face_cascade = cv2.CascadeClassifier(model_path)

    def detect_faces(self, image):
        """
        Detects faces in the provided image frame.
        """
        if image is None:
            return 0
            
        gray_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        
        # --- TUNED PARAMETERS FOR BETTER STABILITY ---
        faces = self.face_cascade.detectMultiScale(
            gray_image,
            # scaleFactor: How much the image size is reduced at each image scale.
            # A value closer to 1.0 (e.g., 1.05) is more thorough but slower. 1.1 is a good balance.
            scaleFactor=1.1, 

            # minNeighbors: How many neighbors each candidate rectangle should have to retain it.
            # A lower value (e.g., 4) makes the detection less strict and better at
            # holding onto faces that are slightly turned.
            minNeighbors=4,

            # minSize: The minimum possible object size. Objects smaller than this are ignored.
            # Increasing this (e.g., to 60x60) helps eliminate small, false detections.
            minSize=(60, 60)
        )
        
        return len(faces)