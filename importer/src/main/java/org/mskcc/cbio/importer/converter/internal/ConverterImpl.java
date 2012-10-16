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
import org.mskcc.cbio.importer.DatabaseUtils;
import org.mskcc.cbio.importer.model.ImportData;
import org.mskcc.cbio.importer.model.PortalMetadata;
import org.mskcc.cbio.importer.model.ImportDataMatrix;
import org.mskcc.cbio.importer.model.DatatypeMetadata;
import org.mskcc.cbio.importer.dao.ImportDataDAO;
import org.mskcc.cbio.importer.util.ClassLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;

/**
 * Class which implements the Converter interface.
 */
final class ConverterImpl implements Converter {

	// our logger
	private static final Log LOG = LogFactory.getLog(ConverterImpl.class);

	// ref to configuration
	private Config config;

	// ref to file utils
	private FileUtils fileUtils;

	// ref to database utils
	private DatabaseUtils databaseUtils;

	// ref to import data
	private ImportDataDAO importDataDAO;

	// ref to caseids
	private CaseIDs caseIDs;

	// ref to IDMapper
	private IDMapper idMapper;

	/**
	 * Constructor.
     *
     * @param config Config
	 * @param fileUtils FileUtils
	 * @param databaseUtils DatabaseUtils
	 * @param importDataDAO ImportDataDAO;
	 * @param caseIDs CaseIDs;
	 * @param idMapper IDMapper
	 */
	public ConverterImpl(final Config config, final FileUtils fileUtils, final DatabaseUtils databaseUtils,
						 final ImportDataDAO importDataDAO, final CaseIDs caseIDs, final IDMapper idMapper) {

		// set members
		this.config = config;
        this.fileUtils = fileUtils;
		this.databaseUtils = databaseUtils;
		this.importDataDAO = importDataDAO;
		this.caseIDs = caseIDs;
		this.idMapper = idMapper;
	}

	/**
	 * Converts data for the given portal.
	 *
     * @param portal String
	 * @param geneDatabaseName String
	 * @throws Exception
	 */
    @Override
	public void convertData(final String portal, final String geneDatabaseName) throws Exception {

		if (LOG.isInfoEnabled()) {
			LOG.info("convertData(), portal: " + portal);
			LOG.info("convertData(), geneDatabaseName: " + geneDatabaseName);
		}

        // check args
        if (portal == null) {
            throw new IllegalArgumentException("portal must not be null");
		}
        if (geneDatabaseName == null) {
            throw new IllegalArgumentException("geneDatabaseName must not be null");
		}

		// initialize the mapper
		if (LOG.isInfoEnabled()) {
			LOG.info("convertData(), initializing the IDMapper.");
		}
		initializeMapper(geneDatabaseName);

        // get portal metadata
        PortalMetadata portalMetadata = config.getPortalMetadata(portal);
        if (portalMetadata == null) {
            if (LOG.isInfoEnabled()) {
                LOG.info("convertData(), cannot find PortalMetadata, returning");
            }
            return;
        }

		// get datatype metadata
		Collection<DatatypeMetadata> datatypeMetadata = config.getDatatypeMetadata();

        // iterate over all import data objects
        for (ImportData importData : importDataDAO.getImportData()) {

            if (LOG.isInfoEnabled()) {
                LOG.info("convertData(), determining if importData object belongs in portal: " +
                         importData.getTumorType() + "/" + importData.getDatatype());
            }

            // does this cancer study / datatype belong in this portal?
            if (!belongsInPortal(portalMetadata, importData)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("convertData(), importData does not belongs in portal, skipping");
                }
                continue;
            }

            if (LOG.isInfoEnabled()) {
                LOG.info("convertData(), importData does belong, getting content");
            }

            // get data to process as JTable
            ImportDataMatrix importDataMatrix = fileUtils.getFileContents(portalMetadata, importData);

            // get converter and create staging file
			Object[] args = { config, fileUtils, caseIDs, idMapper };
			Converter converter =
				(Converter)ClassLoader.getInstance(getConverterClassName(importData.getDatatype(), datatypeMetadata), args);
			converter.createStagingFile(portalMetadata, importData, importDataMatrix);
        }
	}

	/**
	 * Generates case lists for the given portal.
	 *
     * @param portal String
	 * @throws Exception
	 */
    @Override
	public void generateCaseLists(final String portal) throws Exception {

		if (LOG.isInfoEnabled()) {
			LOG.info("generateCaseLists()");
		}
    }

	/**
	 * Creates a staging file from the given data matrix.
	 *
     * @param portalMetadata PortalMetadata
	 * @param importData ImportData
	 * @param importDataMatrix ImportDataMatrix
	 * @throws Exception
	 */
	@Override
	public void createStagingFile(final PortalMetadata portalMetadata, final ImportData importData,
								  final ImportDataMatrix importDataMatrix) throws Exception {
		throw new UnsupportedOperationException();
	}

    /**
     * Helper function - determines if the given ImportData
     * belongs in the given portal.
     *
     * @param portalMetadata PortalMetadata
     * @param importData ImportData
     * @return Boolean
     */
    private Boolean belongsInPortal(final PortalMetadata portalMetadata, final ImportData importData) {
        
        // check cancer studies
        for (String cancerStudy : portalMetadata.getCancerStudies()) {
            if (cancerStudy.contains(importData.getTumorType().toLowerCase())) {
                for (String datatype : portalMetadata.getDatatypes()) {
                    if (datatype.contains(importData.getDatatype().toLowerCase())) {
                        return true;
                    }
                }
            }
        }

        // outta here
        return false;
    }

	/**
	 * Helper function to get converter className
	 *
	 * @param filename String
	 * @param datatypeMetadata Collection<datatypeMetadata>
	 * @return String
	 */
	private String getConverterClassName(final String datatype,
										 final Collection<DatatypeMetadata> datatypeMetadata) {
		
		String toReturn = "";

		for (DatatypeMetadata dtMetadata : datatypeMetadata) {
            if (dtMetadata.getDatatype().toLowerCase().equals(datatype.toLowerCase())) {
                toReturn = dtMetadata.getConverterClassName();
                break;
            }
		}

		// outta here
		return toReturn;
	}

	/**
	 * Helper function to initialize IDMapper.
	 *
	 * @param geneDatabaseName String
	 * @throws Exception
	 */
	private void initializeMapper(final String geneDatabaseName) throws Exception {

		// parse out locat
		String connectionString = (databaseUtils.getDatabaseConnectionString() +
								   geneDatabaseName +
								   "?user=" + databaseUtils.getDatabaseUser() +
								   "&password=" + databaseUtils.getDatabasePassword());
		idMapper.initMapper(connectionString);
	}
}
