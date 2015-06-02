package com._8x8.cloud.hss.filter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link FilterManager} at the integration level.
 *
 * @author kog@epiphanic.org
 * @since 06/02/2015
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath*:applicationContext.xml")
public class FilterManagerITCase
{
    /**
     * Holds an instance of the class under test.
     */
    @Resource
    private FilterManager _filterManager;
    
    /**
     * Does a round-trip test of all of our filters, including some file IO.
     */
    @Test
    public void testRoundTrip() throws Exception
    {
        // Make this file name hard to guess.
        final String uuid = UUID.randomUUID().toString();

        // Test payload.
        final List<String> testPayload = Arrays.asList("hello world", "for serious");
        final File file = new File("/tmp/" + uuid);

        // We're going to use all three filters, in this order.
        final List<String> filters = Arrays.asList("zip", "encrypt", "base64");

        // Write the file, as we would from anywhere else.
        final OutputStream baseOutputStream = spy(new FileOutputStream(file));
        try (Closeable outputStream = _filterManager.prepareOutputFilters(baseOutputStream, filters))
        {
            final OutputStream out = (OutputStream)outputStream;
            IOUtils.writeLines(testPayload, "\r\n", out);
        }

        // Make sure we called close all the way down to the bottom. It's all closeable turtles.
        verify(baseOutputStream).close();

        // Same deal with the input.
        final InputStream baseInputStream = spy(new FileInputStream(file));
        try (Closeable inputStream = _filterManager.prepareInputFilters(baseInputStream, filters))
        {
            final InputStream in = (InputStream)inputStream;
            Assert.assertThat(testPayload, is(equalTo(IOUtils.readLines(in))));
        }

        verify(baseInputStream).close();

        // Make sure that we didn't just write a plain-text file to disk...
        Assert.assertThat(testPayload, is(not(equalTo(IOUtils.readLines(new FileInputStream(file))))));

        FileUtils.forceDelete(file);
    }

    /**
     * Tests what happens when we do a round-trip when none of the filters are actually known.
     */
    @Test
    public void testRoundTripNoFiltersKnown() throws Exception
    {
        // Make this file name hard to guess.
        final String uuid = UUID.randomUUID().toString();
        final List<String> filters = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString() + "narf");

        // Test payload.
        final List<String> testPayload = Arrays.asList("A much", "different payload");
        final File file = new File("/tmp/" + uuid);

        // Write the file, as we would from anywhere else.
        final OutputStream baseOutputStream = spy(new FileOutputStream(file));
        try (Closeable outputStream = _filterManager.prepareOutputFilters(baseOutputStream, filters))
        {
            final OutputStream out = (OutputStream)outputStream;
            IOUtils.writeLines(testPayload, "\r\n", out);
        }

        // Make sure we called close all the way down to the bottom. It's all closeable turtles.
        verify(baseOutputStream).close();

        // Same deal with the input.
        final InputStream baseInputStream = spy(new FileInputStream(file));
        try (Closeable inputStream = _filterManager.prepareInputFilters(baseInputStream, filters))
        {
            final InputStream in = (InputStream)inputStream;
            Assert.assertThat(testPayload, is(equalTo(IOUtils.readLines(in))));
        }

        verify(baseInputStream).close();

        // And just to prove no filters were used...
        Assert.assertThat(testPayload, is(equalTo(IOUtils.readLines(new FileInputStream(file)))));

        FileUtils.forceDelete(file);
    }
}
