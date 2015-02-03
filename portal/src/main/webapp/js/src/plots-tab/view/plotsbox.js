var plotsbox = (function() {
    
    var render = function(data) {
        if (genetic_vs_genetic()) {
            if(isSameGene()) {
                if(is_profile_discretized("x") && !is_profile_discretized("y")) { //copy number profile is gistic
                    scatterPlots.init(ids.main_view.div, data, true, "x");
                } else if (!is_profile_discretized("x") && is_profile_discretized("y")) {
                    scatterPlots.init(ids.main_view.div, data, true, "y");
                } else if (!is_profile_discretized("x") && !is_profile_discretized("y")) { 
                    scatterPlots.init(ids.main_view.div, data, false, "");
                } else if (is_profile_discretized("x") && is_profile_discretized("y")) {
                    heat_map.init(ids.main_view.div, data);
                }
            } else if (isTwoGenes()) {
                if(is_profile_discretized("x") && !is_profile_discretized("y")) { //copy number profile is gistic
                    scatterPlots.init(ids.main_view.div, data, true, "x");
                } else if (!is_profile_discretized("x") && is_profile_discretized("y")) {
                    scatterPlots.init(ids.main_view.div, data, true, "y");
                } else if (!is_profile_discretized("x") && !is_profile_discretized("y")) { 
                    scatterPlots.init(ids.main_view.div, data, false, "");
                } else if (is_profile_discretized("x") && is_profile_discretized("y")) {
                    heat_map.init(ids.main_view.div, data);
                }
            }
        } else if (genetic_vs_clinical()) {
            var _clin_axis = ($("#" + ids.sidebar.x.data_type).val() === vals.data_type.clin)? "x": "y";
            var _genetic_axis = ($("#" + ids.sidebar.x.data_type).val() === vals.data_type.clin)? "y": "x";
            if (clinical_attr_is_discretized(_clin_axis) && is_profile_discretized(_genetic_axis)) {
                heat_map.init(ids.main_view.div, data);
            } else if (clinical_attr_is_discretized(_clin_axis) && !is_profile_discretized(_genetic_axis)) {
                scatterPlots.init(ids.main_view.div, data, true, _clin_axis);
            } else if (!clinical_attr_is_discretized(_clin_axis) && is_profile_discretized(_genetic_axis)) {
                scatterPlots.init(ids.main_view.div, data, true, _genetic_axis);
            } else if (!clinical_attr_is_discretized(_clin_axis) && !is_profile_discretized(_genetic_axis)) {
                scatterPlots.init(ids.main_view.div, data, false, "");
            }
        } else if (clinical_vs_clinical()) {
            if (clinical_attr_is_discretized("x") && clinical_attr_is_discretized("y")) {
                heat_map.init(ids.main_view.div, data);
            } else if (clinical_attr_is_discretized("x") && !clinical_attr_is_discretized("y")) {
                scatterPlots.init(ids.main_view.div, data, true, "x");
            } else if (!clinical_attr_is_discretized("x") && clinical_attr_is_discretized("y")) {
                scatterPlots.init(ids.main_view.div, data, true, "y");
            } else if (!clinical_attr_is_discretized("x") && !clinical_attr_is_discretized("y")) {
                scatterPlots.init(ids.main_view.div, data, false, "");
            }
        }
    };
    
    return {
        init: function() {
            plotsData.get(render);
        }        
    };
    
}());