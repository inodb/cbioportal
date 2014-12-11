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
import org.mskcc.cbio.importer.model.DataSourcesMetadata;
import org.mskcc.cbio.importer.model.DataMatrix;
import org.mskcc.cbio.importer.model.CancerStudyMetadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * Class which implements the Converter interface for processing rna-seq (v1) - RPKM files.
 */
public class RNASEQMRNAMedianNormalConverterImpl extends RNASEQV2MRNAMedianConverterImpl implements Converter {

	public RNASEQMRNAMedianNormalConverterImpl(Config config, FileUtils fileUtils,
	                                           CaseIDs caseIDs, IDMapper idMapper)
	{
		super(config, fileUtils, caseIDs, idMapper,
		      LogFactory.getLog(RNASEQMRNAMedianNormalConverterImpl.class),
		      ConversionType.NORMAL_ONLY);
	}

	@Override
	public void createStagingFile(PortalMetadata portalMetadata, CancerStudyMetadata cancerStudyMetadata,
								  DatatypeMetadata datatypeMetadata, DataMatrix[] dataMatrices) throws Exception
	{
		// sanity check
		if (dataMatrices.length != 1) {
			if (LOG.isErrorEnabled()) {
				LOG.error("createStagingFile(), dataMatrices.length != 1, aborting...");
			}
			return;
		}
		DataMatrix dataMatrix = dataMatrices[0];

		// rnaseq v1 files have 3 columns per sample (first column is Hybridization REF).
		// discard first & second columns and take third - RPKM
		logMessage(LOG, "createStagingFile(), removing  and keepng RPKM column per sample");
		removeUsedV1ColumnHeaders(dataMatrix);
		
		// everything from here is the same for rna seq v2, lets pass processing to it
		dataMatrices = new DataMatrix[] { dataMatrix };
		super.createStagingFile(portalMetadata, cancerStudyMetadata, datatypeMetadata, dataMatrices);
	}
}
