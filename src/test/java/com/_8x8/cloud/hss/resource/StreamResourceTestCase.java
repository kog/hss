package com._8x8.cloud.hss.resource;

import com._8x8.cloud.hss.model.StreamStatus;
import com._8x8.cloud.hss.service.StreamService;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * Tests the {@link StreamResource} at the unit level.
 *
 * @author kog@epiphanic.org
 * @since 05/28/2015
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(IOUtils.class)
public class StreamResourceTestCase
{
    /**
     * Holds an instance of the class under test.
     */
    private StreamResource _resource;

    /**
     * Holds our collaborating {@link StreamService}.
     */
    private StreamService _streamService;

    /**
     * Holds a mocked {@link InputStream} we can do unspeakable things to.
     */
    private InputStream _inputStream;

    @Before
    public void setUp() throws Exception
    {
        _resource = spy(new StreamResource());
        _streamService = mock(StreamService.class);
        _inputStream = mock(InputStream.class);

        _resource.setStreamService(_streamService);
        verify(_resource).setStreamService(_streamService);

        // We're going to ignore this for most of our tests.
        doNothing().when(_resource).validateId(anyString());

        // Make sure we're properly mocking IOUtils, for it is ridiculous. Verily.
        mockStatic(IOUtils.class);
    }

    /**
     * Tests {@link StreamResource#validateId(String)} to make sure it behaves as expected.
     */
    @Test
    public void testValidateId() throws Exception
    {
        doCallRealMethod().when(_resource).validateId(anyString());

        // Try a couple of easy test cases.
        runValidateTestCase("aBcD", true);
        runValidateTestCase("8675309-jenny", true);
        runValidateTestCase("blah41314!_-.*()stilvalid", true);

        // And these should not be valid.
        runValidateTestCase("aBcD@@taters", false);
        runValidateTestCase("8675309-jenny@$%", false);
        runValidateTestCase("blah41314[]!_-.*()stilvalid", false);
    }

    /**
     * Tests {@link StreamResource#getStreamById(String, List)} for the happy path.
     */
    @Test
    public void testGetStreamById() throws Exception
    {
        // We're going to pretend we have this stream.
        doReturn(mock(InputStream.class)).when(_streamService).getStreamById(anyString(), anyListOf(String.class));

        final Response response = _resource.getStreamById("testvendor", Arrays.asList("some", "filters"));

        // Verify interactions.
        verify(_resource).getStreamById(anyString(), anyListOf(String.class));
        verify(_resource).validateId("testvendor");
        verify(_resource).getStreamService();

        verify(_streamService).getStreamById(anyString(), anyListOf(String.class));

        verifyNoMoreCollaborations();

        // Make sure we got back our 200/OK.
        Assert.assertThat(response.getStatus(), is(equalTo(Response.Status.OK.getStatusCode())));
    }

    /**
     * Tests {@link StreamResource#getStreamById(String, List)} for the case where the ID is not known to the system.
     */
    @Test
    public void testGetStreamByIdForUnknownId() throws Exception
    {
        final Response response = _resource.getStreamById("testvendor", Arrays.asList("some", "filters"));

        // Verify interactions.
        verify(_resource).getStreamById(anyString(), anyListOf(String.class));
        verify(_resource).validateId("testvendor");
        verify(_resource).getStreamService();

        verify(_streamService).getStreamById(anyString(), anyListOf(String.class));

        verifyNoMoreCollaborations();

        // Our stream service didn't find anything, so we should get back a 404/NOT FOUND.
        Assert.assertThat(response.getStatus(), is(equalTo(Response.Status.NOT_FOUND.getStatusCode())));
    }

    /**
     * Tests {@link StreamResource#getStreamById(String, List)} for the case where validating the ID throws a
     * {@link WebApplicationException} with a status of 403/FORBIDDEN.
     */
    @Test
    public void testGetStreamByIdThrowsException() throws Exception
    {
        doCallRealMethod().when(_resource).validateId(anyString());

        try
        {
            _resource.getStreamById("invalid@path", Arrays.asList("some", "filters"));
            Assert.fail("Whoops, should have caught an exception here...");
        }
        catch(final WebApplicationException ex)
        {
            // We should get a 403/FORBIDDEN here.
            Assert.assertThat(ex.getResponse().getStatus(), is(equalTo(Response.Status.FORBIDDEN.getStatusCode())));
        }

        verify(_resource).getStreamById(anyString(), anyListOf(String.class));
        verify(_resource).validateId(anyString());

        verifyNoMoreCollaborations();
    }

    /**
     * Tests {@link StreamResource#createStream(UriInfo, String, List, InputStream)} for the happy path.
     */
    @Test
    public void testCreateStream() throws Exception
    {
        // Make sure we don't collide with a file already on hand...
        doReturn(StreamStatus.NOT_FOUND).when(_streamService).getStatusForStreamById(anyString());

        // Mock a URI Info object to return.
        final UriInfo uriInfo = mock(UriInfo.class);
        doReturn(URI.create("http://not.real.host/hss/awesomeUrl")).when(uriInfo).getAbsolutePath();

        final Response response = _resource.createStream(uriInfo, "someId", Arrays.asList("some", "Filters"), _inputStream);

        verify(_resource).createStream(any(UriInfo.class), anyString(), anyListOf(String.class), any(InputStream.class));
        verify(_resource).validateId("someId");

        // We're actually going to get this twice: first to see if we've already got the stream (409/CONFLICT), then to save it.
        verify(_resource, times(2)).getStreamService();

        verify(_streamService).getStatusForStreamById(anyString());
        verify(_streamService).saveStream(anyString(), any(InputStream.class), anyListOf(String.class));

        // Make sure that we grabbed our absolute path off our URI. The URL for getting the document should exactly match
        // the URL for creating it: just different verbs. This might be a problem if we were in a VIP, but for us it's fine -
        // especially since there's no cluster/shared storage.
        verify(uriInfo).getAbsolutePath();

        // Nothing else should have happened here.
        verifyNoMoreCollaborations();
        verifyNoMoreInteractions(uriInfo);

        // Make sure we get a 201/CREATED, and that our location points to the proper URI.
        Assert.assertThat(response.getStatus(), is(equalTo(Response.Status.CREATED.getStatusCode())));
        Assert.assertThat(response.getLocation().toASCIIString(), is(equalTo(uriInfo.getAbsolutePath().toASCIIString())));

        // And make sure we closed our stream...
        verifyStatic(times(1));
        IOUtils.closeQuietly(_inputStream);
    }

    /**
     * Tests {@link StreamResource#createStream(UriInfo, String, List, InputStream)} for the case where the stream ID
     * already exists. Should return a 409/CONFLICT here.
     */
    @Test
    public void testCreateStreamForIdThatAlreadyExists() throws Exception
    {
        // We've already got a file here.
        doReturn(StreamStatus.SUCCESSFUL).when(_streamService).getStatusForStreamById(anyString());

        final Response response = _resource.createStream(mock(UriInfo.class), "someId", Arrays.asList("some", "Filters"), _inputStream);

        verify(_resource).createStream(any(UriInfo.class), anyString(), anyListOf(String.class), any(InputStream.class));
        verify(_resource).validateId("someId");

        // We're not going to save, so this should happen once.
        verify(_resource, times(1)).getStreamService();

        verify(_streamService).getStatusForStreamById(anyString());

        // Nothing else should have happened here.
        verifyNoMoreCollaborations();

        // We should get back a 409/CONFLICT here.
        Assert.assertThat(response.getStatus(), is(equalTo(Response.Status.CONFLICT.getStatusCode())));
        Assert.assertThat(response.getLocation(), is(nullValue()));

        // But we should still close our stream.
        verifyStatic(times(1));
        IOUtils.closeQuietly(_inputStream);
    }

    /**
     * Tests {@link StreamResource#createStream(UriInfo, String, List, InputStream)} for the case where the ID is invalid.
     * This should return a 403/FORBIDDEN.
     */
    @Test
    public void testCreateStreamForInvalidId() throws Exception
    {
        doCallRealMethod().when(_resource).validateId(anyString());

        try
        {
            _resource.createStream(mock(UriInfo.class), "inv@lidId", Arrays.asList("some", "Filters"), _inputStream);
            Assert.fail("Whoops, we should have caught an exception here...");
        }
        catch(final WebApplicationException ex)
        {
            // We should get a 403/FORBIDDEN here.
            Assert.assertThat(ex.getResponse().getStatus(), is(equalTo(Response.Status.FORBIDDEN.getStatusCode())));
        }


        verify(_resource).createStream(any(UriInfo.class), anyString(), anyListOf(String.class), any(InputStream.class));
        verify(_resource).validateId(anyString());

        // Nothing else should have happened here.
        verifyNoMoreCollaborations();

        // But we should still close our stream.
        verifyStatic(times(1));
        IOUtils.closeQuietly(_inputStream);
    }

    /**
     * Tests {@link StreamResource#updateStream(String, List, InputStream)} for the happy path. We should get a 204/NO CONTENT.
     */
    @Test
    public void testUpdateStream() throws Exception
    {
        // Pretend we've seen this file.
        doReturn(StreamStatus.SUCCESSFUL).when(_streamService).getStatusForStreamById(anyString());

        final Response response = _resource.updateStream("someId", Arrays.asList("some", "Filters"), _inputStream);

        verify(_resource).updateStream(anyString(), anyListOf(String.class), any(InputStream.class));
        verify(_resource).validateId("someId");

        // We're actually going to get this twice: first to see if we're missing the stream (404/NOT FOUND), then to save it.
        verify(_resource, times(2)).getStreamService();

        verify(_streamService).getStatusForStreamById(anyString());
        verify(_streamService).saveStream(anyString(), any(InputStream.class), anyListOf(String.class));

        // Nothing else should have happened here.
        verifyNoMoreCollaborations();

        // Make sure we get a 204/NO CONTENT back.
        Assert.assertThat(response.getStatus(), is(equalTo(Response.Status.NO_CONTENT.getStatusCode())));

        // And make sure we closed our stream...
        verifyStatic(times(1));
        IOUtils.closeQuietly(_inputStream);
    }

    /**
     * Tests {@link StreamResource#updateStream(String, List, InputStream)} for the case where the ID is unknown. We should
     * return a 404/NOT FOUND here.
     */
    @Test
    public void testUpdateStreamForUnknownId() throws Exception
    {
        // Pretend we've never seen this file.
        doReturn(StreamStatus.NOT_FOUND).when(_streamService).getStatusForStreamById(anyString());

        final Response response = _resource.updateStream("someId", Arrays.asList("some", "Filters"), _inputStream);

        verify(_resource).updateStream(anyString(), anyListOf(String.class), any(InputStream.class));
        verify(_resource).validateId("someId");

        // We've never seen this stream, so we're going to return a 404/NOT FOUND.
        verify(_resource, times(1)).getStreamService();

        verify(_streamService).getStatusForStreamById(anyString());

        // Nothing else should have happened here.
        verifyNoMoreCollaborations();

        // Make sure we get a 204/NO CONTENT back.
        Assert.assertThat(response.getStatus(), is(equalTo(Response.Status.NOT_FOUND.getStatusCode())));

        // And make sure we closed our stream...
        verifyStatic(times(1));
        IOUtils.closeQuietly(_inputStream);
    }

    /**
     * Tests {@link StreamResource#updateStream(String, List, InputStream)} for the case where the ID is invalid. We should
     * return a 403/FORBIDDEN here.
     */
    @Test
    public void testUpdateStreamForInvalidId() throws Exception
    {
        doCallRealMethod().when(_resource).validateId(anyString());

        try
        {
            _resource.updateStream("inv@lidId", Arrays.asList("some", "Filters"), _inputStream);
            Assert.fail("Whoops, we should have caught an exception here...");
        }
        catch(final WebApplicationException ex)
        {
            // We should get a 403/FORBIDDEN here.
            Assert.assertThat(ex.getResponse().getStatus(), is(equalTo(Response.Status.FORBIDDEN.getStatusCode())));
        }

        verify(_resource).updateStream(anyString(), anyListOf(String.class), any(InputStream.class));
        verify(_resource).validateId(anyString());

        // Nothing else should have happened here.
        verifyNoMoreCollaborations();

        // But we should still close our stream.
        verifyStatic(times(1));
        IOUtils.closeQuietly(_inputStream);
    }

    /**
     * Tests {@link StreamResource#deleteStream(String)} for the happy path. This should return a 202/ACEPTED.
     */
    @Test
    public void testDeleteStream() throws Exception
    {
        final Response response = _resource.deleteStream("asdf");

        verify(_resource).deleteStream("asdf");
        verify(_resource).validateId("asdf");
        verify(_resource).getStreamService();

        verify(_streamService).deleteStream("asdf");

        verifyNoMoreCollaborations();

        // Make sure we got back a 202/ACCEPTED.
        Assert.assertThat(response.getStatus(), is(equalTo(Response.Status.ACCEPTED.getStatusCode())));
    }

    /**
     * Tests {@link StreamResource#deleteStream(String)} for the case where the ID is invalid. This should return a
     * 403/FORBIDDEN.
     */
    @Test
    public void testDeleteStreamForInvalidId() throws Exception
    {
        doCallRealMethod().when(_resource).validateId(anyString());

        try
        {
            _resource.deleteStream("inv@lidp@th");
            Assert.fail("Whoops, we should have caught an exception here...");
        }
        catch (final WebApplicationException ex)
        {
            // We should get back that 403/FORBIDDEN.
            Assert.assertThat(ex.getResponse().getStatus(), is(equalTo(Response.Status.FORBIDDEN.getStatusCode())));
        }

        verify(_resource).deleteStream(anyString());
        verify(_resource).validateId(anyString());

        verifyNoMoreCollaborations();
    }

    /**
     * Because we're throwing a WebApplicationException for now, this is a little wonky... wrap our assertions into
     * a convenience method for easy running.
     *
     * @param id The ID to test.
     * @param isValid <code>True</code> if the ID is considered valid, else <code>false</code>.
     */
    private void runValidateTestCase(final String id, final boolean isValid)
    {
        try
        {
            _resource.validateId(id);

            // If this isn't a valid ID, we should catch a WebApplicationException here... And if not, something
            // done broke.
            if (!isValid)
            {
                Assert.fail(String.format("Whoops, %s should fail.", id));
            }
        }
        catch (final WebApplicationException ex)
        {
            // Likewise, if this *is* a valid ID, it surely won't throw an exception.
            if (isValid)
            {
                Assert.fail(String.format("%s should be a valid ID.", id));
            }

            Assert.assertThat(403, is(equalTo(ex.getResponse().getStatus())));
        }
    }

    /**
     * Provides a convenience mechanism to verify there are no more interactions we should be aware of.
     */
    private void verifyNoMoreCollaborations()
    {
        verifyNoMoreInteractions(_resource, _streamService, _inputStream);
    }
}
