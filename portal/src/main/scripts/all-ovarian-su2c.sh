# Delete all preprocessed files
rm -v $CGDS_DATA_HOME/ovarian/processed_*

# Load up the Ovarian Meta Data File
./importCancerStudy.pl $CGDS_DATA_HOME/ovarian/ovarian.txt

# Pre-process data for import
# Runs:
# /scripts/ovarian/gen-rae.py
# -->  this script converts RefSeq IDs to Entrez Gene Ids.
# /scripts/ovarian/gen-mrna-expression.py
# -->  this script also converts RefSeq IDs to Entrez Gene Ids.
./ovarian/preprocess-all.py

# Imports All Case Lists
./importCaseList.pl $CGDS_DATA_HOME/ovarian/case_lists

# Imports Clinical Data
./importClinicalData.pl $CGDS_DATA_HOME/ovarian/ova_clinical_20110211.txt

# Imports Mutation Data
./importProfileData.pl --data $CGDS_DATA_HOME/ovarian/data_mutations_extended.txt --meta $CGDS_DATA_HOME/ovarian/meta_mutations_extended.txt --dbmsAction clobber --germlineWhiteList $CGDS_DATA_HOME/ovarian/ovarianGermlineWhiteList.txt

# Imports Copy Number Data
./importProfileData.pl --data $CGDS_DATA_HOME/ovarian/data_CNA.txt --meta $CGDS_DATA_HOME/ovarian/meta_CNA.txt --dbmsAction clobber
./importProfileData.pl --data $CGDS_DATA_HOME/ovarian/data_CNA_RAE.txt --meta $CGDS_DATA_HOME/ovarian/meta_CNA_RAE.txt --dbmsAction clobber
./importProfileData.pl --data $CGDS_DATA_HOME/ovarian/data_log2CNA.txt --meta $CGDS_DATA_HOME/ovarian/meta_log2CNA.txt --dbmsAction clobber

# Imports MRNA Expression Data Files
./importProfileData.pl --data $CGDS_DATA_HOME/ovarian/data_mRNA_median.txt --meta $CGDS_DATA_HOME/ovarian/meta_mRNA_median.txt --dbmsAction clobber
./importProfileData.pl --data $CGDS_DATA_HOME/ovarian/data_mRNA_unified.txt --meta $CGDS_DATA_HOME/ovarian/meta_mRNA_unified.txt --dbmsAction clobber
./importProfileData.pl --data $CGDS_DATA_HOME/ovarian/data_mRNA_median_Zscores.txt --meta $CGDS_DATA_HOME/ovarian/meta_mRNA_median_Zscores.txt --dbmsAction clobber
./importProfileData.pl --data $CGDS_DATA_HOME/ovarian/data_mRNA_unified_Zscores.txt --meta $CGDS_DATA_HOME/ovarian/meta_mRNA_unified_Zscores.txt --dbmsAction clobber

# Imports microRNA Expression Data Files
./importProfileData.pl --data $CGDS_DATA_HOME/ovarian/data_miRNA.txt --meta $CGDS_DATA_HOME/ovarian/meta_miRNA.txt --dbmsAction clobber
./importProfileData.pl --data $CGDS_DATA_HOME/ovarian/data_miRNA_median_Zscores.txt --meta $CGDS_DATA_HOME/ovarian/meta_miRNA_median_Zscores.txt --dbmsAction clobber

# Import Methylation Data
./importProfileData.pl --data $CGDS_DATA_HOME/ovarian/data_methylation.txt --meta $CGDS_DATA_HOME/ovarian/meta_methylation.txt --dbmsAction clobber
./importProfileData.pl --data $CGDS_DATA_HOME/ovarian/data_brca1_binary_methylation.txt --meta $CGDS_DATA_HOME/ovarian/meta_brca1_binary_methylation.txt --dbmsAction clobber

# import ovarian protein data
./importProfileData.pl --data $CGDS_DATA_HOME/ovarian/data_protein.txt --meta $CGDS_DATA_HOME/ovarian/meta_protein.txt --dbmsAction clobber