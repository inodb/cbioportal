package org.mskcc.portal.test.util;

import junit.framework.TestCase;
import org.mskcc.portal.util.SkinUtil;

/**
 * Tests the SkinUtil Class.
 *
 * @author Ethan Cerami.
 */
public class TestSkinUtil extends TestCase {

    public void test1() {
        String contactEmail = SkinUtil.getEmailContact();
        assertEquals("<span class=\"mailme\" title=\"Contact us\">" +
                "cbioportal at googlegroups dot com</span>", contactEmail);
    }

    public void test2() {
        assertEquals(true, SkinUtil.showNewsTab());
        assertEquals(true, SkinUtil.showDataTab());
        assertEquals(true, SkinUtil.showRightNavDataSets());
        assertEquals(true, SkinUtil.showRightNavExamples());
        assertEquals("Access to this portal is only available to authorized users.",
                SkinUtil.getAuthorizationMessage());
    }

}
