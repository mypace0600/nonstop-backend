package com.app.nonstop.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3Config {

    // application.yml에 설정된 AWS 리전 값을 주입받습니다.
    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                // DefaultCredentialsProvider는 EC2 인스턴스 프로파일, 환경 변수 등에서 자격 증명을 자동으로 찾아줍니다.
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
