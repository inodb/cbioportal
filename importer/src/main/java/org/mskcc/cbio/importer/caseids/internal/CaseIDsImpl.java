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
package org.mskcc.cbio.importer.caseids.internal;

// imports
import org.mskcc.cbio.importer.Config;
import org.mskcc.cbio.importer.CaseIDs;
import org.mskcc.cbio.importer.model.DataMatrix;
import org.mskcc.cbio.importer.model.CaseIDFilterMetadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which implements the CaseIDs interface.
 */
public class CaseIDsImpl implements CaseIDs {

	// ref to our matchers
	private Collection<Pattern> patterns;

	/**
	 * Constructor.
     *
     * @param config Config
	 */
	public CaseIDsImpl(Config config) {

		// get all the filters
		Collection<CaseIDFilterMetadata> caseIDFilters = config.getCaseIDFilterMetadata(Config.ALL);

		// sanity check
		if (caseIDFilters == null) {
			throw new IllegalArgumentException("cannot instantiate a proper collection of CaseIDFilterMetadata objects.");
		}

		// setup our matchers
		patterns = new ArrayList<Pattern>();
		for (CaseIDFilterMetadata caseIDFilter : caseIDFilters) {
			patterns.add(Pattern.compile(caseIDFilter.getRegex()));
		}
	}

	/**
	 * Converts the given case id to mskcc format.
	 *
	 * @param caseID String
	 * @return String
	 */
	@Override
	public String convertCaseID(String caseID) {

		for (Pattern pattern : patterns) {
			Matcher matcher = pattern.matcher(caseID);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}

		// outta here
		return caseID;
	}

	/**
	 * Determines if given case id is a tumor case id.
	 *
     * @param caseID String
	 * @return boolean
	 */
	@Override
	public boolean isTumorCaseID(String caseID) {

		for (Pattern pattern : patterns) {
			if (pattern.matcher(caseID).matches()) {
				return true;
			}
		}

		// outta here
		return false;
	}

	/**
	 * Computes the number of case ids within the give import data matrix.
	 *
     * @param dataMatrix DataMatrix
	 * @return int
	 */
	@Override
	public int getCaseCount(DataMatrix dataMatrix) {

		int toReturn = 0;

		Collection<String> columnHeaders = dataMatrix.getColumnHeaders();
		for (String columnHeader : columnHeaders) {
			if (isTumorCaseID(columnHeader)) {
				++toReturn;
			}
		}

		// outta here
		return toReturn;
	}
}
