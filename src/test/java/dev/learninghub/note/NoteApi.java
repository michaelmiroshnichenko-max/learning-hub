package dev.learninghub.note;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/** REST helpers for note tests. Requests run as the identity set by {@code @TestSecurity}. */
final class NoteApi {

    static final String PROBLEM_JSON = "application/problem+json";

    private NoteApi() {
    }

    static Map<String, Object> note(String title, String body, Integer courseWeek) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("title", title);
        json.put("body", body);
        if (courseWeek != null) {
            json.put("courseWeek", courseWeek);
        }
        return json;
    }

    static Map<String, Object> validNote() {
        return note("Panache paging", "Use `Page.of(index, size)`.", 5);
    }

    static Response post(Object body) {
        return given().contentType(ContentType.JSON).body(body).when().post("/api/notes");
    }

    static Response create(Object body) {
        return post(body).then().statusCode(201).extract().response();
    }

    static long createId(Object body) {
        return create(body).jsonPath().getLong("id");
    }

    static Response get(long id) {
        return given().accept(ContentType.JSON).when().get("/api/notes/" + id);
    }
}
