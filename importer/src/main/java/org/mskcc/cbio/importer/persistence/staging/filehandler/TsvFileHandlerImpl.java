/*
 *  Copyright (c) 2014 Memorial Sloan-Kettering Cancer Center.
  * 
  *  This library is distributed in the hope that it will be useful, but
  *  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
  *  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
  *  documentation provided hereunder is on an "as is" basis, and
  *  Memorial Sloan-Kettering Cancer Center 
  *  has no obligations to provide maintenance, support,
  *  updates, enhancements or modifications.  In no event shall
  *  Memorial Sloan-Kettering Cancer Center
  *  be liable to any party for direct, indirect, special,
  *  incidental or consequential damages, including lost profits, arising
  *  out of the use of this software and its documentation, even if
  *  Memorial Sloan-Kettering Cancer Center 
  *  has been advised of the possibility of such damage.
 */

package org.mskcc.cbio.importer.persistence.staging.filehandler;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.inject.internal.Sets;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.mskcc.cbio.importer.persistence.staging.StagingCommonNames;
import org.mskcc.cbio.importer.persistence.staging.util.StagingUtils;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.file.StandardOpenOption.*;

/*
concrete implementation of TsvFileHandler interface
responsible for all interactions with importer TSV files
 */


public class TsvFileHandlerImpl implements TsvFileHandler{
     public  Path stagingFilePath;
    private static final Boolean DEFAULT_DELETE_VALUE = true;

    
    private final static Logger logger = Logger.getLogger(TsvFileHandlerImpl.class);

     TsvFileHandlerImpl(Path aPath, List<String> columnHeadings) {
        this.registerTsvStagingFile(aPath, columnHeadings,this.DEFAULT_DELETE_VALUE );
    }

    TsvFileHandlerImpl(Path aPath, List<String> columnHeadings, boolean deleteFlag) {
        this.registerTsvStagingFile(aPath, columnHeadings,deleteFlag );
    }


    /*
    public method to associate a file handler with a specific file
    requires the caller to specify how a existing file should be handled (delete or append)
    this is needed to support import processes that append to an existing file (e.g. DMP)
    if a new TSV file is created, a list of Strings is used to write column headings
     */


    private void registerTsvStagingFile(Path stagingFilePath, List<String> columnHeadings, boolean deleteFile) {

        Preconditions.checkArgument(null != stagingFilePath,
                "A valid Path to a staging file is required");

        /*
        option to delete an existing staging file to support replacement-mode staging
        operations
         */
        if(deleteFile){
            try {
                Files.deleteIfExists(stagingFilePath);
            } catch (IOException e) {
               logger.error(e.getMessage());
            }
        }

       // create the staging file if it doesn't exist and write out the column headers
       if (!Files.exists(stagingFilePath, LinkOption.NOFOLLOW_LINKS)) {
           Preconditions.checkArgument(null != columnHeadings && !columnHeadings.isEmpty(),
                   "Column headings are required for the new staging file: " 
                           +stagingFilePath.toString());
            try {
                // create the file and parent directories
                Files.createDirectories(stagingFilePath.getParent());
                OpenOption[] options = new OpenOption[]{CREATE, APPEND, DSYNC};
                String line = StagingCommonNames.tabJoiner.join(columnHeadings) +"\n";
                Files.write(stagingFilePath,
                        line.getBytes(),
                        options);
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }       
       }
      this.stagingFilePath = stagingFilePath;
        logger.info("Staging file path = " +this.stagingFilePath);

    }

    @Override
    /*
    public method to output a List of objects to the registered staging Path
    a transformation function is applied to each object in the list transforming the
    object to a TSV String
    the write operation is treated as an append since even a new TSV file has column headings
    already written
     */
    public void transformImportDataToTsvStagingFile(List aList, Function transformationFunction) {

        Preconditions.checkArgument(null != aList && !aList.isEmpty(),
                "A valid List of staging data is required");
        Preconditions.checkArgument(null != transformationFunction,
                "A transformation function is required");
        Preconditions.checkState(null != this.stagingFilePath,
                "The requisite Path to the tsv staging file has not be specified");
        
        OpenOption[] options = new OpenOption[]{ APPEND, DSYNC};
        try {
            Files.write(this.stagingFilePath, Lists.transform(aList, 
                    transformationFunction), 
                    Charset.defaultCharset(), options);
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
        }
    }

    /*
    public method to filter out rows from an existing staging file that have been deprecated
    inputs consist of a list of deprecated values and the name of the column for these values.
    typically this is the sample id
    the existing file is copied to a temporary file and the deprecated rows are skipped
    during that process.
    the filtered file is then copied back to its original location
     */

   public void removeDeprecatedSamplesFomTsvStagingFiles(final String sampleIdColumnName, final Set<String> deprecatedSampleSet) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(sampleIdColumnName), "The name of the sample id column is required");
        Preconditions.checkArgument(null != deprecatedSampleSet,
                "A set of deprecated samples is required");
        Preconditions.checkState(null != this.stagingFilePath,
                "The requisite Path to the  staging file has not be specified");
        if (deprecatedSampleSet.size() > 0) {
            OpenOption[] options = new OpenOption[]{CREATE, APPEND, DSYNC};
            Path tempDir = null;
            Path tempFilePath = null;
            try {
                // move staging file to a temporary file, filter out deprecated samples,
                // then write non-deprecated samples
                // back to staging files

                tempDir = Files.createTempDirectory("dmptemp");
                tempFilePath = Files.createTempFile(tempDir, ".txt" ,null);
                Files.deleteIfExists(tempFilePath);
                Files.move(this.stagingFilePath, tempFilePath);
                logger.info(" processing " + tempFilePath.toString());
                final CSVParser parser = new CSVParser(new FileReader(tempFilePath.toFile()),
                        CSVFormat.TDF.withHeader());
                String headings = StagingCommonNames.tabJoiner.join(parser.getHeaderMap().keySet());
                // filter persisted sample ids that are also in the current data input
                List<String> filteredSamples = FluentIterable.from(parser)
                        .filter(new Predicate<CSVRecord>() {
                            @Override
                            public boolean apply(CSVRecord record) {
                                String sampleId = record.get(sampleIdColumnName);
                                if (!Strings.isNullOrEmpty(sampleId) && !deprecatedSampleSet.contains(sampleId)) {
                                    return true;
                                }
                                return false;
                            }
                        }).transform(new Function<CSVRecord, String>() {
                            @Override
                            public String apply(CSVRecord record) {

                                return StagingCommonNames
                                        .tabJoiner
                                        .join(Lists.newArrayList(record.iterator()));
                            }
                        })
                        .toList();

                // write the filtered data to the original staging file
                // column headings
                Files.write(this.stagingFilePath,Lists.newArrayList(headings), Charset.defaultCharset(),options);
                // data
                Files.write(this.stagingFilePath, filteredSamples, Charset.defaultCharset(), options);
            } catch (IOException ex) {
                logger.error(ex.getMessage());
                ex.printStackTrace();
            } finally {
                try {
                    Files.delete(tempFilePath);
                    Files.delete(tempDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /*
        method to scan a tsv staging file and return a list of processed sample ids
        based on a column  being identified as the sample id attribute
        this is to facilitate sample replacement processing
         */
    public Set<String> resolveProcessedSampleSet(final String sampleIdColumnName) {
        Set<String> processedSampleSet = Sets.newHashSet();
        try {
            final CSVParser parser = new CSVParser(new FileReader(this.stagingFilePath.toFile()), CSVFormat.TDF.withHeader());
            Set<String> sampleSet = FluentIterable.from(parser).transform(new Function<CSVRecord, String>() {
                @Override
                public String apply(CSVRecord record) {
                    return record.get(sampleIdColumnName);
                }
            }).toSet();

            processedSampleSet.addAll(sampleSet);
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
        return processedSampleSet;
    }

    /*
    public method to verify that a file handler is associated with a Path to a staging file
     */
    public boolean isRegistered() { return null != this.stagingFilePath; }
    

    /*
    public method to read data from an existing TSV file into a Google Guava Table object
    typically this is for copy number variation data
     */
    @Override
    public Table<String, String, String> initializeCnvTable() {
        com.google.inject.internal.Preconditions.checkState(null != this.stagingFilePath,
                "The Path to the data_CNA.txt file has not been specified");
        logger.info("Reading in existing cnv data from " +this.stagingFilePath.toString());
        Table<String, String,String> cnvTable = HashBasedTable.create();
        // determine if there are persisted cnv data; if so read into Table data structure
        if (Files.exists(stagingFilePath, LinkOption.NOFOLLOW_LINKS)) {
            Reader reader = null;
            try {
                reader = new FileReader(this.stagingFilePath.toFile());
                final CSVParser parser = new CSVParser(reader, CSVFormat.TDF.withHeader());
                Map<String, Integer> headerMap = parser.getHeaderMap();
                List<String> columnList = Lists.newArrayList();
                for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                    columnList.add(entry.getKey());
                }
                // determine the column name used for the Hugo Symbol
                String geneColumnName = columnList.remove(0);
                // process each data row in table
                for (CSVRecord record : parser.getRecords()) {
                    String geneName = record.get(geneColumnName);
                    for (String sampleName : columnList) {
                        cnvTable.put(geneName, sampleName, record.get(sampleName));
                    }
                }
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            } finally {
                try {
                    reader.close();
                } catch (IOException ex) {
                }
            }

        }
        return cnvTable;
    }
    /*
     method to write out updated CNV data as TSV file
     rows = gene names
     columns = DMP sample ids
     values  = gene fold change
     since legacy entries may have been updated, previous file contents are overwritten
     */

    @Override


    public void persistCnvTable(Table<String, String, String> cnvTable) {
        com.google.inject.internal.Preconditions.checkArgument(null != cnvTable, "A Table of CNV data is required");
        com.google.inject.internal.Preconditions.checkState(null != this.stagingFilePath,
                "The Path to the data_CNA.txt file has not been specified");
        try (BufferedWriter writer = Files.newBufferedWriter(
                stagingFilePath, Charset.defaultCharset())) {
            Set<String> geneSet = cnvTable.rowKeySet();
            Set<String> sampleSet = cnvTable.columnKeySet();
            // write out the headers
            writer.append(StagingCommonNames.tabJoiner.join(StagingCommonNames.HUGO_COLUMNNAME,
                    StagingCommonNames.tabJoiner.join(sampleSet)) + "\n");
            // write out values
            for (String gene : geneSet) {
                String geneLine = gene;
                for (String sample : sampleSet) {
                    String value = (cnvTable.get(gene, sample) != null) ? cnvTable.get(gene, sample).toString() : "0";
                    geneLine = StagingCommonNames.tabJoiner.join(geneLine, value);

                }
                writer.append(geneLine + "\n");
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            ex.printStackTrace();
        }
    }


}
