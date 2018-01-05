#!/bin/bash

set -eu

function get_account_id() {
    local aws_profile=$1
    local account_id=$(aws --profile $aws_profile sts get-caller-identity --output text --query Account)
    echo -n ${account_id}
}

function get_cache_bucket_for_account() {
    local account_id=$1
    case $account_id in
    104044260116)  # production
        bucket_name=imos-deploy-cache-prod
        ;;
    615645230945) # non-production
        bucket_name=imos-binary-dev
        ;;
    *)
        bucket_name=""
        ;;
    esac
    echo -n ${bucket_name}
}

function usage() {
    echo "Usage: $0 [AWS_PROFILE] STACK_NAME PROPERTIES_FILE"
    exit 1
}

function main() {
    # parameter parsing
    [[ $# -lt 2 || $# -gt 3 ]] && usage
    local aws_profile
    if [[ $# -eq 3 ]] ; then
      aws_profile=$1; shift
    else
      aws_profile="default"
    fi
    local stack_name=$1; shift
    local properties_file=$1; shift
    [ ! -e ${properties_file} ] && usage

    # resolve the appropriate cache bucket based on the account ID of the current credentials
    account_id=$(get_account_id $aws_profile)
    cache_bucket=$(get_cache_bucket_for_account ${account_id})

    if [ "z${cache_bucket}" == "z" ]; then
        echo "ERROR: unable to determine deployment cache bucket"
        exit 2
    fi

    # reserve a temporary file for the *patched* template produced by the package command
    temporary_template=$(mktemp --tmpdir=. --suffix=.yaml)
    trap "rm -f ${temporary_template}" EXIT

    aws --profile ${aws_profile} cloudformation package --template-file ./wps-cloudformation-template.yaml --s3-bucket ${cache_bucket} --s3-prefix lambda --output-template-file ${temporary_template}
    aws --profile ${aws_profile} cloudformation deploy --template-file ${temporary_template} --stack-name ${stack_name} --parameter-overrides $(cat ${properties_file}) --capabilities CAPABILITY_IAM
}

main $@
