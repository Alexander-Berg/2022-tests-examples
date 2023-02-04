package ru.yandex.qe.test;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Service;

/**
 * @author lvovich
 */
@Service("releaseService")
@Path("/rex")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SampleService {

    @GET
    @Path("/hello")
    public String hello() {
        return "world";
    }
}
