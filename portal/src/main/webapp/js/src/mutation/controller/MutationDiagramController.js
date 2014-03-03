/**
 * Controller class for the Mutation Diagram.
 * Listens to the various events and make necessary changes
 * on the view wrt each event type.
 *
 * @author Selcuk Onur Sumer
 */
var MutationDiagramController = function(mutationDiagram, mutationTable, mutationUtil, tableView)
{
	function init()
	{
		// add listeners to the custom event dispatcher of the mutation table

		mutationTable.dispatcher.on(
			MutationDetailsEvents.MUTATION_TABLE_FILTERED,
			tableFilterHandler);

		// add listeners for the mutation table view

		// TODO make sure to call this event handler before 3D controller's handler,
		// otherwise 3D update will not work properly.
//		tableView.dispatcher.on(
//			MutationDetailsEvents.PDB_LINK_CLICKED,
//			pdbLinkHandler);
	}

	function tableFilterHandler(tableSelector)
	{
		var mutationMap = mutationUtil.getMutationIdMap();
		var currentMutations = [];

		// add current mutations into an array
		var rows = tableSelector.find("tr");
		_.each(rows, function(element, index) {
			var mutationId = $(element).attr("id");

			if (mutationId)
			{
				var mutation = mutationMap[mutationId];

				if (mutation)
				{
					currentMutations.push(mutation);
				}
			}
		});

		// update mutation diagram with the current mutations
		if (mutationDiagram !== null)
		{
			var mutationData = new MutationCollection(currentMutations);
			mutationDiagram.updatePlot(mutationData);
		}
	}

	function pdbLinkHandler(mutationId)
	{
		var mutationMap = mutationUtil.getMutationIdMap();
		var mutation = mutationMap[mutationId];

		if (mutation)
		{
			// highlight the corresponding pileup (without filtering the table)
			mutationDiagram.clearHighlights();
			mutationDiagram.highlightMutation(mutation.mutationSid);
		}
	}

	init();
};
