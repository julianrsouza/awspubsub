package unisinos.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import unisinos.entity.Message;
import unisinos.enumeration.MessageStatus;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class SqsService {

    private static final Logger LOG = Logger.getLogger(SqsService.class);

    @Inject
    SqsClient sqsClient;

    @ConfigProperty(name = "sqs.queue.url")
    String queueUrl;

    @Scheduled(every = "{consumer.sqs.poll-interval}")
    public void poll() {
        try {
            ReceiveMessageRequest req = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(5)
                    .waitTimeSeconds(10)
                    .messageAttributeNames("All")
                    .build();

            ReceiveMessageResponse resp = sqsClient.receiveMessage(req);

            for (software.amazon.awssdk.services.sqs.model.Message m : resp.messages()) {
                processMessage(m);
            }

        } catch (Exception e) {
            LOG.error("Erro ao processar mensagens do SQS", e);
        }
    }

    @Transactional
    public void processMessage(software.amazon.awssdk.services.sqs.model.Message sqsMsg) {
        try {
            JsonReader reader = Json.createReader(new StringReader(sqsMsg.body()));
            JsonObject snsWrapper = reader.readObject();
            reader.close();

            JsonObject messageAttributes = snsWrapper.getJsonObject("MessageAttributes");
            JsonObject messageIdObj = messageAttributes.getJsonObject("messageId");
            JsonObject sentAtObj = messageAttributes.getJsonObject("sentAt");
            UUID id = UUID.fromString(messageIdObj.getString("Value"));
            String text = snsWrapper.getString("Message");
            long sentAtMs = Long.valueOf(sentAtObj.getString("Value"));
            long receivedAt = System.currentTimeMillis();
            long latency = receivedAt - sentAtMs;

            LOG.infov("Recebida msg {0} – latencia {1} ms, texto {2}", id, latency, text );

            Message existing = Message.findById(id);
            Message msg;

            if (existing == null) {
                msg = new Message();
                msg.setId(id);
                msg.setText(text);
                msg.setSentAt(LocalDateTime.now());
            } else if (existing.getStatus() == MessageStatus.PROCESSED) {
                LOG.infov("Mensagem duplicada ignorada: {0}", id);
                delete(sqsMsg);
                return;
            } else {
                msg = existing;
            }

            msg.setUpdatedAt(LocalDateTime.now());
            msg.setStatus(MessageStatus.PROCESSED);
            msg.persist();

        } catch (Exception e) {
            LOG.error("Falha ao processar mensagem SQS", e);
        }

        try {
            delete(sqsMsg);
        } catch (Exception e) {
            LOG.error("Falha ao deletar mensagem SQS", e);
        }
    }

    private void delete(software.amazon.awssdk.services.sqs.model.Message msg) {
        sqsClient.deleteMessage(
                DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(msg.receiptHandle())
                        .build()
        );
    }
}
