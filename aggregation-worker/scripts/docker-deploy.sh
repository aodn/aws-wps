#!/bin/bash
set -ex
tag=${1:-latest}
aws_profile=${2:-nonproduction-admin}

export AWS_PROFILE=${aws_profile}

# Change your local build docker image from javaduck:latest to javaduck:${tag}
docker tag javaduck:latest 615645230945.dkr.ecr.ap-southeast-2.amazonaws.com/javaduck:${tag}

aws ecr get-login-password --region ap-southeast-2 | docker login --username AWS --password-stdin 615645230945.dkr.ecr.ap-southeast-2.amazonaws.com
docker push 615645230945.dkr.ecr.ap-southeast-2.amazonaws.com/javaduck:${tag}
