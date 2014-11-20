/** Copyright (c) 2012 Memorial Sloan-Kettering Cancer Center.
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
 * has been advised of the possibility of such damage.
*/

package org.mskcc.cbio.portal.scripts;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;
import org.mskcc.cbio.portal.dao.DaoCancerStudy;
import org.mskcc.cbio.portal.dao.DaoCaseList;
import org.mskcc.cbio.portal.model.CancerStudy;
import org.mskcc.cbio.portal.model.CaseList;
import org.mskcc.cbio.portal.model.CaseListCategory;
import org.mskcc.cbio.portal.util.ConsoleUtil;
import org.mskcc.cbio.portal.util.ProgressMonitor;

/**
 * Command Line tool to Import Case Lists.
 */
public class ImportCaseList {

   public static void importCaseList(File dataFile, ProgressMonitor pMonitor) throws Exception {
      pMonitor.setCurrentMessage("Read data from:  " + dataFile.getAbsolutePath());
      Properties properties = new Properties();
      properties.load(new FileInputStream(dataFile));

      String stableId = properties.getProperty("stable_id").trim();

      if (stableId.contains(" ")) {
         throw new IllegalArgumentException("stable_id cannot contain spaces:  " + stableId);
      }

      if (stableId == null || stableId.length() == 0) {
         throw new IllegalArgumentException("stable_id is not specified.");
      }

      String cancerStudyIdentifier = properties.getProperty("cancer_study_identifier");
      if (cancerStudyIdentifier == null) {
         throw new IllegalArgumentException("cancer_study_identifier is not specified.");
      }
      CancerStudy theCancerStudy = DaoCancerStudy.getCancerStudyByStableId(cancerStudyIdentifier);
      if (theCancerStudy == null) {
         throw new IllegalArgumentException("cancer study identified by cancer_study_identifier '"
                  + cancerStudyIdentifier + "' not found in dbms or inaccessible to user.");
      }

      String caseListName = properties.getProperty("case_list_name");
       
      String caseListCategoryStr = properties.getProperty("case_list_category");
      if (caseListCategoryStr  == null || caseListCategoryStr.length() == 0) {
          caseListCategoryStr = "other";
      }
      CaseListCategory caseListCategory = CaseListCategory.get(caseListCategoryStr); 
       
      String caseListDescription = properties.getProperty("case_list_description");
      String caseListStr = properties.getProperty("case_list_ids");
      if (caseListName == null) {
         throw new IllegalArgumentException("case_list_name is not specified.");
      } else if (caseListDescription == null) {
         throw new IllegalArgumentException("case_list_description is not specified.");
      }

      // construct case id list
      ArrayList<String> caseIDsList = new ArrayList<String>();
      String[] caseIds = caseListStr.split("\t");
      for (String caseId : caseIds) {
         caseIDsList.add(caseId);
      }

      CaseList caseList = DaoCaseList.getCaseListByStableId(stableId);
      if (caseList != null) {
         throw new IllegalArgumentException("Case list with this stable Id already exists:  " + stableId);
      }

      caseList = new CaseList();
      caseList.setStableId(stableId);
      int cancerStudyId = theCancerStudy.getInternalId();
      caseList.setCancerStudyId(cancerStudyId);
      caseList.setCaseListCategory(caseListCategory);
      caseList.setName(caseListName);
      caseList.setDescription(caseListDescription);
      caseList.setCaseList(caseIDsList);
      DaoCaseList.addCaseList(caseList);

      caseList = DaoCaseList.getCaseListByStableId(stableId);

      pMonitor.setCurrentMessage(" --> stable ID:  " + caseList.getStableId());
      pMonitor.setCurrentMessage(" --> case list name:  " + caseList.getName());
      pMonitor.setCurrentMessage(" --> number of cases:  " + caseIDsList.size());
   }

   public static void main(String[] args) throws Exception {

      // check args
      if (args.length < 1) {
         System.out.println("command line usage:  importCaseListData.pl " + "<data_file.txt or directory>");
            return;
      }
      ProgressMonitor pMonitor = new ProgressMonitor();
      pMonitor.setConsoleMode(true);
      File dataFile = new File(args[0]);
      if (dataFile.isDirectory()) {
         File files[] = dataFile.listFiles();
         for (File file : files) {
            if (file.getName().endsWith("txt")) {
               ImportCaseList.importCaseList(file, pMonitor);
            }
         }
         if (files.length == 0) {
             pMonitor.setCurrentMessage("No case lists found in directory, skipping import: " + dataFile.getCanonicalPath());
         }
      } else {
         ImportCaseList.importCaseList(dataFile, pMonitor);
      }
      ConsoleUtil.showWarnings(pMonitor);
   }
}
