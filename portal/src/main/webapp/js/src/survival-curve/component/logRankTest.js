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

var LogRankTest = function() {

    var datum = {
            time: "",    //num of months
            num_of_failure_1: 0,
            num_of_failure_2: 0,
            num_at_risk_1: 0,
            num_at_risk_2: 0,
            expectation: 0, //(n1j / (n1j + n2j)) * (m1j + m2j)
            variance: 0
        },
        mergedArr = [],
        mergedArrLength = 0,
        callBackFunc = "";
    //os: DECEASED-->1, LIVING-->0; dfs: Recurred/Progressed --> 1, Disease Free-->0
    function mergeGrps(inputGrp1, inputGrp2) {
        var _ptr_1 = 0; //index indicator/pointer for group1
        var _ptr_2 = 0; //index indicator/pointer for group2
        
        while(_ptr_1 < inputGrp1.length && _ptr_2 < inputGrp2.length) { //Stop when either pointer reach the end of the array
            if (inputGrp1[_ptr_1].time < inputGrp2[_ptr_2].time) {
                var _datum = jQuery.extend(true, {}, datum);
                _datum.time = inputGrp1[_ptr_1].time;
                if (inputGrp1[_ptr_1].status === "1") {
                    _datum.num_of_failure_1 = 1;
                    _datum.num_at_risk_1 = inputGrp1[_ptr_1].num_at_risk;
                    _datum.num_at_risk_2 = inputGrp2[_ptr_2].num_at_risk;
                    _ptr_1 += 1;
                } else {
                    _ptr_1 += 1;
                    continue;
                }
            } else if (inputGrp1[_ptr_1].time > inputGrp2[_ptr_2].time) {
                var _datum = jQuery.extend(true, {}, datum);
                _datum.time = inputGrp2[_ptr_2].time;
                if (inputGrp2[_ptr_2].status === "1") {
                    _datum.num_of_failure_2 = 1;
                    _datum.num_at_risk_1 = inputGrp1[_ptr_1].num_at_risk;
                    _datum.num_at_risk_2 = inputGrp2[_ptr_2].num_at_risk;
                    _ptr_2 += 1;
                } else {
                    _ptr_2 += 1;
                    continue;
                }
            } else { //events occur at the same time point
                var _datum = jQuery.extend(true, {}, datum);
                _datum.time = inputGrp1[_ptr_1].time;
                if (inputGrp1[_ptr_1].status === "1" || inputGrp2[_ptr_2].status === "1") {
                    if (inputGrp1[_ptr_1].status === "1") {
                        _datum.num_of_failure_1 = 1;
                    }
                    if (inputGrp2[_ptr_2].status === "1") {
                        _datum.num_of_failure_2 = 1;
                    }
                    _datum.num_at_risk_1 = inputGrp1[_ptr_1].num_at_risk;
                    _datum.num_at_risk_2 = inputGrp2[_ptr_2].num_at_risk;
                    _ptr_1 += 1;
                    _ptr_2 += 1;
                } else {
                    _ptr_1 += 1;
                    _ptr_2 += 1;
                    continue;
                }
            }
            mergedArr.push(_datum);
        }
        mergedArrLength = mergedArr.length;
    }

    function calcExpection() {
        for (var i = 0; i < mergedArrLength; i++) {
            var _item = mergedArr[i];
            _item.expectation = (_item.num_at_risk_1 / (_item.num_at_risk_1 + _item.num_at_risk_2)) * (_item.num_of_failure_1 + _item.num_of_failure_2);
        }
    }

    function calcVariance() {
        for (var i = 0; i < mergedArrLength; i++) {
            var _item = mergedArr[i];
            var _num_of_failures = _item.num_of_failure_1 + _item.num_of_failure_2;
            var _num_at_risk = _item.num_at_risk_1 + _item.num_at_risk_2;
            _item.variance = ( _num_of_failures * (_num_at_risk - _num_of_failures) * _item.num_at_risk_1 * _item.num_at_risk_2) / ((_num_at_risk * _num_at_risk) * (_num_at_risk - 1));
        }
    }

    function calcPval(_callBackFunc) {
        var O1 = 0, E1 = 0, V = 0;
        $.each(mergedArr, function(index, obj) {
            O1 += obj.num_of_failure_1;
            E1 += obj.expectation;
            V += obj.variance;            
        });
        var chi_square_score = (O1 - E1) * (O1 - E1) / V;
        $.post( "calcPval.do", { chi_square_score: chi_square_score })
            .done( function(_data) {
                callBackFunc = _callBackFunc;
            callBackFunc(_data);
            });
    }
    
    function calcPvalList(_jobKey, _chi_square_score_list, _callBackFunc) {
         $.post( "calcPval.do", { chi_square_score_list: _chi_square_score_list,
         job_key: _jobKey})
            .done( function(_data) {
                callBackFunc = _callBackFunc;
            callBackFunc(_data);
            });
    }
    
    return {
        calc: function(inputGrp1, inputGrp2, _callBackFunc) {
            mergedArr.length = 0;
            mergeGrps(inputGrp1, inputGrp2);
            calcExpection();
            calcVariance();
            calcPval(calcChi2(), _callBackFunc);
        },
        calcList: function(_jobKey, input, _callBackFunc) {
            var _inputLength = input.length,
                _chi_square_score_list = [];
        
            for(var i = 0; i< _inputLength; i++) {
                var _inputDatumGrp1 = input[i][0],
                    _inputDatumGrp2 = input[i][1];
                
                mergedArr.length = 0;
                mergeGrps(_inputDatumGrp1, _inputDatumGrp2);
                calcExpection();
                calcVariance();
                _chi_square_score_list.push(calcChi2());
            }
            
            calcPvalList(_jobKey, _chi_square_score_list.join(","), _callBackFunc);
        }
    };
};