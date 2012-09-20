package org.mskcc.cbio.portal.util;

import org.mskcc.cbio.portal.servlet.QueryBuilder;
import org.mskcc.cbio.cgds.model.GeneticProfile;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;

// TODO: perhaps delete this class
public class ZScoreUtil {
    public static final double Z_SCORE_THRESHOLD_DEFAULT = 2;
    public static final double RPPA_SCORE_THRESHOLD_DEFAULT = 2;
    public static final double OUTLIER_THRESHOLD_DEFAULT = 1;

    public static double getZScore(HashSet<String> geneticProfileIdSet,
            ArrayList<GeneticProfile> profileList, HttpServletRequest request) {
        double zScoreThreshold = ZScoreUtil.Z_SCORE_THRESHOLD_DEFAULT;

        //  If user has selected an outlier mRNA expression profile,
        //  switch to OUTLIER_THRESHOLD_DEFAULT.
        if (GeneticProfileUtil.outlierExpressionSelected(geneticProfileIdSet, profileList)) {
            zScoreThreshold = OUTLIER_THRESHOLD_DEFAULT;
        } else {
            String zScoreThesholdStr = request.getParameter(QueryBuilder.Z_SCORE_THRESHOLD);
            if (zScoreThesholdStr != null) {
                try {
                    zScoreThreshold = Double.parseDouble(zScoreThesholdStr);

                    // take absolute value
                    if( zScoreThreshold < 0.0 ){
                       zScoreThreshold = -zScoreThreshold;
                    }

                } catch (NumberFormatException e) {
                }
            }
        }
        return zScoreThreshold;
    }
    
    public static double getRPPAScore(HttpServletRequest request) {
        String rppaScoreStr = request.getParameter(QueryBuilder.RPPA_SCORE_THRESHOLD);
        if (rppaScoreStr == null) {
            return RPPA_SCORE_THRESHOLD_DEFAULT;
        } else {
            try {
                return Double.parseDouble(rppaScoreStr);
            } catch (NumberFormatException e) {
                return RPPA_SCORE_THRESHOLD_DEFAULT;
            }
        }
    }
}
