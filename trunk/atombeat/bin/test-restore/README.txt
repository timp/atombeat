The general idea is...

1. Start with a clean installation of AtomBeat.

2. Run setup.sh to create a couple of test collections and populate them with members.

3. Crawl the collections using the crawl.sh to a folder named "before".

4. Trigger a full backup via the eXist admin web interface.

5. Copy the backup to a separate location.

6. Stop AtomBeat, clean the eXist database, and restart.

7. Restore the eXist database via the command line, using the backup taken above. 
   N.B., if you are using versioned collections, restore the /db/atombeat collection 
   *before* the /db/system collection, otherwise you may see additional revisions 
   appearing as a consequence of the restoration process.

8. Crawl the collections again using crawl.sh to a folder named "after".

9. Diff the "before" and "after" collections. If there are no differences, backup 
   and restore has worked. Otherwise...

