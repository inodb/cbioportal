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
package org.mskcc.cbio.importer.config.internal;

// imports
import org.mskcc.cbio.importer.Config;
import org.mskcc.cbio.importer.converter.internal.ClinicalDataConverterImpl;
import org.mskcc.cbio.importer.model.*;
import org.mskcc.cbio.importer.util.ClassLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.client.spreadsheet.FeedURLFactory;

import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Class which implements the Config interface
 * using google docs as a backend.
 */
class GDataImpl implements Config {

	// our logger
	private static Log LOG = LogFactory.getLog(GDataImpl.class);

	// google docs user
	private String gdataUser;
	// google docs password
	private String gdataPassword;
	// ref to spreadsheet client
	private SpreadsheetService spreadsheetService;

	// for performance optimization - we try to limit the number of accesses to google
	ArrayList<ArrayList<String>> cancerStudiesMatrix;
	ArrayList<ArrayList<String>> caseIDFiltersMatrix;
	ArrayList<ArrayList<String>> caseListMatrix;
	ArrayList<ArrayList<String>> clinicalAttributesMatrix;
	ArrayList<ArrayList<String>> datatypesMatrix;
	ArrayList<ArrayList<String>> dataSourcesMatrix;
	ArrayList<ArrayList<String>> portalsMatrix;
	ArrayList<ArrayList<String>> referenceMatrix;
	ArrayList<ArrayList<String>> tumorTypesMatrix;

	// worksheet names we need for updates
	private String gdataSpreadsheet;
	private String tumorTypesWorksheet;
	private String datatypesWorksheet;
	private String caseIDFiltersWorksheet;
	private String caseListWorksheet;
	private String clinicalAttributesWorksheet;
	private String portalsWorksheet;
	private String referenceDataWorksheet;
	private String dataSourcesWorksheet;
	private String cancerStudiesWorksheet;

	/**
	 * Constructor.
     *
     * Constructor args are passed viaw applicationContext.  We do this so that all our
	 *  metadata objects can be retrieved during construction of this class.  Which will
	 * prevent us from having to access google more than once.  Of course any changes to
	 * the google docs will not be reflected in this class until its next instantiation.
     *
	 * @param gdataUser String
	 * @param gdataPassword String
     * @param spreadsheetService SpreadsheetService
	 * @param gdataSpreadsheet String
	 * @param tumorTypesWorksheet String
	 * @param datatypesWorksheet String
	 * @param caseIDFiltersWorksheet String
	 * @param caseListWorksheet String
	 * @param portalsWorksheet String
	 * @param referenceDataWorksheet String
	 * @param dataSourceseWorksheet String
	 * @param cancerStudiesWorksheet String
	 */
	public GDataImpl(String gdataUser, String gdataPassword, SpreadsheetService spreadsheetService,
					 String gdataSpreadsheet, String tumorTypesWorksheet, String datatypesWorksheet,
					 String caseIDFiltersWorksheet, String caseListWorksheet, String clinicalAttributesWorksheet,
					 String portalsWorksheet, String referenceDataWorksheet, String dataSourcesWorksheet, String cancerStudiesWorksheet) {

		// set members
		this.gdataUser = gdataUser;
		this.gdataPassword = gdataPassword;
		this.spreadsheetService = spreadsheetService;

		// save name(s) of worksheet we update later
		this.gdataSpreadsheet = gdataSpreadsheet;
		this.tumorTypesWorksheet = tumorTypesWorksheet;
		this.datatypesWorksheet = datatypesWorksheet;
		this.caseIDFiltersWorksheet = caseIDFiltersWorksheet;
		this.caseListWorksheet = caseListWorksheet;
		this.clinicalAttributesWorksheet = clinicalAttributesWorksheet;
		this.portalsWorksheet = portalsWorksheet;
		this.referenceDataWorksheet = referenceDataWorksheet;
		this.dataSourcesWorksheet = dataSourcesWorksheet;
		this.cancerStudiesWorksheet = cancerStudiesWorksheet;
	}

	/**
	 * Function to get tumor types to download as String[]
	 *
	 * @return String[]
	 */
	@Override
	public String[] getTumorTypesToDownload() {

		String toReturn = "";
		for (TumorTypeMetadata tumorTypeMetadata : getTumorTypeMetadata(Config.ALL)) {
			if (tumorTypeMetadata.getDownload()) {
				toReturn += tumorTypeMetadata.getType() + ":";
			}
		}

		// outta here
		return toReturn.split(":");
	}

	/**
	 * Gets a TumorTypeMetadata object via tumorType.
	 * If tumorType == Config.ALL, all are returned.
	 *
	 * @param tumortype String
	 * @return TumorTypeMetadata
	 */
	@Override
	public Collection<TumorTypeMetadata> getTumorTypeMetadata(String tumorType) {

		Collection<TumorTypeMetadata> toReturn = new ArrayList<TumorTypeMetadata>();

		if (tumorTypesMatrix == null) {
			tumorTypesMatrix = getWorksheetData(gdataSpreadsheet, tumorTypesWorksheet);
		}

		Collection<TumorTypeMetadata> tumorTypeMetadatas = 
			(Collection<TumorTypeMetadata>)getMetadataCollection(tumorTypesMatrix,
																 "org.mskcc.cbio.importer.model.TumorTypeMetadata");
		// if user wants all, we're done
		if (tumorType.equals(Config.ALL)) {
			return tumorTypeMetadatas;
		}

		// iterate over all TumorTypeMetadata looking for match
		for (TumorTypeMetadata tumorTypeMetadata : tumorTypeMetadatas) {
            if (tumorTypeMetadata.getType().equals(tumorType)) {
				toReturn.add(tumorTypeMetadata);
				break;
            }
		}

		// outta here
		return toReturn;
	}

	/**
	 * Function to get datatypes to download as String[]
	 *
	 * @param dataSourcesMetadata DataSourcesMetadata
	 * @return String[]
	 * @throws Exception
	 */
	@Override
	public String[] getDatatypesToDownload(DataSourcesMetadata dataSourcesMetadata) throws Exception {

		HashSet<String> toReturn = new HashSet<String>();
		for (DatatypeMetadata datatypeMetadata : getDatatypeMetadata(Config.ALL)) {
			if (datatypeMetadata.isDownloaded()) {
				Method downloadArchivesMethod = datatypeMetadata.getDownloadArchivesMethod(dataSourcesMetadata.getDataSource());
				toReturn.addAll((Set<String>)downloadArchivesMethod.invoke(datatypeMetadata, null));
			}
		}

		// outta here
		return toReturn.toArray(new String[0]);
	}

	/**
	 * Function to determine the datatype(s)
	 * of the datasource file (the file that was fetched from a datasource).
	 *
	 * @param dataSourcesMetadata DataSourcesMetadata
	 * @param filename String
	 * @return Collection<DatatypeMetadata>
	 * @throws Exception
	 */
	@Override
	public Collection<DatatypeMetadata> getFileDatatype(DataSourcesMetadata dataSourcesMetadata, String filename)  throws Exception {

		Collection<DatatypeMetadata> toReturn = new ArrayList<DatatypeMetadata>();
		for (DatatypeMetadata datatypeMetadata : getDatatypeMetadata(Config.ALL)) {
			Method downloadArchivesMethod = datatypeMetadata.getDownloadArchivesMethod(dataSourcesMetadata.getDataSource());
			for (String archive : (Set<String>)downloadArchivesMethod.invoke(datatypeMetadata, null)) {
				if (filename.contains(archive)) {
					toReturn.add(datatypeMetadata);
				}
			}
		}

		// outta here
		return toReturn;
	}

	/**
	 * Gets a DatatypeMetadata object for the given datatype name.
	 * If datatype == Config.ALL, all are returned.
	 *
	 * @param datatype String
	 * @return Collection<DatatypeMetadata>
	 */
	@Override
	public Collection<DatatypeMetadata> getDatatypeMetadata(String datatype) {

		Collection<DatatypeMetadata> toReturn = new ArrayList<DatatypeMetadata>();

		if (LOG.isInfoEnabled()) {
			LOG.info("getDatatypeMetadata(): " + datatype);
		}

		if (datatypesMatrix == null) {
			datatypesMatrix = getWorksheetData(gdataSpreadsheet, datatypesWorksheet);
		}

		Collection<DatatypeMetadata> datatypeMetadatas = 
			(Collection<DatatypeMetadata>)getMetadataCollection(datatypesMatrix,
																"org.mskcc.cbio.importer.model.DatatypeMetadata");
		// if user wants all, we're done
		if (datatype.equals(Config.ALL)) {
			return datatypeMetadatas;
		}

		for (DatatypeMetadata datatypeMetadata : datatypeMetadatas) {
            if (datatypeMetadata.getDatatype().equals(datatype)) {
				toReturn.add(datatypeMetadata);
				break;
            }
		}

		// outta here
		return toReturn;
	}

	/**
	 * Gets a collection of Datatype names for the given portal/cancer study.
	 *
	 * @param portalMetadata PortalMetadata
	 * @param cancerStudyMetadata CancerStudyMetadata
	 * @return Collection<String>
	 */
	@Override
	public Collection<DatatypeMetadata> getDatatypeMetadata(PortalMetadata portalMetadata, CancerStudyMetadata cancerStudyMetadata) {

		Collection<DatatypeMetadata> toReturn = new ArrayList<DatatypeMetadata>();

		if (LOG.isInfoEnabled()) {
			LOG.info("getDatatypeMetadata(): " + portalMetadata.getName() + ":" + cancerStudyMetadata.toString());
		}

		if (cancerStudiesMatrix == null) {
			cancerStudiesMatrix = getWorksheetData(gdataSpreadsheet, cancerStudiesWorksheet);
		}

		// get portal-column index in the cancer studies worksheet
		int portalColumnIndex = cancerStudiesMatrix.get(0).indexOf(portalMetadata.getName());
		if (portalColumnIndex == -1) return toReturn;

		// iterate over all studies in worksheet and find row whose first element is cancer study (path)
		for (ArrayList<String> matrixRow : cancerStudiesMatrix) {
			if (matrixRow.get(0).equals(cancerStudyMetadata.getStudyPath())) {
				// the datatypes for the portal/cancer_study is the value of the cell
				String datatypesIndicator = matrixRow.get(portalColumnIndex);
				if (datatypesIndicator.equalsIgnoreCase(CancerStudyMetadata.CANCER_STUDY_IN_PORTAL_INDICATOR)) {
					// all datatypes are desired
					toReturn = getDatatypeMetadata(Config.ALL);
				}
				else {
					// a delimited list of datatypes have been requested
					toReturn = new ArrayList<DatatypeMetadata>();
					for (String datatype : datatypesIndicator.split(DatatypeMetadata.DATATYPES_DELIMITER)) {
                                                Collection<DatatypeMetadata> metaData = getDatatypeMetadata(datatype);
                                                if (!metaData.isEmpty()) {
                                                    DatatypeMetadata datatypeMetadata = metaData.iterator().next();
                                                    toReturn.add(datatypeMetadata);
                                                    if (LOG.isInfoEnabled()) {
                                                            LOG.info("Selecting data type"+datatypeMetadata.getDatatype());
                                                    }
                                                }
					}
				}
				break;
			}
		}

		// outta here
		return toReturn;
	}

	/**
	 * Gets a collection of CaseIDFilterMetadata.
	 *
	 * @param filterName String
	 * @return Collection<CaseIDFilterMetadata>
	 */
	@Override
	public Collection<CaseIDFilterMetadata> getCaseIDFilterMetadata(String filterName) {

		Collection<CaseIDFilterMetadata> toReturn = new ArrayList<CaseIDFilterMetadata>();

		if (caseIDFiltersMatrix == null) {
			caseIDFiltersMatrix = getWorksheetData(gdataSpreadsheet, caseIDFiltersWorksheet);
		}

		Collection<CaseIDFilterMetadata> caseIDFilterMetadatas = 
			(Collection<CaseIDFilterMetadata>)getMetadataCollection(caseIDFiltersMatrix,
																	"org.mskcc.cbio.importer.model.CaseIDFilterMetadata");

		// if user wants all, we're done
		if (filterName.equals(Config.ALL)) {
			return caseIDFilterMetadatas;
		}

		for (CaseIDFilterMetadata caseIDFilterMetadata : caseIDFilterMetadatas) {
			if (caseIDFilterMetadata.getFilterName().equals(filterName)) {
				toReturn.add(caseIDFilterMetadata);
				break;
			}
		}

		// outta here
		return toReturn;
	}

	/**
	 * Gets a collection of CaseListMetadata.
	 * If caseListFilename == Config.ALL, all are returned.
	 *
	 * @param caseListFilename String
	 * @return Collection<CaseListMetadata>
	 */
	@Override
	public Collection<CaseListMetadata> getCaseListMetadata(String caseListFilename) {

		Collection<CaseListMetadata> toReturn = new ArrayList<CaseListMetadata>();

		if (caseListMatrix == null) {
			caseListMatrix = getWorksheetData(gdataSpreadsheet, caseListWorksheet);
		}

		Collection<CaseListMetadata> caseListMetadatas = 
			(Collection<CaseListMetadata>)getMetadataCollection(caseListMatrix,
																"org.mskcc.cbio.importer.model.CaseListMetadata");

		// if user wants all, we're done
		if (caseListFilename.equals(Config.ALL)) {
			return caseListMetadatas;
		}

		for (CaseListMetadata caseListMetadata : caseListMetadatas) {
			if (caseListMetadata.getCaseListFilename().equals(caseListFilename)) {
				toReturn.add(caseListMetadata);
				break;
			}
		}

		// outta here
		return toReturn;
	}

	/**
	 * Gets a collection of ClinicalAttributesMetadata.
	 * If clinicalAttributeColumnHeader == Config.ALL, all are returned.
	 *
	 * @param clinicalAttributeColumnHeader String
	 * @return Collection<ClinicalAttributesMetadata>
	 */
	@Override
	public Collection<ClinicalAttributesMetadata> getClinicalAttributesMetadata(String clinicalAttributesColumnHeader) {

		Collection<ClinicalAttributesMetadata> toReturn = new ArrayList<ClinicalAttributesMetadata>();

		if (clinicalAttributesMatrix == null) {
			clinicalAttributesMatrix = getWorksheetData(gdataSpreadsheet, clinicalAttributesWorksheet);
		}

		Collection<ClinicalAttributesMetadata> clinicalAttributesMetadatas = 
			(Collection<ClinicalAttributesMetadata>)getMetadataCollection(clinicalAttributesMatrix,
																		  "org.mskcc.cbio.importer.model.ClinicalAttributesMetadata");

		// if user wants all, we're done
		if (clinicalAttributesColumnHeader.equals(Config.ALL)) {
			return clinicalAttributesMetadatas;
		}

		for (ClinicalAttributesMetadata clinicalAttributesMetadata : clinicalAttributesMetadatas) {
			if (clinicalAttributesMetadata.getColumnHeader().equals(clinicalAttributesColumnHeader)) {
				toReturn.add(clinicalAttributesMetadata);
				break;
			}
		}

		// outta here
		return toReturn;
	}

    public void insertClinicalAttributesMetadata(ClinicalAttributesMetadata clinicalAttributesMetadata, boolean insertRow) {

        // vars used in call to updateWorksheet below
        String keyColumn = ClinicalAttributesMetadata.WORKSHEET_UPDATE_COLUMN_KEY;
        String key = clinicalAttributesMetadata.getColumnHeader();

        updateWorksheet(gdataSpreadsheet, clinicalAttributesWorksheet,
                insertRow, keyColumn, key, clinicalAttributesMetadata.getPropertiesMap());

        clinicalAttributesMatrix = null;
    }

    /**
     * inserts without asking questions
     * @param clinicalAttributesMetadata ClinicalAttributesMetadata
     */
    @Override
    public void insertClinicalAttributesMetadata(ClinicalAttributesMetadata clinicalAttributesMetadata) {
        insertClinicalAttributesMetadata(clinicalAttributesMetadata, true);
    }

	/**
	 * Updates (or inserts) the given ClinicalAttributesMetadata object.
     * Comparison is done by COLUMN_HEADER (whether it be the empty string
     * or otherwise)
	 *
	 * @param clinicalAttributesMetadata ClinicalAttributesMetadata
	 */
	@Override
	public void updateClinicalAttributesMetadata(ClinicalAttributesMetadata clinicalAttributesMetadata) {

		// vars used in call to updateWorksheet below
		boolean insertRow = true;

		if (clinicalAttributesMatrix == null) {
			clinicalAttributesMatrix = getWorksheetData(gdataSpreadsheet, clinicalAttributesWorksheet);
		}

		Collection<ClinicalAttributesMetadata> clinicalAttributesMetadatas = 
			(Collection<ClinicalAttributesMetadata>) getMetadataCollection(clinicalAttributesMatrix,
																		  "org.mskcc.cbio.importer.model.ClinicalAttributesMetadata");

		// iterate over existing clinicalAttributesMatrix and determine if the given clinicalAttributesMetadata
		// object already exists - this would indicate an update is to take place, not an insert
		for (ClinicalAttributesMetadata potentialClinicalAttributeMetadataMatch : clinicalAttributesMetadatas) {
			if (potentialClinicalAttributeMetadataMatch.getColumnHeader().equals(clinicalAttributesMetadata.getColumnHeader())) {
				insertRow = false;
				break;
			}
		}
        insertClinicalAttributesMetadata(clinicalAttributesMetadata, insertRow);
    }

    /**
     * Special case of updating the clinical attributes worksheet with a Biospecimen Core Resource (BCR).
     *
     * In this case, a row in the worksheet should keep the columns specific to our system:
     * COLUMN_HEADER, DATATYPE, ALIASES, ANNOTATION_STATUS.
     *
     * But should should update columns that should be synced with the "standard" BCR dictionary:
     * DISPLAY_NAME, DESCRIPTION, DISEASE SPECIFICITY.
     *
     * A bcr matches iff. its id has a match in the list of aliases of the row in the worksheet
     * If it doesn't match, it gets added.
     *
     * @param bcr BcrClinicalAttributeEntry
     */
    public void updateClinicalAttributesMetadata(BcrClinicalAttributeEntry bcr) {

        if (clinicalAttributesMatrix == null) {
            clinicalAttributesMatrix = getWorksheetData(gdataSpreadsheet, clinicalAttributesWorksheet);
        }

        Collection<ClinicalAttributesMetadata> clinicalAttributesMetadatas =
                (Collection<ClinicalAttributesMetadata>) getMetadataCollection(clinicalAttributesMatrix,
                        "org.mskcc.cbio.importer.model.ClinicalAttributesMetadata");

        // you say tomaito, i say tomaato
        String bcrAlias = bcr.getId().replaceAll("_", "");

        String keyColumn = ClinicalAttributesMetadata.WORKSHEET_ALIAS_KEY;     // N.B.

        // iterate over existing clinicalAttributesMatrix and determine if the given clinicalAttributesMetadata
        // object already exists - this would indicate an update is to take place, not an insert
        // exists means that the first alias matches
        for (ClinicalAttributesMetadata attribute : clinicalAttributesMetadatas) {
            String[] aliases = attribute.getAliases().split(ClinicalDataConverterImpl.ALIAS_DELIMITER);
            for (String alias : aliases) {
                if (alias.trim().matches(bcrAlias)) {
                    // match!
                    if (!attribute.getAnnotationStatus().equals(ClinicalDataConverterImpl.OK)) {
                        // not OKed!
                        attribute.setDescription(bcr.getDescription());
                        attribute.setDisplayName(bcr.getDisplayName());
                        attribute.setDiseaseSpecificity(bcr.getDiseaseSpecificity());
                        attribute.setColumnHeader(bcr.getId().toUpperCase());

                        boolean insertRow = false;
                        String key = attribute.getAliases();
                        updateWorksheet(gdataSpreadsheet, clinicalAttributesWorksheet,
                                insertRow, keyColumn, key, attribute.getPropertiesMap());
                    }
                    // there was a match, but it has already been Oked so skip.  In other words,
                    // OK stops things from being overridden
                    return;
                }
            }
        }

        // else: insert into worksheet
        String key = null;
        boolean insertRow = true;
        updateWorksheet(gdataSpreadsheet, clinicalAttributesWorksheet,
                insertRow, keyColumn, key, bcr.getPropertiesMap());

//        clinicalAttributesMatrix = null;
    }

    public void batchUpdateClinicalAttributeMetadata(Collection<BcrClinicalAttributeEntry> bcrs) {
        if (clinicalAttributesMatrix == null) {
            clinicalAttributesMatrix = getWorksheetData(gdataSpreadsheet, clinicalAttributesWorksheet);
        }

        Collection<ClinicalAttributesMetadata> clinicalAttributesMetadatas =
                (Collection<ClinicalAttributesMetadata>) getMetadataCollection(clinicalAttributesMatrix,
                        "org.mskcc.cbio.importer.model.ClinicalAttributesMetadata");

        // make a hashmap out of the worksheet
        // alias -> ClinicalAttributeMetadata
        HashMap<String, ClinicalAttributesMetadata> aliasToAttr = new HashMap<String, ClinicalAttributesMetadata>();
        for (ClinicalAttributesMetadata attr : clinicalAttributesMetadatas) {
            String[] aliases = attr.getAliases().split(ClinicalDataConverterImpl.ALIAS_DELIMITER);
            for (String alias : aliases) {
                aliasToAttr.put(alias.trim(), attr);
            }
        }

        Collection<BcrClinicalAttributeEntry> toInsert = new ArrayList<BcrClinicalAttributeEntry>();
        Collection<BcrClinicalAttributeEntry> toUpdate = new ArrayList<BcrClinicalAttributeEntry>();
        for (BcrClinicalAttributeEntry bcr : bcrs) {
            // you say tomaito, i say tomaato
            String bcrAlias = bcr.getId().replaceAll("_", "");       // TODO: duplication

            ClinicalAttributesMetadata attr = aliasToAttr.get(bcrAlias.trim());
            if (attr != null) {
                // match!
                if (!attr.getAnnotationStatus().equals(ClinicalDataConverterImpl.OK)
                        && !attr.getAnnotationStatus().equals(ClinicalDataConverterImpl.IGNORE)) {
                    // not Oked, nor ignored, update away!
                    toUpdate.add(bcr);
                }
            } else {
                // no match insert ho!
                toInsert.add(bcr);
            }
        }

        String keyColumn = ClinicalAttributesMetadata.WORKSHEET_ALIAS_KEY;     // N.B.
        // iterate over toInsert, inserting each one
        for (BcrClinicalAttributeEntry bcr : toInsert) {
            String key = null;
            boolean insertRow = true;
            updateWorksheet(gdataSpreadsheet, clinicalAttributesWorksheet,
                    insertRow, keyColumn, key, bcr.getPropertiesMap());
        }

        for (BcrClinicalAttributeEntry bcr : toUpdate) {
            // you say tomaito, i say tomaato
            String bcrAlias = bcr.getId().replaceAll("_", "");      // TODO: duplication
            String key = bcrAlias;
            boolean insertRow = false;
            updateWorksheet(gdataSpreadsheet, clinicalAttributesWorksheet,
                    insertRow, keyColumn, key, bcr.getPropertiesMap());
        }
    }

	/**
	 * Gets a PortalMetadata object given a portal name.
	 *
     * @param portal String
	 * @return Collection<PortalMetadata>
	 */
    @Override
	public Collection<PortalMetadata> getPortalMetadata(String portalName) {

		Collection<PortalMetadata> toReturn = new ArrayList<PortalMetadata>();

		if (portalsMatrix == null) {
			portalsMatrix = getWorksheetData(gdataSpreadsheet, portalsWorksheet);
		}

		Collection<PortalMetadata> portalMetadatas =
			(Collection<PortalMetadata>)getMetadataCollection(portalsMatrix,
															  "org.mskcc.cbio.importer.model.PortalMetadata");

		// if user wants all, we're done
		if (portalName.equals(Config.ALL)) {
			return portalMetadatas;
		}

		for (PortalMetadata portalMetadata : portalMetadatas) {
			if (portalMetadata.getName().equals(portalName)) {
				toReturn.add(portalMetadata);
				break;
			}
		}

		// outta here
		return toReturn;
    }

	/**
	 * Gets ReferenceMetadata for the given referenceType.
	 * If referenceType == Config.ALL, all are returned.
	 *
	 * @param referenceType String
	 * @return Collection<ReferenceMetadata>
	 */
    @Override
	public Collection<ReferenceMetadata> getReferenceMetadata(String referenceType) {

		Collection<ReferenceMetadata> toReturn = new ArrayList<ReferenceMetadata>();

		if (referenceMatrix == null) {
			referenceMatrix = getWorksheetData(gdataSpreadsheet, referenceDataWorksheet);
		}

		Collection<ReferenceMetadata> referenceMetadatas =
			(Collection<ReferenceMetadata>)getMetadataCollection(referenceMatrix,
																 "org.mskcc.cbio.importer.model.ReferenceMetadata");
		// if user wants all, we're done
		if (referenceType.equals(Config.ALL)) {
			return referenceMetadatas;
		}

		// iterate over all ReferenceMetadata looking for match
		for (ReferenceMetadata referenceMetadata : referenceMetadatas) {
			if (referenceMetadata.getReferenceType().equals(referenceType)) {
				toReturn.add(referenceMetadata);
				break;
			}
		}

        // outta here
        return toReturn;
	}

	/**
	 * Gets DataSourcesMetadata for the given datasource.  If dataSource == Config.ALL,
	 * all are returned.
	 *
	 * @param dataSource String
	 * @return Collection<DataSourcesMetadata>
	 */
    @Override
	public Collection<DataSourcesMetadata> getDataSourcesMetadata(String dataSource) {

		Collection<DataSourcesMetadata> toReturn = new ArrayList<DataSourcesMetadata>();

		if (dataSourcesMatrix == null) {
			dataSourcesMatrix = getWorksheetData(gdataSpreadsheet, dataSourcesWorksheet);
		}

		Collection<DataSourcesMetadata> dataSourceMetadatas =
			(Collection<DataSourcesMetadata>)getMetadataCollection(dataSourcesMatrix,
																   "org.mskcc.cbio.importer.model.DataSourcesMetadata");
		// if user wants all, we're done
		if (dataSource.equals(Config.ALL)) {
			return dataSourceMetadatas;
		}

		// iterate over all DataSourcesMetadata looking for match
		for (DataSourcesMetadata dataSourceMetadata : dataSourceMetadatas) {
			if (dataSourceMetadata.getDataSource().equals(dataSource)) {
				toReturn.add(dataSourceMetadata);
				break;
			}
		}

        // outta here
        return toReturn;
	}

	/**
	 * Gets all the cancer studies for a given portal.
	 *
     * @param portal String
	 * @return Collection<CancerStudyMetadata>
	 */
	@Override
	public Collection<CancerStudyMetadata> getCancerStudyMetadata(String portalName) {

		Collection<CancerStudyMetadata> toReturn = new ArrayList<CancerStudyMetadata>();

		if (cancerStudiesMatrix == null) {
			cancerStudiesMatrix = getWorksheetData(gdataSpreadsheet, cancerStudiesWorksheet);
		}

		// get portal-column index in the cancer studies worksheet
		int portalColumnIndex = cancerStudiesMatrix.get(0).indexOf(portalName);
		if (portalColumnIndex == -1) return toReturn;

		// iterate over all studies in worksheet and determine if 
		// the value at the row and portal/column intersection is not empty
		// (we start at one, because row 0 is the column headers)
		for (int lc = 1; lc < cancerStudiesMatrix.size(); lc++) {
			ArrayList<String> matrixRow = cancerStudiesMatrix.get(lc);
			String datatypesIndicator = matrixRow.get(portalColumnIndex);
			if (datatypesIndicator != null && datatypesIndicator.length() > 0) {
				CancerStudyMetadata cancerStudyMetadata = 
					new CancerStudyMetadata(matrixRow.toArray(new String[0]));
				// get tumor type metadata
				Collection<TumorTypeMetadata> tumorTypeCollection = getTumorTypeMetadata(cancerStudyMetadata.getTumorType());
				if (!tumorTypeCollection.isEmpty()) {
					cancerStudyMetadata.setTumorTypeMetadata(tumorTypeCollection.iterator().next());
				}
				// add to return set
				toReturn.add(cancerStudyMetadata);
			}
		}

        // outta here
        return toReturn;
	}
	
	/**
	 * Gets a CancerStudyMetadata for the given cancer study.
	 *
     * @param cancerStudy String  - fully qualified path as entered on worksheet, e.g.: prad/mskcc/foundation
	 * @return CancerStudyMetadata or null if not found
	 */
	@Override
	public CancerStudyMetadata getCancerStudyMetadataByName(String cancerStudyName) {

		if (cancerStudiesMatrix == null) {
			cancerStudiesMatrix = getWorksheetData(gdataSpreadsheet, cancerStudiesWorksheet);
		}

		Collection<CancerStudyMetadata> cancerStudyMetadatas = 
			(Collection<CancerStudyMetadata>)getMetadataCollection(cancerStudiesMatrix,
																"org.mskcc.cbio.importer.model.CancerStudyMetadata");

		for (CancerStudyMetadata cancerStudyMetadata : cancerStudyMetadatas) {
            if (cancerStudyMetadata.getStudyPath().equals(cancerStudyName)) {
	            // get tumor type metadata
	            Collection<TumorTypeMetadata> tumorTypeCollection = getTumorTypeMetadata(cancerStudyMetadata.getTumorType());
	            if (!tumorTypeCollection.isEmpty()) {
		            cancerStudyMetadata.setTumorTypeMetadata(tumorTypeCollection.iterator().next());
	            }
				return cancerStudyMetadata;
            }
		}

		return null;
	}

	/**
	 * Constructs a collection of objects of the given classname from the given matrix.
	 *
	 * @param metadataMatrix ArrayList<ArrayList<String>>
	 * @param className String
	 * @return Collection<?>
	 */
	public Collection<?> getMetadataCollection(ArrayList<ArrayList<String>> metadataMatrix, String className) {

		Collection<Object> toReturn = new ArrayList<Object>();

		if (LOG.isInfoEnabled()) {
			LOG.info("getMetadataCollection(): " + className);
		}

		// we start at one, because row 0 is the column headers
		for (int lc = 1; lc < metadataMatrix.size(); lc++) {
			Object[] args = { metadataMatrix.get(lc).toArray(new String[0]) };
			try {
				toReturn.add(ClassLoader.getInstance(className, args));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		// outta here
		return toReturn;
	}

	/**
	 * authenticate with google spreadsheet client
	 *
	 * @throws Exception
	 */
	private void login() throws Exception {

		spreadsheetService.setUserCredentials(gdataUser, gdataPassword);
	}

	/**
	 * Gets the spreadsheet.
	 *
	 * @param spreadsheetName String
	 * @returns SpreadsheetEntry
	 * @throws Exception
	 */
	private SpreadsheetEntry getSpreadsheet(String spreadsheetName) throws Exception {

		FeedURLFactory factory = FeedURLFactory.getDefault();
		SpreadsheetFeed feed = spreadsheetService.getFeed(factory.getSpreadsheetsFeedUrl(), SpreadsheetFeed.class);
		for (SpreadsheetEntry entry : feed.getEntries()) {
			if (entry.getTitle().getPlainText().equals(spreadsheetName)) {
				return entry;
			}
		}
		
		// outta here
		return null;
	}

	/**
	 * Gets the worksheet feed.
	 *
	 * @param spreadsheetName String
	 * @param worksheetName String
	 * @returns WorksheetFeed
	 * @throws Exception
	 */
	private WorksheetEntry getWorksheet(String spreadsheetName, String worksheetName) throws Exception {

		// first get the spreadsheet
		SpreadsheetEntry spreadsheet = getSpreadsheet(spreadsheetName);
		if (spreadsheet != null) {
			WorksheetFeed worksheetFeed = spreadsheetService.getFeed(spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
			for (WorksheetEntry worksheet : worksheetFeed.getEntries()) {
				if (worksheet.getTitle().getPlainText().equals(worksheetName)) {
					return worksheet;
				}
			}
		}

		// outta here
		return null;
	}

	/**
	 * Helper function to retrieve the given google worksheet data matrix.
	 * as a list of string lists.
	 *
	 * @param spreadsheetName String
	 * @param worksheet String
	 * @return ArrayList<ArrayList<String>>
	 */
	private ArrayList<ArrayList<String>> getWorksheetData(String spreadsheetName, String worksheetName) {

		ArrayList<ArrayList<String>> toReturn = new ArrayList<ArrayList<String>>();

		if (LOG.isInfoEnabled()) {
			LOG.info("getWorksheetData(): " + spreadsheetName + ", " + worksheetName);
		}

		try {
			login();
			WorksheetEntry worksheet = getWorksheet(spreadsheetName, worksheetName);
			if (worksheet != null) {
				ListFeed feed = spreadsheetService.getFeed(worksheet.getListFeedUrl(), ListFeed.class);
				if (feed != null && feed.getEntries().size() > 0) {
					boolean needHeaders = true;
					for (ListEntry entry : feed.getEntries()) {
						if (needHeaders) {
							ArrayList<String> headers = new ArrayList<String>(entry.getCustomElements().getTags());
							toReturn.add(headers);
							needHeaders = false;
						}
						ArrayList<String> customElements = new ArrayList<String>();
						for (String tag : toReturn.get(0)) {
							String value = entry.getCustomElements().getValue(tag);
							if (value == null) value = "";
							customElements.add(value);
						}
						toReturn.add(customElements);
					}
				}
				else {
					if (LOG.isInfoEnabled()) {
						LOG.info("Worksheet contains no entries!");
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// outta here
		return toReturn;
	}

	/**
	 * Insert (or update) a worksheet row.  If insertRow is true,
	 * a new row will be inserted into the database.  If insertRow is
	 * false, the row will be updated.  Note, if update is to occur,
	 * keyColumn (worksheet column header) and key (key to identify row)
	 * must be set, otherwise they will be ignored (and can be null).
	 *
	 * @param spreadsheetName String
	 * @param worksheetName String
	 * @param insertRow boolean
	 * @param keyColumn String
	 * @param key String
	 * @param propertyMap Map<String,String>
	 */
	private void updateWorksheet(String spreadsheetName, String worksheetName,
								 boolean insertRow, String keyColumn, String keyValue,
								 Map<String,String> properties) {

		if (LOG.isInfoEnabled()) {
			LOG.info("insertWorksheetProperty(): " + spreadsheetName + ", " + worksheetName);
			LOG.info("insertWorksheetProperty(), insertRow: " + insertRow);
			LOG.info("insertWorksheetProperty(), keyColumn: " + keyColumn);
			LOG.info("insertWorksheetProperty(), keyValue: " + keyValue);
			LOG.info("insertWorksheetProperty(), properties: " + properties);
		}
		
		try {
			login();
			WorksheetEntry worksheet = getWorksheet(spreadsheetName, worksheetName);
			if (worksheet != null) {
				// insert the row
				if (insertRow) {
					ListEntry row = new ListEntry();
					for (String key : properties.keySet()) {
						row.getCustomElements().setValueLocal(key, properties.get(key));
					}
					spreadsheetService.insert(worksheet.getListFeedUrl(), row);
					if (LOG.isInfoEnabled()) {
						LOG.info("Worksheet data hase been successfully inserted!");
					}
				}
				// update the row
				else {
					ListFeed feed = spreadsheetService.getFeed(worksheet.getListFeedUrl(), ListFeed.class);
					for (ListEntry entry : feed.getEntries()) {
						if (entry.getCustomElements().getValue(keyColumn) != null &&
							entry.getCustomElements().getValue(keyColumn).equals(keyValue)) {
                            for (String key : properties.keySet()) {
                                entry.getCustomElements().setValueLocal(key, properties.get(key));
                            }
							entry.update();
							if (LOG.isInfoEnabled()) {
								LOG.info("Worksheet data hase been successfully updated!");
							}
						}
					}
				}
			}
			else {
				if (LOG.isInfoEnabled()) {
					LOG.info("Worksheet contains no entries!");
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
