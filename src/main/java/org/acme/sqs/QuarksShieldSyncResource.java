package org.acme.sqs;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.opentelemetry.api.trace.Span;
import io.quarkus.logging.Log;
import org.acme.sqs.model.Quark;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.jboss.logging.MDC;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;

@Path("/sync/shield")
public class QuarksShieldSyncResource {

    private static final Logger LOGGER = Logger.getLogger(QuarksShieldSyncResource.class);

    @Inject
    SqsClient sqs;

    @ConfigProperty(name = "queue.url")
    String queueUrl;

    static ObjectReader QUARK_READER = new ObjectMapper().readerFor(Quark.class);

    @GET
    public List<Quark> receive() {
        //update traceId in MDC so we can see it in the logs
        MDC.put("traceId", Span.current().getSpanContext().getTraceId());
        MDC.put("spanId", Span.current().getSpanContext().getTraceId());
        List<Message> messages = sqs.receiveMessage(m -> m.maxNumberOfMessages(10).queueUrl(queueUrl)).messages();

        //update traceId in MDC so we can see it in the logs (just in case the sqs.receiveMessage changed the Span.current().getSpanContext())
        MDC.put("traceId", Span.current().getSpanContext().getTraceId());
        MDC.put("spanId", Span.current().getSpanContext().getTraceId());
        Log.info("after receiveMessage");

        //delete received message(s)
        messages.forEach(m -> sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(m.receiptHandle()).build()));

        return messages.stream()
            .map(Message::body)
            .map(this::toQuark)
            .collect(Collectors.toList());
    }

    private Quark toQuark(String message) {
        Quark quark = null;
        try {
            quark = QUARK_READER.readValue(message);
            LOGGER.info("Receiving message" + message);
        } catch (Exception e) {
            LOGGER.error("Error decoding message", e);
            throw new RuntimeException(e);
        }
        return quark;
    }
}