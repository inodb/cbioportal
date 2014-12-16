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

package org.mskcc.cbio.portal.dao;

import org.mskcc.cbio.portal.model.CanonicalGene;

import java.util.Comparator;

/**
 * Compares two genes by their HUGO Symbols--ignores case
 */
class GeneComparator implements Comparator {

    public int compare(Object o, Object o1) {
        CanonicalGene gene0 = (CanonicalGene) o;
        CanonicalGene gene1 = (CanonicalGene) o1;
        return (gene0.getHugoGeneSymbolAllCaps().compareTo(gene1.getHugoGeneSymbolAllCaps()));
    }
}