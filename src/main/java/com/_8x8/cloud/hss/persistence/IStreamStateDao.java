package com._8x8.cloud.hss.persistence;

import com._8x8.cloud.hss.model.StreamMetadata;
import com._8x8.cloud.hss.model.StreamStatus;

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
     * Provides a simple creation mechanism to create {@link StreamMetadata} for a given stream ID. Please note that
     * as the streams themselves are quite heavy, we tend to pass around the corresponding metadata. In this case the
     * metadata encodes whether or not the object exists - a normally unRESTful practice. But then, you don't generally
     * haul around large blobs of data...<p/>
     *
     * Please note that if a {@link StreamStatus} is not provided, we will default to {@link StreamStatus#IN_PROGRESS}
     * as this is the default behavior of <b>creating</b> a stream.
     *
     * @param streamId The ID of the stream to find {@link StreamMetadata} for. Must be valid.
     * @param status The {@link StreamStatus} to set for the {@link StreamMetadata}. If null, will default to {@link StreamStatus#IN_PROGRESS}.
     *
     * @return A non-null, valid {@link StreamMetadata} with the given ID and status.
     */
    StreamMetadata createStreamMetadata(String streamId, StreamStatus status);

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
     * @return {@link StreamMetadata} regarding the stream. Will never be null, but may indicate the stream is unknown,
     * as well as omitting some data points.
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
