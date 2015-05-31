package com._8x8.cloud.hss.service;

import com._8x8.cloud.hss.model.StreamStatus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

// TODO [kog@epiphanic.org - 5/30/15]: Add filtering support.

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

    @Override
    public InputStream getStreamById(final String id) throws Exception
    {
        if (getStatusForStreamById(id) != StreamStatus.NOT_FOUND)
        {
            return FileUtils.openInputStream(createFileForId(id));
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
    public void saveStream(final String id, final InputStream stream) throws Exception
    {
        try (final FileOutputStream outputStream = FileUtils.openOutputStream(createFileForId(id)))
        {
            IOUtils.copyLarge(stream, outputStream);
        }
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
