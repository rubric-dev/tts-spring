package rubric_labs.tts_project;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;

@Configuration
public class AwsConfig {

    @Bean
    public PollyClient pollyClient() {
        return PollyClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
    }
}

