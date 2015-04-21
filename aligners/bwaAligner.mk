# vim: set ft=make :
# BWA alignment of short reads
# OPTIONS: NO_MARKDUP = true/false (default: false)
# 		   EXTRACT_FASTQ = true/false (default: false)
# 		   NO_RECAL = true/false (default: false)

include modules/Makefile.inc
include modules/variant_callers/gatk.inc
include modules/aligners/align.inc

ALIGNER := bwa
LOGDIR ?= log/bwa.$(NOW)

SAMTOOLS_SORT_MEM = 2000000000
SEQ_PLATFORM = illumina

VPATH ?= unprocessed_bam

# use fastq; otherwise use bams
FASTQ_CHUNKS := 10
FASTQ_CHUNK_SEQ := $(shell seq 1 $(FASTQ_CHUNKS))
FASTQUTILS = $(HOME)/share/usr/ngsutils/bin/fastqutils

BWA_ALN_OPTS ?= 
#BWA_ALN_OPTS ?= -q 20

.SECONDARY:
.DELETE_ON_ERROR: 
.PHONY: bwa splits
..DUMMY := $(shell mkdir -p version; $(BWA) &> version/bwa.txt; echo "options: $(BWA_ALN_OPTS)" >> version/bwa.txt )


BWA_BAMS = $(foreach sample,$(SAMPLES),bam/$(sample).bam)
bwa : $(addsuffix .md5,$(BWA_BAMS)) $(addsuffix .bai,$(BWA_BAMS))
splits : $(foreach sample,$(SPLIT_SAMPLES),$(foreach split,$(split_lookup.$(sample)),bwa/bam/$(split).bwa.sorted.bam.md5))

bam/%.bam.md5 : bwa/bam/%.bwa.$(BAM_SUFFIX).md5
	$(call LSCRIPT,"ln -f $(<M) $(@M) && $(MD5)")

ifdef SPLIT_SAMPLES
define merged-bam
ifeq ($(shell echo "$(words $2) > 1" | bc),1)
bwa/bam/$1.header.sam : $$(foreach split,$2,bwa/bam/$$(split).bwa.sorted.bam.md5)
	$$(INIT) $$(SAMTOOLS) view -H $$(<M) | grep -v '^@RG' > $$@.tmp; \
	for bam in $$(^M); do $$(SAMTOOLS) view -H $$$$bam | grep '^@RG' >> $$@.tmp; done; \
	uniq $$@.tmp > $$@ && $$(RM) $$@.tmp

bwa/bam/$1.bwa.sorted.bam.md5 : bwa/bam/$1.header.sam $$(foreach split,$2,bwa/bam/$$(split).bwa.sorted.bam.md5)
	if [ `echo "$$(filter %.bam,$$(^M))" | wc -w` -gt 1 ]; then \
		$$(call LSCRIPT_MEM,12G,15G,"$$(SAMTOOLS) merge -f -h $$< $$(@M) $$(filter %.bam,$$(^M)) && $$(MD5) && $$(RM) $$(^M) $$^"); \
	else \
		ln -f $$(word 2,$$(^M)) $$(@M) && ln -f $$(word 2,$$^) $$@; \
	fi
endif
ifeq ($(shell echo "$(words $2) == 1" | bc),1)
bwa/bam/$1.bwa.bam.md5 : bwa/bam/$2.bwa.bam.md5
	$$(INIT) mv $$(<M) $$(@M) && $$(MD5)
endif
endef
$(foreach sample,$(SPLIT_SAMPLES),$(eval $(call merged-bam,$(sample),$(split_lookup.$(sample)))))
endif


fastq/%.fastq.gz.md5 : fastq/%.fastq
	$(call LSCRIPT,"gzip -c $< > $(@M) && $(RM) $< && $(MD5)")

bwa/sai/%.sai.md5 : fastq/%.fastq.gz.md5
	$(call LSCRIPT_PARALLEL_MEM,8,1G,1.2G,"$(CHECK_MD5) $(BWA) aln $(BWA_ALN_OPTS) -t 8 $(REF_FASTA) $(<M) > $(@M) 2> $(LOG) && $(MD5)")
#echo "$(BWA) aln -t 8 $(REF_FASTA) $(<:.md5=) > $(@:.md5=) 2> $(LOG) && $(MD5)" | $(call LSCRIPT_PARALLEL_MEM,8,1G,1.2G)

bwa/bam/%.bwa.bam.md5 : bwa/sai/%.1.sai.md5 bwa/sai/%.2.sai.md5 fastq/%.1.fastq.gz.md5 fastq/%.2.fastq.gz.md5
	LBID=`echo "$*" | sed 's/_[A-Za-z0-9]\+//'`; \
	$(call LSCRIPT_MEM,4G,10G,"$(CHECK_MD5) $(BWA) sampe -P -r \"@RG\tID:$*\tLB:$${LBID}\tPL:${SEQ_PLATFORM}\tSM:$${LBID}\" $(REF_FASTA) $(^M) 2> $(LOG) | $(SAMTOOLS) view -bhS - > $(@M) && $(MD5)")

include modules/bam_tools/processBam.mk
include modules/fastq_tools/fastq.mk