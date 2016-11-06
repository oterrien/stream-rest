package com.ote.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/entities")
public class ServerController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ConcurrentMap<UUID, Map<Integer, List<Entity>>> map = new ConcurrentHashMap<>();

    @Traceable
    @RequestMapping(method = RequestMethod.POST, value = "/start")
    public int start(@RequestParam(value = "correlationId") String correlationId, @RequestParam(value = "pageSize") int pageSize) {

        UUID uuid = UUID.fromString(correlationId);
        return getAll(uuid, pageSize).size();
    }

    @Traceable
    @RequestMapping(method = RequestMethod.POST, value = "/finish")
    public void finish(@RequestParam(value = "correlationId") String correlationId) {

        UUID uuid = UUID.fromString(correlationId);
        map.remove(uuid);
    }

    @Traceable
    @RequestMapping(method = RequestMethod.GET)
    public Entity[] getAllByPage(@RequestParam(value = "correlationId") String correlationId, @RequestParam(value = "pageIndex") int pageIndex) {

        UUID uuid = UUID.fromString(correlationId);
        Optional<Map<Integer, List<Entity>>> group = Optional.ofNullable(map.get(uuid));

        return group.
                map(map -> map.get(pageIndex)).
                map(entities -> entities.toArray(new Entity[entities.size()])).
                orElseThrow(() -> new RuntimeException(new HttpException("no entity exists for correlationId = " + correlationId)));
    }

    @Data
    @AllArgsConstructor
    private static class EntityGroup {

        private Entity entity;
        private Integer pageIndex;
    }

    private Map<Integer, List<Entity>> getAll(UUID correlationId, int pageSize) {

        Map<Integer, List<Entity>> splitList = map.get(correlationId);

        if (splitList == null) {
            splitList = split(getAll(), pageSize);
            save(correlationId, splitList);
        }

        return splitList;
    }

    private void save(UUID correlationId, Map<Integer, List<Entity>> splitList) {
        map.put(correlationId, splitList);
    }

    private Map<Integer, List<Entity>> split(List<Entity> entities, int pageSize) {

        AtomicInteger count = new AtomicInteger(0);
        return entities.
                stream().
                map(e -> new EntityGroup(e, count.incrementAndGet() / pageSize)).
                collect(Collectors.groupingBy(EntityGroup::getPageIndex, Collectors.mapping(EntityGroup::getEntity, Collectors.toList())));
    }

    private List<Entity> getAll() {

        return IntStream.range(0, 1000000).
                mapToObj(i -> new Entity(i, "name_" + i, "decr_" + i)).
                collect(Collectors.toList());
    }

    @Traceable
    @RequestMapping(method = RequestMethod.GET, value = "/{id}")
    public Entity getById(@PathVariable int id) throws Exception {

        long waitTime = Math.abs(new Random().nextInt(1000) * 5);
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Calculation for id=%d : expected wait time : %d ms", id, waitTime));
        }
        Thread.sleep(waitTime);
        return new Entity(id, "name_" + id, "descr_" + id);
    }
}