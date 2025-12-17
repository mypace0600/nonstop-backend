package com.app.nonstop.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3Config {


    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.AP_NORTHEAST_2)
                // DefaultCredentialsProvider는 EC2 인스턴스 프로파일, 환경 변수 등에서 자격 증명을 자동으로 찾아줍니다.
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
