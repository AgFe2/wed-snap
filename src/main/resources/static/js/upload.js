// 전역 변수
let selectedFiles = [];
let toastQueue = [];
let isShowingToast = false;
const MAX_FILES = 20; // 최대 20장으로 제한
const MAX_FILE_SIZE = 20 * 1024 * 1024; // 10MB
const ALLOWED_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/heic', 'image/heif'];

// DOM 요소
const fileInput = document.getElementById('fileInput');
const selectBtn = document.getElementById('selectBtn');
const userNameInput = document.getElementById('userName');
const userNameError = document.getElementById('userNameError');
const previewArea = document.getElementById('previewArea');
const thumbnailGrid = document.getElementById('thumbnailGrid');
const photoCount = document.getElementById('photoCount');
const uploadBtn = document.getElementById('uploadBtn');
const progressSection = document.getElementById('progressSection');
const progressFill = document.getElementById('progressFill');
const progressText = document.getElementById('progressText');
const successModal = document.getElementById('successModal');
const successMessage = document.getElementById('successMessage');
const closeModalBtn = document.getElementById('closeModalBtn');
const errorModal = document.getElementById('errorModal');
const errorMessage = document.getElementById('errorMessage');
const closeErrorModalBtn = document.getElementById('closeErrorModalBtn');

// ========================================
// 이벤트 리스너 등록
// ========================================
document.addEventListener('DOMContentLoaded', () => {
  selectBtn.addEventListener('click', () => fileInput.click());
  fileInput.addEventListener('change', handleFileSelect);
  userNameInput.addEventListener('blur', () => validateUserName());
  userNameInput.addEventListener('input', () => {
    // 20글자 초과 시 자동으로 잘라내기
    if (userNameInput.value.length > 20) {
      userNameInput.value = userNameInput.value.substring(0, 20);
    }

    // 입력 중 에러가 있었다면 실시간 재검증
    if (userNameError.style.display !== 'none') {
      validateUserName();
    }
    updateUploadButtonState();
  });
  uploadBtn.addEventListener('click', handleUpload);
  closeModalBtn.addEventListener('click', closeModal);
  closeErrorModalBtn.addEventListener('click', closeErrorModal);

  // 성공 모달 외부 클릭 시 닫기
  successModal.addEventListener('click', (e) => {
    if (e.target === successModal) {
      closeModal();
    }
  });

  // 에러 모달 외부 클릭 시 닫기
  errorModal.addEventListener('click', (e) => {
    if (e.target === errorModal) {
      closeErrorModal();
    }
  });
});

// ========================================
// 유틸리티 함수
// ========================================

/**
 * 사용자 이름 검증
 */
function validateUserName() {
  const userName = userNameInput.value.trim();
  const regex = /^[가-힣a-zA-Z0-9]{2,20}$/;

  // 빈 값
  if (!userName) {
    showUserNameError('이름을 입력해주세요');
    return false;
  }

  // 2자 미만
  if (userName.length < 2) {
    showUserNameError('이름은 2자 이상이어야 합니다');
    return false;
  }

  // 20자 초과
  if (userName.length > 20) {
    showUserNameError('이름은 20자를 초과할 수 없습니다');
    return false;
  }

  // 특수문자/공백 포함 여부
  if (!regex.test(userName)) {
    showUserNameError('이름에는 특수문자나 공백을 포함할 수 없습니다');
    return false;
  }

  // 검증 통과
  hideUserNameError();
  return true;
}

/**
 * 사용자 이름 에러 표시
 */
function showUserNameError(message) {
  userNameInput.classList.add('input-error');
  userNameError.textContent = message;
  userNameError.style.display = 'block';
}

/**
 * 사용자 이름 에러 숨김
 */
function hideUserNameError() {
  userNameInput.classList.remove('input-error');
  userNameError.textContent = '';
  userNameError.style.display = 'none';
}

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
 * 에러 코드를 사용자 친화적인 메시지로 변환
 */
function getErrorMessage(errorCode) {
  const errorMessages = {
    'FILE_SIZE_EXCEEDED': {
      emoji: '📦',
      title: '파일이 너무 커요',
      message: '사진 용량이 제한을 초과했습니다.\n더 작은 사진으로 다시 시도해주세요.'
    },
    'CONSTRAINT_VIOLATION': {
      emoji: '⚠️',
      title: '입력값을 확인해주세요',
      message: '이름 또는 사진 선택에 문제가 있습니다.\n다시 확인해주세요.'
    },
    'INVALID_VALUE': {
      emoji: '❌',
      title: '잘못된 요청이에요',
      message: '입력하신 정보에 문제가 있습니다.\n페이지를 새로고침하고 다시 시도해주세요.'
    },
    'MISSING_REQUEST_PARAMETER': {
      emoji: '📝',
      title: '필요한 정보가 없어요',
      message: '이름과 사진을 모두 입력해주세요.'
    },
    'NOT_MULTIPART_REQUEST': {
      emoji: '🔄',
      title: '업로드 형식 오류',
      message: '페이지를 새로고침하고 다시 시도해주세요.'
    },
    'INTERNAL_ERROR': {
      emoji: '🔧',
      title: '서버에 문제가 생겼어요',
      message: '일시적인 오류가 발생했습니다.\n잠시 후 다시 시도해주세요.'
    },
    'RESPONSE_STATUS_ERROR': {
      emoji: '⚠️',
      title: '요청 처리 실패',
      message: '요청을 처리할 수 없습니다.\n잠시 후 다시 시도해주세요.'
    }
  };

  return errorMessages[errorCode] || {
    emoji: '❓',
    title: '알 수 없는 오류',
    message: '문제가 발생했습니다.\n계속 문제가 발생하면 관리자에게 문의해주세요.'
  };
}

/**
 * 파일 선택 처리
 */
function handleFileSelect(event) {
  const files = Array.from(event.target.files);

  // 파일이 선택되지 않은 경우
  // if (files.length === 0) {
  //   return;
  // }

  // 파일 수 제한 확인
  // const totalFiles = selectedFiles.length + files.length;
  // if (totalFiles > MAX_FILES) {
  //   showToast(`최대 ${MAX_FILES}장 제한 (추가 가능: ${MAX_FILES - selectedFiles.length}장)`);
  //   return;
  // }

  // 파일 타입 및 크기 검증
  const validFiles = [];
  const invalidTypeFiles = [];
  const oversizedFiles = [];
  const heicFiles = [];

  files.forEach(file => {
    // // 타입 검증
    // if (!ALLOWED_TYPES.includes(file.type.toLowerCase())) {
    //   invalidTypeFiles.push(file.name);
    //   return;
    // }
    //
    // // 크기 검증
    // if (!validateFileSize(file)) {
    //   oversizedFiles.push({
    //     name: file.name,
    //     size: formatFileSize(file.size)
    //   });
    //   return;
    // }
    //
    // // HEIC 파일 감지
    // if (isHEICFile(file)) {
    //   heicFiles.push(file.name);
    // }

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
  // const hasFiles = selectedFiles.length > 0;
  // const isValidUserName = validateUserName();
  //
  // uploadBtn.disabled = !(isValidUserName && hasFiles);
  uploadBtn.disabled = false;
}

/**
 * 업로드 처리
 */
async function handleUpload() {
  const userName = userNameInput.value.trim();

  // if (!userName) {
  //   showToast('이름을 입력해주세요.');
  //   return;
  // }
  //
  // if (selectedFiles.length === 0) {
  //   showToast('업로드할 사진을 선택해주세요.');
  //   return;
  // }

  // 업로드 버튼 비활성화
  uploadBtn.disabled = true;
  progressSection.style.display = 'block';

  try {
    const formData = new FormData();
    formData.append('uploaderName', userName);
    selectedFiles.forEach(file => {
      formData.append('files', file);
    });

    // XMLHttpRequest를 사용하여 실제 업로드 진행률 추적
    const xhr = new XMLHttpRequest();

    // 진행률 이벤트 핸들러
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        const percentage = Math.round((event.loaded / event.total) * 100);
        updateProgress(percentage);
      }
    };

    // 업로드 완료 핸들러
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const response = JSON.parse(xhr.responseText);

          // 성공 응답 (result === true)
          if (response.result === true) {
            const data = response.data || {};
            const successCount = data.successCount || 0;
            const failCount = data.failCount || 0;
            const failedFiles = data.failedFiles || [];

            // 실패한 파일 제외하고 성공한 파일만 제거
            if (failedFiles.length > 0) {
              selectedFiles = selectedFiles.filter(file =>
                failedFiles.includes(file.name)
              );
              updatePreviewArea();
            } else {
              selectedFiles = [];
            }

            showSuccess(successCount, failCount);
          }
          // 에러 응답 (result === false)
          else {
            const error = response.error || {};
            const errorCode = error.errorCode;
            const errorInfo = getErrorMessage(errorCode);

            showErrorModal(
              `${errorInfo.emoji} ${errorInfo.title}`,
              errorInfo.message
            );
            resetUploadState();
          }
        } catch (e) {
          // JSON 파싱 실패
          showErrorModal('🔧 서버 오류', '응답 처리 중 오류가 발생했습니다.\n잠시 후 다시 시도해주세요.');
          resetUploadState();
        }
      } else {
        // HTTP 에러 (4xx, 5xx)
        let errorCode = null;
        try {
          const response = JSON.parse(xhr.responseText);
          const error = response.error || {};
          errorCode = error.errorCode;
        } catch (e) {
          // JSON 파싱 실패 시 errorCode는 null로 유지
        }

        const errorInfo = getErrorMessage(errorCode);
        showErrorModal(
          `${errorInfo.emoji} ${errorInfo.title}`,
          errorInfo.message
        );
        resetUploadState();
      }
    };

    // 네트워크 에러 핸들러
    xhr.onerror = () => {
      showErrorModal('📡 네트워크 오류', '네트워크 연결이 불안정합니다.\n인터넷 연결 상태를 확인해주세요.');
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
function showSuccess(successCount, failCount) {
  progressSection.style.display = 'none';

  // 메시지 업데이트
  let message = '소중한 순간을 공유해 주셔서 감사합니다';
  if (failCount > 0) {
    message += `<br><small style="font-size: 0.85em; color: var(--text-secondary);">(성공: ${successCount}장, 실패: ${failCount}장)</small>`;
  } else {
    message += `<br><small style="font-size: 0.85em; color: var(--text-secondary);">(성공: ${successCount}장)</small>`;
  }

  successMessage.innerHTML = message;
  successModal.style.display = 'flex';
}

/**
 * 성공 모달 닫기
 */
function closeModal() {
  successModal.style.display = 'none';
  resetUploadState();
  updatePreviewArea();
}

/**
 * 에러 모달 표시
 */
function showErrorModal(title, message) {
  progressSection.style.display = 'none';

  // title 업데이트
  const errorTitle = errorModal.querySelector('.modal-title');
  if (errorTitle) {
    errorTitle.textContent = title || '업로드 실패';
  }

  errorMessage.textContent = message || '업로드에 실패했습니다.';
  errorModal.style.display = 'flex';
}

/**
 * 에러 모달 닫기
 */
function closeErrorModal() {
  errorModal.style.display = 'none';
  // 에러 모달은 닫을 때 파일을 유지 (재시도 가능)
  updatePreviewArea();
}

/**
 * 업로드 상태 초기화
 * 에러 발생 시 파일 목록은 유지하여 재시도 가능하도록 함
 */
function resetUploadState() {
  // 파일 목록은 각 케이스에서 개별 처리 (에러 시 유지, 성공 모달 닫을 때 정리)

  // 진행률 초기화
  progressFill.style.width = '0%';
  progressText.textContent = '업로드 중... 0%';

  // 버튼 상태 업데이트
  updateUploadButtonState();
}
