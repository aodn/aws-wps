#!/bin/bash
aws cloudformation package --template-file ./wps-cloudformation-template.yaml --s3-bucket imos-binary-dev --s3-prefix lambda --output-template-file $1-temp.yaml

aws cloudformation deploy --template-file $1-temp.yaml --stack-name $1 --parameter-overrides $(cat $1.properties) --capabilities CAPABILITY_IAM