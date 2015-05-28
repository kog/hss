package com._8x8.cloud.hss.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Provides a resource for dealing with the "streams" - the middle S in HSS.
 *
 * @author kog@epiphanic.org
 * @since 05/28/2015
 */
@Path("streams")
public class StreamResource
{
    // TODO [kog@epiphanic.org - 5/28/15]: Unstub.
    @GET
    public Response ping()
    {
        return Response.ok("pong").build();
    }
}
