// 전역 변수
let selectedFiles = [];
let toastQueue = [];
let isShowingToast = false;
const MAX_FILES = 20; // 최대 20장으로 제한
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
const ALLOWED_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/heic', 'image/heif'];

// DOM 요소
const fileInput = document.getElementById('fileInput');
const selectBtn = document.getElementById('selectBtn');
const userNameInput = document.getElementById('userName');
const previewArea = document.getElementById('previewArea');
const thumbnailGrid = document.getElementById('thumbnailGrid');
const photoCount = document.getElementById('photoCount');
const uploadBtn = document.getElementById('uploadBtn');
const progressSection = document.getElementById('progressSection');
const progressFill = document.getElementById('progressFill');
const progressText = document.getElementById('progressText');
const successModal = document.getElementById('successModal');
const closeModalBtn = document.getElementById('closeModalBtn');

// ========================================
// 이벤트 리스너 등록
// ========================================
document.addEventListener('DOMContentLoaded', () => {
  console.log('🎉 WedSnap upload.js loaded successfully');
  console.log('📍 API endpoint:', '/api/events/devEvent/upload');
  console.log('⚙️ Configuration:', {
    maxFiles: MAX_FILES,
    maxFileSize: MAX_FILE_SIZE,
    allowedTypes: ALLOWED_TYPES
  });

  selectBtn.addEventListener('click', () => fileInput.click());
  fileInput.addEventListener('change', handleFileSelect);
  userNameInput.addEventListener('input', updateUploadButtonState);
  uploadBtn.addEventListener('click', handleUpload);
  closeModalBtn.addEventListener('click', closeModal);

  console.log('✅ Event listeners registered');
});

// ========================================
// 유틸리티 함수
// ========================================

/**
 * 파일 크기 검증
 */
function validateFileSize(file) {
  return file.size <= MAX_FILE_SIZE;
}

/**
 * 바이트를 읽기 쉬운 형식으로 변환
 */
function formatFileSize(bytes) {
  if (bytes === 0) {
    return '0 Bytes';
  }
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

/**
 * Toast 메시지 표시
 */
function showToast(message, duration = 3000) {
  toastQueue.push({message, duration});
  if (!isShowingToast) {
    processToastQueue();
  }
}

/**
 * Toast 큐 처리 (순차적으로 표시)
 */
function processToastQueue() {
  if (toastQueue.length === 0) {
    isShowingToast = false;
    return;
  }

  isShowingToast = true;
  const {message, duration} = toastQueue.shift();

  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.textContent = message;
  document.body.appendChild(toast);

  // 약간의 딜레이 후 표시 (애니메이션을 위해)
  setTimeout(() => toast.classList.add('show'), 10);

  // duration 후 숨기기
  setTimeout(() => {
    toast.classList.remove('show');
    // 애니메이션 종료 후 제거하고 다음 토스트 처리
    setTimeout(() => {
      toast.remove();
      processToastQueue();
    }, 300);
  }, duration);
}

/**
 * HEIC 파일 감지
 */
function isHEICFile(file) {
  return file.type === 'image/heic' ||
         file.type === 'image/heif' ||
         file.name.toLowerCase().endsWith('.heic') ||
         file.name.toLowerCase().endsWith('.heif');
}

/**
 * 파일 선택 처리
 */
function handleFileSelect(event) {
  const files = Array.from(event.target.files);

  // 파일이 선택되지 않은 경우
  if (files.length === 0) {
    return;
  }

  // 파일 수 제한 확인
  const totalFiles = selectedFiles.length + files.length;
  if (totalFiles > MAX_FILES) {
    showToast(`최대 ${MAX_FILES}장 제한 (추가 가능: ${MAX_FILES - selectedFiles.length}장)`);
    return;
  }

  // 파일 타입 및 크기 검증
  const validFiles = [];
  const invalidTypeFiles = [];
  const oversizedFiles = [];
  const heicFiles = [];

  files.forEach(file => {
    // 타입 검증
    if (!ALLOWED_TYPES.includes(file.type.toLowerCase())) {
      invalidTypeFiles.push(file.name);
      return;
    }

    // 크기 검증
    if (!validateFileSize(file)) {
      oversizedFiles.push({
        name: file.name,
        size: formatFileSize(file.size)
      });
      return;
    }

    // HEIC 파일 감지
    if (isHEICFile(file)) {
      heicFiles.push(file.name);
    }

    validFiles.push(file);
  });

  // 사용자 피드백
  if (invalidTypeFiles.length > 0) {
    showToast(`지원하지 않는 형식:\n${invalidTypeFiles.join('\n')}`);
  }

  if (oversizedFiles.length > 0) {
    const message = oversizedFiles.map(f => `${f.name} (${f.size})`).join('\n');
    showToast(`${formatFileSize(MAX_FILE_SIZE)} 초과:\n${message}`);
  }

  if (heicFiles.length > 0) {
    showToast(`HEIC ${heicFiles.length}개 포함 (미리보기 제한, 업로드 가능)`);
  }

  if (validFiles.length > 0) {
    selectedFiles = [...selectedFiles, ...validFiles];
    updatePreviewArea();
    updateUploadButtonState();

    // 성공 피드백: 추가로 선택 가능한 개수 표시
    const remainingSlots = MAX_FILES - selectedFiles.length;
    if (remainingSlots > 0) {
      showToast(`${validFiles.length}장 추가 (추가 가능: ${remainingSlots}장)`);
    } else {
      showToast(`${validFiles.length}장 추가 (최대 도달)`);
    }
  }

  // 파일 입력 초기화 (같은 파일 재선택 가능하도록)
  fileInput.value = '';
}

/**
 * 미리보기 영역 업데이트
 */
function updatePreviewArea() {
  if (selectedFiles.length === 0) {
    previewArea.style.display = 'none';
    return;
  }

  previewArea.style.display = 'block';
  photoCount.textContent = selectedFiles.length;
  thumbnailGrid.innerHTML = '';

  selectedFiles.forEach((file, index) => {
    const thumbnailItem = createThumbnail(file, index);
    thumbnailGrid.appendChild(thumbnailItem);
  });
}

/**
 * 썸네일 생성
 */
function createThumbnail(file, index) {
  const item = document.createElement('div');
  item.className = 'thumbnail-item';

  const img = document.createElement('img');
  img.className = 'thumbnail-img';
  img.alt = file.name;

  // 표파일을 읽어서 이미지 시
  const reader = new FileReader();
  reader.onload = (e) => {
    img.src = e.target.result;
  };
  reader.readAsDataURL(file);

  // 삭제 버튼
  const deleteBtn = document.createElement('button');
  deleteBtn.className = 'thumbnail-delete';
  deleteBtn.innerHTML = '&times;';
  deleteBtn.setAttribute('aria-label', '삭제');
  deleteBtn.addEventListener('click', () => removeThumbnail(index));

  item.appendChild(img);
  item.appendChild(deleteBtn);

  return item;
}

/**
 * 썸네일 삭제
 */
function removeThumbnail(index) {
  selectedFiles.splice(index, 1);
  updatePreviewArea();
  updateUploadButtonState();
}

/**
 * 업로드 버튼 상태 업데이트
 */
function updateUploadButtonState() {
  const userName = userNameInput.value.trim();
  const hasFiles = selectedFiles.length > 0;

  uploadBtn.disabled = !(userName && hasFiles);
}

/**
 * 업로드 처리
 */
async function handleUpload() {
  const userName = userNameInput.value.trim();

  if (!userName) {
    showToast('이름을 입력해주세요.');
    return;
  }

  if (selectedFiles.length === 0) {
    showToast('업로드할 사진을 선택해주세요.');
    return;
  }

  // 업로드 버튼 비활성화
  uploadBtn.disabled = true;
  progressSection.style.display = 'block';

  try {
    const formData = new FormData();
    formData.append('uploaderName', userName);
    selectedFiles.forEach(file => {
      formData.append('files', file);
    });

    // 디버깅: 업로드 요청 정보 로그
    console.log('📤 Upload request started:', {
      url: '/api/events/devEvent/upload',
      uploaderName: userName,
      filesCount: selectedFiles.length,
      totalSize: selectedFiles.reduce((sum, f) => sum + f.size, 0),
      files: selectedFiles.map(f => ({ name: f.name, size: f.size, type: f.type }))
    });

    // XMLHttpRequest를 사용하여 실제 업로드 진행률 추적
    const xhr = new XMLHttpRequest();

    // 진행률 이벤트 핸들러
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        const percentage = Math.round((event.loaded / event.total) * 100);
        console.log(`📊 Upload progress: ${percentage}%`);
        updateProgress(percentage);
      }
    };

    // 업로드 완료 핸들러
    xhr.onload = () => {
      console.log('✅ Upload completed:', {
        status: xhr.status,
        statusText: xhr.statusText,
        response: xhr.responseText
      });
      if (xhr.status >= 200 && xhr.status < 300) {
        const result = JSON.parse(xhr.responseText);
        // TODO: 백엔드 응답 데이터 활용 (예: 업로드된 파일 수, 저장 경로 등)
        showSuccess();
      } else {
        // 에러 응답 처리
        let errorMessage = '업로드에 실패했습니다.';

        try {
          // TODO: 백엔드 에러 응답(CommonApiResponse)에서 구체적인 에러 메시지 파싱 및 표시 구현 필요
          const errorResponse = JSON.parse(xhr.responseText);
          console.error('❌ Upload error response:', errorResponse);
          // errorMessage = errorResponse.message || errorMessage;
        } catch (e) {
          // JSON 파싱 실패 시 기본 에러 메시지 사용
          console.error('❌ Failed to parse error response:', e);
        }

        console.error('❌ Upload failed:', {
          status: xhr.status,
          statusText: xhr.statusText,
          response: xhr.responseText
        });

        showToast('업로드 중 오류가 발생했습니다.\n' + errorMessage, 4000);
        resetUploadState();
      }
    };

    // 네트워크 에러 핸들러
    xhr.onerror = () => {
      console.error('❌ Network error during upload');
      showToast('업로드 중 네트워크 오류가 발생했습니다.', 4000);
      resetUploadState();
    };

    // 요청 전송
    xhr.open('POST', '/api/events/devEvent/upload');
    xhr.send(formData);
  } catch (error) {
    console.error('Upload error:', error);
    showToast('업로드 중 오류가 발생했습니다.\n' + error.message, 4000);
    resetUploadState();
  }
}

/**
 * 진행률 업데이트
 */
function updateProgress(percentage) {
  progressFill.style.width = `${percentage}%`;
  progressText.textContent = `업로드 중... ${percentage}%`;
}

/**
 * 성공 모달 표시
 */
function showSuccess() {
  progressSection.style.display = 'none';
  successModal.style.display = 'flex';
}

/**
 * 모달 닫기
 */
function closeModal() {
  successModal.style.display = 'none';
  resetUploadState();
}

/**
 * 업로드 상태 초기화
 */
function resetUploadState() {
  // 사진 선택 초기화
  selectedFiles = [];
  updatePreviewArea();

  // 진행률 초기화
  progressFill.style.width = '0%';
  progressText.textContent = '업로드 중... 0%';

  // 이름은 유지
  // userNameInput.value = '';

  // 버튼 상태 업데이트
  updateUploadButtonState();
}
