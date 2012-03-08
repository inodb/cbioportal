package org.mskcc.portal.mut_diagram;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.mskcc.portal.mut_diagram.IdMappingService;

/**
 * Abstract unit test for implementations of IdMappingService.
 */
public abstract class AbstractIdMappingServiceTest {
    protected IdMappingService idMappingService;

    /**
     * Create and return a new instance of an implementation of IdMappingService to test.
     *
     * @return a new instance of an implementation of IdMappingService to test
     */
    protected abstract IdMappingService createIdMappingService();

    @Before
    public void setUp() {
        idMappingService = createIdMappingService();
    }

    @Test
    public final void testCreateIdMappingService() {
        assertNotNull(idMappingService);
    }

    @Test(expected=NullPointerException.class)
    public final void testGetUniProtIdNullHugoGeneSymbol() {
        idMappingService.getUniProtIds(null);
    }
}