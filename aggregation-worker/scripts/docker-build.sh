#!/bin/bash
set -ex
eval "$(aws ecr get-login --no-include-email --region us-east-1)"
docker build -t javaduck .
