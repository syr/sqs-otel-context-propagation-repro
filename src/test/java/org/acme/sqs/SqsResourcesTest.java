package org.acme.sqs;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.containsString;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
@QuarkusTestResource(SqsResource.class)
public class SqsResourcesTest {

    private static final BiFunction<String, String, String> QUARK = (flavor, spin) -> String
        .format("{\"flavor\":\"%s\", \"spin\":\"%s\"}", flavor, spin);

    @ParameterizedTest
    @ValueSource(strings = {"sync"})
    void testResource(final String testedResource) {
        List<String> quarks = Arrays.asList("Charm");

        //Fire quarks
        quarks.forEach(quark -> {
            given()
                .pathParam("resource", testedResource)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(QUARK.apply(quark, "1/2"))
                .when()
                .post("/{resource}/cannon/shoot")
                .then()
                .statusCode(Status.OK.getStatusCode())
                .body(any(String.class));
        });

        //Read quarks from the queue
        given()
            .pathParam("resource", testedResource)
            .when()
            .get("/{resource}/shield")
            .then()
            .statusCode(Status.OK.getStatusCode())
            .body(containsString("Charm"));
    }
}
