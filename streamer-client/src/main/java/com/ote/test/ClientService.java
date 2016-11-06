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

    public Batch createProcessorByBatch(int pageSize) {
        return new Batch(pageSize);
    }

    public class Batch implements AutoCloseable {

        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        private final UUID correlationId = UUID.randomUUID();
        private int numberOfPage;

        public Batch(int pageSize) {
            numberOfPage = remoteEntityService.startGetAll(correlationId, pageSize);
        }

        public List<Entity> getAll() {
            long start = System.currentTimeMillis();
            try {
                return IntStream.range(0, numberOfPage).
                        mapToObj(this::getAll).
                        parallel().
                        flatMap(this::get).
                        peek(entity -> logger.trace(entity.toString())).
                        collect(Collectors.toList());
            } finally {
                logger.info("Elapsed time: " + (System.currentTimeMillis() - start));
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
                return Stream.of(future.get());
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