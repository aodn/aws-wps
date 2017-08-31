#!/bin/bash
# mount efs volume 

# run aggregator
java -jar /opt/aggregator/*.jar $*
exit $?

