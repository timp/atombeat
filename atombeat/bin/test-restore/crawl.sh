#!/bin/bash -x

CRAWL_DIR=$1

test -z $CRAWL_DIR && { echo "missing required argument crawl directory" ; exit 1 ; }

CONTENT_DIR=${CRAWL_DIR}/content
HISTORY_DIR=${CRAWL_DIR}/history
DATA_DIR=data
TMP_DIR=tmp
BASE_URL=http://localhost:8081/atombeat/atombeat/content
NVC_URL=${BASE_URL}/non-versioned-collection
VC_URL=${BASE_URL}/versioned-collection
USER=adam:test


echo crawling content to ${CRAWL_DIR}

rm -R ${CRAWL_DIR}
mkdir ${CRAWL_DIR}
mkdir ${CONTENT_DIR}
mkdir ${HISTORY_DIR}



function crawl_collection {

	local COLLECTION_URL=$1
	local COL_DIR=$2
	
	mkdir $COL_DIR

	curl --basic --fail --user ${USER} --output ${COL_DIR}/feed.xml ${COLLECTION_URL}
	
	local ENTRIES=`xmlstarlet sel -N atom=http://www.w3.org/2005/Atom --indent --template --match /atom:feed/atom:entry/atom:link[@rel=\'edit\'] --nl --value-of ./@href ${COL_DIR}/feed.xml`

	for e in $ENTRIES 
	do 
		f=`expr match "$e" '.*/\([^/]*\)'`
		o=${COL_DIR}/${f}.xml
		curl --basic --fail --user ${USER} --output $o $e
	done

}



function crawl_versioned_collection {

	local COLLECTION_URL=$1
	local COL_DIR=$2
	local COL_HIST_DIR=$3
	
	mkdir $COL_DIR
	mkdir $COL_HIST_DIR

	curl --basic --fail --user ${USER} --output ${COL_DIR}/feed.xml ${COLLECTION_URL}
	
	local ENTRIES=`xmlstarlet sel -N atom=http://www.w3.org/2005/Atom --indent --template --match /atom:feed/atom:entry/atom:link[@rel=\'edit\'] --nl --value-of ./@href ${COL_DIR}/feed.xml`

	for e in $ENTRIES 
	do 

		f=`expr match "$e" '.*/\([^/]*\)'`
		o=${COL_DIR}/${f}.xml
		curl --basic --fail --user ${USER} --output $o $e

		# need to retrieve history feed and crawl
		hurl=`xmlstarlet sel -N atom=http://www.w3.org/2005/Atom --indent --template --value-of /atom:entry/atom:link[@rel=\'history\']/@href ${o}`
		echo "history - $hurl"
		hdir=${COL_HIST_DIR}/$f
		crawl_collection $hurl $hdir

	done

}



# crawl the non-versioned collection

COL_DIR=${CONTENT_DIR}/non-versioned-collection
COLLECTION_URL=${NVC_URL}

crawl_collection $COLLECTION_URL $COL_DIR



# crawl the versioned collection

COL_DIR=${CONTENT_DIR}/versioned-collection
COL_HIST_DIR=${HISTORY_DIR}/versioned-collection
COLLECTION_URL=${VC_URL}

crawl_versioned_collection $COLLECTION_URL $COL_DIR $COL_HIST_DIR


