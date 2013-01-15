/** Copyright (c) 2012 Memorial Sloan-Kettering Cancer Center.
**
** This library is free software; you can redistribute it and/or modify it
** under the terms of the GNU Lesser General Public License as published
** by the Free Software Foundation; either version 2.1 of the License, or
** any later version.
**
** This library is distributed in the hope that it will be useful, but
** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
** documentation provided hereunder is on an "as is" basis, and
** Memorial Sloan-Kettering Cancer Center 
** has no obligations to provide maintenance, support,
** updates, enhancements or modifications.  In no event shall
** Memorial Sloan-Kettering Cancer Center
** be liable to any party for direct, indirect, special,
** incidental or consequential damages, including lost profits, arising
** out of the use of this software and its documentation, even if
** Memorial Sloan-Kettering Cancer Center 
** has been advised of the possibility of such damage.  See
** the GNU Lesser General Public License for more details.
**
** You should have received a copy of the GNU Lesser General Public License
** along with this library; if not, write to the Free Software Foundation,
** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
**/

package org.mskcc.cbio.portal.servlet;

import java.io.IOException;

import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.*;


import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.mskcc.cbio.cgds.model.ClinicalData;
import org.mskcc.cbio.portal.model.*;
import org.mskcc.cbio.portal.oncoPrintSpecLanguage.ParserOutput;
import org.mskcc.cbio.portal.remote.*;
import org.mskcc.cbio.portal.util.*;
import org.mskcc.cbio.portal.r_bridge.SurvivalPlot;
import org.mskcc.cbio.cgds.validate.gene.GeneValidator;
import org.mskcc.cbio.cgds.validate.gene.GeneValidationException;
import org.mskcc.cbio.cgds.model.CancerStudy;
import org.mskcc.cbio.cgds.model.CaseList;
import org.mskcc.cbio.cgds.model.GeneticProfile;
import org.mskcc.cbio.cgds.model.GeneticAlterationType;
import org.mskcc.cbio.cgds.model.ExtendedMutation;
import org.mskcc.cbio.cgds.dao.DaoException;
import org.mskcc.cbio.cgds.web_api.GetProfileData;
import org.mskcc.cbio.cgds.web_api.ProtocolException;
import org.mskcc.cbio.cgds.util.AccessControl;
import org.owasp.validator.html.PolicyException;

/**
 * Central Servlet for building queries.
 */
public class QueryBuilder extends HttpServlet {
    public static final String CLIENT_TRANSPOSE_MATRIX = "transpose_matrix";
    public static final String CANCER_TYPES_INTERNAL = "cancer_types";
    public static final String PROFILE_LIST_INTERNAL = "profile_list";
    public static final String CASE_SETS_INTERNAL = "case_sets";
    public static final String CANCER_STUDY_ID = "cancer_study_id";
    public static final String CLINICAL_DATA_LIST = "clinical_data_list";
    public static final String GENETIC_PROFILE_IDS = "genetic_profile_ids";
    public static final String GENE_SET_CHOICE = "gene_set_choice";
    public static final String CASE_SET_ID = "case_set_id";
    public static final String CASE_IDS = "case_ids";
    public static final String CASE_IDS_KEY = "case_ids_key";
    public static final String SET_OF_CASE_IDS = "set_of_case_ids";
    public static final String CLINICAL_PARAM_SELECTION = "clinical_param_selection";
    public static final String GENE_LIST = "gene_list";
    public static final String RAW_GENE_STR = "raw_gene_str";
    public static final String ACTION_NAME = "Action";
    public static final String OUTPUT = "output";
    public static final String FORMAT = "format";
    public static final String PLOT_TYPE = "plot_type";
    public static final String OS_SURVIVAL_PLOT = "os_survival_plot";
    public static final String DFS_SURVIVAL_PLOT = "dfs_survival_plot";
    public static final String XDEBUG = "xdebug";
    public static final String ACTION_SUBMIT = "Submit";
    public static final String STEP1_ERROR_MSG = "step1_error_msg";
    public static final String STEP2_ERROR_MSG = "step2_error_msg";
    public static final String STEP3_ERROR_MSG = "step3_error_msg";
    public static final String STEP4_ERROR_MSG = "step4_error_msg";
    public static final String MERGED_PROFILE_DATA_INTERNAL = "merged_profile_data";
    public static final String PROFILE_DATA_SUMMARY = "profile_data_summary";
    public static final String WARNING_UNION = "warning_union";
    public static final String DOWNLOAD_LINKS = "download_links";
    public static final String NETWORK = "network";
    public static final String HTML_TITLE = "html_title";
    public static final String TAB_INDEX = "tab_index";
    public static final String TAB_DOWNLOAD = "tab_download";
    public static final String TAB_VISUALIZE = "tab_visualize";
    public static final String USER_ERROR_MESSAGE = "user_error_message";
    public static final String ATTRIBUTE_URL_BEFORE_FORWARDING = "ATTRIBUTE_URL_BEFORE_FORWARDING";
    public static final String Z_SCORE_THRESHOLD = "Z_SCORE_THRESHOLD";
    public static final String RPPA_SCORE_THRESHOLD = "RPPA_SCORE_THRESHOLD";
    public static final String MRNA_PROFILES_SELECTED = "MRNA_PROFILES_SELECTED";
    public static final String COMPUTE_LOG_ODDS_RATIO = "COMPUTE_LOG_ODDS_RATIO";
    public static final int MUTATION_DETAIL_LIMIT = 20;
    public static final String MUTATION_DETAIL_LIMIT_REACHED = "MUTATION_DETAIL_LIMIT_REACHED";
    public static final String XDEBUG_OBJECT = "xdebug_object";
    public static final String ONCO_PRINT_HTML = "oncoprint_html";
    public static final String INDEX_PAGE = "index.do";
    public static final String INTERNAL_EXTENDED_MUTATION_LIST = "INTERNAL_EXTENDED_MUTATION_LIST";
    public static final String DATA_PRIORITY = "data_priority";

    private ServletXssUtil servletXssUtil;

	// class which process access control to cancer studies
	private AccessControl accessControl;

    /**
     * Initializes the servlet.
     *
     * @throws ServletException Serlvet Init Error.
     */
    public void init() throws ServletException {
        super.init();
        try {
            servletXssUtil = ServletXssUtil.getInstance();
			ApplicationContext context = 
				new ClassPathXmlApplicationContext("classpath:applicationContext-security.xml");
			accessControl = (AccessControl)context.getBean("accessControl");
        } catch (PolicyException e) {
            throw new ServletException (e);
        }
    }

    /**
     * Handles HTTP GET Request.
     *
     * @param httpServletRequest  Http Servlet Request Object.
     * @param httpServletResponse Http Servelt Response Object.
     * @throws ServletException Servlet Error.
     * @throws IOException      IO Error.
     */
    protected void doGet(HttpServletRequest httpServletRequest,
                         HttpServletResponse httpServletResponse) throws ServletException,
            IOException {
        doPost(httpServletRequest, httpServletResponse);
    }

    /**
     * Handles HTTP POST Request.
     *
     * @param httpServletRequest  Http Servlet Request Object.
     * @param httpServletResponse Http Servelt Response Object.
     * @throws ServletException Servlet Error.
     * @throws IOException      IO Error.
     */
    protected void doPost(HttpServletRequest httpServletRequest,
                          HttpServletResponse httpServletResponse) throws ServletException,
            IOException {
        XDebug xdebug = new XDebug( httpServletRequest );
        xdebug.startTimer();

        xdebug.logMsg(this, "Attempting to initiate new user query.");
        
        if (httpServletRequest.getRequestURL() != null) {
            httpServletRequest.setAttribute(ATTRIBUTE_URL_BEFORE_FORWARDING,
                                            httpServletRequest.getRequestURL().toString());
        }

        //  Get User Selected Action
        String action = servletXssUtil.getCleanInput (httpServletRequest, ACTION_NAME);

        //  Get User Selected Cancer Type
        String cancerTypeId = servletXssUtil.getCleanInput(httpServletRequest, CANCER_STUDY_ID);

        //  Get User Selected Genetic Profiles
        HashSet<String> geneticProfileIdSet = getGeneticProfileIds(httpServletRequest, xdebug);

        //  Get User Defined Gene List
        String geneList = servletXssUtil.getCleanInput (httpServletRequest, GENE_LIST);

        // save the raw gene string as it was entered for other things to work on
        httpServletRequest.setAttribute(RAW_GENE_STR, geneList);

        xdebug.logMsg(this, "Gene List is set to:  " + geneList);

        //  Get all Cancer Types
        try {
			List<CancerStudy> cancerStudyList = accessControl.getCancerStudies();

            if (cancerTypeId == null) {
                cancerTypeId = cancerStudyList.get(0).getCancerStudyStableId();
            }
            
            httpServletRequest.setAttribute(CANCER_STUDY_ID, cancerTypeId);
            httpServletRequest.setAttribute(CANCER_TYPES_INTERNAL, cancerStudyList);

            //  Get Genetic Profiles for Selected Cancer Type
            ArrayList<GeneticProfile> profileList = GetGeneticProfiles.getGeneticProfiles
                (cancerTypeId);
            httpServletRequest.setAttribute(PROFILE_LIST_INTERNAL, profileList);

            //  Get Case Sets for Selected Cancer Type
            xdebug.logMsg(this, "Using Cancer Study ID:  " + cancerTypeId);
            ArrayList<CaseList> caseSets = GetCaseSets.getCaseSets(cancerTypeId);
            xdebug.logMsg(this, "Total Number of Case Sets:  " + caseSets.size());
            CaseList caseSet = new CaseList();
            caseSet.setName("User-defined Case List");
            caseSet.setDescription("User defined case list.");
            caseSet.setStableId("-1");
            caseSets.add(caseSet);
            httpServletRequest.setAttribute(CASE_SETS_INTERNAL, caseSets);

            //  Get User Selected Case Set
            String caseSetId = servletXssUtil.getCleanInput(httpServletRequest, CASE_SET_ID);
            if (caseSetId != null) {
                httpServletRequest.setAttribute(CASE_SET_ID, caseSetId);
            } else {
                if (caseSets.size() > 0) {
                    CaseList zeroSet = caseSets.get(0);
                    httpServletRequest.setAttribute(CASE_SET_ID, zeroSet.getStableId());
                }
            }
            String caseIds = servletXssUtil.getCleanInput(httpServletRequest, CASE_IDS);

            httpServletRequest.setAttribute(XDEBUG_OBJECT, xdebug);

            boolean errorsExist = validateForm(action, profileList, geneticProfileIdSet, geneList,
                                               caseSetId, caseIds, httpServletRequest);
            if (action != null && action.equals(ACTION_SUBMIT) && (!errorsExist)) {

                processData(cancerTypeId, geneticProfileIdSet, profileList, geneList, caseSetId,
                            caseIds, caseSets, getServletContext(), httpServletRequest,
                            httpServletResponse, xdebug);
            } else {
                if (errorsExist) {
                   httpServletRequest.setAttribute(QueryBuilder.USER_ERROR_MESSAGE,
                           "Please fix the errors below.");
                }
                RequestDispatcher dispatcher =
                    getServletContext().getRequestDispatcher("/WEB-INF/jsp/index.jsp");
                dispatcher.forward(httpServletRequest, httpServletResponse);
            }
        } catch (RemoteException e) {
            xdebug.logMsg(this, "Got Remote Exception:  " + e.getMessage());
            forwardToErrorPage(httpServletRequest, httpServletResponse,
                               "An error occurred while trying to connect to the database.", xdebug);
        } catch (DaoException e) {
            xdebug.logMsg(this, "Got Database Exception:  " + e.getMessage());
            forwardToErrorPage(httpServletRequest, httpServletResponse,
                               "An error occurred while trying to connect to the database.", xdebug);
        } catch (ProtocolException e) {
            xdebug.logMsg(this, "Got Protocol Exception:  " + e.getMessage());
            forwardToErrorPage(httpServletRequest, httpServletResponse,
                               "An error occurred while trying to connect to the database.", xdebug);
        }
    }

    /**
     * Gets all Genetic Profile IDs.
     *
     * These values are passed with parameter names like this:
     *
     * genetic_profile_ids
     * genetic_profile_ids_MUTATION
     * genetic_profile_ids_MUTATION_EXTENDED
     * genetic_profile_ids_COPY_NUMBER_ALTERATION
     * genetic_profile_ids_MRNA_EXPRESSION
     *
     *
     * @param httpServletRequest HTTPServlet Request.
     * @return HashSet of GeneticProfileIDs.
     */
    private HashSet<String> getGeneticProfileIds(HttpServletRequest httpServletRequest,
        XDebug xdebug) {
        HashSet<String> geneticProfileIdSet = new HashSet<String>();
        Enumeration nameEnumeration = httpServletRequest.getParameterNames();
        while (nameEnumeration.hasMoreElements()) {
            String currentName = (String) nameEnumeration.nextElement();
            if (currentName.startsWith(GENETIC_PROFILE_IDS)) {
                String geneticProfileIds[] = httpServletRequest.getParameterValues(currentName);
                if (geneticProfileIds != null && geneticProfileIds.length > 0) {
                    for (String geneticProfileIdDirty : geneticProfileIds) {
                        String geneticProfileIdClean = servletXssUtil.getCleanInput(geneticProfileIdDirty);
                        xdebug.logMsg (this, "Received Genetic Profile ID:  "
                                + currentName + ":  " + geneticProfileIdClean);
                        geneticProfileIdSet.add(geneticProfileIdClean);
                    }
                }
            }
        }
        httpServletRequest.setAttribute(GENETIC_PROFILE_IDS, geneticProfileIdSet);
        return geneticProfileIdSet;
    }

    /**
     * process a good request
     * 
    */
    private void processData(String cancerTypeId,
							 HashSet<String> geneticProfileIdSet,
							 ArrayList<GeneticProfile> profileList,
							 String geneListStr,
							 String caseSetId, String caseIds,
							 ArrayList<CaseList> caseSetList,
							 ServletContext servletContext, HttpServletRequest request,
							 HttpServletResponse response,
							 XDebug xdebug) throws IOException, ServletException, DaoException {

       // parse geneList, written in the OncoPrintSpec language (except for changes by XSS clean)
       double zScore = ZScoreUtil.getZScore(geneticProfileIdSet, profileList, request);
       double rppaScore = ZScoreUtil.getRPPAScore(request);
       
       ParserOutput theOncoPrintSpecParserOutput =
               OncoPrintSpecificationDriver.callOncoPrintSpecParserDriver( geneListStr,
                geneticProfileIdSet, profileList, zScore, rppaScore );
       
        ArrayList<String> geneList = new ArrayList<String>();
        geneList.addAll( theOncoPrintSpecParserOutput.getTheOncoPrintSpecification().listOfGenes());
        ArrayList<String> tempGeneList = new ArrayList<String>();
        for (String gene : geneList){
            tempGeneList.add(gene);
        }
        geneList = tempGeneList;
        request.setAttribute(GENE_LIST, geneList);

        xdebug.logMsg(this, "Using gene list geneList.toString():  " + geneList.toString());
        
        HashSet<String> setOfCaseIds = null;
        
        String caseIdsKey = null;
        
        // user-specified cases, but case_ids parameter is missing,
        // so try to retrieve case_ids by using case_ids_key parameter.
        // this is required for survival plot requests  
        if (caseSetId.equals("-1") &&
        	caseIds == null)
        {
        	caseIdsKey = servletXssUtil.getCleanInput(request, CASE_IDS_KEY);
        	
        	if (caseIdsKey != null)
        	{
        		caseIds = CaseSetUtil.getCaseIds(caseIdsKey);
        	}
        }
        
        if (!caseSetId.equals("-1"))
        {
            for (CaseList caseSet : caseSetList) {
                if (caseSet.getStableId().equals(caseSetId)) {
                    caseIds = caseSet.getCaseListAsString();
                    setOfCaseIds = new HashSet<String>(caseSet.getCaseList());
                    caseSet.getCaseList();
                }
            }
        }
        //if user specifies cases, add these to hashset, and send to GetMutationData
        else if (caseIds != null)
        {
            String[] caseIdSplit = caseIds.split("\\s+");
            setOfCaseIds = new HashSet<String>();
            
            for (String caseID : caseIdSplit){
                if (null != caseID){
                   setOfCaseIds.add(caseID);
                }
            }
        }
        
        request.setAttribute(SET_OF_CASE_IDS, caseIds);

        if (caseIdsKey == null)
        {
        	caseIdsKey = CaseSetUtil.shortenCaseIds(caseIds);
        }
        
        // this will create a key even if the case set is a predefined set,
        // because it is required to build a case id string in any case
        request.setAttribute(CASE_IDS_KEY, caseIdsKey);

        Iterator<String> profileIterator = geneticProfileIdSet.iterator();
        ArrayList<ProfileData> profileDataList = new ArrayList<ProfileData>();

        Set<String> warningUnion = new HashSet<String>();
        ArrayList<DownloadLink> downloadLinkSet = new ArrayList<DownloadLink>();
        ArrayList<ExtendedMutation> mutationList = new ArrayList<ExtendedMutation>();

        while (profileIterator.hasNext()) {
            String profileId = profileIterator.next();
            GeneticProfile profile = GeneticProfileUtil.getProfile(profileId, profileList);
            if( null == profile ){
               continue;
            }
            xdebug.logMsg(this, "Getting data for:  " + profile.getProfileName());
            GetProfileData remoteCall = new GetProfileData(profile, geneList, caseIds);
            ProfileData pData = remoteCall.getProfileData();
            DownloadLink downloadLink = new DownloadLink(profile, geneList, caseIds,
                    remoteCall.getRawContent());
            downloadLinkSet.add(downloadLink);
            warningUnion.addAll(remoteCall.getWarnings());
            if( pData == null ){
               System.err.println( "pData == null" );
            }else{
               if( pData.getGeneList() == null ){
                  System.err.println( "pData.getValidGeneList() == null" );
               }
            }
            if (pData != null) {
                xdebug.logMsg(this, "Got number of genes:  " + pData.getGeneList().size());
                xdebug.logMsg(this, "Got number of cases:  " + pData.getCaseIdList().size());
            }
            xdebug.logMsg(this, "Number of warnings received:  " + remoteCall.getWarnings().size());
            profileDataList.add(pData);

            //  Optionally, get Extended Mutation Data.
            if (profile.getGeneticAlterationType().equals
                    (GeneticAlterationType.MUTATION_EXTENDED)) {
                if (geneList.size() <= MUTATION_DETAIL_LIMIT) {
                    xdebug.logMsg(this, "Number genes requested is <= " + MUTATION_DETAIL_LIMIT);
                    xdebug.logMsg(this, "Therefore, getting extended mutation data");
                    GetMutationData remoteCallMutation = new GetMutationData();
                    ArrayList<ExtendedMutation> tempMutationList =
                            remoteCallMutation.getMutationData(profile,
                                    geneList, setOfCaseIds, xdebug);
                    if (tempMutationList != null && tempMutationList.size() > 0) {
                        xdebug.logMsg(this, "Total number of mutation records retrieved:  "
                            + tempMutationList.size());
                        mutationList.addAll(tempMutationList);
                    }
                } else {
                    request.setAttribute(MUTATION_DETAIL_LIMIT_REACHED, Boolean.TRUE);
                }
            }
        }
        
        //  Store Extended Mutations
        request.setAttribute(INTERNAL_EXTENDED_MUTATION_LIST, mutationList);

        // Store download links in session (for possible future retrieval).
        request.getSession().setAttribute(DOWNLOAD_LINKS, downloadLinkSet);

        String tabIndex = servletXssUtil.getCleanInput(request, QueryBuilder.TAB_INDEX);
        if (tabIndex != null && tabIndex.equals(QueryBuilder.TAB_VISUALIZE)) {
            xdebug.logMsg(this, "Merging Profile Data");
            ProfileMerger merger = new ProfileMerger(profileDataList);
            ProfileData mergedProfile = merger.getMergedProfile();

            //  Get Clinical Data
            xdebug.logMsg(this, "Getting Clinical Data:");
            ArrayList <ClinicalData> clinicalDataList =
                    GetClinicalData.getClinicalData(setOfCaseIds);
            xdebug.logMsg(this, "Got Clinical Data for:  " + clinicalDataList.size()
                +  " cases.");
            request.setAttribute(CLINICAL_DATA_LIST, clinicalDataList);

            xdebug.logMsg(this, "Merged Profile, Number of genes:  "
                    + mergedProfile.getGeneList().size());
            ArrayList<String> mergedProfileGeneList = mergedProfile.getGeneList();
            for (String currentGene:  mergedProfileGeneList) {
                xdebug.logMsg(this, "Merged Profile Gene:  " + currentGene);
            }
            xdebug.logMsg(this, "Merged Profile, Number of cases:  "
                    + mergedProfile.getCaseIdList().size());
            request.setAttribute(MERGED_PROFILE_DATA_INTERNAL, mergedProfile);
            request.setAttribute(WARNING_UNION, warningUnion);

            String output = servletXssUtil.getCleanInput(request, OUTPUT);
            String format = servletXssUtil.getCleanInput(request, FORMAT);
            double zScoreThreshold = ZScoreUtil.getZScore(geneticProfileIdSet, profileList, request);
            double rppaScoreThreshold = ZScoreUtil.getRPPAScore(request);
            request.setAttribute(Z_SCORE_THRESHOLD, zScoreThreshold);
            request.setAttribute(RPPA_SCORE_THRESHOLD, rppaScoreThreshold);

            if (output != null) {
				if (output.equals("text")) {
                    outputPlainText(response, mergedProfile, theOncoPrintSpecParserOutput,
                            zScoreThreshold, rppaScoreThreshold);
                } else if (output.equals(OS_SURVIVAL_PLOT)) {
                    outputOsSurvivalPlot(mergedProfile, theOncoPrintSpecParserOutput,
                            zScoreThreshold, rppaScoreThreshold, clinicalDataList, format, response);
                } else if (output.equals(DFS_SURVIVAL_PLOT)) {
                    outputDfsSurvivalPlot(mergedProfile, theOncoPrintSpecParserOutput,
                            zScoreThreshold, rppaScoreThreshold, clinicalDataList, format, response);
				// (via LinkOut servlet - report=oncoprint_html arg)
                }
            } else {

                // Store download links in session (for possible future retrieval).
                request.getSession().setAttribute(DOWNLOAD_LINKS, downloadLinkSet);
                RequestDispatcher dispatcher =
                        getServletContext().getRequestDispatcher("/WEB-INF/jsp/visualize.jsp");
                dispatcher.forward(request, response);
            }
        } else {
            ShowData.showDataAtSpecifiedIndex(servletContext, request,
                    response, 0, xdebug);
        }
    }

    private void outputDfsSurvivalPlot(ProfileData mergedProfile,
            ParserOutput theOncoPrintSpecParserOutput, double zScoreThreshold, double rppaScoreThreshold,
            ArrayList<ClinicalData> clinicalDataList, String format,
            HttpServletResponse response) throws IOException {
        ProfileDataSummary dataSummary = new ProfileDataSummary( mergedProfile,
                theOncoPrintSpecParserOutput.getTheOncoPrintSpecification(), zScoreThreshold, rppaScoreThreshold );
        SurvivalPlot survivalPlot = new SurvivalPlot(SurvivalPlot.SurvivalPlotType.DFS,
                clinicalDataList, dataSummary, format, response);
    }

    private void outputOsSurvivalPlot(ProfileData mergedProfile,
            ParserOutput theOncoPrintSpecParserOutput, double zScoreThreshold, double rppaScoreThreshold,
            ArrayList<ClinicalData> clinicalDataList, String format,
            HttpServletResponse response) throws IOException {
        ProfileDataSummary dataSummary = new ProfileDataSummary( mergedProfile,
                theOncoPrintSpecParserOutput.getTheOncoPrintSpecification(), zScoreThreshold, rppaScoreThreshold );
        SurvivalPlot survivalPlot = new SurvivalPlot(SurvivalPlot.SurvivalPlotType.OS,
                clinicalDataList, dataSummary, format, response);
    }

    private void outputPlainText(HttpServletResponse response, ProfileData mergedProfile,
            ParserOutput theOncoPrintSpecParserOutput, double zScoreThreshold, double rppaScoreThreshold) throws IOException {
        response.setContentType("text/plain");
        ProfileDataSummary dataSummary = new ProfileDataSummary( mergedProfile,
                theOncoPrintSpecParserOutput.getTheOncoPrintSpecification(), zScoreThreshold, rppaScoreThreshold );
        PrintWriter writer = response.getWriter();
        writer.write("" + dataSummary.getPercentCasesAffected());
        writer.flush();
        writer.close();
    }

    /**
     * validate the portal web input form.
     */
    private boolean validateForm(String action,
                                ArrayList<GeneticProfile> profileList,
                                 HashSet<String> geneticProfileIdSet,
                                 String geneList, String caseSetId, String caseIds,
                                 HttpServletRequest httpServletRequest) throws DaoException {
        boolean errorsExist = false;
        String tabIndex = servletXssUtil.getCleanInput(httpServletRequest, QueryBuilder.TAB_INDEX);
        if (action != null) {
            if (action.equals(ACTION_SUBMIT)) {
				// is user authorized for the study
				String cancerStudyIdentifier = (String)httpServletRequest.getAttribute(CANCER_STUDY_ID);
				if (accessControl.isAccessibleCancerStudy(cancerStudyIdentifier).size() != 1) {
                    httpServletRequest.setAttribute(STEP1_ERROR_MSG,
													"You are not authorized to view the cancer study with id: '" +
													cancerStudyIdentifier + "'. ");
					errorsExist = true;
				}
						
                if (geneticProfileIdSet.size() == 0) {
                    if (tabIndex == null || tabIndex.equals(QueryBuilder.TAB_DOWNLOAD)) {
                        httpServletRequest.setAttribute(STEP2_ERROR_MSG,
                                "Please select a genetic profile below. ");
                    } else {
                        httpServletRequest.setAttribute(STEP2_ERROR_MSG,
                                "Please select one or more genetic profiles below. ");
                    }
                    errorsExist = true;
                }
                
                // user-defined case set
                if (caseIds != null &&
                	caseSetId != null &&
                	caseSetId.equals("-1"))
                {
                	// empty case list
                	if (caseIds.trim().length() == 0)
                	{
                		httpServletRequest.setAttribute(STEP3_ERROR_MSG,
                				"Please enter at least one case ID below. ");
                		
                		errorsExist = true;
                	}
                	else
                	{
                		List<String> invalidCases = CaseSetUtil.validateCaseSet(
                				cancerStudyIdentifier, caseIds);
                		
                		String caseSetErrMsg = "Invalid case(s) for the selected cancer study:";
                		
                		// non-empty list, but contains invalid case IDs
                		if (invalidCases.size() > 0)
                		{
                			// append case ids to the message
                    		for (String caseId : invalidCases)
                    		{
                    			caseSetErrMsg += " " + caseId;
                    		}
                    		
                			httpServletRequest.setAttribute(STEP3_ERROR_MSG,
                					caseSetErrMsg);
                    		
                    		errorsExist = true;
                		}
                	}
                }

                errorsExist = validateGenes(geneList, httpServletRequest, errorsExist);

                //  Additional validation rules
                //  If we have selected mRNA Expression Data Check Box, but failed to
                //  select an mRNA profile, this is an error.
                String mRNAProfileSelected = servletXssUtil.getCleanInput(httpServletRequest,
                        QueryBuilder.MRNA_PROFILES_SELECTED);
                if (mRNAProfileSelected != null && mRNAProfileSelected.equalsIgnoreCase("on")) {

                    //  Make sure that at least one of the mRNA profiles is selected
                    boolean mRNAProfileRadioSelected = false;
                    for (int i = 0; i < profileList.size(); i++) {
                        GeneticProfile geneticProfile = profileList.get(i);
                        if (geneticProfile.getGeneticAlterationType()
                                == GeneticAlterationType.MRNA_EXPRESSION
                                && geneticProfileIdSet.contains(geneticProfile.getStableId())) {
                            mRNAProfileRadioSelected = true;
                        }
                    }
                    if (mRNAProfileRadioSelected == false) {
                        httpServletRequest.setAttribute(STEP2_ERROR_MSG,
                                "Please select an mRNA profile.");
                        errorsExist = true;
                    }
                }
            }
        }
        if( errorsExist ){
           httpServletRequest.setAttribute( GENE_LIST, geneList );
       }
        return errorsExist;
    }

    private boolean validateGenes(String geneList, HttpServletRequest httpServletRequest,
            boolean errorsExist) throws DaoException {
        try {
            GeneValidator geneValidator = new GeneValidator(geneList);
        } catch (GeneValidationException e) {
            errorsExist = true;
            httpServletRequest.setAttribute(STEP4_ERROR_MSG, e.getMessage());
        }
        return errorsExist;
    }
    
    private void forwardToErrorPage(HttpServletRequest request, HttpServletResponse response,
                                    String userMessage, XDebug xdebug)
            throws ServletException, IOException {
        request.setAttribute("xdebug_object", xdebug);
        request.setAttribute(USER_ERROR_MESSAGE, userMessage);
        RequestDispatcher dispatcher =
                getServletContext().getRequestDispatcher("/WEB-INF/jsp/error.jsp");
        dispatcher.forward(request, response);
    }
}
