package com._8x8.cloud.hss.service;

import com._8x8.cloud.hss.model.StreamStatus;

import java.io.InputStream;

/**
 * Provides a service to handle the various streams. It puts the streams on the filesystem, else it gets the hose again.
 *
 * @author kog@epiphanic.org
 * @since 05/28/2015
 */
public interface IStreamService
{
    /**
     * Obtains an {@link InputStream} to a given stream object, by ID.
     *
     * @param id The ID of the stream to fetch. Must not be blank, must be valid.
     *
     * @return An {@link InputStream} pointing to the resource, if known, else null.
     */
    InputStream getStreamById(String id);

    /**
     * Obtains the {@link StreamStatus} for a given stream object, by ID.
     *
     * @param id The ID of the stream to fetch. Must not be blank, must be valid.
     *
     * @return A {@link StreamStatus} representing what the system knows of the given entity. Will not be null.
     */
    StreamStatus getStatusForStreamById(String id);

    /**
     * Attempts to persist the given stream to a backing store.
     *
     * @param id The ID to use for the stream. Must not be blank, must be valid.
     * @param stream An {@link InputStream} to the resource to save. Must not be null, must be valid.
     */
    void saveStream(String id, InputStream stream);
}
