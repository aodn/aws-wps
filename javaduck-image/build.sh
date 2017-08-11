#!/bin/sh
eval "$(aws ecr get-login --no-include-email --region us-east-1)"
docker build -t javaduck .
docker tag javaduck:latest 104044260116.dkr.ecr.us-east-1.amazonaws.com/javaduck:latest
docker push 104044260116.dkr.ecr.us-east-1.amazonaws.com/javaduck:latest



