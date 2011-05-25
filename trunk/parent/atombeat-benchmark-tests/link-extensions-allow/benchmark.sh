#!/bin/bash -x
ab -n 1 -c 1 -H 'Accept: application/atom+xml' -A adam:test $1
ab -n 1 -c 1 -H 'Accept: application/atom+xml' -A rebecca:test $1
ab -n 1 -c 1 -H 'Accept: application/atom+xml' -A audrey:test $1
