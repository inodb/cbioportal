package org.mskcc.endometrial.clinical;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Reads in MSI Status for all Cases.
 */
public class MsiReader {
    private HashMap<String, String> msi7MarkerMap = new HashMap<String, String>();
    private HashMap<String, String> msi5MarkerMap = new HashMap<String, String>();

    /**
     * Constructor.
     * @param msiFile       MSI File.
     * @throws IOException  IO Error.
     */
    public MsiReader(File msiFile) throws IOException {
        FileReader reader = new FileReader(msiFile);
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line = bufferedReader.readLine();  //  The header line.
        String headers[] = line.split("\t");
        line = bufferedReader.readLine();
        while (line != null) {
            String parts[] = line.split("\t");
            String barCode = getValue("bcr_patient_barcode", "TCGA ID", headers, parts);
            String msiClass7Marker = getValue("7 Marker MSI Call", "7_marker_msi_call", headers, parts);
            String msiClass5Marker = getValue("5 Concensus Marker MSI Call", "5_marker_msi_call", headers, parts);
            if (barCode.trim().length()>0) {
                String caseId = extractCaseId(barCode);
                msi7MarkerMap.put(caseId, msiClass7Marker);
                msi5MarkerMap.put(caseId, msiClass5Marker);
            }
            line = bufferedReader.readLine();
        }
        bufferedReader.close();
    }

    /**
     * Gets the 7 Marker MSI Status for the Specified Case.
     * @param caseId Case ID.
     * @return MSI Status.
     */
    public String getMsi7Status(String caseId) {
        return msi7MarkerMap.get(caseId);
    }

    /**
     * Gets the 5 Marker MSI Status for the Specified Case.
     * @param caseId Case ID.
     * @return MSI Status.
     */
    public String getMsi5Status(String caseId) {
        return msi5MarkerMap.get(caseId);
    }

    private String extractCaseId(String barCode) {
        if (barCode.startsWith("TCGA")) {
            return barCode;
        } else {
            String idParts[] = barCode.split("-");
            return "TCGA-" + idParts[0] + "-" + idParts[1];
        }
    }

    private String getValue (String targetHeader1, String targetHeader2, String[] colHeaders, String[] parts) {
        for (int i=0; i<colHeaders.length; i++) {
            String currentHeader = colHeaders[i];
            if (currentHeader.equalsIgnoreCase(targetHeader1) || currentHeader.equalsIgnoreCase(targetHeader2)) {
                return parts[i];
            }
        }
        throw new NullPointerException("Could not find column with name:  [" + targetHeader1
                + ", " + targetHeader2 + "]");
    }

}