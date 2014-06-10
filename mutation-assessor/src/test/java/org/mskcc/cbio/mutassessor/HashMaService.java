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

package org.mskcc.cbio.mutassessor;

import java.util.HashMap;

/**
 * Mutation Assessor service built on a hash map for testing purposes.
 */
public class HashMaService extends MutationAssessorService
{
	protected HashMap<String, MutationAssessorRecord> cache;

	public HashMaService()
	{
		this.cache = this.initCache();
	}

	public MutationAssessorRecord getMaRecord(String key) throws MutationAssessorServiceException
	{
		return this.cache.get(key);
	}

	protected HashMap<String, MutationAssessorRecord> initCache()
	{
		HashMap<String, MutationAssessorRecord> cache =
				new HashMap<String, MutationAssessorRecord>();

		// load with some values
		MutationAssessorRecord record = this.generateRecord(
			"11_56258437_56258437_T_C",
			"neutral", 0F, "K137R", "NA",
			"getma.org/?cm=msa&ty=f&p=OR5M8_HUMAN&rb=1&re=137&var=K137R");

		cache.put("11_56258437_56258437_T_C", record);

		record = this.generateRecord("10_100015355_100015355_C_T",
			"medium", 3.11F, "A524T",
		    "getma.org/pdb.php?prot=LOXL4_HUMAN&from=424&to=529&var=A524T",
		    "getma.org/?cm=msa&ty=f&p=LOXL4_HUMAN&rb=424&re=529&var=A524T");

		cache.put("10_100015355_100015355_C_T", record);

		record = this.generateRecord("3_41266137_41266137_C_T",
			"medium", null, "S45F", "NA",
			"getma.org/?cm=msa&ty=f&p=CTNB1_HUMAN&rb=1&re=200&var=S45F");

		cache.put("3_41266137_41266137_C_T", record);

		return cache;
	}

	protected MutationAssessorRecord generateRecord(String key,
		String impact,
		Float score,
		String proteinChange,
		String pdbLink,
		String msaLink)
	{
		MutationAssessorRecord record = new MutationAssessorRecord(key);

		record.setImpact(impact);
		record.setImpactScore(score);
		record.setProteinChange(proteinChange);
		record.setStructureLink(pdbLink);
		record.setAlignmentLink(msaLink);

		return record;
	}
}
