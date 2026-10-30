/**
 * Shared helpers for face-api.js based enrollment, recognition, and liveness verification.
 * Models are loaded once from a CDN mirror of the face-api.js weights.
 */
const MODEL_URL = "https://cdn.jsdelivr.net/gh/justadudewhohacks/face-api.js@master/weights";

// Below this distance, two faces are considered a possible match (lower = stricter).
const MATCH_THRESHOLD = 0.5;
// The best match must beat the second-best candidate by at least this much,
// otherwise the result is too ambiguous to trust (e.g. two similar-looking students).
const MIN_MARGIN = 0.07;
// Minimum face-detector confidence to accept a frame at all (filters blurry / partial faces).
const MIN_DETECTION_SCORE = 0.6;

// Eye Aspect Ratio threshold below which an eye is considered closed (blink)
const BLINK_EAR_THRESHOLD = 0.22;

const ENROLL_DETECTOR_OPTS = new (window.faceapi ? faceapi.TinyFaceDetectorOptions : Object)({ inputSize: 416, scoreThreshold: 0.5 });
const SCAN_DETECTOR_OPTS = new (window.faceapi ? faceapi.TinyFaceDetectorOptions : Object)({ inputSize: 320, scoreThreshold: 0.5 });

async function loadFaceModels(statusEl) {
  if (statusEl) statusEl.textContent = "Loading face recognition & landmark models...";
  await faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_URL);
  await faceapi.nets.faceLandmark68Net.loadFromUri(MODEL_URL);
  await faceapi.nets.faceRecognitionNet.loadFromUri(MODEL_URL);
  if (statusEl) statusEl.textContent = "Models loaded. Starting camera...";
}

async function startCamera(videoEl) {
  const stream = await navigator.mediaDevices.getUserMedia({
    video: { width: { ideal: 1280 }, height: { ideal: 720 } },
    audio: false
  });
  videoEl.srcObject = stream;
  return new Promise((resolve) => {
    videoEl.onloadedmetadata = () => resolve(videoEl);
  });
}

function stopCamera(videoEl) {
  const stream = videoEl.srcObject;
  if (stream) {
    stream.getTracks().forEach((t) => t.stop());
  }
}

/**
 * Returns CSRF headers if meta tags exist on page.
 */
function getCSRFHeaders() {
  const tokenMeta = document.querySelector('meta[name="_csrf"]');
  const headerMeta = document.querySelector('meta[name="_csrf_header"]');
  const headers = { "Content-Type": "application/json" };
  if (tokenMeta && headerMeta) {
    headers[headerMeta.content] = tokenMeta.content;
  }
  return headers;
}

// Computes Eye Aspect Ratio (EAR) for liveness blink checking
function computeEAR(landmarks) {
  const pts = landmarks.positions;
  // Left eye: 36..41, Right eye: 42..47
  function eyeAspectRatio(p1, p2, p3, p4, p5, p6) {
    const v1 = Math.hypot(p2.x - p6.x, p2.y - p6.y);
    const v2 = Math.hypot(p3.x - p5.x, p3.y - p5.y);
    const h = Math.hypot(p1.x - p4.x, p1.y - p4.y);
    return (v1 + v2) / (2.0 * h);
  }

  const leftEAR = eyeAspectRatio(pts[36], pts[37], pts[38], pts[39], pts[40], pts[41]);
  const rightEAR = eyeAspectRatio(pts[42], pts[43], pts[44], pts[45], pts[46], pts[47]);
  return (leftEAR + rightEAR) / 2.0;
}

// Detects the single most prominent face in the video and returns its detection + descriptor + landmarks
async function detectSingleFace(videoEl) {
  const detection = await faceapi
    .detectSingleFace(videoEl, ENROLL_DETECTOR_OPTS)
    .withFaceLandmarks()
    .withFaceDescriptor();
  if (!detection) return null;
  if (detection.detection.score < MIN_DETECTION_SCORE) return null;
  return detection;
}

// Detects all faces currently visible in the video frame with descriptors
async function detectAllFaces(videoEl) {
  const detections = await faceapi
    .detectAllFaces(videoEl, SCAN_DETECTOR_OPTS)
    .withFaceLandmarks()
    .withFaceDescriptors();
  return detections.filter((d) => d.detection.score >= MIN_DETECTION_SCORE);
}

function euclideanDistance(a, b) {
  let sum = 0;
  for (let i = 0; i < a.length; i++) {
    sum += (a[i] - b[i]) * (a[i] - b[i]);
  }
  return Math.sqrt(sum);
}

// Averages several 128-d descriptors captured from different angles/frames
function averageDescriptors(descriptorArrays) {
  const len = descriptorArrays[0].length;
  const avg = new Array(len).fill(0);
  for (const d of descriptorArrays) {
    for (let i = 0; i < len; i++) avg[i] += d[i];
  }
  for (let i = 0; i < len; i++) avg[i] /= descriptorArrays.length;
  return avg;
}

// Matches a live descriptor against known enrolled students
function matchDescriptor(liveDescriptor, knownStudents) {
  let best = null;
  let bestDistance = Infinity;
  let secondBestDistance = Infinity;

  for (const known of knownStudents) {
    const knownArr = JSON.parse(known.descriptor);
    const dist = euclideanDistance(liveDescriptor, knownArr);
    if (dist < bestDistance) {
      secondBestDistance = bestDistance;
      bestDistance = dist;
      best = known;
    } else if (dist < secondBestDistance) {
      secondBestDistance = dist;
    }
  }

  if (!best || bestDistance >= MATCH_THRESHOLD) return null;

  // Margin check for ambiguity avoidance
  const hasMargin = knownStudents.length < 2 || (secondBestDistance - bestDistance) >= MIN_MARGIN;
  if (!hasMargin) return null;

  return { student: best, distance: bestDistance };
}

// Captures the current video frame as a base64 JPEG
function captureSnapshot(videoEl, maxWidth = 200) {
  const scale = maxWidth / videoEl.videoWidth;
  const canvas = document.createElement("canvas");
  canvas.width = maxWidth;
  canvas.height = videoEl.videoHeight * scale;
  canvas.getContext("2d").drawImage(videoEl, 0, 0, canvas.width, canvas.height);
  return canvas.toDataURL("image/jpeg", 0.8);
}
