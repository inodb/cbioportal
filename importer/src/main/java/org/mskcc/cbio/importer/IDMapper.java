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
package org.mskcc.cbio.importer;

// imports

import scala.Tuple2;

/**
 * Interface used to map IDS.
 */
public interface IDMapper {

	/**
	 *
	 * @param chromosome
	 * @param position
	 * @param strand
	 * @return
	 */

	public String findGeneNameByGenomicPosition(String chromosome, String position,String strand);

	/**
	 * For the given symbol, return id.
	 *
	 * @param geneSymbol String
	 * @return String
	 * @throws Exception
	 */
	String symbolToEntrezID(String geneSymbol)  throws Exception;

	/**
	 * For the entrezID, return symbol.
	 *
	 * @param entrezID String
	 * @return String
	 * @throws Exception
	 */
	String entrezIDToSymbol(String entrezID) throws Exception;

	/**
	 * returns the Gene Symbol and Entrez ID for a specified Ensembl ID
	 * @param ensemblID
	 * @return
	 */

	Tuple2<String,String> ensemblToHugoSymbolAndEntrezID(String ensemblID);
}
