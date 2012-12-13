
<%@ page import="org.mskcc.cbio.portal.servlet.TumorMapServlet" %>
<%@ page import="org.mskcc.cbio.portal.servlet.QueryBuilder" %>

<%
request.setAttribute("tumormap", true);
%>

<jsp:include page="../global/header.jsp" flush="true" />
<!--content start-->
<style type="text/css" title="currentStyle"> 
        @import "css/data_table_jui.css";
        @import "css/data_table_ColVis.css";
        .tumormap-datatable-name {
                float: left;
                font-weight: bold;
                font-size: 120%;
                vertical-align: middle;
        }
</style>

<script type="text/javascript">
    var tumormap_cancerstudies = null;
    $(document).ready(function(){
        $('#cancer_study_wrapper_table').hide();
        var params = {
            <%=TumorMapServlet.CMD%>:'<%=TumorMapServlet.GET_STUDY_STATISTICS_CMD%>'
        };
                        
        $.post("tumormap.json", 
            params,
            function(data){
                tumormap_cancerstudies = data;
                var ids = [];
                for (var id in data) {
                    ids.push([id]);
                }
                
                var oTable = $('#cancer_study_table').dataTable({
                    "sDom": '<"H"<"tumormap-datatable-name">fr>t>',
                    "bJQueryUI": true,
                    "bDestroy": true,
                    "aaData": ids,
                    "aoColumnDefs":[
                        {// study id
                            "aTargets": [0],
                            "bVisible": false,
                            "mData": 0
                        },
                        {// study name
                            "aTargets": [1],
                            "mDataProp": function(source,type,value) {
                                if (type==='set') {
                                    return;
                                } else {
                                    var id = source[0];
                                    var name = tumormap_cancerstudies[id]['name'];
                                    if (type==='display') {
                                        return "<a href='study.do?cancer_study_id="+id+"'><b>"+name+"</b></a>";
                                    } else {
                                        return tumormap_cancerstudies[source[0]]['name'];
                                    }
                                }
                            }
                        },
                        {// ref
                            "aTargets": [2],
                            "mDataProp": function(source,type,value) {
                                if (type==='set') {
                                    return;
                                } else {
                                    var ref = tumormap_cancerstudies[source[0]]['citation'];
                                    if (!ref) return '';
                                    var pmid = tumormap_cancerstudies[source[0]]['pmid'];
                                    if (pmid) ref = '<a href="http://www.ncbi.nlm.nih.gov/pubmed/'+pmid+'">'+ref+'</a>';
                                    return ref;
                                }
                            }
                        },
                        {// cases
                            "aTargets": [3],
                            "sClass": "right-align-td",
                            "bSearchable": false,
                            "mDataProp": function(source,type,value) {
                                if (type==='set') {
                                    return;
                                } else {
                                    return tumormap_cancerstudies[source[0]]['cases'];
                                }
                            }
                        },
                        {// mut
                            "aTargets": [4],
                            "sClass": "right-align-td",
                            "bSearchable": false,
                            "mDataProp": function(source,type,value) {
                                if (type==='set') {
                                    return;
                                } else {
                                    var id = source[0];
                                    var mut = tumormap_cancerstudies[id]['mut'];
                                    if (type==='display') {
                                        if (mut==null) return '<img width=12 height=12 src="images/ajax-loader2.gif"/>';
                                        return mut ? mut.toFixed(0) : '';
                                    } else {
                                        return mut ? mut : 0.0;
                                    }
                                }
                            }
                        },
                        {// cna
                            "aTargets": [5],
                            "sClass": "right-align-td",
                            "bSearchable": false,
                            "mDataProp": function(source,type,value) {
                                if (type==='set') {
                                    return;
                                } else {
                                    var id = source[0];
                                    var cna = tumormap_cancerstudies[id]['cna'];
                                    if (type==='display') {
                                        if (cna==null) return '<img width=12 height=12 src="images/ajax-loader2.gif"/>';
                                        return cna ? ((cna*100).toFixed(1)+'%') : '';
                                    } else {
                                        return cna ? cna : 0.0;
                                    }
                                }
                            }
                        }
                    ],
                    "aaSorting": [[1,'asc']],
                    "oLanguage": {
                        "sInfo": "&nbsp;&nbsp;(_START_ to _END_ of _TOTAL_)&nbsp;&nbsp;",
                        "sInfoFiltered": "",
                        "sLengthMenu": "Show _MENU_ per page"
                    },
                    "iDisplayLength": -1
                });
                
                $(".tumormap-datatable-name").html("Available data sets");
                oTable.css("width","100%");
                $('#cancer_study_wait').hide();
                $('#cancer_study_wrapper_table').show();
                
                updateMutCnaStatistics('mut',4,oTable);
                updateMutCnaStatistics('cna',5,oTable);
            }
            ,"json"
        );
    });
    
    function updateMutCnaStatistics(type, col, oTable) {
        var row = 0;
        var rowIndex = {};
        for (var id in tumormap_cancerstudies) {
            rowIndex[id] = row++;
            var params = {
                <%=TumorMapServlet.CMD%>:'<%=TumorMapServlet.GET_STUDY_STATISTICS_CMD%>',
                <%=TumorMapServlet.GET_STUDY_STATISTICS_TYPE%>:type,
                <%=QueryBuilder.CANCER_STUDY_ID%>:id
            };

            $.post("tumormap.json", 
                params,
                function(data){
                    for (var id2 in data) {
                        $.extend(tumormap_cancerstudies[id2],data[id2]);
                        oTable.fnUpdate(null,rowIndex[id2],col);
                    }
                }
                ,"json"
            );
        }
    }
    
</script>

<div id="cancer_study_wait"><img src="images/ajax-loader.gif"/> Loading cancer studies</div>
<table cellpadding="0" cellspacing="0" border="0" id="cancer_study_wrapper_table" width="100%">
    <tr>
        <td>
            <table cellpadding="0" cellspacing="0" border="0" class="display" id="cancer_study_table">
                <thead>
                    <tr valign="bottom">
                        <th>Cancer Study ID</th>
                        <th class="cancer-study-header" alt="Cohort cancer studies">Cancer Study</th>
                        <th class="cancer-study-header" alt="Reference for published studies">Reference</th>
                        <th class="cancer-study-header" alt="Number of cases">Cases</th>
                        <th class="cancer-study-header" alt="Average number of mutations across samples">Avg. # Mut.</th>
                        <th class="cancer-study-header" alt="Average fraction of copy number altered genome across samples">Avg. % CNA</th>
                    </tr>
                </thead>
            </table>
        </td>
    </tr>
</table>
<!--content end-->
        </div>
    </td>
</tr>

<tr>
    <td colspan="3">
	<jsp:include page="../global/footer.jsp" flush="true" />
    </td>
</tr>

</table>
</center>
</div>
<jsp:include page="../global/xdebug.jsp" flush="true" />

</body>
</html>