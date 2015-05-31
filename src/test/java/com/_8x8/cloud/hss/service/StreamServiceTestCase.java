package com._8x8.cloud.hss.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

// TODO [kog@epiphanic.org - 5/30/15]: Finish when we've got collaboration for filters/actions. Also gonna want PowerMockito.

/**
 * Tests the {@link StreamService} at the unit level.
 *
 * @author kog@epiphanic.org
 * @since 05/30/2015
 */
public class StreamServiceTestCase
{
    /**
     * Holds an instance of the class under test.
     */
    private StreamService _streamService;

    @Before
    public void setUp() throws Exception
    {
        _streamService = spy(StreamService.class);
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

        // Make sure we're actually pulling our configured value.
        verify(_streamService).getStreamStorageDirectory();
    }
}
