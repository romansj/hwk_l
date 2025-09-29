package org.home.work.rockets;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.home.work.messages.MessageController;

import java.util.Comparator;
import java.util.PriorityQueue;

@Slf4j
@Getter
public class RocketBuffer {
    private final PriorityQueue<MessageController.RocketTelemetry> queue = new PriorityQueue<>(Comparator.comparingInt(telemetry -> telemetry.metadata().messageNumber()));
    private final Rocket rocket = new Rocket();

    /**
     * Read message number and compare against last processed message number. If smaller or equal, then it's an old/duplicate message.
     * If the number is greater by 1, then the message is next in sequence.
     * More than 1 and we are missing a message, so that goes in the queue until we receive it.
     *
     * @param telemetry Received rocket telemetry
     */
    public synchronized void processTelemetry(MessageController.RocketTelemetry telemetry) {
        var messageNumber = telemetry.metadata().messageNumber();

        if (messageNumber <= rocket.getLastMessageNumber()) {
            log.warn("Ignoring message #{}, already received", messageNumber);
            return;
        }

        if (telemetry.metadata().messageNumber() == rocket.getLastMessageNumber() + 1) {
            rocket.update(telemetry);

            processQueuedMessages();

        } else {
            // missing an update (have 10, got 12, missing 11)
            log.warn("Missing an update: last #{}, adding #{} to queue", rocket.getLastMessageNumber(), messageNumber);
            queue.add(telemetry);
        }

    }

    private void processQueuedMessages() {
        while (!queue.isEmpty() && queue.peek().metadata().messageNumber() == rocket.getLastMessageNumber() + 1) { // reuse
            var rocketTelemetry = queue.poll();
            rocket.update(rocketTelemetry);
        }
    }

}
