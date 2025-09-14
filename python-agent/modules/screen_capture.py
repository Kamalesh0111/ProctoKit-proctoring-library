import cv2
import base64

class CameraFrameCapture:
    """
    A stateless class that captures a single frame from the camera
    and encodes it to a Base64 string. This allows the image data
    to be sent as text within a JSON object.
    """
    
    def get_base64_frame(self, image):
        """
        Encodes a single camera frame into a Base64 string.

        Args:
            image: A single camera frame from OpenCV.

        Returns:
            A Base64 encoded string representation of the JPEG image,
            or None if encoding fails.
        """
        if image is None:
            return None
        
        # Encode the image as a JPEG in memory for efficient transmission.
        # The quality can be adjusted (0-100) to balance file size and clarity.
        success, buffer = cv2.imencode('.jpg', image, [cv2.IMWRITE_JPEG_QUALITY, 90])
        
        if not success:
            return None
            
        # Convert the in-memory buffer to a Base64 string.
        # The 'utf-8' encoding is standard for this purpose.
        base64_frame = base64.b64encode(buffer).decode('utf-8')
        return base64_frame