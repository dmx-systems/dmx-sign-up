#!/bin/bash

## calculates a port number from a domain name
## jpn - 20221015

if [ -z "$1" ]; then
    exit 1
fi
port="$( echo -n "$1" | sum | cut -d' ' -f1 )"
port="$(( ${port} + 10000 ))"
while [ ${port} -gt 65565 ]; do
    port="$(( ${port} - 10000 ))"
done
echo "${port}"
