package com._8x8.cloud.hss.service;

import com._8x8.cloud.hss.filter.FilterManager;
import com._8x8.cloud.hss.model.StreamMetadata;
import com._8x8.cloud.hss.model.StreamStatus;
import com._8x8.cloud.hss.persistence.IStreamStateDao;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

/**
 * Provides a concrete implementation of {@link IStreamService}.<p/>
 *
 * Please note that this is a naive implementation, and does not do a lot of things you would in the real world, most
 * crucially bucketing. In a system where you expect to have a lot of files you wouldn't want to stick them all in the
 * same directory, because this will wreck most filesystems. You'd also probably want to shard the data, though that can
 * get a little rough since files may not be regularly sized. Lastly, you'd expect some sort of replication mechanism
 * (IE: make N copies to hosts within the ring).
 *
 * @author kog@epiphanic.org
 * @since 05/28/2015
 */
public class StreamService implements IStreamService
{
    /**
     * Holds a {@link File} pointing to where we're going to store our streams. Defaults to <code>/tmp/foo</code>.
     */
    private File _streamStorageDirectory = new File("/tmp/foo");

    /**
     * Holds the {@link FilterManager} we use for applying filters to streams.
     */
    public FilterManager _filterManager;

    /**
     * Holds the {@link IStreamStateDao} we're going to use to manipulate our stream status metadata.
     */
    private IStreamStateDao _streamStateDao;

    /**
     * Gets the {@link File} we're using as a base location to store our streams.
     *
     * @return The {@link File} we're using as a base location to store our streams.
     */
    public File getStreamStorageDirectory()
    {
        return _streamStorageDirectory;
    }

    /**
     * Sets the {@link File} we're using as a base location to store our streams.
     *
     * @param streamStorageDirectory An absolute path to use as a base location to store our streams. Must not be blank,
     *                               must be valid.
     */
    public void setStreamStorageDirectory(final String streamStorageDirectory)
    {
        _streamStorageDirectory = new File(streamStorageDirectory);
    }

    /**
     * Gets the {@link FilterManager} to use for applying filters to streams.
     *
     * @return A non-null, valid and fully wired {@link FilterManager} for managing our streams.
     */
    public FilterManager getFilterManager()
    {
        return _filterManager;
    }

    /**
     * Sets the {@link FilterManager} to use for applying filters to streams.
     *
     * @param filterManager A non-null, valid and fully wired {@link FilterManager} for managing our streams.
     */
    public void setFilterManager(FilterManager filterManager)
    {
        _filterManager = filterManager;
    }

    /**
     * Gets the {@link IStreamStateDao} to use for manipulating stream status metadata.
     *
     * @return A non-null, valid and fully wired {@link IStreamStateDao} to use for manipulating stream status metadata.
     */
    public IStreamStateDao getStreamStateDao()
    {
        return _streamStateDao;
    }

    /**
     * Sets the {@link IStreamStateDao} to use for manipulating stream status metadata.
     *
     * @param streamStateDao A non-null, valid and fully wired {@link IStreamStateDao} to use for manipulating stream status metadata.
     */
    public void setStreamStateDao(IStreamStateDao streamStateDao)
    {
        _streamStateDao = streamStateDao;
    }

    /**
     * Takes care of the initialization logic for our stream storage: making sure our storage directory actually exists
     * and such.
     */
    public void init() throws Exception
    {
        // If our storage directory doesn't already exist, let's make it.
        if (!getStreamStorageDirectory().exists())
        {
            FileUtils.forceMkdir(getStreamStorageDirectory());
        }
    }

    // TODO [kog@epiphanic.org - 6/14/2015]: So, if someone tries to do a write while we're in the middle of a get, it'd probably break (depending on things like OS).
    // TODO [kog@epiphanic.org - 6/14/2015]: It might be worth adding cluster-wide read/write locking: reads are idempotent, writes are definitely not.
    // TODO [kog@epiphanic.org - 6/14/2015]: Likewise, you might want some sort of caching mechanism or key/value store, which would need invalidation.

    // TODO [kog@epiphanic.org - 6/14/2015]: Alternatively there could be a stronger versioning system: each stream has a UUID, and the stream ID points to a
    // TODO [kog@epiphanic.org - 6/14/2015]: UUID that gets swapped when the write is complete. Again, this is way outside the scope of the project.

    @Override
    public InputStream getStreamById(final String id, final List<String> filters) throws Exception
    {
        // We can only grab this file if it was successfully uploaded...
        final StreamStatus streamStatus = getStatusForStreamById(id);

        if (StreamStatus.SUCCESSFUL.equals(streamStatus))
        {
            InputStream stream = null;

            try
            {
                stream = FileUtils.openInputStream(createFileForId(id));
                return getFilterManager().prepareInputFilters(stream, filters);
            }
            catch(final Exception ex)
            {
                // If we fail to apply filters, we're going to need to clean up after ourselves. If we put this in a try-with-resources
                // we will instead close the stream before we return up the call stack.
                IOUtils.closeQuietly(stream);

                // We're not doing much with exceptions though, so throw this back up the stack.
                throw ex;
            }
        }

        return null;
    }

    @Override
    public StreamStatus getStatusForStreamById(final String id) throws Exception
    {
        // Our DAO will always hand us back metadata - even if that metadata says the stream doesn't exist. This should
        // be an inherently safe operation.
        return getStreamStateDao().findStreamMetadataById(id).getStatus();
    }

    // TODO [kog@epiphanic.org - 6/14/2015]: We also have an issue where multiple nodes could try and write here. Again,
    // TODO [kog@epiphanic.org - 6/14/2015]: pessimistic locking would be a good thing. Or versioning.

    @Override
    public void saveStream(final String id, final InputStream stream, final List<String> filters) throws Exception
    {
        final StreamMetadata metadata = getStreamStateDao().findStreamMetadataById(id);

        // If we're not currently doing something to this stream...
        if (!StreamStatus.IN_PROGRESS.equals(metadata.getStatus()))
        {
            // Mark this file in progress. Again, token effort here...
            markStreamInProgress(metadata);

            try
            {
                final File outputFile = createFileForId(id);

                try (final OutputStream outputStream = FileUtils.openOutputStream(outputFile);
                     final OutputStream filteredOutputStream = getFilterManager().prepareOutputFilters(outputStream, filters))
                {
                    // Finish the job, mark it as a success. While we're at it, get the latest size...
                    IOUtils.copyLarge(stream, filteredOutputStream);
                    markStreamSuccessful(metadata, outputFile);
                }
            }
            catch (final Exception ex)
            {
                // Unless, of course, we fail.
                markStreamFailure(metadata);

                // And then re-throw...
                throw ex;
            }
        }
    }

    @Override
    public void deleteStream(final String id) throws Exception
    {
        // We can delete anything that exists, and is not currently in use.
        final StreamStatus streamStatus = getStatusForStreamById(id);

        if (StreamStatus.SUCCESSFUL.equals(streamStatus) || StreamStatus.FAILED.equals(streamStatus))
        {
            // Delete the status first. With this gone, even if our force delete fails, people can still do whatever operation.
            getStreamStateDao().deleteStreamMetadataById(id);

            // Wipe the actual file. Even if this fails, the file is more or less useless. We'd probably have a background job to
            // purge things in the FS not in the persistence store.
            FileUtils.deleteQuietly(createFileForId(id));
        }
    }

    @Override
    public Collection<StreamMetadata> getMetadataForStreams() throws Exception
    {
        return getStreamStateDao().findStreamMetadata();
    }

    @Override
    public StreamMetadata getMetadataForStreamById(final String id) throws Exception
    {
        return getStreamStateDao().findStreamMetadataById(id);
    }

    // TODO [kog@epiphanic.org - 6/14/2015]: Would probably refactor these state callbacks into a separate mechanism in
    // TODO [kog@epiphanic.org - 6/14/2015]: the real world. Depending on complexity.

    /**
     * Provides a callback for the case where we fail to save a given stream. In this case, all we do is mark the
     * status metadata as {@link StreamStatus#FAILED}. We might chose to do more in the real world (cleanup, shuffling
     * etc).
     *
     * @param metadata The {@link StreamMetadata} to mark as failed. Must not be null.
     */
    void markStreamFailure(final StreamMetadata metadata) throws Exception
    {
        metadata.setStatus(StreamStatus.FAILED);
        getStreamStateDao().saveOrUpdateStreamMetadata(metadata);
    }

    /**
     * Provides a callback for the case where we successfully save a given stream. In this case we're marking the status
     * as {@link StreamStatus#SUCCESSFUL} and we record the size of the file. We might do something like hashing
     * in the real world.
     *
     * @param metadata The {@link StreamMetadata} to mark as successful. Must not be null.
     * @param outputFile The {@link File} pointing to our output file. Used to do any post-upload actions.
     */
    void markStreamSuccessful(final StreamMetadata metadata, final File outputFile) throws Exception
    {
        metadata.setStatus(StreamStatus.SUCCESSFUL);
        metadata.setFileSize(outputFile.length());

        getStreamStateDao().saveOrUpdateStreamMetadata(metadata);
    }

    /**
     * Provides a callback for the case where we're starting process a given stream. All we're doing at present is
     * marking the status as {@link StreamStatus#IN_PROGRESS}.
     *
     * @param metadata The {@link StreamMetadata} to mark as in progress. Must not be null.
     */
    void markStreamInProgress(final StreamMetadata metadata) throws Exception
    {
        metadata.setStatus(StreamStatus.IN_PROGRESS);
        getStreamStateDao().saveOrUpdateStreamMetadata(metadata);
    }

    /**
     * Provides a convenience method to turn a given stream ID into a {@link File} denoting storage location.
     *
     * @param id The ID of the stream to create a {@link File} for. Must not be blank, must be valid.
     *
     * @return A {@link File} pointing to a storage location for a given stream. Will not be null.
     */
    File createFileForId(final String id)
    {
        return new File(getStreamStorageDirectory(), id);
    }
}
