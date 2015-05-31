package com._8x8.cloud.hss.resource;

import com._8x8.cloud.hss.model.StreamStatus;
import com._8x8.cloud.hss.service.IStreamService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.regex.Pattern;

// TODO [kog@epiphanic.org - 5/28/15]: Clean up stream handling when filters are in place.

/**
 * Provides a resource for dealing with the "streams" - the middle S in HSS. You must not cross them.<p/>
 *
 * Please note that where an ID is used, valid characters are as follows:
 *
 * <ul>
 *      <li>An alpha-numeric character ([0-9a-zA-Z])</li>
 *      <li>One of the following characters: !, -, _, ., *, ', (, and )</li>
 * </ul><p/>
 *
 * These characters are taken from the Amazon S3 guide.<p/>
 *
 * Please note that this is a naive implementation, and is missing things you'd expect if you were actually solving this
 * problem in a production environment. The goal here is to solve the problem with a good balance between not being crap
 * code, and not taking too long to do so...<p/>
 *
 * You'd usually expect to see things like proper exception handling, localization, pagination (for multi-status), maybe
 * E-tag support for some of the operations, auth/auth etc.
 *
 * @author kog@epiphanic.org
 * @since 05/28/2015
 */
@Path("streams")
public class StreamResource
{
    /**
     * Holds our collaborating {@link IStreamService}, which will do most of the heavy lifting. This is unfortunately
     * autowired because that is how the Jersey 2.x Spring support works.
     */
    @Autowired
    private IStreamService _streamService;

    /**
     * Holds the regex {@link Pattern} we use to validate ID strings.
     */
    private static final Pattern VALID_ID_PATTERN = Pattern.compile("^[\\w!\\-_\\.\\*'\\(\\)]+$");

    /**
     * Gets the {@link IStreamService} to use for servicing our streams.
     *
     * @return A non-null, valid and fully wired {@link IStreamService} for stream handling.
     */
    public IStreamService getStreamService()
    {
        return _streamService;
    }

    /**
     * Sets the {@link IStreamService} to use for servicing our streams.
     *
     * @param streamService A non-null, valid and fully wired {@link IStreamService} for stream handling.
     */
    public void setStreamService(IStreamService streamService)
    {
        _streamService = streamService;
    }

    /**
     * Attempts to gets a stream by a given ID.
     *
     * @param id The ID to use for the stream. Must not be blank, must be valid.
     *
     * @return 200/OK with the stream if known,
     *         403/FORBIDDEN if the ID is invalid,
     *         404/NOT FOUND if the id is valid but unknown.
     */
    @Path("/{id}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getStreamById(final @PathParam("id") String id) throws Exception
    {
        validateId(id);

        final InputStream file = getStreamService().getStreamById(id);

        if (null != file)
        {
            // It may seem strange to return a naked stream, but the MessageBodyWriter (InputStreamProvider) will call close on this.
            return Response.ok(file).build();
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Attempts to create a stream for a given ID. Please note that this is not an upsert call, and if a stream already
     * exists for a given ID, a 409/CONFLICT will be returned. Please see {@link #updateStream(String, InputStream)} for updates.
     *
     * @param uriInfo Passed by Jersey, allows us to create our Location header. Must not be null.
     * @param id The ID to use for the stream. Must not be blank, must be valid.
     * @param stream The stream to store. Must not be null or empty.
     *
     * @return 201/CREATED with a Location header pointing to the new resource if creation was successful,
     *         403/FORBIDDEN if the ID is invalid,
     *         409/CONFLICT if the ID is valid but already in use.
     */
    @Path("/{id}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response createStream(final @Context UriInfo uriInfo, final @PathParam("id") String id, final InputStream stream) throws Exception
    {
        try
        {
            validateId(id);

            if (!StreamStatus.NOT_FOUND.equals(getStreamService().getStatusForStreamById(id)))
            {
                return Response.status(Response.Status.CONFLICT).build();
            }

            getStreamService().saveStream(id, stream);
        }
        finally
        {
            IOUtils.closeQuietly(stream);
        }

        return Response.created(uriInfo.getAbsolutePath()).build();
    }

    /**
     * Attempts to update a stream for a given ID, using the given filters. Please note that if a stream for a given ID
     * does not already exists, an error will be thrown. If you wish to create the stream for the ID, please call {@link #createStream(UriInfo, String, InputStream)}.<p/>
     *
     * @param id The ID to use for the stream. Must not be blank, must be valid.
     * @param stream The stream to store. Must not be null or empty.
     *
     * @return 204/NO CONTENT if the update was successful,
     *         403/FORBIDDEN if the ID is invalid,
     *         404/NOT FOUND if the ID is valid but not known.
     */
    @Path("/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response updateStream(final @PathParam("id") String id, final InputStream stream) throws Exception
    {
        try
        {
            validateId(id);

            if (StreamStatus.NOT_FOUND.equals(getStreamService().getStatusForStreamById(id)))
            {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            getStreamService().saveStream(id, stream);
        }
        finally
        {
            IOUtils.closeQuietly(stream);
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    // TODO [kog@epiphanic.org - 5/28/15]: Move this to a @Constraint.

    /**
     * Provides a convenience method to check if an ID is valid. For now will throw a 403/FORBIDDEN if violated.
     *
     * @param id An ID to validate. Must not be blank.
     */
    void validateId(final String id)
    {
        if (!VALID_ID_PATTERN.matcher(id).matches())
        {
            throw new WebApplicationException(403);
        }
    }
}
