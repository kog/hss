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

    /**
     * The stream in question is unknown to the system: no stream for the ID has been submitted.
     */
    NOT_FOUND
}
