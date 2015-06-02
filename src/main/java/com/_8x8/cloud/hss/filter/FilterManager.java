package com._8x8.cloud.hss.filter;

import com._8x8.cloud.hss.filter.Filter.InputFilter;
import com._8x8.cloud.hss.filter.Filter.OutputFilter;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.util.stream.Collectors.toList;

// TODO [kog@epiphanic.org - 6/1/15]: Builder pattern.
// TODO [kog@epiphanic.org - 6/1/15]: Maybe add a way to list all filters? Not sure if relevant to callers.

// TODO [kog@epiphanic.org - 6/2/15]: It was somewhat unclear to me what the spec meant by "encryption" since there doesn't
// TODO [kog@epiphanic.org - 6/2/15]: seem be a very meaningful way of doing this within the context of what's described. I
// TODO [kog@epiphanic.org - 6/2/15]: went ahead and added a quick Rijndael filter, but we could swap this to anything else.

/**
 * Provides a mechanism to look up, and apply, filters.
 *
 * @author kog@epiphanic.org
 * @since 06/01/2015
 */
public class FilterManager
{
    /**
     * Holds the set of filters that may be used for "input" streams (IE: reading).
     */
    private Map<String, InputFilter> _inputFilters = new HashMap<>();

    /**
     * Holds the set of filters that may be used for "output" streams (IE: writing).
     */
    private Map<String, OutputFilter> _outputFilters = new HashMap<>();

    /**
     * Holds the shared secret we use for our asymmetric crypto. You'd never do this in the real world, but it works for
     * the purpose of pedagogy...
     */
    private final static String SHARED_SECRET = "1234567890123456";

    /**
     * Holds the algorithm we're using for our encryption. In this case, garden variety AES. It doesn't really particularly
     * matter what this is, but it's something people will probably recognize.
     */
    private final static String ENCRYPTION_ALGORITHM = "AES";

    /**
     * Provides a Spring-friendly init method to discover, and map, our filters.
     */
    public void init()
    {
        configureInputFilters();
        configureOutputFilters();
    }

    // TODO [kog@epiphanic.org - 6/1/15]: Copy and paste sucks here. See if there's a better way of doing this. Likewise with the stream vs. for-loop.
    // TODO [kog@epiphanic.org - 6/2/15]: Really wish type unification was better here... Closeable may be a red herring.

    /**
     * Given an input stream, finds the appropriate filters by name, and applies them. If no requested filters are found,
     * this is the equivalent of a no-op.
     *
     * @param stream The {@link InputStream} to wrap. Must not be null.
     * @param filterNames A list of zero or more {@link Filter} names to use. May be empty, but must not be null.
     *
     * @return A wrapped {@link InputStream}, ready for streaming. Will not be null.
     */
    public InputStream prepareInputFilters(final InputStream stream, final List<String> filterNames) throws Exception
    {
        InputStream wrappedStream = stream;

        for (final InputFilter filter : findInputFiltersByName(filterNames))
        {
            wrappedStream = filter.apply(wrappedStream);
        }

        return wrappedStream;
    }

    /**
     * Given an output stream, finds the appropriate filters by name, and applies them. If no requested filters are found,
     * this is the equivalent of a no-op.
     *
     * @param stream The @{link OutputStream} to wrap. Must not be null.
     * @param filterNames A list of zero or more {@link Filter} names to use. May be empty, but must not be null.
     *
     * @return A wrapped {@link OutputStream}, ready for streaming. Will not be null.
     */
    public OutputStream prepareOutputFilters(final OutputStream stream, final List<String> filterNames) throws Exception
    {
        OutputStream wrappedStream = stream;

        for (final OutputFilter filter : findOutputFiltersByName(filterNames))
        {
            wrappedStream = filter.apply(wrappedStream);
        }

        return wrappedStream;
    }

    /**
     * Fetches the appropriate list of {@link OutputFilter} by name.
     *
     * @param filterNames A list of zero or more {@link OutputFilter} names known to the system. May be empty, but never null.
     *
     * @return A list of zero or more matching filters for the input names. May be empty, but will never be null.
     */
    List<OutputFilter> findOutputFiltersByName(List<String> filterNames)
    {
        return filterNames.stream()
                          .map(_outputFilters::get)
                          .filter(filter -> null != filter)
                          .collect(toList());
    }

    /**
     * Fetches the appropriate list of {@link InputFilter} by name.
     *
     * @param filterNames A list of zero or more {@link InputFilter} names known to the system. May be empty, but never null.
     *
     * @return A list of zero or more matching filters for the input names. May be empty, but will never be null.
     */
    List<InputFilter> findInputFiltersByName(List<String> filterNames)
    {
        return filterNames.stream()
                          .map(_inputFilters::get)
                          .filter(filter -> null != filter)
                          .collect(toList());
    }

    /**
     * Provides a convenience method for configuring our output filters.
     */
    void configureOutputFilters()
    {
        _outputFilters.put("zip", GZIPOutputStream::new);

        _outputFilters.put("encrypt", stream -> {
            final SecretKeySpec key = new SecretKeySpec(SHARED_SECRET.getBytes(), ENCRYPTION_ALGORITHM);
            final Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            return new CipherOutputStream(stream, cipher);
        });

        _outputFilters.put("base64", Base64OutputStream::new);
    }

    /**
     * Provides a convenience method for configuring our input filters.
     */
    void configureInputFilters()
    {
        _inputFilters.put("zip", GZIPInputStream::new);

        _inputFilters.put("encrypt", stream -> {
            final SecretKeySpec key = new SecretKeySpec(SHARED_SECRET.getBytes(), ENCRYPTION_ALGORITHM);
            final Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);

            return new CipherInputStream(stream, cipher);
        });

        _inputFilters.put("base64", Base64InputStream::new);
    }
}
