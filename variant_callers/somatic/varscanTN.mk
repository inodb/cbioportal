# Run VarScan on tumour-normal matched pairs
# Detect point mutations
##### DEFAULTS ######


REF ?= hg19
LOGDIR = log/varscan.$(NOW)

SPLIT_CHR ?= true

##### MAKE INCLUDES #####
include ~/share/modules/Makefile.inc

VARSCAN_JAR = $(JARDIR)/VarScan.v2.3.7.jar
VARSCAN_MEM = $(JAVA) -Xmx$1 -jar $(VARSCAN_JAR)
VARSCAN = $(call VARSCAN_MEM,8G)
SEGMENTCNV = $(HOME)/share/scripts/segmentCNV2.R
CGHCALL = $(HOME)/share/scripts/cghCall.R

FP_FILTER = $(PERL) $(HOME)/share/usr/bin/fpfilter.pl
BAM_READCOUNT = $(HOME)/share/usr/bin/bam-readcount

VARSCAN_TO_VCF = $(PERL) $(HOME)/share/scripts/varscanToVcf.pl


MIN_MAP_QUAL ?= 1

MIN_VAR_FREQ ?= 0.05

VPATH ?= bam

.DELETE_ON_ERROR:

.SECONDARY: 

.PHONY: all vcfs copycalls segments cnv

SNP_VCF_EFF_FIELDS += VAF
INDEL_VCF_EFF_FIELDS += VAF

ANN_TYPES = eff # annotated
EFF_TYPES = silent missense nonsilent_cds nonsilent
VARIANT_TYPES = varscan_snps varscan_indels

FILTER_SUFFIX := dp_ft.som_ad_ft
ifdef TARGETS_FILE
FILTER_SUFFIX := $(FILTER_SUFFIX).target_ft
endif
ANN_SUFFIX := pass.dbsnp.eff

VCF_SUFFIX.varscan_snps := $(FILTER_SUFFIX).$(ANN_SUFFIX).nsfp.chasm.transfic

VCF_SUFFIX.varscan_indels := $(FILTER_SUFFIX)
ifeq ($(HRUN),true)
HRUN_FILTER ?= 1
VCF_SUFFIX.varscan_indels := $(VCF_SUFFIX.varscan_indels).hrun.hrun_ft
endif
VCF_SUFFIX.varscan_indels := $(VCF_SUFFIX.varscan_indels).$(ANN_SUFFIX)

VCF_SUFFIXES = $(foreach type,$(VARIANT_TYPES),$(type).$(VCF_SUFFIX.$(type)))
TABLE_SUFFIXES = $(foreach suff,$(VCF_SUFFIXES),$(foreach eff,$(EFF_TYPES),$(suff).tab.$(eff).novel))

VCFS = $(foreach pair,$(SAMPLE_PAIRS),$(foreach suff,$(VCF_SUFFIXES),vcf/$(pair).$(suff).vcf))
TABLES = $(foreach pair,$(SAMPLE_PAIRS),$(foreach suff,$(TABLE_SUFFIXES),tables/$(pair).$(suff).txt))
TABLES += $(foreach suff,$(TABLE_SUFFIXES),alltables/allTN.$(suff).txt)

all : vcfs tables cnv
variants : vcfs tables
cnv : copycalls segments cghcalls
vcfs : $(VCFS)
tables : $(TABLES)
segments : $(foreach pair,$(SAMPLE_PAIRS),varscan/segment/$(pair).segment.Rdata)
copycalls : $(foreach pair,$(SAMPLE_PAIRS),varscan/copycall/$(pair).copycall)
cghcalls : $(foreach pair,$(SAMPLE_PAIRS),varscan/cgh_call/$(pair).cgh_call.txt)


%.Somatic.txt : %.txt
	$(call LSCRIPT_MEM,5G,8G,"$(call VARSCAN_MEM,4G) somaticFilter $< && $(call VARSCAN_MEM,4G) processSomatic $< && rename .txt.Somatic .Somatic.txt $** && rename .txt.Germline .Germline.txt $** && rename .txt.LOH .LOH.txt $** && rename .txt.hc .hc.txt $**")

ifeq ($(SPLIT_CHR),true)
define varscan-somatic-tumor-normal-chr
varscan/chr_tables/$1_$2.$3.varscan_timestamp : bam/$1.bam bam/$2.bam
	$$(call LSCRIPT_MEM,9G,12G,"$$(VARSCAN) somatic \
	<($$(SAMTOOLS) mpileup -r $3 -q $$(MIN_MAP_QUAL) -f $$(REF_FASTA) $$(word 2,$$^)) \
	<($$(SAMTOOLS) mpileup -r $3 -q $$(MIN_MAP_QUAL) -f $$(REF_FASTA) $$<) \
	--min-var-freq $(MIN_VAR_FREQ) --output-indel varscan/chr_tables/$1_$2.$3.indel.txt --output-snp varscan/chr_tables/$1_$2.$3.snp.txt && touch $$@")

varscan/chr_tables/$1_$2.$3.indel.txt : varscan/chr_tables/$1_$2.$3.varscan_timestamp
varscan/chr_tables/$1_$2.$3.snp.txt : varscan/chr_tables/$1_$2.$3.varscan_timestamp

varscan/chr_tables/$1_$2.$3.%.fp_pass.txt : varscan/chr_tables/$1_$2.$3.%.txt bam/$1.bam
	$$(call LSCRIPT_MEM,8G,35G,"$$(FP_FILTER) --output-basename varscan/chr_tables/$1_$2.$3.$$* $$< <($$(BAM_READCOUNT) -f $$(REF_FASTA) $$(word 2,$$^) $3) && head -1 $$< > $$@ && cat varscan/chr_tables/$1_$2.$3.$$*.pass >> varscan/chr_tables/$1_$2.$3.$$*.fp_pass.txt")

endef
$(foreach chr,$(CHROMOSOMES), \
	$(foreach i,$(SETS_SEQ), \
		$(foreach tumor,$(call get_tumors,$(set.$i)), \
			$(eval $(call varscan-somatic-tumor-normal-chr,$(tumor),$(call get_normal,$(set.$i)),$(chr))))))

define merge-varscan-tables
varscan/tables/$1.%.txt : $$(foreach chr,$$(CHROMOSOMES),varscan/chr_tables/$1.$$(chr).%.txt)
	$$(INIT) head -1 $$< > $$@ && for x in $$^; do sed 1d $$$$x >> $$@; done
endef
$(foreach pair,$(SAMPLE_PAIRS),$(eval $(call merge-varscan-tables,$(pair))))

define convert-varscan-tumor-normal
varscan/vcf/$1_$2.%.vcf : varscan/tables/$1_$2.%.txt
	$$(INIT) $$(VARSCAN_TO_VCF) -f $$(REF_FASTA) -t $1 -n $2 $$< | $$(VCF_SORT) $$(REF_DICT) - > $$@

endef
$(foreach i,$(SETS_SEQ), \
	$(foreach tumor,$(call get_tumors,$(set.$i)), \
		$(eval $(call convert-varscan-tumor-normal,$(tumor),$(call get_normal,$(set.$i))))))

else # no splitting by chr

define varscan-somatic-tumor-normal
varscan/tables/$1_$2.varscan_timestamp : bam/$1.bam bam/$2.bam
	$$(call LSCRIPT_MEM,9G,12G,"$$(VARSCAN) somatic \
	<($$(SAMTOOLS) mpileup -q $$(MIN_MAP_QUAL) -f $$(REF_FASTA) $$(word 2,$$^)) \
	<($$(SAMTOOLS) mpileup -q $$(MIN_MAP_QUAL) -f $$(REF_FASTA) $$<) \
	--min-var-freq $(MIN_VAR_FREQ) --output-indel varscan/chr_tables/$1_$2.indel.txt --output-snp varscan/chr_tables/$1_$2.snp.txt && touch $$@")

varscan/tables/$1_$2.indel.txt : varscan/tables/$1_$2.varscan_timestamp
varscan/tables/$1_$2.snp.txt : varscan/tables/$1_$2.varscan_timestamp

varscan/tables/$1_$2.%.fp_pass.txt : varscan/tables/$1_$2.%.txt bamrc/$1.bamrc
	$$(call LSCRIPT_MEM,8G,45G,"$$(FP_FILTER) --output-basename varscan/tables/$1_$2.$$* $$^ && mv varscan/tables/$1_$2.$$*.pass varscan/tables/$1_$2.$$*.fp_pass.txt")

endef
$(foreach i,$(SETS_SEQ),\
	$(foreach tumor,$(call get_tumors,$(set.$i)), \
		$(eval $(call varscan-somatic-tumor-normal,$(tumor),$(call get_normal,$(set.$i))))))
endif

vcf/%.varscan_indels.vcf : varscan/vcf/%.indel.Somatic.vcf
	$(INIT) ln $< $@

vcf/%.varscan_snps.vcf : varscan/vcf/%.snp.Somatic.vcf
	$(INIT) ln $< $@


define varscan-copynum-tumor-normal
varscan/copynum/$1_$2.copynumber :  bam/$1.bam bam/$2.bam
	$$(call LSCRIPT_MEM,9G,12G,"$$(SAMTOOLS) mpileup -q 1 -f $$(REF_FASTA) $$(word 2,$$^) $$< | awk 'NF == 9 { print }' |  $$(VARSCAN) copynumber - $$(basename $$@) --mpileup 1")
endef
$(foreach i,$(SETS_SEQ),\
	$(foreach tumor,$(call get_tumors,$(set.$i)), \
		$(eval $(call varscan-copynum-tumor-normal,$(tumor),$(call get_normal,$(set.$i))))))

varscan/copycall/%.copycall : varscan/copynum/%.copynumber
	$(call LSCRIPT_MEM,9G,12G,"n=`awk '{ total += \$$7 } END { print total / NR }' $<`; \
	if [ \$$(bc <<< \"\$$n > 0\") -eq 1 ]; then \
		recenter_opt=\"--recenter-up \$$n\"; \
	else \
		n=\$$(bc <<< \"\$$n*-1\"); \
		recenter_opt=\"--recenter-down \$$n\"; \
	fi; \
	$(VARSCAN) copyCaller $< --output-file $@ \$$recenter_opt")

varscan/segment/%.segment.Rdata : varscan/copycall/%.copycall
	$(call LSCRIPT_MEM,4G,6G,"$(RSCRIPT) $(SEGMENTCNV) --centromereFile=$(CENTROMERE_TABLE2) --prefix=$(@D)/$* $<")

varscan/cgh_call/%.cgh_call.txt : varscan/segment/%.segment.Rdata
	$(call LSCRIPT_MEM,4G,6G,"$(RSCRIPT) $(CGHCALL) --centromereFile=$(CENTROMERE_TABLE2) --prefix=$(@D)/$* $<")

define bamrc-chr
bamrc/%.$1.chr_bamrc : bam/%.bam
	$$(call LSCRIPT_MEM,2G,3G,"$$(BAM_READCOUNT) -f $$(REF_FASTA) $$< $1 > $$@")
endef
$(foreach chr,$(CHROMOSOMES),$(eval $(call bamrc-chr,$(chr))))


include ~/share/modules/variant_callers/gatk.mk
