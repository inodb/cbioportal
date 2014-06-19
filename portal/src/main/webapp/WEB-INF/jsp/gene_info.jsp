<%@ page import="org.mskcc.cbio.portal.model.GeneWithScore" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.mskcc.cbio.portal.dao.DaoSangerCensus" %>
<%@ page import="org.mskcc.cbio.portal.model.SangerCancerGene" %>
<%@ page import="java.io.IOException" %>
<div id="gene_info">
<p><h4>Sanger Cancer Gene Census Information:</h4>

<%
DaoSangerCensus daoSangerCensus = DaoSangerCensus.getInstance();
HashMap<String, SangerCancerGene> censusMap = daoSangerCensus.getCancerGeneSet();
int numCancerGenes = getNumCancerGenes(geneWithScoreList, censusMap);
if (numCancerGenes > 0) {
    out.println ("<P><B>" + numCancerGenes + "</B> of your query genes are known cancer genes, as cataloged"
        + " by the <a href='http://www.sanger.ac.uk/genetics/CGP/Census/'>Sanger Cancer Gene Census</a>:");
%>
<p>
<div class="map">
<table width=98%>
    <tr>
        <th>Gene</th>
        <th>Cancer Types (Somatic)</th>
        <th>Cancer Types (Germline)</th>
        <th>Tissue Types</th>
        <th>Mutation Types</th>
    </tr>
<% for (GeneWithScore geneWithScore : geneWithScoreList) {

    if (censusMap.containsKey(geneWithScore.getGene().trim())) {
        SangerCancerGene cancerGene = censusMap.get(geneWithScore.getGene());
        out.println ("<tr bgcolor=#FFFFFF><td><a href='http://www.ncbi.nlm.nih.gov/gene/"
               + cancerGene.getGene().getEntrezGeneId()
               + "'>" + cancerGene.getGene().getHugoGeneSymbolAllCaps() + "</a></td>");
        ArrayList <String> tumorTypesSomatic = cancerGene.getTumorTypesSomaticMutationList();
        outputParts(out, tumorTypesSomatic);
        ArrayList <String> tumorTypesGermline = cancerGene.getTumorTypesGermlineMutationList();
        outputParts(out, tumorTypesGermline);
        ArrayList <String> tissueTypes = cancerGene.getTissueTypeList();
        outputParts(out, tissueTypes);
        ArrayList <String> mutationTypes = cancerGene.getMutationTypeList();
        outputParts(out, mutationTypes);
        out.println ("</tr>");
    }
} %>
</table>
</div>
<br>
<% } else {
    out.println ("<p>None of your query genes are known cancer genes, as cataloged"
        + " by the <a href='http://www.sanger.ac.uk/genetics/CGP/Census/'>Sanger Cancer Gene Census</a>.");
} %>
</div>

<%!
    private int getNumCancerGenes(ArrayList <GeneWithScore> geneWithScoreList,
            HashMap<String, SangerCancerGene> censusMap) throws IOException {
        int numCancerGenes = 0;
        for (GeneWithScore geneWithScore : geneWithScoreList) {
            if (censusMap.containsKey(geneWithScore.getGene().trim())) {
                numCancerGenes++;
            }
        }
        return numCancerGenes;
    }

    private void outputParts(JspWriter out, ArrayList<String> partsList) throws IOException {
        out.println ("<td>");
        int counter = 0;
        for (String part:  partsList) {
            out.print (part.trim());
            counter++;
            if (counter < partsList.size()) {
                out.println (", ");
            }
        }
        out.println ("</td>");
    }
%>
