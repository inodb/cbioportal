/** Copyright (c) 2012 Memorial Sloan-Kettering Cancer Center.
**
** This library is free software; you can redistribute it and/or modify it
** under the terms of the GNU Lesser General Public License as published
** by the Free Software Foundation; either version 2.1 of the License, or
** any later version.
**
** This library is distributed in the hope that it will be useful, but
** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
** documentation provided hereunder is on an "as is" basis, and
** Memorial Sloan-Kettering Cancer Center 
** has no obligations to provide maintenance, support,
** updates, enhancements or modifications.  In no event shall
** Memorial Sloan-Kettering Cancer Center
** be liable to any party for direct, indirect, special,
** incidental or consequential damages, including lost profits, arising
** out of the use of this software and its documentation, even if
** Memorial Sloan-Kettering Cancer Center 
** has been advised of the possibility of such damage.  See
** the GNU Lesser General Public License for more details.
**
** You should have received a copy of the GNU Lesser General Public License
** along with this library; if not, write to the Free Software Foundation,
** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
**/

// package
package org.mskcc.cbio.importer;

// imports
import org.mskcc.cbio.importer.CaseIDs;
import org.mskcc.cbio.importer.model.ImportDataRecord;
import org.mskcc.cbio.importer.model.PortalMetadata;
import org.mskcc.cbio.importer.model.DataMatrix;
import org.mskcc.cbio.importer.model.DatatypeMetadata;
import org.mskcc.cbio.importer.model.DataSourcesMetadata;
import org.mskcc.cbio.importer.model.CaseListMetadata;
import org.mskcc.cbio.importer.model.CancerStudyMetadata;

import org.apache.commons.io.LineIterator;

import java.io.File;
import java.util.List;
import java.util.Collection;

/**
 * Interface used to access some common file utils.
 */
public interface FileUtils {

	public static final String FILE_URL_PREFIX = "file://";

	/**
	 * Computes the MD5 digest for the given file.
	 * Returns the 32 digit hexadecimal.
	 *
	 * @param file File
	 * @return String
	 * @throws Exception
	 */
	String getMD5Digest(File file) throws Exception;

	/**
	 * Reads the precomputed md5 digest out of a firehose .md5 file.
	 *
	 * @param file File
	 * @return String
	 * @throws Exception 
	 */
	String getPrecomputedMD5Digest(File file) throws Exception;

    /**
     * Makes a directory, including parent directories if necessary.
     *
     * @param directory File
	 * @throws Exception
     */
    void makeDirectory(File directory) throws Exception;

    /**
     * Deletes a directory recursively.
     *
     * @param directory File
	 * @throws Exception
     */
    void deleteDirectory(File directory) throws Exception;

    /**
     * Lists all files in a given directory and its subdirectories.
     *
     * @param directory File
     * @param extensions String[]
     * @param recursize boolean
     * @return Collection<File>
	 * @throws Exception
     */
    Collection<File> listFiles(File directory, String[] extensions, boolean recursive) throws Exception;

	/**
	 * Returns the given file contents in an DataMatrix.
	 *
	 * @param importDataRecord ImportDataRecord
	 * @return DataMatrix
	 * @throws Exception
	 */
	DataMatrix getFileContents(ImportDataRecord importDataRecord) throws Exception;

	/**
	 * Get the case list from the staging file.
	 *
	 * @param caseIDs CaseIDs;
     * @param portalMetadata PortalMetadata
	 * @param cancerStudyMetadata CancerStudyMetadata
	 * @param stagingFilename String
	 * @return List<String>
	 * @throws Exception
	 */
	List<String> getCaseListFromStagingFile(CaseIDs caseIDs, PortalMetadata portalMetadata, CancerStudyMetadata cancerStudyMetadata, String stagingFilename) throws Exception;

	/**
	 * Creates a temporary file with the given contents.
	 *
	 * @param filename String
	 * @param fileContent String
	 * @return File
	 * @throws Exception
	 */
	File createTmpFileWithContents(String filename, String fileContent) throws Exception;

	/**
	 * Creates (or overwrites) the given file with the given contents.  Filename
	 * is canonical path/filename.
	 *
	 * @param filename String
	 * @param fileContent String
	 * @return File
	 * @throws Exception
	 */
	File createFileWithContents(String filename, String fileContent) throws Exception;

	/**
	 * Downloads the given file specified via url to the given canonicalDestination.
	 *
	 * @param uriSource String
	 * @param uriDestination String
	 * @throws Exception
	 */
	void downloadFile(String urlSource, String urlDestination) throws Exception;

	/**
	 * Returns a line iterator over the given file.
	 *
	 * @param urlFile String
	 * @throws Exception
	 */
	LineIterator getFileContents(String urlFile) throws Exception;

	/**
	 * Method which writes the cancer study metadata file.
	 *
     * @param portalMetadata PortalMetadata
	 * @param cancerStudyMetadata CancerStudyMetadata
	 * @param numCases int
	 * @throws Exception
	 */
	void writeCancerStudyMetadataFile(PortalMetadata portalMetadata, CancerStudyMetadata cancerStudyMetadata, int numCases) throws Exception;

	/**
	 * Method which writes a metadata file for the
	 * given DatatypeMetadata.  DataMatrix may be null.
	 *
     * @param portalMetadata PortalMetadata
	 * @param cancerStudyMetadata CancerStudyMetadata
	 * @param datatypeMetadata DatatypeMetadata
	 * @param dataMatrix DataMatrix
	 * @throws Exception
	 *
	 */
	void writeMetadataFile(PortalMetadata portalMetadata, CancerStudyMetadata cancerStudyMetadata,
						   DatatypeMetadata datatypeMetadata, DataMatrix dataMatrix) throws Exception;

	/**
	 * Creates a staging file (and meta file) with contents from the given DataMatrix.
	 *
     * @param portalMetadata PortalMetadata
	 * @param cancerStudyMetadata CancerStudyMetadata
	 * @param datatypeMetadata DatatypeMetadata
	 * @param dataMatrix DataMatrix
	 * @throws Exception
	 */
	void writeStagingFile(PortalMetadata portalMetadata, CancerStudyMetadata cancerStudyMetadata,
						  DatatypeMetadata datatypeMetadata, DataMatrix dataMatrix) throws Exception;

	/**
	 * Creates a staging file for mutation data (and meta file) with contents from the given DataMatrix.
	 * This is called when the mutation file needs to be run through the Oncotator and Mutation Assessor Tools.
	 *
     * @param portalMetadata PortalMetadata
	 * @param cancerStudy CancerStudyMetadata
	 * @param datatypeMetadata DatatypeMetadata
	 * @param dataMatrix DataMatrix
	 * @throws Exception
	 */
	void writeMutationStagingFile(PortalMetadata portalMetadata, CancerStudyMetadata cancerStudyMetadata,
								  DatatypeMetadata datatypeMetadata, DataMatrix dataMatrix) throws Exception;

	/**
	 * Creates a z-score staging file from the given dependencies.  It assumes that the
	 * dependency - staging files have already been created.
	 *
     * @param portalMetadata PortalMetadata
	 * @param cancerStudyMetadata CancerStudyMetadata
	 * @param datatypeMetadata DatatypeMetadata
	 * @param dependencies DatatypeMetadata[]
	 * @throws Exception
	 */
	void writeZScoresStagingFile(PortalMetadata portalMetadata, CancerStudyMetadata cancerStudyMetadata,
								 DatatypeMetadata datatypeMetadata, DatatypeMetadata[] dependencies) throws Exception;

	/**
	 * Returns an override file (if it exists) for the given portal & cancer study.  The override in this case
	 * is the override file that a DataMatrix is created from.
	 *
	 * Null is returned if an override file is not found.
	 *
	 * @param portalMetadata PortalMetadata
	 * @param cancerStudyMetadata CancerStudyMetadata
	 * @param filename String
	 * @return File
	 * @throws Exception
	 */
	File getOverrideFile(PortalMetadata portalMetadata, CancerStudyMetadata cancerStudyMetadata, String filename) throws Exception;

	/**
	 * If it exists, moves an override file into the proper
	 * location in the given portals staging area.
	 *
	 * Note, filename can be the name of a file or directory (like case_lists)
	 *
	 * @param portalMetadata PortalMetadata
	 * @param cancerStudyMetadata CancerStudyMetadata
	 * @param overrideFilename String
	 * @param stagingFilename String
	 * @throws Exception
	 */
	void applyOverride(PortalMetadata portalMetadata, CancerStudyMetadata cancerStudyMetadata,
					   String overrideFilename, String stagingFilename) throws Exception;

	/**
	 * Create a case list file from the given case list metadata file.
	 *
     * @param portalMetadata PortalMetadata
	 * @param cancerStudyMetadata CancerStudyMetadata
	 * @param caseListMetadata CaseListMetadata
	 * @param caseList String[]
	 * @throws Exception
	 */
	void writeCaseListFile(PortalMetadata portalMetadata,
						   CancerStudyMetadata cancerStudyMetadata, CaseListMetadata caseListMetadata, String[] caseList) throws Exception;

	/**
	 * Runs all MAFs for the given dataaSourcesMetadata through
	 * the Oncotator and OMA tools.
	 *
	 * @param dataSourcesMetadata DataSourcesMetadata
	 * @throws Exception
	 */
	void oncotateAllMAFs(DataSourcesMetadata dataSourcesMetadata) throws Exception;

	/**
	 * Runs a MAF file through the Oncotator and OMA tools.
	 *
	 * @param inputMAFURL String
	 * @param outputMAFURL String
	 * @throws Exception
	 */
	void oncotateMAF(String inputMAFURL, String outputMAFURL) throws Exception;
}
