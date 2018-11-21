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
    echo "Usage: $0 [-p AWS_PROFILE] -s STACK_NAME -c CONFIG_FILE [-t \"TAGS\"]"
    exit 1
}

function main() {
    tags=""
    config_file=""
    stack_name=""
    aws_profile=""

    while getopts "p:s:c:t:" opt; do
      case $opt in
        p) aws_profile="$OPTARG"
        ;;
        s) stack_name="$OPTARG"
        ;;
        c) config_file="$OPTARG"
        ;;
        t) tags="$OPTARG"
        ;;
        \?) echo "Invalid option -$OPTARG" >&2
        ;;
      esac
    done

    # Check that we have all the mandatory parameters
    if [ -z "$stack_name" ] || [ -z "$config_file" ]; then
        usage
        exit 2
    fi

    if [ -z "$aws_profile" ] ; then
        aws_profile="default"
    fi

    echo "AWS Profile     : $aws_profile"
    echo "Stack name      : $stack_name"
    echo "Config file     : $config_file"
    echo "Tags            : $tags"

    # Check that the config file exists
    if [ ! -e ${config_file} ]; then
        echo "Config file $config_file does not exist."
        usage
        exit 2
    fi

    # Form the --tags clause if tags were passed
    if [[ ! -z "$tags" ]] ; then
        tags="--tags "$tags; shift
    fi

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
    aws --profile ${aws_profile} cloudformation deploy --template-file ${temporary_template} --stack-name ${stack_name} --parameter-overrides $(cat ${config_file}) --capabilities CAPABILITY_IAM ${tags}
}

main "$@"
