#!/bin/bash
set -ex
tag=${1:-latest}
eval "$(aws ecr get-login --no-include-email --region ap-southeast-2)"
docker tag javaduck:latest 615645230945.dkr.ecr.ap-southeast-2.amazonaws.com/javaduck:${tag}
docker push 615645230945.dkr.ecr.ap-southeast-2.amazonaws.com/javaduck:${tag}
