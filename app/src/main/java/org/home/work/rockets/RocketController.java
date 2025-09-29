package org.home.work.rockets;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Slf4j
@Controller("/rockets")
public class RocketController {

    @Inject
    private RocketRepository repository;

    /**
     * Query params -- If nothing is passed or it's an unknown sort key, use default: sort by mission ascending
     *
     * @param sortBy  Sort results by given property
     * @param orderBy Order results in ascending or descending order
     * @return Sorted rocket list
     */
    @Get
    public HttpResponse<List<Rocket>> data(
        @QueryValue Optional<String> sortBy,
        @QueryValue(defaultValue = "asc") Optional<String> orderBy,
        @QueryValue Optional<String> type
    ) {
        var rocketList = repository.rocketsBy(rocket ->
            type
                .map(t -> t.equalsIgnoreCase(rocket.getType()))
                .orElse(true)); // or all of them

        var comparator = sortBy.map(s -> switch (s) {
            case "type" -> Comparator.comparing(Rocket::getType);
            case "speed" -> Comparator.comparing(Rocket::getSpeed);
            case "status" -> Comparator.comparing(Rocket::getStatus);
            case "launchTime" -> Comparator.comparing(Rocket::getLaunchTime);
            case "endTime" -> Comparator.comparing(Rocket::getMissionEndTime);
            default -> Comparator.comparing(Rocket::getMission);
        }).orElse(Comparator.comparing(Rocket::getMission));

        rocketList.sort(comparator);

        if (orderBy.isPresent() && "desc".equalsIgnoreCase(orderBy.get())) {
            rocketList = rocketList.reversed();
        }

        return HttpResponse.ok(rocketList);
    }

    @Get("/{rocketChannel}")
    public HttpResponse<Rocket> rocketById(@PathVariable String rocketChannel) {
        var rocketData = repository.rocketById(rocketChannel);
        return rocketData
            .map(HttpResponse::ok)
            .orElse(HttpResponse.notFound());
    }

    @Get("/types")
    public HttpResponse<Set<String>> rocketTypes() {
        return HttpResponse.ok(repository.rocketTypes());
    }
}
