package com._8x8.cloud.hss.persistence;

import com._8x8.cloud.hss.model.StreamMetadata;
import com._8x8.cloud.hss.model.StreamStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests {@link StreamStateDao} at the unit level.
 *
 * @author kog@epiphanic.org
 * @since 06/11/2015
 */
public class StreamStateDaoTestCase
{
    /**
     * Holds an instance of the class under test.
     */
    private StreamStateDao _streamStateDao;

    /**
     * Holds a collaborating {@link NamedParameterJdbcTemplate} we can mock for its silly ways.
     */
    private NamedParameterJdbcTemplate _namedParameterJdbcTemplate;

    /**
     * Holds a collaborating {@link ResultSet} which is most deserving of our mockery.
     */
    private ResultSet _resultSet;

    @Before
    public void setUp() throws Exception
    {
        _streamStateDao = spy(new StreamStateDao());

        _namedParameterJdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        doReturn(_namedParameterJdbcTemplate).when(_streamStateDao).getNamedParameterJdbcTemplate();

        _resultSet = mock(ResultSet.class);
    }

    /**
     * Tests {@link StreamStateDao#findStreamMetadata()} to make sure it does what we expect.
     **/
    @Test
    public void testFindStreamMetadata() throws Exception
    {
        _streamStateDao.findStreamMetadata();

        verify(_streamStateDao).findStreamMetadata();
        verify(_streamStateDao).getNamedParameterJdbcTemplate();

        final ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(_namedParameterJdbcTemplate).query(queryCaptor.capture(), any(StreamStateDao.StreamMetadataMapper.class));

        verifyNoMoreCollaboratingInteractions();

        // Make sure our query is what we expect.
        Assert.assertThat(queryCaptor.getValue(), is("SELECT * FROM STREAM_STATUS"));
    }

    /**
     * Tests {@link StreamStateDao#findStreamMetadataById(String)} for the happy path.
     **/
    @Test
    public void testFindStreamMetadataById() throws Exception
    {
        // Pretend we've got data...
        final StreamMetadata metadata = new StreamMetadata();

        doReturn(Collections.singletonList(metadata)).when(_namedParameterJdbcTemplate).query(any(String.class),
                                                                                              any(SqlParameterSource.class),
                                                                                              any(StreamStateDao.StreamMetadataMapper.class));

        // We should get back the item we specified above.
        final ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        Assert.assertThat(metadata, is(_streamStateDao.findStreamMetadataById("foo")));

        // We should probably call a couple of extra methods while we're at it...
        verify(_streamStateDao).findStreamMetadataById(anyString());
        verify(_streamStateDao).getNamedParameterJdbcTemplate();
        verify(_streamStateDao).createParameters(anyString());

        verify(_namedParameterJdbcTemplate).query(queryCaptor.capture(), any(SqlParameterSource.class), any(StreamStateDao.StreamMetadataMapper.class));

        verifyNoMoreCollaboratingInteractions();

        // Make sure our query is what we expect.
        Assert.assertThat(queryCaptor.getValue(), is("SELECT * FROM STREAM_STATUS WHERE STREAM_ID = :streamId"));
    }

    /**
     * Tests {@link StreamStateDao#findStreamMetadataById(String)} for the case where the ID is unknown.
     **/
    @Test
    public void testFindStreamMetadataByIdForNoResult() throws Exception
    {
        Assert.assertThat(_streamStateDao.findStreamMetadataById("foo"), is(nullValue()));

        verify(_streamStateDao).findStreamMetadataById(anyString());
        verify(_streamStateDao).getNamedParameterJdbcTemplate();
        verify(_streamStateDao).createParameters(anyString());

        verify(_namedParameterJdbcTemplate).query(any(String.class), any(SqlParameterSource.class), any(StreamStateDao.StreamMetadataMapper.class));

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamStateDao#saveOrUpdateStreamMetadata(StreamMetadata)} to make sure it does what we expect.
     **/
    @Test
    public void testSaveOrUpdateMetadata() throws Exception
    {
        final String uuid = UUID.randomUUID().toString();
        final StreamMetadata metadata = createMetadata(uuid, StreamStatus.SUCCESSFUL, 8675309L, 123456L, 7890L);

        _streamStateDao.saveOrUpdateStreamMetadata(metadata);

        verify(_streamStateDao).saveOrUpdateStreamMetadata(metadata);
        verify(_streamStateDao).getNamedParameterJdbcTemplate();

        final ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(_namedParameterJdbcTemplate).update(queryCaptor.capture(), any(SqlParameterSource.class));

        verifyNoMoreCollaboratingInteractions();

        // Make sure our query is what we expect.
        Assert.assertThat(queryCaptor.getValue(), is("MERGE INTO STREAM_STATUS AS S USING (VALUES :streamId, :status, :size) I(STREAM_ID, STATUS, SIZE) ON (S.STREAM_ID = I.STREAM_ID) WHEN MATCHED THEN UPDATE SET S.STREAM_ID = I.STREAM_ID, S.STATUS = I.STATUS, S.SIZE = I.SIZE, LAST_UPDATED = NOW() WHEN NOT MATCHED THEN INSERT(STREAM_ID, STATUS, SIZE, CREATED, LAST_UPDATED) VALUES(I.STREAM_ID, I.STATUS, I.SIZE, NOW(), NOW())"));
    }

    /**
     * Tests {@link StreamStateDao#deleteStreamMetadataById(String)} to make sure it does what we expect.
     **/
    @Test
    public void testDeleteStreamMetadataById() throws Exception
    {
        _streamStateDao.deleteStreamMetadataById("foo");

        verify(_streamStateDao).deleteStreamMetadataById(anyString());
        verify(_streamStateDao).getNamedParameterJdbcTemplate();
        verify(_streamStateDao).createParameters(anyString());

        final ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(_namedParameterJdbcTemplate).update(queryCaptor.capture(), any(SqlParameterSource.class));

        verifyNoMoreCollaboratingInteractions();

        // Make sure our query is what we expect.
        Assert.assertThat(queryCaptor.getValue(), is("DELETE FROM STREAM_STATUS WHERE STREAM_ID = :streamId"));
    }

    /**
     * Tests {@link StreamStateDao#createParameters(String)} to make sure it behaves as expected.
     **/
    @Test
    public void testCreateParameters() throws Exception
    {
        final String id = UUID.randomUUID().toString();
        final SqlParameterSource source = _streamStateDao.createParameters(id);

        Assert.assertThat(id, is(source.getValue("streamId")));
    }

    /**
     * Tests {@link com._8x8.cloud.hss.persistence.StreamStateDao.StreamMetadataMapper} to make sure it binds the
     * fields we expect.
     **/
    @Test
    public void testStreamMetadataMapper() throws Exception
    {
        final StreamStateDao.StreamMetadataMapper mapper = mock(StreamStateDao.StreamMetadataMapper.class);
        doCallRealMethod().when(mapper).mapRow(any(ResultSet.class), anyInt());

        // Stub out some data.
        doReturn("IN_PROGRESS").when(_resultSet).getString("STATUS");
        doReturn(mock(Timestamp.class)).when(_resultSet).getTimestamp(anyString());

        final StreamMetadata metadata = mapper.mapRow(_resultSet, 300);

        // Make sure we're binding the fields we expect.
        verify(_resultSet).getString("STREAM_ID");
        verify(_resultSet).getString("STATUS");
        verify(_resultSet).getLong("SIZE");
        verify(_resultSet).getTimestamp("CREATED");
        verify(_resultSet).getTimestamp("LAST_UPDATED");

        // Make sure we're grabbing our enum...
        Assert.assertThat(metadata.getStatus(), is(StreamStatus.IN_PROGRESS));

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Provides a convenience method for making sure no more interactions have occurred between our collaborators.
     */
    private void verifyNoMoreCollaboratingInteractions()
    {
        verifyNoMoreInteractions(_streamStateDao, _namedParameterJdbcTemplate, _resultSet);
    }

    /**
     * Provides a convenience method to create a {@link StreamMetadata} as a one-liner.
     *
     * @param id The ID of the stream. Must be valid.
     * @param status The {@link StreamStatus} of the stream. Must not be null.
     * @param fileSize The filesize to use for the stream.
     * @param lastModified The last modified time for the stream.
     * @param createdTime The creation time for the stream.
     *
     * @return A nice and shiny {@link StreamMetadata}.
     */
    private StreamMetadata createMetadata(final String id, final StreamStatus status, final long fileSize, final long lastModified, final long createdTime)
    {
        final StreamMetadata metadata = new StreamMetadata();

        metadata.setId(id);
        metadata.setStatus(status);
        metadata.setFileSize(fileSize);
        metadata.setLastModified(lastModified);
        metadata.setCreatedTime(createdTime);

        return metadata;
    }
}
