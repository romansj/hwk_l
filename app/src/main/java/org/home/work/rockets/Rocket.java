package org.home.work.rockets;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.home.work.messages.MessageController;
import org.home.work.messages.MessageType;

import java.time.ZonedDateTime;

@Slf4j
@Data
@Serdeable
public class Rocket {
    private String id;
    private String type;
    private int speed;
    private String mission;
    private ZonedDateTime launchTime;
    private int lastMessageNumber;

    private String status;
    private ZonedDateTime missionEndTime;

    public synchronized void update(MessageController.RocketTelemetry telemetry) {
        var message = telemetry.message();
        var type = MessageType.ofStr(telemetry.metadata().messageType());

        switch (type) {
            case LAUNCHED -> {
                this.id = telemetry.metadata().channel();
                this.type = message.get("type");
                this.speed = Integer.parseInt(message.get("launchSpeed"));
                this.mission = message.get("mission");
                this.launchTime = telemetry.metadata().messageTime();

                this.status = "LAUNCHED";

                log.info("\uD83D\uDE80 Rocket {} launched", this.id);
            }

            case SPEED_INCREASED -> {
                this.speed += Integer.parseInt(message.get("by"));
            }
            case SPEED_DECREASED -> {
                this.speed -= Integer.parseInt(message.get("by"));
            }
            case EXPLODED -> {
                log.warn("\uD83D\uDCA5 Rocket {} exploded", id);

                this.status = message.get("reason");
                this.missionEndTime = telemetry.metadata().messageTime();
            }
            case MISSION_CHANGED -> {
                this.mission = message.get("newMission");
            }
            case null, default -> {
                return;
            }
        }

        this.lastMessageNumber++;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }
}
