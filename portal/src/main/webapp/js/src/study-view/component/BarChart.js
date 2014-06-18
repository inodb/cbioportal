/*
 * Basic DC BarChart Component.
 * 
 * @param _param -- Input object
 *                  chartDivClass: currently only accept class name for DIV.chartDiv,
 *                                  (TODO: Add more specific parameters later) 
 *                  chartID: the current bar chart ID which is treated as
 *                           identifier using in global,
 *                  attrId: the attribute name, 
 *                  displayName: the display content of this attribute, 
 *                  transitionDuration: this will be used for initializing
 *                                      DC Bar Chart,
 *                  ndx: crossfilter dimension, used by initializing DC Bar Chart
 *                  chartColors: color schema
 *                  
 * @interface: getChart -- return DC Bar Chart Object.
 * @interface: getCluster -- return the cluster of DC Bar Chart.
 * @interface: updateParam -- pass _param to update current globel parameters,
 *                            this _param should only pass exist keys. 
 * @interface: reDrawChart -- refresh bar chart by redrawing the DC.js Bar
 *                            chart, keep other information.
 * @interface: postFilterCallbackFunc -- pass a function to be called after DC
 *                                       Bar Chart filtered.
 *                                       
 * @authur: Hongxin Zhang
 * @date: Mar. 2014
 * 
 */
        
var BarChart = function(){
    var barChart, cluster;
    
    //All DIV ID names are organized based on the structure rule, initialized 
    //in initParam function
    var DIV = {
        parentID : "",
        mainDiv : "",
        chartDiv : ""
    };
    
    var param = {
        chartID: "",
        className: "",
        selectedAttr: "",
        selectedAttrDisplay: "",
        transitionDuration: "",
        ndx: "",
        needLogScale: false,
        plotDataButtonFlag: false,
        distanceArray: {}
    };
        
        
    var color = [],
        barColor = {},
        seperateDistance,
        startPoint,
        distanceMinMax,
        emptyValueMapping,
        xDomain = [],
        numOfGroups = 10,
        divider = 1,
        chartWidth = 370,
        chartHeight = 125,
        hasEmptyValue = false;
            
    var postFilterCallback,
        postRedrawCallback,
        plotDataCallback;

    //This function is designed to add functions like click, on, or other
    //other functions added after initializing this Bar Chart.
    function addFunctions() {
        barChart.on("filtered", function(chart,filter){
            dc.events.trigger(function() {
                var _currentFilters = barChart.filters();

                if(_currentFilters.length === 0){
                    $("#"+DIV.chartDiv+"-reload-icon")
                                .css('display','none');
                    $("#" + DIV.mainDiv)
                            .css({'border-width':'1px', 'border-style':'solid'});
                }else{
                    $("#"+DIV.chartDiv+"-reload-icon")
                                .css('display','block');
                    $("#" + DIV.mainDiv)
                            .css({'border-width':'2px', 'border-style':'inset'});
                }
                removeMarker();
                postFilterCallback();
            }, 400);
        });
        barChart.on("postRedraw",function(chart){
            postRedrawCallback();
        });
    }
    
    //Add all listener events
    function addEvents() {
        $('#' + DIV.chartDiv + '-download-icon').qtip('destroy', true);
        $('#'+  DIV.chartDiv+'-plot-data').qtip('destroy', true);
        $('#' + DIV.chartDiv + '-download-icon-wrapper').qtip('destroy', true);
        
        //Add qtip for download icon when mouse over
        $('#' + DIV.chartDiv + '-download-icon-wrapper').qtip({
            style: { classes: 'qtip-light qtip-rounded qtip-shadow qtip-lightyellow'  },
            show: {event: "mouseover", delay: 0},
            hide: {fixed:true, delay: 100, event: "mouseout"},
            position: {my:'bottom left',at:'top right', viewport: $(window)},
            content: {
                text:   "Download"
            }
        });
        
        //Add qtip for survival icon
        $('#'+  DIV.chartDiv+'-plot-data').qtip({
            style:  { classes: 'qtip-light qtip-rounded qtip-shadow qtip-lightyellow'  },
            show:   {event: "mouseover"},
            hide:   {fixed:true, delay: 0, event: "mouseout"},
            position:   {my:'bottom left',at:'top right', viewport: $(window)},
            content:    "Survival analysis"
        });
        
        //Add qtip for download icon when mouse click
        $('#' + DIV.chartDiv + '-download-icon').qtip({
            style: { classes: 'qtip-light qtip-rounded qtip-shadow qtip-lightyellow'  },
            show: {event: "click", delay: 0},
            hide: {fixed:true, delay: 100, event: "mouseout "},
            position: {my:'top center',at:'bottom center', viewport: $(window)},
            content: {
                text:   "<form style='display:inline-block;float:left;margin: 0 2px' action='svgtopdf.do' method='post' id='"+DIV.chartDiv+"-pdf'>"+
                        "<input type='hidden' name='svgelement' id='"+DIV.chartDiv+"-pdf-value'>"+
                        "<input type='hidden' name='filetype' value='pdf'>"+
                        "<input type='hidden' id='"+DIV.chartDiv+"-pdf-name' name='filename' value='"+StudyViewParams.params.studyId + "_" +param.selectedAttr+".pdf'>"+
                        "<input type='submit' style='font-size:10px;' value='PDF'>"+          
                        "</form>"+
                        "<form style='display:inline-block;float:left;margin: 0 2px' action='svgtopdf.do' method='post' id='"+DIV.chartDiv+"-svg'>"+
                        "<input type='hidden' name='svgelement' id='"+DIV.chartDiv+"-svg-value'>"+
                        "<input type='hidden' name='filetype' value='svg'>"+
                        "<input type='hidden' id='"+DIV.chartDiv+"-svg-name' name='filename' value='"+StudyViewParams.params.studyId + "_" +param.selectedAttr+".svg'>"+
                        "<input type='submit' style='font-size:10px;clear:right;float:right;' value='SVG'></form>"
            },
            events: {
                show: function() {
                    $('#' + DIV.chartDiv + '-download-icon-wrapper').qtip('api').hide();
                },
                render: function(event, api) {
                    $("#"+DIV.chartDiv+"-pdf", api.elements.tooltip).submit(function(){
                        setSVGElementValue(DIV.chartDiv,
                            DIV.chartDiv+"-pdf-value");
                    });
                    $("#"+DIV.chartDiv+"-svg", api.elements.tooltip).submit(function(){
                        setSVGElementValue(DIV.chartDiv,
                            DIV.chartDiv+"-svg-value");
                    });
                }
            }
        });
        
        StudyViewUtil.showHideDivision(   "#"+DIV.mainDiv, 
                            "#"+DIV.chartDiv+"-header", 0);
    
        if(param.plotDataButtonFlag){
            $("#"+DIV.chartDiv+"-plot-data").click(function(){

                var _casesInfo = {},
                    _caseIds = [];

                StudyViewInitCharts.setPlotDataFlag(true);

                if(barChart.hasFilter()){
                    barChart.filterAll();
                    dc.redrawAll();
                }

                _caseIds = getCaseIds();
                
                for(var key in _caseIds){
                    var _caseInfoDatum = {},
                        _range = key.split("~");
                    if(key === 'NA'){
                        _caseInfoDatum.color = barColor['NA'];
                    }else {
                        for( var _key in barColor) {
                            if( (_key < Number(_range[1]) && 
                                    _key > Number(_range[0]))){

                                _caseInfoDatum.color = barColor[_key];
                                break;
                            }
                        }
                    }
                    if(_caseInfoDatum.color !== 'undefined' && 
                            _caseInfoDatum.color !== ''){
                        
                        _caseInfoDatum.caseIds = _caseIds[key];
                        _casesInfo[key] = _caseInfoDatum;
                    }
                }
                changeBarColor();
                plotDataCallback(_casesInfo, [param.selectedAttr, param.selectedAttrDisplay]);

                setTimeout(function(){
                    StudyViewInitCharts.setPlotDataFlag(false);
                }, StudyViewParams.summaryParams.transitionDuration);

            });
        }
    
        $("#"+DIV.chartDiv+"-reload-icon").click(function() {
            barChart.filterAll();
            dc.redrawAll();
        });
    }
    
    function changeBarColor() {
        var _bars = $("#" + DIV.mainDiv + " g.chart-body").find("rect"),
            _barsLength = _bars.length;
        
        for(var i = 0; i < _barsLength; i++) {
            if(i === _bars.length-1 && hasEmptyValue) {
                $(_bars[i]).attr('fill', '#cccccc');
            }else {
                $(_bars[i]).attr('fill', color[i]);
            }
        }
    }
    
    function getCaseIds(){
        var _cases = barChart.dimension().top(Infinity),
            _caseIds = {},
            _casesLength = _cases.length,
            _xDomainLength = xDomain.length,
            _caseIdsLength = 0;
        
        //Last element in xDomain is NA mapping value, so need to minus 2.
        if(hasEmptyValue){
            _caseIdsLength = _xDomainLength-2;
        }else{
            _caseIdsLength = _xDomainLength-1;
        }
        
        for(var i = 0; i< _caseIdsLength; i++) {
            var _key = xDomain[i] + "~" + xDomain[i+1];
            _caseIds[_key] = [];
        }
        
        _caseIds['NA'] = [];
        
        for(var i = 0; i < _casesLength; i++){
            var _value = Number(_cases[i][param.selectedAttr]);
            
            if(!isNaN(_value)){
                _value = Number(_value);
                for(var j = 0; j < _xDomainLength; j++){
                    if(_value < xDomain[j]){
                        var _key = xDomain[j-1] + "~" + xDomain[j];
                        
                        _caseIds[_key].push(_cases[i].CASE_ID);
                        break;
                    }
                }
            }else{
                _caseIds['NA'].push(_cases[i].CASE_ID);
            }
        }
        
        for(var key in _caseIds) {
            if(_caseIds[key].length === 0) {
                delete _caseIds[key];
            }
        }
        
        return _caseIds;
    }
    
    //Bar chart SVG style is controled by CSS file. In order to change 
    //brush and deselected bar, this function is designed to change the svg
    //style, save svg and delete added style.
    function setSVGElementValue(_svgParentDivId,_idNeedToSetValue){
        var _svgElement;
        
        var _svg = $("#" + _svgParentDivId + " svg");
        //Change brush style
        var _brush = _svg.find("g.brush"),
            _brushWidth = Number(_brush.find('rect.extent').attr('width'));
        
        _brush.find('rect.extent')
                .css({
                    'fill-opacity': '0.2',
                    'fill': '#2986e2'
                });
                
        _brush.find('.resize path')
                .css({
                    'fill': '#eee',
                    'stroke': '#666'
                });
                                    
        //Change deselected bar chart
        var _chartBody = _svg.find('.chart-body'),
            _deselectedCharts = _chartBody.find('.bar.deselected'),
            _deselectedChartsLength = _deselectedCharts.length;
    
        for ( var i = 0; i < _deselectedChartsLength; i++) {
            $(_deselectedCharts[i]).css({
                'stroke': '',
                'fill': '#ccc'
            });
        }
         
        //Change axis style
        var _axis = _svg.find('.axis'),
            _axisDomain = _axis.find('.domain'),
            _axisDomainLength = _axisDomain.length,
            _axisTick = _axis.find('.tick.major line'),
            _axisTickLength = _axisTick.length;
        
        for ( var i = 0; i < _axisDomainLength; i++) {
            $(_axisDomain[i]).css({
                'fill': 'white',
                'fill-opacity': '0',
                'stroke': 'black'
            });
        }
        
        for ( var i = 0; i < _axisTickLength; i++) {
            $(_axisTick[i]).css({
                'stroke': 'black'
            });
        }
        
        //Change x/y axis text size
        var _chartText = _svg.find('.axis text'),
            _chartTextLength = _chartText.length;
            
        for ( var i = 0; i < _chartTextLength; i++) {
            $(_chartText[i]).css({
                'font-size': '12px'
            });
        }
        
        _svgElement = _svg.html();
        
        //Remove brush if brush width is 0, svg file will remove brush
        //automatically, but the pdf file will not
        if(_brushWidth === 0){
            _svgElement = parseSVG(_svg.html());
        }
        
        $("#" + _idNeedToSetValue)
                .val("<svg width='370' height='200'>"+
                    "<g><text x='180' y='20' style='font-weight: bold; "+
                    "text-anchor: middle'>"+
                    param.selectedAttrDisplay+"</text></g>"+
                    "<g transform='translate(0, 20)'>"+_svgElement + "</g></svg>");
       
        
        //Remove added styles
        _brush.find('rect.extent')
                .css({
                    'fill-opacity': '',
                    'fill': ''
                });
                
        _brush.find('.resize path')
                .css({
                    'fill': '',
                    'stroke': ''
                });
                
        for ( var i = 0; i < _deselectedChartsLength; i++) {
            $(_deselectedCharts[i]).css({
                'stroke': '',
                'fill': ''
            });
        }
    
        for ( var i = 0; i < _axisDomainLength; i++) {
            $(_axisDomain[i]).css({
                'fill': '',
                'fill-opacity': '',
                'stroke': ''
            });
        }
        
        for ( var i = 0; i < _axisTickLength; i++) {
            $(_axisTick[i]).css({
                'stroke': ''
            });
        }
        
        for ( var i = 0; i < _chartTextLength; i++) {
            $(_chartText[i]).css({
                'font-size': ''
            });
        }
    }
    
    //Parse string to document recognisable SVG elements
    function parseSVG(s) {
        var _string = '';
        var _div= document.createElementNS('http://www.w3.org/1999/xhtml', 'div');
        var _frag= document.createDocumentFragment();
        
        _div.innerHTML= '<svg xmlns="http://www.w3.org/2000/svg">'+s+'</svg>';
        while (_div.firstChild.firstChild){
            var _tmpElem = _div.firstChild
                                .firstChild
                                .getElementsByClassName('brush')[0];
            
            if(typeof _tmpElem !== 'undefined'){
                _tmpElem.parentNode.removeChild(_tmpElem);
            }
            _string += _div.firstChild.firstChild.innerHTML;
            _frag.appendChild(_div.firstChild.firstChild);
        }
        return _string;
    }
    
    //Initialize HTML tags which will be used for current Bar Chart.
    function createDiv() {
        var _logCheckBox = "",
            _plotDataDiv = "";
        
        
        if(param.needLogScale){
            _logCheckBox = "<div style='float:left'>"+
                "<input type='checkbox' value='"+ param.chartID +","+ 
                param.distanceArray.max +","+ 
                param.distanceArray.min + "," + param.selectedAttr+
                "' id='scale-input-"+param.chartID+
                "' class='study-view-bar-x-log' checked='checked'></input>"+
                "<span id='scale-span-"+param.chartID+
                "' style='float:left; font-size:10px; margin-right: 15px;"+
                "margin-top:3px;color: grey'>Log Scale X</span>"+
                "</div>";
       }
        
        if(param.plotDataButtonFlag) {
            _plotDataDiv = "<img id='"+
                                DIV.chartDiv+"-plot-data' class='study-view-survival-icon' src='images/survival_icon.svg'/>";
        }else {
            _plotDataDiv = "";
        }
        
        var contentHTML = "<div id='"+DIV.chartDiv +"-title-wrapper' "+
                "style='height: 18px; width: 100%'><div style='float:right' "+
                "id='"+DIV.chartDiv+"-header'>"+
                "<img id='"+ DIV.chartDiv +"-reload-icon' class='study-view-title-icon hidden hover' src='images/reload-alt.svg'/>"+    
                _logCheckBox +
                _plotDataDiv +
                "<div id='"+ DIV.chartDiv+"-download-icon-wrapper' class='study-view-download-icon'><img id='"+ 
                DIV.chartDiv+"-download-icon' style='float:left' src='images/in.svg'/></div>"+
                "<img class='study-view-drag-icon' src='images/move.svg'/>"+
                "<span chartID="+param.chartID+" class='study-view-dc-chart-delete'>x</span>"+
                "</div></div><div id=\"" + DIV.chartDiv + 
                "\" class='"+ param.className +"'  oValue='" + param.selectedAttr + "," + 
                param.selectedAttrDisplay + ",bar'>"+
                "<div id='"+DIV.chartDiv+"-side' class='study-view-pdf-svg-side bar'>"+
                "</div></div>"+
                "<div style='width:100%; float:center;text-align:center;'>"+
                "<chartTitleH4>" + param.selectedAttrDisplay + "</chartTitleH4></div>";
        
        if($("#" + DIV.mainDiv).length === 0){
            $("#" + DIV.parentID)
                    .append("<div id=\"" + DIV.mainDiv+ 
                    "\" class='study-view-dc-chart study-view-bar-main'>" + 
                    contentHTML + "</div>");
        }
    }
    
    //This function is designed to draw red down triangle above marked bar.
    //  TODO: Add comments for each component.
    function drawMarker(_value) {
        var _x,
            _y,
            _numItemOfX,
            _numOfBar,
            _barInfo = [],
            _xValue = [],
            _xTranslate = [];
        
        var _allBars = $('#' + DIV.chartDiv + " .chart-body").find('rect'),
            _allBarsLength = _allBars.length,
            _allAxisX = $('#' + DIV.chartDiv + " .axis.x").find("g"),
            _allAxisXLength = _allAxisX.length,
            _transformChartBody = trimTransformString($('#' + DIV.chartDiv + " .chart-body").attr("transform")),
            _transformAxiaX = trimTransformString($('#' + DIV.chartDiv + " .axis.x").attr("transform"));
    
        for ( var i = 0; i < _allBarsLength; i++) {
            var _barDatum = {},
                _bar = _allBars[i];
            
            _barDatum.x = Number($(_bar).attr('x')) + Number(_transformChartBody[0]);
            _barDatum.y = Number($(_bar).attr('y')) + Number(_transformChartBody[1]) - 5;
            _barDatum.width = Number($(_bar).attr('width'));
            _barInfo.push(_barDatum);
        }
        
        _numOfBar = _barInfo.length;
        
        for ( var i = 0; i < _allAxisXLength; i++) {
            var _axisX = _allAxisX[i],
                _text = $(_axisX).select('text').text();
            if(param.needLogScale) {
                //axis always start with 1 if log chart generated, so if this
                //empty value happen, the previous value definitly exits.
                if(_text === '') {
                   _text = Math.pow(10, Math.log(_xValue[i-1]) / Math.log(10) + 0.5);
                }
            }
            _xValue[i] = Number(_text);
            _xTranslate[i] = Number(trimTransformString($(_axisX).attr('transform'))[0]) + Number(_transformAxiaX[0]);
        }
        
        _numItemOfX = _xTranslate.length;
        
        if(_value === 'NA'){
            _x = _barInfo[_numOfBar-1].x + _barInfo[_numOfBar-1].width / 2;
            _y= _barInfo[_numOfBar-1].y;
        }else {
            for(var i=0 ; i< _numItemOfX ; i++){
                if(_value < _xValue[i]){
                    for(var j=0 ; j< _numOfBar ; j++){
                        if(_barInfo[j].x < _xTranslate[i] && _barInfo[j].x > _xTranslate[i-1]){
                            _x = (_xTranslate[i] + _xTranslate[i-1])/2;
                            _y= _barInfo[j].y;
                            break;
                        }
                    }
                    break;
                }
            }
        }
        
        d3.select("#" + DIV.chartDiv + " svg").append("path")
            .attr("transform", function(d) { return "translate(" + _x + "," + _y + ")"; })
            .attr("d", d3.svg.symbol().size('25').type('triangle-down'))
            .attr('fill',"red")
            .attr('class','mark');
    }
    
    //Precalculate all parameters which will be used to initialize bar chart in
    //initDCBARChart(), will not be used in initDCLogBarChart()
    function paramCalculation() {
        var _tmpMaxDomain,
            _distanceLength = parseInt(distanceMinMax).toString().length;
        
        xDomain.length = 0;
        numOfGroups = 10;
        divider = 1;
        
        //Set divider based on the number m in 10(m)
        for( var i = 0; i < _distanceLength - 2; i++ )
            divider *= 10;
        if( param.distanceArray.max < 100 && 
                param.distanceArray.max > 50 ){
            divider = 10;
        }else if ( param.distanceArray.max < 100 && 
                param.distanceArray.max > 30 ){
            divider = 5;
        }else if ( param.distanceArray.max < 100 && 
                param.distanceArray.max > 10 ){
            divider = 2;
        }
        
        if(param.distanceArray.max <= 1 && 
                param.distanceArray.max > 0 && 
                param.distanceArray.min >= -1 && 
                param.distanceArray.min < 0){
            
            _tmpMaxDomain = (parseInt(param.distanceArray.max / divider) + 1) * divider;
            seperateDistance = 0.2;
            startPoint = (parseInt(param.distanceArray.min / 0.2)-1) * 0.2;
            emptyValueMapping = _tmpMaxDomain +0.2;
        
        }else if( distanceMinMax >= 1 ){
            
            seperateDistance = (parseInt(distanceMinMax / (numOfGroups * divider)) + 1) * divider;
            _tmpMaxDomain = (parseInt(param.distanceArray.max / seperateDistance) + 1) * seperateDistance;
            startPoint = parseInt(param.distanceArray.min / seperateDistance) * seperateDistance;
            emptyValueMapping = _tmpMaxDomain+seperateDistance;
            
        }else if( distanceMinMax < 1 && param.distanceArray.min >=0 && param.distanceArray.max <= 1){
            
            seperateDistance = 0.1;
            startPoint = 0;
            emptyValueMapping = 1.1;
        
        }else{
            
            seperateDistance = 0.1;
            startPoint = -1;
            emptyValueMapping = _tmpMaxDomain + 0.1;
        
        }
        
        for( var i = 0; i <= numOfGroups; i++ ){
            var _tmpValue = i * seperateDistance + startPoint;
            
            _tmpValue = Number(cbio.util.toPrecision(Number(_tmpValue),3,0.1));
            
            //If the current tmpValue already bigger than maxmium number, the
            //function should decrease the number of bars and also reset the
            //Mappped empty value.
            if(_tmpValue > param.distanceArray.max){
                //if i = 0 and tmpValue bigger than maximum number, that means
                //all data fall into NA category.
                if(i !== 0){
                    xDomain.push(_tmpValue);
                }
                //Reset the empty mapping value 
                if(distanceMinMax > 1000 || distanceMinMax < 1){
                    emptyValueMapping = (i+1)*seperateDistance + startPoint;
                }
                
                //If the distance of Max and Min value is smaller than 1, give
                //a more precise value
                if(distanceMinMax < 1){
                    emptyValueMapping = Number(cbio.util.toPrecision(Number(emptyValueMapping),3,0.1));
                }
                
                break;
            }else{
                xDomain.push(_tmpValue);
            }
        }
    }
    
    //Initialize BarChart in DC.js
    function initDCBarChart() {
        var _xunitsNum,
            _barValue = [];
        
        barChart = dc.barChart("#" + DIV.chartDiv);
        
       
        cluster = param.ndx.dimension(function (d) {
            var returnValue = d[param.selectedAttr];
            if(returnValue === "NA" || returnValue === '' || returnValue === 'NaN'){
                hasEmptyValue = true;
                returnValue = emptyValueMapping;
            }else{
                if(d[param.selectedAttr] >= 0){
                    returnValue =  parseInt( 
                                    (d[param.selectedAttr]-startPoint) / 
                                    seperateDistance) * 
                                    seperateDistance + startPoint + seperateDistance / 2;
                }else{
                    returnValue =  ( parseInt( 
                                        d[param.selectedAttr] / 
                                        seperateDistance ) - 1 ) * 
                                    seperateDistance + seperateDistance / 2;
                }
            }
            
            if(_barValue.indexOf(returnValue) === -1) {
                _barValue.push(Number(returnValue));
            }
            
            return returnValue;
        });
        
        _barValue.sort(function(a, b) {
            if(a < b){
                return -1;
            }else {
                return 1;
            }
        });
        
        var _barLength = _barValue.length;
        
        for( var i = 0; i < _barLength-1; i++) {
            barColor[_barValue[i]] = color[i];
        }
        
        if(hasEmptyValue){
            xDomain.push( Number( 
                                cbio.util.toPrecision( 
                                    Number(emptyValueMapping), 3, 0.1 )
                                )
                        );
            barColor['NA'] = '#cccccc';
        }else {
            barColor[_barValue[_barLength-1]] = color[_barLength-1];
        }
        
        barChart
            .width(chartWidth)
            .height(chartHeight)
            .margins({top: 10, right: 20, bottom: 30, left: 40})
            .dimension(cluster)
            .group(cluster.group())
            .centerBar(true)
            .elasticY(true)
            .elasticX(false)
            .turnOnControls(true)
            .mouseZoomable(false)
            .brushOn(true)
            .transitionDuration(StudyViewParams.summaryParams.transitionDuration)
            .renderHorizontalGridLines(false)
            .renderVerticalGridLines(false);
    
        barChart.x( d3.scale.linear()
                        .domain([ 
                                  xDomain[0] - seperateDistance ,
                                  xDomain[xDomain.length - 1] + seperateDistance
                                ]));
        
        barChart.yAxis().ticks(6);
        barChart.yAxis().tickFormat(d3.format("d"));            
        barChart.xAxis().tickFormat(function(v) {
            if(v === emptyValueMapping){
                return 'NA'; 
            }else{
                return v;
            }
        });
        
        barChart.xAxis().tickValues(xDomain);
        
        //1.3 could be changed. It will decide the size of each bar.
        _xunitsNum = xDomain.length*1.3;
        
        //Set the min
        if(_xunitsNum <= 5){
            barChart.xUnits(function(){return 5;});
        }else{
            barChart.xUnits(function(){return _xunitsNum;});
        }
    }
    
    //Initialize BarChart in DC.js
    function initDCLogBarChart() {
        
        var _xunitsNum,
            _domainLength,
            _maxDomain = 10000,
            _barValue = [];
    
        emptyValueMapping = "1000";//Will be changed later based on maximum value
        xDomain.length =0;
        
        barChart = dc.barChart("#" + DIV.chartDiv);
        
        for(var i=0; ;i+=0.5){
            var _tmpValue = parseInt(Math.pow(10,i));
            
            xDomain.push(_tmpValue);
            if(_tmpValue > param.distanceArray.max){
                
                emptyValueMapping = Math.pow(10,i+0.5);
                xDomain.push(emptyValueMapping);
                _maxDomain = Math.pow(10,i+1);
                break;
            }
        }
        
        _domainLength = xDomain.length;
        
        cluster = param.ndx.dimension(function (d) {
            var i, _returnValue = Number(d[param.selectedAttr]);
            
            if(isNaN(_returnValue)){
                _returnValue = emptyValueMapping;
                hasEmptyValue = true;
            }else{
                        
                _returnValue = Number(_returnValue);
                for(i = 1;i < _domainLength; i++){
                    if( d[param.selectedAttr] < xDomain[i] && 
                        d[param.selectedAttr] >= xDomain[i-1]){
                        
                        _returnValue = parseInt( Math.pow(10, i / 2 - 0.25 ));
                    }
                }
            }
            
            if(_barValue.indexOf(_returnValue) === -1) {
                _barValue.push(Number(_returnValue));
            }
            
            return _returnValue;
        }); 
        
        _barValue.sort(function(a, b) {
            if(a < b){
                return -1;
            }else {
                return 1;
            }
        });
        
        var _barLength = _barValue.length;
        
        for( var i = 0; i < _barLength-1; i++) {
            barColor[_barValue[i]] = color[i];
        }
        
        if(hasEmptyValue){
            barColor['NA'] = '#cccccc';
        }else {
            barColor[_barValue[_barLength-1]] = color[_barLength-1];
        }
        
        barChart
            .width(chartWidth)
            .height(chartHeight)
            .margins({top: 10, right: 20, bottom: 30, left: 40})
            .dimension(cluster)
            .group(cluster.group())
            .centerBar(true)
            .elasticY(true)
            .elasticX(false)
            .turnOnControls(true)
            .mouseZoomable(false)
            .brushOn(true)
            .transitionDuration(StudyViewParams.summaryParams.transitionDuration)
            .renderHorizontalGridLines(false)
            .renderVerticalGridLines(false);
    
        barChart.centerBar(true);
        barChart.x(d3.scale.log().nice().domain([0.7,_maxDomain]));
        barChart.yAxis().ticks(6);
        barChart.yAxis().tickFormat(d3.format("d"));            
        barChart.xAxis().tickFormat(function(v) {
            var _returnValue = v;
            if(v === emptyValueMapping){
                _returnValue = 'NA';
            }else{
                var index = xDomain.indexOf(v);
                if(index % 2 === 0)
                    return v.toString();
                else
                    return '';
            }
            return _returnValue; 
        });            
        
        barChart.xAxis().tickValues(xDomain);
        
        _xunitsNum = xDomain.length*1.3;
        
        if(_xunitsNum <= 5){
            barChart.xUnits(function(){return 5;});
        }else{
            barChart.xUnits(function(){return _xunitsNum;});
        }
    }
    
    //Initial global parameters by using passed object.
    function initParam(_param) {
        var _baseID = _param.baseID;
        
        param.className = _param.chartDivClass,
        param.chartID = _param.chartID;
        param.selectedAttr = _param.attrID;
        param.selectedAttrDisplay = _param.displayName;
        param.ndx = _param.ndx;
        param.needLogScale = _param.needLogScale;
        param.distanceArray = _param.distanceArray;
        param.plotDataButtonFlag = _param.plotDataButtonFlag;
        
        if(typeof _param.chartWidth !== 'undefined'){
            chartWidth = _param.chartWidth;
        }
        
        if(typeof _param.chartHeight !== 'undefined'){
            chartHeight = _param.chartHeight;
        }
        
        distanceMinMax = param.distanceArray.diff;
        color = jQuery.extend(true, [], StudyViewBoilerplate.chartColors);
        
        DIV.mainDiv = _baseID + "-dc-chart-main-" + param.chartID;
        DIV.chartDiv = _baseID + "-dc-chart-" + param.chartID;
        DIV.parentID = _baseID + "-charts";
    }
    
    //Remove drawed Bar Markder.
    function removeMarker() {
        $("#" + DIV.chartDiv).find('svg .mark').remove();
    }
    
    function trimTransformString(_string){
        var _tmpString = _string.split("(");
        
        _tmpString = _tmpString[1].split(")");
        _tmpString = _tmpString[0].split(",");
        
        return _tmpString;
    }
    
    return {
        init: function(_param) {
            initParam(_param);
            createDiv();
            
            //Logged Scale Plot does not need to calculate param. These two
            //kinds of Bar Chart using same param but initilize them individually
            if(param.needLogScale){
                initDCLogBarChart();
            }else{
                paramCalculation();
                initDCBarChart();
            }
            addFunctions();
            addEvents();
        },

        getChart : function() {
            return barChart;
        },
        
        updateParam : function(_param) {
            for(var key in _param){
                param[key] = _param[key];
            }
        },
        
        getCluster: function() {
            return cluster;
        },
        
        reDrawChart: function() {
            barColor = {};
            if(param.needLogScale){
                initDCLogBarChart();
            }else{
                paramCalculation();
                initDCBarChart();
            }
            addFunctions();
        },
        
        postFilterCallbackFunc: function(_callback) {
            postFilterCallback = _callback;
        },
        postRedrawCallbackFunc: function(_callback) {
            postRedrawCallback = _callback;
        },
        plotDataCallbackFunc: function(_callback) {
            plotDataCallback = _callback;
        },
        
        removeMarker: removeMarker,
        drawMarker: drawMarker,
    };
};