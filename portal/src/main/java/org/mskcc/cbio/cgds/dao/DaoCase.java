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

package org.mskcc.cbio.cgds.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Data access object for Case table
 */
public class DaoCase {
   
   public static final int NO_SUCH_PROFILE_ID = -1;

   // TODO: I think these methods should be static, as this object has no state
    public int addCase(String caseId, int geneticProfileId) throws DaoException {
        if (caseId == null || caseId.trim().length() == 0) {
            throw new IllegalArgumentException ("Case ID is null or empty");
        }
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            if (!caseExistsInGeneticProfile(caseId, geneticProfileId)) {
                con = JdbcUtil.getDbConnection();
                pstmt = con.prepareStatement
                        ("INSERT INTO _case (`CASE_ID`, `GENETIC_PROFILE_ID`) "
                                + "VALUES (?,?)");
                pstmt.setString(1, caseId);
                pstmt.setInt(2, geneticProfileId);
                int rows = pstmt.executeUpdate();
                return rows;
            } else {
                return 0;
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(con, pstmt, rs);
        }
    }

    public boolean caseExistsInGeneticProfile(String caseId, int geneticProfileId)
            throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection();
            pstmt = con.prepareStatement
                    ("SELECT * FROM _case WHERE CASE_ID = ? AND GENETIC_PROFILE_ID = ?");
            pstmt.setString(1, caseId);
            pstmt.setInt(2, geneticProfileId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(con, pstmt, rs);
        }
    }

    public int getProfileIdForCase( String caseId ) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection();
            pstmt = con.prepareStatement("SELECT GENETIC_PROFILE_ID FROM _case WHERE CASE_ID = ?");
            pstmt.setString(1, caseId );
            rs = pstmt.executeQuery();
            if( rs.next() ) {
               return rs.getInt("GENETIC_PROFILE_ID");
            }else{
               return NO_SUCH_PROFILE_ID;
            }

        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(con, pstmt, rs);
        }
    }

    public ArrayList<String> getAllCaseIdsInProfile(int geneticProfileId) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection();
            pstmt = con.prepareStatement
                    ("SELECT * FROM _case WHERE GENETIC_PROFILE_ID = ?");
            pstmt.setInt(1, geneticProfileId);
            rs = pstmt.executeQuery();
            ArrayList<String> caseIds = new ArrayList<String>();
            while (rs.next()) {
                caseIds.add(rs.getString("CASE_ID"));
            }
            return caseIds;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(con, pstmt, rs);
        }
    }

    public ArrayList<String> getAllCases() throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection();
            pstmt = con.prepareStatement
                    ("SELECT * FROM _case");
            rs = pstmt.executeQuery();
            ArrayList<String> caseIds = new ArrayList<String>();
            while (rs.next()) {
                caseIds.add(rs.getString("CASE_ID"));
            }
            return caseIds;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(con, pstmt, rs);
        }
    }

    public void deleteAllRecords() throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection();
            pstmt = con.prepareStatement("TRUNCATE TABLE _case");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(con, pstmt, rs);
        }
    }
}
