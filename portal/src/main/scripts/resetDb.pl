#!/usr/bin/perl
require "env.pl";

system ("$JAVA_HOME/bin/java -Xmx1524M -cp $cp -DCGDS_HOME='$cgdsHome' org.mskcc.cbio.cgds.scripts.ResetDatabase @ARGV");

print "$cp\n";
