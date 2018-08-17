#!/bin/bash
set -ex
tag=${1:-latest}
aws_profile=${2:-nonproduction-admin}
eval "$(aws ecr get-login --profile ${aws_profile} --no-include-email --region ap-southeast-2)"
docker tag javaduck:latest 615645230945.dkr.ecr.ap-southeast-2.amazonaws.com/javaduck:${tag}
docker push 615645230945.dkr.ecr.ap-southeast-2.amazonaws.com/javaduck:${tag}
