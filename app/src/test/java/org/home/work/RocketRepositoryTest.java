package org.home.work;

import org.home.work.rockets.RocketRepository;
import org.home.work.rockets.Rocket;
import org.home.work.rockets.RocketBuffer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.home.work.Fixture.launch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketRepositoryTest {

    @Test
    void sameChannelReturnsSameBuffer() {
        var repository = new RocketRepository();
        var rocketBufferA = repository.processTelemetry(launch("123abc", 500));
        var rocketBufferB = repository.processTelemetry(launch("123abc", 500));
        assertEquals(rocketBufferA, rocketBufferB);
    }

    @Test
    void sameChannelReturnsSameBufferConcurrent() throws InterruptedException {
        var repository = new RocketRepository();

        var threadCount = 5;
        var cld = new CountDownLatch(threadCount);

        List<RocketBuffer> bufferList = new ArrayList<>();
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        var futures = IntStream.range(0, threadCount)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                var rocketBuffer = repository.getRocketBuffer(launch("123abc", 500));
                cld.countDown();
                return rocketBuffer;
            }, executor).thenAccept(bufferList::add))
            .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();

        cld.await();

        var first = bufferList.getFirst();
        for (var rocketBuffer : bufferList) {
            assertEquals(first, rocketBuffer);
        }
    }

    @Test
    void dataStored() {
        var repository = new RocketRepository();
        assertTrue(repository.rocketById("123abc").isEmpty());
        repository.processTelemetry(launch("123abc", 500));
        assertTrue(repository.rocketById("123abc").isPresent());
    }

    @Test
    void canFindRocketByPredicate() {
        var repository = new RocketRepository();
        repository.processTelemetry(launch("qwerty", 1000));
        repository.processTelemetry(launch("abc", 2000));
        repository.processTelemetry(launch("xyz", 3000));

        var rocketList = repository.rocketsBy(rocket -> rocket.getSpeed() >= 2000);
        var idList = rocketList.stream().map(Rocket::getId).toList();
        assertEquals(2, idList.size());
        assertTrue(idList.containsAll(List.of("abc", "xyz")));
    }
}