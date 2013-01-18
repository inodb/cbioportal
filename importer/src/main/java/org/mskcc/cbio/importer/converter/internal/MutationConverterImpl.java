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
package org.mskcc.cbio.importer.converter.internal;

// imports
import org.mskcc.cbio.importer.Config;
import org.mskcc.cbio.importer.CaseIDs;
import org.mskcc.cbio.importer.IDMapper;
import org.mskcc.cbio.importer.Converter;
import org.mskcc.cbio.importer.FileUtils;
import org.mskcc.cbio.importer.util.MapperUtil;
import org.mskcc.cbio.importer.model.PortalMetadata;
import org.mskcc.cbio.importer.model.DatatypeMetadata;
import org.mskcc.cbio.importer.model.DataMatrix;
import org.mskcc.cbio.importer.model.CancerStudyMetadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.List;
import java.util.Arrays;

/**
 * Class which implements the Converter interface.
 */
public class MutationConverterImpl implements Converter {

	// our logger
	private static final Log LOG = LogFactory.getLog(MutationConverterImpl.class);

	// ref to configuration
	private Config config;

	// ref to file utils
	private FileUtils fileUtils;

	// ref to caseids
	private CaseIDs caseIDs;

	// ref to IDMapper
	private IDMapper idMapper;

	/**
	 * Constructor.
     *
     * @param config Config
	 * @param fileUtils FileUtils
	 * @param caseIDs CaseIDs;
	 * @param idMapper IDMapper
	 */
	public MutationConverterImpl(Config config, FileUtils fileUtils,
								 CaseIDs caseIDs, IDMapper idMapper) {

		// set members
		this.config = config;
        this.fileUtils = fileUtils;
		this.caseIDs = caseIDs;
		this.idMapper = idMapper;
	}

	/**
	 * Converts data for the given portal.
	 *
     * @param portal String
	 * @param runDate String
	 * @param applyOverrides Boolean
	 * @throws Exception
	 */
    @Override
	public void convertData(String portal, String runDate, Boolean applyOverrides) throws Exception {
		throw new UnsupportedOperationException();
	}

	/**
	 * Generates case lists for the given portal.
	 *
     * @param portal String
	 * @throws Exception
	 */
    @Override
	public void generateCaseLists(String portal) throws Exception {
		throw new UnsupportedOperationException();
	}

    /**
	 * Applies overrides to the given portal using the given data source.
	 *
	 * @param portal String
	 * @throws Exception
	 */
    @Override
	public void applyOverrides(String portal) throws Exception {
		throw new UnsupportedOperationException();
    }

	/**
	 * Creates a staging file from the given import data.
	 *
     * @param portalMetadata PortalMetadata
	 * @param cancerStudyMetadata CancerStudyMetadata
	 * @param datatypeMetadata DatatypeMetadata
	 * @param dataMatrices DataMatrix[]
	 * @throws Exception
	 */
	@Override
	public void createStagingFile(PortalMetadata portalMetadata, CancerStudyMetadata cancerStudyMetadata,
								  DatatypeMetadata datatypeMetadata, DataMatrix[] dataMatrices) throws Exception {

		// sanity check
		if (dataMatrices.length != 1) {
			if (LOG.isErrorEnabled()) {
				LOG.error("createStagingFile(), dataMatrices.length != 1, aborting...");
			}
			return;
		}
		DataMatrix dataMatrix = dataMatrices[0];
		//dataMatrix.convertCaseIDs(Converter.MUTATION_CASE_ID_COLUMN_HEADER);
		List<String> columnHeaders = dataMatrix.getColumnHeaders();

		if (LOG.isInfoEnabled()) {
			LOG.info("createStagingFile(), writing staging file.");
		}
		if (columnHeaders.contains("ONCOTATOR_VARIANT_CLASSIFICATION")) {
			if (LOG.isInfoEnabled()) {
				LOG.info("createStagingFile(), MAF is already oncotated, create staging file straight-away.");
			}
			// optimization - if an override exists, just copy it over and don't create a staging file from the data matrix
			// this code assumes all MAFs follow the same naming convention as MAFs from the firehose/tcga
			String overrideFilename = datatypeMetadata.getTCGAArchivedFiles(datatypeMetadata.getTCGADownloadArchives()
																			.iterator().next()).iterator().next();
			overrideFilename = overrideFilename.replaceAll(DatatypeMetadata.TUMOR_TYPE_TAG, cancerStudyMetadata.getTumorType().toUpperCase());
			String stagingFilename = datatypeMetadata.getStagingFilename().replaceAll(DatatypeMetadata.CANCER_STUDY_TAG, cancerStudyMetadata.toString());
			System.out.println("override filename: " + overrideFilename);
			File overrideFile = fileUtils.getOverrideFile(portalMetadata, cancerStudyMetadata, overrideFilename);
			// if we have an override file, just copy it over to the staging area
			if (overrideFile != null) {
				if (LOG.isInfoEnabled()) {
					LOG.info("createStagingFile(), we found MAF in override directory, copying it to staging area directly: " +
							 overrideFile.getPath());
				}
				fileUtils.applyOverride(portalMetadata, cancerStudyMetadata, overrideFilename, stagingFilename);
				fileUtils.writeMetadataFile(portalMetadata, cancerStudyMetadata, datatypeMetadata, dataMatrix);
			}
			// we should almost always never get here - when do we have an oncated maf that doesn't exist
			// in overrides?  ...when firehose starts providing oncotated mafs, thats when...
			else {
				if (LOG.isInfoEnabled()) {
					LOG.info("createStagingFile(), we have an oncoated MAF that doesn't exist in overrides, " +
							 "creating staging file from DataMatrix");
				}
				fileUtils.writeStagingFile(portalMetadata, cancerStudyMetadata, datatypeMetadata, dataMatrix);
			}
		}
		else {
			if (LOG.isInfoEnabled()) {
				LOG.info("createStagingFile(), file requires a run through the Oncotator and OMA tool.");
			}
			fileUtils.writeMutationStagingFile(portalMetadata, cancerStudyMetadata, datatypeMetadata, dataMatrix);
		}
		if (LOG.isInfoEnabled()) {
			LOG.info("createStagingFile(), complete.");
		}
	}
}
