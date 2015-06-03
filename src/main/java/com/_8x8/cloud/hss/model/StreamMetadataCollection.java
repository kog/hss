package com._8x8.cloud.hss.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides a transport model for a collection of zero or more {@link StreamMetadata} entities.
 *
 * @author kog@epiphanic.org
 * @since 06/02/2015
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class StreamMetadataCollection
{
    /**
     * Holds a collection of zero or more {@link StreamMetadata}. May be empty, but will never be null.
     */
    private List<StreamMetadata> _metadata = new LinkedList<>();

    public List<StreamMetadata> getMetadata()
    {
        return _metadata;
    }

    public void setMetadata(final List<StreamMetadata> metadata)
    {
        _metadata = metadata;
    }
}
