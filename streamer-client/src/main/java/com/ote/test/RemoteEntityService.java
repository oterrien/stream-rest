package com.ote.test;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

@Service
public class RemoteEntityService {

    private static final String URL = "http://localhost:8082/entities";

    private final RestTemplate restTemplate = new RestTemplate();

    //region GET ONE
    @Async
    public Future<Entity> getOneAsync(int id) throws InterruptedException {
        return new AsyncResult<>(getOne(id));
    }

    public Entity getOne(long id) {
        return restTemplate.getForObject(URL + "/" + id, Entity.class);
    }
    //endregion

    //region GET ALL
    @Traceable
    public int startGetAll(UUID correlationId, int pageSize) {

        Map<String, String> vars = new HashMap<>();
        vars.put("correlationId", correlationId.toString());
        vars.put("pageSize", Integer.toString(pageSize));

        String url = URL + "/start" + "?correlationId=${correlationId}&pageSize=${pageSize}";
        url = StrSubstitutor.replace(url, vars);

        return restTemplate.postForObject(url, null, Integer.class);
    }

    @Traceable
    public Entity[] getAll(UUID correlationId, int pageNumber) {

        Map<String, String> vars = new HashMap<>();
        vars.put("correlationId", correlationId.toString());
        vars.put("pageIndex", Integer.toString(pageNumber));

        String url = URL + "?correlationId=${correlationId}&pageIndex=${pageIndex}";
        url = StrSubstitutor.replace(url, vars);

        return restTemplate.getForObject(url, Entity[].class);
    }

    @Traceable
    public void finishGetAll(UUID correlationId) {

        Map<String, String> vars = new HashMap<>();
        vars.put("correlationId", correlationId.toString());

        String url = URL + "/finish" + "?correlationId=${correlationId}";
        url = StrSubstitutor.replace(url, vars);

        restTemplate.postForObject(url, null, Void.class);
    }
    //endregion
}
