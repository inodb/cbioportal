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
package org.mskcc.cbio.portal.hotspots;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.mskcc.cbio.portal.dao.DaoCancerStudy;
import org.mskcc.cbio.portal.dao.DaoException;
import org.mskcc.cbio.portal.dao.DaoGeneOptimized;
import org.mskcc.cbio.portal.model.CancerStudy;
import org.mskcc.cbio.portal.model.CanonicalGene;
import org.mskcc.cbio.portal.servlet.QueryBuilder;

/**
 *
 * @author jgao
 */
public class HotspotsServlet extends HttpServlet {
    private static Logger logger = Logger.getLogger(HotspotsServlet.class);
    public static final String MUTATION_TYPE = "type";
    public static final String PTM_TYPE = "ptm_type";
    public static final String GENES = "genes";
    public static final String THRESHOLD_SAMPLES = "threshold_samples";
    public static final String THRESHOLD_DISTANCE_PTM_MUTATION = "threshold_distance";
    public static final String THRESHOLD_DISTANCE_ERROR_CONTACT_MAP = "threshold_distance_error";
    public static final String LINEAR_HOTSPOT_WINDOW = "window";
    
    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String studyStableIdsStr = request.getParameter(QueryBuilder.CANCER_STUDY_ID);
        String type = request.getParameter(MUTATION_TYPE);
        int threshold = Integer.parseInt(request.getParameter(THRESHOLD_SAMPLES));
        String genes = request.getParameter(GENES);
        String concatEntrezGeneIds = null;
        String concatExcludeEntrezGeneIds = null;
        if (genes!=null) {
            Set<Long> entrezGeneIds = new HashSet<Long>();
            Set<Long> excludeEntrezGeneIds = new HashSet<Long>();
            DaoGeneOptimized daoGeneOptimized = DaoGeneOptimized.getInstance();
            for (String gene : genes.split("[, ]+")) {
                CanonicalGene canonicalGene = daoGeneOptimized.getGene(gene);
                if (canonicalGene!=null) {
                    entrezGeneIds.add(canonicalGene.getEntrezGeneId());
                } else if (gene.startsWith("-")) {
                    canonicalGene = daoGeneOptimized.getGene(gene.substring(1));
                    if (canonicalGene!=null) {
                        excludeEntrezGeneIds.add(canonicalGene.getEntrezGeneId());
                    }
                }
            }
            if (!entrezGeneIds.isEmpty()) {
                concatEntrezGeneIds = StringUtils.join(entrezGeneIds, ",");
            }
            if (!excludeEntrezGeneIds.isEmpty()) {
                concatExcludeEntrezGeneIds = StringUtils.join(excludeEntrezGeneIds, ",");
            }
        }
        
        Map<Hotspot,Map<Integer, Map<String,Set<String>>>> mapKeywordStudyCaseMut = Collections.emptyMap();
        Map<Integer,String> cancerStudyIdMapping = new HashMap<Integer,String>();
        String[] studyStableIds = studyStableIdsStr.split("[, ]+");
        
        try {
            StringBuilder studyIds = new StringBuilder();
            for (String stableId : studyStableIds) {
                CancerStudy study = DaoCancerStudy.getCancerStudyByStableId(stableId);
                if (study!=null) {
                    studyIds.append(study.getInternalId()).append(",");
                    cancerStudyIdMapping.put(study.getInternalId(), stableId);
                }
            }
            if (studyIds.length()>0) {
                studyIds.deleteCharAt(studyIds.length()-1);
            }
            
            if (type.startsWith("ptm-effect")) {
//                int thresholdDis = Integer.parseInt(request.getParameter(THRESHOLD_DISTANCE_PTM_MUTATION));
//                String ptmType = request.getParameter(PTM_TYPE);
//                mapKeywordStudyCaseMut = DaoMutation.getPtmEffectStatistics(
//                    studyIds.toString(), ptmType==null?null:ptmType.split("[, ]+"),
//                    thresholdDis, threshold, concatEntrezGeneIds, concatExcludeEntrezGeneIds);
//            } else if (type.equalsIgnoreCase("truncating-sep")) {
//                 mapKeywordStudyCaseMut = DaoMutation.getTruncatingMutatationStatistics(
//                    studyIds.toString(), threshold, concatEntrezGeneIds, concatExcludeEntrezGeneIds);
//            } else if (type.equalsIgnoreCase("linear")) {
//                int window = Integer.parseInt(request.getParameter(LINEAR_HOTSPOT_WINDOW));
//                mapKeywordStudyCaseMut = DaoMutation.getMutatationLinearStatistics(
//                        studyIds.toString(), window, threshold, concatEntrezGeneIds, concatExcludeEntrezGeneIds);
//            } else if (type.equalsIgnoreCase("3d")) {
//                double thresholdDisError = Double.parseDouble(request.getParameter(THRESHOLD_DISTANCE_ERROR_CONTACT_MAP));
//                mapKeywordStudyCaseMut = DaoMutation.getMutatation3DStatistics(
//                        studyIds.toString(), threshold, thresholdDisError, concatEntrezGeneIds, concatExcludeEntrezGeneIds);
//            } else if (type.equalsIgnoreCase("pdb-ptm")) {
//                mapKeywordStudyCaseMut = DaoMutation.getMutatationPdbPTMStatistics(
//                        studyIds.toString(), threshold, concatEntrezGeneIds, concatExcludeEntrezGeneIds);
            } else {
                
                mapKeywordStudyCaseMut = DaoHotspots.getSingleHotspots(
                        studyIds.toString(), type.split("[, ]+"), threshold, concatEntrezGeneIds, concatExcludeEntrezGeneIds);
            }
        } catch (DaoException ex) {
            throw new ServletException(ex);
        }
        
        // transform the data to use stable cancer study id
        Map<String,Map<String, Map<String,Set<String>>>> map =
                new HashMap<String,Map<String, Map<String,Set<String>>>>(mapKeywordStudyCaseMut.size());
        for (Map.Entry<Hotspot,Map<Integer, Map<String,Set<String>>>> entry1 : mapKeywordStudyCaseMut.entrySet()) {
            String label = entry1.getKey().getLabel();
            Map<String, Map<String,Set<String>>> map1 = new HashMap<String, Map<String,Set<String>>>(entry1.getValue().size());
            for (Map.Entry<Integer, Map<String,Set<String>>> entry2 : entry1.getValue().entrySet()) {
                map1.put(cancerStudyIdMapping.get(entry2.getKey()), entry2.getValue());
            }
            map.put(label, map1);
        }

        String format = request.getParameter("format");
        
        PrintWriter out = response.getWriter();
        try {
            if (format==null || format.equalsIgnoreCase("json")) {
                response.setContentType("application/json");

                ObjectMapper mapper = new ObjectMapper();
                out.write(mapper.writeValueAsString(map));
            } else if (format.equalsIgnoreCase("text")) {
                out.write("Alteration\t");
                out.write(StringUtils.join(studyStableIds,"\t"));
                out.write("\n");
                for (Map.Entry<String,Map<String, Map<String,Set<String>>>> entry : map.entrySet()) {
                    String keyword = entry.getKey();
                    out.write(keyword);
                    Map<String, Map<String,Set<String>>> mapStudyCaseMut = entry.getValue();
                    for (String study : studyStableIds) {
                        Map<String,Set<String>> mapCaseMut = mapStudyCaseMut.get(study);
                        out.write("\t");
                        if (mapCaseMut!=null && !mapCaseMut.isEmpty()) {
                            out.write(Integer.toString(mapCaseMut.size()));
                        }
                    }
                    out.write("\n");
                }
            }
        } finally {            
            out.close();
        }
    }
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Servlet to calculate and provide data of mutation hotspots";
    }// </editor-fold>
}
