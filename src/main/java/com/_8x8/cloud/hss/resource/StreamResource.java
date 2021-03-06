package com._8x8.cloud.hss.resource;

import com._8x8.cloud.hss.model.StreamMetadata;
import com._8x8.cloud.hss.model.StreamMetadataCollection;
import com._8x8.cloud.hss.model.StreamStatus;
import com._8x8.cloud.hss.service.IStreamService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

// TODO [kog@epiphanic.org - 6/16/2015]: Looks like there's an issue between Swagger 1.3 and 2.x with allowableTypes.
// TODO [kog@epiphanic.org - 6/16/2015]: It's also why Swagger-UI keeps inserting extra 200/OK options (https://github.com/swagger-api/swagger-ui/issues/1266)

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
 * E-tag support for some of the operations, auth/auth etc.<p/>
 *
 * With respect to filters: please note that these are handled via a "soft failure:" if you ask for a filter that does
 * not exist, nothing will occur.
 *
 * @author kog@epiphanic.org
 * @since 05/28/2015
 */
@Api(value = "stream", description = "A resource for handling streams (application/octet-stream) and their metadata.", tags = {"streams"})
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

    // TODO [kog@epiphanic.org - 6/2/15]: Pagination, query criteria etc.

    /**
     * Returns a {@link com._8x8.cloud.hss.model.StreamMetadataCollection} with metadata for all known collections.
     *
     * @return A 200/OK with metadata regarding zero or more streams. May be empty, but never null.
     */
    @ApiOperation(value = "List all streams known to the system", response = StreamMetadataCollection.class)
    @GET
    public Response getStreamMetadata() throws Exception
    {
        final StreamMetadataCollection metadata = new StreamMetadataCollection();
        metadata.getMetadata().addAll(getStreamService().getMetadataForStreams());

        return Response.ok(metadata).build();
    }

    /**
     * Returns the {@link com._8x8.cloud.hss.model.StreamMetadata} associated with the stream ID, if the ID is valid.
     *
     * @param id The ID to use for the stream. Must not be blank, must be valid.
     *
     * @return 200/OK with the {@link com._8x8.cloud.hss.model.StreamMetadata},
     *         403/FORBIDDEN if the stream ID is invalid,
     *         404/NOT FOUND if the stream ID is unknown.
     */
    @ApiOperation(value = "Finds the metadata associated with a given stream, by ID.", response = StreamMetadata.class)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "If the stream ID is considered invalid."),
            @ApiResponse(code = 404, message = "If the stream ID is unknown to the system.")
    })
    @Path("/{id}/status")
    @GET
    public Response getStreamMetadataForId(@ApiParam(value = "ID of the stream to fetch", required = true) final @PathParam("id") String id) throws Exception
    {
        validateId(id);

        final StreamMetadata metadata = getStreamService().getMetadataForStreamById(id);

        if (StreamStatus.NOT_FOUND == metadata.getStatus())
        {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(metadata).build();
    }

    /**
     * Attempts to gets a stream by a given ID.
     *
     * @param id The ID to use for the stream. Must not be blank, must be valid.
     * @param filters A collection of zero or more filters to apply to the given stream. May be empty, but must not be null.
     *
     * @return 200/OK with the stream if known,
     *         403/FORBIDDEN if the ID is invalid,
     *         404/NOT FOUND if the ID is valid but unknown
     *         409/CONFLICT if the ID is known, but {@link StreamStatus#IN_PROGRESS} or {@link StreamStatus#FAILED}.
     */
    @ApiOperation(value = "Gets a stream, by ID. Please note that this is an application/octet-stream.", response = InputStream.class)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "If the stream ID is considered invalid."),
            @ApiResponse(code = 404, message = "If the stream ID is unknown to the system."),
            @ApiResponse(code = 409, message = "If the ID is known, but the stream is in progress or failed.")
    })
    @Path("/{id}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getStreamById(@ApiParam(value = "ID of the stream to fetch", required = true) final @PathParam("id") String id,
                                  @ApiParam(value = "A list of zero or more filters to apply to the stream, on the server side. May be empty.", required = false, allowableValues = "zip,encrypt,base64", allowMultiple = true)
                                    @QueryParam("filters") final List<String> filters) throws Exception
    {
        validateId(id);

        // Make sure that this is neither currently in use, nor failed.
        final StreamStatus status = getStreamService().getStatusForStreamById(id);

        if (StreamStatus.IN_PROGRESS.equals(status) || StreamStatus.FAILED.equals(status))
        {
            return Response.status(Response.Status.CONFLICT).build();
        }

        // If we've got it, return it.
        if (StreamStatus.SUCCESSFUL.equals(status))
        {
            // It may seem strange to return a naked stream, but the MessageBodyWriter (InputStreamProvider) will call close on this.
            // Note that any exceptions with the filters are caught at a lower level, and those streams are closed accordingly.
            return Response.ok(getStreamService().getStreamById(id, filters)).build();
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Attempts to create a stream for a given ID. Please note that this is not an upsert call, and if a stream already
     * exists for a given ID, a 409/CONFLICT will be returned. Please see {@link #updateStream(String, List, InputStream)} for updates.
     *
     * @param uriInfo Passed by Jersey, allows us to create our Location header. Must not be null.
     * @param id The ID to use for the stream. Must not be blank, must be valid.
     * @param filters A collection of zero or more filters to apply to the given stream. May be empty, but must not be null.
     * @param stream The stream to store. Must not be null or empty.
     *
     * @return 201/CREATED with a Location header pointing to the new resource if creation was successful,
     *         403/FORBIDDEN if the ID is invalid,
     *         409/CONFLICT if the ID is valid but already in use.
     */
    @ApiOperation(value = "Attempts to persist a given application/octet-stream in a create operation, with optionally applied filters.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "A created response, with a location header pointing to the new resource.",
                         responseHeaders = {@ResponseHeader(name = "Location", description = "URL to the newly created resource", response = String.class)}),
            @ApiResponse(code = 403, message = "If the stream ID is considered invalid."),
            @ApiResponse(code = 409, message = "If the ID is already in use.")
    })
    @Path("/{id}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response createStream(final @Context UriInfo uriInfo,
                                 @ApiParam(value = "ID of the stream to fetch", required = true) final @PathParam("id") String id,
                                 @ApiParam(value = "A list of zero or more filters to apply to the stream, on the server side. May be empty.", required = false, allowableValues = "zip,encrypt,base64", allowMultiple = true)
                                     @QueryParam("filters") final List<String> filters,
                                 @ApiParam(value = "An actual application/octet-stream of whatever object you'd like to store.", required = true) final InputStream stream) throws Exception
    {
        try
        {
            validateId(id);

            if (!StreamStatus.NOT_FOUND.equals(getStreamService().getStatusForStreamById(id)))
            {
                return Response.status(Response.Status.CONFLICT).build();
            }

            getStreamService().saveStream(id, stream, filters);
        }
        finally
        {
            IOUtils.closeQuietly(stream);
        }

        return Response.created(uriInfo.getAbsolutePath()).build();
    }

    /**
     * Attempts to update a stream for a given ID, using the given filters. Please note that if a stream for a given ID
     * does not already exists, an error will be thrown. If you wish to create the stream for the ID, please call {@link #createStream(UriInfo, String, List, InputStream)}.<p/>
     *
     * @param id The ID to use for the stream. Must not be blank, must be valid.
     * @param filters A collection of zero or more filters to apply to the given stream. May be empty, but must not be null.
     * @param stream The stream to store. Must not be null or empty.
     *
     * @return 204/NO CONTENT if the update was successful,
     *         403/FORBIDDEN if the ID is invalid,
     *         404/NOT FOUND if the ID is valid but not known,
     *         409/CONFLICT if the ID is known, but the stream is in a state such that it cannot be updated.
     */
    @ApiOperation(value = "Attempts to persist a given application/octet-stream in an update operation, with optionally applied filters.")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "No content if the update was successful."),
            @ApiResponse(code = 403, message = "If the stream ID is considered invalid."),
            @ApiResponse(code = 404, message = "If the ID is valid, but not known"),
            @ApiResponse(code = 409, message = "If the ID is already in use.")
    })
    @Path("/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response updateStream(@ApiParam(value = "ID of the stream to fetch", required = true) final @PathParam("id") String id,
                                 @ApiParam(value = "A list of zero or more filters to apply to the stream, on the server side. May be empty.", required = false, allowableValues = "zip,encrypt,base64", allowMultiple = true)
                                    @QueryParam("filters") final List<String> filters,
                                 @ApiParam(value = "An actual application/octet-stream of whatever object you'd like to store.", required = true) final InputStream stream) throws Exception
    {
        try
        {
            validateId(id);

            final StreamStatus status = getStreamService().getStatusForStreamById(id);

            // If this stream is unknown, we can't update it.
            if (StreamStatus.NOT_FOUND.equals(status))
            {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Likewise, it may be "busy".
            if (StreamStatus.IN_PROGRESS.equals(status))
            {
                return Response.status(Response.Status.CONFLICT).build();
            }

            // If it's not busy, and we know what it is, try and update...
            getStreamService().saveStream(id, stream, filters);
        }
        finally
        {
            IOUtils.closeQuietly(stream);
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    // TODO [kog@epiphanic.org - 6/2/15]: This could be async.

    /**
     * Attempts to delete a stream by ID. If the stream is unknown, does nothing.
     *
     * @param id The ID of the stream to delete.
     *
     * @return 202/ACCEPTED unless an exception is thrown,
     *         403/FORBIDDEN if the ID is invalid.
     */
    @ApiOperation(value = "Attempts to delete a given stream.")
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "If the ID is valid."),
            @ApiResponse(code = 403, message = "If the stream ID is considered invalid.")
    })
    @Path("/{id}")
    @DELETE
    public Response deleteStream(@ApiParam(value = "ID of the stream to fetch", required = true) final @PathParam("id") String id) throws Exception
    {
        validateId(id);
        getStreamService().deleteStream(id);

        return Response.status(Response.Status.ACCEPTED).build();
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
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }
}
