package org.mskcc.portal.test.mut_diagram;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mskcc.portal.mut_diagram.FeatureService;

/**
 * Abstract unit test for implementations of FeatureService.
 */
public abstract class AbstractFeatureServiceTest {
    protected FeatureService featureService;

    /**
     * Create and return a new instance of an implementation of FeatureService to test.
     *
     * @return a new instance of an implementation of FeatureService to test
     */
    protected abstract FeatureService createFeatureService();

    @Before
    public void setUp() {
        featureService = createFeatureService();
    }

    @Test
    public final void testCreateFeatureService() {
        assertNotNull(featureService);
    }

    @Test(expected=NullPointerException.class)
    public final void testNullUniProtId() {
        featureService.getFeatures(null);
    }
}
