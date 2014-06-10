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

import org.mskcc.cbio.portal.model.ExtendedMutationMap;
import org.mskcc.cbio.portal.model.ExtendedMutation;

import java.util.ArrayList;
import java.text.DecimalFormat;

/**
 * Calculates Somatic and Germline Mutation Frequency.
 *
 * @author Ethan Cerami.
 */
public class MutationCounter {
    private int numCasesWithSomaticMutation = 0;
    private int numCasesWithGermlineMutation = 0;
    private int numCasesWithMutation = 0;
    private ExtendedMutationMap mutationMap;
    private String gene;

    private int totalNumCases;

    public MutationCounter (String gene, ExtendedMutationMap mutationMap) {
        this.gene = gene;
        this.mutationMap = mutationMap;
        totalNumCases = mutationMap.getCaseList().size();
        for (String caseId:  mutationMap.getCaseList()) {
            if (caseIsMutated(caseId)) {
                numCasesWithMutation++;
                MutationStatus mutationStatus = getMutationStatus (caseId);
                incrementCounters(mutationStatus);
            }
        }
    }

    public String getTextSummary() {
        StringBuffer summary = new StringBuffer();
        DecimalFormat percentFormat = new DecimalFormat("###,###.#%");
        summary.append("[");
        if (getGermlineMutationRate() > 0) {
            summary.append("Germline Mutation Rate:  ");
            summary.append(percentFormat.format(getGermlineMutationRate()));
            summary.append(", ");
        }
        summary.append("Somatic Mutation Rate:  ");
        summary.append(percentFormat.format(getSomaticMutationRate()));
        summary.append("]");
        return summary.toString();
    }

    public double getSomaticMutationRate() {
        return numCasesWithSomaticMutation / (float) totalNumCases;
    }

    public double getGermlineMutationRate() {
        return numCasesWithGermlineMutation / (float) totalNumCases;
    }

    public double getMutationRate() {
        return numCasesWithMutation / (float) totalNumCases;
    }

    private void incrementCounters(MutationStatus mutationStatus) {
        if (mutationStatus.isCaseGermlineMutated()) {
            numCasesWithGermlineMutation++;
        }
        if (mutationStatus.isCaseSomaticallyMutated()) {
            numCasesWithSomaticMutation++;
        }
    }

    private boolean caseIsMutated(String caseId) {
        ArrayList<ExtendedMutation> mutationList = mutationMap.getExtendedMutations(gene, caseId);
        return mutationList != null && mutationList.size() > 0;
    }

    private MutationStatus getMutationStatus(String caseId) {
        ArrayList<ExtendedMutation> mutationList = mutationMap.getExtendedMutations(gene, caseId);
        MutationStatus mutationStatus = new MutationStatus();
        for (ExtendedMutation mutation:  mutationList) {
            setMutationStatus(mutation, mutationStatus);
        }
        return mutationStatus;
    }

    private void setMutationStatus(ExtendedMutation mutation, MutationStatus mutationStatus) {
        if (mutation.isGermlineMutation()) {
            mutationStatus.setGermlineMutated(true);
        } else {
            mutationStatus.setSomaticMutated(true);
        }
    }
}

class MutationStatus {
    private boolean germlineMutated;
    private boolean somaticMutated;

    public boolean isCaseGermlineMutated() {
        return germlineMutated;
    }

    public void setGermlineMutated(boolean germlineMutated) {
        this.germlineMutated = germlineMutated;
    }

    public boolean isCaseSomaticallyMutated() {
        return somaticMutated;
    }

    public void setSomaticMutated(boolean somaticMutated) {
        this.somaticMutated = somaticMutated;
    }
}