package com._8x8.cloud.hss.persistence;

import com._8x8.cloud.hss.model.StreamMetadata;
import com._8x8.cloud.hss.model.StreamStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Provides a concrete implementation of {@link IStreamStateDao}.<p/>
 *
 * So this is more or less here to track the "pending" stream state. There are a number of other common ways of doing this:
 *
 * <ul>
 *     <li>Synchronizing methods - this sucks without even being in a cluster. Nuff said.</li>
 *     <li>Using some sort of locking in-JVM - this would definitely work on a single node, but not so well in a cluster.</li>
 *     <li>Using the FS for locking - this would certainly work in a cluster, but requires shared state for the FS (NFS/CIFS etc).</li>
 *     <li>Using some sort of persistence store - this is more likely to be used in the real world. Granted, you'd probably use a different technology...</li>
 * </ul>
 *
 * I also figured it would demonstrate another common thing we all do in Java: database access. Again, granted the caveat
 * that you wouldn't exactly do it this way in a production system... JDBC template seemed like a good-enough shortcut...<p/>
 *
 * It's also worth noting that persistence/FS allow us to do things like pre-computation on storage/modification. The
 * persistence approach also allows you to do clever things like pointing to other locations.<p/>
 *
 * We could have added something like JPA/Hibernate in here, but at some point you just gotta call it a day...
 *
 * @author kog@epiphanic.org
 * @since 06/11/2015
 */
public class StreamStateDao extends NamedParameterJdbcDaoSupport implements IStreamStateDao
{
    // TODO [kog@epiphanic.org - 6/11/15]: queries -> property files

    @Override
    public StreamMetadata createStreamMetadata(final String streamId, final StreamStatus status)
    {
        final StreamMetadata metadata = new StreamMetadata();

        metadata.setId(streamId);
        metadata.setStatus(null == status ? StreamStatus.IN_PROGRESS : status);

        return metadata;
    }

    @Override
    public List<StreamMetadata> findStreamMetadata() throws Exception
    {
        return getNamedParameterJdbcTemplate().query("SELECT * FROM STREAM_STATUS", new StreamMetadataMapper());
    }

    @Override
    public StreamMetadata findStreamMetadataById(final String streamId)
    {
        final List<StreamMetadata> metadata =  getNamedParameterJdbcTemplate().query("SELECT * FROM STREAM_STATUS WHERE STREAM_ID = :streamId",
                                                                                     createParameters(streamId),
                                                                                     new StreamMetadataMapper());

        // It kind of sucks that there's no better way to re-use our RowMapper than doing a query here...
        if (null != metadata && !metadata.isEmpty())
        {
            // Although, technically, we should never have more than one result here...
            return metadata.get(0);
        }

        // If we've never seen this, return a new record denoting we have no idea what it is.
        return createStreamMetadata(streamId, StreamStatus.NOT_FOUND);
    }

    @Override
    public void saveOrUpdateStreamMetadata(final StreamMetadata metadata)
    {
        final String query = "MERGE INTO STREAM_STATUS AS S "+
                             "USING (VALUES :streamId, :status, :size) I(STREAM_ID, STATUS, SIZE) "+
                             "ON (S.STREAM_ID = I.STREAM_ID) " +
                             "WHEN MATCHED THEN UPDATE SET S.STREAM_ID = I.STREAM_ID, S.STATUS = I.STATUS, S.SIZE = I.SIZE, LAST_UPDATED = NOW() " +
                             "WHEN NOT MATCHED THEN INSERT(STREAM_ID, STATUS, SIZE, CREATED, LAST_UPDATED) VALUES(I.STREAM_ID, I.STATUS, I.SIZE, NOW(), NOW())";

        final SqlParameterSource parameters = new MapSqlParameterSource("streamId", metadata.getId()).addValue("status", metadata.getStatus().toString())
                                                                                                     .addValue("size", metadata.getFileSize());

        getNamedParameterJdbcTemplate().update(query, parameters);
    }

    @Override
    public void deleteStreamMetadataById(final String streamId)
    {
        getNamedParameterJdbcTemplate().update("DELETE FROM STREAM_STATUS WHERE STREAM_ID = :streamId",
                                                createParameters(streamId));
    }

    /**
     * Provides a convenience method to create a {@link SqlParameterSource} for a stream ID.
     *
     * @param streamId The ID of the stream. Must be a valid stream ID.
     *
     * @return A {@link SqlParameterSource} for all your querying needs. Will be valid and non-null.
     */
    SqlParameterSource createParameters(final String streamId)
    {
        return new MapSqlParameterSource("streamId", streamId);
    }

    /**
     * Provides a {@link RowMapper} that we can use to create a {@link StreamMetadata} from a given {@link ResultSet}.
     */
    class StreamMetadataMapper implements RowMapper<StreamMetadata>
    {
        @Override
        public StreamMetadata mapRow(final ResultSet rs, final int rowNum) throws SQLException
        {
            final StreamMetadata metadata = new StreamMetadata();

            metadata.setId(rs.getString("STREAM_ID"));
            metadata.setStatus(StreamStatus.valueOf(rs.getString("STATUS")));
            metadata.setFileSize(rs.getLong("SIZE"));
            metadata.setCreatedTime(rs.getTimestamp("CREATED").getTime());
            metadata.setLastModified(rs.getTimestamp("LAST_UPDATED").getTime());

            return metadata;
        }
    }
}
