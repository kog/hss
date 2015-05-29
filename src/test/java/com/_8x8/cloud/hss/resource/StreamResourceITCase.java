package com._8x8.cloud.hss.resource;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

// TODO [kog@epiphanic.org - 5/28/15]: Unstub. Remove the NF hackery.

// TODO [kog@epiphanic.org - 5/28/15]: Binary file (image?) round trip, filters (when added).
// TODO [kog@epiphanic.org - 5/28/15]: Verify with secondary FS access. Does Travis have any limits here? Maybe look at JimFS.


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
     * Holds a {@link WebTarget} pointing at our base resource.
     */
    private WebTarget _client = ClientBuilder.newClient().target(URL_PREFIX);

    /**
     * Tests {@link StreamResource#getStreamById(String)} for the happy path. We should get a 200/OK with our input
     * stream.
     */
    @Test
    public void testGetStreamById() throws Exception
    {
        final Response response = _client.path("/asdf").request("application/octet-stream").get();

        // We should get a 200/OK with a dummy value.
        Assert.assertThat(200, is(equalTo(response.getStatus())));
        Assert.assertThat(response.readEntity(String.class), is(equalTo("test pattern")));
    }

    /**
     * Tests {@link StreamResource#getStreamById(String)} for the case where the ID is not know to the system. We should
     * get a 404/NOT FOUND here.
     */
    @Test
    public void testGetStreamByIdNotKnown() throws Exception
    {
        final Response response = _client.path("/NF-not-here").request("application/octet-stream").get();

        // We should get a 404/NOT FOUND here.
        Assert.assertThat(404, is(equalTo(response.getStatus())));
    }

    /**
     * Tests {@link StreamResource#getStreamById(String)} for the case where the ID of the stream is considered invalid.
     * This should return a 403/FORBIDDEN.
     */
    @Test
    public void testGetStreamByIdForInvalidId() throws Exception
    {
        final Response response = _client.path("/IAmAnInvalidP@th").request("application/octet-stream").get();

        // This should be a 403/FORBIDDEN.
        Assert.assertThat(403, is(equalTo(response.getStatus())));
    }

    /**
     * Tests {@link StreamResource#createStream(UriInfo, String, InputStream)} for the happy path. We should get a
     * 201/CREATED with a location header to the new resource.
     */
    @Test
    public void testCreateStream() throws Exception
    {
        // Let's create a hard to guess input string.
        final String id = "NF-"+UUID.randomUUID().toString();
        final InputStream fauxFile = IOUtils.toInputStream(String.format("%s %s", id, "I AM A MAN, NOT A UUID"));

        // Let's post that bad boy as app/octet-stream.
        final Response response = _client.path(String.format("/%s", id)).request().post(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // We should get back a 201/CREATED with a Location header to /(uuid).
        Assert.assertThat(201, is(equalTo(response.getStatus())));
        Assert.assertThat(URL_PREFIX + "/" + id, is(equalTo(response.getLocation().toASCIIString())));
    }

    /**
     * Tests {@link StreamResource#createStream(UriInfo, String, InputStream)} for the case where we're trying to create
     * a stream for an ID that already exists. We should get a 409/CONFLICT back here.
     */
    @Test
    public void testCreateStreamThatAlreadyExists() throws Exception
    {
        // Let's create a hard to guess input string.
        final String id = UUID.randomUUID().toString();
        final InputStream fauxFile = IOUtils.toInputStream(String.format("%s %s", id, "I AM A MAN, NOT A UUID"));

        // Let's post that bad boy as app/octet-stream.
        final Response response = _client.path(String.format("/%s", id)).request().post(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // We should get back a 409/CONFLICT since this already exists. Obviously there should be no location header here.
        Assert.assertThat(409, is(equalTo(response.getStatus())));
        Assert.assertThat(response.getLocation(), is(nullValue()));
    }

    /**
     * Tests {@link StreamResource#createStream(UriInfo, String, InputStream)} for the case where we're trying to create
     * a stream for an invalid ID. We should get a 403/FORBIDDEN here.
     */
    @Test
    public void testCreateStreamForInvalidId() throws Exception
    {
        // Let's create a hard to guess input string.
        final String id = "@-"+UUID.randomUUID().toString();
        final InputStream fauxFile = IOUtils.toInputStream(String.format("%s %s", id, "I AM A MAN, NOT A UUID"));

        // Let's post that bad boy as app/octet-stream.
        final Response response = _client.path(String.format("/%s", id)).request().post(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // No @ allowed in the path: we should get a 403/FORBIDDEN and no location header.
        Assert.assertThat(403, is(equalTo(response.getStatus())));
        Assert.assertThat(response.getLocation(), is(nullValue()));
    }

    /**
     * Tests {@link StreamResource#updateStream(String, InputStream)} for the happy path. We should get a 204/NO CONTENT
     * back.
     */
    @Test
    public void testUpdateStream() throws Exception
    {
        // Let's create a hard to guess input string.
        final String id = UUID.randomUUID().toString();
        final InputStream fauxFile = IOUtils.toInputStream(String.format("%s %s", id, "I AM A MAN, NOT A UUID"));

        // Let's post that bad boy as app/octet-stream.
        final Response response = _client.path(String.format("/%s", id)).request().put(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // We should get a 204/NO CONTENT back. And there should be no content.
        Assert.assertThat(204, is(equalTo(response.getStatus())));
        Assert.assertThat(response.readEntity(Object.class), is(nullValue()));

        // The Jersey Client is actually going to represent this value as -1 instead of 0. The former is invalid per RFC-2616.
        Assert.assertThat(1, greaterThan(response.getLength()));

        // Just to be sure...
        Assert.assertThat(1, is(equalTo(response.getHeaders().size())));
        Assert.assertThat("Content-Length", not(equalTo(response.getHeaders().values().iterator().next())));
    }

    /**
     * Tests {@link StreamResource#updateStream(String, InputStream)} for the case where we're trying to update an ID
     * that doesn't already exist. This should return a 404/NOT FOUND.
     */
    @Test
    public void testUpdateStreamThatDoesNotExist() throws Exception
    {
        // Let's create a hard to guess input string.
        final String id = "NF-"+UUID.randomUUID().toString();
        final InputStream fauxFile = IOUtils.toInputStream(String.format("%s %s", id, "I AM A MAN, NOT A UUID"));

        // Let's post that bad boy as app/octet-stream.
        final Response response = _client.path(String.format("/%s", id)).request().put(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // Can't update something that does not exist.
        Assert.assertThat(404, is(equalTo(response.getStatus())));
    }
    
    /**
     * Tests {@link StreamResource#updateStream(String, InputStream)} for the case where we're trying an invalid ID. We
     * should get a 403/FORBIDDEN back.
     */
    @Test
    public void testUpdateStreamForInvalidId() throws Exception
    {
        // Let's create a hard to guess input string.
        final String id = "@-"+UUID.randomUUID().toString();
        final InputStream fauxFile = IOUtils.toInputStream(String.format("%s %s", id, "I AM A MAN, NOT A UUID"));

        // Let's post that bad boy as app/octet-stream.
        final Response response = _client.path(String.format("/%s", id)).request().put(Entity.entity(fauxFile, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // No @ allowed in the path: we should get a 403/FORBIDDEN and no location header.
        Assert.assertThat(403, is(equalTo(response.getStatus())));
    } 
}
