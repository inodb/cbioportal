/*
 *  Copyright (c) 2014 Memorial Sloan-Kettering Cancer Center.
 * 
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 *  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 *  documentation provided hereunder is on an "as is" basis, and
 *  Memorial Sloan-Kettering Cancer Center 
 *  has no obligations to provide maintenance, support,
 *  updates, enhancements or modifications.  In no event shall
 *  Memorial Sloan-Kettering Cancer Center
 *  be liable to any party for direct, indirect, special,
 *  incidental or consequential damages, including lost profits, arising
 *  out of the use of this software and its documentation, even if
 *  Memorial Sloan-Kettering Cancer Center 
 *  has been advised of the possibility of such damage.
 */
package org.mskcc.cbio.importer.persistence.staging.clinical;

import com.google.common.base.Function;
import org.mskcc.cbio.importer.persistence.staging.TsvStagingFileHandler;

import java.nio.file.Path;
import java.util.List;

public interface ClinicalDataFileHandler extends TsvStagingFileHandler {
    
    /*
    register the clinical data file for staging data with handler. If the file does not
    exist, implementors should create it and write out the column headings as tsv
    */
    public void registerClinicalDataStagingFile(Path cdFilePath, List<String> columnHeadings);

    public void registerClinicalDataStagingFile(Path cdFilePath, List<String> columnHeadings, boolean deleteFile);

    /*
    public method to transform a List of sequence data to a List of Strings and
    output that List to the appropriate staging file based on the report type
     */
    public void transformImportDataToStagingFile(List aList,
                                                 Function transformationFunction);
    public boolean isRegistered();

}
