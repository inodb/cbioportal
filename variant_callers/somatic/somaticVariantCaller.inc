ifndef SOMATIC_VARIANT_CALLER_INC
DEPTH_FILTER ?= 5
HRUN ?= false
FFPE_NORMAL_FILTER ?= false
VALIDATION ?= false

VALIDATION_FILTERS := dbsnp eff \
    $(if $(findstring b37,$(REF)),cosmic nsfp)
INDEL_ANN_FILTERS := $(if $(findstring mm10,$(REF)),mgp_dbsnp,dbsnp) \
    eff \
    $(if $(findstring b37,$(REF)),cosmic nsfp gene_ann cn_reg clinvar exac_nontcga)
SNV_ANN_FILTERS := $(INDEL_ANN_FILTERS) \
    $(if $(findstring b37,$(REF)),chasm fathmm)
# apply depth filter to varscan and mutect
# fix vcf sample header for strelka
FILTERS = dp_ft \
    $(if $(findstring varscan,$1)$(findstring mutect,$1),\
    $(if $(findstring true,$(FFPE_NORMAL_FILTER)),ffpe_som_ad_ft,som_ad_ft))
# target filter
FILTERS += $(if $(TARGETS_FILE),target_ft)
# hrun filter
FILTERS += $(if $(findstring indel,$1),\
            $(if $(findstring true,$(HRUN)),hrun hrun_ft))
# pass filter for faster annotations
FILTERS += pass
# indel/snv annotations
FILTERS += $(if $(findstring true,$(VALIDATION)),$(VALIDATION_FILTERS),\
		   $(if $(findstring indel,$1),$(INDEL_ANN_FILTERS),$(SNV_ANN_FILTERS)))

FILTER_SUFFIX = $1.$(subst $( ),.,$(strip $(call FILTERS,$1)))

EFF_TYPES := high_moderate low_modifier synonymous nonsynonymous

VCF_SUFFIXES = $(foreach type,$1,$(call FILTER_SUFFIX,$(type)))

TABLE_EXTENSIONS = tab tab.novel $(foreach eff,$(EFF_TYPES),tab.$(eff) tab.$(eff).novel)

TABLE_SUFFIXES = $(foreach suff,$(call VCF_SUFFIXES,$1),\
				 $(foreach ext,$(TABLE_EXTENSIONS),$(suff).$(ext)))

VCFS = $(foreach pair,$(SAMPLE_PAIRS),\
    $(foreach suff,$(call VCF_SUFFIXES,$1), \
        vcf/$(pair).$(suff).vcf))

TABLES = $(foreach pair,$(SAMPLE_PAIRS),\
    $(foreach suff,$(call TABLE_SUFFIXES,$1),tables/$(pair).$(suff).txt))
TABLES += $(foreach suff,$(call TABLE_SUFFIXES,$1),alltables/allTN.$(suff).txt)
endif
SOMATIC_VARIANT_CALLER_INC = true