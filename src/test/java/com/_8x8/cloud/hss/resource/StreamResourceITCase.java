package com._8x8.cloud.hss.resource;

import com._8x8.cloud.hss.model.StreamMetadata;
import com._8x8.cloud.hss.model.StreamMetadataCollection;
import com._8x8.cloud.hss.model.StreamStatus;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

/**
 * Tests the {@link StreamResource} at the integration level. Please note that you must be running HSS in a servlet
 * container to run this test.
 *
 * @author kog@epiphanic.org
 * @since 05/28/2015
 */
public class StreamResourceITCase
{
    /**
     * Holds the "prefix" to all our URLs - this is pointing to our localhost Streams resource.
     */
    private static final String URL_PREFIX = "http://localhost:8080/hss/api/streams";

    /**
     * Holds the location for our test endpoint. We're going to use this to manipulate metadata state, and grab our
     * storage location.
     */
    private static final String TEST_URL_PREFIX = "http://localhost:8080/hss/api/test";

    /**
     * Holds a {@link WebTarget} pointing at our base resource.
     */
    private WebTarget _client = ClientBuilder.newClient().target(URL_PREFIX);

    /**
     * Holds a {@link WebTarget} pointing at our test resource base.
     */
    private WebTarget _testEndpoint = ClientBuilder.newClient().target(TEST_URL_PREFIX);

    /**
     * Holds a unique ID, generated by {@link #setUp()}.
     */
    private String _uuid;

    /**
     * Holds a {@link File} that points to our storage directory, using our uuid as the name.
     */
    private File _file;

    /**
     * Holds a test payload, generated by {@link #setUp()}.
     */
    private String _testPayload;

    /**
     * Holds the directory we're using to store our files. We're going to want to do some secondary verification. This
     * is populated by hitting the testing endpoint and grabbing the location.
     */
    private String _storageDirectory;

    @Before
    public void setUp() throws Exception
    {
        _uuid = UUID.randomUUID().toString();
        _testPayload = String.format("%s Body payload etc etc", _uuid);

        // Go grab the storage directory from the server.
        _storageDirectory = _testEndpoint.path("storage").request().get().readEntity(String.class);

        _file = new File(_storageDirectory, _uuid);
    }

    @After
    public void tearDown() throws Exception
    {
        // Make sure we wipe down the metadata for the file.
        deleteStreamStatus(_file.getName());

        // Try not to leave any dangling files around if our test fails...
        if (_file.exists())
        {
            FileUtils.forceDelete(_file);
        }
    }

    /**
     * Tests {@link StreamResource#getStreamById(String, List)} for the happy path. We should get a 200/OK with our input
     * stream.
     */
    @Test
    public void testGetStreamById() throws Exception
    {
        // We're going to cheat and write the file straight to the filesystem.
        createStream(_file, IOUtils.toInputStream(_testPayload));

        // So... this should already be here.
        final Response response = _client.path(_uuid).request("application/octet-stream").get();

        // We should get a 200/OK with what we wrote to the filesystem earlier.
        Assert.assertThat(Response.Status.OK.getStatusCode(), is(response.getStatus()));
        Assert.assertThat(response.readEntity(String.class), is(_testPayload));

        // Clean up after ourselves. Hopefully.
        deleteStream(_file);
    }

    /**
     * Tests {@link StreamResource#getStreamById(String, List)} for the case where the ID is not know to the system. We should
     * get a 404/NOT FOUND here.
     */
    @Test
    public void testGetStreamByIdNotKnown() throws Exception
    {
        // Verify nothing is on the filesystem with that ID.
        Assert.assertThat(StreamStatus.NOT_FOUND, is(getStreamMetadata(_uuid).getStatus()));

        final Response response = _client.path(_uuid).request("application/octet-stream").get();

        // We should get a 404/NOT FOUND here.
        Assert.assertThat(Response.Status.NOT_FOUND.getStatusCode(), is(response.getStatus()));
    }

    /**
     * Tests {@link StreamResource#getStreamById(String, List)} for the case where the ID of the stream is considered invalid.
     * This should return a 403/FORBIDDEN.
     */
    @Test
    public void testGetStreamByIdForInvalidId() throws Exception
    {
        final Response response = _client.path("/IAmAnInvalidP@th").request("application/octet-stream").get();

        // This should be a 403/FORBIDDEN.
        Assert.assertThat(Response.Status.FORBIDDEN.getStatusCode(), is(response.getStatus()));
    }

    /**
     * Tests {@link StreamResource#getStreamById(String, List)} for the case where the stream is known, but is listed as
     * {@link StreamStatus#IN_PROGRESS}.
     **/
    @Test
    public void testGetStreamByIdForInProgress() throws Exception
    {
        // Stage a file in progress.
        createStream(_file, IOUtils.toInputStream(_testPayload));
        changeStreamStatus(_file.getName(), StreamStatus.IN_PROGRESS);

        // We should get back a 409/CONFLICT here.
        final Response response = _client.path(_file.getName()).request("application/octet-stream").get();
        Assert.assertThat(Response.Status.CONFLICT.getStatusCode(), is(response.getStatus()));

        deleteStream(_file);
    }

    /**
     * Tests {@link StreamResource#getStreamById(String, List)} for the case where the stream is known, but is listed as
     * {@link StreamStatus#FAILED}.
     **/
    @Test
    public void testGetStreamByIdForFailed() throws Exception
    {
        // Stage a file as failed.
        createStream(_file, IOUtils.toInputStream(_testPayload));
        changeStreamStatus(_file.getName(), StreamStatus.FAILED);

        // We should get back a 409/CONFLICT here.
        final Response response = _client.path(_file.getName()).request("application/octet-stream").get();
        Assert.assertThat(Response.Status.CONFLICT.getStatusCode(), is(response.getStatus()));

        deleteStream(_file);
    }

    /**
     * Tests {@link StreamResource#createStream(UriInfo, String, List, InputStream)} for the happy path. We should get a
     * 201/CREATED with a location header to the new resource.
     */
    @Test
    public void testCreateStream() throws Exception
    {
        // Verify nothing is on the filesystem with that ID.
        Assert.assertThat(false, is(_file.exists()));

        // Let's post that bad boy as app/octet-stream.
        final InputStream fauxFile = IOUtils.toInputStream(_testPayload);
        final Response response = _client.path(String.format("/%s", _uuid)).request().post(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // We should get back a 201/CREATED with a Location header to /(uuid).
        final String location = response.getLocation().toASCIIString();
        Assert.assertThat(Response.Status.CREATED.getStatusCode(), is(response.getStatus()));
        Assert.assertThat(URL_PREFIX + "/" + _uuid, is(location));

        // And if we hit that resource, we should get our file back.
        final WebTarget newClient = ClientBuilder.newClient().target(location);
        final Response getResponse = newClient.request("application/octet-stream").get();

        // We should get a 200/OK with what we wrote to the filesystem earlier.
        Assert.assertThat(Response.Status.OK.getStatusCode(), is(getResponse.getStatus()));
        Assert.assertThat(getResponse.readEntity(String.class), is(_testPayload));

        // Clean up after ourselves. Hopefully.
        deleteStream(_file);
    }

    /**
     * Tests {@link StreamResource#createStream(UriInfo, String, List, InputStream)} for the case where we're trying to create
     * a stream for an ID that already exists. We should get a 409/CONFLICT back here.
     */
    @Test
    public void testCreateStreamThatAlreadyExists() throws Exception
    {
        // We're going to cheat and write the file straight to the filesystem.
        createStream(_file, IOUtils.toInputStream(_testPayload));

        // Let's post that bad boy as app/octet-stream.
        final InputStream fauxFile = IOUtils.toInputStream(_testPayload);
        final Response response = _client.path(String.format("/%s", _uuid)).request().post(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // We should get back a 409/CONFLICT since this already exists. Obviously there should be no location header here.
        Assert.assertThat(Response.Status.CONFLICT.getStatusCode(), is(response.getStatus()));
        Assert.assertThat(response.getLocation(), is(nullValue()));

        // Clean up after ourselves. Hopefully.
        deleteStream(_file);
    }

    /**
     * Tests {@link StreamResource#createStream(UriInfo, String, List, InputStream)} for the case where we're trying to create
     * a stream for an invalid ID. We should get a 403/FORBIDDEN here.
     */
    @Test
    public void testCreateStreamForInvalidId() throws Exception
    {
        // Let's create an invalid stream name.
        final String id = "@-"+ _uuid;

        // Let's post that bad boy as app/octet-stream.
        final InputStream fauxFile = IOUtils.toInputStream(_testPayload);
        final Response response = _client.path(String.format("/%s", id)).request().post(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // No @ allowed in the path: we should get a 403/FORBIDDEN and no location header.
        Assert.assertThat(Response.Status.FORBIDDEN.getStatusCode(), is(response.getStatus()));
        Assert.assertThat(response.getLocation(), is(nullValue()));
    }

    /**
     * Tests {@link StreamResource#updateStream(String, List, InputStream)} for the happy path. We should get a 204/NO CONTENT
     * back.
     */
    @Test
    public void testUpdateStream() throws Exception
    {
        // We're going to cheat and write the file straight to the filesystem.
        createStream(_file, IOUtils.toInputStream(_testPayload));

        final String updatedPayload = UUID.randomUUID().toString() + " I AM TOTALLY UPDATED";

        // Let's put that bad boy as app/octet-stream.
        final InputStream fauxFile = IOUtils.toInputStream(updatedPayload);
        final Response response = _client.path(String.format("/%s", _uuid)).request().put(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // We should get a 204/NO CONTENT back. And there should be no content.
        Assert.assertThat(Response.Status.NO_CONTENT.getStatusCode(), is(response.getStatus()));
        Assert.assertThat(response.readEntity(Object.class), is(nullValue()));

        // The Jersey Client is actually going to represent this value as -1 instead of 0. The former is invalid per RFC-2616.
        Assert.assertThat(1, greaterThan(response.getLength()));

        // Just to be sure...
        Assert.assertThat(response.getHeaders().containsKey("Location"), is(false));

        // And now make sure what's on our filesystem matches what we thought we updated to.
        Assert.assertThat(updatedPayload, is(FileUtils.readFileToString(_file)));

        // Clean up after ourselves. Hopefully.
        deleteStream(_file);
    }

    /**
     * Tests {@link StreamResource#updateStream(String, List, InputStream)} for the case where we're trying to update an ID
     * that doesn't already exist. This should return a 404/NOT FOUND.
     */
    @Test
    public void testUpdateStreamThatDoesNotExist() throws Exception
    {
        // Verify nothing is on the filesystem with that ID.
        Assert.assertThat(false, is(_file.exists()));

        // Let's put that bad boy as app/octet-stream.
        final InputStream fauxFile = IOUtils.toInputStream(_testPayload);
        final Response response = _client.path(String.format("/%s", _uuid)).request().put(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // Can't update something that does not exist.
        Assert.assertThat(Response.Status.NOT_FOUND.getStatusCode(), is(response.getStatus()));
    }
    
    /**
     * Tests {@link StreamResource#updateStream(String, List, InputStream)} for the case where we're trying an invalid ID. We
     * should get a 403/FORBIDDEN back.
     */
    @Test
    public void testUpdateStreamForInvalidId() throws Exception
    {
        // Let's create an invalid stream name.
        final String id = "@-"+ _uuid;

        // Let's put that bad boy as app/octet-stream.
        final InputStream fauxFile = IOUtils.toInputStream(_testPayload);
        final Response response = _client.path(String.format("/%s", id)).request().put(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // An invalid character in the stream ID should return a 403/FORBIDDEN.
        Assert.assertThat(Response.Status.FORBIDDEN.getStatusCode(), is(response.getStatus()));
    }

    /**
     * Tests {@link StreamResource#updateStream(String, List, InputStream)} for the case where we're trying to update a
     * stream that's in progress.
     **/
    @Test
    public void testUpdateStreamForInProgress() throws Exception
    {
        // Stage a file in progress.
        createStream(_file, IOUtils.toInputStream(_testPayload));
        changeStreamStatus(_file.getName(), StreamStatus.IN_PROGRESS);

        // Try and update it.
        final Response response = _client.path(String.format("/%s", _uuid)).request().put(Entity.entity(IOUtils.toInputStream(_testPayload), MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // This should yield a 409/CONFLICT.
        Assert.assertThat(Response.Status.CONFLICT.getStatusCode(), is(response.getStatus()));

        // Clean up after ourselves.
        deleteStream(_file);
    }

    /**
     * Tests a round trip of {@link StreamResource#createStream(UriInfo, String, List, InputStream)},
     * {@link StreamResource#getStreamById(String, List)} for a binary object, in this case, an image.
     */
    @Test
    public void testBinaryRoundTrip() throws Exception
    {
        // Grab an MD5 of the original file;
        final String imageMD5 = DigestUtils.md5Hex(getClass().getResourceAsStream("/8x8_Logo.png"));

        // We should get a 404, since the file is not there yet.
        final Response response = _client.path(_uuid).request("application/octet-stream").get();
        Assert.assertThat(Response.Status.NOT_FOUND.getStatusCode(), is(response.getStatus()));

        // Let's create the stream.
        final Response creationResponse = _client.path(String.format("/%s", _uuid))
                                                 .request()
                                                 .post(Entity.entity(getClass().getResourceAsStream("/8x8_Logo.png"),
                                                       MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // We should get back a 201/CREATED with a Location header to /(uuid).
        final String location = creationResponse.getLocation().toASCIIString();
        Assert.assertThat(Response.Status.CREATED.getStatusCode(), is(creationResponse.getStatus()));
        Assert.assertThat(URL_PREFIX + "/" + _uuid, is(location));

        // And if we hit that resource, we should get our file back.
        final WebTarget newClient = ClientBuilder.newClient().target(location);
        final Response getResponse = newClient.request("application/octet-stream").get();

        // We should get a 200/OK our image back. We'll verify by checking the MD5.
        Assert.assertThat(Response.Status.OK.getStatusCode(), is(getResponse.getStatus()));
        Assert.assertThat(DigestUtils.md5Hex(getResponse.readEntity(InputStream.class)), is(imageMD5));

        // Clean up after ourselves. Hopefully.
        deleteStream(_file);
    }

    /**
     * Tests a round trip where our filters match, and we expect the happy path.
     */
    @Test
    public void testRoundTripWithFilters() throws Exception
    {
        // Verify nothing is on the filesystem with that ID.
        Assert.assertThat(false, is(_file.exists()));

        // Let's post that bad boy as app/octet-stream.
        final InputStream fauxFile = IOUtils.toInputStream(_testPayload);
        final Response response = _client.path(String.format("/%s", _uuid))
                                         .queryParam("filters", "zip", "encrypt", "base64")
                                         .request()
                                         .post(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // We should get back a 201/CREATED with a Location header to /(uuid).
        final String location = response.getLocation().toASCIIString();
        Assert.assertThat(Response.Status.CREATED.getStatusCode(), is(response.getStatus()));
        Assert.assertThat(URL_PREFIX + "/" + _uuid, is(location));

        // And if we hit that resource, we should get our file back.
        final WebTarget newClient = ClientBuilder.newClient().target(location);

        final Response getResponse = newClient.queryParam("filters", "zip", "encrypt", "base64")
                                              .request("application/octet-stream")
                                              .get();

        // We should get a 200/OK with what we wrote to the filesystem earlier.
        Assert.assertThat(Response.Status.OK.getStatusCode(), is(getResponse.getStatus()));
        Assert.assertThat(getResponse.readEntity(String.class), is(_testPayload));

        // Make sure we've actually done something with our filters...
        Assert.assertThat(FileUtils.readFileToString(_file), is(not(equalTo(_testPayload))));

        // Clean up after ourselves. Hopefully.
        deleteStream(_file);
    }

    // TODO [kog@epiphanic.org - 6/2/15]: Should probably come in with an ExceptionMapper here.

    /**
     * Tests a round trip where our input and output filters don't match. The outbound filters are going to fail since
     * we're going to try and unzip and decrypt something we can't. We'll get back a 500/INTERNAL SERVER ERROR here.
     */
    @Test
    public void testRoundTripWithMisMatchedFilters() throws Exception
    {
        // Verify nothing is on the filesystem with that ID.
        Assert.assertThat(false, is(_file.exists()));

        // Let's post that bad boy as app/octet-stream.
        final InputStream fauxFile = IOUtils.toInputStream(_testPayload);
        final Response response = _client.path(String.format("/%s", _uuid))
                                         .queryParam("filters", "base64")
                                         .request()
                                         .post(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // We should get back a 201/CREATED with a Location header to /(uuid).
        final String location = response.getLocation().toASCIIString();
        Assert.assertThat(Response.Status.CREATED.getStatusCode(), is(response.getStatus()));
        Assert.assertThat(URL_PREFIX + "/" + _uuid, is(location));

        // And if we hit that resource, we should get our file back.
        final WebTarget newClient = ClientBuilder.newClient().target(location);

        final Response getResponse = newClient.queryParam("filters", "zip", "encrypt", "base64")
                                              .request("application/octet-stream")
                                              .get();

        // TODO [kog@epiphanic.org - 6/14/2015]: This should probably be a 400-series error, though it's somewhat hard to
        // TODO [kog@epiphanic.org - 6/14/2015]: distinguish between why the filters failed...

        // We're going to get back a 500/INTERNAL_SERVER_ERROR here.
        Assert.assertThat(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), is(getResponse.getStatus()));

        // Demonstrate that all we've done is Base64 encoded our value.
        Assert.assertThat(FileUtils.readFileToString(_file).trim(), is((equalTo(Base64.encodeBase64String(_testPayload.getBytes())))));

        // Clean up after ourselves. Hopefully.
        deleteStream(_file);
    }

    /**
     * Tests a round trip where we've asked for filters that don't exist. This should be the same as a regular round
     * trip.
     */
    @Test
    public void testRoundTripWithUnknownFilters() throws Exception
    {
        // Verify nothing is on the filesystem with that ID.
        Assert.assertThat(false, is(_file.exists()));

        // Let's post that bad boy as app/octet-stream.
        final InputStream fauxFile = IOUtils.toInputStream(_testPayload);
        final Response response = _client.path(String.format("/%s", _uuid))
                                         .queryParam("filters", "taters", "nottaters")
                                         .request()
                                         .post(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // We should get back a 201/CREATED with a Location header to /(uuid).
        final String location = response.getLocation().toASCIIString();
        Assert.assertThat(Response.Status.CREATED.getStatusCode(), is(response.getStatus()));
        Assert.assertThat(URL_PREFIX + "/" + _uuid, is(location));

        // And if we hit that resource, we should get our file back.
        final WebTarget newClient = ClientBuilder.newClient().target(location);

        final Response getResponse = newClient.queryParam("filters", "taters", "nottaters")
                                              .request("application/octet-stream")
                                              .get();

        // We should get a 200/OK with what we wrote to the filesystem earlier.
        Assert.assertThat(Response.Status.OK.getStatusCode(), is(getResponse.getStatus()));
        Assert.assertThat(getResponse.readEntity(String.class), is(_testPayload));

        // And then demonstrate the file has had nothing done to it.
        Assert.assertThat(FileUtils.readFileToString(_file), is(_testPayload));

        // Clean up after ourselves. Hopefully.
        deleteStream(_file);
    }
    
    /**
     * Tests the Accept-Encoding header of "gzip" to demonstrate that Jersey can actually already do this. If you still
     * don't trust that this test isn't some trick of Jersey Client, you can try this via telnet pretty easily:<p/>
     *
     * Write a file to wherever you're storing the files with a text editor, in plain text. Then run HSS, and telnet
     * to localhost on port 8080. Pass in the following (with asdf being whatever file you wrote) in:
     *
     * <pre>
     *      GET /hss/api/streams/asdf HTTP/1.1
     *      Host: asdf
     *      Accept-Encoding: gzip
     *      (CRLF)
     * </pre>
     *
     * And lo... binary appears. Try it without the header, and lo! plain text appears.
     */
    @Test
    public void testGzipAcceptEncodingHeader() throws Exception
    {
        // We're going to cheat and write the file straight to the filesystem.
        createStream(_file, IOUtils.toInputStream(_testPayload));

        // Grab the response, allowing for an Accept-Encoding of gzip.
        final Response response = _client.path(_uuid)
                                         .request("application/octet-stream")
                                         .header("Accept-Encoding", "gzip")
                                         .get();

        // We should get a 200/OK with what we wrote to the filesystem earlier.
        Assert.assertThat(Response.Status.OK.getStatusCode(), is(response.getStatus()));

        // We should also get a Content-Encoding value here of "gzip."
        Assert.assertThat("gzip", is(response.getHeaderString("Content-Encoding")));

        // We're going to want this as a byte[] because we can only consume the entity once.
        final byte[] payload = response.readEntity(byte[].class);

        // Just so there's nothing up our sleeves...
        Assert.assertThat(_testPayload, is(not(equalTo(new String(payload)))));

        // Now, read that back through a gzip stream...
        try(final ByteArrayInputStream byteStream = new ByteArrayInputStream(payload);
            final GZIPInputStream gzipStream = new GZIPInputStream(byteStream))
        {
            // And voila!
            Assert.assertThat(_testPayload, is(IOUtils.toString(gzipStream)));
        }

        // Clean up after ourselves. Hopefully.
        deleteStream(_file);
    }

    /**
     * Tests {@link StreamResource#deleteStream(String)} to make sure it actually does what it should. In this case
     * it should delete the file, then return a 202/ACCEPTED.
     */
    @Test
    public void testDeleteStream() throws Exception
    {
        // Write our file out to the filesystem so we can delete it.
        createStream(_file, IOUtils.toInputStream(_testPayload));

        // So... this should already be here.
        final Response response = _client.path(_uuid).request().delete();

        // We should get a 202/ACCEPTED.
        Assert.assertThat(Response.Status.ACCEPTED.getStatusCode(), is(response.getStatus()));

        // And the file should no longer exist.
        Assert.assertThat(_file.exists(), is(false));
    }

    /**
     * Tests {@link StreamResource#getStreamMetadata()} for the happy path, where we have multiple streams.
     */
    @Test
    public void testGetStreamMetadata() throws Exception
    {
        // Put some files on the filesystem.
        final File firstFile = new File(_storageDirectory, "a"+_uuid);
        createStream(firstFile, IOUtils.toInputStream(_testPayload));

        final File secondFile = new File(_storageDirectory, "z"+UUID.randomUUID().toString());
        createStream(secondFile, IOUtils.toInputStream("Totally awesomelyrandompayloaddddd " + secondFile.getName()));

        // Go grab the statuses.
        final Response response = _client.request().get();

        // We should grab a 200/OK back, and the entity should be our metadata collection.
        Assert.assertThat(Response.Status.OK.getStatusCode(), is(response.getStatus()));
        final StreamMetadataCollection metadataCollection = response.readEntity(StreamMetadataCollection.class);

        // Make sure we have the right number of items.
        Assert.assertThat(2, is(metadataCollection.getMetadata().size()));

        // Sort them for reproducible ordering
        metadataCollection.setMetadata(metadataCollection.getMetadata().stream().sorted((lhs, rhs) -> lhs.getId().compareTo(rhs.getId())).collect(toList()));

        // Make sure the metadata matches the file.
        assertMetadataMatchesFile(firstFile, metadataCollection.getMetadata().get(0));
        assertMetadataMatchesFile(secondFile, metadataCollection.getMetadata().get(1));

        // Clean up our temp files.
        deleteStream(firstFile);
        deleteStream(secondFile);
    }

    /**
     * Tests {@link StreamResource#getStreamMetadata()} for the case where we have no known streams.
     */
    @Test
    public void testGetStreamMetadataWithNoStreams() throws Exception
    {
        // We should have nothing on disk here.
        final Response response = _client.request().get();

        // We should get back a 200/OK with an empty collection.
        Assert.assertThat(Response.Status.OK.getStatusCode(), is(response.getStatus()));
        final StreamMetadataCollection metadataCollection = response.readEntity(StreamMetadataCollection.class);

        Assert.assertThat(0, is(metadataCollection.getMetadata().size()));
    }

    /**
     * Tests {@link StreamResource#getStreamMetadataForId(String)} for the happy path. We should get back a 200/OK that
     * with metadata that is fully populated.
     */
    @Test
    public void testGetStreamMetadataForId() throws Exception
    {
        // Write the file out to disk, so we know it's there.
        createStream(_file, IOUtils.toInputStream(_testPayload));

        final Response response = _client.path(_uuid).path("status").request().get();

        // We should get back a 200/OK with metadata about our input file.
        Assert.assertThat(Response.Status.OK.getStatusCode(), is(response.getStatus()));

        final StreamMetadata metadata = response.readEntity(StreamMetadata.class);
        assertMetadataMatchesFile(_file, metadata);

        // Clean up on aisle 5.
        deleteStream(_file);
    }

    /**
     * Tests {@link StreamResource#getStreamMetadataForId(String)}for the case where the ID is unknown. We should get back
     * a status with only part of the metadata populated: non-existent files can't have a size.
     */
    @Test
    public void testGetStreamMetadataForUnknownId() throws Exception
    {
        final Response response = _client.path(_uuid).path("status").request().get();

        // We should get back a 200/OK with metadata about our input file.
        Assert.assertThat(Response.Status.OK.getStatusCode(), is(response.getStatus()));

        final StreamMetadata metadata = response.readEntity(StreamMetadata.class);

        // We'll have an ID and a status, always.
        Assert.assertThat(_uuid, is(metadata.getId()));
        Assert.assertThat(StreamStatus.NOT_FOUND, is(metadata.getStatus()));

        // We will not have a last mod time or a file size here.
        Assert.assertThat(metadata.getLastModified(), is(0L));
        Assert.assertThat(metadata.getFileSize(), is(0L));
    }

    /**
     * Tests {@link StreamResource#getStreamMetadataForId(String)} for the case where the given ID is invalid. We should
     * get back a 403/FORBIDDEN here.
     */
    @Test
    public void testGetStreamMetadataForInvalidId() throws Exception
    {
        final Response response = _client.path("badp@th").path("status").request().get();
        Assert.assertThat(Response.Status.FORBIDDEN.getStatusCode(), is(response.getStatus()));
    }

    /**
     * Now that we're returning things other than application/octet-stream, content negotiation via URLs has been
     * enabled in the web.xml. This means that for a given resource you can tack on either <code>.json</code> or
     * <code>.xml</code> and it will act as if you set the <code>Content-Type</code> header.<p/>
     *
     * By default our setup should actually provide us with XML, due to JAXB + Jersey, so we're going to test the three
     * standard cases:<p/>
     *
     * <ul>
     *     <li>No Content-Type or conneg URL specified. We should get XML here.</li>
     *     <li>Conneg URL specified as <code>.json</code>. We should get JSON here.</li>
     *     <li>Conneg URL specified as <code>.xml</code>. We should get XML here.</li>
     * </ul>
     *
     * It is also worth noting that because we use an {@link javax.xml.bind.annotation.XmlType} with a property order,
     * we can reliably do some pretty handy things with our raw content, without having to parse it.<p/>
     *
     * Lastly, we're going to use the {@link StreamResource#getStreamMetadata()} endpoint so that we can also make sure
     * that the collection looks good in both raw XML and raw JSON for binding in other languages.
     */
    @Test
    public void testContentNegotiation() throws Exception
    {
        // Write our test file.
        createStream(_file, IOUtils.toInputStream(_testPayload));

        doTestContentNegotiationTestRun("", "xml", _uuid);
        doTestContentNegotiationTestRun(".json", "json", _uuid);
        doTestContentNegotiationTestRun(".xml", "xml", _uuid);

        // Clean our test file up.
        FileUtils.forceDelete(_file);
    }

    /**
     * Provides a convenience method to do a test run for {@link #testContentNegotiation()}.
     *
     * @param urlAddition The bit to tack on to the URL for content negotiation. May be empty, but never null.
     * @param type The "type" - this should either be <code>xml</code> or <code>json</code> and never blank.
     * @param uuid The UUID for the test run. This must not be blank, and is used for token replacement.
     */
    private void doTestContentNegotiationTestRun(final String urlAddition, final String type, final String uuid) throws Exception
    {
        final Response response = ClientBuilder.newClient().target(URL_PREFIX + urlAddition).request().get();
        Assert.assertThat(Response.Status.OK.getStatusCode(), is(response.getStatus()));

        String rawData = response.readEntity(String.class);

        // Bah, if I had a proper output testing framework here...
        if ("xml".equalsIgnoreCase(type))
        {
            rawData = rawData.replaceAll("(<lastModified>)(\\d+)(</lastModified>)", "$10$3").replaceAll("(<createdTime>)(\\d+)(</createdTime>)", "$10$3");
        }
        else if ("json".equalsIgnoreCase(type))
        {
            rawData = rawData.replaceAll("(\"lastModified\":)(\\d+)", "$10").replaceAll("(\"createdTime\":)(\\d+)", "$10");
        }

        final String knownGoodData = IOUtils.toString(getClass().getResourceAsStream("/contentNegotiationPayload." + type))
                                            .replace("%UUID%", uuid);

        Assert.assertThat(rawData, is(knownGoodData));
    }

    /**
     * Provides a convenience method to make sure that our given {@link StreamMetadata} matches the {@link File} we
     * created on the filesystem.
     *
     * @param file The {@link File} to compare. Must not be null.
     * @param streamMetadata {@link StreamMetadata} about the given {@link File}. Must not be null.
     */
    private void assertMetadataMatchesFile(final File file, final StreamMetadata streamMetadata)
    {
        Assert.assertThat(file.getName(), is(streamMetadata.getId()));
        Assert.assertThat(file.length(), is(streamMetadata.getFileSize()));

        // Since our modified time from the DB is NOW(), it'll actually be after the file last mod time. So let's just
        // make sure for this test that the timestamps are close.
        Assert.assertThat(streamMetadata.getLastModified() - file.lastModified(), is(lessThan(1000L)));

        // This is pretty much invariant at the moment...
        Assert.assertThat(streamMetadata.getStatus(), is(StreamStatus.SUCCESSFUL));
    }

    /**
     * Provides a mechanism to create a stream out-of-bounds. It'll stage the file on the system, and manually insert
     * the stream state into the tracking persistence layer.
     *
     * @param file The file to persist. Must not be null.
     * @param stream The stream to pull the data from. Must not be null.
     */
    private void createStream(final File file, final InputStream stream) throws Exception
    {
        // Create the file on disk.
        FileUtils.copyInputStreamToFile(stream, file);

        // Slap in our metadata, complete with file size. Audit timestamps will be handled via the persistence mechanism.
        final StreamMetadata metadata = getStreamMetadata(file.getName());
        metadata.setStatus(StreamStatus.SUCCESSFUL);
        metadata.setFileSize(file.length());

        saveStreamMetadata(metadata);
    }

    /**
     * Provides a mechanism to remove a stream out-of-bounds. It'll remove the file from the underlying filesystem, and
     * delete the stream state from the tracking persistence layer.
     *
     * @param file The file to delete. Must not be null.
     */
    private void deleteStream(final File file) throws Exception
    {
        // Create the file on disk.
        FileUtils.forceDelete(file);

        // Delete metadata.
        deleteStreamStatus(file.getName());
    }

    /**
     * Provides a mechanism to change the state of a stream in the tracking persistence layer.
     *
     * @param id The ID of the stream to modify. Must be a valid Id.
     * @param status The {@link StreamStatus} to set for the stream.
     */
    private void changeStreamStatus(final String id, final StreamStatus status)
    {
        // Pull back the current metadata, and overwrite the status.
        final StreamMetadata metadata = getStreamMetadata(id);
        metadata.setStatus(status);

        // Write a new one.
        saveStreamMetadata(metadata);
    }

    /**
     * Provides a convenience method to delete the stream status of a given stream.
     *
     * @param id The ID of the stream to delete the status of. Must be a valid stream ID.
     */
    private void deleteStreamStatus(final String id)
    {
        final Response response = _testEndpoint.path("state").path(id).request().delete();
        Assert.assertThat(Response.Status.NO_CONTENT.getStatusCode(), is(response.getStatus()));
    }

    /**
     * Provides a convenience method that wraps {@link StreamResource#getStreamMetadataForId(String)}. We're going to
     * be calling this a few times...
     *
     * @param id The ID of the stream to get the {@link StreamMetadata} for.
     */
    private StreamMetadata getStreamMetadata(final String id)
    {
        final Response response = _client.path(id).path("status").request().get();

        Assert.assertThat(Response.Status.OK.getStatusCode(), is(response.getStatus()));
        return response.readEntity(StreamMetadata.class);
    }

    /**
     * Provides a convenience method to save {@link StreamMetadata} out-of-band.
     *
     * @param metadata The {@link StreamMetadata} to save. Must not be null.
     */
    private void saveStreamMetadata(final StreamMetadata metadata)
    {
        final Response response = _testEndpoint.path("state").request().post(Entity.entity(metadata, MediaType.APPLICATION_JSON));
        Assert.assertThat(Response.Status.NO_CONTENT.getStatusCode(), is(response.getStatus()));
    }
}
