#!/bin/bash

# remember to clean the database before running the test
COLLECTIONURI=http://localhost:8080/atombeat/atombeat/content/test
OUTPUT=results.txt

create_member() {
	echo "**************************************************************************" >> results.txt
	echo "* "`date` >> results.txt
	echo "* create members ($1)" >> results.txt
	echo "**************************************************************************" >> results.txt
	ab -n 100 -c 2 -p entry.atom -T application/atom+xml -A adam:test $COLLECTIONURI >> $OUTPUT
}

list_collection() {
	echo "**************************************************************************" >> results.txt
	echo "* "`date` >> results.txt
	echo "* list collection ($1)" >> results.txt
	echo "**************************************************************************" >> results.txt
	ab -n 10 -c 2 -A adam:test $COLLECTIONURI >> $OUTPUT
}

list_collection "0"

for i in {1..5}
do 
	create_member "$i"
	list_collection "$i"
done

exit 0
