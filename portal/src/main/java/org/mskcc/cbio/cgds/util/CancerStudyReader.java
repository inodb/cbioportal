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

package org.mskcc.cbio.cgds.util;

import org.mskcc.cbio.cgds.dao.DaoCancerStudy;
import org.mskcc.cbio.cgds.dao.DaoException;
import org.mskcc.cbio.cgds.model.CancerStudy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Reads and loads a cancer study file. (Before July 2011, was called a cancer type file.)
 * By default, the loaded cancers are public. 
 * 
 * @author Arthur Goldberg goldberg@cbio.mskcc.org
 */
public class CancerStudyReader {

   public static CancerStudy loadCancerStudy(File file) throws IOException, DaoException {
      Properties properties = new Properties();
      properties.load(new FileInputStream(file));

      String cancerStudyIdentifier = properties.getProperty("cancer_study_identifier");
      if (cancerStudyIdentifier == null) {
         throw new IllegalArgumentException("cancer_study_identifier is not specified.");
      }
      
      if ( DaoCancerStudy.doesCancerStudyExistByStableId(cancerStudyIdentifier) ) {
         throw new IllegalArgumentException("cancer study identified by cancer_study_identifier "
                  + cancerStudyIdentifier + " already in dbms.");
      }

      String name = properties.getProperty("name");
      if (name == null) {
         throw new IllegalArgumentException("name is not specified.");
      }

      String description = properties.getProperty("description");
      if (description == null) {
         throw new IllegalArgumentException("description is not specified.");
      }

      String typeOfCancer = properties.getProperty("type_of_cancer");
      if ( typeOfCancer == null) {
         throw new IllegalArgumentException("type of cancer is not specified.");
      }
      
      return addCancerStudy(cancerStudyIdentifier, name, description, 
               typeOfCancer, publicStudy( properties ) );
   }

   private static CancerStudy addCancerStudy(String cancerStudyIdentifier, String name, String description, 
            String typeOfCancer, boolean publicStudy )
            throws DaoException {
      CancerStudy cancerStudy = new CancerStudy( name, description, 
               cancerStudyIdentifier, typeOfCancer, publicStudy );
      DaoCancerStudy.addCancerStudy(cancerStudy);
      return cancerStudy;
   }
   
   private static boolean publicStudy( Properties properties ) {
      String studyAccess = properties.getProperty("study_access");
      if ( studyAccess != null) {
         if( studyAccess.equals("public") ){
            return true;
         }
         if( studyAccess.equals("private") ){
            return false;
         }
         throw new IllegalArgumentException("study_access must be either 'public' or 'private', but is " + 
                  studyAccess );
      }
      // studies are public by default
      return true;
   }

}