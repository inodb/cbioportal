#!/usr/bin/env bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "TEST TARGETED SEQUENCING test_data/tseq"
cd ${DIR}/test_data/tseq
echo "==bwamem=="
echo "run"
make bwamem && echo "success" || (echo "failed" && exit 1)
echo "check files"
diff -q <(stat -c %s bam/test1T.bam) <(stat -c %s output/bam/test1T.bam) && echo "success" || (echo "failed" && exit 1)

exit 0
