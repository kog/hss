package com._8x8.cloud.hss.model;

/**
 * Provides an enumeration that yields insight into the state of a given stream. This is particularly helpful because
 * the streams themselves will probably be heavy objects.
 *
 * @author kog@epiphanic.org
 * @since 05/28/2015
 */
public enum StreamStatus
{
    /**
     * The stream in question has successfully been stored and processed. Consider this "complete" and ready for reading.
     */
    SUCCESSFUL,

    /**
     * The stream is being processed: it is either still uploading or being processed by one of the optional filters
     * (encryption, deflation).
     */
    IN_PROGRESS,

    // TODO [kog@epiphanic.org - 6/14/2015]: We might consider storing failure status with the metadata.
    // TODO [kog@epiphanic.org - 6/14/2015]: Note, again, that if we went with some sort of versioning approach we'd
    // TODO [kog@epiphanic.org - 6/14/2015]: probably mark the audit information as failed, but leave the stream ID at
    // TODO [kog@epiphanic.org - 6/14/2015]: the last valid version (if any).

    /**
     * The last file upload failed for some reason. The file should probably not be used without manual intervention.
     */
    FAILED,

    /**
     * The stream in question is unknown to the system: no stream for the ID has been submitted.
     */
    NOT_FOUND
}
