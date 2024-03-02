#!/bin/bash

TARGET="$1"
if [ "${TARGET}" != "snapshot" ] && [ "${TARGET}" != "release" ]; then
    echo "ERROR! Please enter a valid target: 'snapshot' or 'release'."
    exit 1
else
    echo "INFO: Publishing '${TARGET}' build artifacts for ${CI_PROJECT_NAME}."
fi

## get job id of maven-buil job (required to access artifacts)
RESULT="$( curl -sS --header "JOB_TOKEN: ${CI_JOB_TOKEN}" "https://git.dmx.systems/api/v4/projects/${CI_PROJECT_ID}/pipelines/${CI_PIPELINE_ID}/jobs?scope[]=success" )"
MAVEN_BUILD_JOB_ID="$( echo "${RESULT}" | jq -c '.[] | select(.name | contains("maven-build")).id' )"
if [ ! -z "${MAVEN_BUILD_JOB_ID##*[!0-9]*}" ]; then
    echo "INFO: Found job id from 'maven-build' job. (MAVEN_BUILD_JOB_ID=${MAVEN_BUILD_JOB_ID})"
else
    echo "ERROR! Could not get job id from 'maven-build' job. (MAVEN_BUILD_JOB_ID=${MAVEN_BUILD_JOB_ID})"
    exit 1
fi

## vars
WEBCGI='https://download.dmx.systems/cgi-bin/v1/deploy-version.cgi?'

## plugin vs. platform
if [ "${CI_PROJECT_ROOT_NAMESPACE}" == "dmx-plugins" ]; then
    ARTIFACTS_PATH='target'
    FILE_EXT='jar'
    DOWNLOAD_PATH="${CI_PROJECT_NAME}"
elif [ "${CI_PROJECT_ROOT_NAMESPACE}" == "dmx-platform" ]; then
    ARTIFACTS_PATH='modules/dmx-distribution/target'
    FILE_EXT='zip'
    DOWNLOAD_PATH=""
else
    echo "ERROR! Invalid CI root namespace: CI_PROJECT_ROOT_NAMESPACE != [dmx-plugins|dmx-platform]."
    exit 1
fi

FILE_NAME="$( basename $( ls ${ARTIFACTS_PATH}/dmx-*.${FILE_EXT} | tail -n1 ) )"
APPEND="$( date +%F )_${CI_PIPELINE_ID}"

## snapshot vs. release
if [ "${TARGET}" == "snapshot" ]; then
    PARAMS=("&append=${APPEND}&project=${CI_PROJECT_NAME}&tag=none")
    DESTFILE="$( basename ${FILE_NAME} .${FILE_EXT} )_${APPEND}.${FILE_EXT}"
elif [ "${TARGET}" == "release" ]; then
    PARAMS=("&append=none&project=${CI_PROJECT_NAME}&tag=${CI_COMMIT_TAG}")
    DESTFILE="$( basename ${FILE_NAME} .${FILE_EXT} ).${FILE_EXT}"
else
    echo "ERROR! Invalid target. (TARGET=${TARGET})"
    exit 1
fi

## action
RESULT="$( wget --server-response -q -O - "${WEBCGI}/${CI_PROJECT_PATH}/-/jobs/${BUILD_JOB_ID}/artifacts/raw/${ARTIFACTS_PATH}/${FILE_NAME}${PARAMS}" 2>&1 | head -n1 )"
if [ -z "$( echo "${RESULT}" | grep 200 | grep OK )" ]; then
    echo "ERROR! Failed to trigger download for ${DESTFILE}. (RESULT=${RESULT}"
    exit 1
fi

## check file exists for download
if [ "${TARGET}" == "snapshot" ]; then
    if [ "${CI_PROJECT_ROOT_NAMESPACE}" == "dmx-plugins" ]; then
        DOWNLOAD_URL="https://download.dmx.systems/ci/${CI_PROJECT_NAME}/${DESTFILE}"
    elif [ "${CI_PROJECT_ROOT_NAMESPACE}" == "dmx-platform" ]; then
        DOWNLOAD_URL="https://download.dmx.systems/ci/${DESTFILE}"
    fi
elif [ "${TARGET}" == "release" ]; then
    if [ "${CI_PROJECT_ROOT_NAMESPACE}" == "dmx-plugins" ]; then
        DOWNLOAD_URL="https://download.dmx.systems/${CI_PROJECT_NAME}/${DESTFILE}"
    elif [ "${CI_PROJECT_ROOT_NAMESPACE}" == "dmx-platform" ]; then
        DOWNLOAD_URL="https://download.dmx.systems/${DESTFILE}"
    fi
fi
if [ -z "$( curl -o /dev/null --silent -Iw '%{http_code}' "${DOWNLOAD_URL}" | grep 200 )" ]; then
    echo "ERROR! File not found at ${DOWNLOAD_URL}."
else
    echo "INFO: Successfuly published ${DESTFILE}."
fi

## EOF
