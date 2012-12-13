#!/usr/bin/perl
# file: moveGDACOverrides.pl
# author: Arthur Goldberg, goldberg@cbio.mskcc.org

use strict;
use warnings;
use File::Spec;
use File::Util;
use Data::Dumper;
use Getopt::Long;

use FirehoseTransformationWorkflow;
        # TODO: ACCESS CONTROL: change to CANCER_STUDY

my $usage = <<EOT;
usage:
moveGDACOverrides.pl

Given a set of override files in a directory, move them into a Firehose data set.

--DeepFirehoseDirectory <Firehose Directory>        # required; directory which stores firehose data
--customFileType                                    # indicator of the custom file type to move (Agilent MRNA, RNA Seq, CNA, MAF)
--customDirectory                                   # directory containing overrides
--RunDate                                           # rundate for the Firehose run
--customFilesToMoveFile <file containing pairs of custom_file cancer_type>
                                                    # required; 
EOT

# args:
# on laptop, for testing:
# --DeepFirehoseDirectory /Users/goldbera/Data/firehose/data/copyOfCurrent/data
# --customDirectory /Users/goldbera/Documents/workspace/cgds/data/MAFs --RunDate 20110327 
# --customFilesToMoveFile /Users/goldbera/Data/firehose/data/copyOfCurrent/specialMAFs.txt
# on buri:
# --DeepFirehoseDirectory /scratch/data/goldberg/firehoseData/tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/tcga4yeo/other/gdacs/gdacbroad --customDirectory /home/goldberg/workspace/sander/cgds/data/MAFs --RunDate 20110421 --customFilesToMoveFile /home/goldberg/workspace/sander/import_and_convert_Firehose_data/config/specialMAFs.txt

my( $customDirectory, $customFileType, $DeepFirehoseDirectory, $runDate, $customFilesToMoveFile );

# make sure to put the customFile file in the proper gdac directory, 
# because that's what convertFirehoseData.pl will use via getLastestVersionOfFile()

# map - key is customFileType, value is gdac dir - file pair
my $customFileProperties = {
	'AGILENT-MRNA' => [ 'gdac.broadinstitute.org_<CANCER>.Merge_transcriptome__agilentg4502a_07_3__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.Level_3.<date><version>', '<CANCER>.transcriptome__agilentg4502a_07_3__unc_edu__Level_3__unc_lowess_normalization_gene_level__data.data.txt'],
	'RNA-SEQ' => [ 'gdac.broadinstitute.org_<CANCER>.Merge_rnaseq__illumina<RNA-SEQ-PLATFORM>_rnaseq__unc_edu__Level_3__gene_expression__data.Level_3.<date><version>', '<CANCER>.rnaseq__illumina<RNA-SEQ-PLATFORM>_rnaseq__unc_edu__Level_3__gene_expression__data.data.txt'],
	'CNA' => [ 'gdac.broadinstitute.org_<CANCER>.CopyNumber_Gistic2.Level_4.<date><version>', 'all_thresholded.by_genes.txt'],
	'LOG2CNA' => [ 'gdac.broadinstitute.org_<CANCER>.CopyNumber_Gistic2.Level_4.<date><version>', 'all_data_by_genes.txt'],
	'MAF' => [ 'gdac.broadinstitute.org_<CANCER>.Mutation_Assessor.Level_4.<date><version>', '<CANCER>.maf.annotated'],
	'SEG' => [ 'gdac.broadinstitute.org_<CANCER>.Merge_snp__genome_wide_snp_6__broad_mit_edu__Level_3__segmented_scna_minus_germline_cnv_hg19__seg.Level_3.<date><version>', '<CANCER>.snp__genome_wide_snp_6__broad_mit_edu__Level_3__segmented_scna_minus_germline_cnv_hg18__seg.seg.txt'],
	'RPPA' => [ 'gdac.broadinstitute.org_<CANCER>.RPPA_AnnotateWithGene.Level_3.<date><version>', '<CANCER>.rppa.txt'],
};

main();
sub main{
	
    # process arg list
    GetOptions (
        "customDirectory=s" => \$customDirectory,
		"customFileType=s" => \$customFileType,
        "DeepFirehoseDirectory=s" => \$DeepFirehoseDirectory,
        "runDate=s" => \$runDate,
        "customFilesToMoveFile=s" => \$customFilesToMoveFile );

	unless(exists($customFileProperties->{$customFileType})) {
		warn "customFileType: $customFileType is not recognized.";
		exit;
	}
        
    my %customFilesToMove;
    my $f = File::Util->new();
    my @tmp = $f->load_file( $customFilesToMoveFile, '--as-lines' );
    foreach (@tmp){
    	my( $file, $cancer ) = split( /\s+/, $_ );
    	$customFilesToMove{$file} = $cancer;
    }

	my $destDir = $customFileProperties->{$customFileType}->[0];
	my $destFile = $customFileProperties->{$customFileType}->[1];

	foreach my $customFile (keys %customFilesToMove){
	    moveGDACOverridesFile( $customFile, $customFilesToMove{$customFile}, $destDir, $destFile );
	}
}

sub moveGDACOverridesFile{
	my( $customFile, $cancer, $destDir, $destFile ) = @_;
	# in override dir, cancer directory is called <CANCER>_tcga
	my $fromFile = File::Spec->catdir( ($customDirectory, $cancer . '_tcga' ), $customFile );

    my $CancersFirehoseDataDir = File::Spec->catfile( $DeepFirehoseDirectory, $cancer, $runDate . '00' );

	# if we are using custom CNA, we will need to move over Log2CNA,
	# lets get latest version before we get next version
	my $latestVersionOfLog2CNAFile;
	if ( $customFileType eq "CNA" ) {
	  $latestVersionOfLog2CNAFile = getLastestVersionOfFile( $CancersFirehoseDataDir, $destDir, "all_data_by_genes.txt", $cancer, $runDate );
	}

	# if we are using custom Log2CNA, we will need to move over cna,
	# lets get latest version before we get next version
	my $latestVersionOfCNAFile;
	if ( $customFileType eq "LOG2CNA" ) {
	  $latestVersionOfCNAFile = getLastestVersionOfFile( $CancersFirehoseDataDir, $destDir, "all_thresholded.by_genes.txt", $cancer, $runDate );
	}

	# if we are using custom CNA or LOG2CNA, copy of amp, dels
	my $latestAmpGenesFile;
	my $latestDelGenesFile;
	my $latestTableAmpFile;
	my $latestTableDelFile;
	if ($customFileType eq "CNA" || $customFileType eq "LOG2CNA") {
	  $latestAmpGenesFile = getLastestVersionOfFile( $CancersFirehoseDataDir, $destDir, "amp_genes.conf_99.txt", $cancer, $runDate );
	  $latestDelGenesFile = getLastestVersionOfFile( $CancersFirehoseDataDir, $destDir, "del_genes.conf_99.txt", $cancer, $runDate );
	  $latestTableAmpFile = getLastestVersionOfFile( $CancersFirehoseDataDir, $destDir, "table_amp.conf_99.txt", $cancer, $runDate );
	  $latestTableDelFile = getLastestVersionOfFile( $CancersFirehoseDataDir, $destDir, "table_del.conf_99.txt", $cancer, $runDate );
	}

	# if we are processing MAF, we will need to move mut sig file,
	# lets get latest version before we get next version
	my $latestVersionOfMutSigFile = getLastestVersionOfFile( $CancersFirehoseDataDir, 'gdac.broadinstitute.org_<CANCER>.Mutation_Significance.Level_4.<date><version>', "<CANCER>.sig_genes.txt", $cancer, $runDate );
	if (defined($latestVersionOfMutSigFile)) {
	  print "latest version of MutSig: $latestVersionOfMutSigFile\n";
	}

    my( $customFileDir, $customFileFile ) = getNextVersionOfFile( $CancersFirehoseDataDir, 
																  $destDir, $destFile,
																  $cancer, $runDate );
    
    my $toFile = File::Spec->catfile( $customFileDir, $customFileFile );
    print "\ncopying:\n", "from: $fromFile\n", "  to: $toFile\n";
	print `wc -l $fromFile`; 
    mkdir( $customFileDir ); system( "cp $fromFile $toFile"); 
    print `cmp  $fromFile $toFile`;

	# if using custom CNA, lets now copy over Log2CNA
	if ( $customFileType eq "CNA") {
	  if (defined($latestVersionOfLog2CNAFile) ) {
		my $newLog2CNAFile = File::Spec->catfile( $customFileDir, "all_data_by_genes.txt" );
		print "\ncopying Log2CNA:\n", "from: $latestVersionOfLog2CNAFile\n", "  to: $newLog2CNAFile\n";
		system( "cp $latestVersionOfLog2CNAFile $newLog2CNAFile"); 
		print `cmp  $latestVersionOfLog2CNAFile $newLog2CNAFile`;
	  }
	  else {
		warn "Copying custom CNA and cannot find Log2CNA data\n";
	  }
	}

	# if using custom LOG2CNA, lets now copy over cna
	if ( $customFileType eq "LOG2CNA") {
	  if (defined($latestVersionOfCNAFile) ) {
		my $newCNAFile = File::Spec->catfile( $customFileDir, "all_thresholded.by_genes.txt" );
		print "\ncopying cna:\n", "from: $latestVersionOfCNAFile\n", "  to: $newCNAFile\n";
		system( "cp $latestVersionOfCNAFile $newCNAFile"); 
		print `cmp  $latestVersionOfCNAFile $newCNAFile`;
	  }
	  else {
		warn "Copying custom Log2CNA and cannot find CNA data\n";
	  }
	}

	if ($customFileType eq "CNA" || $customFileType eq "LOG2CNA") {
	  if (defined($latestAmpGenesFile)) {
		my $newAmpGenesFile = File::Spec->catfile( $customFileDir, "amp_genes.conf_99.txt" );
		system( "cp $latestAmpGenesFile $newAmpGenesFile"); 
	  }
	  if (defined($latestDelGenesFile)) {
		my $newDelGenesFile = File::Spec->catfile( $customFileDir, "del_genes.conf_99.txt" );
		system( "cp $latestDelGenesFile $newDelGenesFile"); 
	  }
	  if (defined($latestTableAmpFile)) {
		my $newTableAmpFile = File::Spec->catfile( $customFileDir, "table_amp.conf_99.txt" );
		system( "cp $latestTableAmpFile $newTableAmpFile"); 
	  }
	  if (defined($latestTableDelFile)) {
		my $newTableDelFile = File::Spec->catfile( $customFileDir, "table_del.conf_99.txt" );
		system( "cp $latestTableDelFile $newTableDelFile"); 
	  }
	}
	
	# must also create a new directory in which the sig_genes.txt file will live
	# and move an existing file if necessary
	if ( $customFileType eq "MAF" ) {

	  my( $mutSigDir, $mutSigFile ) = getNextVersionOfFile( $CancersFirehoseDataDir, 
															'gdac.broadinstitute.org_<CANCER>.Mutation_Significance.Level_4.<date><version>', '<CANCER>.sig_genes.txt',
															$cancer, $runDate );
	  print "making: $mutSigDir\n";
	  mkdir( $mutSigDir );

	  # if a file exists, move it
	  if (defined($latestVersionOfMutSigFile) ) {
		my $mutSigFilename = uc($cancer) . ".sig_genes.txt";
		my $newMutSigFile = File::Spec->catfile( $mutSigDir, $mutSigFilename );
		print "\ncopying mutsig:\n", "from: $latestVersionOfMutSigFile\n", "  to: $newMutSigFile\n";
		system( "cp $latestVersionOfMutSigFile $newMutSigFile"); 
		print `cmp  $latestVersionOfMutSigFile $newMutSigFile`;
	  }
	  else {
		warn "Copying custom Mutation Assessor File (MAF) and cannot find MutSig data\n";
	  }
	}
}

# get a lexicographically later version of a file
sub getNextVersionOfFile{
    my( $CancersFirehoseDataDir, $directoryNamePattern, $fileNamePattern, $cancer, $runDate ) =@_;
	$directoryNamePattern =~ s/<RNA-SEQ-PLATFORM>/hiseq/;
	$fileNamePattern =~ s/<RNA-SEQ-PLATFORM>/hiseq/;
    my $latestVersion = getLastestVersionOfFile( $CancersFirehoseDataDir, $directoryNamePattern, $fileNamePattern, $cancer, $runDate );
    my($volume, $latestDir, $latestFile ) = File::Spec->splitpath( $latestVersion );
    my $nextDir = $latestDir;
	# if we are processing RNA-SEQ and didn't find illuminahiseq,
	# check for illuminaga before creating a new version of illuminahiseq
	unless ( -d $nextDir && $directoryNamePattern =~ /rnaseq/ ) {
	  $directoryNamePattern =~ s/hiseq/ga/;
	  $fileNamePattern =~ s/hiseq/ga/;
	  $latestVersion = getLastestVersionOfFile( $CancersFirehoseDataDir, $directoryNamePattern, $fileNamePattern, $cancer, $runDate );
	  ($volume, $latestDir, $latestFile ) = File::Spec->splitpath( $latestVersion );
	  $nextDir = $latestDir;
	}
	unless ( -d $nextDir) {
		my $customFileDir = File::Spec->catfile($CancersFirehoseDataDir, $directoryNamePattern);
		my $cancer_UC = uc( $cancer );
		$customFileDir =~ s/<CANCER>/$cancer_UC/;
		my $dateVersion = $runDate . "00.0.0";
		$customFileDir =~ s/<date><version>/$dateVersion/;
		if ( -d $customFileDir ) {
		  $nextDir = $customFileDir;
		}
		else {
		  print "cannot find dir to put customFile file, making: $customFileDir\n";
		  $nextDir = File::Util->new->make_dir($customFileDir);
		}
		# latest dir/file did not exist, we need to set latestFile properly here
		$latestFile = $fileNamePattern;
		$latestFile =~ s/<CANCER>/$cancer_UC/;
	}
    # pattern is '.digit.digit/'
    $nextDir =~ /(\d)\.(\d)\/?$/;
    unless( defined($1) and defined($2)){
	  die "Did not match directory pattern correctly on $nextDir.";
    }   
    # arbitrarily increment the last digit 
    my $nextVersionNum = $2 + 1;
    $nextDir =~ s|(\d)\.(\d)\/$|$1.$nextVersionNum|;
    return ($nextDir, $latestFile);
}
