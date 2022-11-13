package org.acme.sqs;

import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import org.acme.sqs.model.Quark;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.jboss.logging.MDC;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;

//@Path("/sync/shield")
public class QuarksShieldSyncResource {

    private static final Logger LOGGER = Logger.getLogger(QuarksShieldSyncResource.class);

    @Inject
    SqsClient sqs;

    @ConfigProperty(name = "queue.url")
    String queueUrl;

    static ObjectReader QUARK_READER = new ObjectMapper().readerFor(Quark.class);

    @Scheduled(every = "3s")
    public void scheduledMethod(){
        Span span = createSpanLinkedToParent();
        try (Scope scope = span.makeCurrent()) {
            receive();
        } finally {
            span.end();
        }
    }

//    @GET
    public void receive() {
        //update traceId in MDC so we can see it in the logs
        MDC.put("traceId", Span.current().getSpanContext().getTraceId());
        MDC.put("spanId", Span.current().getSpanContext().getTraceId());
        Log.info("receive");
//        List<Message> messages = sqs.receiveMessage(m -> m.maxNumberOfMessages(10).queueUrl(queueUrl)).messages();
//
//        //update traceId in MDC so we can see it in the logs (just in case the sqs.receiveMessage changed the Span.current().getSpanContext())
//        MDC.put("traceId", Span.current().getSpanContext().getTraceId());
//        MDC.put("spanId", Span.current().getSpanContext().getTraceId());
//        Log.info("after receiveMessage");
//
//        //delete received message(s)
//        messages.forEach(m -> sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(m.receiptHandle()).build()));

//        return messages.stream()
//            .map(Message::body)
//            .map(this::toQuark)
//            .collect(Collectors.toList());
    }

    /*
    https://stackoverflow.com/questions/72668718/how-to-create-context-using-traceid-in-open-telemetry
     */
    private static Span createSpanLinkedToParent() {
        // Fetch the trace and span IDs from wherever you've stored them
//        String traceIdHex = System.getProperty("otel.traceid");
//        String spanIdHex = System.getProperty("otel.spanid");



        String traceIdHex =  HexFormat.of().formatHex("123".getBytes());
        String spanIdHex = HexFormat.of().formatHex("456".getBytes());

        Log.info("traceIdHex: " + traceIdHex);
        Log.info("spanIdHex: " + spanIdHex);

        SpanContext remoteContext = SpanContext.createFromRemoteParent(
                traceIdHex,
                spanIdHex,
                TraceFlags.getSampled(),
                TraceState.getDefault());

        return GlobalOpenTelemetry.getTracer("")
                .spanBuilder("root span name")
                .setParent(Context.current().with(Span.wrap(remoteContext)))
                .startSpan();
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