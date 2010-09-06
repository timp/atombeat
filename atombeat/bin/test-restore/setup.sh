#!/bin/bash -x

DATA_DIR=data
TMP_DIR=tmp
BASE_URL=http://localhost:8081/atombeat/atombeat/content
NVC_URL=${BASE_URL}/non-versioned-collection
VC_URL=${BASE_URL}/versioned-collection
USER=adam:test


test -d ${TMP_DIR} || mkdir ${TMP_DIR}


function populate_collection {
	
	local COLLECTION_URL=$1

	# create a member

	curl --basic --fail --user ${USER} --data-binary @${DATA_DIR}/cds.entry.xml --header "Content-Type: application/atom+xml" ${COLLECTION_URL}
	test $? == 0 || { echo failed to create member ; exit 1 ; }

	# create and update a member

	curl --basic --fail --user ${USER} --data-binary @${DATA_DIR}/plants.entry.xml --header "Content-Type: application/atom+xml" --output ${TMP_DIR}/plants-created-response.xml ${COLLECTION_URL}
	test $? == 0 || { echo failed to create member ; exit 1 ; }

	PLANTS_LOCATION=`xmlstarlet sel -N atom=http://www.w3.org/2005/Atom -t -v /atom:entry/atom:link[@rel=\'edit\']/@href ${TMP_DIR}/plants-created-response.xml`

	curl --basic --fail --user ${USER} --upload-file ${DATA_DIR}/plants-modified.entry.xml --header "Content-Type: application/atom+xml" ${PLANTS_LOCATION}
	test $? == 0 || { echo failed to update member ; exit 1 ; }

	# create and delete a member

	curl --basic --fail --user ${USER} --data-binary @${DATA_DIR}/food.entry.xml --header "Content-Type: application/atom+xml" --output ${TMP_DIR}/food-created-response.xml ${COLLECTION_URL}
	test $? == 0 || { echo failed to create member ; exit 1 ; }

	FOOD_LOCATION=`xmlstarlet sel -N atom=http://www.w3.org/2005/Atom -t -v /atom:entry/atom:link[@rel=\'edit\']/@href ${TMP_DIR}/food-created-response.xml`

	curl --basic --fail --user ${USER} --request DELETE ${FOOD_LOCATION}
	test $? == 0 || { echo failed to delete member ; exit 1 ; }

}



function create_collection {

	local COLLECTION_URL=$1
	local FEED_FILE=$2
	
	# first, does the collection already exist?

	curl --basic --fail --user ${USER} ${COLLECTION_URL}

	# curl returns code 22 for http status 400 and above
	# we'll assume 22 means 404

	if [ $? == 22 ]
	then

		# create the collection 
		echo creating collection
		curl --basic --fail --user ${USER} --upload-file ${FEED_FILE} --header "Content-Type: application/atom+xml" ${COLLECTION_URL}
		test $? == 0 || { echo failed to create collection ; exit 1 ; }

	fi

}



create_collection ${NVC_URL} ${DATA_DIR}/non-versioned-collection.feed.xml
for i in {1..3}
do 
	populate_collection ${NVC_URL}
done

create_collection ${VC_URL} ${DATA_DIR}/versioned-collection.feed.xml
for i in {1..3}
do 
	populate_collection ${VC_URL}
done



