

var StudyViewSummaryTabController = (function() {
    var initComponents = function (_data) {
        StudyViewInitCharts.init(_data);
        StudyViewInitTopComponents.init();
        StudyViewWindowEvents.init();
        $('#dc-plots-loading-wait').hide();
        $('#study-view-main').show();
    };
    
    return {
        init: function(_data) {
            initComponents(_data);
        }
    };

})();