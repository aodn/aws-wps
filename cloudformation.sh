#!/bin/bash

set -eu

declare -A BUCKET_MAP=( ["104044260116"]="imos-deploy-cache-prod" ["615645230945"]="imos-binary-dev" )

function get_account_id() {
    local account_id=$(aws sts get-caller-identity 2>/dev/null | jq --raw-output '.Account' 2>/dev/null)
    echo ${account_id}
}

function get_cache_bucket_for_account() {
    local account_id=$1
    local bucket_name=${BUCKET_MAP[${account_id}]}
    echo ${bucket_name}
}

function usage() {
    echo "Usage: $0 STACK_NAME PROPERTIES_FILE"
    exit 1
}

function main() {
    # parameter validation
    [ $# -ne 2 ] && usage
    local stack_name=$1; shift
    local properties_file=$1; shift
    [ ! -e ${properties_file} ] && usage

    # resolve the appropriate cache bucket based on the account ID of the current credentials
    account_id=$(get_account_id)
    cache_bucket=$(get_cache_bucket_for_account ${account_id})

    if [ "z${cache_bucket}" == "z" ]; then
        echo "ERROR: unable to determine deployment cache bucket"
        exit 2
    fi

    # reserve a temporary file for the *patched* template produced by the package command
    temporary_template=$(mktemp --tmpdir=. --suffix=.yaml)
    trap "rm -f ${temporary_template}" EXIT

    aws cloudformation package --template-file ./wps-cloudformation-template.yaml --s3-bucket ${cache_bucket} --s3-prefix lambda --output-template-file ${temporary_template}
    aws cloudformation deploy --template-file ${temporary_template} --stack-name ${stack_name} --parameter-overrides $(cat ${properties_file}) --capabilities CAPABILITY_IAM
}

main $@
