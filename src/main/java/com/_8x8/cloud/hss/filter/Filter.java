package com._8x8.cloud.hss.filter;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides an interface for a generic filter strategy.<p/>
 *
 * Please note that passing IO streams as {@link Closeable} is somewhat of an artificial abstraction due to how Java
 * handles them. As such there are two specializations herein: {@link com._8x8.cloud.hss.filter.Filter.InputFilter} and
 * {@link com._8x8.cloud.hss.filter.Filter.OutputFilter}. Use the appropriate interface.
 *
 * @param <T> Either an {@link java.io.InputStream} or {@link java.io.OutputStream}
 *
 * @author kog@epiphanic.org
 * @since 06/01/2015
 */
public interface Filter<T extends Closeable>
{
    /**
     * Provides a method that will filter our stream via wrapping it in other streams.
     *
     * @param t A {@link Closeable} to wrap.
     *
     * @return A wrapped {@link Closeable},
     *
     * @throws Exception If execution of wrapping fails.
     */
    T apply(T t) throws Exception;

    /**
     * Provides a {@link Filter} that acts on {@link InputStream}s.
     */
    interface InputFilter extends Filter<InputStream>
    {
        // Blargh, marker interface.
    }

    /**
     * Provides a {@link Filter} that acts on {@link OutputStream}s.
     */
    interface OutputFilter extends Filter<OutputStream>
    {
        // Likewise.
    }
}
