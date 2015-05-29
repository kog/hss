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
    // TODO [kog@epiphanic.org - 5/28/15]: refactor this when we add in filters/actions. These are temporary.

    FOUND,
    NOT_FOUND
}
