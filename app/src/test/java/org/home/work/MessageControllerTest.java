package org.home.work;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.runtime.server.EmbeddedServer;
import org.home.work.rockets.Rocket;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MessageControllerTest {
    private EmbeddedServer server;
    private BlockingHttpClient client;

    @BeforeEach
    void setupServer() {
        server = ApplicationContext.run(EmbeddedServer.class);
        client = server
            .getApplicationContext()
            .createBean(HttpClient.class, server.getURL()).toBlocking();
    }

    // with one server these tests would impact each other's state, so reset after every call
    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop();
        }
        if (client != null) {
            client.stop();
        }
    }

    @Test
    void malformedJsonReturnsBadRequest() {
        var post = HttpRequest.POST("/messages", """
            {
                "channel": "193270a9-c9cf-404a-8f83-838e71d9ae67",
                "messageNumber": 1,
                "messageTime": "2022-02-02T19:39:05.86337+01:00",
                "messageType": "RocketLaunched"
            }
            """);

        var exception = assertThrowsExactly(HttpClientResponseException.class, () -> client.exchange(post));
        assertEquals(400, exception.code());
    }

    @Test
    void postedRocketReturnedInList() {
        var post = HttpRequest.POST("/messages", rocketLaunchMessage("abc123", 500, "ARTEMIS", "Falcon-9"));

        var responsePost = client.exchange(post);
        assertEquals(200, responsePost.code());


        var responseRockets = client.exchange("/rockets");
        assertEquals(200, responseRockets.code());

        var rocketList = readResponse(responseRockets, new TypeReference<>() {});
        assertEquals(1, rocketList.size());
    }

    @Test
    void explodedRocketStatusChanged() {
        var post = HttpRequest.POST("/messages", rocketLaunchMessage("abc123", 500, "ARTEMIS", "Falcon-9"));

        var responsePost = client.exchange(post);
        assertEquals(200, responsePost.code());

        var responseRockets = client.exchange("/rockets");
        assertEquals(200, responseRockets.code());


        var postExplode = HttpRequest.POST("/messages", """
            {
                "metadata": {
                    "channel": "abc123",
                    "messageNumber": 2,
                    "messageTime": "2022-02-03T19:39:05.86337+01:00",
                    "messageType": "RocketExploded"
                },
                "message": {
                    "reason": "PRESSURE_VESSEL_FAILURE"
                }
            }
            """);
        var responseExplode = client.exchange(postExplode);
        assertEquals(200, responseExplode.code());

        responseRockets = client.exchange("/rockets");
        assertEquals(200, responseRockets.code());
        var rocketList = readResponse(responseRockets);
        var rocket = rocketList.getFirst();
        assertEquals("PRESSURE_VESSEL_FAILURE", rocket.getStatus());
    }

    @Test
    void defaultSortByMission() {
        client.exchange(HttpRequest.POST("/messages", rocketLaunchMessage("abc123", 500, "ARTEMIS", "Falcon-9")));
        client.exchange(HttpRequest.POST("/messages", rocketLaunchMessage("abc456", 500, "VOYAGER", "Falcon-9")));

        var rocketListNoSort = readResponse(client.exchange("/rockets"));
        var rocketListSortMission = readResponse(client.exchange("/rockets?sortBy=mission"));
        var rocketListSortDescMission = readResponse(client.exchange("/rockets?sortBy=mission&orderBy=desc"));

        // default sort by mission
        assertEquals("ARTEMIS", rocketListNoSort.getFirst().getMission());
        assertEquals("VOYAGER", rocketListNoSort.getLast().getMission());
        assertEquals(rocketListNoSort.getFirst(), rocketListSortMission.getFirst());

        // default asc, desc is opposite
        assertEquals("VOYAGER", rocketListSortDescMission.getFirst().getMission());
        assertEquals("ARTEMIS", rocketListSortDescMission.getLast().getMission());
    }

    @Test
    void sortByOtherProperties() {
        client.exchange(HttpRequest.POST("/messages", rocketLaunchMessage("abc123", 500, "ARTEMIS", "Falcon-9")));
        client.exchange(HttpRequest.POST("/messages", rocketLaunchMessage("abc456", 1000, "VOYAGER", "Falcon-9")));

        var rocketListBySpeedDesc = readResponse(client.exchange("/rockets?sortBy=speed&orderBy=desc"));
        var rocketListBySpeedAsc = readResponse(client.exchange("/rockets?sortBy=speed"));

        assertEquals(1000, rocketListBySpeedDesc.getFirst().getSpeed());
        assertEquals(500, rocketListBySpeedAsc.getFirst().getSpeed());
    }

    @Test
    void rocketsByType() {
        client.exchange(HttpRequest.POST("/messages", rocketLaunchMessage("abc123", 500, "ARTEMIS", "Falcon-9")));
        client.exchange(HttpRequest.POST("/messages", rocketLaunchMessage("abc456", 500, "ARTEMIS", "Falcon-9")));
        client.exchange(HttpRequest.POST("/messages", rocketLaunchMessage("xyz123", 1000, "VOYAGER", "Titan-IV")));
        client.exchange(HttpRequest.POST("/messages", rocketLaunchMessage("xyz456", 1000, "VOYAGER", "Juno-I")));


        var rocketTypes = readResponse(client.exchange("/rockets/types"), new TypeReference<List<String>>() {});
        assertEquals(3, rocketTypes.size());
        assertTrue(rocketTypes.containsAll(List.of("Falcon-9", "Titan-IV", "Juno-I")));

        assertEquals(2, readResponse(client.exchange("/rockets?type=Falcon-9")).size());
        assertEquals(1, readResponse(client.exchange("/rockets?type=Titan-IV")).size());
        assertEquals(1, readResponse(client.exchange("/rockets?type=Juno-I")).size());
    }

    private static String rocketLaunchMessage(String id, int speed, String mission, String type) {
        return """
            {
                "metadata": {
                    "channel": "%s",
                    "messageNumber": 1,
                    "messageTime": "2022-02-02T19:39:05.86337+01:00",
                    "messageType": "RocketLaunched"
                },
                "message": {
                    "type": "%s",
                    "launchSpeed": %s,
                    "mission": "%s"
                }
            }
            """.formatted(id, type, speed, mission);
    }

    private static List<Rocket> readResponse(HttpResponse<?> httpResponse) {
        return readResponse(httpResponse, new TypeReference<>() {});
    }

    private static <T> List<T> readResponse(HttpResponse<?> httpResponse, TypeReference<List<T>> typeRef) {
        try {
            return App.objectMapper.readValue(httpResponse.getBody(String.class).get(), typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
