#!/bin/bash

tempfile=$(mktemp)
cat << SETVAR  > $tempfile
Resources:
  awswps:
    CloudformationTemplateURL: '$1/wps-cloudformation-template.yaml'
    Parameters:
      dockerImage: '615645230945.dkr.ecr.ap-southeast-2.amazonaws.com/javaduck:$2'
      geoserver: 'https://geoserver-123.aodn.org.au/geoserver/imos/ows'
      templatesURL: 'https://raw.githubusercontent.com/aodn/geoserver-config/production/wps/templates.xml'
      administratorEmail: 'developers@emii.org.au'
      geonetworkCatalogueURL: 'https://catalogue.aodn.org.au/geonetwork'
      sourceArn: 'arn:aws:ses:us-east-1:615645230945:identity/aodn.org.au'
      AllowEphemeralBuckets: 'true'
    Endpoint: ''

Lambdas:
  - $1/lambda/job-status-service-lambda-package.zip
  - $1/lambda/request-handler-lambda-package.zip

parent_stack_enabled: false

SETVAR

echo "Yaml configuration write to $tempfile"
export AWS_PROFILE=nonproduction-admin

if [ -f bin/stackman ]; then
  bin/stackman deploy --stack-name $2-wps --stack-profile $tempfile
else
  echo "You must run this script in the root folder of cloud-deploy"
fi

# Clean up file
rm "$tempfile"