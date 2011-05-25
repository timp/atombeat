#!/bin/bash

# create the collection
col=http://localhost:8080/atombeat-exist-minimal-secure/service/content/benchmark-`date +%s`
curl --upload-file feed.xml --user adam:test --header "Content-Type: application/atom+xml;type=feed" --header "Accept: application/atom+xml" $col

# create test data
for i in `seq 1 200`; 
do
  echo $i
  curl --data-binary @media.txt --user audrey:test --header "Content-Type: text/plain" --header "Accept: application/atom+xml" $col >/dev/null 2>&1
done  

echo "collection ready for benchmarking: $col"
