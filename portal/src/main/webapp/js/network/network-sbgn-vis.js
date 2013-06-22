/**
 * Constructor for the network (sbgn) visualization class.
 *
 * @param divId     target div id for this visualization.
 * @constructor
 */
function NetworkSbgnVis(divId)
{
	// call the parent constructor
	// NetworkVis.call(this, divId);

    // div id for the network vis html content
    this.divId = divId;

	this.networkTabsSelector = "#" + this.divId + " #network_tabs_sbgn";
	this.filteringTabSelector = "#" + this.divId + " #filtering_tab_sbgn";
	this.genesTabSelector = "#" + this.divId + " #genes_tab_sbgn";
	this.detailsTabSelector = "#" + this.divId + " #element_details_tab_sbgn";

	// node glyph class constants
	this.MACROMOLECULE = "macromolecule";
	this.PROCESS = "process";
	this.COMPARTMENT = "compartment";
	this.COMPLEX = "complex";
	this.NUCLEIC_ACID = "nucleic acid feature";
	this.SIMPLE_CHEMICAL = "simple chemical";
	this.SOURCE_SINK = "source and sink";
	this.HUGOGENES = new Array();
	//global array for 
	this._manuallyFiltered = new Array();

	//////////////////////////////////////////////////
    this.nodeInspectorSelector = this._createNodeInspector(divId);
    this.edgeInspectorSelector = this._createEdgeInspector(divId);
    this.geneLegendSelector = this._createGeneLegend(divId);
    this.drugLegendSelector = this._createDrugLegend(divId);
    this.edgeLegendSelector = this._createEdgeLegend(divId);
    this.settingsDialogSelector = this._createSettingsDialog(divId);

    this.mainMenuSelector = "#" + this.divId + " #network_menu_div";
    this.quickInfoSelector = "#" + this.divId + " #quick_info_div";
    this.geneListAreaSelector = "#" + this.divId + " #gene_list_area";

    // flags
    this._autoLayout = false;
    this._removeDisconnected = false;
    this._nodeLabelsVisible = false;
    this._edgeLabelsVisible = false;
    this._panZoomVisible = false;
    this._linksMerged = false;
    this._profileDataVisible = false;
    this._selectFromTab = false;

    // array of control functions
    this._controlFunctions = null;

    // edge type constants
    this.IN_SAME_COMPONENT = "IN_SAME_COMPONENT";
    this.REACTS_WITH = "REACTS_WITH";
    this.STATE_CHANGE = "STATE_CHANGE";
    this.DRUG_TARGET = "DRUG_TARGET";
    this.OTHER = "OTHER";

    // node type constants
    this.PROTEIN = "Protein";
    this.SMALL_MOLECULE = "SmallMolecule";
    this.DRUG = "Drug";
    this.UNKNOWN = "Unknown";

    // default values for sliders
    this.ALTERATION_PERCENT = 0;

    // class constants for css visualization
    this.CHECKED_CLASS = "checked-menu-item";
    this.MENU_SEPARATOR_CLASS = "separator-menu-item";
    this.FIRST_CLASS = "first-menu-item";
    this.LAST_CLASS = "last-menu-item";
    this.MENU_CLASS = "main-menu-item";
    this.SUB_MENU_CLASS = "sub-menu-item";
    this.HOVERED_CLASS = "hovered-menu-item";
    this.SECTION_SEPARATOR_CLASS = "section-separator";
    this.TOP_ROW_CLASS = "top-row";
    this.BOTTOM_ROW_CLASS = "bottom-row";
    this.INNER_ROW_CLASS = "inner-row";

    // string constants
    this.ID_PLACE_HOLDER = "REPLACE_WITH_ID";
    this.ENTER_KEYCODE = "13";

    // name of the graph layout
    this._graphLayout = {name: "ForceDirected"};
    //var _graphLayout = {name: "ForceDirected", options:{weightAttr: "weight"}};

    // force directed layout options
    this._layoutOptions = null;

    // map of selected elements, used by the filtering functions
    this._selectedElements = null;

    // map of connected nodes, used by filtering functions
    this._connectedNodes = null;

    // array of previously filtered elements
    this._alreadyFiltered = null;

    // array of nodes filtered due to disconnection
    this._filteredByIsolation = null;

    // array of filtered edge sources
    this._sourceVisibility = null;

    // map used to resolve cross-references
    this._linkMap = null;

    // map used to filter genes by weight slider
    this._geneWeightMap = null;

    // threshold value used to filter genes by weight slider
    this._geneWeightThreshold = null;

    // maximum alteration value among the non-seed genes in the network
    this._maxAlterationPercent = 0;

    // CytoscapeWeb.Visualization instance
    this._vis = null;

	this.sliderVal = 0;

	//////////////////////////////////////////////////
	this.visibleNodes = null;
}

/**
 * Initializes all necessary components. This function should be invoked, before
 * calling any other function in this script.
 *
 * @param vis	CytoscapeWeb.Visualization instance associated with this UI
 */
NetworkSbgnVis.prototype.initNetworkUI = function(vis, genomicData, annotationData)
{
	var self = this;
	this._vis = vis;
	this._linkMap = this._xrefArray();


	// init filter arrays
	// delete this later because it os not used anymore
	this._alreadyFiltered = new Array();
	this._filteredByIsolation = new Array();
	// parse and add genomic data to cytoscape nodes
	this.parseGenomicData(genomicData,annotationData);
	//for once only, get all the process sources and updates _sourceVisibility array
	this._sourceVisibility = this._initSourceArray(); 
	var weights = this.initializeWeights();
	this._geneWeightMap = this._geneWeightArray(weights);
	this._geneWeightThreshold = this.ALTERATION_PERCENT;
	this._maxAlterationPercent = this._maxAlterValNonSeed(this._geneWeightMap);

	this._resetFlags();

	this._initControlFunctions();

	/**
	* handlers for selecting nodes
	* for more information see visialization in cytoscape website
	**/
	// first one chooses all nodes with same glyph label
	// and updates the details tab accordingly
	var handleMultiNodeSelect = function(evt) 
	{
		self.multiSelectHugos(evt);
		self.multiUpdateDetailsTab(evt);
	};
	// normal select which just chooses one node
	// the details update will only accept one node
	var handleNodeSelect = function(evt) 
	{
		self.updateGenesTab(evt);
		self.updateDetailsTab(evt);
	};

	// if to choose multiple nodes by glyph labels we need to add and remove listeners
	// on CTRL key down and keyup (now dblClick is also doing this, we might remove this)
	var keyDownSelect = function(evt) 
	{
		if(evt.keyCode == self.CTRL_KEYCODE)
		{
		    self._vis.removeListener("select",
		     "nodes", 
		     handleNodeSelect);

	    	    self._vis.removeListener("deselect",
		     "nodes", 
		     handleNodeSelect);

		    self._vis.addListener("select",
		     "nodes", 
		     handleMultiNodeSelect);
		    self._vis.addListener("deselect",
		     "nodes", 
		     handleMultiNodeSelect);
		}
	
    	};
	var keyUpSelect = function(evt) 
	{
		self._vis.removeListener("select",
		     "nodes", 
		     handleMultiNodeSelect);
		self._vis.removeListener("deselect",
		     "nodes", 
		     handleMultiNodeSelect);

		self._vis.addListener("select",
		     "nodes", 
		     handleNodeSelect);

		self._vis.addListener("deselect",
		     "nodes", 
		     handleNodeSelect);
	};
	// add jquery listeners
	$(' #vis_content').keydown(keyDownSelect);
	$(' #vis_content').keyup(keyUpSelect);
	// dblclick event listener to select multi nodes by glyph label
	self._vis.addListener("dblclick",
	     "nodes", 
	     handleMultiNodeSelect);

	// because here the update source is in a different div 
	// than the SIF we have to change the jquery listener
	// to (this.filteringTabSelector)
	var updateSource = function() 
	{
		self.updateSource();
	};
	$(this.filteringTabSelector + " #update_source").click(updateSource);

	//$(' #vis_content').dblclick(dblClickSelect);
	this._initLayoutOptions();

	// initializing the tabs and UIs
	this._initMainMenu();

	this._initDialogs();
	this._initPropsUI();
	this._initSliders();
	//this._initTooltipStyle();

	// add listener for the main tabs to hide dialogs when user selects
	// a tab other than the Network tab

	var hideDialogs = function(evt, ui){
		self.hideDialogs(evt, ui);
	};

	$("#tabs").bind("tabsshow", hideDialogs);

	// this is required to prevent hideDialogs function to be invoked
	// when clicked on a network tab
	$(this.networkTabsSelector).bind("tabsshow", false);

	// init tabs
	$(this.networkTabsSelector).tabs();
	$(this.networkTabsSelector + " .network-tab-ref").tipTip(
		{defaultPosition: "top", delay:"100", edgeOffset: 10, maxWidth: 200});

	this._initGenesTab();
	this._refreshGenesTab();
	    
	// add node source filtering checkboxes
	for (var key in this._sourceVisibility)
	{
		$(this.filteringTabSelector + " #source_filter").append(
		    '<tr class="' + key + '">' +
		    '<td class="source-checkbox">' +
		    '<input id="' + key + '_check" type="checkbox" checked="checked">' +
		    '<label>' + key + '</label>' +
		    '</td></tr>');
	}

	// adjust things for IE
	this._adjustIE();

	// make UI visible
	this._setVisibility(true);


};



//update constructor
//NetworkSbgnVis.prototype.constructor = NetworkSbgnVis;

//TODO override necessary methods (filters, inspectors, initializers, etc.) to have a proper UI.

//Genomic data parser method
NetworkSbgnVis.prototype.parseGenomicData = function(genomicData, annotationData)
{
	var hugoToGene 		= "hugo_to_gene_index";
	var geneData   		= "gene_data";
	var cna 	   	= "cna";
	var hugo 	   	= "hugo";
	var mrna	   	= "mrna";
	var mutations  		= "mutations";
	var rppa	   	= "rppa";
	var percent_altered 	= "percent_altered";
	var attributes		= "attributes";

	//first extend node fields to support genomic data
	this.addGenomicFields();
	this.addAnnotationFields();
	this.addInQueryField();

	// iterate for every hugo gene symbol in incoming data
	for(var hugoSymbol in genomicData[hugoToGene])
	{
		var geneDataIndex 	= genomicData[hugoToGene][hugoSymbol];		// gene data index for hugo gene symbol
		var _geneData 	= genomicData[geneData][geneDataIndex];			// corresponding gene data

		// Arrays and percent altered data 
		var cnaArray   		= _geneData[cna];
		var mrnaArray  	= _geneData[mrna];
		var mutationsArray 	= _geneData[mutations];
		var rppaArray	  	= _geneData[rppa];
		var percentAltered 	= _geneData[percent_altered];
		
		// corresponding cytoscape web node
		var vis = this._vis;
		var targetNodes = findNode(hugoSymbol, vis );
		this.addInQueryData(targetNodes);
		this.calcCNAPercents(cnaArray, targetNodes);
		this.calcMutationPercent(mutationsArray, targetNodes);
		this.calcRPPAorMRNAPercent(mrnaArray, mrna, targetNodes);
		this.calcRPPAorMRNAPercent(rppaArray, rppa, targetNodes);

		//Calculate alteration percent and add them to the corresponding nodes.
		var alterationPercent = parseInt(percentAltered.split('%'),10)/100;		
		var alterationData =  {PERCENT_ALTERED: alterationPercent };
		this._vis.updateData("nodes",targetNodes, alterationData);
	}

	//Lastly parse annotation data and add "dataSource" fields
	this.addAnnotationData(annotationData);
};

NetworkSbgnVis.prototype.addInQueryData = function(targetNodes)
{
	var in_query_data =  {IN_QUERY: "true" };
	this._vis.updateData("nodes",targetNodes, in_query_data);
}

NetworkSbgnVis.prototype.addAnnotationData = function(annotationData)
{
	var nodeArray = this._vis.nodes();
	for ( var i = 0; i < nodeArray.length; i++) 
	{
		if(nodeArray[i].data.glyph_class == "process")
		{
			//Temporary hack to get rid of id extensions of glyphs.
			var glyphID = ((nodeArray[i].data.id).replace("LEFT_TO_RIGHT", "")).replace("LEFT_TO_RIGHT", "");
			var annData = annotationData[glyphID];
			var parsedData = _safeProperty(annData.dataSource[0].split(";")[0]);
			var data    = {DATA_SOURCE: parsedData};
			//var data    = {DATA_SOURCE: annData.dataSource[0]};
			this._vis.updateData("nodes",[nodeArray[i].data.id], data);
		}
	}
}


/** 
 * Searches an sbgn node whose label fits with parameter hugoSymbol
**/
function findNode(hugoSymbol, vis)
{
	var nodeArray = vis.nodes();
	var nodes = new Array();
	for ( var i = 0; i < nodeArray.length; i++) 
	{
		if(nodeArray[i].data.glyph_label_text == hugoSymbol)
		{
			nodes.push(nodeArray[i].data.id);
		}
	}
	return nodes;
}


/** 
 * calculates cna percents ands adds them to target node
**/
NetworkSbgnVis.prototype.calcCNAPercents = function(cnaArray, targetNodes)
{  
	var amplified	= "AMPLIFIED";
	var gained    	= "GAINED";
	var hemiDeleted = "HEMIZYGOUSLYDELETED";
	var homoDeleted	= "HOMODELETED";

	var percents = {};
	percents[amplified] = 0;
	percents[gained] = 0;
	percents[hemiDeleted] = 0;
	percents[homoDeleted] = 0;

	var increment = 1/cnaArray.length;

	for(var i = 0; i < cnaArray.length; i++)
	{
		if(cnaArray[i] != null)
			percents[cnaArray[i]] += increment; 

	}

	var ampl = { PERCENT_CNA_AMPLIFIED:percents[amplified] };
	var gain = { PERCENT_CNA_GAINED: percents[gained]};
	var hem =  { PERCENT_CNA_HEMIZYGOUSLY_DELETED: percents[hemiDeleted] };
	var hom =  { PERCENT_CNA_HOMOZYGOUSLY_DELETED: percents[homoDeleted]};
	
	this._vis.updateData("nodes",targetNodes, ampl);
	this._vis.updateData("nodes",targetNodes, gain);
	this._vis.updateData("nodes",targetNodes, hem);
	this._vis.updateData("nodes",targetNodes, hom);

};

/** 
 * calculates rppa or mrna percents ands adds them to target node, data indicator determines which data will be set
**/
NetworkSbgnVis.prototype.calcRPPAorMRNAPercent = function(dataArray, dataIndicator, targetNodes)
{  
	var up		= "UPREGULATED";
	var down   	= "DOWNREGULATED";
	
	var upData = null;
	var DownData = null;

	var percents = {};
	percents[up] = 0;
	percents[down] = 0;

	var increment = 1/dataArray.length;

	for(var i = 0; i < dataArray.length; i++)
	{
		if(dataArray[i] != null)
			percents[dataArray[i]] += increment; 
	}

	if (dataIndicator == "mrna") 
	{
		upData =  {PERCENT_MRNA_UP: percents[up]};
		downData = {PERCENT_MRNA_DOWN: percents[down]};
	} 
	else if(dataIndicator == "rppa") 
	{
		upData =   {PERCENT_RPPA_UP: percents[up]};
		downData = {PERCENT_RPPA_DOWN: percents[down]};
	}
	
	this._vis.updateData("nodes",targetNodes, upData);
	this._vis.updateData("nodes",targetNodes, downData);
};

/**
 * calculates mutation percents ands adds them to target node
**/
NetworkSbgnVis.prototype.calcMutationPercent = function(mutationArray, targetNodes)
{  
	var percent = 0;
	var increment = 1/mutationArray.length
	for(var i = 0; i < mutationArray.length; i++)
	{
		if(mutationArray[i] != null)
			percent += increment;  
	}
	var mutData = {PERCENT_MUTATED: percent};
	this._vis.updateData("nodes",targetNodes, mutData);
};

NetworkSbgnVis.prototype.addInQueryField = function()
{
	var IN_QUERY = {name:"IN_QUERY", type:"string", defValue: "false"};
	this._vis.addDataField(IN_QUERY);
}

/**
 * extends node fields by adding new fields according to annotation data
**/
NetworkSbgnVis.prototype.addAnnotationFields = function()
{
	var DATA_SOURCE = {name:"DATA_SOURCE", type:"string", defValue: ""};
	this._vis.addDataField(DATA_SOURCE);
};


/**
 * extends node fields by adding new fields according to genomic data
**/
NetworkSbgnVis.prototype.addGenomicFields = function()
{
	var cna_amplified 	= {name:"PERCENT_CNA_AMPLIFIED", type:"number", defValue: 0};
	var cna_gained		= {name:"PERCENT_CNA_GAINED", type:"number"};
	var cna_homodel 	= {name:"PERCENT_CNA_HOMOZYGOUSLY_DELETED", type:"number", defValue: 0};
	var cna_hemydel		= {name:"PERCENT_CNA_HEMIZYGOUSLY_DELETED", type:"number", defValue: 0};

	var mrna_up 		= {name:"PERCENT_MRNA_UP", type:"number", defValue: 0};
	var mrna_down 		= {name:"PERCENT_MRNA_DOWN", type:"number", defValue: 0};

	var rppa_up 		= {name:"PERCENT_RPPA_UP", type:"number", defValue: 0};
	var rppa_down 		= {name:"PERCENT_RPPA_DOWN", type:"number", defValue: 0};

	var mutated			= {name:"PERCENT_MUTATED", type:"number", defValue: 0};
	var altered			= {name:"PERCENT_ALTERED", type:"number", defValue: 0};


	this._vis.addDataField(cna_amplified);
	this._vis.addDataField(cna_gained);
	this._vis.addDataField(cna_homodel);
	this._vis.addDataField(cna_hemydel);

	this._vis.addDataField(mrna_down);
	this._vis.addDataField(mrna_up);

	this._vis.addDataField(rppa_down);
	this._vis.addDataField(rppa_up);

	this._vis.addDataField(mutated);
	this._vis.addDataField(altered);
	//this._vis.addDataField(label);
};


/**
 * Select multiple nodes by glyph label
 * all states of a gene will be chosen
**/
NetworkSbgnVis.prototype.multiSelectHugos = function(event)
{
	var selected = this._vis.selected("nodes");

	// do not perform any action on the gene list,
	// if the selection is due to the genes tab
	if(!this._selectFromTab)
	{
		if (_isIE())
		{
		    this._setComponentVis($(this.geneListAreaSelector + " select"), false);
		}

		// deselect all options
		$(this.geneListAreaSelector + " select option").each(
		    function(index)
		    {
			$(this).removeAttr("selected");
		    });

		// select all nodes with same label
		var nodes = this._vis.nodes();
		var sameNodes =  this.sameHugoGenes(selected);
		this._vis.select("nodes", sameNodes);
		// select all nodes with same glyph label text in the gene tab list
		var hugos = this.hugoGenes(selected);
		// update the genelist in the genes tab and select the glyphlabels chosen
		// note we do not select the genes from the geneList when normal select is done
		for (var i=0; i < hugos.length; i++)
		{
			$(this.geneListAreaSelector + " #" +  _safeProperty(hugos[i].data.id)).attr(
					     "selected", "selected");
		}

		if (_isIE())
		{
		    this._setComponentVis($(this.geneListAreaSelector + " select"), true);
		}
	}
	// also update Re-submit button
	if (selected.length > 0)
	{
		// enable the button
		$(this.genesTabSelector + " #re-submit_query").button("enable");
	}
	else
	{
		// disable the button
		$(this.genesTabSelector + " #re-submit_query").button("disable");
	}
}

/*
/**
 * Calculates weight values for each gene by its alteration frequency
 * then these weights are adjusted by the adjustWeights function (this._geneWeightArray)
 * unique to SBGN view
 * also creates and updates the HUGOGENES
 * @returns an array of weights ranging from 0 to 100
 */
NetworkSbgnVis.prototype.initializeWeights = function()
{
	var weights = new Array();
	var nodes = this._vis.nodes();
	for (var i = 0; i < nodes.length; i++)
	{
		if (nodes[i].data["PERCENT_ALTERED"] != null)
		{
			weights[nodes[i].data.id] = nodes[i].data["PERCENT_ALTERED"]  *  100;
		}
		else
		{
			weights[nodes[i].data.id] = 0;
		}
		var glyph = nodes[i].data.glyph_class;
		if (glyph == this.MACROMOLECULE)
		{
			// first update hugogenes to hold one node of every macromolecule hugotype
			// these are either proteins or genes
			var label = this.geneLabel(nodes[i].data);
			var check = 0;
			for (var j = 0; j < this.HUGOGENES.length; j++)
			{
				if (label == this.geneLabel(this.HUGOGENES[j].data))
				{
					check = 1;
					break;
				}
			}
			// if its a new hugolabel add it to the hugogenes
			if (check == 0)
			{
				this.HUGOGENES.push(nodes[i]);
			}
		}		
	}
	return weights;
};
/**
 * ADJUST WEIGHTS
 * used to adjust weights based on the proposed algorithm
 * propogates the weights to maintain 5 principles:
 * P1. If a node has a weight of at least  it should be displayed.
 * P2. If a non-process node has an initial weight of at least , 
 * all the processes it is involved with should be displayed.
 * P3. If a process is to be displayed, then all its inputs (substrates), 
 * outputs (products), and effectors should be displayed too.
 * P4. If a node has an initial weight of at least , the parent node 
 * (complex or compartment) should be shown. In other words a parent node 
 * should be shown if at least one of its children has an initial weight 
 * of at least .
 * P5. A complex molecule should always be shown with all its components.
 * The code has initialization (A0) and  4 steps (A1-A4)
**/
NetworkSbgnVis.prototype._geneWeightArray = function(w)
{
	var weights = w;
	var parents = new Array();
	var pId = new Array();
	var processes = new Array();
	var leaves = new Array();
	
	var nodes = this._vis.nodes();

	// A0: initialization
	for (var i = 0; i < nodes.length; i++)
	{
		var glyph = nodes[i].data.glyph_class;

		// make a list of processes for latter update of weights
		if (glyph == this.PROCESS)
		{
			processes.push(nodes[i]);
		}
		// initialize the parent ID
		pId[nodes[i].data.id] = -1;
		
		// update leaves array
		if ( this._vis.childNodes(nodes[i].data.id).length == 0)
		{				
			leaves.push(nodes[i]);
		}
		
	}
	
	// update parents array
	var k = 0;
	for (var i = 0; i < nodes.length; i++)
	{
		if ( this._vis.childNodes(nodes[i]).length > 0)
		{
			var children = this._vis.childNodes(nodes[i]);
			for (var j = 0; j < children.length; j++)
			{
				pId[children[j].data.id] = k;
				
			}
			parents[k] = nodes[i].data.id;
			k++;
		}
	}
	
	// A1: update process weights based on neighbors
	// for each process, set the initial weight the maximum of its neighbors
	for (var i = 0; i < processes.length; i++)
	{
		var max = 0;
		var neighbors = this._vis.firstNeighbors([processes[i]]).neighbors;
		for(var j = 0; j < neighbors.length; j++)
		{
			var nID = neighbors[j].data.id;
			if (weights[nID] > max)
			{
				max = weights[nID];
			}
		}
		if (weights[processes[i].data.id] < max)
		{
			weights[processes[i].data.id] = max;
		}
	}
	
	// update all neighbors of processes to have the weight of the process
	for (var i = 0; i < processes.length; i++)
	{
		var w = weights[processes[i].data.id] ;
		var neighbors = this._vis.firstNeighbors([processes[i]]).neighbors;
		var complexNeighbors = new Array();
		for(var j = 0; j < neighbors.length; j++)
		{
			if (weights[neighbors[j].data.id]  < w)
			{
				weights[neighbors[j].data.id] = w;
			}
		}
	}
	
	// make the parent nodes hold the maximum weight of its children
	for (var i = 0; i < leaves.length; i++)
	{
		var node = leaves[i];
		
		while (true)
		{
			// see if we can go higher one level
			if (pId[node.data.id] == -1)
				break;
			var parentID = parents[pId[node.data.id]];
			// if the weight of the parent is less than the child, update its weight by the child
			if (weights[node.data.id] > weights[parentID] )
			{
				 weights[parentID]  = weights[node.data.id];
			}
			else
			{
				// if the parent is higher than the child what should be done?
				// by default it breaks
				// in our case else will never happen
				// kept for debugging or future changes
				break;
			}
			// go up one level
			node = this._vis.node(parentID);
		}
	}

	// A3: propogate max values to parents from leaves to root
	for (var i = 0; i < leaves.length; i++)
	{
		var node = leaves[i];
		var nodeID = node.data.id;
		var pCheck= pId[nodeID];
		while (pCheck > -1)
		{
			var parent = this._vis.node(parents[pCheck]);
			var parentID = parent.data.id;
			if (weights[parentID] < weights[nodeID])
			{
				weights[parentID]  = weights[nodeID];
			}
			pCheck = pId[parentID];
			node = parent;
		}
	}
	
	// make sure all complex nodes 
	// A4: propogate max values of complex hierarchies down to leaves
	for (var i = 0; i < nodes.length; i++)
	{
		var n = nodes[i];
		if (n.data.glyph_class == this.COMPLEX && weights[n.data.id] > 0)
		{
			var children = this._vis.childNodes(n);
			while (children.length > 0)
			{
				var nextGeneration = new Array();
				for(var j = 0; j < children.length; j++)
				{
					weights[children[j].data.id] = weights[n.data.id];
					if (children[j].data.glyph_class == this.COMPLEX)
					{
						nextGeneration.push(children[j]);
					}
				}
				children = nextGeneration;
			}	
		}
	}
	
	return weights;
};


/**
 * Creates an array of visible (i.e. non-filtered) genes.
 * the HUGOGENES are macromolecules, each of a different glyph_label
 * as a representative of that glyph_label
 * @return		array of visible genes
 */
NetworkSbgnVis.prototype._visibleGenes = function()
{
	// set the genes to be shown in the gene list
	var genes = this.HUGOGENES;

	// sort genes by glyph class (alphabetically)
	genes.sort(_geneSort);
	return genes;
};

/**
 * Updates selected genes when clicked on a gene on the Genes Tab.
 * When a gene is selected from the gene list, all macromolecules or nucleic
 * acid features with the same glyph name are selected.
 * @param evt	target event that triggered the action
 */
NetworkSbgnVis.prototype.updateSelectedGenes = function(evt)
{
	// this flag is set to prevent updateGenesTab function to update the tab
	// when _vis.select function is called.
	
	this._selectFromTab = true;
	var selectedHugos = new Array();

	// deselect all nodes
	this._vis.deselect("nodes");

	// collect id's of selected node's on the tab
	$(this.geneListAreaSelector + " select option").each(
        function(index)
        {
            if ($(this).is(":selected"))
            {
                var nodeId = $(this).val();
                selectedHugos.push(nodeId);
            }
        });

	var nodes = this.getVisibleNodes();

	// array of nodes to select
	var selectedNodes = new Array();
	
	for (var i = 0; i < nodes.length; i++)
	{
		if (nodes[i].data.glyph_class == this.MACROMOLECULE || 
			nodes[i].data.glyph_class == this.NUCLEIC_ACID)
		{
			for ( var j = 0; j < selectedHugos.length; j++) 
			{
				if (this.geneLabel(nodes[i].data) == selectedHugos[j])
				{
					selectedNodes.push(nodes[i].data.id);
					break;
				}
			}
		}
	}

	// select all checked nodes
	this._vis.select("nodes", selectedNodes);

	// reset flag
	this._selectFromTab = false;
};

/**
 * returns all nodes from HUGOGENES array that have the same glyph_label as
 * the nodes in the elements list.
 */
NetworkSbgnVis.prototype.hugoGenes = function(elements)
{	
	//hugo elements contains nodes from hugo genes.
	var hugoElements = new Array();
	
	for (var i=0; i < elements.length; i++)
	{
		if (elements[i].data.glyph_class != this.MACROMOLECULE)
		{
			continue;
		}
		for(var j=0; j<this.HUGOGENES.length; j++)
		{
			if (this.geneLabel(this.HUGOGENES[j].data) == this.geneLabel(elements[i].data))
			{
				hugoElements.push(this.HUGOGENES[j]);
				break;
			}
		}
	}
	return hugoElements;
};

/**
 * returns all nodes in the graph that have the same label as
 * the nodes in the elements list.
 */
NetworkSbgnVis.prototype.sameHugoGenes = function(elements)
{
	var sameElements = new Array();
	var nodes = this._vis.nodes();
	for (var i=0; i < elements.length; i++)
	{
		if (elements[i].data.glyph_class == this.MACROMOLECULE)
		{
			for(var j=0; j<nodes.length; j++)
			{
				if (this.geneLabel(nodes[j].data) == this.geneLabel(elements[i].data))
				{
					sameElements.push(nodes[j]);
				}
			}
		}
		else
		{
			sameElements.push(elements[i]);
		}
		
	}
	return sameElements;
}
/**
 * Updates the gene tab if at least one node is selected or deselected on the
 * network. Here, the gene list is not changed. This is single click, 
 * double click is used for multiple selecting
 * for now whenever a gene is selected the row associated with the glyph label
 * is highlighted and all genes with same property are selected.
 * 
 * @param evt	event that triggered the action
 */
NetworkSbgnVis.prototype.updateGenesTab = function(evt)
{
	var selected = this._vis.selected("nodes");

	// do not perform any action on the gene list,
    // if the selection is due to the genes tab
	if(!this._selectFromTab)
	{
		if (_isIE())
		{
			this._setComponentVis($(this.geneListAreaSelector + " select"), false);
		}

		// deselect all options
		$(this.geneListAreaSelector + " select option").each(
			function(index)
			{
			$(this).removeAttr("selected");
			});

		if (_isIE())
		{
			this._setComponentVis($(this.geneListAreaSelector + " select"), true);
		}
	}
	// also update Re-submit button
	if (selected.length > 0)
	{
		// enable the button
		$(this.genesTabSelector + " #re-submit_query").button("enable");
	}
	else
	{
		// disable the button
		$(this.genesTabSelector + " #re-submit_query").button("disable");
	}
};

/**
 * Comparison function to sort genes alphabetically.
 * overwritten to check againsts glyph_label
 * @param node1	node to compare to node2
 * @param node2 node to compare to node1
 * @return 		positive integer if node1 is alphabetically greater than node2
 * 				negative integer if node2 is alphabetically greater than node1
 * 				zero if node1 and node2 are alphabetically equal
 */
function _geneSort (node1, node2)
{
    if (node1.data.glyph_label_text > node2.data.glyph_label_text)
    {
        return 1;
    }
    else if (node1.data.glyph_label_text < node2.data.glyph_label_text)
    {
        return -1;
    }
    else
    {
        return 0;
    }
}

/**
 *  returns the glyph label which is the name of the macromolecule
**/
NetworkSbgnVis.prototype.geneLabel = function(data)
{
	return data.glyph_label_text;
};
/**
 * Filters out all non-selected nodes by the adjust weights (filtering algorithm)
 * First, we get the selected nodes
 * Second, by calling adjustWeights, we get weights of nodes to be filtered. 
 * Third, we add the remaining nodes to manually filtered array. 
 * Fourth, we update the visibility.
**/
NetworkSbgnVis.prototype.filterNonSelected = function()
{
	var nodes = this._vis.nodes();
	var selected = this._vis.selected("nodes");
	var weights = new Array();

	for (var i=0; i < nodes.length; i++)
	{
		var id = nodes[i].data.id;
		weights[id] = 0;
	}
	for (var i=0; i < selected.length; i++)
	{
		var id = selected[i].data.id;
		weights[id] = 1;
	}
	weights = this._geneWeightArray(weights);
	for (var i=0; i < nodes.length; i++)
	{
		var id = nodes[i].data.id;
		if(weights[id] == 0)
		{
			this._manuallyFiltered.push(id);
		}
	}
	this.updateVisibility();
};

/**
 * Filters out all selected nodes by the adjust weights (filtering algorithm)
 * First, we get the selected nodes
 * Second, by calling adjustWeights, we get weights of nodes to be filtered. 
 * Third, we add these nodes to manually filtered array. 
 * Fourth, we update the visibility.
**/
NetworkSbgnVis.prototype.filterSelectedGenes = function()
{
	var nodes = this._vis.nodes();
	var selected = this._vis.selected("nodes");
	var weights = new Array();

	for (var i=0; i < nodes.length; i++)
	{
		var id = nodes[i].data.id;
		weights[id] = 0;
	}
	for (var i=0; i < selected.length; i++)
	{
		var id = selected[i].data.id;
		weights[id] = 1;
	}
	weights = this._geneWeightArray(weights);
	for (var i=0; i < nodes.length; i++)
	{
		var id = nodes[i].data.id;
		if(weights[id] == 1)
		{
			this._manuallyFiltered.push(id);
		}
	}
	this.updateVisibility();
};

/**
 * Initializes the gene filter sliders.
 */
NetworkSbgnVis.prototype._initSliders = function()
{
	var self = this;

	var keyPressListener = function(evt) {
		self._keyPressListener(evt);
	};

	var weightSliderStop = function(evt, ui) {
		self._weightSliderStop(evt, ui);
	};

	var weightSliderMove = function(evt, ui) {
		self._weightSliderMove(evt, ui);
	};


	// add key listeners for input fields
	$(this.filteringTabSelector + " #weight_slider_field").keypress(keyPressListener);
	$(this.filteringTabSelector + " #affinity_slider_field").keypress(keyPressListener);

	// show gene filtering slider
	$(this.filteringTabSelector + " #weight_slider_bar").slider(
		{value: this.ALTERATION_PERCENT,
		    stop: weightSliderStop,
		    slide: weightSliderMove});

};

/**
 * Updates the contents of the details tab according to
 * the currently selected elements.
 * used for multiple selecting of nodes
 * so multiple nodes of glyph_class = macromolecule || nucleic acid feature
 * are acceptable if all of them have the same glyph_label_text
 * @param evt
 */
NetworkSbgnVis.prototype.multiUpdateDetailsTab = function(evt)
{
	var selected = this._vis.selected("nodes");
	var data;
	var self = this;
	// empty everything and make the error div and hide it
	$(self.detailsTabSelector).empty();
	jQuery('<div />' , {class: 'error'}).appendTo(self.detailsTabSelector);
	$(self.detailsTabSelector + " .error").empty();
	$(self.detailsTabSelector + " .error").hide();
	var glyph0 = selected[0].data.glyph_class;
	// if there is more than one node selected and the first is a macromolecule or nucleic acide
	if (selected.length > 1 && 
		(glyph0 == this.MACROMOLECULE || glyph0 == this.NUCLEIC_ACID))
	{
		var allMacro = 1;
		// check if all of them have the same glyph_label_text
		for(var i=1; i < selected.length; i++)
		{
			if(selected[i].data.glyph_label_text != selected[i-1].data.glyph_label_text)
			{
				allMacro = 0;
				break;
			}
		}
		// if so, retrieve information for the first one
		if(allMacro == 1)
		{
			data = selected[0].data;
		}
		else
		{
			$(self.detailsTabSelector + " .error").html(
			    "Currently more than one node is selected. Please, select only one node to see details.");
			$(self.detailsTabSelector + " .error").show();
			return;
		}
		// right the glyph_class and the glyph_label of the gene 
		var label = _safeProperty(this.geneLabel(data));
		$(self.detailsTabSelector + " div").empty();
		var text = '<div class="header"><span class="title"><label>';
		text +=  toTitleCase(data.glyph_class) + ' Properties';
		text += '</label></span></div>';
		text += '<div class="name"><label>Name: </label>' + label + '</div>';
		text += '<div class="genomic-profile-content"></div>';
		text += '<div class="biogene-content"></div>';

		$(self.detailsTabSelector).html(text);
		
		// send AJAX request to retrieve information
		var queryParams = {"query": label,
			"org": "human",
			"format": "json",
			"timeout": 5000};
		
		$(self.detailsTabSelector + " .genomic-profile-content").append(
			'<img src="images/ajax-loader.gif">');
		// the ajax request expires in 5 seconds, can be reduced
		$.ajax({
		    type: "POST",
		    url: "bioGeneQuery.do",
		    async: true,
		    timeout: 5000,
		    data: queryParams,
		    error: function(){
				$(self.detailsTabSelector).empty();
				jQuery('<div />' , {class: 'error'}).appendTo(self.detailsTabSelector);
				$(self.detailsTabSelector + " .error").append(
				    "Error retrieving data: " + queryResult.returnCode);
				$(self.detailsTabSelector + " .error").show();
				return;
			},
		    success: function(queryResult) {
			if(queryResult.count > 0)
			{
				// generate the view by using backbone
				var biogeneView = new BioGeneView(
					{el: self.detailsTabSelector + " .biogene-content",
					data: queryResult.geneInfo[0]});
			}
			else
			{
				$(self.detailsTabSelector + " .biogene-content").html(
					"<p>No additional information available for the selected node.</p>");
			}
	
			// generate view for genomic profile data
			var genomicProfileView = new GenomicProfileView(
			    {el: self.detailsTabSelector + " .genomic-profile-content",
				data: data});
			// very important to return to avoid unpredictable delays
			return;
		    }
		});	
	}
	else
	{
		// if this is not the case, go to the normal updateDetailsTab function 
		// that accepts only one node at a time
		this.updateDetailsTab(evt);
	}
};

/**
 * Updates the contents of the details tab according to
 * the currently selected elements.
 *
 * @param evt
 */
NetworkSbgnVis.prototype.updateDetailsTab = function(evt)
{
	var selected = this._vis.selected("nodes");
	var data;
	var self = this;
	// empty everything and make the error div and hide it
	$(self.detailsTabSelector).empty();
	jQuery('<div />' , {class: 'error'}).appendTo(self.detailsTabSelector);
	$(self.detailsTabSelector + " .error").empty();
	$(self.detailsTabSelector + " .error").hide();
	// only one node should be selected at a time
	if(selected.length == 1)
	{

		data = selected[0].data;
		// first show the glyph_class
		var text = '<div class="header"><span class="title"><label>';
		text +=  toTitleCase(data.glyph_class) + ' Properties';
		text += '</label></span></div>';
		// compartment and simple chemicals just have a name
		if(data.glyph_class == this.COMPARTMENT 
			|| data.glyph_class == this.SIMPLE_CHEMICAL)
		{
			text += '<div class="name"><label>Name: </label>' + this.geneLabel(data) + "</div>";
		}
		// processes have data source
		else if (data.glyph_class == this.PROCESS)
		{
			text += '<div class="name"><label>Data Source: </label>' + data.DATA_SOURCE + "</div>";
		}
		// for macromolecules and nucleic acids we have to write the name and then send a query to get the information
		else if (data.glyph_class == this.MACROMOLECULE 
			|| data.glyph_class == this.NUCLEIC_ACID)
		{
			// get the label and make it safe to avoid characters that might cause errorous html code
			var label = _safeProperty(this.geneLabel(data));
			text += '<div class="name"><label>Name: </label>' + label + '</div>';
			// make two areas for genomic data (in the element.data)
			text += '<div class="genomic-profile-content"></div>';
			// and biogene content which comes from the ajax query
			text += '<div class="biogene-content"></div>';
			// flush this html by jquery (jSON) to update the data later
			$(self.detailsTabSelector).html(text);
			// make the ajax query in json format
			var queryParams = {"query": label,
				"org": "human",
				"format": "json"};
			// put the wait sign
			$(self.detailsTabSelector + " .genomic-profile-content").append(
				'<img src="images/ajax-loader.gif">');
			// send ajax request with async = true and timeout is 5"
			$.ajax({
			    type: "POST",
			    url: "bioGeneQuery.do",
			    async: true,
			    timeout: 5000,
			    data: queryParams,
			    // if the response fails write an error message
			    error: function(){
					$(self.detailsTabSelector).empty();
					jQuery('<div />' , {class: 'error'}).appendTo(self.detailsTabSelector);
					$(self.detailsTabSelector + " .error").append(
					    "Error retrieving data: " + queryResult.returnCode);
					$(self.detailsTabSelector + " .error").show();
					return;
				},
			    // if success code is returned write the given data in the divs with jSon
			    success: function(queryResult) {
				if(queryResult.count > 0)
				{
					// generate the view by using backbone
					var biogeneView = new BioGeneView(
						{el: self.detailsTabSelector + " .biogene-content",
						data: queryResult.geneInfo[0]});
				}
				else
				{
					$(self.detailsTabSelector + " .biogene-content").html(
						"<p>No additional information available for the selected node.</p>");
				}
	
				// generate view for genomic profile data
				var genomicProfileView = new GenomicProfileView(
				    {el: self.detailsTabSelector + " .genomic-profile-content",
					data: data});
				return;
			    }
			});
		}
		// complexes are different. all macromolecule or nuleic acid children should be listed
		else if (data.glyph_class == this.COMPLEX)
		{
			text += '<div class="complexProperty">';
			// get the children
			var children = this._vis.childNodes(selected[0].data.id);
			// holds data of children
			var dataList = new Array();
			var check = new Array();
			
			for(var i = 0; i < children.length; i++)
			{
				data = children[i].data;
				var label = _safeProperty(this.geneLabel(data));
				if(data.glyph_class == this.MACROMOLECULE
					|| data.glyph_class == this.NUCLEIC_ACID)
				{
					// to ensure every child is checked only once
					check[label] = 0;
				}
	
			}
			// number of unrepetitive children
			var cnt = 0;
		
			for(var i = 0; i < children.length; i++)
			{
				// for each child
				data = children[i].data;
				var label = _safeProperty(this.geneLabel(data));
				if(data.glyph_class == this.MACROMOLECULE
					&& check[label] == 0)
				{
					// make divs to update by ajax requests
					// and add hide and show with jquery
					text += '<div class="geneHide" id="gene' + label + 'Hide" ';
					text += 'onclick="$(' + "'#gene" + label + "').hide();";
					text += "$('#gene" + label + "Hide').hide();";
					text += "$('#gene" + label + "Show').show();" + '"><span class="title"><label> - ' + this.geneLabel(data);
					text += '</label></span></div>';

					text += '<div class="geneShow" id="gene' + label + 'Show" ';
					text += 'onclick="' + "$('#gene" + label + "').show();";
					text += "$('#gene" + label + "Hide').show();";
					text += "$('#gene" + label + "Show').hide();" + '"><span class="title"><label> + ' + this.geneLabel(data);
					text += '</label></span></div>';

					text += '<div class="geneProperty" style="display:none;" id="gene' + label + '">';
					text += '<div class="genomic-profile-content"></div>';
					text += '<div class="biogene-content"></div>';
					text += '</div><br />';
					// check the element to not write it again
					check[label] = 1;
					dataList[cnt] = data;
					cnt ++;
				}
			}
			text += "</div>";
			// flush the html5 code to the details tab
			$(self.detailsTabSelector).html(text);
			for(var i = 0; i < dataList.length  ; i++)
			{
				// for each data send an ajax request as done before 
				// for macromolecules and nucleic acid features
				data = dataList[i];
				var label = _safeProperty(this.geneLabel(data));
				var queryParams = {"query": label,
					"org": "human",
					"format": "json",};
				// the div to update the data with jSon
				var divName = self.detailsTabSelector + " #gene" + label;
				$(divName + " .genomic-profile-content").append(
				'<img src="images/ajax-loader.gif">');
				// for each request waits 3" to avoid unresponsiveness
				$.ajax({
				    type: "POST",
				    url: "bioGeneQuery.do",
				    async: false,
				    timeout: 3000,
				    data: queryParams,
				    error: function(){
						$(divName + " .genomic-profile-content").empty();
						$(divName + " .genomic-profile-content").append(
						    "Error retrieving data: " + queryResult.returnCode);
						$(divName + " .genomic-profile-content").show();
						return;
					},
				    success: function(queryResult) {
					if(queryResult.count > 0)
					{
						// generate the view by using backbone
						var biogeneView = new BioGeneView(
							{el: divName + " .biogene-content",
							data: queryResult.geneInfo[0]});
					}
					else
					{
						$(divName + " .biogene-content").html(
							"<p>No additional information available for the selected node.</p>");
					}
	
					// generate view for genomic profile data
					var genomicProfileView = new GenomicProfileView(
					    {el: divName + " .genomic-profile-content",
						data: data});
				    }
				});
			}
			// makle sure for complexes we do not continue
			return;
		}
		// update the div with jquery
		$(self.detailsTabSelector).html(text);
	}
	else if (selected.length > 1)
	{
		// no nodes were selected
		$(self.detailsTabSelector + " div").empty();
		$(self.detailsTabSelector + " .error").append(
		    "Currently more than one node is selected." +
		    "Please, select one node to see details or double click on a gene.");
		    $(self.detailsTabSelector + " .error").show();
		return;
	}
	else
	{
		// no nodes were selected
		$(self.detailsTabSelector + " div").empty();
		$(self.detailsTabSelector + " .error").append(
		    "Currently there is no selected node. Please, select a node to see details.");
		    $(self.detailsTabSelector + " .error").show();
		return;
	}	
	
};
/**
 * makes the first letter of each word uppercase
 *  and the rest lowercase
**/
function toTitleCase(str) {
    return str.replace(/(?:^|\s)\w/g, function(match) {
        return match.toUpperCase();
    });
}
/**
 * remove illegal characters from text 
 * to avoid security leaks or incorrect html
**/
function _safeProperty(str)
{
    var safeProperty = str;

    safeProperty = _replaceAll(safeProperty, " ", "_");
    safeProperty = _replaceAll(safeProperty, "/", "_");
    safeProperty = _replaceAll(safeProperty, "\\", "_");
    safeProperty = _replaceAll(safeProperty, "#", "_");
    safeProperty = _replaceAll(safeProperty, ".", "_");
    safeProperty = _replaceAll(safeProperty, ":", "_");
    safeProperty = _replaceAll(safeProperty, ";", "_");
    safeProperty = _replaceAll(safeProperty, '"', "_");
    safeProperty = _replaceAll(safeProperty, "'", "_");

    return safeProperty;
}

/**
 * Initializes Genes tab.
 */
NetworkSbgnVis.prototype._initGenesTab = function()
{
	// init buttons

	$(this.genesTabSelector + " #filter_genes").button({icons: {primary: 'ui-icon-circle-minus'},
		                  text: false});

	$(this.genesTabSelector + " #crop_genes").button({icons: {primary: 'ui-icon-crop'},
		                text: false});

	$(this.genesTabSelector + " #unhide_genes").button({icons: {primary: 'ui-icon-circle-plus'},
		                  text: false});

	$(this.genesTabSelector + " #search_genes").button({icons: {primary: 'ui-icon-search'},
		                  text: false});

	$(this.filteringTabSelector + " #update_source").button({icons: {primary: 'ui-icon-refresh'},
		                  text: false});

	// re-submit button is initially disabled
	$(this.genesTabSelector + " #re-submit_query").button({icons: {primary: 'ui-icon-play'},
		                     text: false,
		                     disabled: true});

	// $(this.genesTabSelector + " #re-run_query").button({label: "Re-run query with selected genes"});

	// apply tiptip to all buttons on the network tabs
	$(this.networkTabsSelector + " button").tipTip({edgeOffset:8});
};

/**
 * Listener for weight slider movement. Updates current value of the slider
 * after each mouse move.
 */
NetworkSbgnVis.prototype._weightSliderMove = function(event, ui)
{
	// get slider value
	this.sliderVal = ui.value;

	// update current value field
	$(this.filteringTabSelector + "#weight_slider_field").val(
		(_transformValue(this.sliderVal) * (this._maxAlterationPercent / 100)).toFixed(1));
};

/**
 * Listener for weight slider value change. Updates filters with respect to
 * the new slider value.
 */
NetworkSbgnVis.prototype._weightSliderStop = function(event, ui)
{
	// get slider value
	this.sliderVal = ui.value;

	// apply transformation to prevent filtering of low values
	// with a small change in the position of the cursor.
	this.sliderVal = _transformValue(this.sliderVal) * (this._maxAlterationPercent / 100);

	// update threshold
	this._geneWeightThreshold = this.sliderVal;

	// update current value field
	$(this.filteringTabSelector + " #weight_slider_field").val(this.sliderVal.toFixed(1));

	// update filters
	this._filterBySlider();
};


/**
 * Creates a map for process source visibility.
 * Scan all processes and add their data sources.
 * @return	an array (map) of edge source visibility.
 */
NetworkSbgnVis.prototype._initSourceArray = function()
{
	var sourceArray = new Array();

	// dynamically collect all sources

	var nodes = this._vis.nodes();

	for (var i = 0; i < nodes.length; i++)
	{
		if(nodes[i].data.glyph_class == this.PROCESS)
		{
			var source = nodes[i].data.DATA_SOURCE;
		    	sourceArray[source] = true;
		}
	}

	// also set a flag for unknown (undefined) sources
	sourceArray[this.UNKNOWN] = true;

	return sourceArray;
};

/**
 * when update button is clicked, first updates the source visibility
 * array and then updates visibility.
 */
NetworkSbgnVis.prototype.updateSource = function()
{
	for (var key in this._sourceVisibility)
	{
		this._sourceVisibility[key] =
		    $(this.filteringTabSelector + " #" + key + "_check").is(":checked");
	}
	this.updateVisibility();
};

/**
 * updates the visibility according to three priorities
 * P1 : manually filtered nodes
 * P2 : source visibility
 * P3 : alteration visibility (set by the slider)
 * the details of the design is given in NetworkFiltering.Design documentation
 * under ftp://cs.bilkent.edu.tr/cBioPortal/node-filtering/
 */
NetworkSbgnVis.prototype.updateVisibility = function()
{
	//get all nodes.
	var nodes = this._vis.nodes();
	var weights = new Array();
	//set threshold as the current slider value (in range of 0-MAX).
	var threshold = this.sliderVal;
	for(var i = 0; i < nodes.length; i++)
	{
		var data = nodes[i].data;
		//check if it should be shown according to alteration frequency
		if(this._geneWeightMap[data.id] >= threshold)
		{
			// if so, check the source and set weight accordingly.
			//notice, only processes have data sources.
			//source array has boolean value and refers to
			//whether the source is checked or not
			if (data.glyph_class == this.PROCESS
					&& this._sourceVisibility[data.DATA_SOURCE])
			{
				weights[data.id] = 1;
			}
			else
			{
				weights[data.id] = 0;
			}
		}
		else
		{
			//if it is not to be shown by alteration, set the weight to be zero.
			weights[data.id] = 0;
		}
	}
	// set manually filtered nodes to zero
	for (var i = 0; i < this._manuallyFiltered.length; i++)
	{
		var id =  this._manuallyFiltered[i];
		weights[id] = 0;
	}
	// adjust weights
	weights = this._geneWeightArray(weights);
	// find the nodes that should be shown
	var showList = new Array();
	for(var i = 0; i < nodes.length; i++)
	{
		var id = nodes[i].data.id;
		if(weights[id] == 1)
		{
			showList.push(nodes[i]);		
		}
	}

	this.visibleNodes = showList.slice(0);
	// filter out every nodes except show list.
	this._vis.filter("nodes", showList);
	// apply changes
	this._refreshGenesTab();
	this._visChanged();
};


/**
 * to show all , first empty the manuallyFiltered array, and then update visibility.
 **/
NetworkSbgnVis.prototype._unhideAll = function()
{
	this._manuallyFiltered = new Array();
	this.updateVisibility();
};

/**
 * update visibility when the slider value is changed.
 */
NetworkSbgnVis.prototype._filterBySlider = function()
{
	this.updateVisibility();
};
/**
 * Listener for affinity slider value change. Updates filters with respect to
 * the new slider value.
 */
NetworkSbgnVis.prototype._affinitySliderChange = function(event, ui)
{
    var sliderVal = ui.value;

    // update current value field
    $(this.genesTabSelector + " #affinity_slider_field").val((sliderVal / 100).toFixed(2));

    // re-calculate gene weights
    this._geneWeightMap = this._geneWeightArray(sliderVal / 100);

    // update filters
};

/**
 * Listener for affinity slider movement. Updates current value of the slider
 * after each mouse move.
 */
NetworkSbgnVis.prototype._affinitySliderMove = function(event, ui)
{
    // get slider value
    var sliderVal = ui.value;

    // update current value field
    $(this.genesTabSelector + " #affinity_slider_field").val((sliderVal / 100).toFixed(2));
};

/**
 * Key listener for input fields on the genes tab.
 * Updates the slider values (and filters if necessary), if the input
 * value is valid.
 *
 * @param event		event triggered the action
 */
NetworkSbgnVis.prototype._keyPressListener = function(event)
{
    var input;

    // check for the ENTER key first
    if (event.keyCode == this.ENTER_KEYCODE)
    {
        if (event.target.id == "weight_slider_field")
        {
            input = $(this.filteringTabSelector + " #weight_slider_field").val();

            // update weight slider position if input is valid

            if (isNaN(input))
            {
                // not a numeric value, update with defaults
                input = this.ALTERATION_PERCENT;
            }
            else if (input < 0)
            {
                // set values below zero to zero
                input = 0;
            }
            else if (input > 100)
            {
                // set values above 100 to 100
                input = 100;
            }

            $(this.filteringTabSelector + " #weight_slider_bar").slider("option", "value",
                                           _reverseTransformValue(input / (this._maxAlterationPercent / 100)));

            // update threshold value
            this._geneWeightThreshold = input;
			this.sliderVal = input;
            // also update filters
            this._filterBySlider();
        }
        else if (event.target.id == "affinity_slider_field")
        {
            input = $(this.filteringTabSelector + " #affinity_slider_field").val();

            var value;
            // update affinity slider position if input is valid
            // (this will also update filters if necessary)

            if (isNaN(input))
            {
                // not a numeric value, update with defaults
                value = 0;
            }
            else if (input < 0)
            {
                // set values below zero to zero
                value = 0;
            }
            else if (input > 1)
            {
                // set values above 1 to 1
                value = 1;
            }

            $(this.filteringTabSelector + " #affinity_slider_bar").slider("option",
                                             "value",
                                             Math.round(input * 100));
        }
        else if (event.target.id == "search_box")
        {
            this.searchGene();
        }
    }
};

/**
 * Creates a map (an array) with <command, function> pairs. Also, adds listener
 * functions for the buttons and for the CytoscapeWeb canvas.
 */
NetworkSbgnVis.prototype._initControlFunctions = function()
{
    var self = this;

    // define listeners as local variables
    // (this is required to pass "this" instance to the listener functions)

    var showNodeDetails = function(evt) {

	    // open details tab instead
	    $(self.networkTabsSelector).tabs("select", 2);
    };

    var handleNodeSelect = function(evt) {
        self.updateGenesTab(evt);
        self.updateDetailsTab(evt);
    };

    var filterSelectedGenes = function() {
        self.filterSelectedGenes();
    };

    var unhideAll = function() {
        self._unhideAll();
    };

    var performLayout = function() {
        self._performLayout();
    };

    var toggleNodeLabels = function() {
        self._toggleNodeLabels();
    };

    var toggleEdgeLabels = function() {
        self._toggleEdgeLabels();
    };

    var toggleMerge = function() {
        self._toggleMerge();
    };

    var togglePanZoom = function() {
        self._togglePanZoom();
    };

    var toggleAutoLayout = function() {
        self._toggleAutoLayout();
    };

    var toggleRemoveDisconnected = function() {
        self._toggleRemoveDisconnected();
    };

    var toggleProfileData = function() {
        self._toggleProfileData();
    };

    var saveAsPng = function() {
        self._saveAsPng();
    };

    var openProperties = function() {
        self._openProperties();
    };

    var highlightNeighbors = function() {
        self._highlightNeighbors();
    };

    var removeHighlights = function() {
        self._removeHighlights();
    };

    var filterNonSelected = function() {
        self.filterNonSelected();
    };

    var showNodeLegend = function() {
        self._showNodeLegend();
    };

    var saveSettings = function() {
        self.saveSettings();
    };

    var defaultSettings = function() {
        self.defaultSettings();
    };

    var searchGene = function() {
        self.searchGene();
    };

    var reRunQuery = function() {
        self.reRunQuery();
    };

    var updateSource = function() {
        self.updateSource();
    };

    var keyPressListener = function(evt) {
        self._keyPressListener(evt);
    };

    var handleMenuEvent = function(evt){
        self.handleMenuEvent(evt.target.id);
    };

    this._controlFunctions = new Array();

    //_controlFunctions["hide_selected"] = _hideSelected;
    this._controlFunctions["hide_selected"] = filterSelectedGenes;
    this._controlFunctions["unhide_all"] = unhideAll;
    this._controlFunctions["perform_layout"] = performLayout;
    this._controlFunctions["show_node_labels"] = toggleNodeLabels;
    //_controlFunctions["show_edge_labels"] = toggleEdgeLabels;
    this._controlFunctions["merge_links"] = toggleMerge;
    this._controlFunctions["show_pan_zoom_control"] = togglePanZoom;
    this._controlFunctions["auto_layout"] = toggleAutoLayout;
    this._controlFunctions["remove_disconnected"] = toggleRemoveDisconnected;
    this._controlFunctions["show_profile_data"] = toggleProfileData;
    this._controlFunctions["save_as_png"] = saveAsPng;
    //_controlFunctions["save_as_svg"] = _saveAsSvg;
    this._controlFunctions["layout_properties"] = openProperties;
    this._controlFunctions["highlight_neighbors"] = highlightNeighbors;
    this._controlFunctions["remove_highlights"] = removeHighlights;
    this._controlFunctions["hide_non_selected"] = filterNonSelected;
    this._controlFunctions["show_node_legend"] = showNodeLegend;

    // add menu listeners
    $(this.mainMenuSelector + " #network_menu a").unbind(); // TODO temporary workaround (there is listener attaching itself to every 'a's)
    $(this.mainMenuSelector + " #network_menu a").click(handleMenuEvent);

    // add button listeners

    $(this.settingsDialogSelector + " #save_layout_settings").click(saveSettings);
    $(this.settingsDialogSelector + " #default_layout_settings").click(defaultSettings);

    $(this.genesTabSelector + " #search_genes").click(searchGene);
    $(this.genesTabSelector + " #search_box").keypress(keyPressListener);
    $(this.genesTabSelector + " #filter_genes").click(filterSelectedGenes);
    $(this.genesTabSelector + " #crop_genes").click(filterNonSelected);
    $(this.genesTabSelector + " #unhide_genes").click(unhideAll);
    $(this.genesTabSelector + " #re-submit_query").click(reRunQuery);

	// add listener for double click action
    this._vis.addListener("dblclick",
                     "nodes",
                     showNodeDetails);

    // add listener for node select & deselect actions

    this._vis.addListener("select",
                     "nodes", 
                     handleNodeSelect);

    this._vis.addListener("deselect",
                     "nodes", 
                     handleNodeSelect);

};

/**
 * Searches for genes by using the input provided within the search text field.
 * Also, selects matching genes both from the canvas and gene list.
 */
NetworkSbgnVis.prototype.searchGene = function()
{
    var query = $(this.genesTabSelector + " #search_box").val();

    // do not perform search for an empty string
    if (query.length == 0)
    {
        return;
    }
	
	visNodes = this.getVisibleNodes();
    var matched = new Array();
    var i;

    // linear search for the input text

    for (i=0; i < visNodes.length; i++)
    {
        if (this.geneLabel(visNodes[i].data).toLowerCase().indexOf(
            query.toLowerCase()) != -1)
        {
            matched.push(visNodes[i].data.id);
        }
    }

    // deselect all nodes
    this._vis.deselect("nodes");

    // select all matched nodes
    this._vis.select("nodes", matched);
};

NetworkSbgnVis.prototype.getVisibleNodes = function()
{	
	var self = this;
    if(this.visibleNodes == null)
    {
    	return self._vis.nodes();
    }

    return this.visibleNodes;
}

NetworkSbgnVis.prototype.getMapOfVisibleNodes = function()
{
	var self = this;

	var visibleMap = new Array();

	var visNodes = (self.visibleNodes == null) ? self._vis.nodes() : self.visibleNodes;

	for(var i = 0 ; i < visNodes.length ; i++)
	{
		if(visNodes[i].data.glyph_class == self.MACROMOLECULE)
		{
			if(visNodes[i].data.IN_QUERY == "true")
				visibleMap[self.geneLabel(visNodes[i].data)] = true;
			else
				visibleMap[self.geneLabel(visNodes[i].data)] = false;
		}
	}

	return visibleMap;
}

/**
 * Refreshes the content of the genes tab, by populating the list with visible
 * (i.e. non-filtered) genes.
 */
NetworkSbgnVis.prototype._refreshGenesTab = function()
{
    // (this is required to pass "this" instance to the listener functions)
    var self = this;

    var showGeneDetails = function(evt){
        self.showGeneDetails(evt);
    };

    var visibleMap = self.getMapOfVisibleNodes();
    var nodeList = self.getVisibleNodes();

    // clear old content
    $(this.geneListAreaSelector + " select").remove();

    $(this.geneListAreaSelector).append('<select multiple></select>');

    // add new content

    for (var key in visibleMap)
    {
        var safeId = _safeProperty(key);

        var classContent;

        if (visibleMap[key] == "true")
        {
            classContent = 'class="in-query" ';
        }
        else
        {
            classContent = 'class="not-in-query" ';
        }

        $(this.geneListAreaSelector + " select").append(
            '<option id="' + safeId + '" ' +
            classContent +
            'value="' + key + '" ' + '>' +
            '<label>' + key + '</label>' +
            '</option>');

        // add double click listener for each gene

        $(this.genesTabSelector + " #" + safeId).dblclick(showGeneDetails);

    }

    var updateSelectedGenes = function(evt){
        self.updateSelectedGenes(evt);
    };

    // add change listener to the select box
    $(this.geneListAreaSelector + " select").change(updateSelectedGenes);

    if (_isIE())
    {
        // listeners on <option> elements do not work in IE, therefore add
        // double click listener to the select box
        $(this.geneListAreaSelector + " select").dblclick(showGeneDetails);

        // TODO if multiple genes are selected, double click always shows
        // the first selected gene's details in IE
    }
};

/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/////////////////////////////////////////////////
/**
 * Hides all dialogs upon selecting a tab other than the network tab.
 */
NetworkSbgnVis.prototype.hideDialogs = function (evt, ui)
{
    // get the index of the tab that is currently selected
    // var selectIdx = $("#tabs").tabs("option", "selected");

    // close all dialogs
    $(this.settingsDialogSelector).dialog("close");
    $(this.nodeInspectorSelector).dialog("close");
    $(this.edgeInspectorSelector).dialog("close");
    $(this.geneLegendSelector).dialog("close");
    $(this.drugLegendSelector).dialog("close");
    $(this.edgeLegendSelector).dialog("close");
};

NetworkSbgnVis.prototype.handleMenuEvent = function(command)
{
    // execute the corresponding function
    var func = this._controlFunctions[command];

	// try to call the handler if it is defined
	if (func != null)
	{
		func();
	}
};

/**
 * Saves layout settings when clicked on the "Save" button of the
 * "Layout Options" panel.
 */
NetworkSbgnVis.prototype.saveSettings = function()
{
    // update layout option values

    for (var i=0; i < (this._layoutOptions).length; i++)
    {

        if (this._layoutOptions[i].id == "autoStabilize")
        {
            // check if the auto stabilize box is checked

            if($(this.settingsDialogSelector + " #autoStabilize").is(":checked"))
            {
                this._layoutOptions[i].value = true;
                $(this.settingsDialogSelector + " #autoStabilize").val(true);
            }
            else
            {
                this._layoutOptions[i].value = false;
                $(this.settingsDialogSelector + " #autoStabilize").val(false);
            }
        }
        else
        {
            // simply copy the text field value
            this._layoutOptions[i].value =
                $(this.settingsDialogSelector + " #" + this._layoutOptions[i].id).val();
        }
    }

    // update graphLayout options
    this._updateLayoutOptions();

    // close the settings panel
    $(this.settingsDialogSelector).dialog("close");
};

/**
 * Reverts to default layout settings when clicked on "Default" button of the
 * "Layout Options" panel.
 */
NetworkSbgnVis.prototype.defaultSettings = function()
{
    this._layoutOptions = this._defaultOptsArray();
    this._updateLayoutOptions();
    this._updatePropsUI();
};

/**
 * Updates the content of the node inspector with respect to the provided data.
 * Data is assumed to be the data of a node.
 *
 * @param data	node data containing necessary fields
 */
NetworkSbgnVis.prototype._updateNodeInspectorContent = function(data, node)
{
    // set title

    var title = this.geneLabel(data);

    if (title == null)
    {
        title = data.id;
    }

    $(this.nodeInspectorSelector).dialog("option",
                                "title",
                                title);

    // clean xref, percent, and data rows

    // These rows for drug view of node inspector.
    $(this.nodeInspectorSelector + " .node_inspector_content .data .targets-data-row").remove();
    $(this.nodeInspectorSelector + " .node_inspector_content .data .atc_codes-data-row").remove();
    $(this.nodeInspectorSelector + " .node_inspector_content .data .synonyms-data-row").remove();
    $(this.nodeInspectorSelector + " .node_inspector_content .data .description-data-row").remove();
    $(this.nodeInspectorSelector + " .node_inspector_content .data .fda-data-row").remove();
    $(this.nodeInspectorSelector + " .node_inspector_content .data .pubmed-data-row").remove();

	$(this.nodeInspectorSelector + " .node_inspector_content .data .clinicaltrials-data-row").remove();
	$(this.nodeInspectorSelector + " .node_inspector_content .data .cancerdrug-data-row").remove();

    // For non drug view of node inspector
    $(this.nodeInspectorSelector + " .node_inspector_content .data .data-row").remove();

    $(this.nodeInspectorSelector + " .node_inspector_content .xref .xref-row").remove();
    $(this.nodeInspectorSelector + " .node_inspector_content .profile .percent-row").remove();
    $(this.nodeInspectorSelector + " .node_inspector_content .profile-header .header-row").remove();

    if (data.type == this.PROTEIN)
    {
        this._addDataRow(this.nodeInspectorSelector, "node", "Gene Symbol", data.label);
        //_addDataRow(this.nodeInspectorSelector, "node", "User-Specified", data.IN_QUERY);

        // add percentage information
        this._addPercentages(data);
    }

    // add cross references

    var links = new Array();

    // parse the xref data, and construct link and labels

    var xrefData = new Array();

    if (data["UNIFICATION_XREF"] != null)
    {
        xrefData = data["UNIFICATION_XREF"].split(";");
    }

    if (data["RELATIONSHIP_XREF"] != null)
    {
        xrefData = xrefData.concat(data["RELATIONSHIP_XREF"].split(";"));
    }

    var link, xref;

    for (var i = 0; i < xrefData.length; i++)
    {
        link = this._resolveXref(xrefData[i]);
        links.push(link);
    }

    // add each link as an xref entry

    if (links.length > 0)
    {
        $(this.nodeInspectorSelector + " .node_inspector_content .xref").append(
            '<tr class="xref-row"><td><strong>More at: </strong></td></tr>');

        this._addXrefEntry(this.nodeInspectorSelector, 'node', links[0].href, links[0].text);
    }

    for (i=1; i < links.length; i++)
    {
        $(this.nodeInspectorSelector + " .node_inspector_content .xref-row td").append(', ');
        this._addXrefEntry(this.nodeInspectorSelector, 'node', links[i].href, links[i].text);
    }
};

/**
 * Add percentages (genomic profile data) to the node inspector with their
 * corresponding colors & names.
 *
 * @param data	node (gene) data
 */
NetworkSbgnVis.prototype._addPercentages = function(data)
{
    var percent;

    // init available profiles array
    var available = new Array();
    available['CNA'] = new Array();
    available['MRNA'] = new Array();
    available['MUTATED'] = new Array();

    // add percentage values

    if (data["PERCENT_CNA_AMPLIFIED"] != null)
    {
        percent = (data["PERCENT_CNA_AMPLIFIED"] * 100);
        this._addPercentRow("cna-amplified", "Amplification", percent, "#FF2500");
        available['CNA'].push("cna-amplified");
    }

    if (data["PERCENT_CNA_HOMOZYGOUSLY_DELETED"] != null)
    {
        percent = (data["PERCENT_CNA_HOMOZYGOUSLY_DELETED"] * 100);
        this._addPercentRow("cna-homozygously-deleted", "Homozygous Deletion", percent, "#0332FF");
        available['CNA'].push("cna-homozygously-deleted");
    }

    if (data["PERCENT_CNA_GAINED"] != null)
    {
        percent = (data["PERCENT_CNA_GAINED"] * 100);
        this._addPercentRow("cna-gained", "Gain", percent, "#FFC5CC");
        available['CNA'].push("cna-gained");
    }

    if (data["PERCENT_CNA_HEMIZYGOUSLY_DELETED"] != null)
    {
        percent = (data["PERCENT_CNA_HEMIZYGOUSLY_DELETED"] * 100);
        this._addPercentRow("cna-hemizygously-deleted", "Hemizygous Deletion", percent, "#9EDFE0");
        available['CNA'].push("cna-hemizygously-deleted");
    }

    if (data["PERCENT_MRNA_WAY_UP"] != null)
    {
        percent = (data["PERCENT_MRNA_WAY_UP"] * 100);
        this._addPercentRow("mrna-way-up", "Up-regulation", percent, "#FFACA9");
        available['MRNA'].push("mrna-way-up");
    }

    if (data["PERCENT_MRNA_WAY_DOWN"] != null)
    {
        percent = (data["PERCENT_MRNA_WAY_DOWN"] * 100);
        this._addPercentRow("mrna-way-down", "Down-regulation", percent, "#78AAD6");
        available['MRNA'].push("mrna-way-down");
    }

    if (data["PERCENT_MUTATED"] != null)
    {
        percent = (data["PERCENT_MUTATED"] * 100);
        this._addPercentRow("mutated", "Mutation", percent, "#008F00");
        available['MUTATED'].push("mutated");
    }

    // add separators

    if (available['CNA'].length > 0)
    {
        $(this.nodeInspectorSelector + " .profile ." + available['CNA'][0]).addClass(
            this.SECTION_SEPARATOR_CLASS);
    }

    if (available['MRNA'].length > 0)
    {
        $(this.nodeInspectorSelector + " .profile ." + available['MRNA'][0]).addClass(
            this.SECTION_SEPARATOR_CLASS);
    }

    if (available['MUTATED'].length > 0)
    {
        $(this.nodeInspectorSelector + " .profile ." + available['MUTATED'][0]).addClass(
            this.SECTION_SEPARATOR_CLASS);
    }


    // add header & total alteration value if at least one of the profiles is
    // available

    if (available['CNA'].length > 0
            || available['MRNA'].length > 0
        || available['MUTATED'].length > 0)
    {

        // add header
        $(this.nodeInspectorSelector + " .profile-header").append('<tr class="header-row">' +
                                                    '<td><div>Genomic Profile(s):</div></td></tr>');

        // add total alteration frequency

        percent = (data["PERCENT_ALTERED"] * 100);

        var row = '<tr class="total-alteration percent-row">' +
                  '<td><div class="percent-label">Total Alteration</div></td>' +
                  '<td class="percent-cell"></td>' +
                  '<td><div class="percent-value">' + percent.toFixed(1) + '%</div></td>' +
                  '</tr>';

        // append as a first row
        $(this.nodeInspectorSelector + " .profile").prepend(row);
    }
};

/**
 * Adds a row to the genomic profile table of the node inspector.
 *
 * @param section	class name of the percentage
 * @param label		label to be displayed
 * @param percent	percentage value
 * @param color		color of the percent bar
 */
NetworkSbgnVis.prototype._addPercentRow = function(section, label, percent, color)
{
    var row = '<tr class="' + section + ' percent-row">' +
              '<td><div class="percent-label"></div></td>' +
              '<td class="percent-cell"><div class="percent-bar"></div></td>' +
              '<td><div class="percent-value"></div></td>' +
              '</tr>';

    $(this.nodeInspectorSelector + " .profile").append(row);

    $(this.nodeInspectorSelector + " .profile ." + section + " .percent-label").text(label);

    $(this.nodeInspectorSelector + " .profile ." + section + " .percent-bar").css(
        "width", Math.ceil(percent) + "%");

    $(this.nodeInspectorSelector + " .profile ." + section + " .percent-bar").css(
        "background-color", color);

    $(this.nodeInspectorSelector + " .profile ." + section + " .percent-value").text(
        percent.toFixed(1) + "%");
};

/**
 * This function shows gene details when double clicked on a node name on the
 * genes tab.
 *
 * @param evt	event that triggered the action
 */
NetworkSbgnVis.prototype.showGeneDetails = function(evt)
{
    // retrieve the selected node
    var node = this._vis.node(evt.target.value);

    // TODO position the inspector, (also center the selected gene?)

    // update inspector content
    this._updateNodeInspectorContent(node.data, node);

    // open inspector panel
    $(this.nodeInspectorSelector).dialog("open").height("auto");

    // if the inspector panel height exceeds the max height value
    // adjust its height (this also adds scroll bars by default)
    if ($(this.nodeInspectorSelector).height() >
        $(this.nodeInspectorSelector).dialog("option", "maxHeight"))
    {
        $(this.nodeInspectorSelector).dialog("open").height(
            $(this.nodeInspectorSelector).dialog("option", "maxHeight"));
    }
};

NetworkSbgnVis.prototype.reRunQuery = function()
{
    // TODO get the list of currently interested genes
    var currentGenes = "";
    var nodeMap = this._selectedElementsMap("nodes");

    for (var key in nodeMap)
    {
        currentGenes += this.geneLabel(nodeMap[key].data) + " ";
    }

    if (currentGenes.length > 0)
    {
        // update the list of seed genes for the query
        $("#main_form #gene_list").val(currentGenes);

        // re-run query by performing click action on the submit button
        $("#main_form #main_submit").click();
    }
};

/**
 * Determines the visibility of a node for filtering purposes. This function is
 * designed to filter disconnected nodes.
 *
 * @param element	node to be checked for visibility criteria
 * @return			true if the node should be visible, false otherwise
 */
NetworkSbgnVis.prototype.isolation = function(element)
{
    var visible = false;

    // if an element is already filtered then it should remain invisible
    if (this._alreadyFiltered[element.data.id] != null)
    {
        visible = false;
    }
    else
    {
        // check if the node is connected, if it is disconnected it should be
        // filtered out
        if (this._connectedNodes[element.data.id] != null)
        {
            visible = true;
        }

        if (!visible)
        {
            // if the node should be filtered, then add it to the map
            this._alreadyFiltered[element.data.id] = element;
            this._filteredByIsolation[element.data.id] = element;
        }
    }

    return visible;
};


/**
 * Creates a map (on element id) of selected elements.
 *
 * @param group		data group (nodes, edges, all)
 * @return			a map of selected elements
 */
NetworkSbgnVis.prototype._selectedElementsMap = function(group)
{
    var selected = this._vis.selected(group);
    var map = new Array();

    for (var i=0; i < selected.length; i++)
    {
        var key = selected[i].data.id;
        map[key] = selected[i];
    }

    return map;
};

/**
 * Creates a map (on element id) of connected nodes.
 *
 * @return	a map of connected nodes
 */
NetworkSbgnVis.prototype._connectedNodesMap = function()
{
    var map = new Array();
    var edges;

    // if edges merged, traverse over merged edges for a better performance
    if (this._vis.edgesMerged())
    {
        edges = this._vis.mergedEdges();
    }
    // else traverse over regular edges
    else
    {
        edges = this._vis.edges();
    }

    var source;
    var target;


    // for each edge, add the source and target to the map of connected nodes
    for (var i=0; i < edges.length; i++)
    {
        if (edges[i].visible)
        {
            source = this._vis.node(edges[i].data.source);
            target = this._vis.node(edges[i].data.target);

            map[source.data.id] = source;
            map[target.data.id] = target;
        }
    }

    return map;
};

/**
 * This function is designed to be invoked after an operation (such as filtering
 * nodes or edges) that changes the graph topology.
 */
NetworkSbgnVis.prototype._visChanged = function()
{
    // perform layout if auto layout flag is set

    if (this._autoLayout)
    {
        // re-apply layout
        this._performLayout();
    }
};

/**
 * Highlights the neighbors of the selected nodes.
 *
 * The content of this method is copied from GeneMANIA (genemania.org) sources.
 */
NetworkSbgnVis.prototype._highlightNeighbors = function(/*nodes*/)
{
    /*
     if (nodes == null)
     {
     nodes = _vis.selected("nodes");
     }
     */

    var nodes = this._vis.selected("nodes");

    if (nodes != null && nodes.length > 0)
    {
        var fn = this._vis.firstNeighbors(nodes, true);
        var neighbors = fn.neighbors;
        var edges = fn.edges;
        edges = edges.concat(fn.mergedEdges);
        neighbors = neighbors.concat(fn.rootNodes);
        var bypass = this._vis.visualStyleBypass() || {};

        if( ! bypass.nodes )
        {
            bypass.nodes = {};
        }
        if( ! bypass.edges )
        {
            bypass.edges = {};
        }

        var allNodes = this._vis.nodes();

        $.each(allNodes, function(i, n) {
            if( !bypass.nodes[n.data.id] ){
                bypass.nodes[n.data.id] = {};
            }
            bypass.nodes[n.data.id].opacity = 0.25;
        });

        $.each(neighbors, function(i, n) {
            if( !bypass.nodes[n.data.id] ){
                bypass.nodes[n.data.id] = {};
            }
            bypass.nodes[n.data.id].opacity = 1;
        });

        var opacity;
        var allEdges = this._vis.edges();
        allEdges = allEdges.concat(this._vis.mergedEdges());

        $.each(allEdges, function(i, e) {
            if( !bypass.edges[e.data.id] ){
                bypass.edges[e.data.id] = {};
            }
            /*
             if (e.data.networkGroupCode === "coexp" || e.data.networkGroupCode === "coloc") {
             opacity = AUX_UNHIGHLIGHT_EDGE_OPACITY;
             } else {
             opacity = DEF_UNHIGHLIGHT_EDGE_OPACITY;
             }
             */

            opacity = 0.15;

            bypass.edges[e.data.id].opacity = opacity;
            bypass.edges[e.data.id].mergeOpacity = opacity;
        });

        $.each(edges, function(i, e) {
            if( !bypass.edges[e.data.id] ){
                bypass.edges[e.data.id] = {};
            }
            /*
             if (e.data.networkGroupCode === "coexp" || e.data.networkGroupCode === "coloc") {
             opacity = AUX_HIGHLIGHT_EDGE_OPACITY;
             } else {
             opacity = DEF_HIGHLIGHT_EDGE_OPACITY;
             }
             */

            opacity = 0.85;

            bypass.edges[e.data.id].opacity = opacity;
            bypass.edges[e.data.id].mergeOpacity = opacity;
        });

        this._vis.visualStyleBypass(bypass);
        //CytowebUtil.neighborsHighlighted = true;

        //$("#menu_neighbors_clear").removeClass("ui-state-disabled");
    }
};

/**
 * Removes all highlights from the visualization.
 *
 * The content of this method is copied from GeneMANIA (genemania.org) sources.
 */
NetworkSbgnVis.prototype._removeHighlights = function()
{
    var bypass = this._vis.visualStyleBypass();
    bypass.edges = {};

    var nodes = bypass.nodes;

    for (var id in nodes)
    {
        var styles = nodes[id];
        delete styles["opacity"];
        delete styles["mergeOpacity"];
    }

    this._vis.visualStyleBypass(bypass);

    //CytowebUtil.neighborsHighlighted = false;
    //$("#menu_neighbors_clear").addClass("ui-state-disabled");
};

/**
 * Displays the gene legend in a separate panel.
 */
NetworkSbgnVis.prototype._showNodeLegend = function()
{
    // open legend panel
    $(this.geneLegendSelector).dialog("open").height("auto");
};

/**
 * Adds a data row to the node or edge inspector.
 *
 * @param selector	node or edge inspector selector (div id)
 * @param type		type of the inspector (should be "node" or "edge")
 * @param label		label of the data field
 * @param value		value of the data field
 * @param section	optional class value for row element
 */
NetworkSbgnVis.prototype._addDataRow = function(selector, type, label, value /*, section*/)
{
    var section = arguments[4];

    if (section == null)
    {
        section = "";
    }
    else
    {
        section += " ";
    }

    // replace null string with a predefined string

    if (value == null)
    {
        value = this.UNKNOWN;
    }

    $(selector + " ." + type + "_inspector_content .data").append(
        '<tr align="left" class="' + section + 'data-row"><td>' +
        '<strong>' + label + ':</strong> ' + value +
        '</td></tr>');
};

/**
 * Adds a cross reference entry to the node or edge inspector.
 *
 * @param selector	node or edge inspector selector (div id)
 * @param type		type of the inspector (should be "node" or "edge")
 * @param href		URL of the reference
 * @param label		label to be displayed
 */
NetworkSbgnVis.prototype._addXrefEntry = function(selector, type, href, label)
{
    $(selector + " ." + type + "_inspector_content .xref-row td").append(
        '<a href="' + href + '" target="_blank">' +
        label + '</a>');
};

/**
 * Generates the URL and the display text for the given xref string.
 *
 * @param xref	xref as a string
 * @return		array of href and text pairs for the given xref
 */
NetworkSbgnVis.prototype._resolveXref = function(xref)
{
    var link = null;

    if (xref != null)
    {
        // split the string into two parts
        var pieces = xref.split(":", 2);

        // construct the link object containing href and text
        link = new Object();

        link.href = this._linkMap[pieces[0].toLowerCase()];

        if (link.href == null)
        {
            // unknown source
            link.href = "#";
        }
        // else, check where search id should be inserted
        else if (link.href.indexOf(this.ID_PLACE_HOLDER) != -1)
        {
            link.href = link.href.replace(this.ID_PLACE_HOLDER, pieces[1]);
        }
        else
        {
            link.href += pieces[1];
        }

        link.text = xref;
        link.pieces = pieces;
    }

    return link;
};

/**
 * Sets the default values of the control flags.
 */
NetworkSbgnVis.prototype._resetFlags = function()
{
    this._autoLayout = false;
    this._removeDisconnected = false;
    this._nodeLabelsVisible = true;
    this._edgeLabelsVisible = false;
    this._panZoomVisible = true;
    this._linksMerged = true;
    this._profileDataVisible = false;
    this._selectFromTab = false;
};

/**
 * Sets the visibility of the complete UI.
 *
 * @param visible	a boolean to set the visibility.
 */
NetworkSbgnVis.prototype._setVisibility = function(visible)
{
    if (visible)
    {
        if ($(this.networkTabsSelector).hasClass("hidden-network-ui"))
        //if ($("#network_menu_div").hasClass("hidden-network-ui"))
        {
            $(this.mainMenuSelector).removeClass("hidden-network-ui");
            $(this.quickInfoSelector).removeClass("hidden-network-ui");
            $(this.networkTabsSelector).removeClass("hidden-network-ui");
            $(this.nodeInspectorSelector).removeClass("hidden-network-ui");
            $(this.edgeInspectorSelector).removeClass("hidden-network-ui");
            $(this.geneLegendSelector).removeClass("hidden-network-ui");
            $(this.drugLegendSelector).removeClass("hidden-network-ui");
            $(this.edgeLegendSelector).removeClass("hidden-network-ui");
            $(this.settingsDialogSelector).removeClass("hidden-network-ui");
        }
    }
    else
    {
        if (!$(this.networkTabsSelector).hasClass("hidden-network-ui"))
        //if (!$("#network_menu_div").hasClass("hidden-network-ui"))
        {
            $(this.mainMenuSelector).addClass("hidden-network-ui");
            $(this.quickInfoSelector).addClass("hidden-network-ui");
            $(this.networkTabsSelector).addClass("hidden-network-ui");
            $(this.nodeInspectorSelector).addClass("hidden-network-ui");
            $(this.edgeInspectorSelector).addClass("hidden-network-ui");
            $(this.geneLegendSelector).addClass("hidden-network-ui");
            $(this.drugLegendSelector).addClass("hidden-network-ui");
            $(this.edgeLegendSelector).addClass("hidden-network-ui");
            $(this.settingsDialogSelector).addClass("hidden-network-ui");
        }
    }
};

/**
 * Sets visibility of the given UI component.
 *
 * @param component	an html UI component
 * @param visible	a boolean to set the visibility.
 */
NetworkSbgnVis.prototype._setComponentVis = function(component, visible)
{
    // set visible
    if (visible)
    {
        if (component.hasClass("hidden-network-ui"))
        {
            component.removeClass("hidden-network-ui");
        }
    }
    // set invisible
    else
    {
        if (!component.hasClass("hidden-network-ui"))
        {
            component.addClass("hidden-network-ui");
        }
    }
};

/**
 * Creates an array containing default option values for the ForceDirected
 * layout.
 *
 * @return	an array of default layout options
 */
NetworkSbgnVis.prototype._defaultOptsArray = function()
{
    var defaultOpts =
        [ { id: "gravitation", label: "Gravitation",       value: -350,   tip: "The gravitational constant. Negative values produce a repulsive force." },
            { id: "mass",        label: "Node mass",         value: 3,      tip: "The default mass value for nodes." },
            { id: "tension",     label: "Edge tension",      value: 0.1,    tip: "The default spring tension for edges." },
            { id: "restLength",  label: "Edge rest length",  value: "auto", tip: "The default spring rest length for edges." },
            { id: "drag",        label: "Drag co-efficient", value: 0.4,    tip: "The co-efficient for frictional drag forces." },
            { id: "minDistance", label: "Minimum distance",  value: 1,      tip: "The minimum effective distance over which forces are exerted." },
            { id: "maxDistance", label: "Maximum distance",  value: 10000,  tip: "The maximum distance over which forces are exerted." },
            { id: "iterations",  label: "Iterations",        value: 400,    tip: "The number of iterations to run the simulation." },
            { id: "maxTime",     label: "Maximum time",      value: 30000,  tip: "The maximum time to run the simulation, in milliseconds." },
            { id: "autoStabilize", label: "Auto stabilize",  value: true,   tip: "If checked, layout automatically tries to stabilize results that seems unstable after running the regular iterations." } ];

    return defaultOpts;
};

/**
 * Creates a map for xref entries.
 *
 * @return	an array (map) of xref entries
 */
NetworkSbgnVis.prototype._xrefArray = function()
{
    var linkMap = new Array();

    // TODO find missing links (Nucleotide Sequence Database)
    //linkMap["refseq"] =	"http://www.genome.jp/dbget-bin/www_bget?refseq:";
    linkMap["refseq"] = "http://www.ncbi.nlm.nih.gov/protein/";
    linkMap["entrez gene"] = "http://www.ncbi.nlm.nih.gov/gene?term=";
    linkMap["hgnc"] = "http://www.genenames.org/cgi-bin/quick_search.pl?.cgifields=type&type=equals&num=50&search=" + this.ID_PLACE_HOLDER + "&submit=Submit";
    linkMap["uniprot"] = "http://www.uniprot.org/uniprot/";
	linkMap["uniprotkb"] = "http://www.uniprot.org/uniprot/";
    //linkMap["chebi"] = "http://www.ebi.ac.uk/chebi/advancedSearchFT.do?searchString=" + this.ID_PLACE_HOLDER + "&queryBean.stars=3&queryBean.stars=-1";
	linkMap["chebi"] = "http://www.ebi.ac.uk/chebi/searchId.do?chebiId=CHEBI%3A" + this.ID_PLACE_HOLDER;
	linkMap["pubmed"] = "http://www.ncbi.nlm.nih.gov/pubmed?term=";
    linkMap["drugbank"] = "http://www.drugbank.ca/drugs/" + this.ID_PLACE_HOLDER;
	linkMap["kegg"] = "http://www.kegg.jp/dbget-bin/www_bget?dr:" + this.ID_PLACE_HOLDER;
	linkMap["kegg drug"] = "http://www.kegg.jp/dbget-bin/www_bget?dr:" + this.ID_PLACE_HOLDER;
	linkMap["chebi"] = "http://www.ebi.ac.uk/chebi/searchId.do?chebiId=CHEBI%3A" + this.ID_PLACE_HOLDER;
	linkMap["chemspider"] = "http://www.chemspider.com/Chemical-Structure." + this.ID_PLACE_HOLDER + ".html";
	linkMap["kegg compund"] = "http://www.genome.jp/dbget-bin/www_bget?cpd:" + this.ID_PLACE_HOLDER;
	linkMap["doi"] = "http://www.nature.com/nrd/journal/v10/n8/full/nrd3478.html?";
	linkMap["nci_drug"] = "http://www.cancer.gov/drugdictionary?CdrID=" + this.ID_PLACE_HOLDER;
	linkMap["national drug code directory"] = "http://www.fda.gov/Safety/MedWatch/SafetyInformation/SafetyAlertsforHumanMedicalProducts/ucm" + this.ID_PLACE_HOLDER + ".htm";
	linkMap["pharmgkb"] = "http://www.pharmgkb.org/gene/" + this.ID_PLACE_HOLDER;
	linkMap["pubchem compund"] = "http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid=" + this.ID_PLACE_HOLDER + "&loc=ec_rcs";
	linkMap["pubchem substance"] = "http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?sid=" + this.ID_PLACE_HOLDER + "&loc=ec_rss";
	linkMap["pdb"] = "http://www.rcsb.org/pdb/explore/explore.do?structureId=" + this.ID_PLACE_HOLDER;
	linkMap["bindingdb"] = "http://www.bindingdb.org/data/mols/tenK3/MolStructure_" + this.ID_PLACE_HOLDER  + ".html";
	linkMap["genbank"] = "http://www.ncbi.nlm.nih.gov/nucleotide?term=" + this.ID_PLACE_HOLDER;
	linkMap["iuphar"] = "http://www.iuphar-db.org/DATABASE/ObjectDisplayForward?objectId=" + this.ID_PLACE_HOLDER;
	linkMap["drugs product database (dpd)"] = "http://205.193.93.51/dpdonline/searchRequest.do?din=" + this.ID_PLACE_HOLDER;
	linkMap["guide to pharmacology"] = "http://www.guidetopharmacology.org/GRAC/LigandDisplayForward?ligandId=" + this.ID_PLACE_HOLDER;
	linkMap["nucleotide sequence database"] = "";

    return linkMap;
};

/**
 * Finds the non-seed gene having the maximum alteration percent in
 * the network, and returns the maximum alteration percent value.
 *
 * @param map	weight map for the genes in the network
 * @return		max alteration percent of non-seed genes
 */
NetworkSbgnVis.prototype._maxAlterValNonSeed = function(map)
{
    var max = 0.0;

    for (var key in map)
    {
        // skip seed genes

        var node = this._vis.node(key);

        if (node != null &&
            node.data["IN_QUERY"] == "true")
        {
            continue;
        }

        // update max value if necessary
        if (map[key] > max)
        {
            max = map[key];
        }
    }

    return max+1;
};

/**
 * Initializes the main menu by adjusting its style. Also, initializes the
 * inspector panels and tabs.
 */
NetworkSbgnVis.prototype._initMainMenu = function()
{
    _initMenuStyle(this.divId, this.HOVERED_CLASS);

    // adjust separators between menu items

    $(this.mainMenuSelector + " #network_menu_file").addClass(this.MENU_CLASS);
    $(this.mainMenuSelector + " #network_menu_topology").addClass(this.MENU_CLASS);
    $(this.mainMenuSelector + " #network_menu_view").addClass(this.MENU_CLASS);
    $(this.mainMenuSelector + " #network_menu_layout").addClass(this.MENU_CLASS);
    $(this.mainMenuSelector + " #network_menu_legends").addClass(this.MENU_CLASS);

    $(this.mainMenuSelector + " #save_as_png").addClass(this.FIRST_CLASS);
    $(this.mainMenuSelector + " #save_as_png").addClass(this.MENU_SEPARATOR_CLASS);
    $(this.mainMenuSelector + " #save_as_png").addClass(this.LAST_CLASS);

    $(this.mainMenuSelector + " #hide_selected").addClass(this.FIRST_CLASS);
    $(this.mainMenuSelector + " #hide_selected").addClass(this.MENU_SEPARATOR_CLASS);
    $(this.mainMenuSelector + " #remove_disconnected").addClass(this.MENU_SEPARATOR_CLASS);
    $(this.mainMenuSelector + " #remove_disconnected").addClass(this.LAST_CLASS);

    $(this.mainMenuSelector + " #show_profile_data").addClass(this.FIRST_CLASS);
    $(this.mainMenuSelector + " #show_profile_data").addClass(this.MENU_SEPARATOR_CLASS);
    $(this.mainMenuSelector + " #highlight_neighbors").addClass(this.MENU_SEPARATOR_CLASS);
    $(this.mainMenuSelector + " #remove_highlights").addClass(this.LAST_CLASS);

    $(this.mainMenuSelector + " #perform_layout").addClass(this.FIRST_CLASS);
    $(this.mainMenuSelector + " #perform_layout").addClass(this.MENU_SEPARATOR_CLASS);
    //$("#layout_properties").addClass(SUB_MENU_CLASS);
    $(this.mainMenuSelector + " #auto_layout").addClass(this.MENU_SEPARATOR_CLASS);
    $(this.mainMenuSelector + " #auto_layout").addClass(this.LAST_CLASS);

    $(this.mainMenuSelector + " #show_node_legend").addClass(this.FIRST_CLASS);
    $(this.mainMenuSelector + " #show_edge_legend").addClass(this.LAST_CLASS);

    // init check icons for checkable menu items
    this._updateMenuCheckIcons();
};

/**
 * Updates check icons of the checkable menu items.
 */
NetworkSbgnVis.prototype._updateMenuCheckIcons = function()
{
    if (this._autoLayout)
    {
        $(this.mainMenuSelector + " #auto_layout").addClass(this.CHECKED_CLASS);
    }
    else
    {
        $(this.mainMenuSelector + " #auto_layout").removeClass(this.CHECKED_CLASS);
    }

    if (this._removeDisconnected)
    {
        $(this.mainMenuSelector + " #remove_disconnected").addClass(this.CHECKED_CLASS);
    }
    else
    {
        $(this.mainMenuSelector + " #remove_disconnected").removeClass(this.CHECKED_CLASS);
    }

    if (this._nodeLabelsVisible)
    {
        $(this.mainMenuSelector + " #show_node_labels").addClass(this.CHECKED_CLASS);
    }
    else
    {
        $(this.mainMenuSelector + " #show_node_labels").removeClass(this.CHECKED_CLASS);
    }

    if (this._edgeLabelsVisible)
    {
        $(this.mainMenuSelector + " #show_edge_labels").addClass(this.CHECKED_CLASS);
    }
    else
    {
        $(this.mainMenuSelector + " #show_edge_labels").removeClass(this.CHECKED_CLASS);
    }

    if (this._panZoomVisible)
    {
        $(this.mainMenuSelector + " #show_pan_zoom_control").addClass(this.CHECKED_CLASS);
    }
    else
    {
        $(this.mainMenuSelector + " #show_pan_zoom_control").removeClass(this.CHECKED_CLASS);
    }

    if (this._linksMerged)
    {
        $(this.mainMenuSelector + " #merge_links").addClass(this.CHECKED_CLASS);
    }
    else
    {
        $(this.mainMenuSelector + " #merge_links").removeClass(this.CHECKED_CLASS);
    }

    if (this._profileDataVisible)
    {
        $(this.mainMenuSelector + " #show_profile_data").addClass(this.CHECKED_CLASS);
    }
    else
    {
        $(this.mainMenuSelector + " #show_profile_data").removeClass(this.CHECKED_CLASS);
    }
};

/**
 * Initializes dialog panels for node inspector, edge inspector, and layout
 * settings.
 */
NetworkSbgnVis.prototype._initDialogs = function()
{
    // adjust settings panel
    $(this.settingsDialogSelector).dialog({autoOpen: false,
                                     resizable: false,
                                     width: 333});

    // adjust node inspector
    $(this.nodeInspectorSelector).dialog({autoOpen: false,
                                    resizable: false,
                                    width: 366,
                                    maxHeight: 300});

    // adjust edge inspector
    $(this.edgeInspectorSelector).dialog({autoOpen: false,
                                    resizable: false,
                                    width: 366,
                                    maxHeight: 300});

    // adjust node legend
    $(this.geneLegendSelector).dialog({autoOpen: false,
                                 resizable: false,
                                 width: 440});

    // adjust drug legend
    $(this.drugLegendSelector).dialog({autoOpen: false,
                                 resizable: false,
                                 width: 320});

    // adjust edge legend
    $(this.edgeLegendSelector).dialog({autoOpen: false,
                                 resizable: false,
                                 width: 280,
                                 height: 152});
};

NetworkSbgnVis.prototype._adjustIE = function()
{
    if (_isIE())
    {
        // this is required to position scrollbar on IE
        //var width = $("#help_tab").width();
        //$("#help_tab").width(width * 1.15);
    }
};

/**
 * Initializes the layout options by default values and updates the
 * corresponding UI content.
 */
NetworkSbgnVis.prototype._initLayoutOptions = function()
{
    this._layoutOptions = this._defaultOptsArray();
    this._updateLayoutOptions();
};

/**
 * Performs the current layout on the graph.
 */
NetworkSbgnVis.prototype._performLayout = function()
{
    this._vis.layout(this._graphLayout);
};

/**
 * Toggles the visibility of the node labels.
 */
NetworkSbgnVis.prototype._toggleNodeLabels = function()
{
    // update visibility of labels

    this._nodeLabelsVisible = !this._nodeLabelsVisible;
    this._vis.nodeLabelsVisible(this._nodeLabelsVisible);

    // update check icon of the corresponding menu item

    var item = $(this.mainMenuSelector + " #show_node_labels");

    if (this._nodeLabelsVisible)
    {
        item.addClass(this.CHECKED_CLASS);
    }
    else
    {
        item.removeClass(this.CHECKED_CLASS);
    }
};

/**
 * Toggles the visibility of the edge labels.
 */
NetworkSbgnVis.prototype._toggleEdgeLabels = function()
{
    // update visibility of labels

    this._edgeLabelsVisible = !this._edgeLabelsVisible;
    this._vis.edgeLabelsVisible(this._edgeLabelsVisible);

    // update check icon of the corresponding menu item

    var item = $(this.mainMenuSelector + " #show_edge_labels");

    if (this._edgeLabelsVisible)
    {
        item.addClass(this.CHECKED_CLASS);
    }
    else
    {
        item.removeClass(this.CHECKED_CLASS);
    }
};

/**
 * Toggles the visibility of the pan/zoom control panel.
 */
NetworkSbgnVis.prototype._togglePanZoom = function()
{
    // update visibility of the pan/zoom control

    this._panZoomVisible = !this._panZoomVisible;

    this._vis.panZoomControlVisible(this._panZoomVisible);

    // update check icon of the corresponding menu item

    var item = $(this.mainMenuSelector + " #show_pan_zoom_control");

    if (this._panZoomVisible)
    {
        item.addClass(this.CHECKED_CLASS);
    }
    else
    {
        item.removeClass(this.CHECKED_CLASS);
    }
};

/**
 * Merges the edges, if not merged. If edges are already merges, then show all
 * edges.
 */
NetworkSbgnVis.prototype._toggleMerge = function()
{
    // merge/unmerge the edges

    this._linksMerged = !this._linksMerged;

    this._vis.edgesMerged(this._linksMerged);

    // update check icon of the corresponding menu item

    var item = $(this.mainMenuSelector + " #merge_links");

    if (this._linksMerged)
    {
        item.addClass(this.CHECKED_CLASS);
    }
    else
    {
        item.removeClass(this.CHECKED_CLASS);
    }
};

/**
 * Toggle auto layout option on or off. If auto layout is active, then the
 * graph is laid out automatically upon any change.
 */
NetworkSbgnVis.prototype._toggleAutoLayout = function()
{
    // toggle autoLayout option

    this._autoLayout = !this._autoLayout;

    // update check icon of the corresponding menu item

    var item = $(this.settingsDialogSelector + " #auto_layout");

    if (this._autoLayout)
    {
        item.addClass(this.CHECKED_CLASS);
    }
    else
    {
        item.removeClass(CHECKED_CLASS);
    }
};

/**
 * Toggle "remove disconnected on hide" option on or off. If this option is
 * active, then any disconnected node will also be hidden after the hide action.
 */
NetworkSbgnVis.prototype._toggleRemoveDisconnected = function()
{
    // toggle removeDisconnected option

    this._removeDisconnected = !this._removeDisconnected;

    // update check icon of the corresponding menu item

    var item = $(this.mainMenuSelector + " #remove_disconnected");

    if (this._removeDisconnected)
    {
        item.addClass(this.CHECKED_CLASS);
    }
    else
    {
        item.removeClass(this.CHECKED_CLASS);
    }
};

/**
 * Toggles the visibility of the profile data for the nodes.
 */
NetworkSbgnVis.prototype._toggleProfileData = function()
{
    // toggle value and pass to CW

    this._profileDataVisible = !this._profileDataVisible;
    this._vis.profileDataAlwaysShown(this._profileDataVisible);

    // update check icon of the corresponding menu item

    var item = $(this.mainMenuSelector + " #show_profile_data");

    if (this._profileDataVisible)
    {
        item.addClass(this.CHECKED_CLASS);
    }
    else
    {
        item.removeClass(this.CHECKED_CLASS);
    }
};

/**
 * Saves the network as a PNG image.
 */
NetworkSbgnVis.prototype._saveAsPng = function()
{
    this._vis.exportNetwork('png', 'export_network.jsp?type=png');
};

/**
 * Saves the network as a SVG image.
 */
NetworkSbgnVis.prototype._saveAsSvg = function()
{
    this._vis.exportNetwork('svg', 'export_network.jsp?type=svg');
};

/**
 * Displays the layout properties panel.
 */
NetworkSbgnVis.prototype._openProperties = function()
{
    this._updatePropsUI();
    $(this.settingsDialogSelector).dialog("open").height("auto");
};

/**
 * Initializes the layout settings panel.
 */
NetworkSbgnVis.prototype._initPropsUI = function()
{
    $(this.settingsDialogSelector + " #fd_layout_settings tr").tipTip();
};

/**
 * Updates the contents of the layout properties panel.
 */
NetworkSbgnVis.prototype._updatePropsUI = function()
{
    // update settings panel UI

    for (var i=0; i < this._layoutOptions.length; i++)
    {
//		if (_layoutOptions[i].id == "weightNorm")
//		{
//			// clean all selections
//			$("#norm_linear").removeAttr("selected");
//			$("#norm_invlinear").removeAttr("selected");
//			$("#norm_log").removeAttr("selected");
//
//			// set the correct option as selected
//
//			$("#norm_" + _layoutOptions[i].value).attr("selected", "selected");
//		}

        if (this._layoutOptions[i].id == "autoStabilize")
        {
            if (this._layoutOptions[i].value == true)
            {
                // check the box
                $(this.settingsDialogSelector + " #autoStabilize").attr("checked", true);
                $(this.settingsDialogSelector + " #autoStabilize").val(true);
            }
            else
            {
                // uncheck the box
                $(this.settingsDialogSelector + " #autoStabilize").attr("checked", false);
                $(this.settingsDialogSelector + " #autoStabilize").val(false);
            }
        }
        else
        {
            $(this.settingsDialogSelector + " #" + this._layoutOptions[i].id).val(
                this._layoutOptions[i].value);
        }
    }
};

/**
 * Updates the graphLayout options for CytoscapeWeb.
 */
NetworkSbgnVis.prototype._updateLayoutOptions = function()
{
    // update graphLayout object

    var options = new Object();

    for (var i=0; i < this._layoutOptions.length; i++)
    {
        options[this._layoutOptions[i].id] = this._layoutOptions[i].value;
    }

    this._graphLayout.options = options;
};

NetworkSbgnVis.prototype._createNodeInspector = function(divId)
{
    var id = "node_inspector_" + divId;

    var html =
        '<div id="' + id + '" class="network_node_inspector hidden-network-ui" title="Node Inspector">' +
            '<div class="node_inspector_content content ui-widget-content">' +
                '<table class="data"></table>' +
                '<table class="profile-header"></table>' +
                '<table class="profile"></table>' +
                '<table class="xref"></table>' +
            '</div>' +
        '</div>';

    $("#" + divId).append(html);

    return "#" + id;
};

NetworkSbgnVis.prototype._createEdgeInspector = function(divId)
{
    var id = "edge_inspector_" + divId;

    var html =
        '<div id="' + id + '" class="network_edge_inspector hidden-network-ui" title="Edge Inspector">' +
            '<div class="edge_inspector_content content ui-widget-content">' +
                '<table class="data"></table>' +
                '<table class="xref"></table>' +
            '</div>' +
        '</div>';

    $("#" + divId).append(html);

    return "#" + id;
};

NetworkSbgnVis.prototype._createGeneLegend = function(divId)
{
    var id = "node_legend_" + divId;

    var html =
        '<div id="' + id + '" class="network_node_legend hidden-network-ui" title="Gene Legend">' +
            '<div id="node_legend_content" class="content ui-widget-content">' +
                '<img src="images/network/gene_legend.png"/>' +
            '</div>' +
        '</div>';

    $("#" + divId).append(html);

    return "#" + id;
};

NetworkSbgnVis.prototype._createDrugLegend = function(divId)
{
    var id = "drug_legend_" + divId;

    var html =
        '<div id="' + id + '" class="network_drug_legend hidden-network-ui" title="Drug Legend">' +
            '<div id="drug_legend_content" class="content ui-widget-content">' +
                '<img src="images/network/drug_legend.png"/>' +
            '</div>' +
        '</div>';

    $("#" + divId).append(html);

    return "#" + id;
};

NetworkSbgnVis.prototype._createEdgeLegend = function(divId)
{
    var id = "edge_legend_" + divId;

    var html =
        '<div id="' + id + '" class="network_edge_legend hidden-network-ui" title="Interaction Legend">' +
            '<div id="edge_legend_content" class="content ui-widget-content">' +
                '<img src="images/network/interaction_legend.png"/>' +
            '</div>' +
        '</div>';

    $("#" + divId).append(html);

    return "#" + id;
};

NetworkSbgnVis.prototype._createSettingsDialog = function(divId)
{
    var id = "settings_dialog_" + divId;

    var html =
        '<div id="' + id + '" class="settings_dialog hidden-network-ui" title="Layout Properties">' +
            '<div id="fd_layout_settings" class="content ui-widget-content">' +
                '<table>' +
                    '<tr title="The gravitational constant. Negative values produce a repulsive force.">' +
                        '<td align="right">' +
                            '<label>Gravitation</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="gravitation" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    '<tr title="The default mass value for nodes.">' +
                        '<td align="right">' +
                            '<label>Node mass</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="mass" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    '<tr title="The default spring tension for edges.">' +
                        '<td align="right">' +
                            '<label>Edge tension</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="tension" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    '<tr title="The default spring rest length for edges.">' +
                        '<td align="right">' +
                            '<label>Edge rest length</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="restLength" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    '<tr title="The co-efficient for frictional drag forces.">' +
                        '<td align="right">' +
                            '<label>Drag co-efficient</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="drag" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    '<tr title="The minimum effective distance over which forces are exerted.">' +
                        '<td align="right">' +
                            '<label>Minimum distance</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="minDistance" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    '<tr title="The maximum distance over which forces are exerted.">' +
                        '<td align="right">' +
                            '<label>Maximum distance</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="maxDistance" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    '<tr title="The number of iterations to run the simulation.">' +
                        '<td align="right">' +
                            '<label>Iterations</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="iterations" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    '<tr title="The maximum time to run the simulation, in milliseconds.">' +
                        '<td align="right">' +
                            '<label>Maximum Time</label>' +
                        '</td>' +
                        '<td>' +
                            '<input type="text" id="maxTime" value=""/>' +
                        '</td>' +
                    '</tr>' +
                    '<tr title="If checked, layout automatically tries to stabilize results that seems unstable after running the regular iterations.">' +
                        '<td align="right">' +
                            '<label>Auto Stabilize</label>' +
                        '</td>' +
                        '<td align="left">' +
                            '<input type="checkbox" id="autoStabilize" value="true" checked="checked"/>' +
                        '</td>' +
                    '</tr>' +
                '</table>' +
            '</div>' +
            '<div class="footer">' +
                '<input type="button" id="save_layout_settings" value="Save"/>' +
                '<input type="button" id="default_layout_settings" value="Default"/>' +
            '</div>' +
        '</div>';

    $("#" + divId).append(html);

    return "#" + id;
};

/*
 * ##################################################################
 * ##################### Utility Functions ##########################
 * ##################################################################
 */

/**
 * Initializes the style of the network menu by adjusting hover behaviour.
 *
 * @param divId
 * @param hoverClass
 * @private
 */
function _initMenuStyle(divId, hoverClass)
{
    // Opera fix
    $("#" + divId + " #network_menu ul").css({display: "none"});

    // adds hover effect to main menu items (File, Topology, View)

    $("#" + divId + " #network_menu li").hover(
        function() {
            $(this).find('ul:first').css(
                {visibility: "visible",display: "none"}).show(400);
        },
        function() {
            $(this).find('ul:first').css({visibility: "hidden"});
        });


    // adds hover effect to menu items

    $("#" + divId + " #network_menu ul a").hover(
        function() {
            $(this).addClass(hoverClass);
        },
        function() {
            $(this).removeClass(hoverClass);
        });
}

/**
 * Replaces all occurrences of the given string in the source string.
 *
 * @param source		string to be modified
 * @param toFind		string to match
 * @param toReplace		string to be replaced with the matched string
 * @return				modified version of the source string
 */
function _replaceAll(source, toFind, toReplace)
{
    var target = source;
    var index = target.indexOf(toFind);

    while (index != -1)
    {
        target = target.replace(toFind, toReplace);
        index = target.indexOf(toFind);
    }

    return target;
}

/**
 * Checks if the user browser is IE.
 *
 * @return	true if IE, false otherwise
 */
function _isIE()
{
    var result = false;

    if (navigator.appName.toLowerCase().indexOf("microsoft") != -1)
    {
        result = true;
    }

    return result;
}

/**
 * Converts the given string to title case format. Also replaces each
 * underdash with a space.
 *
 * @param source	source string to be converted to title case
 */
function _toTitleCase(source)
{
    var str;

    if (source == null)
    {
        return source;
    }

    // first, trim the string
    str = source.replace(/\s+$/, "");

    // replace each underdash with a space
    str = _replaceAll(str, "_", " ");

    // change to lower case
    str = str.toLowerCase();

    // capitalize starting character of each word

    var titleCase = new Array();

    titleCase.push(str.charAt(0).toUpperCase());

    for (var i = 1; i < str.length; i++)
    {
        if (str.charAt(i-1) == ' ')
        {
            titleCase.push(str.charAt(i).toUpperCase());
        }
        else
        {
            titleCase.push(str.charAt(i));
        }
    }

    return titleCase.join("");
}

/**
 * Finds and returns the maximum value in a given map.
 *
 * @param map	map that contains real numbers
 */
function _getMaxValue(map)
{
    var max = 0.0;

    for (var key in map)
    {
        if (map[key] > max)
        {
            max = map[key];
        }
    }

    return max;
}

/**
 * Transforms the input value by using the function:
 * y = (0.000230926)x^3 - (0.0182175)x^2 + (0.511788)x
 *
 * This function is designed to transform slider input, which is between
 * 0 and 100, to provide a better filtering.
 *
 * @param value		input value to be transformed
 */
function _transformValue(value)
{
    // previous function: y = (0.000166377)x^3 - (0.0118428)x^2 + (0.520007)x

    var transformed = 0.000230926 * Math.pow(value, 3) -
                      0.0182175 * Math.pow(value, 2) +
                      0.511788 * value;

    if (transformed < 0)
    {
        transformed = 0;
    }
    else if (transformed > 100)
    {
        transformed = 100;
    }

    return transformed;
}

/**
 * Transforms the given value by solving the equation
 *
 *   y = (0.000230926)x^3 - (0.0182175)x^2 + (0.511788)x
 *
 * where y = value
 *
 * @param value	value to be reverse transformed
 * @returns		reverse transformed value
 */
function _reverseTransformValue(value)
{
    // find x, where y = value

    var reverse = _solveCubic(0.000230926,
                              -0.0182175,
                              0.511788,
                              -value);

    return reverse;
}

/**
 * Solves the cubic function
 *
 *   a(x^3) + b(x^2) + c(x) + d = 0
 *
 * by using the following formula
 *
 *   x = {q + [q^2 + (r-p^2)^3]^(1/2)}^(1/3) + {q - [q^2 + (r-p^2)^3]^(1/2)}^(1/3) + p
 *
 * where
 *
 *   p = -b/(3a), q = p^3 + (bc-3ad)/(6a^2), r = c/(3a)
 *
 * @param a	coefficient of the term x^3
 * @param b	coefficient of the term x^2
 * @param c coefficient of the term x^1
 * @param d coefficient of the term x^0
 *
 * @returns one of the roots of the cubic function
 */
function _solveCubic(a, b, c, d)
{
    var p = (-b) / (3*a);
    var q = Math.pow(p, 3) + (b*c - 3*a*d) / (6 * Math.pow(a,2));
    var r = c / (3*a);

    //alert(q*q + Math.pow(r - p*p, 3));

    var sqrt = Math.pow(q*q + Math.pow(r - p*p, 3), 1/2);

    //var root = Math.pow(q + sqrt, 1/3) +
    //	Math.pow(q - sqrt, 1/3) +
    //	p;

    var x = _cubeRoot(q + sqrt) +
            _cubeRoot(q - sqrt) +
            p;

    return x;
}

/**
 * Evaluates the cube root of the given value. This function also handles
 * negative values unlike the built-in Math.pow() function.
 *
 * @param value	source value
 * @returns		cube root of the source value
 */
function _cubeRoot(value)
{
    var root = Math.pow(Math.abs(value), 1/3);

    if (value < 0)
    {
        root = -root;
    }

    return root;
}
