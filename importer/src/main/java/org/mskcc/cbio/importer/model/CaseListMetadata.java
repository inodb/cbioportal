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
package org.mskcc.cbio.importer.model;

// imports
import java.util.Set;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;

/**
 * Class which contains caselist metadata.
 */
public class CaseListMetadata {

	// if either delimiter changes, update ConverterImpl

	// delimiter between download archive pairs
	public static final String CASE_LIST_UNION_DELIMITER = "|";
	// delimiter between download archive pairs
	public static final String CASE_LIST_INTERSECTION_DELIMITER = "&";

	// bean properties
	private String caseListFilename;
    private String stagingFilenames;
    private String metaStableID;
    private String metaCaseListCategory;
    private String metaCancerStudyIdentifier;
    private String metaCaseListName;
	private String metaCaseListDescription;

    /**
     * Create a CaseListMetadata instance with properties in given array.
	 * Its assumed order of properties is that from google worksheet.
     *
	 * @param properties String[]
     */
    public CaseListMetadata(String[] properties) {

		if (properties.length < 7) {
            throw new IllegalArgumentException("corrupt properties array passed to contructor");
		}

		this.caseListFilename = properties[0].trim();
		this.stagingFilenames = properties[1].trim();
		this.metaStableID = properties[2].trim();
		this.metaCaseListCategory = properties[3].trim();
		this.metaCancerStudyIdentifier = properties[4].trim();
		this.metaCaseListName = properties[5].trim();
		this.metaCaseListDescription = properties[6].trim();
	}

	public String getCaseListFilename() { return caseListFilename; }
	public String getStagingFilenames() { return stagingFilenames; }
	public String getMetaStableID() { return metaStableID; }
	public String getMetaCaseListCategory() { return metaCaseListCategory; }
	public String getMetaCancerStudyIdentifier() { return metaCancerStudyIdentifier; }
	public String getMetaCaseListName() { return metaCaseListName; }
	public String getMetaCaseListDescription() { return metaCaseListDescription; }
}
