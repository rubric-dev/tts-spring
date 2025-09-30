package rubric_labs.tts_project;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Value("${spring.cloud.aws.region.static}")
    private String awsRegion;

    @Bean
    public PollyClient pollyClient() {
        return PollyClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .build();
    }
}

