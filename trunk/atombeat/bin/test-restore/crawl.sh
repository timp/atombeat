#!/bin/bash -x

CRAWL_DIR=$1

test -z $CRAWL_DIR && { echo "missing required arguments: [1] crawl directory" ; exit 1 ; }

CONTENT_DIR=${CRAWL_DIR}/content
HISTORY_DIR=${CRAWL_DIR}/history
DATA_DIR=data
TMP_DIR=tmp
BASE_URL=http://localhost:8080/atombeat/atombeat/content
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
	local COLLECTION_DIR=$2
	local REL=$3
	
	# default to following 'edit' links in feed entries
	test -z $REL && REL=edit
	echo $REL
	
	mkdir $COLLECTION_DIR

	curl --basic --fail --user ${USER} --output ${COLLECTION_DIR}/feed.xml ${COLLECTION_URL}
	
	local ENTRIES=`xmlstarlet sel -N atom=http://www.w3.org/2005/Atom --indent --template --match /atom:feed/atom:entry/atom:link[@rel=\'${REL}\'] --nl --value-of ./@href ${COLLECTION_DIR}/feed.xml`
	
	echo $ENTRIES

	for e in $ENTRIES 
	do 
		f=`expr match "$e" '.*/\([^/]*\)'`
		o=${COLLECTION_DIR}/${f}.xml
		curl --basic --fail --user ${USER} --output $o $e
	done

}



function crawl_versioned_collection {

	local COLLECTION_URL=$1
	local COLLECTION_DIR=$2
	local COLLECTION_HISTORY_DIR=$3
	
	mkdir $COLLECTION_DIR
	mkdir $COLLECTION_HISTORY_DIR

	curl --basic --fail --user ${USER} --output ${COLLECTION_DIR}/feed.xml ${COLLECTION_URL}
	
	local ENTRIES=`xmlstarlet sel -N atom=http://www.w3.org/2005/Atom --indent --template --match /atom:feed/atom:entry/atom:link[@rel=\'edit\'] --nl --value-of ./@href ${COLLECTION_DIR}/feed.xml`

	for e in $ENTRIES 
	do 

		f=`expr match "$e" '.*/\([^/]*\)'`
		o=${COLLECTION_DIR}/${f}.xml
		curl --basic --fail --user ${USER} --output $o $e

		# need to retrieve history feed and crawl 'this-revision' links
		hurl=`xmlstarlet sel -N atom=http://www.w3.org/2005/Atom --indent --template --value-of /atom:entry/atom:link[@rel=\'history\']/@href ${o}`
		echo "history - $hurl"
		hdir=${COLLECTION_HISTORY_DIR}/$f
		crawl_collection $hurl $hdir this-revision

	done

}



# crawl the non-versioned collection

COLLECTION_DIR=${CONTENT_DIR}/non-versioned-collection
COLLECTION_URL=${NVC_URL}

crawl_collection $COLLECTION_URL $COLLECTION_DIR



# crawl the versioned collection

COLLECTION_DIR=${CONTENT_DIR}/versioned-collection
COLLECTION_HISTORY_DIR=${HISTORY_DIR}/versioned-collection
COLLECTION_URL=${VC_URL}

crawl_versioned_collection $COLLECTION_URL $COLLECTION_DIR $COLLECTION_HISTORY_DIR


