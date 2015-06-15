package com._8x8.cloud.hss.persistence;

import com._8x8.cloud.hss.model.StreamMetadata;
import com._8x8.cloud.hss.model.StreamStatus;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

/**
 * Tests the {@link StreamStateDao} at the integration level.
 *
 * @author kog@epiphanic.org
 * @since 06/11/2015
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath*:applicationContext.xml")
public class StreamStateDaoITCase
{
    /**
     * Holds an instance of the class under test.
     */
    @Resource
    private StreamStateDao _streamStateDao;

    /**
     * Holds an {@link ObjectMapper} we can use for serializing values.
     */
    private ObjectMapper _objectMapper;

    @SuppressWarnings("deprecation")
    @Before
    public void setUp() throws Exception
    {
        // If we want a reproducible ordering, we're going to need our JAXB annotations...
        final AnnotationIntrospector pair = AnnotationIntrospector.pair(new JaxbAnnotationIntrospector(), new JacksonAnnotationIntrospector());

        _objectMapper = new ObjectMapper();
        _objectMapper.setAnnotationIntrospectors(pair, pair);

        // Make sure we've got some test data on hand.
        _streamStateDao.saveOrUpdateStreamMetadata(createMetadata("BusyFile", StreamStatus.IN_PROGRESS, 1024, System.currentTimeMillis(), System.currentTimeMillis()));
        _streamStateDao.saveOrUpdateStreamMetadata(createMetadata("DoneFile", StreamStatus.SUCCESSFUL, 2048, System.currentTimeMillis(), System.currentTimeMillis()));
    }

    /**
     * Tests {@link StreamStateDao#findStreamMetadataById(String)} for the happy path. We're going to try and find a
     * record that we create in our schema, so this should always work...
     **/
    @Test
    public void testFindStreamMetadataById() throws Exception
    {
        final StreamMetadata metadata = _streamStateDao.findStreamMetadataById("BusyFile");

        Assert.assertThat("BusyFile", is(metadata.getId()));
        Assert.assertThat(StreamStatus.IN_PROGRESS, is(metadata.getStatus()));
        Assert.assertThat(1024L, is(metadata.getFileSize()));

        // On the timestamps, we're just going to make sure the created/last updated times are current.
        Assert.assertThat(System.currentTimeMillis() - metadata.getCreatedTime(), is(lessThan(1000L)));
        Assert.assertThat(System.currentTimeMillis() - metadata.getLastModified(), is(lessThan(1000L)));
    }

    /**
     * Tests {@link StreamStateDao#findStreamMetadataById(String)} for the case where the stream ID is unknown. We should
     * get back <code>null</code>.
     **/
    @Test
    public void testFindStreamMetadataForUnknownStreamId() throws Exception
    {
        final String uuid = UUID.randomUUID().toString();
        assertMetadataUnknown(_streamStateDao.findStreamMetadataById(uuid), uuid);
    }

    /**
     * Tests {@link StreamStateDao#saveOrUpdateStreamMetadata(StreamMetadata)} for the case where we're creating a record.
     **/
    @Test
    public void testSaveOrUpdateStreamMetadataForSave() throws Exception
    {
        final String uuid = UUID.randomUUID().toString();

        // We should have nothing here for this UUID right now.
        assertMetadataUnknown(_streamStateDao.findStreamMetadataById(uuid), uuid);

        // Now make something.
        final StreamMetadata metadata = createMetadata(uuid, StreamStatus.SUCCESSFUL, 8675309L, System.currentTimeMillis(), System.currentTimeMillis());
        _streamStateDao.saveOrUpdateStreamMetadata(metadata);

        // Make sure that what we pull back is accurate.
        assertMetadataSimilar(metadata, _streamStateDao.findStreamMetadataById(uuid));

        // And now delete it...
        _streamStateDao.deleteStreamMetadataById(uuid);
    }

    /**
     * Tests {@link StreamStateDao#saveOrUpdateStreamMetadata(StreamMetadata)} for the case where we're updating a record.
     **/
    @Test
    public void testSaveOrUpdateStreamMetadataForUpdate() throws Exception
    {
        final String uuid = UUID.randomUUID().toString();

        // We should have nothing here for this UUID right now.
        assertMetadataUnknown(_streamStateDao.findStreamMetadataById(uuid), uuid);

        // Now make something.
        final StreamMetadata metadata = createMetadata(uuid, StreamStatus.SUCCESSFUL, 8675309L, System.currentTimeMillis(), System.currentTimeMillis());
        _streamStateDao.saveOrUpdateStreamMetadata(metadata);

        // Make sure that what we pull back is accurate.
        final StreamMetadata persistedMetadata = _streamStateDao.findStreamMetadataById(uuid);
        assertMetadataSimilar(metadata, persistedMetadata);

        // Update the data.
        final StreamMetadata updatedMetadata = createMetadata(uuid, StreamStatus.IN_PROGRESS, 11447780L, System.currentTimeMillis(), System.currentTimeMillis());
        _streamStateDao.saveOrUpdateStreamMetadata(updatedMetadata);

        // Now make sure our updated data was persisted.
        final StreamMetadata finalMetadata = _streamStateDao.findStreamMetadataById(uuid);

        // The value we're persisting should be the same as what we think we persisted (derp).
        assertMetadataSimilar(updatedMetadata, finalMetadata);

        // The ID should be the same, obviously.
        Assert.assertThat(metadata.getId(), is(finalMetadata.getId()));

        // The status and size should be updated.
        Assert.assertThat(metadata.getStatus(), is(not(equalTo(finalMetadata.getStatus()))));
        Assert.assertThat(metadata.getFileSize(), is(not(equalTo(finalMetadata.getFileSize()))));

        // These values should be relatively fresh.
        Assert.assertThat(System.currentTimeMillis() - finalMetadata.getLastModified(), lessThan(1000L));
        Assert.assertThat(System.currentTimeMillis() - finalMetadata.getCreatedTime(), lessThan(1000L));

        // And now delete it...
        _streamStateDao.deleteStreamMetadataById(uuid);
    }

    /**
     * Tests {@link StreamStateDao#deleteStreamMetadataById(String)} for the case where we're deleting a known stream.
     **/
    @Test
    public void testDeleteStreamMetadataById() throws Exception
    {
        // Create and persist our metadata.
        final String uuid = UUID.randomUUID().toString();
        final StreamMetadata metadata = createMetadata(uuid, StreamStatus.SUCCESSFUL, 8675309L, System.currentTimeMillis(), System.currentTimeMillis());

        _streamStateDao.saveOrUpdateStreamMetadata(metadata);

        // Pull it back.
        final StreamMetadata persistedMetadata =  _streamStateDao.findStreamMetadataById(uuid);

        // Make sure what we saved is the same as what we originally had.
        assertMetadataSimilar(metadata, persistedMetadata);

        // Delete the stream for the ID.
        _streamStateDao.deleteStreamMetadataById(uuid);

        // Make sure we're not pulling anything back.
        assertMetadataUnknown(_streamStateDao.findStreamMetadataById(uuid), uuid);
    }

    /**
     * Tests {@link StreamStateDao#deleteStreamMetadataById(String)} for the case where we're trying to delete an unknown
     * stream. This should have no effect.
     **/
    @Test
    public void testDeleteStreamMetadataByIdForUnknownId() throws Exception
    {
        final String uuid = UUID.randomUUID().toString();

        // Nothing here before, or after.
        assertMetadataUnknown(_streamStateDao.findStreamMetadataById(uuid), uuid);
        _streamStateDao.deleteStreamMetadataById(uuid);
        assertMetadataUnknown(_streamStateDao.findStreamMetadataById(uuid), uuid);
    }

    /**
     * Tests {@link StreamStateDao#findStreamMetadata()} for the happy path.
     **/
    @Test
    public void testFindStreamMetadata() throws Exception
    {
        // Grab our metadata, in a reproducible sort order.
        final List<StreamMetadata> metadata = _streamStateDao.findStreamMetadata().stream()
                                                             .sorted((lhs, rhs) -> lhs.getId().compareTo(rhs.getId()))
                                                             .collect(toList());

        Assert.assertThat(2, is(metadata.size()));

        // Make sure that all the timestamps are recent...
        Assert.assertThat(System.currentTimeMillis() - metadata.get(0).getCreatedTime(), is(lessThan(1000L)));
        Assert.assertThat(System.currentTimeMillis() - metadata.get(0).getLastModified(), is(lessThan(1000L)));
        Assert.assertThat(System.currentTimeMillis() - metadata.get(1).getCreatedTime(), is(lessThan(1000L)));
        Assert.assertThat(System.currentTimeMillis() - metadata.get(1).getLastModified(), is(lessThan(1000L)));

        // Then wipe them down...
        metadata.get(0).setCreatedTime(0);
        metadata.get(0).setLastModified(0);

        metadata.get(1).setCreatedTime(0);
        metadata.get(1).setLastModified(0);

        // Then check them against a known value.
        final String knownGoodData = IOUtils.toString(getClass().getResourceAsStream("/findStreamMetadataPayload.json"));
        Assert.assertThat(_objectMapper.writeValueAsString(metadata), is(knownGoodData));
    }

    /**
     * Tests {@link StreamStateDao#findStreamMetadata()} for the case where no metadata is known to the system.
     **/
    @Test
    public void testFindStreamMetadataForEmptySystem() throws Exception
    {
        // Grab the two IDs we should have on hand.
        final StreamMetadata busy = _streamStateDao.findStreamMetadataById("BusyFile");
        final StreamMetadata done = _streamStateDao.findStreamMetadataById("DoneFile");

        // Wipe out our store.
        _streamStateDao.deleteStreamMetadataById("BusyFile");
        _streamStateDao.deleteStreamMetadataById("DoneFile");

        // We should have nothing left.
        Assert.assertThat(0, is(_streamStateDao.findStreamMetadata().size()));

        // Now put them back.
        _streamStateDao.saveOrUpdateStreamMetadata(busy);
        _streamStateDao.saveOrUpdateStreamMetadata(done);

        // Lo! and then there were two!
        Assert.assertThat(2, is(_streamStateDao.findStreamMetadata().size()));
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

    /**
     * Provides a convenience method to assert that two {@link StreamMetadata} are more or less the same.
     *
     * @param metadata The {@link StreamMetadata} we created in memory. Must not be null.
     * @param persistedMetadata The {@link StreamMetadata} we pulled back from our persistence store. Must not be null.
     */
    private void assertMetadataSimilar(final StreamMetadata metadata, final StreamMetadata persistedMetadata)
    {
        Assert.assertThat(metadata.getId(), is(persistedMetadata.getId()));
        Assert.assertThat(metadata.getStatus(), is(persistedMetadata.getStatus()));
        Assert.assertThat(metadata.getFileSize(), is(persistedMetadata.getFileSize()));
        Assert.assertThat(System.currentTimeMillis() - persistedMetadata.getLastModified(), lessThan(1000L));
        Assert.assertThat(System.currentTimeMillis() - persistedMetadata.getCreatedTime(), lessThan(1000L));
    }

    /**
     * Provides a convenience method to assert that this stream is unknown.
     *
     * @param metadata {@link StreamMetadata} to verify. Must not be null.
     * @param id The ID we expect for the stream. Must not be null, must be a valid ID.
     */
    private void assertMetadataUnknown(final StreamMetadata metadata, final String id)
    {
        Assert.assertThat(metadata.getId(), is(id));
        Assert.assertThat(metadata.getStatus(), is(StreamStatus.NOT_FOUND));

        Assert.assertThat(metadata.getFileSize(), is(0L));
        Assert.assertThat(metadata.getLastModified(), is(0L));
        Assert.assertThat(metadata.getCreatedTime(), is(0L));
    }
}
