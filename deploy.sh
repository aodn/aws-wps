#!/bin/bash
aws cloudformation deploy --template-file wps-cloudformation-template.yaml --stack-name $1 --parameter-overrides  $(cat $1.properties) --capabilities CAPABILITY_IAM
