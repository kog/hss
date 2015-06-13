package com._8x8.cloud.hss.service;

import com._8x8.cloud.hss.filter.FilterManager;
import com._8x8.cloud.hss.model.StreamMetadata;
import com._8x8.cloud.hss.model.StreamStatus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// TODO [kog@epiphanic.org - 6/2/15]: There are some race conditions with regards to the various stream statuses. Should probably
// TODO [kog@epiphanic.org - 6/2/15]: come in here and address that...

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

    // TODO [kog@epiphanic.org - 6/2/15]: Need to add checking of stream state for in progress.

    @Override
    public InputStream getStreamById(final String id, final List<String> filters) throws Exception
    {
        if (getStatusForStreamById(id) != StreamStatus.NOT_FOUND)
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

    // TODO [kog@epiphanic.org - 5/30/15]: Add StreamStatus for in progress. Either something like SQLite or use files on FS.

    @Override
    public StreamStatus getStatusForStreamById(final String id)
    {
        if (!createFileForId(id).exists())
        {
            return StreamStatus.NOT_FOUND;
        }

        return StreamStatus.SUCCESSFUL;
    }

    @Override
    public void saveStream(final String id, final InputStream stream, final List<String> filters) throws Exception
    {
        try (final OutputStream outputStream = FileUtils.openOutputStream(createFileForId(id));
             final OutputStream filteredOutputStream = getFilterManager().prepareOutputFilters(outputStream, filters))
        {
            IOUtils.copyLarge(stream, filteredOutputStream);
        }
    }

    // TODO [kog@epiphanic.org - 6/2/15]: Need to add checking of stream state for in progress.

    @Override
    public void deleteStream(final String id) throws Exception
    {
        if (StreamStatus.SUCCESSFUL.equals(getStatusForStreamById(id)))
        {
            FileUtils.forceDelete(createFileForId(id));
        }
    }

    @Override
    public Collection<StreamMetadata> getMetadataForStreams() throws Exception
    {
        final Collection<StreamMetadata> metadata = new ArrayList<>();

        FileUtils.iterateFiles(getStreamStorageDirectory(), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
                 .forEachRemaining(file -> metadata.add(getMetadataForStreamById(file.getName())));

        return metadata;
    }

    // TODO [kog@epiphanic.org - 6/2/15]: Revisit when other statuses added.

    @Override
    public StreamMetadata getMetadataForStreamById(String id)
    {
        final File file = createFileForId(id);
        final StreamMetadata metadata = new StreamMetadata();

        metadata.setId(id);
        metadata.setStatus(file.exists() ? StreamStatus.SUCCESSFUL : StreamStatus.NOT_FOUND);

        if (file.exists())
        {
            metadata.setFileSize(file.length());
            metadata.setLastModified(file.lastModified());
        }

        return metadata;
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
