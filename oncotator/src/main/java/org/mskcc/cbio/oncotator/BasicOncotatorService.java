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

package org.mskcc.cbio.oncotator;

import java.io.IOException;

/**
 * Basic Oncotator Service implementaion with no cache or database.
 *
 * @author Selcuk Onur Sumer
 */
public class BasicOncotatorService extends OncotatorService
{
	/**
	 * Retrieves the data from the Oncotator service for the given query key.
	 *
	 * @param key   key for the service query
	 * @return      oncotator record containing the query result
	 */
	public OncotatorRecord getOncotatorRecord(String key) throws OncotatorServiceException
	{
		// get record directly from the oncotator web service
		OncotatorRecord record = null;

		try {
			record = this.getRecordFromService(key);
		} catch (IOException e) {
			e.printStackTrace();
			throw new OncotatorServiceException(e.getMessage());
		}

		// if record is null, then there is an error with JSON parsing
		if (record == null)
		{
			record = new OncotatorRecord(key);
			this.errorCount++;
		}

		return record;
	}
}
