<%@ page import="org.mskcc.cbio.cgds.model.CancerStudy" %>
<%@ page import="org.mskcc.cbio.portal.oncoPrintSpecLanguage.Utilities" %>
<%@ page import="org.mskcc.cbio.portal.servlet.QueryBuilder" %>
<%@ page import="org.mskcc.cbio.portal.servlet.ServletXssUtil" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.mskcc.cbio.portal.util.SkinUtil" %>
<%@ page import="java.io.IOException" %>
<%@ page import="org.mskcc.cbio.cgds.model.ExtendedMutation" %>
<%@ page import="org.mskcc.cbio.portal.model.ExtendedMutationMap" %>
<%@ page import="org.mskcc.cbio.portal.util.MutationCounter" %>
<%@ page import="org.codehaus.jackson.map.ObjectMapper" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="java.util.List" %>
<%@ page import="org.mskcc.cbio.cgds.model.CaseList" %>
<%@ page import="java.text.DecimalFormat" %>

<script type="text/javascript" src="js/raphael/raphael.js"></script>
<script type="text/javascript" src="js/mutation_diagram.js"></script>

<%
    String siteTitle = SkinUtil.getTitle();
    request.setAttribute(QueryBuilder.HTML_TITLE, siteTitle);
    ArrayList<CancerStudy> cancerStudies = (ArrayList<CancerStudy>)
            request.getAttribute(QueryBuilder.CANCER_TYPES_INTERNAL);

    // Get priority settings
    Integer dataPriority;
    try {
        dataPriority
                = Integer.parseInt(request.getParameter(QueryBuilder.DATA_PRIORITY).trim());
    } catch (Exception e) {
        dataPriority = 0;
    }

    // Divide histograms only if we are interested in mutations
    boolean divideHistograms = (dataPriority != 2);

    // Check if we wanna show only the mutation data
    boolean onlyMutationData = (dataPriority == 1);

    int skippedCancerStudies = 0;
    // Now pool the cancer studies
    ArrayList<CancerStudy> primaryStudies = new ArrayList<CancerStudy>(),
                           secondaryStudies = new ArrayList<CancerStudy>();
    for(CancerStudy cancerStudy: cancerStudies) {
        if(!divideHistograms) {
            // A dirty maneuver to show all studies within a single histogram
            if(cancerStudy.hasCnaData()) {
                primaryStudies.add(cancerStudy);
            } else {
                skippedCancerStudies++;
            }
        } else if(cancerStudy.hasMutationData()) {
            primaryStudies.add(cancerStudy);
        } else {
            // If user wants to get only the mutation data
            // don't even add these studies to the list since they don't have any
            if(!onlyMutationData) {
                secondaryStudies.add(cancerStudy);
            } else {
                skippedCancerStudies++;
            }
        }
    }

    // Now let's reorder the loads
    cancerStudies.clear();
    cancerStudies.addAll(primaryStudies);
    cancerStudies.addAll(secondaryStudies);

    ServletXssUtil servletXssUtil = ServletXssUtil.getInstance();
    String geneList = servletXssUtil.getCleanInput(request, QueryBuilder.GENE_LIST);

    // Infer whether there is multiple genes or not (for histogram switching)
    int geneCount = 0;
    if(geneList.contains(":")) {
        for (String line : geneList.split("\\r?\\n")) {
            for (String token : line.trim().split(";")) {
                if(token.trim().length() > 0)
                    geneCount++;
            }
        }
    } else {
        for (String words : geneList.split(" ")) {
            for (String token : words.split("\\r?\\n")) {
                if(token.trim().length() > 0)
                    geneCount++;
            }
        }
    }

    boolean multipleGenes = geneCount > 1;

    //  Prepare gene list for URL.
    //  Extra spaces must be removed.  Otherwise AJAX Load will not work.
    geneList = Utilities.appendSemis(geneList);
    geneList = geneList.replaceAll("\\s+", " ");
    geneList = URLEncoder.encode(geneList);

%>

<%
    ArrayList<ExtendedMutation> extendedMutationList = (ArrayList<ExtendedMutation>)
            request.getAttribute(QueryBuilder.INTERNAL_EXTENDED_MUTATION_LIST);

    ArrayList<CaseList> allCaseLists
            = (ArrayList<CaseList>) request.getAttribute(QueryBuilder.CROSS_CANCER_CASESETS);
    ArrayList<String> caseListStrs = new ArrayList<String>();
    for (CaseList aCaseList : allCaseLists) {
        caseListStrs.add(aCaseList.getStableId());
    }
    ExtendedMutationMap mutationMap = new ExtendedMutationMap(extendedMutationList, caseListStrs);
    ArrayList<String> genes = (ArrayList<String>) request.getAttribute(QueryBuilder.CROSS_CANCER_GENES);
%>

<jsp:include page="global/header.jsp" flush="true"/>

<%
    //  Iterate through each Cancer Study
    //  For each cancer study, init AJAX
    //  This is to prevent Chrome crash when loading the page
    String studiesList = "";
    String studiesNames = "";
    assert(cancerStudies.size() > 0);
    for (CancerStudy cancerStudy:  cancerStudies) {
        studiesList += "'" + cancerStudy.getCancerStudyStableId() + "',";
        studiesNames += "'" + cancerStudy.getName() + "',";
    }
    studiesList = studiesList.substring(0, studiesList.length()-1);
    studiesNames = studiesNames.substring(0, studiesNames.length()-1);
%>

<style type="text/css">
    #chart_div5 #mutation_details {
        padding: 20px;
        padding-left: 40px;

    }
</style>

<script type="text/javascript" src="http://www.google.com/jsapi"></script>
<script type="text/javascript">
    google.load("visualization", "1", {packages:["corechart"]});
    var genesQueried = "";
    var shownHistogram = 1;
    var multipleGenes = <%=multipleGenes%>;
    var divideHistograms = <%=divideHistograms%>;
    var onlyMutationData = <%=(dataPriority == 1)%>;
    var maxAlterationPercent = 0;
    var lastStudyLoaded = false;


    $(document).ready(function() {
        $("#crosscancer_summary_message").hide();

        $("#chart_div2").toggle();
        $("#chart_div3").toggle();
        $("#chart_div4").toggle();
        $("#chart_div5").toggle();
        function toggleHistograms() {
	        var histIndex = $("#hist_toggle_box").val();
            $("#chart_div1").hide();
            $("#chart_div2").hide();
            $("#chart_div3").hide();
            $("#chart_div4").hide();
            $("#chart_div5").hide();

            $("#chart_div" + histIndex).show();
            shownHistogram = histIndex;
            drawChart();
            reDrawMutDiagrams();
        }
        $("#hist_toggle_box").change( toggleHistograms );

        var histogramData = new google.visualization.DataTable();
        var histogramData2 = new google.visualization.DataTable();
        var histogramData3 = new google.visualization.DataTable();
        var histogramData4 = new google.visualization.DataTable();

        var histogramChart = new google.visualization.ColumnChart(document.getElementById('chart_div1'));
        var histogramChart2 = new google.visualization.ColumnChart(document.getElementById('chart_div2'));
        var histogramChart3 = new google.visualization.ColumnChart(document.getElementById('chart_div3'));
        var histogramChart4 = new google.visualization.ColumnChart(document.getElementById('chart_div4'));

        var cancerStudies = [<%=studiesList%>];
        var cancerStudyNames = [<%=studiesNames%>];
        var numOfStudiesWithMutData = <%=primaryStudies.size()%>;

        if(!multipleGenes) {
            histogramData.addColumn('string', 'Cancer Study');
            histogramData.addColumn('number', 'Multiple Alterations');
            histogramData.addColumn('number', 'Mutation');
            histogramData.addColumn('number', 'Deletion');
            histogramData.addColumn('number', 'Amplification');

            histogramData2.addColumn('string', 'Cancer Study');
            histogramData2.addColumn('number', 'Multiple Alterations');
            histogramData2.addColumn('number', 'Mutation');
            histogramData2.addColumn('number', 'Deletion');
            histogramData2.addColumn('number', 'Amplification');

            histogramData3.addColumn('string', 'Cancer Study');
            histogramData3.addColumn('number', 'Multiple Alterations');
            histogramData3.addColumn('number', 'Mutation');
            histogramData3.addColumn('number', 'Deletion');
            histogramData3.addColumn('number', 'Amplification');
            histogramData3.addColumn('number', 'Not altered');

            histogramData4.addColumn('string', 'Cancer Study');
            histogramData4.addColumn('number', 'Multiple Alterations');
            histogramData4.addColumn('number', 'Mutation');
            histogramData4.addColumn('number', 'Deletion');
            histogramData4.addColumn('number', 'Amplification');
            histogramData4.addColumn('number', 'Not altered');
        } else {
            histogramData.addColumn('string', 'Cancer Study');
            histogramData.addColumn('number', 'Altered Cases');

            histogramData2.addColumn('string', 'Cancer Study');
            histogramData2.addColumn('number', 'Altered Cases');

            histogramData3.addColumn('string', 'Cancer Study');
            histogramData3.addColumn('number', 'Altered Cases');
            histogramData3.addColumn('number', 'Not Altered Cases');

            histogramData4.addColumn('string', 'Cancer Study');
            histogramData4.addColumn('number', 'Altered Cases');
            histogramData4.addColumn('number', 'Not Altered Cases');
        }

        for(var i=0; i < cancerStudies.length; i++) {
            if(i < numOfStudiesWithMutData ) {
                if(!multipleGenes) {
                    histogramData.addRow([cancerStudyNames[i], 0, 0, 0, 0]);
                    histogramData3.addRow([cancerStudyNames[i], 0, 0, 0, 0, 0]);
                } else {
                    histogramData.addRow([cancerStudyNames[i], 0]);
                    histogramData3.addRow([cancerStudyNames[i], 0, 0]);
                }
            } else {
                if(!multipleGenes) {
                    histogramData2.addRow([cancerStudyNames[i], 0, 0, 0, 0]);
                    histogramData4.addRow([cancerStudyNames[i], 0, 0, 0, 0, 0]);
                } else {
                    histogramData2.addRow([cancerStudyNames[i], 0]);
                    histogramData4.addRow([cancerStudyNames[i], 0, 0]);
                }
            }

        }

        drawChart();

        $("#histogram_sort").tipTip();
        $("#download_histogram").tipTip();

        $("#download_histogram").click(function(event) {
            event.preventDefault();

            var chartContainer = $("#chart_div" + shownHistogram + " div div");
            var svg = chartContainer.html();

            // Our custom form submission
            $("#histogram_svg_xml").val(svg);
            $("#histogram_download_form").submit();
        });

        $("#histogram_sort").click(function(event) {
            event.preventDefault(); // Not to scroll to the top
            sortPermanently = !sortPermanently;

            $(this).css({
                color: !sortPermanently ? "#1974b8" : "gray",
                "text-decoration": !sortPermanently ? "none" : "line-through"
            });

            drawChart();
        });

        $("#toggle_query_form").tipTip();

        loadStudiesWithIndex(0);
        function formatPercent(number) {
            return parseFloat(number.toFixed(3));
        }

        function updateHistograms(bundleIndex, cancerID) {
            var alts = eval("GENETIC_ALTERATIONS_SORTED_" + cancerID
                    + ".get('GENETIC_ALTERATIONS_SORTED_" + cancerID + "')");

            if( genesQueried.length == 0 ) {
                for(var k=0; k < alts.length; k++) {
                    genesQueried += alts[k].hugoGeneSymbol + ", ";
                }

                genesQueried = genesQueried.substr(0, genesQueried.length-2);
                var genesBold = $("<b>").html(genesQueried);
                var geneStr = " for gene";
                if(alts.length > 1)
                    geneStr += "s";

                $("#queried-genes").html(geneStr + " ").append(genesBold);
            }

            var numOfCases = alts[0].alterations.length;
            var numOfMuts = 0;
            var numOfDels = 0;
            var numOfAmp = 0;
            var numOfCombo = 0

            for(var i=0; i < numOfCases; i++) {
                var isMut = false;
                var isAmp = false;
                var isDel = false;

                var altCnt = 0;
                for(var j=0; j < alts.length; j++) {
                    var alt = alts[j].alterations[i].alteration;

                    if( alt & MUTATED ) {
                        isMut = true;
                        altCnt++;
                    }
                    if(alt & CNA_AMPLIFIED) {
                        isAmp = true;
                        altCnt++;
                    }
                    if(alt & CNA_HOMODELETED) {
                        isDel = true;
                        altCnt++
                    }
                }

                if(altCnt > 1)
                    numOfCombo++;
                else if(altCnt == 1) {
                    if(isAmp) {numOfAmp++;}
                    if(isDel) {numOfDels++}
                    if(isMut) {numOfMuts++}
                }
            }

            var numOfAltered = numOfMuts+numOfDels+numOfAmp+numOfCombo;

            var hist1, hist2;
            if(bundleIndex < numOfStudiesWithMutData) {
                hist1 = histogramData;
                hist2 = histogramData3;
            } else {
                hist1 = histogramData2;
                hist2 = histogramData4;
                bundleIndex = bundleIndex - numOfStudiesWithMutData;
            }

            if(!multipleGenes) {
                hist1.setValue(bundleIndex, 1, formatPercent(numOfCombo/numOfCases));
                hist1.setValue(bundleIndex, 2, formatPercent(numOfMuts/numOfCases));
                hist1.setValue(bundleIndex, 3, formatPercent(numOfDels/numOfCases));
                hist1.setValue(bundleIndex, 4, formatPercent(numOfAmp/numOfCases));
                tmpTotal = hist1.getValue(bundleIndex, 1)
                        + hist1.getValue(bundleIndex, 2)
                        + hist1.getValue(bundleIndex, 3)
                        + hist1.getValue(bundleIndex, 4);
                if(maxAlterationPercent < tmpTotal) {
                    maxAlterationPercent = tmpTotal;
                    maxAlterationPercent = Math.ceil(maxAlterationPercent*10) / 10;
                }

                hist2.setValue(bundleIndex, 1, numOfCombo);
                hist2.setValue(bundleIndex, 2, numOfMuts);
                hist2.setValue(bundleIndex, 3, numOfDels);
                hist2.setValue(bundleIndex, 4, numOfAmp);
                hist2.setValue(bundleIndex, 5, numOfCases-numOfAltered);
            } else {
                hist1.setValue(bundleIndex, 1, formatPercent(numOfAltered/numOfCases));
                if(maxAlterationPercent < hist1.getValue(bundleIndex, 1)) {
                    maxAlterationPercent = hist1.getValue(bundleIndex, 1);
                    maxAlterationPercent = Math.ceil(maxAlterationPercent*10) / 10;
                }
                hist2.setValue(bundleIndex, 1, numOfAltered);
                hist2.setValue(bundleIndex, 2, numOfCases-numOfAltered);
            }

	        if(bundleIndex == 0 || bundleIndex % 2 == 0 || bundleIndex == numOfStudiesWithMutData-1 || bundleIndex == cancerStudies.length-1)
	    	    drawChart();

        }

        function loadStudiesWithIndex(bundleIndex) {
            if(bundleIndex >= cancerStudies.length) {
                $("#crosscancer_summary_loading").fadeOut();
                $("#crosscancer_summary_message").fadeIn();
                lastStudyLoaded = true;
                return;
            }

            var cancerID = cancerStudies[bundleIndex];
            $("#study_" + cancerID)
                .load('cross_cancer_summary.do?gene_list=<%= geneList %>&cancer_study_id='
                    + cancerID + '&<%=QueryBuilder.DATA_PRIORITY + "=" + dataPriority%>',
                        function() {

                            setTimeout(function() {
                                updateHistograms(bundleIndex, cancerID);
                            }, 760);

                            $("#crosscancer_summary_loading_done").html("" + (bundleIndex+1));
                            loadStudiesWithIndex(bundleIndex+1);
                        }
                 );
        }

        function sumSort(dataView, skipLast) {
            var numOfRows = dataView.getNumberOfRows();
            var numOfCols = dataView.getNumberOfColumns();
            if(skipLast) {
                numOfCols--;
            }
            var rowIndex = [];
            for(var i=0; i < numOfRows; i++)
                rowIndex.push(i);

            rowIndex.sort(function(a, b) {
                var sumA = 0;
                var sumB = 0;
                for(var j=1; j < numOfCols; j++) {
                    sumA += dataView.getValue(a, j);
                    sumB += dataView.getValue(b, j);
                }

                return sumB-sumA;
            });

            return rowIndex;
        }

       var sortPermanently = false;
       function drawChart() {
           var formatter = new google.visualization.NumberFormat({ pattern: "#.#%"});
           if(multipleGenes) {
               formatter.format(histogramData, 1);
               formatter.format(histogramData2, 1);
           } else {
               formatter.format(histogramData, 1);
               formatter.format(histogramData, 2);
               formatter.format(histogramData, 3);
               formatter.format(histogramData, 4);
               formatter.format(histogramData2, 1);
               formatter.format(histogramData2, 2);
               formatter.format(histogramData2, 3);
               formatter.format(histogramData2, 4);
           }

           var histogramView = new google.visualization.DataView(histogramData);
           var histogramView2 = new google.visualization.DataView(histogramData2);
           var histogramView3 = new google.visualization.DataView(histogramData3);
           var histogramView4 = new google.visualization.DataView(histogramData4);

           if(sortPermanently) {
               var skipLast = shownHistogram > 2;

               var hv = !skipLast ? histogramView : histogramView3;
               var hv2 = !skipLast ? histogramView2 : histogramView4;

               var sortedIndex = sumSort(hv, skipLast);
               histogramView.setRows(sortedIndex);
               histogramView3.setRows(sortedIndex);
               var sortedIndex2 = sumSort(hv2, skipLast);
               histogramView2.setRows(sortedIndex2);
               histogramView4.setRows(sortedIndex2);
           }

           var options = {
              title: divideHistograms ? 'Percent Sample Alteration for Each Cancer Study with Mutation Data (' + genesQueried + ')' : 'Percent Sample Alteration for Each Cancer Study (' + genesQueried + ')',
              colors: ['#aaaaaa', '#008000', '#002efa', '#ff2617'],
              legend: {
                position: 'bottom'
              },
              hAxis: {
		slantedText: true,
                slantedTextAngle: 45,
		showTextEvery: 1,
                maxTextLines: 2
              },
              vAxis: {
                    title: 'Percent Altered',
                    maxValue: lastStudyLoaded ? maxAlterationPercent : 1,
                    minValue: 0,
                    format: '#.#%'
              },
    	      animation: {
                  duration: 750,
                  easing: 'linear'
    	      },
              isStacked: true
            };

            histogramChart.draw(histogramView, options);
            
	    var options2 = {
              title: 'Percent Sample Alteration for Each Cancer Study without Mutation Data (' + genesQueried + ')',
              colors: ['#aaaaaa', '#008000', '#002efa', '#ff2617'],
              legend: {
                position: 'bottom'
              },
              hAxis: {
		slantedText: true,
		showTextEvery: 1,
                slantedTextAngle: 45,
                maxTextLines: 2
              },
              vAxis: {
	            title: 'Percent Altered',
                maxValue: lastStudyLoaded ? maxAlterationPercent : 1,
                minValue: 0,
                format: '#.#%'
              },
              animation: {
                    duration: 750,
                    easing: 'linear'
      	      },
              isStacked: true

            };

            histogramChart2.draw(histogramView2, options2);

            var options3 = {
              title: divideHistograms ? 'Number of Altered Cases for Each Cancer Study with Mutation data (' + genesQueried + ')' : 'Number of Altered Cases for Each Cancer Study (' + genesQueried + ')',
              colors: multipleGenes ? ['#aaaaaa', '#eeeeee'] : ['#aaaaaa',  '#008000', '#002efa', '#ff2617', '#eeeeee'],
              legend: {
                position: 'bottom'
              },
              animation: {
                duration: 750,
                easing: 'linear'
        	  },
              hAxis: {
		         slantedText: true,
		         showTextEvery: 1,
                 slantedTextAngle: 45,
                 maxTextLines: 2
               },
              yAxis: {
                title: 'Number of cases'
              },
              isStacked: true
            };

            histogramChart3.draw(histogramView3, options3);
            
	    var options4 = {
              title: 'Number of Altered Cases for Each Cancer Study without Mutation Data (' + genesQueried + ')',
              colors: multipleGenes ? ['#aaaaaa', '#eeeeee'] : ['#aaaaaa',  '#008000', '#002efa', '#ff2617', '#eeeeee'],
              legend: {
                position: 'bottom'
              },
              hAxis: {
		slantedText: true,
		showTextEvery: 1,
               slantedTextAngle: 45,
               maxTextLines: 2
              },
              animation: {
                duration: 750,
                easing: 'linear'
          	  },
	          yAxis: {
	      	    title: 'Number of cases'
	          },
              isStacked: true
            };

            histogramChart4.draw(histogramView4, options4);
       }
    });
</script>

<table>
    <tr>
        <td>

            <div id="results_container">

                <!--[if lte IE 8]>
                <div class="ui-state-highlight ui-corner-all" id="cc-ie-message">
                    <p>
                        <span class="ui-icon ui-icon-alert" style="float: left; margin-right: .3em; margin-left: .3em">
                        </span>
                        We have detected that you are using an older web browser that might cause problems in viewing
                        this cross-cancer summary page. For better performance, we suggest upgrading your browser
                        to the latest version or using a different one (e.g. Chrome, Firefox, Safari).
                    </p>
                </div>
                <br/>
                <![endif]-->


                <div class="ui-state-highlight ui-corner-all">
                    <p id="crosscancer_summary_message"><span class="ui-icon ui-icon-info"
                             style="float: left; margin-right: .3em; margin-left: .3em"></span>
                        Results are available for <strong><%= (cancerStudies.size()) %>
                        cancer studies</strong>. Click on a cancer study below to view a summary of
                        results<span id="queried-genes"></span>.
                    </p>
                    <p id="crosscancer_summary_loading">
                        <img src='images/ajax-loader2.gif' style="margin-right: .6em; margin-left: 1.0em">
                        Loading summaries for cancer studies...
                        (<span id="crosscancer_summary_loading_done">0</span>/<%=cancerStudies.size()%> done)
                    </p>
                </div>

                <p><a href=""
                      title="Modify your original query.  Recommended over hitting your browser's back button."
                      id="toggle_query_form">
                    <span class='query-toggle ui-icon ui-icon-triangle-1-e'
                          style='float:left;'></span>
                    <span class='query-toggle ui-icon ui-icon-triangle-1-s'
                          style='float:left; display:none;'></span><b>Modify Query</b></a>

                <p/>

                <div style="margin-left:5px;display:none;" id="query_form_on_results_page">
                    <%@ include file="query_form.jsp" %>
                </div>

                <br/>
                <hr align="left" class="crosscancer-hr"/>
                <h1 class="crosscancer-header">Summary for All Cancer Studies</h1>
                <br/>
                <br/>

                <div id="historam_toggle" style="text-align: right; padding-right: 125px">

                    <select id="hist_toggle_box">
                        <%
                            if(divideHistograms && !onlyMutationData) {
                        %>
                        <option value="1">Show percent of altered cases (studies with mutation data)</option>
                        <option value="2">Show percent of altered cases (studies without mutation data)</option>
                        <option value="3">Show number of altered cases (studies with mutation data)</option>
                        <option value="4">Show number of altered cases (studies without mutation data)</option>
                        <option value="-1" disabled="true">--</option>
                        <option value="5">Show combined mutation diagram(s)</option>
                        <%
                            } else if(onlyMutationData) {
                        %>
                        <option value="1">Show percent of altered cases (studies with mutation data)</option>
                        <option value="3">Show number of altered cases (studies with mutation data)</option>
                        <option value="-1" disabled="true">--</option>
                        <option value="5">Show combined mutation diagram(s)</option>
                        <%
                            } else {
                        %>
                        <option value="1">Show percent of altered cases</option>
                        <option value="3">Show number of altered cases</option>
                        <%
                            }
                        %>
                    </select>
                     |
                     <a href="#" id="histogram_sort" title="Sorts/unsorts histograms by alteration in descending order">Sort</a>
                     |
                     <a href="#" id="download_histogram" title="Downloads the current histogram in SVG format.">Export</a>
                </div>
                <div id="chart_div1" style="width: 975px; height: 450px;"></div>
                <div id="chart_div2" style="width: 975px; height: 450px;"></div>
                <div id="chart_div3" style="width: 975px; height: 450px;"></div>
                <div id="chart_div4" style="width: 975px; height: 450px;"></div>
                <div id="chart_div5" style="width: 900px;">
                    <div id='mutation_details'>

                        <%
                            if (mutationMap.getNumGenesWithExtendedMutations() > 0) {
                                for (String gene: genes) {
                                    MutationCounter mutationCounter = new MutationCounter(gene, mutationMap);

                                    if (mutationMap.getNumExtendedMutations(gene) > 0)
                                    {
                                        outputHeader(out, gene, mutationCounter);
                                    }
                                }
                            } else {
                                outputNoMutationDetails(out);
                            }
                        %>

                    </div>
                </div>
                <br/>
                <br/>

                <form id="histogram_download_form" method="POST" action="histogram_converter.svg">
                    <input type="hidden" name="format" value="svg">
                    <input type="hidden" name="xml" id="histogram_svg_xml" value="">
                </form>

                <hr align="left" class="crosscancer-hr"/>
                <h1 class="crosscancer-header">Details for Each Cancer Study</h1>
                <br/>

                <jsp:include page="global/small_onco_print_legend.jsp" flush="true"/>

                <script>
                    var windowTmp = this;
                    var panelIsActive = {};
                    var panelHasAlreadyBeenDrawn = {};

                    jQuery(document).ready(function() {
                        $('#accordion .head').click(function() {
                            //  This toggles the next element, right after head,
                            //  which is the accordion ajax panel
                            $(this).next().toggle();
                            //  This toggles the ui-icons within head
                            jQuery(".ui-icon", this).toggle();
							// determine if we are opening or closing
							var cancerStudyID = $(this).attr('id');
							panelIsActive[cancerStudyID] = !panelIsActive[cancerStudyID];
							// redraw oncoprint (TBD: only draw on opening)
							if (panelIsActive[cancerStudyID]) {
								if (!panelHasAlreadyBeenDrawn[cancerStudyID]) {
									panelHasAlreadyBeenDrawn[cancerStudyID] = true;
									eval("DrawOncoPrintHeader(ONCOPRINT_" + this.id +
										 ", LONGEST_LABEL_" + this.id + ".get('LONGEST_LABEL_" + this.id +
										 "'), HEADER_VARIABLES_" + this.id + ", false)");
									eval("DrawOncoPrintBody(ONCOPRINT_" + this.id +
										 ", LONGEST_LABEL_" + this.id + ".get('LONGEST_LABEL_" + this.id +
										 "'), GENETIC_ALTERATIONS_SORTED_" + this.id +
										 ".get('GENETIC_ALTERATIONS_SORTED_" + this.id + "'),'"+cancerStudyID+"')");
								}
							}
                            return false;
                        }).next().hide();

                        $(".movable-icon").tipTip();

                        var oq2Id = "oql2";
                        var oqTopLoc = $("#oncoquery-legend").position().top;
                        var oqClone = $("#oncoquery-legend").clone().attr("id", oq2Id);
                        $("#oncoquery-legend").after(oqClone);
                        oqClone.hide();

                        $.fn.showOnScroll = function() {
                            var $this = this,
                                $window = $(windowTmp);

                            $window.scroll(function(e){
                                if ($window.scrollTop() > oqTopLoc) {
                                    $this.fadeIn();
                                    $this.css({
                                        position: 'fixed',
                                        top: 0,
                                        margin: 0,
                                        'padding-top': '15px',
                                        'padding-bottom': '15px',
                                        border: '2px solid #777777'
                                    });
                                } else {
                                    $this.fadeOut();
                                }
                            });
                        };

                        oqClone.showOnScroll();
                    });
                </script>

                <div id="accordion">

                    <% if(divideHistograms) { %>
                    <h2 class="cross_cancer_header">Studies with Mutation Data</h2>
                    <% } %>

                    <div>
                    <% outputCancerStudies(primaryStudies, out); %>
                    <% if( !secondaryStudies.isEmpty() ) {
                    %>
                    </div>
                    <div>

                            <% if(divideHistograms) { %>
                            <h2 class="cross_cancer_header">Studies without Mutation Data</h2>
                            <% }

                            outputCancerStudies(secondaryStudies, out);

                        } else if(onlyMutationData) { // Show a message to the user if only mutation data is wanted
                    %>

                        <br/>
                        <br/>
                        <div class="ui-state-highlight ui-corner-all">
                            <p>
                                <span class="ui-icon ui-icon-alert" style="float: left; margin-right: .3em; margin-left: .3em">
                                </span>
                                Since the data priority was set to 'Only Mutations', <b><%=skippedCancerStudies%> cancer
                                studies</b> that do not have mutation data were excluded from this view.
                            </p>
                        </div>
                        <br/>

                    <%
                        } else {
                    %>
                        <br/>
                        <br/>
                        <div class="ui-state-highlight ui-corner-all">
                            <p>
                                <span class="ui-icon ui-icon-alert" style="float: left; margin-right: .3em; margin-left: .3em">
                                </span>
                                Since the data priority was set to 'Only CNA', <b><%=skippedCancerStudies%> cancer
                                studies</b> that do not have CNA data were excluded from this view.
                            </p>
                        </div>
                        <br/>

                        <%
                        }
                    %>
                    </div>
                </div>

            </div>
            <!-- end results container -->
        </td>
    </tr>
</table>
</div>
</td>
</tr>
<tr>
    <td colspan="3">
        <jsp:include page="global/footer.jsp" flush="true"/>
    </td>
</tr>
</table>
</center>
</div>

<script type="text/javascript">

    var mutationsDrawn = false;
    //  Set up Mutation Diagrams
    var reDrawMutDiagrams = function() {
        if(mutationsDrawn) {
            return; // do nothing
        }
        mutationsDrawn = true;

        function addMouseOverCustom(node, metadata, id){
            var countText = "";

            if (metadata.count == 1) {
                countText = "<b>" + metadata.count + " mutation</b>";
            }
            else {
                countText = "<b>" + metadata.count + " mutations</b>";
            }

            var txt = countText + "<br/>Amino Acid Change:  " + metadata.label + " ";

            // add the potential histogram component
            txt += "<br/><br/><div id='cc_hist_" + id + "_" + metadata.label + "'>"
                    + "<img class='cc-histograms-loading' src='images/ajax-loader.gif'/>"
                    + "</div>";

            $(node).qtip({
                content: {
                    text: function(api) {

                        $.getJSON("cross_cancer_mutation_histogram.json",
                                {
                                    gene: id,
                                    mutation: metadata.label,
                                    data_priority: <%=dataPriority%>
                                },
                                function(data) {
                                    var options = {
                                        hAxis: {title: 'Cancer studies'},
                                        yAxis: {title: 'Num. of Mutations'}
                                    };

                                    //var chart = new google.visualization.ColumnChart(
                                    var chart = new google.visualization.PieChart(
                                            document.getElementById("cc_hist_" + id + "_" + metadata.label)
                                    );

                                    var gData = google.visualization.arrayToDataTable(data);
                                    chart.draw(gData, options);
                                }
                        );

                        return txt;
                    }
                },
                hide: { fixed: true, delay: 100 },
                style: { classes: 'ui-tooltip-light ui-tooltip-rounded ui-tooltip-shadow ui-tooltip-lightyellow' },
                position: {my:'bottom center',at:'top center'}
            });
        }

        var geneSymbol;
        var diagramMutations;
        var tableMutations;



<%

if(dataPriority != 2) {
    for (String gene: genes) {
        if (mutationMap.getNumExtendedMutations(gene) > 0) {
%>
        geneSymbol = "<%= gene.toUpperCase() %>";
        diagramMutations = "<%= outputMutationsJson(gene, mutationMap) %>";

        $.ajax({ url: "mutation_diagram_data.json",
            dataType: "json",
            data: {
                hugoGeneSymbol: geneSymbol,
                mutations: diagramMutations
            },
            success: function(sequences) {
                $(".cc-diagrams-loading").hide();
                drawMutationDiagramWithCustomTooltips(sequences, addMouseOverCustom);

            },
            type: "POST"
        });

<%
        }
    }
}
%>

    };
</script>

<jsp:include page="global/xdebug.jsp" flush="true"/>

</body>
</html>

<%!
    private void outputCancerStudies(ArrayList<CancerStudy> cancerStudies,
            JspWriter out) throws IOException {
        for (CancerStudy cancerStudy : cancerStudies) {
            out.println("<div class='accordion_panel'>");
            out.println("<h1 class='head' id=\"" + cancerStudy.getCancerStudyStableId() + "\">");

            //  output triangle icons
            //  the float:left style is required;  otherwise icons appear on their own line.
            out.println("<span class='ui-icon ui-icon-triangle-1-e' style='float:left;'></span>");
            out.println("<span class='ui-icon ui-icon-triangle-1-s'"
                    + " style='float:left;display:none;'></span>");
            out.println(cancerStudy.getName());
            out.println("<span class='percent_altered' id='percent_altered_" + cancerStudy.getCancerStudyStableId()
                    + "' style='float:right'><img  src='images/ajax-loader2.gif'></span>");
            out.println("</h1>");
            out.println("<div class='accordion_ajax' id=\"study_"
                    + cancerStudy.getCancerStudyStableId() + "\">");
            out.println("</div>");
            out.println("</div>");
        }
    }
%>

<%!
    private String outputMutationsJson(String gene, final ExtendedMutationMap mutationMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        StringWriter stringWriter = new StringWriter();
        List<ExtendedMutation> mutations = mutationMap.getExtendedMutations(gene);
        try {
            objectMapper.writeValue(stringWriter, mutations);
        }
        catch (Exception e) {
            // ignore
        }
        return stringWriter.toString().replace("\"", "\\\"");
    }

    private void outputHeader(JspWriter out, String gene, MutationCounter mutationCounter) throws IOException {
        DecimalFormat percentFormat = new DecimalFormat("###,###.#%");
        out.print("<h4>"
                + gene.toUpperCase()
                + "</h4>"
        );
        out.println("<div id='mutation_diagram_" + gene.toUpperCase() + "'></div>");
        out.println("<div id='mutation_histogram_" + gene.toUpperCase() + "'></div>");
        out.println("<div id='mutation_table_" + gene.toUpperCase() + "'>" +
                "<img class='cc-diagrams-loading' src='images/ajax-loader.gif'/>" +
                "</div>");
    }

    private void outputNoMutationDetails(JspWriter out) throws IOException {
        out.println("<p>There are no mutation details available for the gene set entered.</p>");
        out.println("<br><br>");
    }
%>

