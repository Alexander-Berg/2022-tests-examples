package ru.yandex.qe.bus.exception;

import javax.annotation.Nonnull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import ru.yandex.qe.bus.MediaTypeConstants;

/**
 * @author rurikk
 */
@Path("/exceptions")
@Produces({MediaTypeConstants.APPLICATION_JSON_WITH_UTF})
@Consumes({MediaTypeConstants.APPLICATION_JSON_WITH_UTF})
public interface ExceptionApi {
    @GET
    @Path("/throwing")
    @Nonnull
    String throwing(@QueryParam("exceptionClass") String exceptionClass) throws AppException;

}
