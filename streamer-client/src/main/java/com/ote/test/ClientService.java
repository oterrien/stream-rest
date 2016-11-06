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
    public Entity getOne(int id) {
        return remoteEntityService.getOne(id);
    }

    @Traceable
    public List<Entity> getAll(int pageSize) throws Exception {

        try (Batch batch = new Batch(pageSize)) {
            return batch.getAll();
        }
    }

    private class Batch implements AutoCloseable {

        protected final Logger logger = LoggerFactory.getLogger(this.getClass());

        protected final UUID correlationId = UUID.randomUUID();
        protected int numberOfPage;

        public Batch(int pageSize) {
            long start = System.currentTimeMillis();
            try {
                numberOfPage = remoteEntityService.startGetAll(correlationId, pageSize);
            } finally {
                logger.info("start : the result has been split into " + numberOfPage + " pages");
                logger.info("start : Elapsed time: " + (System.currentTimeMillis() - start) + "ms");
            }
        }

        public List<Entity> getAll() {
            long start = System.currentTimeMillis();
            try {
                return IntStream.range(0, numberOfPage).
                        parallel().
                        mapToObj(pageIndex -> remoteEntityService.getAll(correlationId, pageIndex)).
                        flatMap(Stream::of).
                        collect(Collectors.toList());
            } finally {
                logger.info("getAll : Elapsed time: " + (System.currentTimeMillis() - start) + "ms");
            }
        }

        @Override
        public void close() throws Exception {
            remoteEntityService.finishGetAll(correlationId);
        }
    }
}