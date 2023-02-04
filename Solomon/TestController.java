package ru.yandex.solomon.experiments.jamel;

import java.util.concurrent.CompletableFuture;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author Sergey Polovko
 */
@Controller
public class TestController {

    @PostMapping(value = "/query", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public CompletableFuture<ResponseEntity<String>> query(ServerWebExchange request) {
        var future = new CompletableFuture<ResponseEntity<String>>();
        request.getFormData().subscribe(data -> {
            future.complete(ResponseEntity.ok(data.toString()));
        });
        return future;
    }

    @PostMapping(value = "/query2", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public CompletableFuture<ResponseEntity<String>> query2(@RequestBody MultiValueMap<String, String> formData) {
        return CompletableFuture.completedFuture(ResponseEntity.ok(formData.toString()));
    }
}
