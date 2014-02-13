<script type="text/javascript" src="js/src/co-expression-tab/CoexpPlots.js"></script>
<script type="text/javascript" src="js/src/co-expression-tab/CoexpPlotsProxy.js"></script>
<script type="text/javascript" src="js/src/co-expression-tab/CoexpPlotsView.js"></script>
<script type="text/javascript" src="js/src/co-expression-tab/coExpression.js"></script>
<script type="text/javascript" src="js/lib/portal-plots/PlotsBoilerplate.js"></script>
<script type="text/javascript" src="js/lib/portal-plots/PlotsHeader.js"></script>
<script type="text/javascript" src="js/lib/portal-plots/ScatterPlots.js"></script>

<style>
    #coexp .coexp-table-filter-custom {
        width: 100%;
        float: left;
    }
    #coexp .datatables_filter {
        width: 300px;
        float: left;
        margin-left: 0px;
        text-align: left;
        font-size: 11px;
        padding-left: 6px;
    }

    #coexp .dataTables_info {
        float: left;
        width: auto;
    }
    #coexp .coexp-tabs-ref {
        font-size: 11px !important;
    }
    #coexp .coexp-table {
        width: 100%;
    }
    #coexp .coexp-plots {
        float: left;
    }
    #coexp p {
        font-size: 12px;
        display: block;
        text-align: left;
        font-family: Verdana,Arial,sans-serif;
        margin-bottom: 12px;
    }
    .ui-state-disabled {
        display: none;
    }  

</style>

<div class="section" id="coexp">
    <p style='margin-top: -25px;'>
        This tableb lists the genes with the highest expression correlation with the query genes. Click on a row to see the corresponding correlation plot. 
        <img src='images/help.png' id='coexp-help'>
    </p>

    <div id="coexp-tabs" class="coexp-tabs">
        <ul id='coexp-tabs-list'></ul>
        <div id='coexp-tabs-content'>
        </div>
    </div>
</div>

<script>
    $(document).ready( function() {
        var coexp_tab_init = false;
        $("#tabs").bind("tabsactivate", function(event, ui) {
            if (ui.newTab.text().trim().toLowerCase() === "co-expression") {
                if (coexp_tab_init === false) {
                    CoExpView.initTabs();
                    CoExpView.initView();
                    coexp_tab_init = true;
                } else {
                    $(window).trigger("resize");
                }
            }
        });
    });
    $("#coexp-help").qtip({
        content: { text:'Pearson correlations are computed first.  For genes with an absolute correlation greater than 0.3, the Spearman correlations are also computed. By default, only gene pairs with values > 0.3 (or < -0.3) in both measures are shown.'},
        style: { classes: 'ui-tooltip-light ui-tooltip-rounded ui-tooltip-shadow ui-tooltip-lightyellow' },
        show: {event: "mouseover"},
        hide: {fixed:true, delay: 100, event: "mouseout"},
        position: {my:'left bottom',at:'top right'}
    })
</script>
