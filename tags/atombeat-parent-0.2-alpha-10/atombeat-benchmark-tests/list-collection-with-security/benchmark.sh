#!/bin/bash -x
ab -n 1 -c 1 -H 'Accept: application/atom+xml' -A adam:test $1
