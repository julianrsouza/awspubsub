package unisinos.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@ApplicationScoped
public class SnsClientProducer {

    @ConfigProperty(name = "aws.region")
    String region;

    @ConfigProperty(name = "aws.access-key")
    String accessKey;

    @ConfigProperty(name = "aws.secret-key")
    String secretKey;

    @Produces
    @ApplicationScoped
    public SnsClient produce() {
        return SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .build();
    }
}