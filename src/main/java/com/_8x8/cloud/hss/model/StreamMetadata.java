package com._8x8.cloud.hss.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

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
@ApiModel(value = "Provides metadata about a given stream, even if unknown.")
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

    @ApiModelProperty(value = "Stream Id", required = true)
    public String getId() { return _id; }
    public void setId(final String id) { _id = id; }

    @ApiModelProperty(value = "Stream status", required = true)
    public StreamStatus getStatus() { return _status; }
    public void setStatus(final StreamStatus status) { _status = status; }

    @ApiModelProperty(value = "Stream size, after filters have been applied", required = false)
    public long getFileSize() { return _fileSize; }
    public void setFileSize(final long fileSize) { _fileSize = fileSize; }

    @ApiModelProperty(value = "Last modification time of the stream", required = false)
    public long getLastModified() { return _lastModifiedTime; }
    public void setLastModified(final long lastModifiedDate) { _lastModifiedTime = lastModifiedDate; }

    @ApiModelProperty(value = "Creation time of the stream", required = false)
    public long getCreatedTime() { return _createdTime; }
    public void setCreatedTime(final long createdTime) { _createdTime = createdTime; }
}
