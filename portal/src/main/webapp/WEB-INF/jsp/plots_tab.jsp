<%@ page import="org.mskcc.cbio.portal.model.GeneWithScore" %>
<%@ page import="org.mskcc.cbio.portal.servlet.QueryBuilder" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.io.IOException" %>
<%@ page import="org.mskcc.cbio.portal.model.GeneticProfile" %>
<%@ page import="org.mskcc.cbio.portal.model.GeneticAlterationType" %>

<script type="text/javascript" src="js/src/plots-tab/plotsTab.js"></script>
<script type="text/javascript" src="js/src/plots-tab/view/sidebar.js"></script>
<script type="text/javascript" src="js/src/plots-tab/view/plotsbox.js"></script>
<script type="text/javascript" src="js/src/plots-tab/proxy/metaData.js"></script>
<script type="text/javascript" src="js/src/plots-tab/proxy/plotsData.js"></script>
<script type="text/javascript" src="js/src/plots-tab/util/map.js"></script>
<script type="text/javascript" src="js/src/plots-tab/util/plotsUtil.js"></script>
<script type="text/javascript" src="js/src/plots-tab/util/mutationInterpreter.js"></script>
<script type="text/javascript" src="js/src/plots-tab/util/gisticInterpreter.js"></script>
<script type="text/javascript" src="js/src/plots-tab/util/clinicalDataInterpreter.js"></script>
<script type="text/javascript" src="js/src/plots-tab/util/stylesheet.js"></script>
<script type="text/javascript" src="js/src/plots-tab/view/components/profileSpec.js"></script>
<script type="text/javascript" src="js/src/plots-tab/view/components/clinSpec.js"></script>
<script type="text/javascript" src="js/src/plots-tab/view/components/optSpec.js"></script>
<script type="text/javascript" src="js/src/plots-tab/view/components/scatterPlots.js"></script>
<script type="text/javascript" src="js/src/plots-tab/view/components/boxPlots.js"></script>
<script type="text/javascript" src="js/src/plots-tab/view/components/heatMap.js"></script>

<style>
    #plots .plots {
        border: 1px solid #aaaaaa;
        border-radius: 4px;
        margin: 15px;
    }
    
    #plots-sidebar {
        width: 320px;
    }
    #plots-sidebar-x-div {
        width: inherit;
        height: 202px;
    }
    #plots-sidebar-y-div {
        width: inherit;
        height: 202px;
    }
    #plots-sidebar-util-div {
        width: inherit;
        height: 170px;
    }

    #plots-sidebar h4 {
        margin: 15px;
        font-size: 12px;
        color: grey;
        background-color: white;
        margin-top: -6px;
        display: table;
        padding: 5px;
    }
    #plots-sidebar h5 {
        margin-left: 20px;
        padding-left: 5px;
        padding-right: 5px;
        display: inline-block;
        margin-bottom: 10px;
    }
    #plots-sidebar select {
        max-width: 180px;
    }
    #plots-box {
        width: 820px;
        height: 610px;
        float: right;
    }
</style>

<div class="section" id="plots">
    <table>
        <tr>
            <td>
                 <div id="plots-sidebar">
                    <div id="plots-sidebar-x-div" class="plots">
                        <h4>Horizontal Axis</h4>
                        <br><h5>Data Type</h5> 
                        <select id="plots-x-data-type">
                            <option value="genetic_profile">Genetic Profile</option>
                            <option value="clinical_attribute">Clinical Attribute</option>
                        </select>
                        <div id="plots-x-spec"></div>
                    </div>
                    <div id="plots-sidebar-y-div" class="plots">
                        <h4>Vertical Axis</h4>
                        <br><h5>Data Type</h5>
                        <select id="plots-y-data-type">
                            <option value="genetic_profile">Genetic Profile</option>
                            <option value="clinical_attribute">Clinical Attribute</option>
                        </select>
                        <div id="plots-y-spec"></div>
                    </div>
                    <div id="plots-sidebar-util-div" class="plots">
                        <h4>Utilities</h4>
                        <h5>Search Case(s)</h5><input type="text" id="case_id_search_keyword" name="case_id_search_keyword" placeholder="Case ID.." onkeyup="search_case_id();"><br>
                        <h5>Search Mutation(s)</h5><input type="text" id="mutation_search_keyword" name="mutation_search_keyword" placeholder="Protein Change.." onkeyup="search_mutation();"><br>
                        <h5>Download</h5><button type="button">SVG</button><button type="button">PDF</button>
                        <div id="mutation_details_vs_gistic_view" class="mutation_details_vs_gistic_view" style="display:inline;"></div>
                        
                    </div>        
                </div>
            </td>
            <td>
                <div id="plots-box" class="plots">
                </div>
            </td>
        </tr>
    </table>
</div>


<script>
    $(document).ready( function() {
        plotsTab.init();
    });
</script>

