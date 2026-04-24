import face_recognition
import cv2
import json
import sys
import os
import subprocess
import numpy as np

# Silence pygame's stdout spam BEFORE importing it
os.environ['PYGAME_HIDE_SUPPORT_PROMPT'] = '1'
try:
    import pygame
    pygame.mixer.init()
    PYGAME_OK = True
except Exception:
    PYGAME_OK = False

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
VIDEO_PATH = os.path.join(SCRIPT_DIR, "look_away_video.mp4")
AUDIO_PATH = os.path.join(SCRIPT_DIR, "look_away_audio.mp3")

# Extract audio from mp4 once on first run (pygame cant read mp4 audio)
if os.path.exists(VIDEO_PATH) and not os.path.exists(AUDIO_PATH):
    try:
        import subprocess as _sp
        _sp.run(["ffmpeg", "-y", "-i", VIDEO_PATH, "-q:a", "0", "-map", "a", AUDIO_PATH],
                stdout=_sp.DEVNULL, stderr=_sp.DEVNULL)
    except Exception:
        pass

FONT      = cv2.FONT_HERSHEY_SIMPLEX
FONT_BOLD = cv2.FONT_HERSHEY_DUPLEX

# BGR palette
C_WHITE  = (255, 255, 255)
C_DIM    = (130, 135, 145)
C_DARK   = ( 10,  12,  20)
C_GREEN  = ( 80, 210,  90)
C_ORANGE = ( 30, 150, 255)
C_RED    = ( 50,  50, 210)

_audio_on = False

def audio_play():
    global _audio_on
    if not PYGAME_OK or _audio_on or not os.path.exists(AUDIO_PATH):
        return
    try:
        pygame.mixer.music.load(AUDIO_PATH)
        pygame.mixer.music.play(-1)
        _audio_on = True
    except Exception:
        pass

def audio_stop():
    global _audio_on
    if not PYGAME_OK or not _audio_on:
        return
    try:
        pygame.mixer.music.stop()
        _audio_on = False
    except Exception:
        pass


def alpha_rect(img, x1, y1, x2, y2, color, a):
    x1,y1 = max(0,x1), max(0,y1)
    x2,y2 = min(img.shape[1],x2), min(img.shape[0],y2)
    roi = img[y1:y2, x1:x2]
    bg  = np.full_like(roi, color)
    img[y1:y2, x1:x2] = cv2.addWeighted(bg, a, roi, 1-a, 0)


def top_bar(frame):
    h, w = frame.shape[:2]
    alpha_rect(frame, 0, 0, w, 52, C_DARK, 0.88)
    cv2.line(frame, (0,52), (w,52), (40,44,58), 1)
    cv2.putText(frame, "FLUENTLY", (16,33), FONT_BOLD, 0.70, C_WHITE, 1, cv2.LINE_AA)
    cv2.circle(frame, (108,27), 3, C_ORANGE, -1, cv2.LINE_AA)
    cv2.putText(frame, "Face Authentication", (118,33), FONT, 0.45, C_DIM, 1, cv2.LINE_AA)
    cv2.putText(frame, "SPACE  scan     Q  quit", (w-230,33), FONT, 0.40, C_DIM, 1, cv2.LINE_AA)


def status_bar(frame, text, subtext, dot_color):
    h, w = frame.shape[:2]
    alpha_rect(frame, 0, h-58, w, h, C_DARK, 0.88)
    cv2.line(frame, (0,h-58), (w,h-58), (40,44,58), 1)
    cv2.circle(frame, (20, h-34), 5, dot_color, -1, cv2.LINE_AA)
    cv2.putText(frame, text,    (34, h-29), FONT_BOLD, 0.55, dot_color, 1, cv2.LINE_AA)
    cv2.putText(frame, subtext, (34, h-13), FONT,      0.38, C_DIM,     1, cv2.LINE_AA)


def face_brackets(frame, face_locations_small, color):
    """Minimal corner brackets around detected face, no oval, no circle."""
    L, t = 18, 2
    for (top, right, bottom, left) in face_locations_small:
        top, right, bottom, left = top*2, right*2, bottom*2, left*2
        pad = 14
        x1, y1, x2, y2 = left-pad, top-pad, right+pad, bottom+pad
        for (cx, cy, dx, dy) in [(x1,y1,1,1),(x2,y1,-1,1),(x1,y2,1,-1),(x2,y2,-1,-1)]:
            cv2.line(frame, (cx,cy), (cx+dx*L,cy), color, t, cv2.LINE_AA)
            cv2.line(frame, (cx,cy), (cx,cy+dy*L), color, t, cv2.LINE_AA)


def pip_video(frame, vid_cap, fc):
    """Picture-in-picture video overlay, top-right."""
    ret, vid = vid_cap.read()
    if not ret:
        vid_cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
        ret, vid = vid_cap.read()
    if not ret:
        return frame
    h, w = frame.shape[:2]
    pw = int(w * 0.40)
    vh, vw = vid.shape[:2]
    ph = int(pw * vh / vw)
    max_h = h - 58 - 52 - 12
    if ph > max_h:
        ph = max_h
        pw = int(ph * vw / vh)
    vid_r = cv2.resize(vid, (pw, ph))
    m = 12
    x, y = w - pw - m, 52 + m
    # Shadow
    alpha_rect(frame, x-4, y-4, x+pw+4, y+ph+4, (0,0,0), 0.50)
    # Video frame
    frame[y:y+ph, x:x+pw] = vid_r
    # Thin border, no pulse
    cv2.rectangle(frame, (x,y), (x+pw,y+ph), C_ORANGE, 1, cv2.LINE_AA)
    # Tiny label
    alpha_rect(frame, x, y, x+60, y+18, C_DARK, 0.75)
    cv2.putText(frame, "PLAYING", (x+5, y+13), FONT, 0.35, C_ORANGE, 1, cv2.LINE_AA)
    return frame


def get_eye_direction(landmarks, fw):
    try:
        le = landmarks.get('left_eye', [])
        re = landmarks.get('right_eye', [])
        if not le or not re:
            return True
        mid = (sum(p[0] for p in le)//len(le) + sum(p[0] for p in re)//len(re)) / 2
        return fw*0.20 < mid < fw*0.80
    except Exception:
        return True


def capture_and_encode():
    cap = cv2.VideoCapture(0, cv2.CAP_DSHOW)
    if not cap.isOpened():
        print(json.dumps({"error": "Cannot open webcam"}), file=sys.stdout, flush=True)
        sys.exit(1)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH,  640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

    vid_cap = None
    if os.path.exists(VIDEO_PATH):
        vid_cap = cv2.VideoCapture(VIDEO_PATH)
        if not vid_cap.isOpened():
            vid_cap = None

    WIN = "Fluently - Face Login"
    cv2.namedWindow(WIN, cv2.WINDOW_NORMAL)
    cv2.resizeWindow(WIN, 640, 480)
    cv2.setWindowProperty(WIN, cv2.WND_PROP_TOPMOST, 1)

    fc = 0
    prev_state = None   # "none" | "looking" | "away"

    while True:
        ret, frame = cap.read()
        if not ret:
            continue
        fc += 1
        display = frame.copy()

        small = cv2.resize(frame, (0,0), fx=0.5, fy=0.5)
        rgb   = cv2.cvtColor(small, cv2.COLOR_BGR2RGB)
        locs  = face_recognition.face_locations(rgb, model="hog")
        found = len(locs) > 0
        looking = True

        if found:
            lmarks = face_recognition.face_landmarks(rgb, locs)
            if lmarks:
                looking = get_eye_direction(lmarks[0], small.shape[1])

        # Determine state
        if not found:
            state = "none"
        elif looking:
            state = "looking"
        else:
            state = "away"

        # ── Audio control ─────────────────────────────────────────────────────
        # Play audio when no face OR looking away
        if state in ("none", "away"):
            audio_play()
        else:
            audio_stop()

        # Reset video position when transitioning back to looking
        if state == "looking" and prev_state != "looking":
            if vid_cap:
                vid_cap.set(cv2.CAP_PROP_POS_FRAMES, 0)

        prev_state = state

        # ── Render ────────────────────────────────────────────────────────────
        if state == "none":
            # Play video PiP, show "no face" status
            if vid_cap:
                display = pip_video(display, vid_cap, fc)
            top_bar(display)
            status_bar(display,
                       "No face detected",
                       "Position your face in front of the camera",
                       C_DIM)

        elif state == "looking":
            # Clean: just brackets around face, green status
            face_brackets(display, locs, C_GREEN)
            top_bar(display)
            status_bar(display,
                       "Face detected  —  press SPACE to authenticate",
                       "Hold still and look directly at the camera",
                       C_GREEN)

        else:  # away
            # Play video PiP, orange status
            if vid_cap:
                display = pip_video(display, vid_cap, fc)
            face_brackets(display, locs, C_ORANGE)
            top_bar(display)
            status_bar(display,
                       "Look at the camera to continue",
                       "Authentication paused",
                       C_ORANGE)

        cv2.imshow(WIN, display)
        cv2.setWindowProperty(WIN, cv2.WND_PROP_TOPMOST, 1)

        key = cv2.waitKey(1) & 0xFF

        if key == ord('q'):
            audio_stop()
            cap.release()
            if vid_cap: vid_cap.release()
            cv2.destroyAllWindows()
            print(json.dumps({"error": "Cancelled"}), flush=True)
            sys.exit(0)

        if key == ord(' '):
            rgb_full = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            fl = face_recognition.face_locations(rgb_full)
            if not fl:
                alpha_rect(display, 0, 0, display.shape[1], display.shape[0], C_RED, 0.25)
                cv2.imshow(WIN, display)
                cv2.waitKey(300)
                continue
            encs = face_recognition.face_encodings(rgb_full, fl)
            if encs:
                audio_stop()
                cap.release()
                if vid_cap: vid_cap.release()
                cv2.destroyAllWindows()
                # IMPORTANT: only print the JSON, nothing else
                sys.stdout.write(json.dumps({"descriptor": encs[0].tolist()}) + "\n")
                sys.stdout.flush()
                sys.exit(0)

if __name__ == "__main__":
    capture_and_encode()