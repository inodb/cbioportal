package org.mskcc.cbio.importer.persistence.staging.fusion;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.mskcc.cbio.importer.model.CancerStudyMetadata;
import org.mskcc.cbio.importer.persistence.staging.MetadataFileHandler;
import org.mskcc.cbio.importer.persistence.staging.TsvStagingFileHandler;

import java.nio.file.Path;
import java.util.Map;

/**
 * Copyright (c) 2014 Memorial Sloan-Kettering Cancer Center.
 * <p/>
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
 * <p/>
 * Created by criscuof on 11/13/14.
 */
public class FusionTransformer {
    protected final TsvStagingFileHandler fileHandler;

    public FusionTransformer(TsvStagingFileHandler aHandler) {
        Preconditions.checkArgument(aHandler != null,
                "A TsvStagingFileHandler implementation is required");
        this.fileHandler = aHandler;
    }

    /*
     package method to register the staging file directory with the file handler
     this requires a distinct method in order to support multiple source files being
     transformed to a single staging file
      */
   public  void registerStagingFileDirectory(CancerStudyMetadata csMetadata,Path stagingDirectoryPath){
        Preconditions.checkArgument(null != stagingDirectoryPath,
                "A Path to the staging file directory is required");
        Preconditions.checkArgument(null != csMetadata,
                "A CancerStudyMetadata object is required");
        Path mafPath = stagingDirectoryPath.resolve("data_fusions.txt");
        this.fileHandler.registerTsvStagingFile(mafPath, FusionModel.resolveColumnNames(), true);
        this.generateMetadataFile(csMetadata,stagingDirectoryPath);
    }

    private void generateMetadataFile(CancerStudyMetadata csMetadata, Path stagingDirectoryPath){
        Path metadataPath = stagingDirectoryPath.resolve("meta_fusions.txt");
        MetadataFileHandler.INSTANCE.generateMetadataFile(this.generateMetadataMap(csMetadata),
                metadataPath);
    }

    private Map<String,String> generateMetadataMap(CancerStudyMetadata meta){

        Map<String,String> metaMap = Maps.newTreeMap();
        metaMap.put("001cancer_study_identifier:", meta.getStableId());
        metaMap.put("002genetic_alteration_type:","FUSION");
        metaMap.put("003stable_id:",meta.getStableId()+"_mutations");
        metaMap.put("004show_profile_in_analysis_tab:","false");
        metaMap.put("005profile_description:",meta.getDescription());
        metaMap.put("006profile_name:","Fusions");
        return metaMap;
    }
}
