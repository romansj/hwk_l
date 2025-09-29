package org.home.work;

import org.home.work.messages.MessageController;
import org.home.work.rockets.RocketBuffer;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.home.work.Fixture.changeSpeed;
import static org.home.work.Fixture.launch;
import static org.junit.jupiter.api.Assertions.*;

class RocketBufferTest {
    @Test
    void rocketInitialStateSet() {
        var buffer = new RocketBuffer();
        buffer.processTelemetry(launch("123abc", 500));
        assertEquals(500, buffer.getRocket().getSpeed());
        assertEquals("LAUNCHED", buffer.getRocket().getStatus());
    }

    @Test
    void speedUpdated() {
        var buffer = new RocketBuffer();
        buffer.processTelemetry(launch("123abc", 500));
        buffer.processTelemetry(changeSpeed(2, "123abc", 3000));

        var rocket = buffer.getRocket();
        assertEquals(2, rocket.getLastMessageNumber());
        assertEquals(3500, rocket.getSpeed());
    }

    @Test
    void oldDataIgnored() {
        var buffer = new RocketBuffer();
        buffer.processTelemetry(launch("123abc", 500));
        buffer.processTelemetry(changeSpeed(2, "123abc", 3000));
        buffer.processTelemetry(changeSpeed(2, "123abc", 3000));

        var rocket = buffer.getRocket();
        assertEquals(2, rocket.getLastMessageNumber());
        assertEquals(3500, rocket.getSpeed()); // 500+3000+0
    }

    // this of course becomes problematic if there is a long waiting time. e.g. 300 message backlog, waiting for 1
    // "receive missing" timeout could be an option
    @Test
    void outOfOrderUpdateDelayed() {
        var buffer = new RocketBuffer();
        buffer.processTelemetry(launch("123abc", 500));
        buffer.processTelemetry(changeSpeed(2, "123abc", 3000));
        // message from the future
        buffer.processTelemetry(changeSpeed(4, "123abc", -5000));

        var rocket = buffer.getRocket();

        assertEquals(2, rocket.getLastMessageNumber());
        assertEquals(3500, rocket.getSpeed()); // 500+3000 -0

        // the message we've been waiting for
        buffer.processTelemetry(changeSpeed(3, "123abc", 2000));

        assertEquals(4, rocket.getLastMessageNumber());
        assertEquals(500, rocket.getSpeed()); // 500+3000+2000 -5000 = 500
    }

    @Test
    void explodedNotActiveAnymore() {
        var buffer = new RocketBuffer();
        buffer.processTelemetry(launch("123abc", 500));
        buffer.processTelemetry(new MessageController.RocketTelemetry(
            new MessageController.Metadata("123abc", 2, "RocketExploded", ZonedDateTime.now()),
            Map.of("reason", "PRESSURE_VESSEL_FAILURE")
        ));

        var rocket = buffer.getRocket();
        assertEquals("PRESSURE_VESSEL_FAILURE", rocket.getStatus());
    }

    @Test
    void unknownMessageTypeIgnored() {
        var buffer = new RocketBuffer();
        buffer.processTelemetry(launch("123abc", 500));
        buffer.processTelemetry(new MessageController.RocketTelemetry(
            new MessageController.Metadata("123abc", 2, "RocketFlewIntoSun", ZonedDateTime.now()),
            Map.of("temperature", "15000000C")
        ));

        var rocket = buffer.getRocket();
        assertEquals("LAUNCHED", rocket.getStatus());
        assertEquals(500, rocket.getSpeed());
        assertEquals(1, rocket.getLastMessageNumber());
    }


    @Test
    void concurrentDuplicateIgnored() throws InterruptedException {
        var buffer = new RocketBuffer();
        buffer.processTelemetry(launch("123abc", 1000));


        var cld = new CountDownLatch(2);

        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var increase4K = CompletableFuture.runAsync(() -> {
            buffer.processTelemetry(changeSpeed(2, "123abc", 4000));
            cld.countDown();
        }, executor);

        var increase4KDup = CompletableFuture.runAsync(() -> {
            buffer.processTelemetry(changeSpeed(2, "123abc", 4000));
            cld.countDown();
        }, executor);

        CompletableFuture.allOf(increase4K, increase4KDup).join();
        cld.await();

        var rocket = buffer.getRocket();
        assertEquals(5000, rocket.getSpeed());
    }

    @Test
    void concurrentUpdatesExecutedInOrder() throws InterruptedException {
        var buffer = new RocketBuffer();
        buffer.processTelemetry(launch("123abc", 1000));

        var cld = new CountDownLatch(2);

        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var increase4K = CompletableFuture.runAsync(() -> {
            buffer.processTelemetry(changeSpeed(2, "123abc", 4000));
            cld.countDown();
        }, executor);

        var decrease5K = CompletableFuture.runAsync(() -> {
            buffer.processTelemetry(changeSpeed(3, "123abc", -5000));
            cld.countDown();
        }, executor);

        CompletableFuture.allOf(increase4K, decrease5K).join();
        cld.await();

        var rocket = buffer.getRocket();
        assertEquals(0, rocket.getSpeed());
    }
}