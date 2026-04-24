import face_recognition
import cv2
import json
import sys

def capture_and_encode():
    video_capture = cv2.VideoCapture(0, cv2.CAP_DSHOW)

    if not video_capture.isOpened():
        print(json.dumps({"error": "Cannot open webcam"}))
        sys.exit(1)

    window_name = 'Enregistrement Visage - ESPACE=Capturer  Q=Quitter'
    cv2.namedWindow(window_name, cv2.WINDOW_NORMAL)
    cv2.resizeWindow(window_name, 640, 480)

    while True:
        ret, frame = video_capture.read()
        if not ret:
            continue

        cv2.imshow(window_name, frame)
        cv2.setWindowProperty(window_name, cv2.WND_PROP_TOPMOST, 1)
        key = cv2.waitKey(1) & 0xFF

        if key == ord('q'):
            video_capture.release()
            cv2.destroyAllWindows()
            print(json.dumps({"error": "Cancelled"}))
            sys.exit(0)

        if key == ord(' '):
            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            face_locations = face_recognition.face_locations(rgb_frame)
            if len(face_locations) == 0:
                continue
            encodings = face_recognition.face_encodings(rgb_frame, face_locations)
            if encodings:
                descriptor = encodings[0].tolist()
                video_capture.release()
                cv2.destroyAllWindows()
                print(json.dumps({"descriptor": descriptor}))
                sys.exit(0)

if __name__ == "__main__":
    capture_and_encode()