<%@ page import="org.codehaus.jackson.map.ObjectMapper" %>
<%@ page import="org.mskcc.cbio.cgds.model.ExtendedMutation" %>
<%@ page import="org.mskcc.cbio.portal.html.MutationTableUtil" %>
<%@ page import="org.mskcc.cbio.portal.model.ExtendedMutationMap" %>
<%@ page import="org.mskcc.cbio.portal.model.GeneWithScore" %>
<%@ page import="org.mskcc.cbio.portal.servlet.QueryBuilder" %>
<%@ page import="org.mskcc.cbio.portal.util.MutationCounter" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.StringWriter" %>

<script type="text/javascript" src="js/raphael/raphael.js"></script>
<script type="text/javascript" src="js/mutation_diagram.js"></script>

<%
    ArrayList<ExtendedMutation> extendedMutationList = (ArrayList<ExtendedMutation>)
            request.getAttribute(QueryBuilder.INTERNAL_EXTENDED_MUTATION_LIST);
    ExtendedMutationMap mutationMap = new ExtendedMutationMap(extendedMutationList,
            mergedProfile.getCaseIdList());

    out.println("<div class='section' id='mutation_details'>");

    if (mutationMap.getNumGenesWithExtendedMutations() > 0) {
        for (GeneWithScore geneWithScore : geneWithScoreList) {
            outputGeneTable(geneWithScore, mutationMap, out, mergedCaseList);
        }
    } else {
        outputNoMutationDetails(out);
    }
    out.println("</div>");
%>

<style type="text/css" title="currentStyle"> 
        .mutation_datatables_filter {
                width: 40%;
                float: right;
                padding-top:5px;
                padding-bottom:5px;
                padding-right:5px;
        }
        .mutation_datatables_info {
                width: 55%;
                float: left;
                padding-left:5px;
                padding-top:7px;
                font-size:90%;
        }
</style>

<script type="text/javascript">
    jQuery.fn.dataTableExt.oSort['aa-change-col-asc']  = function(a,b) {
        var ares = a.match(/.*[A-Z]([0-9]+)[^0-9]+/);
        var bres = b.match(/.*[A-Z]([0-9]+)[^0-9]+/);
        
        if (ares) {
            if (bres) {
                var ia = parseInt(ares[1]);
                var ib = parseInt(bres[1]);
                return ia==ib ? 0 : (ia<ib ? -1:1);
            } else {
                return -1;
            }
        } else {
            if (bres) {
                return 1;
            } else {
                return a==b ? 0 : (a<b ? -1:1);
            }
        }
    };

    jQuery.fn.dataTableExt.oSort['aa-change-col-desc'] = function(a,b) {
        var ares = a.match(/.*[A-Z]([0-9]+)[^0-9]+/);
        var bres = b.match(/.*[A-Z]([0-9]+)[^0-9]+/);
        
        if (ares) {
            if (bres) {
                var ia = parseInt(ares[1]);
                var ib = parseInt(bres[1]);
                return ia==ib ? 0 : (ia<ib ? 1:-1);
            } else {
                return -1;
            }
        } else {
            if (bres) {
                return 1;
            } else {
                return a==b ? 0 : (a<b ? 1:-1);
            }
        }
    };
    
    function assignValueToPredictedImpact(str) {
        if (str=="Low") {
            return 1;
        } else if (str=="Medium") {
            return 2;
        } else if (str=="High") {
            return 3;
        } else {
            return 0;
        }
    }
    
    jQuery.fn.dataTableExt.oSort['predicted-impact-col-asc']  = function(a,b) {
        var av = assignValueToPredictedImpact(a.replace(/<[^>]*>/g,""));
        var bv = assignValueToPredictedImpact(b.replace(/<[^>]*>/g,""));
        
        if (av>0) {
            if (bv>0) {
                return av==bv ? 0 : (av<bv ? -1:1);
            } else {
                return -1;
            }
        } else {
            if (bv>0) {
                return 1;
            } else {
                return a==b ? 0 : (a<b ? 1:-1);
            }
        }
    };
    
    jQuery.fn.dataTableExt.oSort['predicted-impact-col-desc']  = function(a,b) {
        var av = assignValueToPredictedImpact(a.replace(/<[^>]*>/g,""));
        var bv = assignValueToPredictedImpact(b.replace(/<[^>]*>/g,""));
        
        if (av>0) {
            if (bv>0) {
                return av==bv ? 0 : (av<bv ? 1:-1);
            } else {
                return -1;
            }
        } else {
            if (bv>0) {
                return 1;
            } else {
                return a==b ? 0 : (a<b ? -1:1);
            }
        }
    };

    //  Place mutation_details_table in a JQuery DataTable
    $(document).ready(function(){
        <%
        for (GeneWithScore geneWithScore : geneWithScoreList) {
            if (mutationMap.getNumExtendedMutations(geneWithScore.getGene()) > 0) { %>
              $('#mutation_details_table_<%= geneWithScore.getGene().toUpperCase() %>').dataTable( {
                  "sDom": '<"H"<"mutation_datatables_filter"f><"mutation_datatables_info"i>>t',
                  "bPaginate": false,
                  "bFilter": true,
                  "aoColumnDefs":[
                      {"sType": 'aa-change-col',
                              "aTargets": [ 5 ]},
                      {"sType": 'predicted-impact-col',
                              "aTargets": [ 6 ]}
                  ]
              } );
            <% } %>
        <% } %>
    });
    
    //  Set up Mutation Diagrams
    $(document).ready(function(){
	    // initially hide all tooltip boxes
	    $("div.mutation_diagram_details").hide();
    <%
    for (GeneWithScore geneWithScore : geneWithScoreList) {
        if (mutationMap.getNumExtendedMutations(geneWithScore.getGene()) > 0) { %>
          $.ajax({ url: "mutation_diagram_data.json",
              dataType: "json",
              data: { hugoGeneSymbol: "<%= geneWithScore.getGene().toUpperCase() %>", mutations: "<%= outputMutationsJson(geneWithScore, mutationMap) %>" },
              success: drawMutationDiagram,
              type: "POST"});
        <% } %>
    <% } %>
    });         
</script>


<%!

    private String outputMutationsJson(final GeneWithScore geneWithScore, final ExtendedMutationMap mutationMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        StringWriter stringWriter = new StringWriter();
        List<ExtendedMutation> mutations = mutationMap.getExtendedMutations(geneWithScore.getGene());
        try {
            objectMapper.writeValue(stringWriter, mutations);
        }
        catch (Exception e) {
            // ignore
        }
        return stringWriter.toString().replace("\"", "\\\"");
    }

    private void outputGeneTable(GeneWithScore geneWithScore,
            ExtendedMutationMap mutationMap, JspWriter out, 
            ArrayList<String> mergedCaseList) throws IOException {
        MutationTableUtil mutationTableUtil = new MutationTableUtil(geneWithScore.getGene());
        MutationCounter mutationCounter = new MutationCounter(geneWithScore.getGene(),
                mutationMap);

        if (mutationMap.getNumExtendedMutations(geneWithScore.getGene()) > 0) {
            outputHeader(out, geneWithScore, mutationCounter);
            outputOmaHeader(out);
            out.println("<table cellpadding='0' cellspacing='0' border='0' " +
                    "class='display mutation_details_table' " +
                    "id='mutation_details_table_" + geneWithScore.getGene().toUpperCase()
                    +"'>");

            //  Table column headers
            out.println("<thead>");
            out.println(mutationTableUtil.getTableHeaderHtml() + "<BR>");
            out.println("</thead>");

            //  Mutations are sorted by case
            out.println("<tbody>");
            for (String caseId : mergedCaseList) {
                ArrayList<ExtendedMutation> mutationList =
                        mutationMap.getExtendedMutations(geneWithScore.getGene(), caseId);
                if (mutationList != null && mutationList.size() > 0) {
                    for (ExtendedMutation mutation : mutationList) {
                        out.println(mutationTableUtil.getDataRowHtml(mutation));
                    }
                }
            }
            out.println("</tbody>");

            //  Table column footer
            out.println("<tfoot>");
            out.println(mutationTableUtil.getTableHeaderHtml());
            out.println("</tfoot>");

            out.println("</table><p><br>");
            out.println(mutationTableUtil.getTableFooterMessage());
            out.println("<br>");
        }
    }

    private void outputHeader(JspWriter out, GeneWithScore geneWithScore,
            MutationCounter mutationCounter) throws IOException {
        out.print("<h4>" + geneWithScore.getGene().toUpperCase() + ": ");
        out.println(mutationCounter.getTextSummary());
        out.println("</h4>");
        out.println("<div id='mutation_diagram_" + geneWithScore.getGene().toUpperCase() + "'></div>");
        out.println("<div class='mutation_diagram_details' id='mutation_diagram_details_" + geneWithScore.getGene().toUpperCase() + "'>The height of the bars indicates the number of mutations at each position.<BR>Roll-over the dots and domains to view additional details.<BR>Domain details derived from <a href='http://pfam.sanger.ac.uk/'>Pfam</a>.</div>");
    }

    private void outputNoMutationDetails(JspWriter out) throws IOException {
        out.println("<p>There are no mutation details available for the gene set entered.</p>");
        out.println("<br><br>");
    }

    private void outputOmaHeader(JspWriter out) throws IOException {
        out.println("<br>** Predicted functional impact (via " +
                "<a href='http://mutationassessor.org'>Mutation Assessor</a>)" +
                " is provided for missense mutations only.  ");
        out.println("<br>");
    }
%>
