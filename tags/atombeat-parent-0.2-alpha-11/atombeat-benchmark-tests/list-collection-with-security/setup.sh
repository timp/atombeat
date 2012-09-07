#!/bin/bash

# create the collection
col=http://localhost:8081/atombeat-exist-minimal-secure/service/content/benchmark-`date +%s`
curl --upload-file feed.xml --user adam:test --header "Content-Type: application/atom+xml;type=feed" --header "Accept: application/atom+xml" $col

# create test data
for i in `seq 1 1000`; 
do
  echo $i
  curl --data-binary @entry.xml --user adam:test --header "Content-Type: application/atom+xml" --header "Accept: application/atom+xml" $col >/dev/null 2>&1
done  

echo "collection ready for benchmarking: $col"
