package com._8x8.cloud.hss.filter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests the {@link FilterManager} at the unit level.
 *
 * @author kog@epiphanic.org
 * @since 06/02/2015
 */
public class FilterManagerTestCase
{
    /**
     * Holds an instance of the class under test.
     */
    private FilterManager _filterManager;

    /**
     * Holds a mocked {@link InputStream} we can use for our tests.
     */
    private InputStream _inputStream;

    /**
     * Holds a mocked {@link OutputStream} we can use for our tests.
     */
    private OutputStream _outputStream;

    @Before
    public void setUp() throws Exception
    {
        // Since the class does a lot of IO related behavior, we're gonna mock this one up.
        _filterManager = mock(FilterManager.class);

        _inputStream = mock(InputStream.class);
        _outputStream = mock(OutputStream.class);
    }

    /**
     * Tests {@link FilterManager#init()} to make sure it does what we expect.
     */
    @Test
    public void testInit() throws Exception
    {
        doCallRealMethod().when(_filterManager).init();
        _filterManager.init();

        verify(_filterManager).init();
        verify(_filterManager).configureInputFilters();
        verify(_filterManager).configureOutputFilters();

        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link FilterManager#prepareInputFilters(InputStream, List)} for the happy path.
     */
    @Test
    public void testPrepareInputFilters() throws Exception
    {
        doCallRealMethod().when(_filterManager).prepareInputFilters(any(InputStream.class), anyListOf(String.class));

        // Wire up a fake filter.
        final Filter.InputFilter fakeFilter = mock(Filter.InputFilter.class);
        doReturn(Collections.singletonList(fakeFilter)).when(_filterManager).findInputFiltersByName(anyListOf(String.class));

        // Call the method under test.
        _filterManager.prepareInputFilters(_inputStream, Arrays.asList("filter", "anotherFilter"));

        // We should certainly have called the method we just called...
        verify(_filterManager).prepareInputFilters(_inputStream, Arrays.asList("filter", "anotherFilter"));

        // Make sure we applied our fake filter.
        verify(fakeFilter).apply(_inputStream);

        // Make sure we called the method that does our actual findings.
        verify(_filterManager).findInputFiltersByName(anyListOf(String.class));

        // Nothing else of note should have occurred here.
        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link FilterManager#prepareInputFilters(InputStream, List)} for the case where none of the selected filters
     * actually exist.
     */
    @Test
    public void testPrepareInputFiltersForNoMatches() throws Exception
    {
        doCallRealMethod().when(_filterManager).prepareInputFilters(any(InputStream.class), anyListOf(String.class));
        final List<String> filterNames = Arrays.asList("filter", "anotherFilter");

        // Call the method under test.
        _filterManager.prepareInputFilters(_inputStream, filterNames);

        // We should certainly have called the method we just called...
        verify(_filterManager).prepareInputFilters(_inputStream, filterNames);

        // Make sure we called the method that does our actual findings.
        verify(_filterManager).findInputFiltersByName(anyListOf(String.class));

        // Nothing else of note should have occurred here.
        verifyNoMoreCollaboratingInteractions();

        // We've got nothing up our sleeve here...
        final List<Filter.InputFilter> inputFilters = _filterManager.findInputFiltersByName(filterNames);
        Assert.assertThat(inputFilters, is(empty()));
    }

    /**
     * Tests {@link FilterManager#prepareOutputFilters(OutputStream, List)} for the happy path.
     */
    @Test
    public void testPrepareOutputFilters() throws Exception
    {
        doCallRealMethod().when(_filterManager).prepareOutputFilters(any(OutputStream.class), anyListOf(String.class));

        // Wire up a fake filter.
        final Filter.OutputFilter fakeFilter = mock(Filter.OutputFilter.class);
        doReturn(Collections.singletonList(fakeFilter)).when(_filterManager).findOutputFiltersByName(anyListOf(String.class));

        // Call the method under test.
        _filterManager.prepareOutputFilters(_outputStream, Arrays.asList("filter", "anotherFilter"));

        // We should certainly have called the method we just called...
        verify(_filterManager).prepareOutputFilters(_outputStream, Arrays.asList("filter", "anotherFilter"));

        // Make sure we applied our fake filter.
        verify(fakeFilter).apply(_outputStream);

        // Make sure we called the method that does our actual findings.
        verify(_filterManager).findOutputFiltersByName(anyListOf(String.class));

        // Nothing else of note should have occurred here.
        verifyNoMoreCollaboratingInteractions();
    }

    /**
     * Tests {@link FilterManager#prepareOutputFilters(OutputStream, List)} for the case where no matching filters are
     * found.
     */
    @Test
    public void testPrepareOutputFiltersForNoMatches() throws Exception
    {
        doCallRealMethod().when(_filterManager).prepareOutputFilters(any(OutputStream.class), anyListOf(String.class));
        final List<String> filterNames = Arrays.asList("filter", "anotherFilter");

        // Call the method under test.
        _filterManager.prepareOutputFilters(_outputStream, filterNames);

        // We should certainly have called the method we just called...
        verify(_filterManager).prepareOutputFilters(_outputStream, filterNames);

        // Make sure we called the method that does our actual findings.
        verify(_filterManager).findOutputFiltersByName(anyListOf(String.class));

        // Nothing else of note should have occurred here.
        verifyNoMoreCollaboratingInteractions();

        // We've got nothing up our sleeve here...
        final List<Filter.OutputFilter> outputFilters = _filterManager.findOutputFiltersByName(filterNames);
        Assert.assertThat(outputFilters, is(empty()));
    }

    private void verifyNoMoreCollaboratingInteractions()
    {
        verifyNoMoreInteractions(_filterManager, _inputStream, _outputStream);
    }
}
