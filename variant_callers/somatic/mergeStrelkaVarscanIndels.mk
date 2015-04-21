# Merge strelka and varscan indel results
LOGDIR = log/merge_strelka_varscan_indels.$(NOW)

include modules/Makefile.inc
include modules/config.inc
include modules/variant_callers/gatk.inc
include modules/variant_callers/somatic/somaticVariantCaller.inc

#VCF_GEN_IDS = DP FDP SDP SUBDP AU CU GU TU TAR TIR TOR
#VCF_FIELDS += QSS TQSS NT QSS_NT TQSS_NT SGT SOMATIC
INDEL_VCF_EFF_FIELDS += VAF

.DELETE_ON_ERROR:
.SECONDARY: 
.PHONY : strelka_varscan_merge strelka_varscan_merge_vcfs strelka_varscan_merge_tables

strelka_varscan_merge : strelka_varscan_merge_vcfs strelka_varscan_merge_tables
strelka_varscan_merge_vcfs : $(foreach pair,$(SAMPLE_PAIRS),vcf/$(pair).strelka_varscan_indels.vcf)
strelka_varscan_merge_tables : $(foreach pair,$(SAMPLE_PAIRS),\
	$(foreach ext,$(TABLE_EXTENSIONS),tables/$(pair).strelka_varscan_indels.$(ext).txt))

vcf/%.strelka_varscan_indels.vcf : vcf/%.$(call VCF_SUFFIXES,varscan_indels).vcf vcf/%.$(call VCF_SUFFIXES,strelka_indels).vcf
	$(call LSCRIPT_MEM,9G,12G,"grep -P '^#' $< > $@ && $(BEDTOOLS) intersect -a $< -b $(<<) >> $@")

include modules/vcf_tools/vcftools.mk