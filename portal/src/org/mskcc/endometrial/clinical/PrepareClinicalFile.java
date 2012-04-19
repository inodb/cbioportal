package org.mskcc.endometrial.clinical;

import org.mskcc.cgds.dao.DaoException;
import org.mskcc.endometrial.cluster.ClusterReader;
import org.mskcc.endometrial.cna.CnaSummarizer;
import org.mskcc.endometrial.cna.CopyNumberMap;
import org.mskcc.endometrial.genomic.GenomicMap;
import org.mskcc.endometrial.mutation.CoverageReader;
import org.mskcc.endometrial.mutation.MutationMap;
import org.mskcc.endometrial.mutation.MutationSummarizer;
import org.mskcc.endometrial.rnaseq.RnaSeqReader;

import java.io.*;
import java.util.*;

/**
 * Prepares the Endometrial Clinical File.
 */
public class PrepareClinicalFile {
    private static final String NA_OUTPUT = "NA";
    private static final String TAB = "\t";
    private static final String NEW_LINE = "\n";
    private StringBuffer newTable = new StringBuffer();
    private HashSet<String> sequencedCaseSet;
    private HashSet<String> gisticCaseSet;
    private File mafFile;
    private MsiReader msiReader;
    private CoverageReader coverageReader;
    private RnaSeqReader rnaSeqReader;
    private CaseListUtil caseListUtil = new CaseListUtil();
    private HashSet<String> targetGeneSet;
    private MutationMap mutationMap;
    private CopyNumberMap copyNumberMap;
    private GenomicMap genomicMap;
    private ArrayList<ClusterReader> clusterReaders = new ArrayList<ClusterReader>();

    /**
     * Constructor.

     * @throws IOException IO Error.
     */
    public PrepareClinicalFile(File inputDir) throws IOException, DaoException {
        initTargetGeneSet();
        File clinicalFile = new File(inputDir + "/clinical/clinical.txt");
        File msiFile = new File(inputDir + "/clinical/msi.txt");
        File somaticMafFile = new File(inputDir + "/mutation/UCEC_somatic.maf");
        File cnaFile = new File(inputDir +  "/cna/all_thresholded.by_genes.txt");
        File coverageFile = new File(inputDir + "/mutation/coverage.txt");
        File rnaSeqFile = new File(inputDir + "/rna-seq/rna_seq_rpkm.txt");
        this.mafFile = somaticMafFile;
        System.out.println("Reading in Mutation data...");
        this.mutationMap = new MutationMap(somaticMafFile, targetGeneSet);
        this.copyNumberMap = new CopyNumberMap(cnaFile, targetGeneSet);
        System.out.println("Reading in CNA data...");
        this.genomicMap = new GenomicMap(mutationMap, copyNumberMap);
        System.out.println("Reading in Clinical data...");
        initReaders(inputDir, msiFile, coverageFile, rnaSeqFile);

        CnaSummarizer cnaSummarizer = new CnaSummarizer(cnaFile);
        gisticCaseSet = cnaSummarizer.getGisticCaseSet();
        MutationSummarizer mutationSummarizer = new MutationSummarizer(somaticMafFile);
        sequencedCaseSet = mutationSummarizer.getSequencedCaseSet();
        FileReader reader = new FileReader(clinicalFile);
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line = bufferedReader.readLine();  //  The header line.
        this.appendColumnHeaders(line);
        validateHeader(line);
        String headers[] = line.split("\t");

        line = bufferedReader.readLine();
        while (line != null) {
            String parts[] = line.split("\t");
            String caseId = parts[0];
            String histSubTypeAndGrade = getValue("histology_grade", headers, parts);
            caseListUtil.categorizeByHistologicalSubType(histSubTypeAndGrade, caseId);
            newTable.append(line.trim());
            appendMsiStatus(caseId);
            appendSequencedColumn(caseId);
            appendGenomicDataAvailable(cnaSummarizer, caseId);
            appendMutationCounts(mutationSummarizer, caseId);
            appendMutationSpectra(mutationSummarizer, caseId);
            newTable.append(TAB + coverageReader.getCoverage(caseId));
            appendClusters(caseId);
            appendTargetGeneSet(caseId, newTable);
            newTable.append(NEW_LINE);
            line = bufferedReader.readLine();
        }
        bufferedReader.close();
    }

    private void appendClusters(String caseId) {
        for (ClusterReader clusterReader:  clusterReaders) {
            ArrayList<String> valueList = clusterReader.getValueList(caseId);
            if (valueList != null) {
                for (String value:  valueList) {
                    newTable.append(TAB + value);
                }
            } else {
                ArrayList<String> headerList = clusterReader.getHeaderList();
                for (String header:  headerList) {
                    newTable.append(TAB + NA_OUTPUT);
                }
            }
        }
    }

    private void initTargetGeneSet() {
        targetGeneSet = new HashSet<String>();
        //        targetGeneSet.add("PTEN");
        //        targetGeneSet.add("PIK3CA");
        //        targetGeneSet.add("PIK3R1");
        //        targetGeneSet.add("PIK3R2");
        //        targetGeneSet.add("AKT1");
        //        targetGeneSet.add("AKT2");
        //        targetGeneSet.add("AKT3");
        //        targetGeneSet.add("KRAS");
    }
    
    private String getValue (String targetHeader, String[] colHeaders, String[] parts) {
        for (int i=0; i<colHeaders.length; i++) {
            String currentHeader = colHeaders[i];
            if (currentHeader.equalsIgnoreCase(targetHeader)) {
                return parts[i]; 
            }
        }
        throw new NullPointerException("Could not find column with name:  " + targetHeader);
    }

    private void initReaders(File inputDir, File msiFile, File coverageFile,
                             File rnaSeqFile) throws IOException {
        System.out.println("Reading in MSI Data...");
        msiReader = new MsiReader(msiFile);
        System.out.println("Reading in Sequence Coverage Data...");
        coverageReader = new CoverageReader(coverageFile);

        System.out.println("Reading in RNA-Seq Data...");
        rnaSeqReader = new RnaSeqReader(rnaSeqFile);

        clusterReaders.add(new ClusterReader(new File(inputDir + "/clusters/cna_clusters.txt")));
        clusterReaders.add(new ClusterReader(new File(inputDir + "/clusters/mrna_expression_clusters.txt")));
        clusterReaders.add(new ClusterReader(new File(inputDir + "/clusters/dna_methylation_clusters.txt")));
        clusterReaders.add(new ClusterReader(new File(inputDir + "/clusters/mlh1_hypermethylated.txt")));
        clusterReaders.add(new ClusterReader(new File(inputDir + "/clusters/mutation_rate_clusters.txt")));
        clusterReaders.add(new ClusterReader(new File(inputDir + "/clusters/micro_rna_clusters.txt")));
    }
    
    private void appendTargetGeneSet(String caseId, StringBuffer newTable) throws IOException, DaoException {
        Iterator<String> geneIterator = targetGeneSet.iterator();
        while(geneIterator.hasNext()) {
            String geneSymbol = geneIterator.next();
            ArrayList<String> dataFields = genomicMap.getDataFields(geneSymbol, caseId);
            appendColumns(dataFields, newTable);
        }
    }
    
    private void appendMutationSpectra(MutationSummarizer mutationSummarizer, String caseId) {
        newTable.append(TAB + mutationSummarizer.getTGMutationCount(caseId));
        newTable.append(TAB + mutationSummarizer.getTCMutationCount(caseId));
        newTable.append(TAB + mutationSummarizer.getTAMutationCount(caseId));
        newTable.append(TAB + mutationSummarizer.getCTMutationCount(caseId));
        newTable.append(TAB + mutationSummarizer.getCGMutationCount(caseId));
        newTable.append(TAB + mutationSummarizer.getCAMutationCount(caseId));
    }

    private void appendColumnHeaders(String newHeaderLine) {
        newTable.append(newHeaderLine.trim() + TAB
                + "msi_status_7_marker_call" + TAB
                + "msi_status_5_marker_call" + TAB
                + "data_maf" + TAB
                + "data_gistic" + TAB
                + "data_rna_seq" + TAB
                + "data_core_sample" + TAB
                + "silent_mutation_count" + TAB
                + "non_silent_mutation_count" + TAB
                + "total_snv_count" + TAB
                + "indel_mutation_count" + TAB
                + "tg_count" + TAB
                + "tc_count" + TAB
                + "ta_count" + TAB
                + "ct_count" + TAB
                + "cg_count" + TAB
                + "ca_count" + TAB
                + "covered_bases" + TAB);
        for (ClusterReader reader:  clusterReaders) {
            ArrayList<String> headerList = reader.getHeaderList();
            for (String header:  headerList) {
                newTable.append(header + TAB);
            }
        }
        appendTargetGeneSetColumns();
        newTable.append(NEW_LINE);
    }

    private void appendTargetGeneSetColumns() {
        Iterator<String> geneIterator = targetGeneSet.iterator();
        while(geneIterator.hasNext()) {
            String geneSymbol = geneIterator.next();
            ArrayList<String> headingList = genomicMap.getColumnHeaders(geneSymbol);
            appendColumns(headingList, newTable);
        }
    }

    private void appendMutationCounts(MutationSummarizer mutationSummarizer, String caseId) {
        long totalSnvCount = mutationSummarizer.getSilentMutationCount(caseId)
                + mutationSummarizer.getNonSilentMutationCount(caseId);
        newTable.append (TAB + mutationSummarizer.getSilentMutationCount(caseId));
        newTable.append (TAB + mutationSummarizer.getNonSilentMutationCount(caseId));
        newTable.append (TAB + totalSnvCount);
        newTable.append (TAB + mutationSummarizer.getInDelCount(caseId));
    }

    private void appendGenomicDataAvailable(CnaSummarizer cnaSummarizer, String caseId) {
        if (cnaSummarizer.hasCnaData(caseId)) {
            newTable.append (TAB + "Y");
        } else {
            newTable.append (TAB + "N");
        }
        if (rnaSeqReader.hasRnaReqData(caseId)) {
            newTable.append (TAB + "Y");
        } else {
            newTable.append (TAB + "N");
        }
        if (sequencedCaseSet.contains(caseId) && cnaSummarizer.hasCnaData(caseId)
                && rnaSeqReader.hasRnaReqData(caseId)) {
            newTable.append (TAB + "Y");
        } else {
            newTable.append (TAB + "N");
        }
    }

    private void appendSequencedColumn(String caseId) {
        if (sequencedCaseSet.contains(caseId)) {
            newTable.append (TAB + "Y");
        } else {
            newTable.append (TAB + "N");
        }
    }

    private void appendMsiStatus(String caseId) {
        String msi5Status = msiReader.getMsi5Status(caseId);
        if (msi5Status != null) {
            newTable.append(TAB + msi5Status);
        } else {
            newTable.append(TAB + NA_OUTPUT);
        }
        String msi7Status = msiReader.getMsi7Status(caseId);
        if (msi7Status != null) {
            newTable.append(TAB + msi5Status);
        } else {
            newTable.append(TAB + NA_OUTPUT);
        }
    }

    public void writeCaseLists(String outputDir) throws IOException {
        caseListUtil.writeCaseLists(sequencedCaseSet, gisticCaseSet, outputDir);
    }

    public HashSet<String> getSequencedCaseSet() {
        return sequencedCaseSet;
    }

//    /**
//     * The portal expects specific data column names.  This functions transforms into header names that the
//     * portal likes.
//     */
//    private String transformHeader(String headerLine) {
//        headerLine = headerLine.replaceAll("bcr_patient_barcode", "CASE_ID");
//        headerLine = headerLine.replaceAll("NewVitalStatus", "OS_STATUS");
//        return headerLine;
//    }

    /**
     * Validate the Headers.  If the headers have changed, all bets are off and abort.
     */
    private void validateHeader(String header) {
        String parts[] = header.split("\t");
        if (!parts[0].equals("tcga_id")) {
            throw new IllegalArgumentException ("Header at 0 was expecting:  TCGAID");
        }
        if (!parts[8].equals("vital_status")) {
            throw new IllegalArgumentException ("Header at 10 was expecting:  VitalStatus");
        }
    }

    public String getNewClinicalTable() {
        return newTable.toString();
    }

    private void appendColumns(ArrayList<String> list, StringBuffer table) {
        for (String value:  list) {
            table.append(TAB + value);
        }
    }

    public static void main(String[] args) throws Exception {
        // check args
        if (args.length < 1) {
            System.out.println("command line usage:  prepareClinical.pl <input_id>");
            System.exit(1);
        }

        PrepareClinicalFile prepareClinicalFile = new PrepareClinicalFile(new File(args[0]));

        prepareClinicalFile.writeCaseLists(args[0]);

        File newClinicalFile = new File(args[0] + "/clinical/clinical_unified.txt");
        FileWriter writer = new FileWriter(newClinicalFile);
        writer.write(prepareClinicalFile.getNewClinicalTable());

        HashSet <String> sequencedCaseSet = prepareClinicalFile.getSequencedCaseSet();
        System.out.println ("Number of cases sequenced:  " + sequencedCaseSet.size());
        System.out.println ("New Clinical File Written to:  " + newClinicalFile.getAbsolutePath());
        writer.flush();
        writer.close();
    }
}