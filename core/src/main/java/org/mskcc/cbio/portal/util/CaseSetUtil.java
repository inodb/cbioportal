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

package org.mskcc.cbio.portal.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.mskcc.cbio.portal.dao.DaoCancerStudy;
import org.mskcc.cbio.portal.dao.DaoClinicalData;
import org.mskcc.cbio.portal.dao.DaoException;
import org.mskcc.cbio.portal.dao.DaoTextCache;
import org.mskcc.cbio.portal.model.CancerStudy;
import org.mskcc.cbio.portal.model.CaseList;
import org.mskcc.cbio.portal.web_api.GetCaseLists;

/**
 * Utility class for the user-defined case sets (case ID list).
 * 
 * @author Selcuk Onur Sumer
 */
public class CaseSetUtil
{
	/**
	 * Checks whether the provided case IDs are valid for a specific
	 * cancer study. This method returns a list of invalid cases if
	 * any, or returns an empty list if all the cases are valid.
	 * 
	 * @param studyId			stable cancer study id
	 * @param caseIds			case IDs as a single string
	 * @return					list of invalid cases
	 * @throws DaoException		if a DB error occurs
	 */
	public static List<String> validateCaseSet(String studyId,
			String caseIds) throws DaoException
	{
		ArrayList<String> invalidCases = new ArrayList<String>();
		
		// get list of all case sets for the given cancer study
		ArrayList<CaseList> caseLists = GetCaseLists.getCaseLists(studyId);
		
		// get cancer study for the given stable id
		CancerStudy study = DaoCancerStudy.getCancerStudyByStableId(studyId);
		
		// get all cases in the clinical free form table for the given cancer study
		Set<String> freeFormCases = DaoClinicalData.getAllCases(study.getInternalId());
		
		if (!caseLists.isEmpty() &&
			caseIds != null)
		{
			// validate each case ID
			for(String caseId: caseIds.trim().split("\\s+"))
			{
				boolean valid = false;
				
				// search all lists for the current case
				for (CaseList caseList: caseLists)
				{
					// if the case is found in any of the lists,
					// then it is valid, no need to search further
					if(caseList.getCaseList().contains(caseId))
					{
						valid = true;
						break;
					}
				}
				
				// search also clinical free form table for the current case
				if (freeFormCases.contains(caseId))
				{
					valid = true;
				}
				
				// if the case cannot be found in any of the lists,
				// then it is an invalid case for this cancer study
				if (!valid)
				{
					invalidCases.add(caseId);
				}
			}
		}
		
		return invalidCases;
	}
	
	/**
	 * Shortens the (possibly long) user-defined case id list by hashing.
	 * Also adds the generated (key, text) pair to the database for
	 * future reference.
	 * 
	 * @param caseIds	case ID string to be shortened
	 * @return			short (hashed) version of case IDs
	 * @throws DaoException 
	 */
	public static String shortenCaseIds(String caseIds)
			throws DaoException
	{
		DaoTextCache dao = new DaoTextCache();
		
		// normalize case IDs list to avoid redundant white spaces  
		String normalizedIds = normalizeCaseIds(caseIds);
		
		// generate hash key for the case IDs string
		String caseIdsKey = dao.generateKey(normalizedIds);
		
		// add new key and list pair to DB if record does not exist
		if (dao.getText(caseIdsKey) == null)
		{
			dao.cacheText(caseIdsKey, normalizedIds);
		}
		
		// return hash key
		return caseIdsKey;
	}
	
	/**
	 * Retrieves the case ID list corresponding to the given key.
	 * 
	 * @param caseIdsKey	key for a specific case id list
	 * @return				case id list corresponding to the given key
	 * @throws DaoException 
	 */
	public static String getCaseIds(String caseIdsKey)
			throws DaoException
	{
		DaoTextCache dao = new DaoTextCache();
		
		String caseIds = dao.getText(caseIdsKey);
		
		return caseIds;
	}
	
	/**
	 * Normalize the given case list by trimming and replacing any white space
	 * character with single space. This is to prevent same case lists 
	 * to be interpreted as different lists just because of the different white
	 * space characters.
	 * 
	 * @param caseIds	list of case ids to be normalized
	 * @return			normalized string with the same case ids
	 */
	public static String normalizeCaseIds(String caseIds)
	{
		return caseIds.trim().replaceAll("\\s", " ");
	}
}
