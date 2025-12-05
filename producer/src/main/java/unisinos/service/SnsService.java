package unisinos.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@ApplicationScoped
public class SnsService {

    @Inject
    SnsClient snsClient;

    @ConfigProperty(name = "sns.topic.arn")
    String topicArn;

    @ConfigProperty(name = "sns.timeout-ms")
    long timeoutMs;

    @ConfigProperty(name = "sns.fifo.message.group.id")
    String groupId;

    private static final Logger LOG = Logger.getLogger(SnsService.class);
    private static final int MAX_ATTEMPTS = 3;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public Instant publish(UUID id, String text) {

        long sentAtMillis = System.currentTimeMillis();

        PublishRequest request = PublishRequest.builder()
                .topicArn(topicArn)
                .messageGroupId(groupId)
                .message(text)
                .messageAttributes(Map.of(
                        "messageId", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(id.toString())
                                .build(),
                        "sentAt", MessageAttributeValue.builder()
                                .dataType("Number")
                                .stringValue(String.valueOf(sentAtMillis))
                                .build()
                ))
                .build();

        int attempt = 0;

        while (attempt < MAX_ATTEMPTS) {
            attempt++;

            try {
                LOG.infof("SNS publish attempt %d for message %s", attempt, id);

                Future<PublishResponse> future = executor.submit(() -> snsClient.publish(request));

                PublishResponse resp = future.get(timeoutMs, TimeUnit.MILLISECONDS);

                LOG.infof("SNS publish successful. MessageId: %s", resp.messageId());

                return Instant.now();

            } catch (TimeoutException tex) {
                LOG.warnf("SNS publish TIMEOUT on attempt %d: %s", attempt, tex.getMessage());
            } catch (Exception e) {
                LOG.warnf("SNS publish attempt %d failed: %s", attempt, e.getMessage());
            }

            if (attempt >= MAX_ATTEMPTS) {
                LOG.error("SNS publish failed after max attempts.");
                throw new RuntimeException("Failed to publish SNS message after retries.");
            }
        }

        throw new IllegalStateException("Unexpected state in SNS publish retry logic.");
    }
}