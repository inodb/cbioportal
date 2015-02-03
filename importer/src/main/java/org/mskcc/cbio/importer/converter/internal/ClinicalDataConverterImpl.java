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
package org.mskcc.cbio.importer.converter.internal;

import com.google.common.collect.*;
import org.mskcc.cbio.portal.model.ClinicalAttribute;
import org.mskcc.cbio.portal.scripts.ImportClinicalData;
import org.mskcc.cbio.importer.*;
import org.mskcc.cbio.importer.model.*;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.logging.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Class which implements the Converter interface for use
 * with TCGA clinical data generated by the Biospecimen Core Resource.
 */
public class ClinicalDataConverterImpl extends ConverterBaseImpl implements Converter
{
    class PatientFollowUpMatrixComparator implements Comparator {
        public int compare (Object o, Object o1) {
            DataMatrix matrix0 = (DataMatrix)o;
            DataMatrix matrix1 = (DataMatrix)o1;
            return matrix0.getFilename().compareTo(matrix1.getFilename());
        }
    }

	private static final Log LOG = LogFactory.getLog(ClinicalDataConverterImpl.class);

    private static final List<String> blacklistedColumns = initializeBlacklisted();
    private static List<String> initializeBlacklisted() {
        String[] blacklist = { "patient_id" };
        return Arrays.asList(blacklist);
    }

	private Config config;
	private FileUtils fileUtils;
	private CaseIDs caseIDs;
	private IDMapper idMapper;
    private SurvivalDataCalculator survivalDataCalculator;

	public ClinicalDataConverterImpl(Config config, FileUtils fileUtils,
                                     CaseIDs caseIDs, IDMapper idMapper)
    {
		this.config = config;
        this.fileUtils = fileUtils;
		this.caseIDs = caseIDs;
		this.idMapper = idMapper;

        ApplicationContext context = new ClassPathXmlApplicationContext(Admin.contextFile);
        survivalDataCalculator = (SurvivalDataCalculator)context.getBean("tcgaSurvivalDataCalculator");
	}

    @Override
	public void convertData(String portal, String runDate, Boolean applyOverrides) throws Exception
    {
		throw new UnsupportedOperationException();
	}

    @Override
	public void generateCaseLists(String portal) throws Exception
    {
		throw new UnsupportedOperationException();
	}

    @Override
	public void applyOverrides(String portal, Set<String> excludeDatatypes, boolean applyCaseLists) throws Exception
    {
		throw new UnsupportedOperationException();
    }

	@Override
	public void createStagingFile(PortalMetadata portalMetadata, CancerStudyMetadata cancerStudyMetadata,
                                  DatatypeMetadata datatypeMetadata, DataMatrix[] dataMatrices) throws Exception
    {
        List<DataMatrix> dataMatrixList = new ArrayList<DataMatrix>(Arrays.asList(dataMatrices));
        DataMatrix sampleMatrix = removeMatrix(dataMatrixList, DatatypeMetadata.CLINICAL_SAMPLE_FILE_REGEX);
        DataMatrix patientMatrix = removeMatrix(dataMatrixList, DatatypeMetadata.CLINICAL_PATIENT_FILE_REGEX);
        if (patientMatrix == null) {
            logMessage(LOG, "createStagingFile(), cannot find patient matrix, aborting...");
            return;
        }
        DataMatrix nteMatrix = removeMatrix(dataMatrixList, DatatypeMetadata.CLINICAL_NTE_FILE_REGEX);
        List<DataMatrix> followUps = getSortedFollowUpMatrices(dataMatrixList);
        if (nteMatrix != null) {
            insertNTEMatrixInFollowUpList(followUps, nteMatrix);
        }

        processPatientMatrix(cancerStudyMetadata, patientMatrix, followUps);
        processSampleMatrix(cancerStudyMetadata, sampleMatrix);
        mergeSampleIntoPatientMatrix(patientMatrix, sampleMatrix);

        logMessage(LOG, "createStagingFile(), writing staging file.");
        fileUtils.writeStagingFile(portalMetadata.getStagingDirectory(), cancerStudyMetadata, datatypeMetadata, patientMatrix);
        logMessage(LOG, "createStagingFile(), complete.");
    }

    private DataMatrix removeMatrix(List<DataMatrix> dataMatrices, Pattern p) throws Exception
    {
        for (DataMatrix dataMatrix : dataMatrices) {
            Matcher matcher = p.matcher(dataMatrix.getFilename());
            if (matcher.find()) {
                dataMatrices.remove(dataMatrix);
                return dataMatrix;
            }
        }
        return null;
    }

    private List<DataMatrix> getSortedFollowUpMatrices(List<DataMatrix> dataMatrices)
    {
        List<DataMatrix> followUps = new ArrayList<DataMatrix>(dataMatrices);
        Collections.sort(followUps, new PatientFollowUpMatrixComparator());
        return followUps;
    }

    private void insertNTEMatrixInFollowUpList(List<DataMatrix> followUps, DataMatrix nte)
    {
        int index = -1;
        for (DataMatrix followUp : followUps) {
            ++index;
            Matcher matcher = DatatypeMetadata.CLINICAL_NTE_FOLLOWUP_FILE_REGEX.matcher(followUp.getFilename());
            if (matcher.find()) {
                break;
            }
        }
        if (index != -1) {
            followUps.add(index, nte);
        }
        else {
            followUps.add(nte);
        }
    }

    private void processPatientMatrix(CancerStudyMetadata cancerStudyMetadata, DataMatrix patientMatrix, List<DataMatrix> followUps)
    {
        Map<String, ClinicalAttributesMetadata> clinicalAttributes =
            getClinicalAttributes(patientMatrix.getColumnHeaders(), true);
        config.flagMissingClinicalAttributes(cancerStudyMetadata.toString(), cancerStudyMetadata.getTumorType(),
                                             removeUnknownColumnsFromMatrix(patientMatrix, clinicalAttributes));
        addSurvivalDataToMatrix(patientMatrix, computeSurvivalData(patientMatrix, followUps));
        normalizeHeaders(patientMatrix, clinicalAttributes);
        patientMatrix.ignoreRow(0, true);
    }

    private SurvivalStatus computeSurvivalData(DataMatrix patientMatrix, List<DataMatrix> followUps)
    {
        ArrayList<DataMatrix> allMatrices = new ArrayList<DataMatrix>();
        allMatrices.add(patientMatrix);
        allMatrices.addAll(followUps);
        return survivalDataCalculator.computeSurvivalData(allMatrices);
    }

    private void addSurvivalDataToMatrix(DataMatrix dataMatrix, SurvivalStatus oss)
    {
        dataMatrix.addColumn(ClinicalAttribute.OS_STATUS, oss.osStatus);
        dataMatrix.addColumn(ClinicalAttribute.OS_MONTHS, oss.osMonths);
        dataMatrix.addColumn(ClinicalAttribute.DFS_STATUS, oss.dfStatus);
        dataMatrix.addColumn(ClinicalAttribute.DFS_MONTHS, oss.dfMonths);
    }

    private void normalizeHeaders(DataMatrix dataMatrix, Map<String, ClinicalAttributesMetadata> clinicalAttributes)
    {
        for (String externalColumnHeader : dataMatrix.getColumnHeaders()) {
            if (clinicalAttributes.containsKey(externalColumnHeader)) {
                ClinicalAttributesMetadata metadata = clinicalAttributes.get(externalColumnHeader);
                dataMatrix.renameColumn(externalColumnHeader, metadata.getNormalizedColumnHeader());
            }
        }
    }

    private Map<String, ClinicalAttributesMetadata> getClinicalAttributes(List<String> externalColumnHeaders,
                                                                          boolean addSurvival)
    {
        Map<String, ClinicalAttributesMetadata> clinicalAttributes =
            config.getClinicalAttributesMetadata(externalColumnHeaders);

        if (addSurvival) {
            // add overall survival attributes
            clinicalAttributes.put(ClinicalAttribute.OS_STATUS,
                                   config.getClinicalAttributesMetadata(ClinicalAttribute.OS_STATUS).iterator().next());
            clinicalAttributes.put(ClinicalAttribute.OS_MONTHS,
                                   config.getClinicalAttributesMetadata(ClinicalAttribute.OS_MONTHS).iterator().next());
            // add disease free attributes
            clinicalAttributes.put(ClinicalAttribute.DFS_STATUS,
                                   config.getClinicalAttributesMetadata(ClinicalAttribute.DFS_STATUS).iterator().next());
            clinicalAttributes.put(ClinicalAttribute.DFS_MONTHS,
                                   config.getClinicalAttributesMetadata(ClinicalAttribute.DFS_MONTHS).iterator().next());
        }
        
        return clinicalAttributes;
    }

    private Set<String> removeUnknownColumnsFromMatrix(DataMatrix dataMatrix, Map<String, ClinicalAttributesMetadata> clinicalAttributes)
    {
        Set<String> missingAttributes = new HashSet<String>();

        List<String> cdeIds = dataMatrix.getRowData(0);
        for (String externalColumnHeader : dataMatrix.getColumnHeaders()) {
            if (!clinicalAttributes.containsKey(externalColumnHeader)) {
                dataMatrix.ignoreColumn(externalColumnHeader, true);
                String cdeId = cdeIds.get(dataMatrix.getColumnHeaders().indexOf(externalColumnHeader));
                cdeId = cdeId.replace(ClinicalAttributesNamespace.CDE_TAG, "");
                missingAttributes.add(externalColumnHeader +
                                      ClinicalAttributesNamespace.CDE_DELIM +
                                      cdeId);
                logMessage(LOG, "removeUnknownColumnsFromMatrix(), " +
                           "unknown clinical attribute (or missing normalized mapping): " +
                           externalColumnHeader + " (" + dataMatrix.getFilename() + ")");
            }
            // tcga data has "patient_id" in addition to "bcr_patient_barcode"
            else if (blacklistedColumns.contains(externalColumnHeader)) {
                dataMatrix.ignoreColumn(externalColumnHeader, true);
            }
        }

        return missingAttributes;
    }

    private void processSampleMatrix(CancerStudyMetadata cancerStudyMetadata, DataMatrix sampleMatrix)
    {
        Map<String, ClinicalAttributesMetadata> clinicalAttributes =
            getClinicalAttributes(sampleMatrix.getColumnHeaders(), false);
        config.flagMissingClinicalAttributes(cancerStudyMetadata.toString(), cancerStudyMetadata.getTumorType(),
                                             removeUnknownColumnsFromMatrix(sampleMatrix, clinicalAttributes));
        normalizeHeaders(sampleMatrix, clinicalAttributes);
    }

    private void mergeSampleIntoPatientMatrix(DataMatrix patientMatrix, DataMatrix sampleMatrix)
    {
        addSampleColumnsToPatientMatrix(patientMatrix, sampleMatrix);           
        addSampleRowsToPatientMatrix(patientMatrix, sampleMatrix);
    }

    private void addSampleColumnsToPatientMatrix(DataMatrix patientMatrix, DataMatrix sampleMatrix)
    {
        for (String sampleColumnHeader : sampleMatrix.getColumnHeaders()) {
            patientMatrix.addColumn(sampleColumnHeader,
                                    new ArrayList<String>(patientMatrix.getNumberOfRows()));
            if (sampleMatrix.isColumnIgnored(sampleColumnHeader)) {
                patientMatrix.ignoreColumn(sampleColumnHeader, true);
            }
        }
    }

    private void addSampleRowsToPatientMatrix(DataMatrix patientMatrix, DataMatrix sampleMatrix)
    {
        int newRowDataSize = patientMatrix.getColumnHeaders().size();
        List<String> sampleIdsColumn = sampleMatrix.getColumnData(0);
        for (int lc = 0; lc < sampleIdsColumn.size(); lc++) {
            String sampleId = sampleIdsColumn.get(lc);
            String patientId = caseIDs.getPatientId(sampleId);
            List<String> patientIdsColumn = patientMatrix.getColumnData(0);
            int patientMatrixIndex = patientIdsColumn.indexOf(patientId);
            if (patientMatrixIndex == -1) {
                logMessage(LOG, "addSampleRowsToPatientMatrix(), corresponding patient id " +
                                "not found for sample: " + sampleId);
                continue;
            }
            List<String> newRowData = getNewRowData(sampleMatrix, lc,
                                                    initNewRowData(newRowDataSize));
            newRowData.set(0, patientId);
            // insert sample row after patient row
            patientMatrix.insertRow(newRowData, patientMatrixIndex+1);
        }
    }

    private ArrayList<String> getNewRowData(DataMatrix sampleMatrix, int sampleRowIndex, ArrayList<String> newRowData)
    {
        List<String> sampleRowData = sampleMatrix.getRowData(sampleRowIndex);

        int sampleRowDataIndex = -1;
        int startIndex = newRowData.size()-sampleMatrix.getColumnHeaders().size();
        for (int lc = startIndex; lc < newRowData.size(); lc++) {
            newRowData.set(lc, sampleRowData.get(++sampleRowDataIndex));
        }
        return newRowData;
    }

    private ArrayList<String> initNewRowData(int newRowDataSize)
    {
        ArrayList<String> newRowData = new ArrayList<String>(newRowDataSize);
        for (int lc = 0; lc < newRowDataSize; lc++) {
            newRowData.add("");
        }
        return newRowData;
    }
}