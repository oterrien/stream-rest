package com.ote.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class ClientController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ClientService clientService;

    /**
     * example http://localhost:8081/1
     *
     * @param id
     * @throws Exception
     */
    @Traceable
    @RequestMapping(method = RequestMethod.POST, value = "/{id}")
    public void processOne(@PathVariable int id) throws Exception {

        long start = System.currentTimeMillis();

        CompletableFuture.
                supplyAsync(() -> clientService.getOneAsync(id)).
                thenAccept(entityFuture -> {
                    try {
                        logger.info("--> " + entityFuture.get());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        logger.info("Elapsed time: " + (System.currentTimeMillis() - start));
                    }
                });
    }

    /**
     * example http://localhost:8081/many?ids=1,2
     *
     * @param ids
     * @throws Exception
     */
    @Traceable
    @RequestMapping(method = RequestMethod.POST, value = "/many")
    public void processMany(@RequestParam(value = "ids", required = true) int[] ids) throws Exception {

        long start = System.currentTimeMillis();

        CompletableFuture<Void> get1 = CompletableFuture.
                supplyAsync(() -> this.getOne(ids[0])).
                thenAccept(entity -> logger.info("--> " + entity));

        CompletableFuture<Void> get2 = CompletableFuture.
                supplyAsync(() -> this.getOne(ids[1])).
                thenAccept(entity -> logger.info("--> " + entity));

        get1.join();
        get2.join();

        logger.info("Elapsed time: " + (System.currentTimeMillis() - start));
    }

    private Entity getOne(int id) {
        try {
            return clientService.getOneAsync(id).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * example http://localhost:8081?pageSize=50
     *
     * @param pageSize
     * @throws Exception
     */
    @Traceable
    @RequestMapping(method = RequestMethod.POST)
    public void processAll(@RequestParam(value = "pageSize", required = true) int pageSize) throws Exception {

        AtomicInteger count = new AtomicInteger(0);
        clientService.
                getAll(pageSize).
                stream().
                peek(entity -> count.incrementAndGet()).
                count();
                //forEach(entity -> logger.trace("--> " + entity));

        logger.info("Number of processed element : " + count.get());
    }


}
