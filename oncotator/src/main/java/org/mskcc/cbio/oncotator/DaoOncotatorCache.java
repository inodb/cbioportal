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

package org.mskcc.cbio.oncotator;

import org.mskcc.cbio.dbcache.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Database implementation (DAO) for the Oncotator Cache Service.
 *
 */
public class DaoOncotatorCache implements OncotatorCacheService
{
    private static DaoOncotatorCache daoOncotatorCache;

    private DaoOncotatorCache() {
    }

    /**
     * Gets Singleton Instance.
     * @return DaoOncotator Object.
     */
    public static DaoOncotatorCache getInstance()
    {
        if (daoOncotatorCache == null) {
            daoOncotatorCache = new DaoOncotatorCache();
        }
        
        return daoOncotatorCache;
    }

    /**
     * Adds a new oncotator record to the database cache.
     *
     * @param record    Oncotator Record.
     * @return          number of records successfully added.
     * @throws          SQLException Database Error.
     */
    public int put(OncotatorRecord record) throws OncotatorCacheException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = DatabaseUtil.getDbConnection();
            pstmt = con.prepareStatement
                    ("INSERT INTO oncotator_cache (`CACHE_KEY`,`GENE_SYMBOL`, `GENOME_CHANGE`, `PROTEIN_CHANGE`," +
                            " `VARIANT_CLASSIFICATION`," +
                            " `EXON_AFFECTED`, `COSMIC_OVERLAP`, `DB_SNP_RS`)" +
                            " VALUES (?,?,?,?,?,?,?,?)");
            pstmt.setString(1, record.getKey());
            pstmt.setString(2, record.getBestEffectTranscript().getGene());
            pstmt.setString(3, record.getGenomeChange());
            pstmt.setString(4, record.getBestEffectTranscript().getProteinChange());
            pstmt.setString(5, record.getBestEffectTranscript().getVariantClassification());
            pstmt.setInt(6, record.getBestEffectTranscript().getExonAffected());
            pstmt.setString(7, record.getCosmicOverlappingMutations());
            pstmt.setString(8, record.getDbSnpRs());
            int rows = pstmt.executeUpdate();
            return rows;
        } catch (SQLException e) {
	        e.printStackTrace();
	        throw new OncotatorCacheException(e.getMessage());
        } finally {
            DatabaseUtil.closeAll(con, pstmt, rs);
        }
    }

	/**
	 * Gets an oncotator record for the provided key.
	 *
	 * @param key               cache key
	 * @return                  corresponding record for the given key
	 * @throws SQLException
	 */
    public OncotatorRecord get(String key) throws OncotatorCacheException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseUtil.getDbConnection();
            pstmt = con.prepareStatement
                    ("SELECT * FROM oncotator_cache WHERE CACHE_KEY = ?");
            pstmt.setString(1, key);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                OncotatorRecord record = new OncotatorRecord(rs.getString("CACHE_KEY"));
                record.getBestEffectTranscript().setGene(rs.getString("GENE_SYMBOL"));
                record.setGenomeChange(rs.getString("GENOME_CHANGE"));
                record.getBestEffectTranscript().setProteinChange(rs.getString("PROTEIN_CHANGE"));
                record.getBestEffectTranscript().setVariantClassification(rs.getString("VARIANT_CLASSIFICATION"));
                record.setCosmicOverlappingMutations(rs.getString("COSMIC_OVERLAP"));
                record.getBestEffectTranscript().setExonAffected(rs.getInt("EXON_AFFECTED"));
                record.setDbSnpRs(rs.getString("DB_SNP_RS"));
                return record;
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
	        throw new OncotatorCacheException(e.getMessage());
        } finally {
            DatabaseUtil.closeAll(con, pstmt, rs);
        }
    }

    public void deleteAllRecords() throws SQLException {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseUtil.getDbConnection();
            pstmt = con.prepareStatement("TRUNCATE TABLE oncotator_cache");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            DatabaseUtil.closeAll(con, pstmt, rs);
        }
    }
}