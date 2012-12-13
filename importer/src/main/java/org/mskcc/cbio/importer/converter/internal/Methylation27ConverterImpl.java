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
import org.mskcc.cbio.importer.model.ImportDataMatrix;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Vector;

/**
 * Class which implements the Converter interface.
 */
public final class Methylation27ConverterImpl implements Converter {

	// our logger
	private static final Log LOG = LogFactory.getLog(Methylation27ConverterImpl.class);

	// statics for column identifiers in correlate - methylation file
	private static final String CORRELATE_GENE_COLUMN_HEADER_NAME = "Gene";
	private static final String CORRELATE_METH_PROBE_COLUMN_HEADER_NAME = "Meth_Probe";
	private static final String CORRELATE_SPEARMAN_COLUMN_HEADER_NAME = "Corr_Spearman";

	// statics for column identifires in <CANCER>.methylation__humanmethylation27 file
	private static final String METHYLATION_CHROMOSOME_COLUMN_HEADER_NAME = "Chromosome";
	private static final String METHYLATION_GENE_SYMBOL_COLUMN_HEADER_NAME = "Gene_Symbol";
	private static final String METHYLATION_HYBRIDIZATION_REF_COLUMN_HEADER_NAME = "Hybridization REF";
	private static final String METHYLATION_GENOMIC_COORDINATE_COLUMN_HEADER_NAME = "Genomic_Coordinate";

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
	public Methylation27ConverterImpl(final Config config, final FileUtils fileUtils,
									  final CaseIDs caseIDs, final IDMapper idMapper) {

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
	 * @throws Exception
	 */
    @Override
	public void convertData(final String portal) throws Exception {
		throw new UnsupportedOperationException();
	}

	/**
	 * Generates case lists for the given portal.
	 *
     * @param portal String
	 * @throws Exception
	 */
    @Override
	public void generateCaseLists(final String portal) throws Exception {}

	/**
	 * Creates a staging file from the given import data.
	 *
     * @param portalMetadata PortalMetadata
	 * @param cancerStudy String
	 * @param datatypeMetadata DatatypeMetadata
	 * @param importDataMatrices ImportDataMatrix[]
	 * @throws Exception
	 */
	@Override
	public void createStagingFile(final PortalMetadata portalMetadata, final String cancerStudy,
								  final DatatypeMetadata datatypeMetadata, final ImportDataMatrix[] importDataMatrices) throws Exception {

		// sanity check
		if (importDataMatrices.length != 2) {
			throw new IllegalArgumentException("ImportDataMatrices.length != 2, aborting...");
		}

		// determine which matrix is methylation data
		// and which matrix is the correlation data
		ImportDataMatrix importDataMatrixMethylationData = null;
		ImportDataMatrix importDataMatrixCorrelationData = null;
		if (importDataMatrices[0].getColumnHeaders().firstElement().equalsIgnoreCase(CORRELATE_METH_PROBE_COLUMN_HEADER_NAME) &&
			importDataMatrices[1].getColumnHeaders().firstElement().equalsIgnoreCase(METHYLATION_HYBRIDIZATION_REF_COLUMN_HEADER_NAME)) {
			importDataMatrixCorrelationData = importDataMatrices[0];
			importDataMatrixMethylationData = importDataMatrices[1];
		}
		else if (importDataMatrices[0].getColumnHeaders().firstElement().equalsIgnoreCase(METHYLATION_HYBRIDIZATION_REF_COLUMN_HEADER_NAME) &&
				 importDataMatrices[1].getColumnHeaders().firstElement().equalsIgnoreCase(CORRELATE_METH_PROBE_COLUMN_HEADER_NAME)) {
			importDataMatrixMethylationData = importDataMatrices[0];
			importDataMatrixCorrelationData = importDataMatrices[1];
		}
		else {
			throw new IllegalArgumentException("Cannot determine correlation & methylation data matrices, aborting...");
		}

		// get probe with lowest Spearman correlation for each gene in importDataMatrixCorrelationData
		// (this works whether or not importDataMatrixCorrelationData is sorted by correlation value)
		HashMap<String,String[]> lowestCorrelationMap = new HashMap<String,String[]>();
		Vector<String> genes = importDataMatrixCorrelationData.getColumnData(CORRELATE_GENE_COLUMN_HEADER_NAME).get(0);
		Vector<String> methProbes = importDataMatrixCorrelationData.getColumnData(CORRELATE_METH_PROBE_COLUMN_HEADER_NAME).get(0);
		Vector<String> corrSpearman = importDataMatrixCorrelationData.getColumnData(CORRELATE_SPEARMAN_COLUMN_HEADER_NAME).get(0);
		// sanity check
		if (genes.size() != methProbes.size() && methProbes.size() != corrSpearman.size()) {
			throw new IllegalArgumentException("Genes, probs, and corrSpearman vectors are different sizes, aborting...");
		}
		// iterate over all records
		for (int lc = 0; lc < genes.size(); lc++) {
			String gene = genes.get(lc);
			String methProbe = methProbes.get(lc);
			String cSpearman = corrSpearman.get(lc);
			if (lowestCorrelationMap.containsKey(gene)) {
				if (Double.valueOf(cSpearman) < Double.valueOf(lowestCorrelationMap.get(gene)[1])) {
					String[] value = { methProbe, cSpearman };
					lowestCorrelationMap.put(gene, value);
				}
			}
			else {
				String[] value = { methProbe, cSpearman };
				lowestCorrelationMap.put(gene, value);
			}
		}
		// determine set of probes
		Map<String, String> probeToGene = new HashMap<String, String>();
		for (String gene : genes) {
			String[] value = lowestCorrelationMap.get(gene);
			String probe = value[0];
			if (probeToGene.containsKey(probe)) {
				if (LOG.isInfoEnabled()) {
					LOG.info("createStagingFile(), " + probe + " duplicated in multiple genes");
				}
			}
			probeToGene.put(probe, gene);
		}

		// remove extraneous columns:
		//
		// original methylation__humanmethylation27 file contains
		// multiple columns with the same name, in particular 4 columns (Beta_Value  Gene_Symbol Chromosome  Genomic_Coordinate)
		// for each case.
		// so, count the # of columns in methylation__humanmethylation27 and delete all columns but
		// 1, 2, 6, 10, ...,
		Vector<String> columnHeaders = importDataMatrixMethylationData.getColumnHeaders();
		if ((columnHeaders.size()-1) % 4 != 0) {
			throw new IllegalArgumentException(cancerStudy + ": methylation__humanmethylation27 does not have 4 columns per case, aborting...");
		}
		String previousHeader = "";
		for (int lc = 1; lc < columnHeaders.size(); lc++) {
			String columnHeader = columnHeaders.get(lc);
			if (columnHeader.equals(previousHeader)) {
				importDataMatrixMethylationData.ignoreColumn(lc, true);
			}
			else {
				previousHeader = columnHeader;
			}
		}

		// remove 2nd row (row data starts at 0 index, ignoring column headings)
		// of data (containing "Composite Element REF Beta_Value...";
		importDataMatrixMethylationData.ignoreRow(0, true);

		// select rows in with lowest correlated probes
		// if probe has the gene lowest correlation file (i.e., is in the $probes array ) then select its methylation row
		Vector<String> hugoSymbols = new Vector<String>();
		Vector<String> hybridizationRefs = importDataMatrixMethylationData.getColumnData(METHYLATION_HYBRIDIZATION_REF_COLUMN_HEADER_NAME).get(0);
		for (int lc = 0; lc < hybridizationRefs.size(); lc++) {
			if (probeToGene.containsKey(hybridizationRefs.get(lc))) {
				// add gene to Hugo_Symbol at row lc
				hugoSymbols.add(probeToGene.get(hybridizationRefs.get(lc)));
			}
			else {
				// ignore probe/row
				hugoSymbols.add("NA");
				importDataMatrixCorrelationData.ignoreRow(lc, true);
			}
		}

		// add gene & entrez gene id columns
		importDataMatrixMethylationData.addColumn(Converter.GENE_SYMBOL_COLUMN_HEADER_NAME, hugoSymbols);
		importDataMatrixMethylationData.addColumn(Converter.GENE_ID_COLUMN_HEADER_NAME, new Vector<String>());
		importDataMatrixMethylationData.setGeneIDColumnHeading(Converter.GENE_ID_COLUMN_HEADER_NAME);

		// perform gene mapping, remove records as needed
		if (LOG.isInfoEnabled()) {
			LOG.info("createStagingFile(), calling MapperUtil.mapGeneSymbolToID()...");
		}
		MapperUtil.mapGeneSymbolToID(importDataMatrixMethylationData, idMapper,
									 Converter.GENE_ID_COLUMN_HEADER_NAME, Converter.GENE_SYMBOL_COLUMN_HEADER_NAME);

		// convert case ids
		if (LOG.isInfoEnabled()) {
			LOG.info("createStagingFile(), filtering & converting case ids");
		}
		String[] columnsToIgnore = { Converter.GENE_SYMBOL_COLUMN_HEADER_NAME, Converter.GENE_ID_COLUMN_HEADER_NAME }; // drop Hybridization REF
		importDataMatrixMethylationData.convertCaseIDs(Arrays.asList(columnsToIgnore));

		// ensure the first two columns are symbol, id respectively
		if (LOG.isInfoEnabled()) {
			LOG.info("createStagingFile(), sorting column headers");
		}
		columnHeaders = importDataMatrixMethylationData.getColumnHeaders();
		columnHeaders.removeElement(Converter.GENE_SYMBOL_COLUMN_HEADER_NAME);
		columnHeaders.insertElementAt(Converter.GENE_SYMBOL_COLUMN_HEADER_NAME, 0);
		columnHeaders.removeElement(Converter.GENE_ID_COLUMN_HEADER_NAME);
		columnHeaders.insertElementAt(Converter.GENE_ID_COLUMN_HEADER_NAME, 1);
		importDataMatrixMethylationData.setColumnOrder(columnHeaders);
		
		if (LOG.isInfoEnabled()) {
			//importDataMatrixMethylationData.setGeneIDColumnHeading();
			//LOG.info("createStagingFile(), of ", genes.size(), " with measured methylation, only ", lowestCorrelation.size(), " have correlations.\n";);
		}

		if (LOG.isInfoEnabled()) {
			LOG.info("createStagingFile(), writing staging file.");
		}
		fileUtils.writeStagingFile(portalMetadata, cancerStudy, datatypeMetadata, importDataMatrixMethylationData);

		if (LOG.isInfoEnabled()) {
			LOG.info("createStagingFile(), complete.");
		}
	}
}

