#!/bin/bash

mvn install:install-file -Dfile="$EXIST_HOME/lib/core/xmldb.jar" -DgroupId=org.exist-db -DartifactId=exist-xmldb -Dversion=1.4.0 -Dpackaging=jar
mvn install:install-file -Dfile="$EXIST_HOME/exist.jar" -DpomFile=exist-pom.xml
mvn install:install-file -Dfile="$EXIST_HOME/exist-optional.jar" -DpomFile=exist-optional-pom.xml
mvn install:install-file -Dfile="$EXIST_HOME/lib/extensions/exist-modules.jar" -DpomFile=exist-modules-pom.xml
mvn install:install-file -Dfile="$EXIST_HOME/lib/extensions/exist-ngram-module.jar" -DpomFile=exist-ngram-module-pom.xml
mvn install:install-file -Dfile="$EXIST_HOME/lib/extensions/exist-lucene-module.jar" -DpomFile=exist-lucene-module-pom.xml
mvn install:install-file -Dfile="$EXIST_HOME/lib/extensions/exist-versioning.jar" -DpomFile=exist-versioning-module-pom.xml

