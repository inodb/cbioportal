<%@ page import="org.json.simple.JSONObject"%>

<style>
    #survival .survival_stats_table {
        margin-top: 10px;
        margin-bottom: 30px;
        margin-left: 95px;
        width: 620px;
        background-color: #FEFFC5;
        height: 80px;
        width: 720px;
    }
    #survival td{
        width: 140px;
        font-size: 13px;
        font-family: Arial, Helvetica, sans-serif;
        text-align: center;
        border: 1px solid #D8D8D8;
    }
    #survival h4{
        margin-left: 60px;
        margin-top: 20px;
        font-size: 150%;
        height: 30px;
    }
    #survival .img_buttons{
        font-size: 13px;
        display: inline;
        padding-left: 5px;
    }
</style>

<script>
<<<<<<< local
    var cancer_study_id = "<%out.print(cancer_study_id);%>",
            case_set_id = "<%out.print(case_set_id);%>";
    var case_ids_key = "";
    if (case_set_id === "-1") {
        case_ids_key = "<%out.print(caseIdsKey);%>";
=======
    var cancer_study_id = "<%out.print(cancer_study_id);%>";
    var patient_set_id = "<%out.print(patient_set_id);%>";
    var patient_ids_key = "";
    if (patient_set_id === "-1") {
        patient_ids_key = "<%out.print(patientIdsKey);%>";
>>>>>>> other
    }
</script>
<<<<<<< local
<script type="text/javascript" src="js/src/survival-tab.js"></script>
=======

<script type="text/javascript" src="js/src/survival_tab.js?<%=GlobalProperties.getAppVersion()%>"></script>
<script type="text/javascript" src="js/src/survival-curve/survivalCurveView.js?<%=GlobalProperties.getAppVersion()%>"></script>
<script type="text/javascript" src="js/src/survival-curve/survivalCurveProxy.js?<%=GlobalProperties.getAppVersion()%>"></script>
<script type="text/javascript" src="js/src/survival-curve/component/survivalCurve.js?<%=GlobalProperties.getAppVersion()%>"></script>
<script type="text/javascript" src="js/src/survival-curve/component/kmEstimator.js?<%=GlobalProperties.getAppVersion()%>"></script>
<script type="text/javascript" src="js/src/survival-curve/component/logRankTest.js?<%=GlobalProperties.getAppVersion()%>"></script>
<script type="text/javascript" src="js/src/survival-curve/component/boilerPlate.js?<%=GlobalProperties.getAppVersion()%>"></script>
>>>>>>> other

<div class="section" id="survival">
    <h4 id='os_header'>Overall Survival Kaplan-Meier Estimate</h4>
    <div id="os_survival_curve"></div>
    <div class="survival_stats_table" id="os_stat_table"></div>
    <h4 id='dfs_header'>Disease Free Survival Kaplan-Meier Estimate</h4>
    <div id="dfs_survival_curve"></div>
    <div class="survival_stats_table" id="dfs_stat_table"></div>
</div>

<script>
    function getSurvivalPlotsCaseList() {
        <%
            JSONObject result = new JSONObject();
<<<<<<< local
            for (String caseId : mergedCaseList) {
=======
            for (String caseId : mergedPatientList) { 
>>>>>>> other
                if (dataSummary.isCaseAltered(caseId)) {
                    result.put(caseId, "altered");
                } else {
                    result.put(caseId, "unaltered");
                }
            }
        %>
        var obj = jQuery.parseJSON('<%=result%>');
        return obj;
    }

    $(document).ready(function() {
<<<<<<< local
        survivalCurves.init(getSurvivalPlotsCaseList());
=======
        SurvivalTab.init(getSurvivalPlotsCaseList());
>>>>>>> other
    });
</script>