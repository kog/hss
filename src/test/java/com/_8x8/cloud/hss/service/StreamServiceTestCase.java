package com._8x8.cloud.hss.service;

import com._8x8.cloud.hss.filter.FilterManager;
import com._8x8.cloud.hss.model.StreamStatus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

// TODO [kog@epiphanic.org - 6/2/15]: Add cases for other statuses when added.

/**
 * Tests the {@link StreamService} at the unit level.
 *
 * @author kog@epiphanic.org
 * @since 05/30/2015
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({FileUtils.class, IOUtils.class})
public class StreamServiceTestCase
{
    /**
     * Holds an instance of the class under test.
     */
    private StreamService _streamService;

    /**
     * Holds a collaborating {@link FilterManager} we can ceaselessly mock.
     */
    private FilterManager _filterManager;

    @Before
    public void setUp() throws Exception
    {
        _streamService = spy(StreamService.class);
        _filterManager = mock(FilterManager.class);

        _streamService.setFilterManager(_filterManager);
        verify(_streamService).setFilterManager(_filterManager);

        mockStatic(FileUtils.class);
        mockStatic(IOUtils.class);
    }

    /**
     * Tests our {@link StreamService#init()} method to make sure it does what we expect.
     */
    @Test
    public void testInit() throws Exception
    {
        _streamService.init();

        verify(_streamService).init();
        verify(_streamService, times(2)).getStreamStorageDirectory();

        verifyStatic(times(1));
        FileUtils.forceMkdir(any(File.class));

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#createFileForId(String)} to make sure it behaves as expected.
     */
    @Test
    public void testCreateFileForId() throws Exception
    {
        final String path = String.format("/tmp/%s", UUID.randomUUID().toString());
        _streamService.setStreamStorageDirectory(path);

        Assert.assertThat(_streamService.createFileForId("id").getAbsolutePath(), is(equalTo(path + "/id")));

        verify(_streamService).setStreamStorageDirectory(anyString());
        verify(_streamService).createFileForId(anyString());

        // Make sure we're actually pulling our configured value.
        verify(_streamService).getStreamStorageDirectory();

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests the happy path of {@link StreamService#getStatusForStreamById(String)}.
     */
    @Test
    public void testGetStreamById() throws Exception
    {
        doReturn(StreamStatus.SUCCESSFUL).when(_streamService).getStatusForStreamById(anyString());
        doReturn(new File("asdf")).when(_streamService).createFileForId(anyString());

        _streamService.getStreamById("test", Arrays.asList("foo", "bar"));

        verify(_streamService).getStreamById(anyString(), anyListOf(String.class));
        verify(_streamService).getStatusForStreamById("test");
        verify(_streamService).getFilterManager();
        verify(_streamService).createFileForId("test");

        verify(_filterManager).prepareInputFilters(any(InputStream.class), anyListOf(String.class));

        verifyStatic(times(1));
        FileUtils.openInputStream(any(File.class));

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#getStatusForStreamById(String)} for the case where the requested file is not found.
     */
    @Test
    public void testGetStreamByIdWhenFileNotFound() throws Exception
    {
        doReturn(StreamStatus.NOT_FOUND).when(_streamService).getStatusForStreamById(anyString());

        _streamService.getStreamById("test", Arrays.asList("foo", "bar"));

        verify(_streamService).getStreamById(anyString(), anyListOf(String.class));
        verify(_streamService).getStatusForStreamById("test");

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#getStatusForStreamById(String)} for the happy path.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testGetStatusForStreamById() throws Exception
    {
        final File file = mock(File.class);
        doReturn(true).when(file).exists();

        doReturn(file).when(_streamService).createFileForId(anyString());

        // This file should exist given what we're doing above.
        Assert.assertThat(_streamService.getStatusForStreamById("asdf"), is(equalTo(StreamStatus.SUCCESSFUL)));

        verify(_streamService).getStatusForStreamById("asdf");
        verify(_streamService).createFileForId("asdf");

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#getStatusForStreamById(String)} for the case where the requested stream ID doesn't exist.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testGetStatusForStreamByIdWhenFileNotFound() throws Exception
    {
        final File file = mock(File.class);
        doReturn(false).when(file).exists();

        doReturn(file).when(_streamService).createFileForId(anyString());

        // This file, however, does not exist.
        Assert.assertThat(_streamService.getStatusForStreamById("asdf"), is(equalTo(StreamStatus.NOT_FOUND)));

        verify(_streamService).getStatusForStreamById("asdf");
        verify(_streamService).createFileForId("asdf");

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#saveStream(String, InputStream, List)} for the happy path.
     */
    @Test
    public void testSaveStream() throws Exception
    {
        doReturn(new File("asdf")).when(_streamService).createFileForId(anyString());

        final InputStream stream = mock(InputStream.class);
        _streamService.saveStream("asdf", stream, Arrays.asList("curly", "shemp"));

        verify(_streamService).saveStream(anyString(), any(InputStream.class), anyListOf(String.class));
        verify(_streamService).getFilterManager();
        verify(_streamService).createFileForId(anyString());

        verify(_filterManager).prepareOutputFilters(any(OutputStream.class), anyListOf(String.class));

        verifyStatic(times(1));
        FileUtils.openOutputStream(any(File.class));

        verifyStatic(times(1));
        IOUtils.copyLarge(any(InputStream.class), any(OutputStream.class));

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#deleteStream(String)} for the happy path.
     */
    @Test
    public void testDeleteStream() throws Exception
    {
        // Pretend we've got the file on hand.
        doReturn(StreamStatus.SUCCESSFUL).when(_streamService).getStatusForStreamById(anyString());

        _streamService.deleteStream("asdf");

        verify(_streamService).deleteStream("asdf");
        verify(_streamService).getStatusForStreamById("asdf");
        verify(_streamService).createFileForId("asdf");
        verify(_streamService).getStreamStorageDirectory();

        verifyStatic(times(1));
        FileUtils.forceDelete(any(File.class));

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#deleteStream(String)} for the case where the stream ID is not known to the
     * system. This should result in a no-op.
     */
    @Test
    public void testDeleteStreamForUnknownId() throws Exception
    {
        // This file, however, we do not know.
        doReturn(StreamStatus.NOT_FOUND).when(_streamService).getStatusForStreamById(anyString());

        _streamService.deleteStream("asdf");

        verify(_streamService).deleteStream("asdf");
        verify(_streamService).getStatusForStreamById("asdf");

        verifyStatic(times(0));
        FileUtils.forceDelete(any(File.class));

        verifyNoMoreCollaboratingInteractions();
    }

    private void verifyNoMoreCollaboratingInteractions()
    {
        verifyNoMoreInteractions(_streamService, _filterManager);
    }
}
