
var LoadingJS = (function(){
    //Tmp include public libraries in here, will change JSarray to empty array
    //before merge study view to default branch
    var JSPublic = [
                    'util/StudyViewBoilerplate',
                    'js/src/survival-curve/survivalCurveProxy.js',
                    'js/src/survival-curve/component/survivalCurve.js',
                    'js/src/survival-curve/component/confidenceIntervals.js',
                    'js/src/survival-curve/component/kmEstimator.js',
                    'js/src/survival-curve/component/logRankTest.js',
                    'js/src/survival-curve/component/boilerPlate.js'];
    
    //As input for RequireJS
    var JSarray = [];
    
    //Callback Function which is used to run after loadding all JS files
    var callbackFunc = "";
    
    //Put all self created js files into array
    function constructJSarray() {
        var _key;
        
        var _folder = {
                component: [
                    'ScatterPlots',
                    'PieChart', 
                    'BarChart', 
                    'DataTable',
                    'AddCharts'
                ],
                data: ['StudyViewProxy'],
                util: [
                    'FnGetColumnData',
                    'FnColumnFilter',
                    'FnSetFilteringDelay',
                    'StudyViewUtil',
                    'StudyViewPrototypes'
                ],
                view: [
                    'StudyViewInitCharts', 
                    'StudyViewInitTopComponents',
                    'StudyViewInitScatterPlot',
                    'StudyViewInitIntroJS',
                    'StudyViewInitWordCloud',
                    'StudyViewWindowEvents',
                    'StudyViewInitMutationsTab',
                    'StudyViewInitCNATab',
                    'StudyViewInitClinicalTab',
                    'StudyViewSurvivalPlotView'
                ],
                controller: [
                    'StudyViewMainController',
                    'StudyViewSummaryTabController',
                    'StudyViewMutationsTabController',
                    'StudyViewCNATabController',
                    'StudyViewClinicalTabController',
                    'StudyViewParams'
                ]
            };
            
        for(_key in _folder){
            var _currentLength = _folder[_key].length;
            
            for(var j = 0; j < _currentLength; j++){
                var _file = _key + "/" + _folder[_key][j];
                JSarray.push(_file);
            }
        }    
    }
    
    function main(){
        constructJSarray();

        //After loding JS files, run Study View Controller
        require(JSPublic,function(){
             require(JSarray, function(){
                 if(callbackFunc !== ''){
                     callbackFunc();
                 }else{
                     console.log('%c Error: No Callback Function Initialized.', 
                                    "color:red");
                 }
            });
        });
    }
    
    return {
        init: function(_callbackFunc){
            callbackFunc = _callbackFunc;
            main();
        }
    };
})();