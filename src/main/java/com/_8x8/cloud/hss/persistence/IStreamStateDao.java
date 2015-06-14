package com._8x8.cloud.hss.persistence;

import com._8x8.cloud.hss.model.StreamMetadata;

import java.util.List;

/**
 * Provides a contract for a DAO, allowing manipulation of stream state.
 *
 * @author kog@epiphanic.org
 * @since 06/11/2015
 */
public interface IStreamStateDao
{
    /**
     * Attempts to find the {@link StreamMetadata} associated with all streams known to the system.
     *
     * @return A list of zero or more {@link StreamMetadata}, as known to the system. May be empty, but will never be
     * null.
     */
    List<StreamMetadata> findStreamMetadata() throws Exception;

    /**
     * Attempts to find the known {@link StreamMetadata} for a given stream.
     *
     * @param streamId The ID of the stream to find {@link StreamMetadata} for. Must be valid.
     *
     * @return Any known {@link StreamMetadata} for the ID, if known, otherwise <code>null</code>.
     */
    StreamMetadata findStreamMetadataById(String streamId) throws Exception;

    /**
     * Attempts to save (or update) the {@link StreamMetadata} for a given stream. Please note that this operation may
     * fail if the stream is currently {@link com._8x8.cloud.hss.model.StreamStatus#IN_PROGRESS}.
     *
     * @param metadata The {@link StreamMetadata} associated with the stream. Must be a valid stream ID.
     */
    void saveOrUpdateStreamMetadata(StreamMetadata metadata) throws Exception;

    /**
     * Attempts to delete the {@link StreamMetadata} for a given stream.
     *
     * @param streamId The ID of the stream to delete the metadata for. Must be a valid stream ID.
     */
    void deleteStreamMetadataById(String streamId) throws Exception;
}
