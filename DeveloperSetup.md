# 1. Branch the Source #

All development work must be done in personal branches.

```
$ svn mkdir https://atombeat.googlecode.com/svn/branches/[yourname] 
$ svn copy https://atombeat.googlecode.com/svn/trunk/parent https://atombeat.googlecode.com/svn/branches/[yourname]/parent
$ svn checkout https://atombeat.googlecode.com/svn/branches/[yourname]/parent my-atombeat
```

**Do not merge changes from your personal branch into the trunk.** If you think your changes are ready to be merged into the trunk, contact [me](mailto:alimanfoo@gmail.com) and I'll review the code and carry out the merge (sorry, I'm a control freak).

If you already have a personal branch checked out, merge changes from the trunk into your branch:

```
$ # review diff
$ svn diff https://atombeat.googlecode.com/svn/branches/[yourname]/parent https://atombeat.googlecode.com/svn/trunk/parent 
$ # do merge
$ svn merge https://atombeat.googlecode.com/svn/branches/[yourname]/parent https://atombeat.googlecode.com/svn/trunk/parent my-atombeat
$ # commit changes
$ cd my-atombeat
$ svn commit -m "merge trunk into my personal branch"
```

# 2. Build the Source #

AtomBeat is built using Maven. Building the source will confirm that everything is working correctly.

```
$ cd my-atombeat
$ export MAVEN_OPTS="-Xmx2048M -XX:MaxPermSize=256M"
$ mvn clean install
```

This will take a few minutes, especially the first time round as maven downloads dependencies.

This will also run all the integration tests against all the different web application packages, which can take time.

If the build is successful, proceed to the next step.

# 3. Setup Eclipse #

Assuming you have Eclipse 3.5 with web tools and m2eclipse, and a Tomcat 6.0 server at localhost configured.

  * File > Import
  * General > Existing Projects into Workspace
  * Select root directory: /path/to/my-atombeat
  * Projects:
    * atombeat-exist-minimal-secure
    * atombeat-integration-tests
    * atombeat-servlet-filters
    * atombeat-workspace
    * atombeat-xquery-functions

Add atombeat-exist-minimal-secure to Tomcat.

Clean and start Tomcat.

Go to http://localhost:8080/atombeat-exist-minimal-secure/ - you should see, "It works!"

Set up a TCP proxy (e.g., tcpwatch or tcpmon) listening on 8081 and forwarding to 8080, e.g.:

```
$ tcpwatch-httpproxy -h -L 8081:8080 &
```

Go to http://localhost:8081/atombeat-exist-minimal-secure/ - you should see, "It works!"

Run the integration tests from within Eclipse:

  * Right click the atombeat-integration-tests project
  * Run As > JUnit Test

All tests should pass.

# 4. Developing with Eclipse #

The main AtomBeat XQuery files are all contained within the atombeat-workspace project. The atombeat-workspace project is overlaid by maven on the atombeat-exist-minimal-secure project.

If you need to make changes to the atombeat-workspace project, you can make these changes directly within the atombeat-workspace project, but to see those changes reflected in the running atombeat-exist-minimal-secure web application you will need to do the following:

```
$ cd my-atombeat
$ cd atombeat-workspace
$ mvn clean install
$ cd ../atombeat-exist-minimal-secure
$ mvn clean package
```

...then refresh the atombeat-exist-minimal-secure project in Eclipse.

This is slow if you're making lots of small changes, so there is a workaround to get instant gratification.

Let's say you want to work on the lib/atomdb.xqm file in atombeat-workspace. Create a folder at src/main/webapp/workspace/lib in the atombeat-exist-minimal-secure project, and copy the file into this directory. Make whatever changes you like to the file at this new location. Any changes will get automatically published to Tomcat by Eclipse. When you're done working on the file, copy the modified file back to the atombeat-workspace project and overwrite the original file, then commit your changes there. Delete the src/main/webapp/workspace/lib in the atombeat-exist-minimal-secure project (don't commit it!).

Finally, note that if you use this workaround, although changes to the XQuery files **will** get automatically published to Tomcat by Eclipse, the eXist XQuery compiler may not detect the changes and recompile the queries. This is a known issue in eXist where there are multiple XQuery import levels. To force eXist to recompile the XQueries, you can either restart Tomcat, or you can make a trivial change to the target/atombeat-exist-minimal-secure/workspace/content.xql file and save it (or whichever XQuery is the root of the import graph) - eXist will notice the change and recompile the query, without needing a Tomcat restart.