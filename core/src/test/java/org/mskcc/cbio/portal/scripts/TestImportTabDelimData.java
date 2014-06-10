/** Copyright (c) 2012 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 * documentation provided hereunder is on an "as is" basis, and
 * Memorial Sloan-Kettering Cancer Center 
 * has no obligations to provide maintenance, support,
 * updates, enhancements or modifications.  In no event shall
 * Memorial Sloan-Kettering Cancer Center
 * be liable to any party for direct, indirect, special,
 * incidental or consequential damages, including lost profits, arising
 * out of the use of this software and its documentation, even if
 * Memorial Sloan-Kettering Cancer Center 
 * has been advised of the possibility of such damage.
*/

package org.mskcc.cbio.portal.scripts;

import junit.framework.TestCase;
import org.mskcc.cbio.portal.dao.*;
import org.mskcc.cbio.portal.model.CopyNumberStatus;
import org.mskcc.cbio.portal.model.CanonicalGene;
import org.mskcc.cbio.portal.model.GeneticAlterationType;
import org.mskcc.cbio.portal.model.GeneticProfile;
import org.mskcc.cbio.portal.scripts.ImportTabDelimData;
import org.mskcc.cbio.portal.scripts.ResetDatabase;
import org.mskcc.cbio.portal.util.ProgressMonitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * JUnit tests for ImportTabDelimData class.
 */
public class TestImportTabDelimData extends TestCase {

    /**
     * Test importing of cna_test.txt file.
     * @throws Exception All Errors.
     */
    public void testImportCnaData() throws Exception {

        // test with both values of MySQLbulkLoader.isBulkLoad()
        MySQLbulkLoader.bulkLoadOff();
        runImportCnaData();
        MySQLbulkLoader.bulkLoadOn();
        runImportCnaData();
    }
    
    private void runImportCnaData() throws DaoException, IOException{
        ResetDatabase.resetDatabase();
        DaoGeneOptimized daoGene = DaoGeneOptimized.getInstance();
        DaoGeneticAlteration dao = DaoGeneticAlteration.getInstance();
        daoGene.addGene(new CanonicalGene(207, "AKT1"));
        daoGene.addGene(new CanonicalGene(208, "AKT2"));
        daoGene.addGene(new CanonicalGene(10000, "AKT3"));
        daoGene.addGene(new CanonicalGene(369, "ARAF"));
        daoGene.addGene(new CanonicalGene(472, "ATM"));
        daoGene.addGene(new CanonicalGene(673, "BRAF"));
        daoGene.addGene(new CanonicalGene(672, "BRCA1"));
        daoGene.addGene(new CanonicalGene(675, "BRCA2"));

        ProgressMonitor pMonitor = new ProgressMonitor();
        pMonitor.setConsoleMode(false);
		// TBD: change this to use getResourceAsStream()
        File file = new File("target/test-classes/cna_test.txt");
        ImportTabDelimData parser = new ImportTabDelimData(file, ImportTabDelimData.BARRY_TARGET, 1, pMonitor);
        parser.importData();

        String value = dao.getGeneticAlteration(1, "TCGA-02-0001", 207);
        assertEquals ("0", value);
        value = dao.getGeneticAlteration(1, "TCGA-02-0007", 207);
        assertEquals ("-1", value);
        value = dao.getGeneticAlteration(1, "TCGA-06-0241", 207);
        assertEquals ("-1", value);
        value = dao.getGeneticAlteration(1, "TCGA-06-0241", 675);
        assertEquals ("2", value);
        value = dao.getGeneticAlteration(1, "TCGA-06-0148", 207);
        assertEquals ("2", value);

        int cnaStatus = Integer.parseInt(dao.getGeneticAlteration(1, "TCGA-06-0148", 207));
        assertEquals(CopyNumberStatus.COPY_NUMBER_AMPLIFICATION, cnaStatus);
        cnaStatus = Integer.parseInt(dao.getGeneticAlteration(1, "TCGA-06-0241", 675));
        assertEquals(CopyNumberStatus.COPY_NUMBER_AMPLIFICATION, cnaStatus);
        cnaStatus = Integer.parseInt(dao.getGeneticAlteration(1, "TCGA-02-0007", 207));
        assertEquals(CopyNumberStatus.HEMIZYGOUS_DELETION, cnaStatus);

        assertTrue(DaoCaseProfile.caseExistsInGeneticProfile("TCGA-02-0001", 1));
        assertTrue(DaoCaseProfile.caseExistsInGeneticProfile("TCGA-06-0241", 1));
        ArrayList caseIds = DaoCaseProfile.getAllCaseIdsInProfile(1);
        assertEquals(94, caseIds.size());
    }

    /**
     * Test importing of cna_test2.txt file.
     * This is identical to cna_test.txt, except there is no target line.
     * @throws Exception All Errors.
     */
    public void testImportCnaData2() throws Exception {
        // test with both values of MySQLbulkLoader.isBulkLoad()
        MySQLbulkLoader.bulkLoadOff();
        runImportCnaData2();
        MySQLbulkLoader.bulkLoadOn();
        runImportCnaData2();
    }
    
    private void runImportCnaData2() throws DaoException, IOException{

        ResetDatabase.resetDatabase();
        DaoGeneOptimized daoGene = DaoGeneOptimized.getInstance();
        DaoGeneticAlteration dao = DaoGeneticAlteration.getInstance();
        daoGene.addGene(new CanonicalGene(207, "AKT1"));
        daoGene.addGene(new CanonicalGene(208, "AKT2"));
        daoGene.addGene(new CanonicalGene(10000, "AKT3"));
        daoGene.addGene(new CanonicalGene(369, "ARAF"));
        daoGene.addGene(new CanonicalGene(472, "ATM"));
        daoGene.addGene(new CanonicalGene(673, "BRAF"));
        daoGene.addGene(new CanonicalGene(672, "BRCA1"));
        daoGene.addGene(new CanonicalGene(675, "BRCA2"));

        ProgressMonitor pMonitor = new ProgressMonitor();
        pMonitor.setConsoleMode(false);
		// TBD: change this to use getResourceAsStream()
        File file = new File("target/test-classes/cna_test2.txt");
        ImportTabDelimData parser = new ImportTabDelimData(file, null, 1, pMonitor);
        parser.importData();

        String value = dao.getGeneticAlteration(1, "TCGA-02-0001", 207);
        assertEquals (value, "0");
        value = dao.getGeneticAlteration(1, "TCGA-02-0007", 207);
        assertEquals (value, "-1");
        value = dao.getGeneticAlteration(1, "TCGA-06-0241", 207);
        assertEquals (value, "-1");
        value = dao.getGeneticAlteration(1, "TCGA-06-0241", 675);
        assertEquals (value, "2");
        value = dao.getGeneticAlteration(1, "TCGA-06-0148", 207);
        assertEquals (value, "2");

        int cnaStatus = Integer.parseInt(dao.getGeneticAlteration(1, "TCGA-06-0148", 207));
        assertEquals(CopyNumberStatus.COPY_NUMBER_AMPLIFICATION, cnaStatus);
        cnaStatus = Integer.parseInt(dao.getGeneticAlteration(1, "TCGA-06-0241", 675));
        assertEquals(CopyNumberStatus.COPY_NUMBER_AMPLIFICATION, cnaStatus);
        cnaStatus = Integer.parseInt(dao.getGeneticAlteration(1, "TCGA-02-0007", 207));
        assertEquals(CopyNumberStatus.HEMIZYGOUS_DELETION, cnaStatus);

        assertTrue(DaoCaseProfile.caseExistsInGeneticProfile("TCGA-02-0001", 1));
        assertTrue(DaoCaseProfile.caseExistsInGeneticProfile("TCGA-06-0241", 1));
        ArrayList caseIds = DaoCaseProfile.getAllCaseIdsInProfile(1);
        assertEquals(94, caseIds.size());
    }

    /**
     * Test importing of mrna_test file.
     * @throws Exception All Errors.
     */
    public void testImportmRnaData1() throws Exception {
        // test with both values of MySQLbulkLoader.isBulkLoad()
        MySQLbulkLoader.bulkLoadOff();
        runImportRnaData1();
        MySQLbulkLoader.bulkLoadOn();
        runImportRnaData1();
    }
    
    private void runImportRnaData1() throws DaoException, IOException{

        ResetDatabase.resetDatabase();
        DaoGeneOptimized daoGene = DaoGeneOptimized.getInstance();
        DaoGeneticAlteration dao = DaoGeneticAlteration.getInstance();

        daoGene.addGene(new CanonicalGene(780, "A"));
        daoGene.addGene(new CanonicalGene(5982, "B"));
        daoGene.addGene(new CanonicalGene(3310, "C"));
        daoGene.addGene(new CanonicalGene(7849, "D"));
        daoGene.addGene(new CanonicalGene(2978, "E"));
        daoGene.addGene(new CanonicalGene(7067, "F"));
        daoGene.addGene(new CanonicalGene(11099, "G"));
        daoGene.addGene(new CanonicalGene(675, "6352"));

        GeneticProfile geneticProfile = new GeneticProfile();

        geneticProfile.setCancerStudyId(1);
        geneticProfile.setStableId("gbm_mrna");
        geneticProfile.setGeneticAlterationType(GeneticAlterationType.MRNA_EXPRESSION);
        geneticProfile.setDatatype("CONTINUOUS");
        geneticProfile.setProfileName("MRNA Data");
        geneticProfile.setProfileDescription("mRNA Data");
        DaoGeneticProfile.addGeneticProfile(geneticProfile);

        ProgressMonitor pMonitor = new ProgressMonitor();
        pMonitor.setConsoleMode(false);
		// TBD: change this to use getResourceAsStream()
        File file = new File("target/test-classes/mrna_test.txt");
        ImportTabDelimData parser = new ImportTabDelimData(file, 1, pMonitor);
        parser.importData();
        
        String value = dao.getGeneticAlteration(1, "DD639", 2978);
        assertEquals ("2.01", value );

        value = dao.getGeneticAlteration(1, "DD638", 7849);
        assertEquals ("0.55", value );
    }
}
