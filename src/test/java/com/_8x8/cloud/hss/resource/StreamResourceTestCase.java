package com._8x8.cloud.hss.resource;

import com._8x8.cloud.hss.service.StreamService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

// TODO [kog@epiphanic.org - 6/2/15]: Finish filling this in.

/**
 * Tests the {@link StreamResource} at the unit level.
 *
 * @author kog@epiphanic.org
 * @since 05/28/2015
 */
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

    @Before
    public void setUp() throws Exception
    {
        _resource = spy(new StreamResource());
        _streamService = mock(StreamService.class);

        _resource.setStreamService(_streamService);
        verify(_resource).setStreamService(_streamService);
    }

    /**
     * Tests {@link StreamResource#validateId(String)} to make sure it behaves as expected.
     */
    @Test
    public void testValidateId() throws Exception
    {
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
        verifyNoMoreInteractions(_resource, _streamService);
    }
}
