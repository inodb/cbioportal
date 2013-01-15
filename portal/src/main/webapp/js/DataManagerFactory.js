// Gideon Dresdner
// dresdnerg@cbio.mskcc.org
// November 2012

var DataManagerFactory = (function() {

    var getNewDataManager = function() {

        var that = {};

        var listeners = [],                     // queue of callback functions
            data = {},                          // data holder
            FIRED = false;                      // have we fired already?

        that.fire = function(d) {
            // kaboom!

            FIRED = true;

            data = d;

            for (var fun = listeners.pop(); fun !== undefined; fun = listeners.pop()) {
                fun(d);
            }

            return listeners;
        };

        that.subscribe = function(fun) {
            // fun takes data as a parameter

            var queue_pos = listeners.push(fun);
            // todo: does fire order matter?

            if (FIRED) {
                fun(data);
            }

            return queue_pos;
        };

        return that;
    };


    var that = {};

    var GENE_DATA_MANAGER = getNewDataManager();

    that.getGeneDataManager = function() {
        return GENE_DATA_MANAGER;
    };

    that.getGeneDataJsonUrl = function() { return 'GeneData.json'; };     // json url

    that.getNewDataManager = function() { return getNewDataManager(); };

    return that;
})();
