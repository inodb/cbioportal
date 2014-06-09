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

package org.mskcc.cbio.portal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.mskcc.cbio.portal.model.Case;
import org.mskcc.cbio.portal.model.CaseList;
import org.mskcc.cbio.portal.model.CaseListCategory;

/**
 * Data access object for Case_List table
 */
public final class DaoCaseList {
    private DaoCaseList() {}

	/**
	 * Adds record to case_list table.
	 */
    public static int addCaseList(CaseList caseList) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int rows;
        try {
            con = JdbcUtil.getDbConnection(DaoCaseList.class);

            pstmt = con.prepareStatement("INSERT INTO case_list (`STABLE_ID`, `CANCER_STUDY_ID`, `NAME`, `CATEGORY`," +
                    "`DESCRIPTION`)" + " VALUES (?,?,?,?,?)");
            pstmt.setString(1, caseList.getStableId());
            pstmt.setInt(2, caseList.getCancerStudyId());
            pstmt.setString(3, caseList.getName());
            pstmt.setString(4, caseList.getCaseListCategory().getCategory());
            pstmt.setString(5, caseList.getDescription());
            rows = pstmt.executeUpdate();
   			int listListRow = addCaseListList(caseList, con);
   			rows = (listListRow != -1) ? (rows + listListRow) : rows;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoCaseList.class, con, pstmt, rs);
        }
        
        // added to _case
        for (String caseId : caseList.getCaseList()) {
            rows += DaoCase.addCase(new Case(caseId, caseList.getCancerStudyId()));
        }
            
        return rows;
    }

	/**
	 * Given a case list by stable Id, returns a case list.
	 */
    public static CaseList getCaseListByStableId(String stableId) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoCaseList.class);
            pstmt = con.prepareStatement
                    ("SELECT * FROM case_list WHERE STABLE_ID = ?");
            pstmt.setString(1, stableId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                CaseList caseList = extractCaseList(rs);
                caseList.setCaseList(getCaseListList(caseList, con));
                return caseList;
            }
			return null;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoCaseList.class, con, pstmt, rs);
        }
    }

	/**
	 * Given a case list ID, returns a case list.
	 */
    public static CaseList getCaseListById(int id) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoCaseList.class);
            pstmt = con.prepareStatement
                    ("SELECT * FROM case_list WHERE LIST_ID = ?");
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                CaseList caseList = extractCaseList(rs);
				caseList.setCaseList(getCaseListList(caseList, con));
                return caseList;
            }
			return null;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoCaseList.class, con, pstmt, rs);
        }
    }

	/**
	 * Given a cancerStudyId, returns all case list.
	 */
    public static ArrayList<CaseList> getAllCaseLists( int cancerStudyId) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoCaseList.class);

            pstmt = con.prepareStatement
                    ("SELECT * FROM case_list WHERE CANCER_STUDY_ID = ? ORDER BY NAME");
            pstmt.setInt(1, cancerStudyId);
            rs = pstmt.executeQuery();
            ArrayList<CaseList> list = new ArrayList<CaseList>();
            while (rs.next()) {
                CaseList caseList = extractCaseList(rs);
                list.add(caseList);
            }
			// get case list-list
			for (CaseList caseList : list) {
				caseList.setCaseList(getCaseListList(caseList, con));
			}
            return list;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoCaseList.class, con, pstmt, rs);
        }
    }

	/**
	 * Returns a list of all case lists.
	 */
    public static ArrayList<CaseList> getAllCaseLists() throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoCaseList.class);
            pstmt = con.prepareStatement
                    ("SELECT * FROM case_list");
            rs = pstmt.executeQuery();
            ArrayList<CaseList> list = new ArrayList<CaseList>();
            while (rs.next()) {
                CaseList caseList = extractCaseList(rs);
                list.add(caseList);
            }
			// get case list-list
			for (CaseList caseList : list) {
				caseList.setCaseList(getCaseListList(caseList, con));
			}
            return list;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoCaseList.class, con, pstmt, rs);
        }
    }

    /**
	 * Given a case id, determines if it exists
	 */
    public static boolean caseIDExists(String caseID) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoCaseList.class);
            pstmt = con.prepareStatement
                    ("SELECT * FROM case_list_list WHERE CASE_ID = ?");
            pstmt.setString(1, caseID);
            rs = pstmt.executeQuery();
            return (rs.next());
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoCaseList.class, con, pstmt, rs);
        }
    }

	/**
	 * Clears all records from case list & case_list_list.
	 */
    public static void deleteAllRecords() throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoCaseList.class);
            pstmt = con.prepareStatement("TRUNCATE TABLE case_list");
            pstmt.executeUpdate();
            pstmt = con.prepareStatement("TRUNCATE TABLE case_list_list");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoCaseList.class, con, pstmt, rs);
        }
    }

	/**
	 * Given a case list, gets list id from case_list table
	 */
	private static int getCaseListId(CaseList caseList) throws DaoException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = JdbcUtil.getDbConnection(DaoCaseList.class);
            pstmt = con.prepareStatement("SELECT LIST_ID FROM case_list WHERE STABLE_ID=?");
            pstmt.setString(1, caseList.getStableId());
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("LIST_ID");
            }
            return -1;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoCaseList.class, con, pstmt, rs);
        }
	}

	/**
	 * Adds record to case_list_list.
	 */
    private static int addCaseListList(CaseList caseList, Connection con) throws DaoException {
		
	// get case list id
	int caseListId = getCaseListId(caseList);
	if (caseListId == -1) {
            return -1;
        }
        
        if (caseList.getCaseList().isEmpty()) {
            return 0;
        }

        PreparedStatement pstmt  ;
        ResultSet rs = null;
        try {
            StringBuilder sql = new StringBuilder("INSERT INTO case_list_list (`LIST_ID`, `CASE_ID`) VALUES ");
            for (String caseId : caseList.getCaseList()) {
                sql.append("('").append(caseListId).append("','").append(caseId).append("'),");
            }
            sql.deleteCharAt(sql.length()-1);
            pstmt = con.prepareStatement(sql.toString());
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(rs);
        }
    }

	/**
	 * Given a case list object (thus case list id) gets case list list.
	 */
	private static ArrayList<String> getCaseListList(CaseList caseList, Connection con) throws DaoException {

        PreparedStatement pstmt  ;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement
                    ("SELECT * FROM case_list_list WHERE LIST_ID = ?");
            pstmt.setInt(1, caseList.getCaseListId());
            rs = pstmt.executeQuery();
            ArrayList<String> toReturn = new ArrayList<String>();
            while (rs.next()) {
				toReturn.add(rs.getString("CASE_ID"));
			}
			return toReturn;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(rs);
        }
	}

	/**
	 * Given a result set, creates a case list object.
	 */
    private static CaseList extractCaseList(ResultSet rs) throws SQLException {
        CaseList caseList = new CaseList();
        caseList.setStableId(rs.getString("STABLE_ID"));
        caseList.setCancerStudyId(rs.getInt("CANCER_STUDY_ID"));
        caseList.setName(rs.getString("NAME"));
        caseList.setCaseListCategory(CaseListCategory.get(rs.getString("CATEGORY")));
        caseList.setDescription(rs.getString("DESCRIPTION"));
        caseList.setCaseListId(rs.getInt("LIST_ID"));
        return caseList;
    }
}