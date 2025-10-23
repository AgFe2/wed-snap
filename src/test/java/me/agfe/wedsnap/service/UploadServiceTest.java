package me.agfe.wedsnap.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import me.agfe.wedsnap.dto.UploadRequest;
import me.agfe.wedsnap.dto.UploadResponse;
import me.agfe.wedsnap.exception.ErrorCode;
import me.agfe.wedsnap.exception.WedSnapException;
import me.agfe.wedsnap.repository.UploadRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UploadService 테스트")
class UploadServiceTest {

    @Mock
    private UploadRepository uploadRepository;

    @InjectMocks
    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        // environment 필드 주입
        ReflectionTestUtils.setField(uploadService, "environment", "test");
    }

    @Test
    @DisplayName("파일 업로드 성공 - 단일 파일")
    void processUpload_SingleFile_Success() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "홍길동";
        String uniqueUploaderName = "홍길동";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(List.of(file))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class)))
                .thenReturn("saved-file-uuid.jpg");

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEventName()).isEqualTo(eventName);
        assertThat(response.getUploaderName()).isEqualTo(uniqueUploaderName);
        assertThat(response.getTotalFiles()).isEqualTo(1);
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailCount()).isEqualTo(0);
        assertThat(response.getFailedFiles()).isEmpty();
        assertThat(response.getMessage()).contains("1개 업로드 성공");
        assertThat(response.getTimestamp()).isNotNull();

        verify(uploadRepository, times(1)).findUniqueUploaderName(eventName, uploaderName);
        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 성공 - 다중 파일")
    void processUpload_MultipleFiles_Success() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "김철수";
        String uniqueUploaderName = "김철수";

        MockMultipartFile file1 = new MockMultipartFile(
                "file1", "image1.jpg", "image/jpeg", "content1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "file2", "image2.png", "image/png", "content2".getBytes()
        );
        MockMultipartFile file3 = new MockMultipartFile(
                "file3", "image3.gif", "image/gif", "content3".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(Arrays.asList(file1, file2, file3))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class)))
                .thenReturn("saved-uuid.jpg");

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getTotalFiles()).isEqualTo(3);
        assertThat(response.getSuccessCount()).isEqualTo(3);
        assertThat(response.getFailCount()).isEqualTo(0);
        assertThat(response.getFailedFiles()).isEmpty();

        verify(uploadRepository, times(3)).saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - 빈 파일")
    void processUpload_EmptyFile_Failure() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "이영희";
        String uniqueUploaderName = "이영희";

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(List.of(emptyFile))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getTotalFiles()).isEqualTo(1);
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailCount()).isEqualTo(1);
        assertThat(response.getFailedFiles()).contains("empty.jpg");
        assertThat(response.getMessage()).contains("0개 업로드 성공, 1개 실패");

        verify(uploadRepository, never()).saveFile(anyString(), anyString(), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - 파일명이 null")
    void processUpload_NullFileName_Failure() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "박민수";
        String uniqueUploaderName = "박민수";

        MockMultipartFile fileWithNullName = new MockMultipartFile(
                "file", null, "image/jpeg", "content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(List.of(fileWithNullName))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailCount()).isEqualTo(1);
        assertThat(response.getFailedFiles()).hasSize(1);

        verify(uploadRepository, never()).saveFile(anyString(), anyString(), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - 허용되지 않은 파일 확장자")
    void processUpload_InvalidExtension_Failure() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "최지영";
        String uniqueUploaderName = "최지영";

        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "pdf content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(List.of(invalidFile))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailCount()).isEqualTo(1);
        assertThat(response.getFailedFiles()).contains("document.pdf");

        verify(uploadRepository, never()).saveFile(anyString(), anyString(), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 부분 성공 - 일부 파일만 성공")
    void processUpload_PartialSuccess() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "정다은";
        String uniqueUploaderName = "정다은";

        MockMultipartFile validFile = new MockMultipartFile(
                "file1", "valid.jpg", "image/jpeg", "valid content".getBytes()
        );
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file2", "invalid.exe", "application/exe", "invalid content".getBytes()
        );
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file3", "empty.png", "image/png", new byte[0]
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(Arrays.asList(validFile, invalidFile, emptyFile))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), eq(validFile)))
                .thenReturn("saved-uuid.jpg");

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getTotalFiles()).isEqualTo(3);
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailCount()).isEqualTo(2);
        assertThat(response.getFailedFiles()).containsExactlyInAnyOrder("invalid.exe", "empty.png");
        assertThat(response.getMessage()).contains("1개 업로드 성공, 2개 실패");

        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - IOException 발생 시 예외 throw")
    void processUpload_IOException_ThrowsException() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "강민호";
        String uniqueUploaderName = "강민호";

        MockMultipartFile file = new MockMultipartFile(
                "file", "error.jpg", "image/jpeg", "content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(List.of(file))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class)))
                .thenThrow(new IOException("Disk full"));

        // when & then
        WedSnapException exception = assertThrows(WedSnapException.class, () -> uploadService.processUpload(request));

        // 예외 상세 검증
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);
        assertThat(exception.getDetail()).contains("error.jpg");
        assertThat(exception.getCause()).isInstanceOf(IOException.class);

        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class));
    }

    @Test
    @DisplayName("업로더명 중복 처리 확인")
    void processUpload_UniqueUploaderName_Applied() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "홍길동";
        String uniqueUploaderName = "홍길동(1)"; // 중복 처리된 이름

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(List.of(file))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class)))
                .thenReturn("saved-uuid.jpg");

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getUploaderName()).isEqualTo(uniqueUploaderName);
        assertThat(request.getUploaderName()).isEqualTo(uniqueUploaderName); // request 객체도 업데이트되어야 함

        verify(uploadRepository, times(1)).findUniqueUploaderName(eventName, uploaderName);
        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 확장자 대소문자 구분 없이 검증")
    void processUpload_CaseInsensitiveExtension_Success() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "김수진";
        String uniqueUploaderName = "김수진";

        MockMultipartFile upperCaseFile = new MockMultipartFile(
                "file", "photo.JPG", "image/jpeg", "content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(List.of(upperCaseFile))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class)))
                .thenReturn("saved-uuid.jpg");

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailCount()).isEqualTo(0);

        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class));
    }

    @Test
    @DisplayName("HEIF 확장자 파일 업로드 성공")
    void processUpload_HeifExtension_Success() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "이지훈";
        String uniqueUploaderName = "이지훈";

        MockMultipartFile heifFile = new MockMultipartFile(
                "file", "iphone-photo.heif", "image/heif", "heif content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(List.of(heifFile))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class)))
                .thenReturn("saved-uuid.heif");

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailCount()).isEqualTo(0);

        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class));
    }

    @Test
    @DisplayName("확장자가 없는 파일 업로드 실패")
    void processUpload_NoExtension_Failure() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "박서연";
        String uniqueUploaderName = "박서연";

        MockMultipartFile noExtFile = new MockMultipartFile(
                "file", "noextension", "image/jpeg", "content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(List.of(noExtFile))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailCount()).isEqualTo(1);
        assertThat(response.getFailedFiles()).contains("noextension");

        verify(uploadRepository, never()).saveFile(anyString(), anyString(), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - 파일명이 공백만 있는 경우")
    void processUpload_BlankFileName_Failure() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "윤서준";
        String uniqueUploaderName = "윤서준";

        MockMultipartFile blankNameFile = new MockMultipartFile(
                "file", "   ", "image/jpeg", "content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(List.of(blankNameFile))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailCount()).isEqualTo(1);
        assertThat(response.getFailedFiles()).hasSize(1);

        verify(uploadRepository, never()).saveFile(anyString(), anyString(), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - 점으로 시작하는 파일명")
    void processUpload_FileNameStartsWithDot_Failure() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "한지우";
        String uniqueUploaderName = "한지우";

        MockMultipartFile dotFile = new MockMultipartFile(
                "file", ".hidden", "image/jpeg", "content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(List.of(dotFile))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getFailCount()).isEqualTo(1);
        assertThat(response.getFailedFiles()).contains(".hidden");

        verify(uploadRepository, never()).saveFile(anyString(), anyString(), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 성공 - 여러 개의 점이 있는 파일명")
    void processUpload_MultipleDotsInFileName_Success() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "최민재";
        String uniqueUploaderName = "최민재";

        MockMultipartFile multiDotFile = new MockMultipartFile(
                "file", "my.photo.image.jpg", "image/jpeg", "content".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(List.of(multiDotFile))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class)))
                .thenReturn("saved-uuid.jpg");

        // when
        UploadResponse response = uploadService.processUpload(request);

        // then
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailCount()).isEqualTo(0);
        assertThat(response.getFailedFiles()).isEmpty();

        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), any(MultipartFile.class));
    }

    @Test
    @DisplayName("파일 업로드 중 IOException 발생 - 부분 성공 후 예외")
    void processUpload_IOException_AfterPartialSuccess_ThrowsException() throws IOException {
        // given
        String eventName = "wedding2024";
        String uploaderName = "송하은";
        String uniqueUploaderName = "송하은";

        MockMultipartFile validFile1 = new MockMultipartFile(
                "file1", "photo1.jpg", "image/jpeg", "content1".getBytes()
        );
        MockMultipartFile validFile2 = new MockMultipartFile(
                "file2", "photo2.jpg", "image/jpeg", "content2".getBytes()
        );
        MockMultipartFile errorFile = new MockMultipartFile(
                "file3", "photo3.jpg", "image/jpeg", "content3".getBytes()
        );

        UploadRequest request = UploadRequest.builder()
                                             .eventName(eventName)
                                             .uploaderName(uploaderName)
                                             .files(Arrays.asList(validFile1, validFile2, errorFile))
                                             .build();

        when(uploadRepository.findUniqueUploaderName(eventName, uploaderName))
                .thenReturn(uniqueUploaderName);
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), eq(validFile1)))
                .thenReturn("saved-uuid-1.jpg");
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), eq(validFile2)))
                .thenReturn("saved-uuid-2.jpg");
        when(uploadRepository.saveFile(eq(eventName), eq(uniqueUploaderName), eq(errorFile)))
                .thenThrow(new IOException("Network error"));

        // when & then
        WedSnapException exception = assertThrows(WedSnapException.class, () -> uploadService.processUpload(request));

        // 예외 상세 검증
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);
        assertThat(exception.getDetail()).contains("photo3.jpg");
        assertThat(exception.getCause()).isInstanceOf(IOException.class);

        // 첫 번째와 두 번째 파일은 성공적으로 저장되었고, 세 번째 파일에서 예외 발생
        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), eq(validFile1));
        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), eq(validFile2));
        verify(uploadRepository, times(1)).saveFile(eq(eventName), eq(uniqueUploaderName), eq(errorFile));
    }
}
