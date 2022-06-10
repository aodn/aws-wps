#!/bin/bash

tempfile=$(mktemp)
cat << SETVAR  > $tempfile
Description: Test instance for aws-wps, save to delete.
Resources:
  awswps:
    CloudformationTemplateURL: '$1/wps-cloudformation-template.yaml'
    Parameters:
      dockerImage: '615645230945.dkr.ecr.ap-southeast-2.amazonaws.com/javaduck:$2'
      geoserver: 'http://geoserver-123.aodn.org.au/geoserver/imos/ows'
      templatesURL: 'https://raw.githubusercontent.com/aodn/geoserver-config/production/wps/templates.xml'
      administratorEmail: 'developers@emii.org.au'
      geonetworkCatalogueURL: 'http://catalogue.aodn.org.au/geonetwork'
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

bin/stackman deploy --stack-name raymond-wps --stack-profile $tempfile

# Clean up file
rm "$tempfile"