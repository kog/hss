package com._8x8.cloud.hss.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

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
        "createdTime"
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
     * Holds the size of the particular file. Please note that this is the size of the file after filters have been
     * applied, and not the original size.
     */
    private long _fileSize;

    /**
     * Holds the time at which the file was first seen.
     */
    private long _createdTime;

    /**
     * Holds the time at which the file was last modified.
     */
    private long _lastModifiedTime;

    public String getId() { return _id; }
    public void setId(final String id) { _id = id; }

    public StreamStatus getStatus() { return _status; }
    public void setStatus(final StreamStatus status) { _status = status; }

    public long getFileSize() { return _fileSize; }
    public void setFileSize(final long fileSize) { _fileSize = fileSize; }

    public long getLastModified() { return _lastModifiedTime; }
    public void setLastModified(final long lastModifiedDate) { _lastModifiedTime = lastModifiedDate; }

    public long getCreatedTime() { return _createdTime; }
    public void setCreatedTime(final long createdTime) { _createdTime = createdTime; }
}
