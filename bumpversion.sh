#!/usr/bin/env bash

set -eux

# remote branch to which version updates will be pushed
RELEASE_BRANCH=master

get_maven_version() {
  # extract version from pom.xml
  version=$(xmllint --xpath '/*[local-name()="project"]/*[local-name()="version"]/text()' pom.xml)
  echo "${version}"
}

set_maven_version() {
  local suffix="$1"; shift

  # use Maven versions plugin to bump version
  mvn build-helper:parse-version versions:set \
    -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}${suffix} \
    versions:commit
}

update_git() {
  local version=$1; shift
  git fetch --prune --prune-tags
  git add pom.xml '*/pom.xml'
  git commit -m "Jenkins version bump (${version})"
  git tag -a -f -m 'Jenkins: create tag ${version}' ${version}
  git push origin tag ${version}
  git push origin "HEAD:${RELEASE_BRANCH}"
}

main() {
  local mode=$1; shift

  # add a '-dev' suffix to non-release builds, so that non-release artifacts are more easily identifiable
  [ "x${mode}" == "xrelease" ] && suffix=" " || suffix="-dev"

  set_maven_version "$suffix"
  new_version=$(get_maven_version)

  # if run in release mode, update version information on GitHub
  [ "x${mode}" == "xrelease" ] && update_git "${new_version}"

  exit 0
}

main "$@"
