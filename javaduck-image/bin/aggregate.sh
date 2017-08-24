#!/bin/bash
# mount efs volume 

# run aggregator
java -jar /opt/aggregator/aggregation-worker-1.0-SNAPSHOT.jar $*
exit $?

