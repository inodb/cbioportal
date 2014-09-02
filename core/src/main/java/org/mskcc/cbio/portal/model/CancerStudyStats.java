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

package org.mskcc.cbio.portal.model;

import org.mskcc.cbio.portal.dao.DaoException;
import org.mskcc.cbio.portal.dao.DaoMutation;
import org.mskcc.cbio.portal.util.EqualsUtil;
import org.mskcc.cbio.portal.web_api.GetGeneticProfiles;

import java.util.ArrayList;

/**
 * This class encapsulates cancer study stats displayed on the Data Sets Page.
 *
 * @author Benjamin Gross
 */
public class CancerStudyStats {

	private String stableID;
	private String studyName;
        private String reference;
	private Integer all;
	private Integer sequenced;
	private Integer cna;
	private Integer rnaSEQ;
	private Integer tumorMRNA;
	private Integer normal;
	private Integer tumorMIRNA;
	private Integer methylation;
	private Integer rppa;
	private Integer complete;

	/**
	 * Constructor.
	 *
	 */
	public CancerStudyStats(String stableID, String studyName, String reference, Integer all, Integer sequenced,
							Integer cna, Integer rnaSEQ, Integer tumorMRNA, Integer normal,
							Integer tumorMIRNA, Integer methylation, Integer rppa, Integer complete) {

		this.stableID = stableID;
		this.studyName = studyName;
                this.reference = reference;
		this.all = all;
		this.sequenced = sequenced;
		this.cna = cna;
		this.rnaSEQ = rnaSEQ;
		this.tumorMRNA = tumorMRNA;
		this.normal = normal;
		this.tumorMIRNA = tumorMIRNA;
		this.methylation = methylation;
		this.rppa = rppa;
		this.complete = complete;
	}

	// accessors
	public String getStableID() { return this.stableID; }
	public String getStudyName() { return this.studyName; }
	public String getReference() { return this.reference; }
	public Integer getAll() { return this.all; }
	public Integer getSequenced() { return this.sequenced; }
	public Integer getCNA() { return this.cna; }
	public Integer getRNASEQ() { return this.rnaSEQ; }
	public Integer getTumorMRNA() { return this.tumorMRNA; }
	public Integer getNormal() { return this.normal; }
	public Integer getTumorMIRNA() { return this.tumorMIRNA; }
	public Integer getMethylation() { return this.methylation; }
	public Integer getRPPA() { return this.rppa; }
	public Integer getComplete() { return this.complete; }
}