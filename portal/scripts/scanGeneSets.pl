#!/usr/bin/perl
require "../scripts/env.pl";

system ("$JAVA_HOME/bin/java -Xmx1524M -cp $cp -DCGDS_HOME='$cgdsHome' org.mskcc.portal.tool.ScanGeneSets @ARGV");
