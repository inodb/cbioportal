package org.mskcc.cbio.portal.util;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringValueResolver;

/**
 * Helper class for providing the application context programatically.
 * 
 * @author <a href="mailto:manuel.holtgrewe@bihealth.de">Manuel Holtgrewe</a>
 */
public class EmbeddedValueResolverProvider implements EmbeddedValueResolverAware {

    private static StringValueResolver resolver;

    public static StringValueResolver getResolver() {
        return resolver;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        EmbeddedValueResolverProvider.resolver = resolver;
    }
}