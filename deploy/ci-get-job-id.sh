#!/bin/bash
echo "curl -sS --header \"JOB_TOKEN: ${CI_JOB_TOKEN}\" \"https://git.dmx.systems/api/v4/projects/${CI_PROJECT_ID}/pipelines/${CI_PIPELINE_ID}/jobs?scope[]=success\""
RESULT="$( curl -sS --header "JOB_TOKEN: ${CI_JOB_TOKEN}" "https://git.dmx.systems/api/v4/projects/${CI_PROJECT_ID}/pipelines/${CI_PIPELINE_ID}/jobs?scope[]=success" )"
MAVEN_BUILD_JOB_ID="$( echo "${RESULT}" | jq -c '.[] | select(.name | contains("maven-build")).id' )"
echo -n "MAVEN_BUILD_JOB_ID=${MAVEN_BUILD_JOB_ID}"
[ ! -z "${MAVEN_BUILD_JOB_ID##*[!0-9]*}" ] && echo " is a number" || echo " is not a number";

#RESULT="$( curl -sS --header "JOB_TOKEN: ${CI_JOB_TOKEN}" "https://git.dmx.systems/api/v4/projects/${CI_PROJECT_ID}/jobs/${MAVEN_BUILD_JOB_ID}" )"
#echo "${RESULT}"