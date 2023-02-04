package ru.yandex.qe.bus.api;

import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import ru.yandex.qe.bus.MediaTypeConstants;

@Path("/test")
@Produces({MediaTypeConstants.APPLICATION_JSON_WITH_UTF})
@Consumes({MediaTypeConstants.APPLICATION_JSON_WITH_UTF})
public interface ApiService {

    @GET
    @Path("/x/{login}")
    @Nonnull
    String getLogin(@PathParam("login") String login);

    @GET
    @Path("/streaming_return")
    @Nonnull
    Iterator<ApiJsonObject> getLotOfObjects(@QueryParam("count") long numberOfObjects);

    @PUT
    @Path("/streaming_put")
    int putLotOfObject(Iterator<ApiJsonObject> iterator);

    @PUT
    @Path("/streaming_transform")
    Iterator<String> transformLofOfObjects(Iterator<String> object);
}
