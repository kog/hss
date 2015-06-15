package com._8x8.cloud.hss.resource;

import com._8x8.cloud.hss.model.StreamMetadata;
import com._8x8.cloud.hss.persistence.IStreamStateDao;
import com._8x8.cloud.hss.service.StreamService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Provides a couple of utility endpoints to help us with testing. Normally these would be filtered out by something like
 * a load balancer/VIP.
 *
 * @author kog@epiphanic.org
 * @since 05/28/2015
 */
@Path("test")
public class TestingResource
{
    /**
     * Holds the collaborating {@link IStreamStateDao} we're going to use to modify the state of streams for testing.
     */
    @Autowired
    private IStreamStateDao _streamStateDao;

    /**
     * Holds the collaborating {@link StreamService} we're going to use to do naughty things.
     */
    @Autowired
    private StreamService _streamService;

    /**
     * Gets the {@link IStreamStateDao} we're going to use to modify the state of streams for testing.
     *
     * @return A non-null, valid and fully wired {@link IStreamStateDao} to manipulate stream state.
     */
    public IStreamStateDao getStreamStateDao()
    {
        return _streamStateDao;
    }

    /**
     * Sets the {@link IStreamStateDao} we're going to use to modify the state of streams for testing.
     *
     * @param streamStateDao A non-null, valid and fully wired {@link IStreamStateDao} to manipulate stream state.
     */
    public void setStreamStateDao(IStreamStateDao streamStateDao)
    {
        _streamStateDao = streamStateDao;
    }

    /**
     * Gets the {@link StreamService} we're going to use to do very naughty things.
     *
     * @return A non-null, valid and fully wired {@link StreamService} for your shenanigans.
     */
    public StreamService getStreamService()
    {
        return _streamService;
    }

    /**
     * Sets the {@link StreamService} we're going to use to do very naughty things.
     *
     * @param streamService A non-null, valid and fully wired {@link StreamService} for your nefarious endeavors.
     */
    public void setStreamService(StreamService streamService)
    {
        _streamService = streamService;
    }

    /**
     * Since we're going to use an embedded DB, provide a testing endpoint for overwriting {@link StreamMetadata} to
     * enable various test cases. This would traditionally be filtered out of public access by things like your VIP
     * or load balancer.
     *
     * @param metadata The {@link StreamMetadata} to overwrite. Must not be null.
     */
    @Path("state")
    @POST
    public void overwriteState(final StreamMetadata metadata) throws Exception
    {
        getStreamStateDao().saveOrUpdateStreamMetadata(metadata);
    }

    /**
     * Provides a mechanism to delete the persisted state for a givne stream.
     *
     * @param streamId The ID of the stream to delete.
     */
    @Path("state/{id}")
    @DELETE
    public void deleteState(final @PathParam("id") String streamId) throws Exception
    {
        getStreamStateDao().deleteStreamMetadataById(streamId);
    }

    /**
     * Gets the location we're using to store our files.
     *
     * @return The location we're using to store our files.
     */
    @Produces("text/plain")
    @Path("storage")
    @GET
    public String getStorageLocation()
    {
        return getStreamService().getStreamStorageDirectory().getAbsolutePath();
    }
}
