package org.mskcc.cbio.importer.foundation.transformer;

import com.google.common.base.*;
import com.google.common.base.Optional;
import com.google.common.collect.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.mskcc.cbio.foundation.jaxb.*;
import org.mskcc.cbio.importer.Config;
import org.mskcc.cbio.importer.FileTransformer;
import org.mskcc.cbio.importer.foundation.extractor.FileDataSource;
import org.mskcc.cbio.importer.foundation.support.CasesTypeSupplier;

import org.mskcc.cbio.importer.foundation.transformer.util.FoundationTransformerUtil;
import org.mskcc.cbio.importer.model.CancerStudyMetadata;
import org.mskcc.cbio.importer.model.FoundationMetadata;
import org.mskcc.cbio.importer.persistence.staging.CaseListFileHandler;
import org.mskcc.cbio.importer.persistence.staging.MetadataFileHandler;

/*
 responsible for transforming one or more XML files representing Foundation
 studies in a directory to cbio staging files
 Transformation steps: 
 1. delete any existing staging files
 2. create new instances of staging files
 3. for each XML file within the directory
 a. unmarshal XML to JAXB object graph
 b. for each case in object graph
 i. filter out excluded cases
 ii. transform Java class attribute values to values in a staging file record
 iii. append staging file record to appropriate staging file
 4. close staging files
    
 */
public class FoundationXMLTransformer implements FileTransformer {

    private static final Logger logger = Logger.getLogger(FoundationXMLTransformer.class);

    // short variant transformer
    private  FoundationShortVariantTransformer svtTransformer;
   // CNV transformer
    private  FoundationCnvTransformer cnvTransformer;
   // clinical data transformer
    private FoundationClinicalDataTransformer clinicalDataTransformer;
    // fusion data transformer
    private FoundationFusionTransformer fusionDataTransformer;
    // JAXB object source
    private Supplier<CasesType> casesTypeSupplier;

    private FoundationMetadata foundationMetadata;
    private CancerStudyMetadata csMetadata;

    /*
    responsible for initiating the Foundation specific data transformers and for coordinating
    the transformation of multiple XML source files
     */

    public FoundationXMLTransformer(FoundationShortVariantTransformer svtTransformer,
                                    FoundationCnvTransformer cnvTransformer,
                                    FoundationClinicalDataTransformer clinicalDataTransformer,
                                    FoundationFusionTransformer fusionDataTransformer) {

        Preconditions.checkArgument(null != svtTransformer);
        Preconditions.checkArgument(null!= cnvTransformer);
        Preconditions.checkArgument(null!=clinicalDataTransformer);
        Preconditions.checkArgument(null!= fusionDataTransformer);
        this.svtTransformer = svtTransformer;
        this.cnvTransformer = cnvTransformer;
        this.clinicalDataTransformer = clinicalDataTransformer;
        this.fusionDataTransformer = fusionDataTransformer;
    }

    @Override
    public void transform(FileDataSource xmlSource) {
        Preconditions.checkArgument(null != xmlSource,
                "A FileDataSource for XML input files is required");
        Preconditions.checkArgument(!xmlSource.getFilenameList().isEmpty(),
                "The FileDataSource does not contain any XML files");
        this.resolveStudyMetadata(xmlSource);
        //initialize the file handlers responsible for generating staging files
        registerStagingFileDirectoryPathWithTransformers(xmlSource);
        // process the XML files listed in the file data source
        processFoundationFileDataSource(xmlSource);
    }

    private void resolveStudyMetadata(FileDataSource xmlSource) {
        // use the first file name to determine the FoundtationMetadata for this study
        // from that determine the CancerStudyMetadata
        Optional<FoundationMetadata> metadataOptional = FoundationMetadata.
                findFoundationMetadataByXmlFileName(xmlSource.getFilenameList().get(0).toString());
        //Optional<FoundationMetadata> metadataOptional =
         //       FoundationTransformerUtil.resolveFoundationMetadataFromXMLFilename(config, xmlSource.getFilenameList().get(0).toString());
        if (metadataOptional.isPresent()) {
            this.foundationMetadata = metadataOptional.get();
            Optional<CancerStudyMetadata> csMeta = CancerStudyMetadata.
                    findCancerStudyMetaDataByCancerStudyName(foundationMetadata.getCancerStudy());
            if (csMeta.isPresent()){
                this.csMetadata = csMeta.get();
            } else {
                logger.error("Unable to resolve cancer study  metadata for Foundation file "
                        +xmlSource.getFilenameList().get(0).toString());
            }
        } else {
            logger.error("Unable to resolve foundation metadata for Foundation file "
                    +xmlSource.getFilenameList().get(0).toString());
        }
       }

    /*
    Evoke transformation of each Foundation listed in the file data source
    Output from multiple Foundation XML file relating to the same study are
    appended to a common staging file
     */

    private void processFoundationFileDataSource(FileDataSource xmlSource) {
        // the CNA table must be persisted across all the XML files in a study
        // initialize the set of case ids
        Set<String> caseIdSet = Sets.newHashSet();
        // process each xml file in the study's staging directory
        for (Path xmlPath : xmlSource.getFilenameList()) {
           // Optional<FoundationMetadata> metadataOptional =
            //        this.resolveFoundationMetadataFromXMLFilename(xmlPath.getFileName().toString());

            if(null != this.foundationMetadata) {
                this.casesTypeSupplier = Suppliers.memoize(new CasesTypeSupplier(xmlPath.toString(),
                       this.foundationMetadata));

                this.processFoundationData();
                // add the file's case ids to set of case ids for the study
               caseIdSet.addAll(this.resolveFoundationCaseSetForFile());
            } else {
                logger.error("File "+ xmlPath.toString() +" cannot be associated with a cancer study");
            }
        }
        // the CNA report can only be generated after all the XML files have been processed
       this.cnvTransformer.persistFoundationCnvs();
        // generate caseLists
        CaseListFileHandler caseListFileHandler = new CaseListFileHandler(this.csMetadata.getStableId(),
                Paths.get(xmlSource.getDirectoryName()));
        caseListFileHandler.generateCaseListFiles(caseIdSet);

    }

    private void generateMetadataFile(CancerStudyMetadata csMetadata, Path stagingDirectoryPath){
        Path metadataPath = stagingDirectoryPath.resolve(csMetadata.getCancerStudyMetadataFilename());
        MetadataFileHandler.INSTANCE.generateMetadataFile(this.generateMetadataMap(csMetadata),
                metadataPath);
    }

    /*
    Register the staging directory for the location of the transformed data with the transformers
     */
    private void registerStagingFileDirectoryPathWithTransformers(FileDataSource xmlSource) {
        Path stagingFileDirectory = Paths.get(xmlSource.getDirectoryName());
        this.generateMetadataFile(this.csMetadata,stagingFileDirectory);
        this.svtTransformer.registerStagingFileDirectory(this.csMetadata,stagingFileDirectory);
        this.cnvTransformer.registerStagingFileDirectory(this.csMetadata,stagingFileDirectory);
        this.clinicalDataTransformer.registerStagingFileDirectory(stagingFileDirectory);
        this.fusionDataTransformer.registerStagingFileDirectory(this.csMetadata,stagingFileDirectory);
    }

    /*
    private method to generate the metadata mappings
     */
    private Map<String,String> generateMetadataMap(CancerStudyMetadata meta){
        Map<String,String> metaMap = Maps.newTreeMap();
        metaMap.put("001type_of_cancer:", meta.getTumorType());
        metaMap.put("002cancer_study_identifier:",meta.getStableId());
        metaMap.put("003name:",meta.getName());
        metaMap.put("005profile_description:",meta.getDescription());
        metaMap.put("006groups:",meta.getGroups());
        return metaMap;
    }


    /*
    private method to resolve the set of unique case (i.e. sample) ids found in the XML file
     */
    private Set<String> resolveFoundationCaseSetForFile(){
      return  FluentIterable.from(this.casesTypeSupplier.get().getCase())
                .transform(new Function<CaseType, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable CaseType caseType) {
                        return caseType.getCase();
                    }
                }).toSet();
    }

    @Override
    public void transform(Path aPath) throws IOException {
    }

    private void processFoundationData() {
        this.svtTransformer.transform(this.casesTypeSupplier.get());
        this.cnvTransformer.generateCNATable(this.casesTypeSupplier.get());
        this.clinicalDataTransformer.transform(this.casesTypeSupplier.get());
        this.fusionDataTransformer.transform(this.casesTypeSupplier.get());
        logger.info("Foundation data processed");
    }

    @Override
    /*
     * The primary identifier for Foundation Medicine cases is the study id
     */
    public String getPrimaryIdentifier() {
        CasesType casesType = this.casesTypeSupplier.get();
        // get the study from the first case
        return casesType.getCase().get(0).getVariantReport().getStudy();
    }

    @Override
    /*
     * the primary entity for Foundation Medicine XML data is the Case
     */
    public Integer getPrimaryEntityCount() {
        CasesType casesType = this.casesTypeSupplier.get();
        return casesType.getCase().size();
    }

}
