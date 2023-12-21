#!/bin/bash

sleep 1

if [ -z "$1" ]; then
    declare -a USERS=(testuser testuser1 testuser2)
else
    declare -a USERS=($1)
fi

USERNAME='admin'
PASSWORD="${DMX_ADMIN_PASSWORD}"
LDAPPASSWORD='testpass'
#echo "PASSWORD=${PASSWORD}"
if [ -z "${TIER}" ]; then
    export TIER='dev'
fi
if [ -z "${WEB_URL}" ] && [ "${CI_COMMIT_BRANCH}" == "master" -o "${CI_COMMIT_BRANCH}" == "main" ]; then
    WEB_URL="${CI_PROJECT_NAME}-${TIER}.ci.dmx.systems"
elif [ -z "${WEB_URL}" ] && [ "${CI_COMMIT_BRANCH}" != "master" -a "${CI_COMMIT_BRANCH}" == "main" ]; then
    WEB_URL="${CI_COMMIT_REF_SLUG}_${CI_PROJECT_NAME}-${TIER}.ci.dmx.systems"
fi
HOST="https://${WEB_URL}:443"
## Test access to Administration workspace to ensure login as admin was successful.
URL='core/topic/uri/dmx.workspaces.administration'
BASE64="$( echo -n "${USERNAME}:${PASSWORD}" | base64 )"
AUTH="Authorization: Basic ${BASE64}"
#echo "curl -sS -H "${AUTH}" "${HOST}/${URL}" -i 2>&1"
SESSION="$( curl -sS -H "${AUTH}" "${HOST}/${URL}" -i 2>&1 )"
#echo "SESSION = ${SESSION}"
HTTPCODE="$( echo "${SESSION}" | grep HTTP | cut -d' ' -f2 )"

if [ "${HTTPCODE}" != "200" -a "${HTTPCODE}" != "204" ]; then
    echo "ERROR! Login ${USERNAME} at ${HOST} failed! (HTTPCODE=${HTTPCODE})"
    exit 1
else
    SESSIONID="$( echo "${SESSION}" | grep ^Set-Cookie: | cut -d';' -f1 | cut -d'=' -f2 )"
    echo "INFO: Login ${USERNAME} at ${HOST} successful. (SESSIONID=${SESSIONID})"
fi

## create users
## POST /sign-up/user-account/{username}/{emailAddress}/{displayname}/{password}
for user in "${USERS[@]}"; do
    echo "Creating LDAP account for ${user}"
    MAILNAME="$( echo "${user}" | tr '[:upper:]' '[:lower:]' | sed 's/\ /\_/g' )"
    MAILBOX="${MAILNAME}@example.org"
    ## replace space in DISPLAYNAME with encoded space (%20)
    DISPLAYNAME="${user}%20Testuser"
    # LDAPPASSWORDBASE64="$( echo -n "${LDAPPASSWORD}" | base64 )"
    URL="sign-up/user-account/${MAILBOX}/${MAILBOX}/${DISPLAYNAME}/${LDAPPASSWORD}"
    echo "POST ${HOST}/${URL}"
    ## mind "Accept" header!
    RESULT="$( curl -sS -H "Cookie: JSESSIONID=${SESSIONID}" -H "Accept: application/json" -X POST "${HOST}/${URL}" -i 2>&1 )"
    echo "RESULT: ${RESULT}"
done

## test ldap login
USERS+=('thiswontwork')
for user in "${USERS[@]}"; do
    MAILNAME="$( echo "${user}" | tr '[:upper:]' '[:lower:]' | sed 's/\ /\_/g' )"
    MAILBOX="${MAILNAME}@example.org"
    BASE64=$( echo -n "${MAILBOX}:${LDAPPASSWORD}" | base64 )
    AUTH="Authorization: LDAP ${BASE64}"
    ## Test user creation was successful by checking login and accessing the private workspace.
    URL='access-control/user/workspace'
    LOGIN_RESPONSE="$( curl -I -sS -H "${AUTH}" "${HOST}/${URL}" )"
    HTTP_CODE="$( echo "${LOGIN_RESPONSE}" | head -n1 | cut -d' ' -f2 )"
    if [ ${HTTP_CODE} -eq 200 ]; then
        SESSION_ID="$( echo "${LOGIN_RESPONSE}" | grep ^Set-Cookie: | cut -d';' -f1 | cut -d'=' -f2 )"
        echo "INFO: LDAP login ${MAILBOX} successful (id=${SESSION_ID}). (HTTP_CODE=${HTTP_CODE})"
    elif [ "${MAILBOX}" != "thiswontwork@example.org" ]; then
        echo "ERROR! LDAP login ${MAILBOX} failed! (HTTP_CODE=${HTTP_CODE})"
        exit 1
    else
        ## user 'thiswontswork' is expected to fail
        echo "INFO: LDAP login ${MAILBOX} failed as expected. (HTTP_CODE=${HTTP_CODE})"
        exit 0
    fi
done

