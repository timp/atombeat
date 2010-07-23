# remember to clean the database before running the test
COLLECTIONURI=http://localhost:8080/atombeat/atombeat/content/test
OUTPUT=results.txt

create_member() {
	echo "**************************************************************************" >> results.txt
	echo "* "`date` >> results.txt
	echo "* post 10 entries" >> results.txt
	echo "**************************************************************************" >> results.txt
	ab -n 10 -c 2 -p entry.atom -T application/atom+xml -A adam:test $COLLECTIONURI >> $OUTPUT
}

list_collection() {
	echo "**************************************************************************" >> results.txt
	echo "* "`date` >> results.txt
	echo "* list collection" >> results.txt
	echo "**************************************************************************" >> results.txt
	ab -n 10 -c 2 -A adam:test $COLLECTIONURI >> $OUTPUT
}

for i in {1..3}
do 
	create_member
	list_collection
done

exit 0
