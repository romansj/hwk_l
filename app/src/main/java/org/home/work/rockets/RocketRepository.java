package org.home.work.rockets;

import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.home.work.messages.MessageController;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class RocketRepository {

    private final ConcurrentHashMap<String, RocketBuffer> bufferMap = new ConcurrentHashMap<>();

    /**
     * Creates a rocket buffer if not yet created for rocket channel and forwards telemetry for processing
     *
     * @param telemetry Received rocket telemetry
     * @return Created rocket buffer
     */
    public RocketBuffer processTelemetry(MessageController.RocketTelemetry telemetry) {
        var rocketBuffer = getRocketBuffer(telemetry);
        rocketBuffer.processTelemetry(telemetry);

        return rocketBuffer;
    }

    public RocketBuffer getRocketBuffer(MessageController.RocketTelemetry telemetry) {
        return bufferMap.computeIfAbsent(telemetry.metadata().channel(), k -> new RocketBuffer());
    }

    public Optional<Rocket> rocketById(String id) {
        var rocketBuffer = bufferMap.get(id);
        if (rocketBuffer != null) {
            return Optional.of(rocketBuffer.getRocket());
        }

        return Optional.empty();
    }

    public List<Rocket> rocketsBy(Predicate<Rocket> predicate) {
        return bufferMap.values().stream()
            .map(RocketBuffer::getRocket)
            .filter(predicate).collect(Collectors.toCollection(ArrayList::new));
    }

    public Set<String> rocketTypes() {
        Set<String> types = new HashSet<>();
        for (var entry : bufferMap.entrySet()) {
            var type = entry.getValue().getRocket().getType();
            types.add(type);
        }

        return types;
    }
}
