

# Introduction #

This tutorial provides an introduction to AtomBeat's support for access control.

We commonly found we were not able to fit our security requirements into the unix model of one user and one group per resource (which eXist implements natively). We typically found that we wanted to set different access rules for two or more different roles or groups of users, e.g., we wanted to be able to configure a collection to allow "authors" to create new members but only update their own members, to allow "editors" to update any member, to allow "readers" to retrieve any member, and to allow authors to grant permission to update their members to other specified authors.

To support this kind of scenario, based on the approaches used in NFS and the WebDav ACL extension, we developed a security plugin for AtomBeat's Atom protocol engine, which can be configured to intercept all incoming requests. AtomBeat's security plugin has support for fine-grained access control policies via access control lists (ACLs). ACLs can be defined at workspace, collection and member levels, with configurable precedence. Atom protocol operations can be allowed or denied based on users, roles and groups. The security plugin also enables you to retrieve and update access control lists via HTTP using a subset of the Atom Protocol.

**Please note that AtomBeat is a work in progress and comes with no warranty. The security plugin may have bugs - use at your own risk.**

# Prerequisites #

You will need to have a servlet container like [Tomcat](http://tomcat.apache.org/download-60.cgi) or [Jetty](http://docs.codehaus.org/display/JETTY/Downloading+Jetty) installed on your computer, and you will need to know how to deploy a WAR file. AtomBeat is tested with Tomcat 6.0 and Jetty 6.1.

You will need to have the cURL HTTP command-line utility installed on your computer. If you're on a Linux computer, you probably already have curl installed, or can install it via a software repository, e.g.:

```
sudo apt-get install curl
```

If you are on a Windows or Mac computer, you can [download cURL](http://curl.haxx.se/download.html) and install it manually.

OPTIONAL: You might also like to install a TCP proxy so you can observe the communication between the client (cURL) and server (AtomBeat). There are a couple of options here. You can download and install [tcpmon](https://tcpmon.dev.java.net/) on any operating system. You can also install a very simple utility called [tcpwatch](http://hathawaymix.org/Software/TCPWatch), e.g.:

```
sudo apt-get install tcpwatch-httpproxy
tcpwatch-httpproxy -h -L 8081:8080 &
```

This tutorial assumes you have your servlet container installed and listening on port 8080.

This tutorial also assumes you have a TCP proxy installed and running, listening on port 8081 and forwarding to port 8080. If you are doing this tutorial **without** a TCP proxy, replace "8081" with "8080" wherever you see it below.

# Downloading and Installing AtomBeat #

We are going to be downloading and installing one of the **security-enabled** WAR packages available for AtomBeat. Specifically, we are going to be using the **atombeat-exist-minimal-secure** WAR package. This package is a web application containing an AtomBeat service, overlaid with a cut-down version of the [eXist](http://exist.sourceforge.net/) web application, and with the AtomBeat security plugin enabled and pre-configured with some example settings.

N.B., you will need to install atombeat-exist-minimal-secure **version 0.2-alpha-4 or later** to follow this tutorial. (Most, but not all, of this tutorial will still work with earlier versions.)

Please note that WAR packages in the 0.2 series are **not** available from the Google Code project downloads page. To obtain a WAR package, you can either download directly from the [CGGH maven repository](http://cloud1.cggh.org/maven2/org/atombeat/), e.g.:

```
wget http://cloud1.cggh.org/maven2/org/atombeat/atombeat-exist-minimal-secure/0.2-alpha-4/atombeat-exist-minimal-secure-0.2-alpha-4.war
```

...or you can check out and build it yourself (currently only works on Linux), e.g.:

```
svn checkout http://atombeat.googlecode.com/svn/tags/atombeat-parent-0.2-alpha-4
cd atombeat-parent-0.2-alpha-4
export MAVEN_OPTS="-Xmx1024M -XX:MaxPermSize=256M"
mvn install # might take a while first time
```

This tutorial assumes you have downloaded the atombeat-exist-minimal-secure WAR package version 0.2-alpha-4 or later and deployed it to a local Tomcat or Jetty server running on port 8080, and that **you have deployed the WAR at the context path `/atombeat`**. E.g., you might do something like:

```
wget http://cloud1.cggh.org/maven2/org/atombeat/atombeat-exist-minimal-secure/0.2-alpha-4/atombeat-exist-minimal-secure-0.2-alpha-4.war
sudo unzip atombeat-exist-minimal-secure-0.2-alpha-4.war -d /opt/atombeat-exist-minimal-secure-0.2-alpha-4
sudo rm /var/lib/tomcat6/webapps/atombeat # remove previous link if already there
sudo ln -s /opt/atombeat-exist-minimal-secure-0.2-alpha-4 /var/lib/tomcat6/webapps/atombeat
sudo chown -R tomcat6:tomcat6 /opt/atombeat-exist-minimal-secure-0.2-alpha-4
sudo service tomcat6 restart
tcpwatch-httpproxy -h -L 8081:8080 &
```

To check that the AtomBeat web application is installed and running, go to http://localhost:8081/atombeat/ (or http://localhost:8080/atombeat/ if you're not using a TCP proxy) - you should see a web page saying, "It works!"

If you have problems with any of the above, try the [Getting Started Tutorial](TutorialGettingStarted.md) or [email the AtomBeat google group](mailto:atombeat@googlegroups.com).

# Configuring Collections #

There are several different ways to create an Atom collection in AtomBeat. In this tutorial, we're going to use an administration utility that comes with AtomBeat to create a pre-configured Atom collection. For more information on managing Atom collections, see TODO.

Go to the following link in your browser: http://localhost:8081/atombeat/service/admin/install.xql

You will be challenged for a username and password - use "adam" as the username and "test" as the password.

You should see a page entitled "Atom Collections" and a table listing two collections.

Click the "Install" button. You should see the "Available" column change from "false" to "true". You have just created two Atom collections.

To verify that the Test Collection has been successfully created, click on the [/test](http://localhost:8081/atombeat/service/content/test) link, or go to the following URL: http://localhost:8081/atombeat/service/content/test

If you are using Firefox, you should see the default Firefox feed reader offering to subscribe to the feed, and below that the title of the collection: "Test Collection". What you see in other browsers will vary.

Note that the administration utility you just used to create a collection also installs some default security settings - these are needed for the tutorial to work.

# A Note on Authentication #

This tutorial is **not** about authentication. AtomBeat does **not** have any custom code for implementing authentication. In the `atombeat-*-secure` WAR packages, authentication is implemented using [Spring Security](http://TODO). In these WAR packages there is a simple, example Spring Security configuration, using HTTP basic authentication, with a few predefined users. You can find the default Spring Security configuration in the file `WEB-INF/security-example.xml`.

If you want to use a different authentication configuration, you'll need to modify the Spring Security configuration file, see the [Spring Security documentation](http://TODO) for more information. Spring Security can be configured to use just about any authentication mechanism, and to provision user credentials and authorities from a wide range of sources, including relational databases and LDAP repositories.

# Retrieving a Security Descriptor #

Start by retrieving an Atom feed from the test collection using curl:

```
curl --user adam:test --verbose --header "Accept: application/atom+xml" http://localhost:8081/atombeat/service/content/test
```

You should see an Atom feed document with no entries, e.g.:

```
> GET /atombeat/service/content/test HTTP/1.1
> Authorization: Basic YWRhbTp0ZXN0
> User-Agent: curl/7.19.7 (x86_64-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15
> Host: localhost:8081
> Accept: application/atom+xml
> 
< HTTP/1.1 200 OK
< Server: Apache-Coyote/1.1
< Set-Cookie: JSESSIONID=16FAA94D7CBB3626A357E25452B520F7; Path=/atombeat
< pragma: no-cache
< Cache-Control: no-cache
< Content-Type: application/atom+xml;type=feed;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Wed, 19 Jan 2011 11:18:15 GMT
< 
<atom:feed xmlns:atom="http://www.w3.org/2005/Atom" xmlns:atombeat="http://purl.org/atombeat/xmlns" atombeat:enable-versioning="false" atombeat:exclude-entry-content="false" atombeat:recursive="false">
    <atom:id>http://localhost:8081/atombeat/service/content/test</atom:id>
    <atom:updated>2011-01-19T11:13:00.548Z</atom:updated>
    <atom:author>
        <atom:name>adam</atom:name>
    </atom:author>
    <atom:title type="text">Test Collection</atom:title>
    <app:collection xmlns:app="http://www.w3.org/2007/app" href="http://localhost:8081/atombeat/service/content/test">
        <atom:title type="text">Test Collection</atom:title>
    </app:collection>
    <atom:link rel="self" href="http://localhost:8081/atombeat/service/content/test" type="application/atom+xml;type=feed"/>
    <atom:link rel="edit" href="http://localhost:8081/atombeat/service/content/test" type="application/atom+xml;type=feed"/>
    <atom:link rel="http://purl.org/atombeat/rel/security-descriptor" href="http://localhost:8081/atombeat/service/security/test" type="application/atom+xml;type=entry"/>
</atom:feed>
```

Notice the final `atom:link` element in the feed document, with `rel="http://purl.org/atombeat/rel/security-descriptor"`. The target of this link - http://localhost:8081/atombeat/service/security/test - is the URI of this collection's **security descriptor**. The security descriptor is a document containing an **access control list** (ACL) for the collection, in addition to some other optional security-related information.

Let's retrieve the collection's security descriptor:

```
curl --user adam:test --verbose --header "Accept: application/atom+xml" http://localhost:8081/atombeat/service/security/test
```

You should see an Atom entry document, with some inline XML content, e.g.:

```
> GET /atombeat/service/security/test HTTP/1.1
> Authorization: Basic YWRhbTp0ZXN0
> User-Agent: curl/7.19.7 (x86_64-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15
> Host: localhost:8081
> Accept: application/atom+xml
> 
< HTTP/1.1 200 OK
< Server: Apache-Coyote/1.1
< Set-Cookie: JSESSIONID=751AEFE1B8FC48C9CF7D84B9708C09BA; Path=/atombeat
< pragma: no-cache
< Cache-Control: no-cache
< Content-Type: application/atom+xml;type=entry;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Wed, 19 Jan 2011 11:49:29 GMT
< 
<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <atom:id>http://localhost:8081/atombeat/service/security/test</atom:id>
    <atom:title type="text">Security Descriptor</atom:title>
    <atom:link rel="self" href="http://localhost:8081/atombeat/service/security/test" type="application/atom+xml;type=entry"/>
    <atom:link rel="edit" href="http://localhost:8081/atombeat/service/security/test" type="application/atom+xml;type=entry"/>
    <atom:link rel="http://purl.org/atombeat/rel/secured" href="http://localhost:8081/atombeat/service/content/test" type="application/atom+xml;type=feed"/>
    <atom:updated>2011-01-19T11:13:00.731Z</atom:updated>
    <atom:content type="application/vnd.atombeat+xml">
        <atombeat:security-descriptor xmlns:atombeat="http://purl.org/atombeat/xmlns">
            <atombeat:acl><!--  
            Authors can create entries and media, and can list the collection,
            but can only retrieve resources they have created.
            -->
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_AUTHOR</atombeat:recipient>
                    <atombeat:permission>CREATE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_AUTHOR</atombeat:recipient>
                    <atombeat:permission>CREATE_MEDIA</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_AUTHOR</atombeat:recipient>
                    <atombeat:permission>LIST_COLLECTION</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_AUTHOR</atombeat:recipient>
                    <atombeat:permission>MULTI_CREATE</atombeat:permission>
                </atombeat:ace><!--
            Editors can list the collection, retrieve and update any member.
            -->
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_EDITOR</atombeat:recipient>
                    <atombeat:permission>LIST_COLLECTION</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_EDITOR</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_EDITOR</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_EDITOR</atombeat:recipient>
                    <atombeat:permission>DELETE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_EDITOR</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_MEMBER_ACL</atombeat:permission>
                </atombeat:ace><!-- Media editors -->
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_MEDIA_EDITOR</atombeat:recipient>
                    <atombeat:permission>LIST_COLLECTION</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_MEDIA_EDITOR</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_MEDIA_EDITOR</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_MEDIA</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_MEDIA_EDITOR</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEDIA</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_MEDIA_EDITOR</atombeat:recipient>
                    <atombeat:permission>DELETE_MEDIA</atombeat:permission>
                </atombeat:ace><!--
            Readers can list the collection and retrieve any member.
            -->
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_READER</atombeat:recipient>
                    <atombeat:permission>LIST_COLLECTION</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_READER</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_READER</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_MEDIA</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_READER</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_HISTORY</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_READER</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_REVISION</atombeat:permission>
                </atombeat:ace><!--
            Data authors can only create media resources with a specific media
            type.
            -->
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_DATA_AUTHOR</atombeat:recipient>
                    <atombeat:permission>CREATE_MEDIA</atombeat:permission>
                    <atombeat:conditions>
                        <atombeat:condition type="mediarange">application/vnd.ms-excel</atombeat:condition>
                    </atombeat:conditions>
                </atombeat:ace>
            </atombeat:acl>
        </atombeat:security-descriptor>
    </atom:content>
</atom:entry>
```

What you see within the `atom:content` element is an `atombeat:security-descriptor` element. Within this is an `atombeat:acl` element - the container for the collection's access control list. The `atombeat:acl` element has a number of `atombeat:ace` elements - these are **access control entries** (ACEs), which make up the **access control list** (ACL).

Hopefully, the meaning of an ACE should be fairly obvious. E.g., the ACE:

```
<atombeat:ace>
    <atombeat:type>ALLOW</atombeat:type>
    <atombeat:recipient type="role">ROLE_AUTHOR</atombeat:recipient>
    <atombeat:permission>CREATE_MEMBER</atombeat:permission>
</atombeat:ace>
```

...says that any user with the role `ROLE_AUTHOR` is allowed to CREATE\_MEMBER. Here, "CREATE\_MEMBER" corresponds to the standard Atom protocol operation of creating a collection member by POSTing an Atom entry document to the collection URI.

# Updating a Security Descriptor #

You can update (i.e., modify) a security descriptor using the standard Atom protocol operation for updating a collection member, i.e., by sending an HTTP PUT request to the URL given in the `edit` link, with a modified Atom entry document representing the resource.

We're going to demonstrate this, but to show that the change has had an effect, we're going to use another user called "rebecca". Rebecca has the ROLE\_READER role, so she should be able to list the test collection. Check that she can list the collection by doing:

```
curl --user rebecca:test --verbose --header "Accept: application/atom+xml" http://localhost:8081/atombeat/service/content/test
```

Next, as Adam (who has the ROLE\_ADMINISTRATOR role), let's retrieve the test collection's security descriptor again, but this time save to a local file:

```
curl --user adam:test --verbose --header "Accept: application/atom+xml" --output test-descriptor.xml http://localhost:8081/atombeat/service/security/test
```

Open the file `test-descriptor.xml` in your text editor of choice, and remove all of the `atombeat:ace` elements, so you end up with something that looks like:

```
<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <atom:id>http://localhost:8081/atombeat/service/security/test</atom:id>
    <atom:title type="text">Security Descriptor</atom:title>
    <atom:link rel="self" href="http://localhost:8081/atombeat/service/security/test" type="application/atom+xml;type=entry"/>
    <atom:link rel="edit" href="http://localhost:8081/atombeat/service/security/test" type="application/atom+xml;type=entry"/>
    <atom:link rel="http://purl.org/atombeat/rel/secured" href="http://localhost:8081/atombeat/service/content/test" type="application/atom+xml;type=feed"/>
    <atom:updated>2011-01-19T11:13:00.731Z</atom:updated>
    <atom:content type="application/vnd.atombeat+xml">
        <atombeat:security-descriptor xmlns:atombeat="http://purl.org/atombeat/xmlns">
            <atombeat:acl>
            </atombeat:acl>
        </atombeat:security-descriptor>
    </atom:content>
</atom:entry>
```

Now, we'll PUT this modified representation back to the `edit` link URI - http://localhost:8081/atombeat/service/security/test - e.g.,:

```
curl --user adam:test --verbose --header "Accept: application/atom+xml" --header "Content-Type: application/atom+xml" --upload-file test-descriptor.xml http://localhost:8081/atombeat/service/security/test
```

You should see something like the following from curl:

```
> PUT /atombeat/service/security/test HTTP/1.1
> Authorization: Basic YWRhbTp0ZXN0
> User-Agent: curl/7.19.7 (x86_64-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15
> Host: localhost:8081
> Accept: application/atom+xml
> Content-Type: application/atom+xml
> Content-Length: 931
> Expect: 100-continue
> 
< HTTP/1.1 100 Continue
< HTTP/1.1 200 OK
< Server: Apache-Coyote/1.1
< Set-Cookie: JSESSIONID=30F55429121C32DEF90999E2455C621C; Path=/atombeat
< pragma: no-cache
< Cache-Control: no-cache
< Content-Type: application/atom+xml;type=entry;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Wed, 19 Jan 2011 12:02:57 GMT
< 
<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <atom:id>http://localhost:8081/atombeat/service/security/test</atom:id>
    <atom:title type="text">Security Descriptor</atom:title>
    <atom:link rel="self" href="http://localhost:8081/atombeat/service/security/test" type="application/atom+xml;type=entry"/>
    <atom:link rel="edit" href="http://localhost:8081/atombeat/service/security/test" type="application/atom+xml;type=entry"/>
    <atom:link rel="http://purl.org/atombeat/rel/secured" href="http://localhost:8081/atombeat/service/content/test" type="application/atom+xml;type=feed"/>
    <atom:updated>2011-01-19T12:02:57.059Z</atom:updated>
    <atom:content type="application/vnd.atombeat+xml">
        <atombeat:security-descriptor xmlns:atombeat="http://purl.org/atombeat/xmlns">
            <atombeat:acl>
            </atombeat:acl>
        </atombeat:security-descriptor>
    </atom:content>
</atom:entry>
```

If so, you have successfully updated the security descriptor. You can verify this has had an effect by trying to list the collection again as Rebecca:

```
curl --user rebecca:test --verbose --header "Accept: application/atom+xml" http://localhost:8081/atombeat/service/content/test
```

You should see something like:

```
> GET /atombeat/service/content/test HTTP/1.1
> Authorization: Basic cmViZWNjYTp0ZXN0
> User-Agent: curl/7.19.7 (x86_64-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15
> Host: localhost:8081
> Accept: application/atom+xml
> 
< HTTP/1.1 403 Forbidden
< Server: Apache-Coyote/1.1
< Set-Cookie: JSESSIONID=61E72A769ACED25196DEC320FA061A02; Path=/atombeat
< pragma: no-cache
< Cache-Control: no-cache
< Content-Type: application/xml;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Wed, 19 Jan 2011 15:59:55 GMT
< 
<error>
    <status>403</status>
    <message>The server understood the request, but is refusing to fulfill it. Authorization will not help and the request SHOULD NOT be repeated.&gt;</message>
    <request>
        <method>GET</method>
        <path-info>/test</path-info>
        <parameters/>
        <headers>
            <header>
                <name>authorization</name>
                <value>Basic cmViZWNjYTp0ZXN0</value>
            </header>
            <header>
                <name>user-agent</name>
                <value>curl/7.19.7 (x86_64-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15</value>
            </header>
            <header>
                <name>host</name>
                <value>localhost:8081</value>
            </header>
            <header>
                <name>accept</name>
                <value>application/atom+xml</value>
            </header>
        </headers>
        <user>rebecca</user>
        <roles>ROLE_READER ROLE_USER</roles>
    </request>
</error>
```

# Security Descriptors for Collection Members and Media Resources #

As well as Atom collections having security descriptors, every collection member (i.e., Atom entry) also has a security descriptor. You can find the security descriptor for a member by following an `atom:link` with `rel="http://purl.org/atombeat/rel/security-descriptor"` within the Atom entry document.

Media resources also get their own security descriptors. You can find the security descriptor for a media resource by following an `atom:link` with `rel="http://purl.org/atombeat/rel/media-security-descriptor"` within the associated media-link entry document.

# The Workspace Security Descriptor #

In addition to collections, collection members and media resources each having a security descriptor, there is also a single security descriptor for the entire workspace.

Unfortunately, there is no way to follow your nose to the workspace security descriptor at the moment ([issue 137](https://code.google.com/p/atombeat/issues/detail?id=137)), you just have to know that if your AtomBeat service is deployed at http://localhost:8081/atombeat/service/ then the workspace security descriptor URI is:

  * http://localhost:8081/atombeat/service/security/

You can retrieve the workspace security descriptor and inspect it, e.g.:

```
curl --user adam:test --verbose --header "Accept: application/atom+xml" http://localhost:8081/atombeat/service/security/
```

You should see something like:

```
> GET /atombeat/service/security/ HTTP/1.1
> Authorization: Basic YWRhbTp0ZXN0
> User-Agent: curl/7.19.7 (x86_64-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15
> Host: localhost:8081
> Accept: application/atom+xml
> 
< HTTP/1.1 200 OK
< Server: Apache-Coyote/1.1
< Set-Cookie: JSESSIONID=2C648B789AC2D9F1B696F0D4D039E4BA; Path=/atombeat
< pragma: no-cache
< Cache-Control: no-cache
< Content-Type: application/atom+xml;type=entry;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Wed, 19 Jan 2011 12:15:22 GMT
< 
<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <atom:id>http://localhost:8081/atombeat/service/security/</atom:id>
    <atom:title type="text">Security Descriptor</atom:title>
    <atom:link rel="self" href="http://localhost:8081/atombeat/service/security/" type="application/atom+xml;type=entry"/>
    <atom:link rel="edit" href="http://localhost:8081/atombeat/service/security/" type="application/atom+xml;type=entry"/>
    <atom:link rel="http://purl.org/atombeat/rel/secured" href="http://localhost:8081/atombeat/service/content/" type=""/>
    <atom:updated>2011-01-19T11:13:00.422Z</atom:updated>
    <atom:content type="application/vnd.atombeat+xml">
        <atombeat:security-descriptor xmlns:atombeat="http://purl.org/atombeat/xmlns">
            <atombeat:acl>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>CREATE_COLLECTION</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>UPDATE_COLLECTION</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>LIST_COLLECTION</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>CREATE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>DELETE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>CREATE_MEDIA</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_MEDIA</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEDIA</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>DELETE_MEDIA</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_WORKSPACE_ACL</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_COLLECTION_ACL</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_MEMBER_ACL</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_MEDIA_ACL</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>UPDATE_WORKSPACE_ACL</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>UPDATE_COLLECTION_ACL</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER_ACL</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEDIA_ACL</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>MULTI_CREATE</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_HISTORY</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_REVISION</atombeat:permission>
                </atombeat:ace><!-- you could also use a wildcard --><!--
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>*</atombeat:permission>
            </atombeat:ace>
            -->
            </atombeat:acl>
        </atombeat:security-descriptor>
    </atom:content>
</atom:entry>
```

The ACL in this security descriptor allows users with the role `ROLE_ADMINISTRATOR` to perform any of AtomBeat's recognised protocol operations. So far we've been using the username "adam", and adam has this role (see `WEB-INF/security-example.xml` for the default users and role assignments), which explains why you've been able to do everything as adam.

# Access Control Concepts #

## ACE Type ##

One of the components of an access control entry is the `atombeat:type` element. This element can take any of the following values:

  * ALLOW
  * DENY

I.e., an ACE gives a rule for either allowing or denying a protocol operation for a given recipient.

Broadly speaking, there are two ways you can approach the design of your access control rules.

One alternative is to start with the default decision to DENY, then use only ALLOW ACEs, i.e., everything is denied, except for those things you explicitly allow.

The other alternative is to start with the default decision to ALLOW, then use DENY ACEs to say which operations are excluded. I.e., everything is allowed, except for those things you explicitly deny.

AtomBeat gives you the option of taking either approach, by setting the `$security-config:default-decision` variable in the `service/config/security.xqm` file to either ALLOW or DENY. This setting determines the access control decision for a given request in the absence of any matching ACEs.

## Matching Protocol Operations ##

One of the components of an access control entry is the `atombeat:permission` element. This element can take any of the following values:

  * LIST\_COLLECTION
  * CREATE\_MEMBER
  * RETRIEVE\_MEMBER
  * UPDATE\_MEMBER
  * DELETE\_MEMBER
  * CREATE\_MEDIA
  * RETRIEVE\_MEDIA
  * UPDATE\_MEDIA
  * DELETE\_MEDIA
  * CREATE\_COLLECTION
  * UPDATE\_COLLECTION
  * RETRIEVE\_WORKSPACE\_ACL
  * UPDATE\_WORKSPACE\_ACL
  * RETRIEVE\_COLLECTION\_ACL
  * UPDATE\_COLLECTION\_ACL
  * RETRIEVE\_MEMBER\_ACL
  * UPDATE\_MEMBER\_ACL
  * RETRIEVE\_MEDIA\_ACL
  * UPDATE\_MEDIA\_ACL
  * MULTI\_CREATE
  * RETRIEVE\_HISTORY
  * RETRIEVE\_REVISION
  * `*`

Each of these values corresponds to a protocol operation that AtomBeat recognises (except for the wildcard "`*`" which matches any protocol operation). Some of these are standard Atom protocol operations, some of these are protocol extensions supported by AtomBeat.

Let's consider only the standard Atom protocol operations:

| **AtomBeat Operation** | **HTTP Request Features** |
|:-----------------------|:--------------------------|
| LIST\_COLLECTION | GET collection URI |
| CREATE\_MEMBER | POST collection URI, content type "application/atom+xml", content body root element `atom:entry` |
| RETRIEVE\_MEMBER | GET member URI |
| UPDATE\_MEMBER | PUT member URI, content type "application/atom+xml", content body root element `atom:entry` |
| DELETE\_MEMBER | DELETE member URI |
| CREATE\_MEDIA | POST collection URI, content type **not** "application/atom+xml" |
| RETRIEVE\_MEDIA | GET media resource URI |
| UPDATE\_MEDIA | PUT media resource URI, content type **not** "application/atom+xml" |
| DELETE\_MEDIA | DELETE media resource URI or media-link URI |

E.g., if you send an HTTP GET request to a collection URI, then an ACE where the `atombeat:permission` element contains "LIST\_COLLECTION" may be matched during ACL processing.

E.g., if you send an HTTP POST request to a collection URI, where the request header "Content-Type" is "application/atom+xml" and the request body is an Atom entry document, then an ACE where the `atombeat:permission` element contains "CREATE\_MEMBER" may be matched during ACL processing.

(It would probably have been more appropriate to name this element `atombeat:operation` rather than `atombeat:permission`, as this element only serves to match the current protocol operation, see also [issue 138](https://code.google.com/p/atombeat/issues/detail?id=138).)

## Matching Recipients - Users, Roles and Groups ##

One of the components of an access control entry is the `atombeat:recipient` element. The element itself can take any value. If the value is the wildcard "`*`" then the ACE will match any recipient.

This element must have a `@type` attribute, which can take one of the following values:

  * user
  * role
  * group

### Recipient Type: User ###

If the `@type` is "user" then the ACE will be matched against the authenticated user's user name. This is obtained by AtomBeat via Spring Security using the following Java code:

```
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
String username = authentication.getName();
```

I.e., AtomBeat will use whatever is returned by `authentication.getName()` as the user's user name, and will attempt to match this against ACEs where the recipient type is "user".

### Recipient Type: Role ###

I will apologise in advance for the potentially confusing terminology, but before reading this section you should be aware that what different people mean by "role" in different security systems can vary a lot.

AtomBeat has a particular definition of "role", which is defined operationally. In a nutshell, AtomBeat considers "roles" to be attributes of a user that are provisioned via Spring Security as authorities. I.e., the set of roles for an authenticated user are obtained via the following Java code:

```
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
Collection<GrantedAuthority> authorities = authentication.getAuthorities();
```

So what "roles" a user has will depend entirely on how you configure Spring Security.

Sometimes you may want to map things that are called "groups" in an external system onto the recipient type "role" within AtomBeat ACEs, e.g., if you are using an LDAP repository and using LDAP groups to organise your users.

### Recipient Type: Group ###

Again, I will apologise in advance for the potentially confusing terminology - what different people mean by "group" in different security systems can vary a lot.

AtomBeat has a particular definition of "group", which is defined operationally. In AtomBeat, groups are basically syntactic sugar, that help make ACEs more concise, which in turn makes them easier to write and maintain.

Groups are defined **inline** within a security descriptor, and allow you to write a policy that applies a common set of ACEs to a common set of users. E.g., you could have a security descriptor like:

```
<atombeat:security-descriptor>
    <atombeat:groups>
        <atombeat:group id="readers">
            <atombeat:member>richard</atombeat:member>
            <atombeat:member>rebecca</atombeat:member>
        </atombeat:group>
        <atombeat:group id="authors">
            <atombeat:member>alice</atombeat:member>
            <atombeat:member>austin</atombeat:member>
        </atombeat:group>
        <atombeat:group id="editors">
            <atombeat:member>emma</atombeat:member>
            <atombeat:member>edward</atombeat:member>
        </atombeat:group>
    </atombeat:groups>
    <atombeat:acl>
        <atombeat:ace>
            <atombeat:type>ALLOW</atombeat:type>
            <atombeat:recipient type="group">readers</atombeat:recipient>
            <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
        </atombeat:ace>
        <atombeat:ace>
            <atombeat:type>ALLOW</atombeat:type>
            <atombeat:recipient type="group">authors</atombeat:recipient>
            <atombeat:permission>CREATE_MEMBER</atombeat:permission>
        </atombeat:ace>
        <atombeat:ace>
            <atombeat:type>ALLOW</atombeat:type>
            <atombeat:recipient type="group">editors</atombeat:recipient>
            <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
        </atombeat:ace>
    </atombeat:acl>
</atombeat:security-descriptor>
```

Groups can also be **sourced** from other security descriptors or collection members, see the section @@TODO below.

## Matching Other Conditions ##

For some protocol operations, other conditions may be specified in an ACE. For example:

```
<atombeat:ace>
    <atombeat:type>ALLOW</atombeat:type>
    <atombeat:recipient type="role">ROLE_DATA_AUTHOR</atombeat:recipient>
    <atombeat:permission>CREATE_MEDIA</atombeat:permission>
    <atombeat:conditions>
        <atombeat:condition type="mediarange">application/vnd.ms-excel</atombeat:condition>
    </atombeat:conditions>
</atombeat:ace>
```

This ACE says that any user with the role ROLE\_DATA\_AUTHOR can create media resources where the media type of the resource matches a given media range (here the specific media type "application/vnd.ms-excel").

## Decisions - Processing ACLs ##

For any given protocol operation, the AtomBeat security plugin will reach a decision about whether to ALLOW or DENY the operation. If the decision is proceed (ALLOW), then the operation will be executed as normal. If the decision is **not** to proceed (DENY), then execution of the operation will terminate and the client will be sent a `403 Forbidden` response.

To reach a decision for a particular request, the security plugin first **assembles an effective ACL** for the resource to which the request applies. How this ACL is assembled will depend on which protocol operation is requested, and on how the security plugin is configured.

For example, if the operation is LIST\_COLLECTION, then the effective ACL will be assembled from the ACL in the workspace security descriptor and the ACL in the collection security descriptor.

For example, if the operation is RETRIEVE\_MEMBER, then the effective ACL will be assembled from ACLs in the member, collection and workspace descriptors.

The ACEs are then tested sequentially to see if the ACE matches the current request. I.e., for each ACE, an attempt is made to match the **recipient**, **permission** and any **conditions** to the current request. **The first matching ACE wins**.

## Precedence & Effective ACLs ##

Because the first matching ACE wins, the order in which ACLs are concatenated to assemble an effective ACL determines the precedence.

I.e., if the effective ACL for a collection is assembled by concatenating the workspace ACL followed by the collection ACL, then any matching ACEs in the workspace ACL will take precedence over ACEs in the collection ACL.

For example, consider an AtomBeat service where the workspace security descriptor is:

```
<atombeat:security-descriptor>
    <atombeat:acl>
        <atombeat:ace>
            <atombeat:type>ALLOW</atombeat:type>
            <atombeat:recipient type="role">administrator</atombeat:recipient>
            <atombeat:permission>CREATE_MEMBER</atombeat:permission>
        </atombeat:ace>
    </atombeat:acl>
</atombeat:security-descriptor>
```

...and the security descriptor for an Atom collection at http://localhost:8081/atombeat/service/content/test is:

```
<atombeat:security-descriptor>
    <atombeat:acl>
        <atombeat:ace>
            <atombeat:type>DENY</atombeat:type>
            <atombeat:recipient type="role">administrator</atombeat:recipient>
            <atombeat:permission>CREATE_MEMBER</atombeat:permission>
        </atombeat:ace>
    </atombeat:acl>
</atombeat:security-descriptor>
```

In this case, if the order of precedence is ( workspace , collection , resource ), then any user with the role ROLE\_ADMINISTRATOR **will** be allowed to create members within the test collection, because the ALLOW ACE in the workspace ACL will match before the DENY ACL in the collection ACL. I.e., the **effective ACL** for a CREATE\_MEMBER operation on the collection will be:

```
<atombeat:acl>
    <atombeat:ace>
        <atombeat:type>ALLOW</atombeat:type>
        <atombeat:recipient type="role">administrator</atombeat:recipient>
        <atombeat:permission>CREATE_MEMBER</atombeat:permission>
    </atombeat:ace>
    <atombeat:ace>
        <atombeat:type>DENY</atombeat:type>
        <atombeat:recipient type="role">administrator</atombeat:recipient>
        <atombeat:permission>CREATE_MEMBER</atombeat:permission>
    </atombeat:ace>
</atombeat:acl>
```

However, if the order of precedence were reversed to ( resource , collection , workspace ) then the opposite would be true.

The order of precedence is configurable in AtomBeat via the `$security-config:priority` configuration variable in the service/config/security.xqm` file. By default the precedence is ( workspace , collection , resource ), because in the use cases we've come across you typically want to define rules at a global (workspace) level that cannot be overridden at a collection or resource level by individual users. However, other situations may require the opposite, e.g., you may want to allow users to override workspace-level or collection-level rules.

# In-line and Out-of-line Groups #

As mentioned above, a group can be defined **in-line** within a security descriptor, e.g.:

```
<atombeat:security-descriptor>
    <atombeat:groups>
        <atombeat:group id="readers">
            <atombeat:member>richard</atombeat:member>
            <atombeat:member>rebecca</atombeat:member>
        </atombeat:group>
        <atombeat:group id="authors">
            <atombeat:member>alice</atombeat:member>
            <atombeat:member>austin</atombeat:member>
        </atombeat:group>
        <atombeat:group id="editors">
            <atombeat:member>emma</atombeat:member>
            <atombeat:member>edward</atombeat:member>
        </atombeat:group>
    </atombeat:groups>
    <atombeat:acl>
        <atombeat:ace>
            <atombeat:type>ALLOW</atombeat:type>
            <atombeat:recipient type="group">readers</atombeat:recipient>
            <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
        </atombeat:ace>
        <atombeat:ace>
            <atombeat:type>ALLOW</atombeat:type>
            <atombeat:recipient type="group">authors</atombeat:recipient>
            <atombeat:permission>CREATE_MEMBER</atombeat:permission>
        </atombeat:ace>
        <atombeat:ace>
            <atombeat:type>ALLOW</atombeat:type>
            <atombeat:recipient type="group">editors</atombeat:recipient>
            <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
        </atombeat:ace>
    </atombeat:acl>
</atombeat:security-descriptor>
```

Groups can also be **sourced** from another security descriptor or collection member - this is also called "out-of-line" group definitions, or "referencing" groups.

## Sourcing Groups from Another Security Descriptor ##

E.g., if the above security descriptor was the security descriptor for the test collection we created above and had the URI http://localhost:8081/atombeat/service/security/test then you could reference these groups in another security descriptor, e.g.:

```
<atombeat:security-descriptor>
    <atombeat:groups>
        <atombeat:group id="readers" src="http://localhost:8081/atombeat/service/security/test"/>
        <atombeat:group id="authors" src="http://localhost:8081/atombeat/service/security/test"/>
        <atombeat:group id="editors" src="http://localhost:8081/atombeat/service/security/test"/>
    </atombeat:groups>
    <atombeat:acl>
        <atombeat:ace>
            <atombeat:type>ALLOW</atombeat:type>
            <atombeat:recipient type="group">readers</atombeat:recipient>
            <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
        </atombeat:ace>
        <atombeat:ace>
            <atombeat:type>ALLOW</atombeat:type>
            <atombeat:recipient type="group">authors</atombeat:recipient>
            <atombeat:permission>CREATE_MEMBER</atombeat:permission>
        </atombeat:ace>
        <atombeat:ace>
            <atombeat:type>ALLOW</atombeat:type>
            <atombeat:recipient type="group">editors</atombeat:recipient>
            <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
        </atombeat:ace>
    </atombeat:acl>
</atombeat:security-descriptor>
```

## Sourcing Groups from a Collection Member ##

As well as referencing groups defined inline within another security descriptor, you can also reference groups within a plain old collection member.

E.g., if you created a member that looked like:

```
<atom:entry xmlns:atom="http://www.w3.org/2005/Atom" xmlns:atombeat="http://purl.org/atombeat/xmlns">
     <atom:title>TEST ENTRY WITH GROUPS</atom:title>
     <atom:content type="application/xml">
         <atombeat:groups>
             <atombeat:group id="readers">
                 <atombeat:member>richard</atombeat:member>
                 <atombeat:member>rebecca</atombeat:member>
             </atombeat:group>
             <atombeat:group id="authors">
                 <atombeat:member>alice</atombeat:member>
                 <atombeat:member>austin</atombeat:member>
             </atombeat:group>
             <atombeat:group id="editors">
                 <atombeat:member>emma</atombeat:member>
                 <atombeat:member>edward</atombeat:member>
             </atombeat:group>
         </atombeat:groups>
     </atom:content>
 </atom:entry>
```

...and that member had the URI http://localhost:8081/atombeat/service/content/test/1234-56789-0123456 then you could reference these groups in another security descriptor, e.g.:

```
<atombeat:security-descriptor>
    <atombeat:groups>
        <atombeat:group id="readers" src="http://localhost:8081/atombeat/service/content/test/1234-56789-0123456"/>
        <atombeat:group id="authors" src="http://localhost:8081/atombeat/service/content/test/1234-56789-0123456"/>
        <atombeat:group id="editors" src="http://localhost:8081/atombeat/service/content/test/1234-56789-0123456"/>
    </atombeat:groups>
    <atombeat:acl>
        <atombeat:ace>
            <atombeat:type>ALLOW</atombeat:type>
            <atombeat:recipient type="group">readers</atombeat:recipient>
            <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
        </atombeat:ace>
        <atombeat:ace>
            <atombeat:type>ALLOW</atombeat:type>
            <atombeat:recipient type="group">authors</atombeat:recipient>
            <atombeat:permission>CREATE_MEMBER</atombeat:permission>
        </atombeat:ace>
        <atombeat:ace>
            <atombeat:type>ALLOW</atombeat:type>
            <atombeat:recipient type="group">editors</atombeat:recipient>
            <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
        </atombeat:ace>
    </atombeat:acl>
</atombeat:security-descriptor>
```

This means that you can use Atom collection members to manage groups if you want to. This is useful, e.g., where you want certain users to be able to add and remove group members but not to be able to alter access control lists.

The definition of a group can appear **anywhere** within the member's Atom entry document. I.e., AtomBeat will use the XPath `//atombeat:group[@id='readers']` to find a "readers" group within the referenced Atom entry document.

# Configuring Default Security Descriptors #

Whenever you create a new collection member or media resource via the Atom protocol, or create a new collection via AtomBeat's protocol extensions, the security plugin will automatically install a default security descriptor for that member, media resource or collection, at the time of creation.

These default security descriptors are configured in the `service/config/security.xqm` file, via the following functions:

  * `security-config:default-collection-security-descriptor()`
  * `security-config:default-member-security-descriptor()`
  * `security-config:default-media-security-descriptor()`

You can modify these functions to alter the default security policy that your service implements.

# Special Considerations #

## Listing a Collection; Filtering Feeds ##

The LIST\_COLLECTION operation (i.e., a GET request to a collection URI) is handled slightly differently by the security plugin.

The security plugin will first process the effective ACL for the collection to determine whether the LIST\_COLLECTION operation is allowed for the current user. If the operation is denied, then processing will immediately terminate and a 403 reponse is sent. If the operation is allowed, then a further processing step occurs, where the entries in the feed are **filtered**. The feed is filtered by testing each individual Atom entry see whether a RETRIEVE\_MEMBER operation would be allowed on that member for the current user. If the user is **not** allowed to retrieve that member, then the Atom entry is excluded from the feed.

I.e., listing a collection is not a security hole - if a user is not allowed to retrieve a collection member directly, then the member will not appear in the feed when listing the collection.

## Deleting Media Resources ##

Note that there are **two ways** to deleting a media resource. Either you can send a DELETE request directly to the media resource URI, or you can send a DELETE request to the associated media-link's edit URI. In both cases the effect is the same - **both** the media resource **and** the media-link are deleted.

To make this work within the (one ACE, one operation, one decision) model used by the AtomBeat security plugin, the plugin handles delete media requests by considering that **both** of the above alternatives match the DELETE\_MEDIA operation, and **niether** matches the DELETE\_MEMBER operation.

I.e., in order to delete a media resource, either via the media resource URI or via the media-link's edit URI, you only need to be allowed the DELETE\_MEDIA operation. You **do not** need to be allowed the DELETE\_MEMBER operation.

## Nested and Recursive Collections ##

Note that, if you have one Atom collection at /foo and another collection at /foo/bar, by default AtomBeat is **not** aware of the nesting of the two collections. This means that, e.g., when retrieving a member of the collection at /foo/bar, the only ACLs that will be involved will be the member ACL, the /foo/bar collection ACL, and the workspace ACL. The ACL for the /foo collection **will not** be considered. I.e., even though the collections appear to be nested, they are not, and there is no inheritance of ACLs from the apparent parent to child.

The case of recursive collections is slightly more complicated. If you have a collection configured as a recursive collection, then when you list the recursive collection, the collection and workspace ACLs will be consulted to see if you are allowed the LIST\_COLLECTION operation. If you are, then AtomBeat will generate a feed for the collection. When generating the feed, AtomBeat will filter the entries that appear in the feed. For each entry that appears in the feed, AtomBeat will include the entry if you are allowed the RETRIEVE\_MEMBER operation for that entry. When determining if you have this permission, the member's ACL is consulted, along with **the ACL for the collection containing the member** (which might **not** be the collection you are listing, if the member occurs in a sub-collection) and the workspace ACL.

I.e., when assembling an effective ACL, there are never more than three ACLs involved - workspace, collection and member/media resource.

# Further Reading #

For a list of other AtomBeat tutorials available, see the [AtomBeat wiki home page](AtomBeat.md).

The [Atom Protocol Specification](http://www.atomenabled.org/developers/protocol/atom-protocol-spec.php) and the [Atom Format Specification](http://www.atomenabled.org/developers/syndication/atom-format-spec.php) are recommended reading.