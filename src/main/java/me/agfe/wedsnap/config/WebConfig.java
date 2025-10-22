package me.agfe.wedsnap.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 MVC 설정 클래스
 * 정적 리소스(CSS, JS, 이미지 등)에 대한 캐시 제어 설정을 담당합니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 정적 리소스 핸들러 설정
     * CSS, JS 파일에 대해 브라우저 캐싱을 방지하는 HTTP 헤더를 설정합니다.
     *
     * @param registry 리소스 핸들러 레지스트리
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // CSS 파일 캐싱 방지 설정
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(CacheControl.noStore()
                                             .mustRevalidate()
                                             .cachePrivate());

        // JS 파일 캐싱 방지 설정
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCacheControl(CacheControl.noStore()
                                             .mustRevalidate()
                                             .cachePrivate());

        // 이미지 파일은 캐싱 허용 (성능 최적화)
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS)
                                             .cachePublic());
    }
}
