package com.ote.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class ClientService {

    @Autowired
    private RemoteEntityService remoteEntityService;

    @Traceable
    @Async
    public Future<Entity> getOneAsync(int id) {

        try {
            return remoteEntityService.getOneAsync(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Traceable
    public List<Entity> getAll(int pageSize) throws Exception {

        try (Batch batch = new Batch(pageSize)) {
            return batch.getAll();
        }
    }

    private class Batch implements AutoCloseable {

        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        private final UUID correlationId = UUID.randomUUID();
        private int numberOfPage;

        public Batch(int pageSize) {
            long start = System.currentTimeMillis();
            try {
                numberOfPage = remoteEntityService.startGetAll(correlationId, pageSize);
            } finally {
                logger.info("start : Elapsed time: " + (System.currentTimeMillis() - start) + "ms");
            }
        }

        public List<Entity> getAll() {
            long start = System.currentTimeMillis();
            try {
                return IntStream.range(0, numberOfPage).
                        parallel().
                        mapToObj(this::getAll).
                        flatMap(this::get).
                        collect(Collectors.toList());
            } finally {
                logger.info("getAll : Elapsed time: " + (System.currentTimeMillis() - start) + "ms");
            }
        }

        @Async
        private Future<Entity[]> getAll(int pageIndex) {
            try {
                return remoteEntityService.getAllAsync(correlationId, pageIndex);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private Stream<Entity> get(Future<Entity[]> future) {
            try {
                Entity[] entities = future.get();
                //logger.info("Number of elements : " + entities.length);
                return Stream.of(entities);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        @Override
        public void close() throws Exception {
            remoteEntityService.finishGetAll(correlationId);
        }
    }

}