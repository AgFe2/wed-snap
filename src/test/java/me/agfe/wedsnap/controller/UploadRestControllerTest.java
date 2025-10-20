package me.agfe.wedsnap.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import me.agfe.wedsnap.dto.UploadRequest;
import me.agfe.wedsnap.dto.UploadResponse;
import me.agfe.wedsnap.service.UploadService;

@WebMvcTest(UploadRestController.class)
@DisplayName("UploadRestController 테스트")
class UploadRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadService uploadService;

    @Test
    @DisplayName("파일 업로드 성공 - 정상적인 요청")
    void upload_Success() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "홍길동";

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        UploadResponse mockResponse = UploadResponse.builder()
                                                    .eventName(eventName)
                                                    .uploaderName(uploaderName)
                                                    .totalFiles(1)
                                                    .successCount(1)
                                                    .failCount(0)
                                                    .failedFiles(Collections.emptyList())
                                                    .timestamp(LocalDateTime.now())
                                                    .message("1개 업로드 성공, 0개 실패")
                                                    .build();

        when(uploadService.processUpload(any(UploadRequest.class))).thenReturn(mockResponse);

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.result").value(true))
               .andExpect(jsonPath("$.data.eventName").value(eventName))
               .andExpect(jsonPath("$.data.uploaderName").value(uploaderName))
               .andExpect(jsonPath("$.data.totalFiles").value(1))
               .andExpect(jsonPath("$.data.successCount").value(1));

        verify(uploadService, times(1)).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 성공 - 다중 파일")
    void upload_MultipleFiles_Success() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "김철수";

        MockMultipartFile file1 = new MockMultipartFile(
                "files", "image1.jpg", "image/jpeg", "content1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "image2.png", "image/png", "content2".getBytes()
        );
        MockMultipartFile file3 = new MockMultipartFile(
                "files", "image3.gif", "image/gif", "content3".getBytes()
        );

        UploadResponse mockResponse = UploadResponse.builder()
                                                    .eventName(eventName)
                                                    .uploaderName(uploaderName)
                                                    .totalFiles(3)
                                                    .successCount(3)
                                                    .failCount(0)
                                                    .failedFiles(Collections.emptyList())
                                                    .timestamp(LocalDateTime.now())
                                                    .message("3개 업로드 성공, 0개 실패")
                                                    .build();

        when(uploadService.processUpload(any(UploadRequest.class))).thenReturn(mockResponse);

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file1);
        builder.file(file2);
        builder.file(file3);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.result").value(true))
               .andExpect(jsonPath("$.data.totalFiles").value(3))
               .andExpect(jsonPath("$.data.successCount").value(3));

        verify(uploadService, times(1)).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - files가 빈 리스트")
    void upload_FilesEmpty_Failure() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "박민수";

        MockMultipartFile emptyFile = new MockMultipartFile(
                "files", "empty.jpg", "image/jpeg", new byte[0]
        );

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(emptyFile);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isBadRequest());

        verify(uploadService, never()).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - 파일 개수가 20개 초과")
    void upload_FilesExceedMaxSize_Failure() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "최지영";

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.param("uploaderName", uploaderName);

        // 21개의 파일 추가
        for (int i = 1; i <= 21; i++) {
            builder.file(new MockMultipartFile(
                    "files",
                    "image" + i + ".jpg",
                    "image/jpeg",
                    ("content" + i).getBytes()
            ));
        }

        mockMvc.perform(builder)
               .andExpect(status().isBadRequest());

        verify(uploadService, never()).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - uploaderName이 null")
    void upload_UploaderNameNull_Failure() throws Exception {
        // given
        String eventName = "wedding2024";

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "content".getBytes()
        );

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file);

        mockMvc.perform(builder)
               .andExpect(status().isBadRequest());

        verify(uploadService, never()).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - uploaderName이 빈 문자열")
    void upload_UploaderNameBlank_Failure() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "   ";

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "content".getBytes()
        );

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isBadRequest());

        verify(uploadService, never()).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - uploaderName이 2자 미만")
    void upload_UploaderNameTooShort_Failure() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "홍";

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "content".getBytes()
        );

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isBadRequest());

        verify(uploadService, never()).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - uploaderName이 20자 초과")
    void upload_UploaderNameTooLong_Failure() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "가나다라마바사아자차카타파하가나다라마바사";  // 21자

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "content".getBytes()
        );

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isBadRequest());

        verify(uploadService, never()).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - uploaderName에 특수문자 포함")
    void upload_UploaderNameWithSpecialChars_Failure() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "홍길동!@#";

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "content".getBytes()
        );

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isBadRequest());

        verify(uploadService, never()).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - uploaderName에 공백 포함")
    void upload_UploaderNameWithSpace_Failure() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "홍 길 동";

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "content".getBytes()
        );

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isBadRequest());

        verify(uploadService, never()).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 실패 - uploaderName에 비속어 포함")
    void upload_UploaderNameWithProfanity_Failure() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "시발홍길동";

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "content".getBytes()
        );

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isBadRequest());

        verify(uploadService, never()).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 성공 - uploaderName 한글만")
    void upload_UploaderNameKoreanOnly_Success() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "홍길동";

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "content".getBytes()
        );

        UploadResponse mockResponse = UploadResponse.builder()
                                                    .eventName(eventName)
                                                    .uploaderName(uploaderName)
                                                    .totalFiles(1)
                                                    .successCount(1)
                                                    .failCount(0)
                                                    .failedFiles(Collections.emptyList())
                                                    .timestamp(LocalDateTime.now())
                                                    .message("1개 업로드 성공, 0개 실패")
                                                    .build();

        when(uploadService.processUpload(any(UploadRequest.class))).thenReturn(mockResponse);

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isOk());

        verify(uploadService, times(1)).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 성공 - uploaderName 영문만")
    void upload_UploaderNameEnglishOnly_Success() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "HongGilDong";

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "content".getBytes()
        );

        UploadResponse mockResponse = UploadResponse.builder()
                                                    .eventName(eventName)
                                                    .uploaderName(uploaderName)
                                                    .totalFiles(1)
                                                    .successCount(1)
                                                    .failCount(0)
                                                    .failedFiles(Collections.emptyList())
                                                    .timestamp(LocalDateTime.now())
                                                    .message("1개 업로드 성공, 0개 실패")
                                                    .build();

        when(uploadService.processUpload(any(UploadRequest.class))).thenReturn(mockResponse);

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isOk());

        verify(uploadService, times(1)).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 성공 - uploaderName 숫자 포함")
    void upload_UploaderNameWithNumber_Success() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "홍길동123";

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "content".getBytes()
        );

        UploadResponse mockResponse = UploadResponse.builder()
                                                    .eventName(eventName)
                                                    .uploaderName(uploaderName)
                                                    .totalFiles(1)
                                                    .successCount(1)
                                                    .failCount(0)
                                                    .failedFiles(Collections.emptyList())
                                                    .timestamp(LocalDateTime.now())
                                                    .message("1개 업로드 성공, 0개 실패")
                                                    .build();

        when(uploadService.processUpload(any(UploadRequest.class))).thenReturn(mockResponse);

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isOk());

        verify(uploadService, times(1)).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 성공 - eventName 다양한 형식")
    void upload_EventNameVariousFormats_Success() throws Exception {
        // given
        String eventName = "wedding-2024-spring";
        String uploaderName = "홍길동";

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "content".getBytes()
        );

        UploadResponse mockResponse = UploadResponse.builder()
                                                    .eventName(eventName)
                                                    .uploaderName(uploaderName)
                                                    .totalFiles(1)
                                                    .successCount(1)
                                                    .failCount(0)
                                                    .failedFiles(Collections.emptyList())
                                                    .timestamp(LocalDateTime.now())
                                                    .message("1개 업로드 성공, 0개 실패")
                                                    .build();

        when(uploadService.processUpload(any(UploadRequest.class))).thenReturn(mockResponse);

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.eventName").value(eventName));

        verify(uploadService, times(1)).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 성공 - 경계값 테스트: 정확히 20개 파일")
    void upload_ExactlyTwentyFiles_Success() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "정다은";

        UploadResponse mockResponse = UploadResponse.builder()
                                                    .eventName(eventName)
                                                    .uploaderName(uploaderName)
                                                    .totalFiles(20)
                                                    .successCount(20)
                                                    .failCount(0)
                                                    .failedFiles(Collections.emptyList())
                                                    .timestamp(LocalDateTime.now())
                                                    .message("20개 업로드 성공, 0개 실패")
                                                    .build();

        when(uploadService.processUpload(any(UploadRequest.class))).thenReturn(mockResponse);

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.param("uploaderName", uploaderName);

        // 정확히 20개의 파일 추가
        for (int i = 1; i <= 20; i++) {
            builder.file(new MockMultipartFile(
                    "files",
                    "image" + i + ".jpg",
                    "image/jpeg",
                    ("content" + i).getBytes()
            ));
        }

        mockMvc.perform(builder)
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.result").value(true))
               .andExpect(jsonPath("$.data.totalFiles").value(20));

        verify(uploadService, times(1)).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 성공 - 경계값 테스트: uploaderName 정확히 2자")
    void upload_UploaderNameExactlyTwoChars_Success() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "홍길";

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "content".getBytes()
        );

        UploadResponse mockResponse = UploadResponse.builder()
                                                    .eventName(eventName)
                                                    .uploaderName(uploaderName)
                                                    .totalFiles(1)
                                                    .successCount(1)
                                                    .failCount(0)
                                                    .failedFiles(Collections.emptyList())
                                                    .timestamp(LocalDateTime.now())
                                                    .message("1개 업로드 성공, 0개 실패")
                                                    .build();

        when(uploadService.processUpload(any(UploadRequest.class))).thenReturn(mockResponse);

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isOk());

        verify(uploadService, times(1)).processUpload(any(UploadRequest.class));
    }

    @Test
    @DisplayName("파일 업로드 성공 - 경계값 테스트: uploaderName 정확히 20자")
    void upload_UploaderNameExactlyTwentyChars_Success() throws Exception {
        // given
        String eventName = "wedding2024";
        String uploaderName = "가나다라마바사아자차카타파하가나다라마바";  // 정확히 20자

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.jpg", "image/jpeg", "content".getBytes()
        );

        UploadResponse mockResponse = UploadResponse.builder()
                                                    .eventName(eventName)
                                                    .uploaderName(uploaderName)
                                                    .totalFiles(1)
                                                    .successCount(1)
                                                    .failCount(0)
                                                    .failedFiles(Collections.emptyList())
                                                    .timestamp(LocalDateTime.now())
                                                    .message("1개 업로드 성공, 0개 실패")
                                                    .build();

        when(uploadService.processUpload(any(UploadRequest.class))).thenReturn(mockResponse);

        // when & then
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/events/{eventName}/upload", eventName);
        builder.file(file);
        builder.param("uploaderName", uploaderName);

        mockMvc.perform(builder)
               .andExpect(status().isOk());

        verify(uploadService, times(1)).processUpload(any(UploadRequest.class));
    }
}
