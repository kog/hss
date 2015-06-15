package com._8x8.cloud.hss.service;

import com._8x8.cloud.hss.filter.FilterManager;
import com._8x8.cloud.hss.model.StreamMetadata;
import com._8x8.cloud.hss.model.StreamStatus;
import com._8x8.cloud.hss.persistence.IStreamStateDao;
import com._8x8.cloud.hss.persistence.StreamStateDao;
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
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

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

    /**
     * Holds a collaborating {@link IStreamStateDao} for our craven mockery.
     */
    private IStreamStateDao _streamStateDao;

    @Before
    public void setUp() throws Exception
    {
        _streamService = spy(StreamService.class);
        _filterManager = mock(FilterManager.class);
        _streamStateDao = mock(StreamStateDao.class);

        _streamService.setFilterManager(_filterManager);
        verify(_streamService).setFilterManager(_filterManager);

        _streamService.setStreamStateDao(_streamStateDao);
        verify(_streamService).setStreamStateDao(_streamStateDao);

        mockStatic(FileUtils.class);
        mockStatic(IOUtils.class);

        // We're going to test these callbacks separately.
        doNothing().when(_streamService).markStreamInProgress(any(StreamMetadata.class));
        doNothing().when(_streamService).markStreamSuccessful(any(StreamMetadata.class), any(File.class));
        doNothing().when(_streamService).markStreamFailure(any(StreamMetadata.class));
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

        Assert.assertThat(_streamService.createFileForId("id").getAbsolutePath(), is(equalTo(new File(path + "/id").getAbsolutePath())));

        verify(_streamService).setStreamStorageDirectory(anyString());
        verify(_streamService).createFileForId(anyString());

        // Make sure we're actually pulling our configured value.
        verify(_streamService).getStreamStorageDirectory();

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests the happy path of {@link StreamService#getStreamById(String, List)}.
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
     * Tests {@link StreamService#getStreamById(String, List)} for the case where the requested file is not found.
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
     * Tests {@link StreamService#getStreamById(String, List)} for the case where the requested file failed to upload.
     **/
    @Test
    public void testGetStreamByIdWhenUploadFailed() throws Exception
    {
        doReturn(StreamStatus.FAILED).when(_streamService).getStatusForStreamById(anyString());

        _streamService.getStreamById("test", Arrays.asList("foo", "bar"));

        verify(_streamService).getStreamById(anyString(), anyListOf(String.class));
        verify(_streamService).getStatusForStreamById("test");

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#getStreamById(String, List)} for the case where the requested file is in progress.
     **/
    @Test
    public void testGetStreamByIdWhenUploadInProgress() throws Exception
    {
        doReturn(StreamStatus.IN_PROGRESS).when(_streamService).getStatusForStreamById(anyString());

        _streamService.getStreamById("test", Arrays.asList("foo", "bar"));

        verify(_streamService).getStreamById(anyString(), anyListOf(String.class));
        verify(_streamService).getStatusForStreamById("test");

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#getStatusForStreamById(String)} for the case where an exception is thrown. This would be
     * something like an invalid filter combination being selected. We want to make sure we're closing our streams, since
     * the MessageBodyWriter will never actually be called.
     **/
    @Test
    public void testGetStreamIdThrowsException() throws Exception
    {
        // Wire up some stuff we'll need.
        doReturn(StreamStatus.SUCCESSFUL).when(_streamService).getStatusForStreamById(anyString());
        doReturn(new File("asdf")).when(_streamService).createFileForId(anyString());

        // Bail when we're trying to get our filters.
        doThrow(new RuntimeException("moo")).when(_streamService).getFilterManager();

        try
        {
            _streamService.getStreamById("test", Arrays.asList("foo", "bar"));
            Assert.fail("Whoops, we should have caught an exception here.");
        }
        catch (final RuntimeException ex)
        {
            Assert.assertThat("moo", is(ex.getMessage()));
        }

        // We're still going to call our service calls.
        verify(_streamService).getStreamById(anyString(), anyListOf(String.class));
        verify(_streamService).getStatusForStreamById("test");
        verify(_streamService).createFileForId("test");
        verify(_streamService).getFilterManager();

        // But no filters.
        verify(_filterManager, times(0)).prepareInputFilters(any(InputStream.class), anyListOf(String.class));

        // We're going to hit both of these.
        verifyStatic(times(1));
        FileUtils.openInputStream(any(File.class));

        verifyStatic(times(1));
        IOUtils.closeQuietly(any(InputStream.class));

        // And that should be it.
        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#getStatusForStreamById(String)} to make sure it does what we expect.
     **/
    @Test
    public void testGetStatusForStreamById() throws Exception
    {
        // Cough. NPE only occurs in test code.
        doReturn(new StreamMetadata()).when(_streamStateDao).findStreamMetadataById(anyString());

        _streamService.getStatusForStreamById("foo");

        verify(_streamService).getStatusForStreamById("foo");
        verify(_streamService).getStreamStateDao();

        verify(_streamStateDao).findStreamMetadataById("foo");

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#saveStream(String, InputStream, List)} for the happy path.
     **/
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testSaveStream() throws Exception
    {
        final File file = spy(new File("asdf"));
        doReturn(4096L).when(file).length();
        doReturn(file).when(_streamService).createFileForId(anyString());

        // Pretend this file was previously successfully uploaded.
        final StreamMetadata metadata = new StreamMetadata();
        metadata.setStatus(StreamStatus.SUCCESSFUL);

        doReturn(metadata).when(_streamStateDao).findStreamMetadataById(anyString());

        // Call our method.
        _streamService.saveStream("asdf", mock(InputStream.class), Arrays.asList("curly", "shemp"));

        // Make sure it did what it should...
        verify(_streamService).saveStream(anyString(), any(InputStream.class), anyListOf(String.class));
        verify(_streamService).getStreamStateDao();
        verify(_streamService).markStreamInProgress(any(StreamMetadata.class));
        verify(_streamService).createFileForId(anyString());
        verify(_streamService).markStreamSuccessful(any(StreamMetadata.class), any(File.class));

        // We're going to call the stream status DAO to find the status for our ID.
        verify(_streamStateDao).findStreamMetadataById("asdf");

        // Filtering occurs.
        verify(_streamService).getFilterManager();
        verify(_filterManager).prepareOutputFilters(any(OutputStream.class), anyListOf(String.class));

        // Some static IO helpers
        verifyStatic(times(1));
        FileUtils.openOutputStream(any(File.class));

        verifyStatic(times(1));
        IOUtils.copyLarge(any(InputStream.class), any(OutputStream.class));

        verifyNoMoreCollaboratingInteractions();
    }
    /**
     * Tests {@link StreamService#saveStream(String, InputStream, List)} being called on a stream that is in progress.
     * This should do nothing.
     **/
    @Test
    public void testSaveStreamInProgress() throws Exception
    {
        // Pretend this file was previously successfully uploaded.
        final StreamMetadata metadata = new StreamMetadata();
        metadata.setStatus(StreamStatus.IN_PROGRESS);

        doReturn(metadata).when(_streamStateDao).findStreamMetadataById(anyString());

        // Call our method.
        _streamService.saveStream("asdf", mock(InputStream.class), Arrays.asList("curly", "shemp"));

        // Make sure it did what it should...
        verify(_streamService).saveStream(anyString(), any(InputStream.class), anyListOf(String.class));
        verify(_streamService).getStreamStateDao();
        verify(_streamStateDao).findStreamMetadataById("asdf");

        // We're not doing any file IO, so these shouldn't get called.
        verifyStatic(times(0));
        FileUtils.openOutputStream(any(File.class));

        verifyStatic(times(0));
        IOUtils.copyLarge(any(InputStream.class), any(OutputStream.class));

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#saveStream(String, InputStream, List)} for the case where it throws an exception during
     * the persistence. In this case, we want to make sure we're properly setting the status as failed. We're also going
     * to re-throw the exception as there's no unified policy in place for exception management...
     **/
    @Test
    public void testSaveStreamThrowsException() throws Exception
    {
        // Fail when we're trying to save.
        doThrow(new RuntimeException("moo")).when(_streamService).createFileForId(anyString());

        // Pretend this file was previously successfully uploaded.
        final StreamMetadata metadata = new StreamMetadata();
        metadata.setStatus(StreamStatus.SUCCESSFUL);

        doReturn(metadata).when(_streamStateDao).findStreamMetadataById(anyString());

        // Call our method.
        try
        {
            _streamService.saveStream("asdf", mock(InputStream.class), Arrays.asList("curly", "shemp"));
            Assert.fail("Hm, there should be an exception here...");
        }
        catch (final RuntimeException ex)
        {
            Assert.assertThat(ex.getMessage(), is("moo"));
        }

        // Make sure it did what it should...
        verify(_streamService).saveStream(anyString(), any(InputStream.class), anyListOf(String.class));
        verify(_streamService).getStreamStateDao();
        verify(_streamService).markStreamInProgress(any(StreamMetadata.class));
        verify(_streamService).createFileForId(anyString());
        verify(_streamService).markStreamFailure(any(StreamMetadata.class));

        // We're going to call the stream status DAO to find the status for our ID.
        verify(_streamStateDao).findStreamMetadataById("asdf");

        // NO IO occurs because we failed prior to getting here.
        verifyStatic(times(0));
        FileUtils.openOutputStream(any(File.class));

        verifyStatic(times(0));
        IOUtils.copyLarge(any(InputStream.class), any(OutputStream.class));

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#deleteStream(String)} for the case where the file has been successfully uploaded.
     */
    @Test
    public void testDeleteStreamForSuccess() throws Exception
    {
        // Pretend we've got the file on hand.
        doReturn(StreamStatus.SUCCESSFUL).when(_streamService).getStatusForStreamById(anyString());

        _streamService.deleteStream("asdf");

        verify(_streamService).deleteStream("asdf");
        verify(_streamService).getStatusForStreamById("asdf");
        verify(_streamService).getStreamStateDao();
        verify(_streamService).createFileForId("asdf");
        verify(_streamService).getStreamStorageDirectory();

        verify(_streamStateDao).deleteStreamMetadataById("asdf");

        verifyStatic(times(1));
        FileUtils.deleteQuietly(any(File.class));

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#deleteStream(String)} for the case where the file has failed to upload. This should
     * also work, as no current operations are being done on the file.
     */
    @Test
    public void testDeleteStreamForFailure() throws Exception
    {
        // Pretend we've got the file on hand.
        doReturn(StreamStatus.FAILED).when(_streamService).getStatusForStreamById(anyString());

        _streamService.deleteStream("asdf");

        verify(_streamService).deleteStream("asdf");
        verify(_streamService).getStatusForStreamById("asdf");
        verify(_streamService).getStreamStateDao();
        verify(_streamService).createFileForId("asdf");
        verify(_streamService).getStreamStorageDirectory();

        verify(_streamStateDao).deleteStreamMetadataById("asdf");

        verifyStatic(times(1));
        FileUtils.deleteQuietly(any(File.class));

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#deleteStream(String)} for the case where the file is unknown. This should be pretty
     * much a no-op.
     **/
    @Test
    public void testDeleteStreamForFileNotFound() throws Exception
    {
        doReturn(StreamStatus.NOT_FOUND).when(_streamService).getStatusForStreamById(anyString());

        _streamService.deleteStream("asdf");

        verify(_streamService).deleteStream("asdf");
        verify(_streamService).getStatusForStreamById("asdf");

        verifyStatic(times(0));
        FileUtils.forceDelete(any(File.class));

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#deleteStream(String)} for the case where the file is busy. This should be pretty
     * much a no-op.
     **/
    @Test
    public void testDeleteStreamForFileInProgress() throws Exception
    {
        doReturn(StreamStatus.IN_PROGRESS).when(_streamService).getStatusForStreamById(anyString());

        _streamService.deleteStream("asdf");

        verify(_streamService).deleteStream("asdf");
        verify(_streamService).getStatusForStreamById("asdf");

        verifyStatic(times(0));
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

    /**
     * Tests {@link StreamService#getMetadataForStreams()} to make sure it does what we expect.
     */
    @Test
    public void testGetMetadataForStreams() throws Exception
    {
        _streamService.getMetadataForStreams();

        verify(_streamService).getMetadataForStreams();
        verify(_streamService).getStreamStateDao();

        verify(_streamStateDao).findStreamMetadata();

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#getMetadataForStreamById(String)} to make sure it does what we expect.
     **/
    @Test
    public void testGetMetadataForStreamById() throws Exception
    {
        _streamService.getMetadataForStreamById("foo");

        verify(_streamService).getMetadataForStreamById("foo");
        verify(_streamService).getStreamStateDao();

        verify(_streamStateDao).findStreamMetadataById("foo");

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link StreamService#markStreamFailure(StreamMetadata)} to make sure it does what we expect.
     **/
    @Test
    public void testMarkStreamFailure() throws Exception
    {
        // Hook this back up.
        doCallRealMethod().when(_streamService).markStreamFailure(any(StreamMetadata.class));

        final StreamMetadata metadata = new StreamMetadata();
        metadata.setStatus(StreamStatus.SUCCESSFUL);

        Assert.assertThat(metadata.getStatus(), is(StreamStatus.SUCCESSFUL));

        _streamService.markStreamFailure(metadata);

        verify(_streamService).markStreamFailure(metadata);
        verify(_streamService).getStreamStateDao();

        verify(_streamStateDao).saveOrUpdateStreamMetadata(metadata);

        verifyNoMoreCollaboratingInteractions();

        // We should have set the status to failed here.
        Assert.assertThat(metadata.getStatus(), is(StreamStatus.FAILED));
    }

    /**
     * Tests {@link StreamService#markStreamSuccessful(StreamMetadata, File)} to make sure it does what we expect.
     **/
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testMarkStreamSuccessful() throws Exception
    {
        doCallRealMethod().when(_streamService).markStreamSuccessful(any(StreamMetadata.class), any(File.class));

        final StreamMetadata metadata = new StreamMetadata();
        metadata.setStatus(StreamStatus.IN_PROGRESS);

        final File file = spy(new File("asdf"));
        doReturn(4096L).when(file).length();

        Assert.assertThat(metadata.getStatus(), is(StreamStatus.IN_PROGRESS));
        Assert.assertThat(metadata.getFileSize(), is(0L));

        _streamService.markStreamSuccessful(metadata, file);

        verify(_streamService).markStreamSuccessful(metadata, file);
        verify(_streamService).getStreamStateDao();

        verify(_streamStateDao).saveOrUpdateStreamMetadata(metadata);

        verifyNoMoreCollaboratingInteractions();

        // We're going to set this to successful, and the file size to whatever our input file was.
        Assert.assertThat(metadata.getStatus(), is(StreamStatus.SUCCESSFUL));
        Assert.assertThat(metadata.getFileSize(), is(4096L));
    }

    /**
     * Tests {@link StreamService#markStreamInProgress(StreamMetadata)} to make sure it does what we expect.
     **/
    @Test
    public void testMarkStreamInProgress() throws Exception
    {
        doCallRealMethod().when(_streamService).markStreamInProgress(any(StreamMetadata.class));

        final StreamMetadata metadata = new StreamMetadata();
        metadata.setStatus(StreamStatus.SUCCESSFUL);

        Assert.assertThat(metadata.getStatus(), is(StreamStatus.SUCCESSFUL));

        _streamService.markStreamInProgress(metadata);

        verify(_streamService).markStreamInProgress(metadata);
        verify(_streamService).getStreamStateDao();

        verify(_streamStateDao).saveOrUpdateStreamMetadata(metadata);

        verifyNoMoreCollaboratingInteractions();

        // We should have set the status to failed here.
        Assert.assertThat(metadata.getStatus(), is(StreamStatus.IN_PROGRESS));
    }

    private void verifyNoMoreCollaboratingInteractions()
    {
        verifyNoMoreInteractions(_streamService, _filterManager, _streamStateDao);
    }
}
