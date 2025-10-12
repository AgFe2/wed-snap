// 전역 변수
let selectedFiles = [];
const MAX_FILES = 40;
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

// 이벤트 리스너 등록
document.addEventListener('DOMContentLoaded', () => {
    selectBtn.addEventListener('click', () => fileInput.click());
    fileInput.addEventListener('change', handleFileSelect);
    userNameInput.addEventListener('input', updateUploadButtonState);
    uploadBtn.addEventListener('click', handleUpload);
    closeModalBtn.addEventListener('click', closeModal);
});

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
        alert(`최대 ${MAX_FILES}장까지만 선택할 수 있습니다.\n현재 ${selectedFiles.length}장 선택됨`);
        return;
    }
    
    // 파일 타입 검증 및 추가
    const validFiles = files.filter(file => {
        if (!ALLOWED_TYPES.includes(file.type.toLowerCase())) {
            alert(`${file.name}은(는) 지원하지 않는 파일 형식입니다.\n이미지 파일만 업로드 가능합니다.`);
            return false;
        }
        return true;
    });
    
    // 선택된 파일 추가
    selectedFiles = [...selectedFiles, ...validFiles];
    
    // UI 업데이트
    updatePreviewArea();
    updateUploadButtonState();
    
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
    
    // 파일을 읽어서 이미지 표시
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
        alert('이름을 입력해주세요.');
        return;
    }
    
    if (selectedFiles.length === 0) {
        alert('업로드할 사진을 선택해주세요.');
        return;
    }
    
    // 업로드 버튼 비활성화
    uploadBtn.disabled = true;
    progressSection.style.display = 'block';
    
    // FormData 준비
    const formData = new FormData();
    formData.append('userName', userName);
    selectedFiles.forEach(file => {
        formData.append('files', file);
    });
    
    // ========================================
    // TODO: 백엔드 구현 후 아래 주석 해제
    // ========================================
    /*
    try {
        const response = await fetch('/upload', {
            method: 'POST',
            body: formData
        });
        
        if (!response.ok) {
            throw new Error('업로드에 실패했습니다.');
        }
        
        const result = await response.json();
        showSuccess();
    } catch (error) {
        console.error('Upload error:', error);
        alert('업로드 중 오류가 발생했습니다.\n' + error.message);
        resetUploadState();
    }
    */
    
    // ========================================
    // 임시 시뮬레이션 (백엔드 구현 전)
    // ========================================
    simulateUpload();
}

/**
 * 업로드 시뮬레이션 (임시)
 */
function simulateUpload() {
    let progress = 0;
    const interval = setInterval(() => {
        progress += 5;
        updateProgress(progress);
        
        if (progress >= 100) {
            clearInterval(interval);
            setTimeout(() => {
                showSuccess();
            }, 300);
        }
    }, 100);
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
