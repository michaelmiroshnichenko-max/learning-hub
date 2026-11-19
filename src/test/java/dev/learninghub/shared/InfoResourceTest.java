package dev.learninghub.shared;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class InfoResourceTest {

    @Test
    void returnsApplicationNameAndProjectVersion() {
        String projectVersion = System.getProperty("project.version");

        given()
                .when().get("/api/info")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", equalTo("learning-hub"))
                .body("version", equalTo(projectVersion));
    }
}
