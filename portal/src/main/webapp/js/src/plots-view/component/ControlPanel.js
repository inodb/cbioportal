/*
 * Copyright (c) 2012 Memorial Sloan-Kettering Cancer Center.
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 * documentation provided hereunder is on an "as is" basis, and
 * Memorial Sloan-Kettering Cancer Center
 * has no obligations to provide maintenance, support,
 * updates, enhancements or modifications.  In no event shall
 * Memorial Sloan-Kettering Cancer Center
 * be liable to any party for direct, indirect, special,
 * incidental or consequential damages, including lost profits, arising
 * out of the use of this software and its documentation, even if
 * Memorial Sloan-Kettering Cancer Center
 * has been advised of the possibility of such damage.  See
 * the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

 /*
  * Generate the control menu on the left side for the plots tab
  *
  * Jun 2014
  * @Author: Yichao S/ Eduardo Velasco
  *
  * @Input:
  * @Ouput: 
  */ 
var PlotsMenu = (function () {
    var tabType = {
            ONE_GENE: "one_gene",
            TWO_GENES: "two_genes",
            CUSTOM: "custom"
        },
        // keys = {
        //     MRNA : "mrna",
        //     COPY_NO : "copy_no",
        //     METHYLATION : "dna_methylation",
        //     RPPA : "rppa",
        //     MRNA_COPY_NO : "mrna_vs_copy_no", 
        //     MRNA_METHYLATION : "mrna_vs_dna_methylation",
        //     RPPA_MRNA : "rppa_vs_mrna"
        // },
        values = {
            MRNA : "mrna",
            COPY_NO : "copy_no",
            METHYLATION : "dna_methylation",
            RPPA : "rppa",
            MRNA_COPY_NO : "mrna_vs_copy_no", 
            MRNA_METHYLATION : "mrna_vs_dna_methylation",
            RPPA_MRNA : "rppa_vs_mrna"        
        }; 
    var oneGene = {
            plot_type : {
                MRNA_COPY_NO : {
                    value : values.MRNA_COPY_NO,
                    text : "mRNA vs. Copy Number"
                },
                MRNA_METHYLATION : {
                    value : values.MRNA_METHYLATION,
                    text : "mRNA vs. DNA Methylation"
                },
                RPPA_MRNA : {
                    value : values.RPPA_MRNA,
                    text : "RPPA Protein Level vs. mRNA"
                }
            },
            data_type : {
                MRNA : {
                    label: "- mRNA -",
                    value: values.MRNA,
                    genetic_profile : []
                },
                COPY_NO : {
                    label: "- Copy Number -",
                    value: values.COPY_NO,
                    genetic_profile : []
                },
                METHYLATION : {
                    label: "- DNA Methylation -",
                    value: values.METHYLATION,
                    genetic_profile : []
                },
                RPPA : {
                    label: "- RPPA Protein Level -",
                    value: values.RPPA,
                    genetic_profile : []
                }                
            },
            status : {
                has_mrna : false,
                has_dna_methylation : false,
                has_rppa : false,
                has_copy_no : false
            }
        },
        twoGenes = {
            plot_type : {
                MRNA : { 
                    value : values.MRNA, 
                    name :  "mRNA Expression" 
                },
                COPY_NO : { 
                    value : values.COPY_NO, 
                    name :  "Copy Number Alteration" 
                },
                METHYLATION : { 
                    value : values.METHYLATION, 
                    name :  "DNA Methylation" 
                },
                RPPA : { 
                    value : values.RPPA, 
                    name :  "RPPA Protein Level" 
                }                
            },
            
            data_type : {  //Only contain genetic profiles that has data available for both genes
                // "mutations" : {
                //     genetic_profile : []
                // },
                MRNA : {
                    genetic_profile : []
                },
                COPY_NO : {
                    genetic_profile : []
                },
                METHYLATION : {
                    genetic_profile : []
                },
                RPPA : {
                    genetic_profile : []
                }                  
            }
        };

    var Util = (function() {

        function mergeList(arrX, arrY) {
            var result = [];
            var _arrY = [];
            $.each(arrY, function(index, val) {
                _arrY.push(val[0]);
            });
            $.each(arrX, function(index, val) {
                if (_arrY.indexOf(val[0]) !== -1) {
                    result.push(arrX[index]);
                }
            });
            return result;
        }

        function appendDropDown(divId, value, text) {
            $(divId).append("<option value='" + value + "'>" + text + "</option>");
        }

        function toggleVisibilityX(elemId) {
            var e = document.getElementById(elemId);
            e.style.display = 'block';
            $("#" + elemId).append("<div id='one_gene_log_scale_x_div'></div>");
        }

        function toggleVisibilityY(elemId) {
            var e = document.getElementById(elemId);
            e.style.display = 'block';
            $("#" + elemId).append("<div id='one_gene_log_scale_y_div'></div>");
        }

        function toggleVisibilityHide(elemId) {
            var e = document.getElementById(elemId);
            e.style.display = 'none';
        }

        function generateList(selectId, options) {
            var select = document.getElementById(selectId);
            options.forEach(function(option){
                var el = document.createElement("option");
                el.textContent = option;
                el.value = option;
                select.appendChild(el);
            });
        }

        return {
            appendDropDown: appendDropDown,
            toggleVisibilityX: toggleVisibilityX,
            toggleVisibilityY: toggleVisibilityY,
            toggleVisibilityHide: toggleVisibilityHide,
            generateList: generateList,
            mergeList: mergeList

        };

    }());

    function buildContent(plotsType, selectedGenes) { 
        if (plotsType === tabType.ONE_GENE) {
            var selectedGene = selectedGenes.geneX;
            oneGene.data_type.MRNA.genetic_profile = Plots.getGeneticProfiles(selectedGene).genetic_profile_mrna;
            oneGene.data_type.COPY_NO.genetic_profile = Plots.getGeneticProfiles(selectedGene).genetic_profile_copy_no;
            oneGene.data_type.METHYLATION.genetic_profile = Plots.getGeneticProfiles(selectedGene).genetic_profile_dna_methylation;
            oneGene.data_type.RPPA.genetic_profile = Plots.getGeneticProfiles(selectedGene).genetic_profile_rppa;
            oneGene.status.has_mrna = (oneGene.data_type.MRNA.genetic_profile.length !== 0);
            oneGene.status.has_copy_no = (oneGene.data_type.COPY_NO.genetic_profile.length !== 0);
            oneGene.status.has_dna_methylation = (oneGene.data_type.METHYLATION.genetic_profile.length !== 0);
            oneGene.status.has_rppa = (oneGene.data_type.RPPA.genetic_profile.length !== 0);
        } else if (plotsType === tabType.TWO_GENES) {
            var geneX = selectedGenes.geneX, 
                geneY = selectedGenes.geneY;
            //content.genetic_profile_mutations = Plots.getGeneticProfiles(geneX).genetic_profile_mutations;
            twoGenes.data_type.MRNA = Util.mergeList(
                Plots.getGeneticProfiles(geneX).genetic_profile_mrna,
                Plots.getGeneticProfiles(geneY).genetic_profile_mrna
            );
            twoGenes.data_type.COPY_NO = Util.mergeList(
                Plots.getGeneticProfiles(geneX).genetic_profile_copy_no,
                Plots.getGeneticProfiles(geneY).genetic_profile_copy_no
            );
            twoGenes.data_type.METHYLATION = Util.mergeList(
                Plots.getGeneticProfiles(geneX).genetic_profile_dna_methylation,
                Plots.getGeneticProfiles(geneY).genetic_profile_dna_methylation
            );
            twoGenes.data_type.RPPA = Util.mergeList(
                Plots.getGeneticProfiles(geneX).genetic_profile_rppa,
                Plots.getGeneticProfiles(geneY).genetic_profile_rppa
            );
        } else {
            ///TODO: custom 
        }
    }

    function drawMenu() {

        $("#one_gene_type_specification").show();
        $("#plots_type").empty();
        $("#one_gene_platform_select_div").empty();
        //Plots Type field
        if (status.has_mrna && status.has_copy_no) {
            Util.appendDropDown(
                '#plots_type',
                content.one_gene_tab_plots_type.mrna_copyNo.value,
                content.one_gene_tab_plots_type.mrna_copyNo.text
            );
        }
        if (status.has_mrna && status.has_dna_methylation) {
            Util.appendDropDown(
                '#plots_type',
                content.one_gene_tab_plots_type.mrna_methylation.value,
                content.one_gene_tab_plots_type.mrna_methylation.text
            );
        }
        if (status.has_mrna && status.has_rppa) {
            Util.appendDropDown(
                '#plots_type',
                content.one_gene_tab_plots_type.rppa_mrna.value,
                content.one_gene_tab_plots_type.rppa_mrna.text
            );
        }
        //Data Type Field : profile
        for (var key in content.data_type) {
            var singleDataTypeObj = content.data_type[key];
            $("#one_gene_platform_select_div").append(
                "<div id='" + singleDataTypeObj.value + "_dropdown' style='padding:5px;'>" +
                    "<label for='" + singleDataTypeObj.value + "'>" + singleDataTypeObj.label + "</label><br>" +
                    "<select id='" + singleDataTypeObj.value + "' onchange='PlotsView.init();PlotsMenu.updateLogScaleOption();' class='plots-select'></select></div>"
            );
            for (var index in singleDataTypeObj.genetic_profile) { //genetic_profile is ARRAY!
                var item_profile = singleDataTypeObj.genetic_profile[index];
                $("#" + singleDataTypeObj.value).append(
                    "<option value='" + item_profile[0] + "|" + item_profile[2] + "'>" + item_profile[1] + "</option>");
            }
        }
    }

    function drawErrMsgs() {
        $("#one_gene_type_specification").hide();
        $("#menu_err_msg").append("<h5>Profile data missing for generating this view.</h5>");
    }

    function setDefaultCopyNoSelection() {
        //-----Priority: discretized(gistic, rae), continuous
        //TODO: refactor
        $('#data_type_copy_no > option').each(function() {
            if (this.text.toLowerCase().indexOf("(rae)") !== -1) {
                $(this).prop('selected', true);
                return false;
            }
        });
        $("#data_type_copy_no > option").each(function() {
            if (this.text.toLowerCase().indexOf("gistic") !== -1) {
                $(this).prop('selected', true);
                return false;
            }
        });
        var userSelectedCopyNoProfile = "";
        $.each(geneticProfiles.split(/\s+/), function(index, value){
            if (value.indexOf("cna") !== -1 || value.indexOf("log2") !== -1 ||
                value.indexOf("CNA")!== -1 || value.indexOf("gistic") !== -1) {
                userSelectedCopyNoProfile = value;
                return false;
            }
        });
        $("#data_type_copy_no > option").each(function() {
            if (this.value === userSelectedCopyNoProfile){
                $(this).prop('selected', true);
                return false;
            }
        });
    }

    function setDefaultMrnaSelection() {
        var userSelectedMrnaProfile = "";  //from main query
        //geneticProfiles --> global variable, passing user selected profile IDs
        $.each(geneticProfiles.split(/\s+/), function(index, value){
            if (value.indexOf("mrna") !== -1) {
                userSelectedMrnaProfile = value;
                return false;
            }
        });

        //----Priority List: User selection, RNA Seq V2, RNA Seq, Z-scores
        $("#data_type_mrna > option").each(function() {
            if (this.text.toLowerCase().indexOf("z-scores") !== -1){
                $(this).prop('selected', true);
                return false;
            }
        });
        $("#data_type_mrna > option").each(function() {
            if (this.text.toLowerCase().indexOf("rna seq") !== -1 &&
                this.text.toLowerCase().indexOf("z-scores") === -1){
                $(this).prop('selected', true);
                return false;
            }
        });
        $("#data_type_mrna > option").each(function() {
            if (this.text.toLowerCase().indexOf("rna seq v2") !== -1 &&
                this.text.toLowerCase().indexOf("z-scores") === -1){
                $(this).prop('selected', true);
                return false;
            }
        });
        $("#data_type_mrna > option").each(function() {
            if (this.value === userSelectedMrnaProfile){
                $(this).prop('selected', true);
                return false;
            }
        });
    }

    function setDefaultMethylationSelection() {
        $('#data_type_dna_methylation > option').each(function() {
            if (this.text.toLowerCase().indexOf("hm450") !== -1) {
                $(this).prop('selected', true);
                return false;
            }
        });
    }

    function updateVisibility() {
        $("#one_gene_log_scale_x_div").remove();
        $("#one_gene_log_scale_y_div").remove();
        var currentPlotsType = $('#plots_type').val();
        if (currentPlotsType.indexOf("copy_no") !== -1) {
            Util.toggleVisibilityX("data_type_copy_no_dropdown");
            Util.toggleVisibilityY("data_type_mrna_dropdown");
            Util.toggleVisibilityHide("data_type_dna_methylation_dropdown");
            Util.toggleVisibilityHide("data_type_rppa_dropdown");
        } else if (currentPlotsType.indexOf("dna_methylation") !== -1) {
            Util.toggleVisibilityX("data_type_dna_methylation_dropdown");
            Util.toggleVisibilityY("data_type_mrna_dropdown");
            Util.toggleVisibilityHide("data_type_copy_no_dropdown");
            Util.toggleVisibilityHide("data_type_rppa_dropdown");
        } else if (currentPlotsType.indexOf("rppa") !== -1) {
            Util.toggleVisibilityX("data_type_mrna_dropdown");
            Util.toggleVisibilityY("data_type_rppa_dropdown");
            Util.toggleVisibilityHide("data_type_copy_no_dropdown");
            Util.toggleVisibilityHide("data_type_dna_methylation_dropdown");
        }
        updateLogScaleOption();
    }

    function updateLogScaleOption() {
        $("#one_gene_log_scale_x_div").empty();
        $("#one_gene_log_scale_y_div").empty();
        var _str_x = "<input type='checkbox' id='log_scale_option_x' checked onchange='PlotsView.applyLogScaleX();'/> log scale";
        var _str_y = "<input type='checkbox' id='log_scale_option_y' checked onchange='PlotsView.applyLogScaleY();'/> log scale";
        if ($("#plots_type").val() === content.one_gene_tab_plots_type.mrna_copyNo.value) {
            if ($("#data_type_mrna option:selected").val().toUpperCase().indexOf(("rna_seq").toUpperCase()) !== -1 &&
                $("#data_type_mrna option:selected").val().toUpperCase().indexOf(("zscores").toUpperCase()) === -1) {
                $("#one_gene_log_scale_y_div").append(_str_y);
            }
        } else if ($("#plots_type").val() === content.one_gene_tab_plots_type.mrna_methylation.value) {
            if ($("#data_type_mrna option:selected").val().toUpperCase().indexOf(("rna_seq").toUpperCase()) !== -1 &&
                $("#data_type_mrna option:selected").val().toUpperCase().indexOf(("zscores").toUpperCase()) === -1) {
                $("#one_gene_log_scale_y_div").append(_str_y);
            }
        } else if ($("#plots_type").val() === content.one_gene_tab_plots_type.rppa_mrna.value) {
            if ($("#data_type_mrna option:selected").val().toUpperCase().indexOf(("rna_seq").toUpperCase()) !== -1 &&
                $("#data_type_mrna option:selected").val().toUpperCase().indexOf(("zscores").toUpperCase()) === -1) {
                $("#one_gene_log_scale_x_div").append(_str_x);
            }
        }
    }



    return {
        init: function () {
            //$("#menu_err_msg").empty();
            //fetchFrameContent(gene_list[0]);
            buildContent(
                tabType.ONE_GENE, 
                {   
                    geneX: gene_list[0], 
                    geneY: gene_list[0]
                }
            );
            buildContent(
                tabType.TWO_GENES, 
                {   
                    geneX: gene_list[0], 
                    geneY: gene_list[1] //TODO: DOM doesn't work
                }
            );
            Util.generateList("one_gene", gene_list);
            // if (oneGene.status.has_mrna && 
            //    (oneGene.status.has_copy_no || 
            //     oneGene.status.has_dna_methylation || 
            //     oneGene.status.has_rppa)) {
            //         drawMenu();
            //         setDefaultMrnaSelection();
            //         setDefaultCopyNoSelection();
            //         setDefaultMethylationSelection();
            //         updateVisibility();
            // } else {
            //     drawErrMsgs();
            // }
        },
        updateMenu: function() {
            $("#menu_err_msg").empty();
            fetchFrameContent(document.getElementById("gene").value);
            if(status.has_mrna && (status.has_copy_no || status.has_dna_methylation || status.has_rppa)) {
                drawMenu();
                setDefaultMrnaSelection();
                setDefaultCopyNoSelection();
                setDefaultMethylationSelection();
                updateVisibility();
            } else {
                drawErrMsgs();
            }
        },
        updateDataType: function() {
            setDefaultMrnaSelection();
            setDefaultCopyNoSelection();
            setDefaultMethylationSelection();
            updateVisibility();
        },
        updateLogScaleOption: updateLogScaleOption,
        getStatus: function() {
            return status;
        }
    };
}()); //Closing PlotsMenu