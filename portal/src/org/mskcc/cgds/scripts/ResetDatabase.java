package org.mskcc.cgds.scripts;

import org.mskcc.cgds.dao.*;

/**
 * Empty the database.
 *
 * @author Ethan Cerami
 * @author Arthur Goldberg goldberg@cbio.mskcc.org
 */
public class ResetDatabase {

    public static final int MAX_RESET_SIZE = 6;

    /**
     * Remove all records from any size database.
     * Whenever a new Dao* class is defined, must add its deleteAllRecords() method here.
     *
     * @throws DaoException
     */
    public static void resetAnySizeDatabase() throws DaoException {

        DaoUser.deleteAllRecords();
        DaoUserAuthorities.deleteAllRecords();
        DaoTypeOfCancer.deleteAllRecords();
        DaoCancerStudy.deleteAllRecords();
        DaoMicroRna daoMicroRna = new DaoMicroRna();
        daoMicroRna.deleteAllRecords();
        DaoGeneOptimized daoGene = DaoGeneOptimized.getInstance();
        daoGene.deleteAllRecords();
        DaoCase daoCase = new DaoCase();
        daoCase.deleteAllRecords();
        DaoGeneticAlteration daoGenetic = DaoGeneticAlteration.getInstance();
        daoGenetic.deleteAllRecords();
        DaoMicroRnaAlteration daoMicroRnaAlteration = DaoMicroRnaAlteration.getInstance();
        daoMicroRnaAlteration.deleteAllRecords();
        DaoMutSig daoMutSig = DaoMutSig.getInstance();
        daoMutSig.deleteAllRecords();
        DaoGeneticProfile daoGeneticProfile = new DaoGeneticProfile();
        daoGeneticProfile.deleteAllRecords();
        DaoCaseList daoCaseList = new DaoCaseList();
        daoCaseList.deleteAllRecords();
        DaoClinicalData daoClinicalData = new DaoClinicalData();
        daoClinicalData.deleteAllRecords();
        DaoMutation daoMutation = DaoMutation.getInstance();
        daoMutation.deleteAllRecords();
        DaoMutationFrequency daoMutationFrequency = new DaoMutationFrequency();
        daoMutationFrequency.deleteAllRecords();
        DaoGeneticProfileCases daoGeneticProfileCases = new DaoGeneticProfileCases();
        daoGeneticProfileCases.deleteAllRecords();
        DaoInteraction daoInteraction = DaoInteraction.getInstance();
        daoInteraction.deleteAllRecords();
        DaoProteinArrayData.getInstance().deleteAllRecords();
        DaoProteinArrayInfo.getInstance().deleteAllRecords();
        DaoProteinArrayTarget.getInstance().deleteAllRecords();

        DaoSangerCensus daoSangerCensus = DaoSangerCensus.getInstance();
        daoSangerCensus.deleteAllRecords();

        DaoGeneSet daoGeneSet = new DaoGeneSet();
        daoGeneSet.deleteAllRecords();
    }

    public static void resetDatabase() throws DaoException {

        // safety measure: don't reset a big database
        if (MAX_RESET_SIZE < DaoCancerStudy.getCount()) {
            throw new DaoException("Database has " + DaoCancerStudy.getCount() +
                    " studies, and we don't reset a database with more than " + MAX_RESET_SIZE + " records.");
        } else {
            resetAnySizeDatabase();
        }

    }

    public static void main(String[] args) throws DaoException {
        StatDatabase.statDb();
        ResetDatabase.resetDatabase();
        System.err.println("Database Cleared and Reset.");
    }
}
