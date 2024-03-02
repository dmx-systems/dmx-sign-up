#!/bin/bash
echo "curl -sS --header \"JOB_TOKEN: ${CI_JOB_TOKEN}\" \"https://git.dmx.systems/api/v4/projects/${CI_PROJECT_ID}/pipelines/${CI_PIPELINE_ID}/jobs?scope[]=success\""
RESULT="$( curl -sS --header "JOB_TOKEN: ${CI_JOB_TOKEN}" "https://git.dmx.systems/api/v4/projects/${CI_PROJECT_ID}/pipelines/${CI_PIPELINE_ID}/jobs?scope[]=success" )"
echo "${RESULT}" | -c '.[] | select(.name | contains("maven-build"))'

