package com._8x8.cloud.hss.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

// TODO [kog@epiphanic.org - 6/2/15]: Add in a pre-computed MD5 hash if we start to persist state information (DB, FS).

/**
 * Provides a model encapsulating information about a given stream.<p/>
 *
 * Please note that we use an explicit {@link XmlType} so that we can have reproducible ordering.
 *
 * @author kog@epiphanic.org
 * @since 06/02/2015
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(propOrder = {
        "id",
        "status",
        "fileSize",
        "lastModified",
})
public class StreamMetadata
{
    /**
     * Holds the ID of the stream.
     */
    private String _id;

    /**
     * Holds the {@link StreamStatus} of a given stream.
     */
    private StreamStatus _status;

    /**
     * Holds the size of the particular file.
     */
    private Long _fileSize;

    /**
     * Holds the time at which the file was last modified.
     */
    private Long _lastModifiedTime;

    public String getId() { return _id; }
    public void setId(final String id) { _id = id; }

    public StreamStatus getStatus() { return _status; }
    public void setStatus(final StreamStatus status) { _status = status; }

    public Long getFileSize() { return _fileSize; }
    public void setFileSize(final Long fileSize) { _fileSize = fileSize; }

    public Long getLastModified() { return _lastModifiedTime; }
    public void setLastModified(final Long lastModifiedDate) { _lastModifiedTime = lastModifiedDate; }
}
