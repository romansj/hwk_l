package org.home.work;

import org.home.work.messages.MessageController;

import java.time.ZonedDateTime;
import java.util.Map;

public class Fixture {
    public static MessageController.RocketTelemetry changeSpeed(int messageNo, String channel, int by) {
        var type = by < 0 ? "RocketSpeedDecreased" : "RocketSpeedIncreased";
        return new MessageController.RocketTelemetry(
            new MessageController.Metadata(channel, messageNo, type, ZonedDateTime.now()),
            Map.of("by", String.valueOf(Math.abs(by)))
        );
    }

    public static MessageController.RocketTelemetry launch(String channel, int speed) {
        return new MessageController.RocketTelemetry(
            new MessageController.Metadata(channel, 1, "RocketLaunched", ZonedDateTime.now()),
            Map.of(
                "type", "Falcon-9",
                "launchSpeed", String.valueOf(speed),
                "mission", "ARTEMIS"
            )
        );
    }
}
