package com._8x8.cloud.hss.service;

import com._8x8.cloud.hss.model.StreamStatus;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;

// TODO [kog@epiphanic.org - 5/29/15]: Test when unstubbed.

/**
 * Provides a concrete implementation of {@link IStreamService}.
 *
 * @author kog@epiphanic.org
 * @since 05/28/2015
 */
public class StreamService implements IStreamService
{
    // TODO [kog@epiphanic.org - 5/28/15]: Unstub me.

    @Override
    public InputStream getStreamById(String id)
    {
        if (id.startsWith("NF"))
        {
            return null;
        }

        return IOUtils.toInputStream("test pattern");
    }

    @Override
    public StreamStatus getStatusForStreamById(String id)
    {
        if (id.startsWith("NF"))
        {
            return StreamStatus.NOT_FOUND;
        }

        return StreamStatus.FOUND;
    }

    @Override
    public void saveStream(String id, InputStream stream)
    {
        // And the pill your mother gave you, does nothing at all.
    }
}
