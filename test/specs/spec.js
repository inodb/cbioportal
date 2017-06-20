var assert = require('assert');
describe('homepage', function() {


    it('it should have 31 studies in list', function () {

        browser.url('http://www.cbioportal.org/beta');

        var studies = $(".styles-module__cancerStudyListContainer__12ru0 > ul > ul");
        
        studies.waitForExist(5000); // same as `browser.waitForExist('.notification', 5000)`
        
        assert.equal(browser.elements('.styles-module__cancerStudyListContainer__12ru0 > ul > ul').value.length, 31);
        
    });


    it('should filter study list according to filter text input', function () {
        
        assert.equal(browser.elements('.styles-module__cancerStudyListContainer__12ru0 > ul > ul').value.length, 31);
        
        var input = $(".autosuggest input[type=text]");

        input.waitForExist(5000); 

        input.setValue('tract');
        
        browser.pause(500);
        
        assert.equal(browser.elements('.styles-module__cancerStudyListContainer__12ru0 > ul > ul').value.length, 2);
        
    });
    
    it('when a single study is selected, a case set selector is provided', function(){
        
        var caseSetSelectorClass = '.styles-module__CaseSetSelector__1pYBm';
        
        var checkBox = $('.styles-module__Study__398QL');
        
        checkBox.waitForExist(5000);
        
        assert.equal(browser.isExisting(caseSetSelectorClass), false);
        
        browser.click('.styles-module__Study__398QL');

        var caseSetSelector = $(caseSetSelectorClass);
        caseSetSelector.waitForExist(5000);
        
        assert.equal(browser.isExisting(caseSetSelectorClass), true);
        
    });
    
});

describe('patient page', function(){

    it('oncokb indicators show up and hovering produces oncocard', function(){

        browser.url('http://www.cbioportal.org/beta/case.do#/patient?studyId=ucec_tcga_pub&caseId=TCGA-BK-A0CC');

        var oncokbIndicator = $('.oncogenicIcon-module__oncogenic-icon-image__3ptbU');
        oncokbIndicator.waitForExist(5000);

        browser.moveToObject('.oncogenicIcon-module__oncogenic-icon-image__3ptbU',5,5);

        var oncokbCard = $('.oncokb-card');

        oncokbCard.waitForExist(5000);
        
        assert.equal(browser.getText('.tip-header'), 'PPP2R1A S256F in Uterine Serous Carcinoma/Uterine Papillary Serous Carcinoma');

    });

});
