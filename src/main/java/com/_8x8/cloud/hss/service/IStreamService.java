package com._8x8.cloud.hss.service;

import com._8x8.cloud.hss.model.StreamStatus;

import java.io.InputStream;
import java.util.List;

// TODO [kog@epiphanic.org - 5/30/15]: Should probably not bubble up Exceptions here... Either that, or use an ExceptionMapper.

/**
 * Provides a service to handle the various streams. It puts the streams on the filesystem, else it gets the hose again.
 *
 * @author kog@epiphanic.org
 * @since 05/28/2015
 */
public interface IStreamService
{
    /**
     * Obtains an {@link InputStream} to a given stream object, by ID.<p/>
     *
     * Please note that callers are responsible for closing the returned {@link InputStream}.
     *
     * @param id The ID of the stream to fetch. Must not be blank, must be valid.
     * @param filters A list of zero or more filters to apply to the stream. May be empty, but must not be null.
     *
     * @return An {@link InputStream} pointing to the resource, if known, else null.
     *
     * @throws Exception If we fail to return the stream for the given ID. This must be handled up the call stack.
     */
    InputStream getStreamById(String id, List<String> filters) throws Exception;

    /**
     * Obtains the {@link StreamStatus} for a given stream object, by ID.<p/>
     *
     * @param id The ID of the stream to fetch. Must not be blank, must be valid.
     *
     * @return A {@link StreamStatus} representing what the system knows of the given entity. Will not be null.
     */
    StreamStatus getStatusForStreamById(String id);

    /**
     * Attempts to persist the given stream to a backing store.</p>
     *
     * Please note that callers are required to close the passed input stream.
     *
     * @param id The ID to use for the stream. Must not be blank, must be valid.
     * @param stream An {@link InputStream} to the resource to save. Must not be null, must be valid.
     * @param filters A list of zero or more filters to apply to the stream. May be empty, but must not be null.
     *
     * @throws Exception If we fail to return the stream for the given ID. This must be handled up the call stack.
     */
    void saveStream(String id, InputStream stream, List<String> filters) throws Exception;
}
