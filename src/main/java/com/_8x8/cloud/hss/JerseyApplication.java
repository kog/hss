package com._8x8.cloud.hss;

// TODO [kog@epiphanic.org - 05/28/15]: Build something that uses Spring to resolve classes, akin to the SpringComponentProviderFactory in Jersey 1.x

import com._8x8.cloud.hss.resource.StreamResource;
import com._8x8.cloud.hss.resource.TestingResource;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;

/**
 * Provides code-based wiring for our Jersey application.
 *
 * @author kog@epiphanic.org
 * @since 05/28/2015
 */
public class JerseyApplication extends ResourceConfig
{
    public JerseyApplication()
    {
        // Explicitly register our Resource instead of @Component and classpath scanning. Really wish we didn't have to
        // auto-wire...
        register(StreamResource.class);
        register(TestingResource.class);

        // Demonstrate that we could just use the Accept-Encoding header of gzip here...
        register(EncodingFilter.class);
        register(GZipEncoder.class);
    }
}
