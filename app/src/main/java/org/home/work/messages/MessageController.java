package org.home.work.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.home.work.App;
import org.home.work.rockets.RocketRepository;

import java.time.ZonedDateTime;
import java.util.Map;

@Slf4j
@Controller("/messages") // rest semantics
public class MessageController {

    @Inject
    private RocketRepository repository;

    public record Metadata(
        String channel,
        int messageNumber,
        String messageType,
        ZonedDateTime messageTime
    ) {}

    public record RocketTelemetry(
        Metadata metadata,
        Map<String, String> message
    ) {}

    @Post
    HttpResponse<String> receiveMessage(@Body String json) {
        try {
            var telemetry = App.objectMapper.readValue(json, RocketTelemetry.class);
            repository.processTelemetry(telemetry);

        } catch (JsonProcessingException e) {
            log.error("Json processing exception", e);
            return HttpResponse.badRequest("json processing exception: %s".formatted(e.getMessage()));
        }

        return HttpResponse.ok();
    }


}
