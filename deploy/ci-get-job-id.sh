#!/bin/bash

RESULT="$( curl -sS --header "JOB_TOKEN: ${CI_JOB_TOKEN}" "https://git.dmx.systems/api/v4/projects/${CI_PROJECT_ID}/pipelines/${CI_PIPELINE_ID}/jobs?scope[]=success" )"
MAVEN_BUILD_JOB_ID="$( echo "${RESULT}" | jq -c '.[] | select(.name | contains("maven-build")).id' )"
if [ ! -z "${MAVEN_BUILD_JOB_ID##*[!0-9]*}" ]; then
    echo "INFO: Found job id from maven build job. (MAVEN_BUILD_JOB_ID=${MAVEN_BUILD_JOB_ID})"
else
    echo "ERROR! Could not get job id from maven build job."
    exit 1
fi
