

# Introduction #

This tutorial provides an introduction to AtomBeat's support for versioning Atom collection members.

Note that there is currently no support in AtomBeat for versioning media resources.

# Prerequisites #

This tutorial assumes that you have completed the [Getting Started Tutorial](TutorialGettingStarted.md). In short, it assumes that you have:

  * version >0.2-alpha-2 of the **atombeat-exist-minimal** web application deployed at the context path **/atombeat** on a servlet container running on port 8080
  * cURL installed on your computer
  * OPTIONAL: a TCP proxy listening on port 8081 and forwarding to port 8080

If you are doing this tutorial without a TCP proxy, replace "8081" with "8080" wherever you see it below.

To check that the AtomBeat web application is installed and running, go to http://localhost:8081/atombeat/ - you should see a web page saying, "It works!"

# Creating a Versioned Collection #

When you create an Atom collection using AtomBeat, the versioning support can either be enabled or disabled for that collection. By default, it will be disabled for any new collection, unless explicitly configured as enabled.

There are several different ways to create an Atom collection in AtomBeat. In this tutorial, we're going to use an AtomBeat-specific protocol extension to create a versioned collection.

First, create a file in your current directory called "versioned-feed.xml" with the following content:

```
<?xml version="1.0"?>
<atom:feed 
  xmlns:atom="http://www.w3.org/2005/Atom" 
  xmlns:atombeat="http://purl.org/atombeat/xmlns" 
  atombeat:enable-versioning="true">
  <atom:title type="text">My Versioned Collection</atom:title>
  <atom:summary type="text">A collection to try out AtomBeat's versioning support.</atom:summary>
</atom:feed>
```

Note the value of the `@atombeat:enable-versioning` attribute on the root element.

You can create a new collection at a URI of your choosing by sending an HTTP PUT request to that URI with an Atom feed document as the request body.

E.g., to create a versioned collection at http://localhost:8081/atombeat/service/content/my-versioned-collection do:

```
$ curl --verbose --header "Content-Type: application/atom+xml" --upload-file versioned-feed.xml http://localhost:8081/atombeat/service/content/my-versioned-collection
```

You should see something like the following in your TCP trace:

```
PUT /atombeat/service/content/my-versioned-collection HTTP/1.1
User-Agent: curl/7.19.7 (x86_64-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15
Host: localhost:8081
Accept: */*
Content-Type: application/atom+xml
Content-Length: 340

<?xml version="1.0"?>
<atom:feed 
  xmlns:atom="http://www.w3.org/2005/Atom" 
  xmlns:atombeat="http://purl.org/atombeat/xmlns" 
  atombeat:enable-versioning="true">
  <atom:title type="text">My Versioned Collection</atom:title>
  <atom:summary type="text">A collection to try out AtomBeat's versioning support.</atom:summary>
</atom:feed>


HTTP/1.1 201 Created
Server: Apache-Coyote/1.1
pragma: no-cache
Cache-Control: no-cache
Location: http://localhost:8081/atombeat/service/content/my-versioned-collection
Content-Type: application/atom+xml;type=feed;charset=UTF-8
Transfer-Encoding: chunked
Date: Fri, 15 Oct 2010 15:28:19 GMT

<atom:feed xmlns:atom="http://www.w3.org/2005/Atom" xmlns:atombeat="http://purl.org/atombeat/xmlns" atombeat:enable-versioning="true">
    <atom:id>http://localhost:8081/atombeat/service/content/my-versioned-collection</atom:id>
    <atom:updated>2010-10-15T16:28:19.342+01:00</atom:updated>
    <atom:author>
        <atom:name/>
    </atom:author>
    <atom:title type="text">My Versioned Collection</atom:title>
    <atom:summary type="text">A collection to try out AtomBeat's versioning support.</atom:summary>
    <atom:link rel="self" href="http://localhost:8081/atombeat/service/content/my-versioned-collection" type="application/atom+xml;type=feed"/>
    <atom:link rel="edit" href="http://localhost:8081/atombeat/service/content/my-versioned-collection" type="application/atom+xml;type=feed"/>
</atom:feed>
```

To check the collection is successfully created, go to http://localhost:8081/atombeat/service/content/my-versioned-collection in your browser, or retrieve a feed using cURL:

```
$ curl --verbose http://localhost:8081/atombeat/service/content/my-versioned-collection
```

# Retrieving a Member History Feed #

For each new member of your versioned collection, AtomBeat makes available a "history feed", which is a feed of revisions to that member. To see this working, we first need to create and then update a member of this collection.

Assuming you still have the files `entry1.xml` and `entry1-updated.xml` in your current directory that you created during the TutorialGettingStarted, do the following:

```
$ curl --verbose --header "Content-Type: application/atom+xml" --data-binary @entry1.xml http://localhost:8081/atombeat/service/content/my-versioned-collection
```

You should see something like the following:

```
POST /atombeat/service/content/my-versioned-collection HTTP/1.1
User-Agent: curl/7.19.7 (x86_64-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15
Host: localhost:8081
Accept: */*
Content-Type: application/atom+xml
Content-Length: 188

<?xml version="1.0"?>
<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
  <atom:title>Atom-Powered Robots Run Amok</atom:title>
  <atom:content>Some text.</atom:content>
</atom:entry>


HTTP/1.1 201 Created
Server: Apache-Coyote/1.1
pragma: no-cache
Cache-Control: no-cache
Location: http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom
Content-Location: http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom
ETag: "a22c67c67bcf00d2380d4e6d1885f5cd"
Content-Type: application/atom+xml;type=entry;charset=UTF-8
Transfer-Encoding: chunked
Date: Fri, 15 Oct 2010 15:34:32 GMT

<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <atom:id>http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom</atom:id>
    <atom:published>2010-10-15T16:34:32.897+01:00</atom:published>
    <atom:updated>2010-10-15T16:34:32.897+01:00</atom:updated>
    <atom:author>
        <atom:name/>
    </atom:author>
    <atom:title>Atom-Powered Robots Run Amok</atom:title>
    <atom:content>Some text.</atom:content>
    <ar:comment xmlns:ar="http://purl.org/atompub/revision/1.0">
        <atom:author>
            <atom:name/>
        </atom:author>
        <atom:updated>2010-10-15T16:34:32.897+01:00</atom:updated>
        <atom:summary>initial revision</atom:summary>
    </ar:comment>
    <atom:link rel="self" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
    <atom:link rel="edit" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
    <atom:link rel="history" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom" type="application/atom+xml;type=feed"/>
</atom:entry>
```

Notice that the entry has a "history" link - we'll use that in a moment, but first let's update this member so we have something interesting to look at. Using your member URI, update the member via a PUT request, e.g.:

```
$ curl --verbose --header "Content-Type: application/atom+xml" --upload-file entry1-updated.xml http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom
```

You should see something like:

```
PUT /atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom HTTP/1.1
User-Agent: curl/7.19.7 (x86_64-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15
Host: localhost:8081
Accept: */*
Content-Type: application/atom+xml
Content-Length: 306

<?xml version="1.0"?>
<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
  <atom:title>Atom-Powered Robots Run Amok</atom:title>
  <atom:content type="xhtml">
    <div xmlns="http://www.w3.org/1999/xhtml">
      <p><em>AtomBeat 0.2 has been released!</em></p>
    </div>
  </atom:content>
</atom:entry>


HTTP/1.1 200 OK
Server: Apache-Coyote/1.1
pragma: no-cache
Cache-Control: no-cache
ETag: "bc2146a008b0c97f37df834d7eed8678"
Content-Type: application/atom+xml;type=entry;charset=UTF-8
Transfer-Encoding: chunked
Date: Fri, 15 Oct 2010 15:39:35 GMT

<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <atom:id>http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom</atom:id>
    <atom:published>2010-10-15T16:34:32.897+01:00</atom:published>
    <atom:updated>2010-10-15T16:39:35.649+01:00</atom:updated>
    <atom:author>
        <atom:name/>
    </atom:author>
    <atom:title>Atom-Powered Robots Run Amok</atom:title>
    <atom:content type="xhtml">
        <div xmlns="http://www.w3.org/1999/xhtml">
            <p>
                <em>AtomBeat 0.2 has been released!</em>
            </p>
        </div>
    </atom:content>
    <ar:comment xmlns:ar="http://purl.org/atompub/revision/1.0">
        <atom:author>
            <atom:name/>
        </atom:author>
        <atom:updated>2010-10-15T16:39:35.649+01:00</atom:updated>
        <atom:summary/>
    </ar:comment>
    <atom:link rel="self" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
    <atom:link rel="edit" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
    <atom:link rel="history" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom" type="application/atom+xml;type=feed"/>
</atom:entry>
```

Now for the good stuff. Send a GET request to the "history" link URI, e.g.:

```
$ curl --verbose http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom
```

You should see something like:

```
GET /atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom HTTP/1.1
User-Agent: curl/7.19.7 (x86_64-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15
Host: localhost:8081
Accept: */*


HTTP/1.1 200 OK
Server: Apache-Coyote/1.1
pragma: no-cache
Cache-Control: no-cache
Content-Type: application/atom+xml;type=feed;charset=UTF-8
Transfer-Encoding: chunked
Date: Fri, 15 Oct 2010 15:42:07 GMT

<atom:feed xmlns:atom="http://www.w3.org/2005/Atom" xmlns:atombeat="http://purl.org/atombeat/xmlns" atombeat:exclude-entry-content="true">
    <atom:id>http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom</atom:id>
    <atom:title type="text">Version History</atom:title>
    <atom:updated>2010-10-15T16:39:35.649+01:00</atom:updated>
    <atom:link rel="self" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom" type="application/atom+xml;type=feed"/>
    <atom:link rel="http://purl.org/atombeat/rel/versioned" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom" type="application/atom+xml;type=entry"/>
    <atom:entry>
        <ar:revision xmlns:ar="http://purl.org/atompub/revision/1.0" number="1" when="2010-10-15T16:34:32.897+01:00" initial="yes"/>
        <atom:id>http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom</atom:id>
        <atom:published>2010-10-15T16:34:32.897+01:00</atom:published>
        <atom:updated>2010-10-15T16:34:32.897+01:00</atom:updated>
        <atom:author>
            <atom:name/>
        </atom:author>
        <atom:title>Atom-Powered Robots Run Amok</atom:title>
        <atom:content/>
        <ar:comment xmlns:ar="http://purl.org/atompub/revision/1.0">
            <atom:author>
                <atom:name/>
            </atom:author>
            <atom:updated>2010-10-15T16:34:32.897+01:00</atom:updated>
            <atom:summary>initial revision</atom:summary>
        </ar:comment>
        <atom:link rel="current-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
        <atom:link rel="initial-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=1"/>
        <atom:link rel="this-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=1"/>
        <atom:link rel="next-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=2"/>
        <atom:link rel="self" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
        <atom:link rel="edit" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
    </atom:entry>
    <atom:entry>
        <ar:revision xmlns:ar="http://purl.org/atompub/revision/1.0" number="2" when="2010-10-15T16:39:35.649+01:00" initial="no"/>
        <atom:id>http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom</atom:id>
        <atom:published>2010-10-15T16:34:32.897+01:00</atom:published>
        <atom:updated>2010-10-15T16:39:35.649+01:00</atom:updated>
        <atom:author>
            <atom:name/>
        </atom:author>
        <atom:title>Atom-Powered Robots Run Amok</atom:title>
        <atom:content type="xhtml"/>
        <ar:comment xmlns:ar="http://purl.org/atompub/revision/1.0">
            <atom:author>
                <atom:name/>
            </atom:author>
            <atom:updated>2010-10-15T16:39:35.649+01:00</atom:updated>
            <atom:summary/>
        </ar:comment>
        <atom:link rel="current-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
        <atom:link rel="initial-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=1"/>
        <atom:link rel="this-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=2"/>
        <atom:link rel="previous-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=1"/>
        <atom:link rel="self" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
        <atom:link rel="edit" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
    </atom:entry>
</atom:feed>
```

This is the history feed for your collection member.

Notice that there are two `atom:entry` elements in the feed. Each of these represents a **revision** of your collection member.

Notice that each entry has an `ar:revision` element with a version number, a date when the version was created, and an attribute indicating whether the entry represents the initial revision of the member.

Notice also that each entry has several new links:

  * this-revision
  * previous-revision
  * next-revision
  * initial-revision
  * current-revision

...these provide a way to navigate between revisions.

Finally, notice that the `atom:content` elements on each revision are empty. AtomBeat assumes that you will store most of your data for each member within the `atom:content` element (although this is not a hard restriction, you are free to add foreign markup anywhere within an Atom entry document, and AtomBeat will store it). AtomBeat also assumes that this data could be fairly sizeable, and that retrieving a feed containing a complete representation of every single revision of a member could be a costly operation, especially in terms of the amount of data sent between client and server, so it strips out any children of the `atom:content` element when generating a history feed, to provide a useful and relatively efficient summary of available revisions for that member - but don't worry, you can still retrieve a complete representation of each revision...

# Retrieving Member Revisions #

To retrieve a complete representation of a revision, use the URI given in the "this-revision" link. For example, using the history feed above, to retrieve a representation of the first (initial) revision of the member, do:

```
$ curl --verbose http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=1
```

You should see something like:

```
GET /atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=1 HTTP/1.1
User-Agent: curl/7.19.7 (x86_64-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15
Host: localhost:8081
Accept: */*


HTTP/1.1 200 OK
Server: Apache-Coyote/1.1
pragma: no-cache
Cache-Control: no-cache
Content-Type: application/atom+xml;type=entry;charset=UTF-8
Transfer-Encoding: chunked
Date: Fri, 15 Oct 2010 15:51:57 GMT

<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <ar:revision xmlns:ar="http://purl.org/atompub/revision/1.0" number="1" when="2010-10-15T16:34:32.897+01:00" initial="yes"/>
    <atom:id>http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom</atom:id>
    <atom:published>2010-10-15T16:34:32.897+01:00</atom:published>
    <atom:updated>2010-10-15T16:34:32.897+01:00</atom:updated>
    <atom:author>
        <atom:name/>
    </atom:author>
    <atom:title>Atom-Powered Robots Run Amok</atom:title>
    <atom:content>Some text.</atom:content>
    <ar:comment xmlns:ar="http://purl.org/atompub/revision/1.0">
        <atom:author>
            <atom:name/>
        </atom:author>
        <atom:updated>2010-10-15T16:34:32.897+01:00</atom:updated>
        <atom:summary>initial revision</atom:summary>
    </ar:comment>
    <atom:link rel="current-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
    <atom:link rel="initial-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=1"/>
    <atom:link rel="this-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=1"/>
    <atom:link rel="next-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=2"/>
    <atom:link rel="self" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
    <atom:link rel="edit" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
</atom:entry>
```

Notice that the `atom:content` element is not empty, i.e., this is a complete representation of that revision.

To retrieve a representation of the next revision, we could follow the "next-revision" link, and do:

```
$ curl --verbose http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=2
```

...which should give you something like:

```
GET /atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=2 HTTP/1.1
User-Agent: curl/7.19.7 (x86_64-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15
Host: localhost:8081
Accept: */*


HTTP/1.1 200 OK
Server: Apache-Coyote/1.1
pragma: no-cache
Cache-Control: no-cache
Content-Type: application/atom+xml;type=entry;charset=UTF-8
Transfer-Encoding: chunked
Date: Fri, 15 Oct 2010 15:53:43 GMT

<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <ar:revision xmlns:ar="http://purl.org/atompub/revision/1.0" number="2" when="2010-10-15T16:39:35.649+01:00" initial="no"/>
    <atom:id>http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom</atom:id>
    <atom:published>2010-10-15T16:34:32.897+01:00</atom:published>
    <atom:updated>2010-10-15T16:39:35.649+01:00</atom:updated>
    <atom:author>
        <atom:name/>
    </atom:author>
    <atom:title>Atom-Powered Robots Run Amok</atom:title>
    <atom:content type="xhtml">
        <div xmlns="http://www.w3.org/1999/xhtml">
            <p>
                <em>AtomBeat 0.2 has been released!</em>
            </p>
        </div>
    </atom:content>
    <ar:comment xmlns:ar="http://purl.org/atompub/revision/1.0">
        <atom:author>
            <atom:name/>
        </atom:author>
        <atom:updated>2010-10-15T16:39:35.649+01:00</atom:updated>
        <atom:summary/>
    </ar:comment>
    <atom:link rel="current-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
    <atom:link rel="initial-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=1"/>
    <atom:link rel="this-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=2"/>
    <atom:link rel="previous-revision" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom?revision=1"/>
    <atom:link rel="self" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
    <atom:link rel="edit" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
</atom:entry>
```

Again, notice that the `atom:content` element is fully populated.

# Submitting a Revision Comment #

You've probably noticed the `ar:comment` element that appears in the Atom entries above. These elements provide a means of communicating who performed each revision, and also any comments they submitted when making the revision.

To submit a revision comment when updating a member, use the `X-Atom-Revision-Comment` request header.

E.g., create a file "entry1-updated-again.xml" in your current directory containing:

```
<?xml version="1.0"?>
<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
  <atom:title>Atom-Powered Robots Run Amok</atom:title>
  <atom:content type="xhtml">
    <div xmlns="http://www.w3.org/1999/xhtml">
      <p><em>AtomBeat 0.2 has been released!</em></p>
      <p><a href="http://purl.org/atombeat">Visit purl.org/atombeat to find out more.</a></p>
    </div>
  </atom:content>
</atom:entry>
```

(...or whatever you want to write.)

Then update the member, providing a comment request header, e.g.:

```
$ curl --verbose --header "Content-Type: application/atom+xml" --header "X-Atom-Revision-Comment: updated a second time" --upload-file entry1-updated-again.xml http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom
```

You should see something like:

```
PUT /atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom HTTP/1.1
User-Agent: curl/7.19.7 (x86_64-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15
Host: localhost:8081
Accept: */*
Content-Type: application/atom+xml
X-Atom-Revision-Comment: updated a second time
Content-Length: 400

<?xml version="1.0"?>
<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
  <atom:title>Atom-Powered Robots Run Amok</atom:title>
  <atom:content type="xhtml">
    <div xmlns="http://www.w3.org/1999/xhtml">
      <p><em>AtomBeat 0.2 has been released!</em></p>
      <p><a href="http://purl.org/atombeat">Visit purl.org/atombeat to find out more.</a></p>
    </div>
  </atom:content>
</atom:entry>


HTTP/1.1 200 OK
Server: Apache-Coyote/1.1
pragma: no-cache
Cache-Control: no-cache
ETag: "e4d7a4c6c71db0914041e4e869c50e5c"
Content-Type: application/atom+xml;type=entry;charset=UTF-8
Transfer-Encoding: chunked
Date: Fri, 15 Oct 2010 16:09:57 GMT

<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <atom:id>http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom</atom:id>
    <atom:published>2010-10-15T16:34:32.897+01:00</atom:published>
    <atom:updated>2010-10-15T17:09:57.392+01:00</atom:updated>
    <atom:author>
        <atom:name/>
    </atom:author>
    <atom:title>Atom-Powered Robots Run Amok</atom:title>
    <atom:content type="xhtml">
        <div xmlns="http://www.w3.org/1999/xhtml">
            <p>
                <em>AtomBeat 0.2 has been released!</em>
            </p>
            <p>
                <a href="http://purl.org/atombeat">Visit purl.org/atombeat to find out more.</a>
            </p>
        </div>
    </atom:content>
    <ar:comment xmlns:ar="http://purl.org/atompub/revision/1.0">
        <atom:author>
            <atom:name/>
        </atom:author>
        <atom:updated>2010-10-15T17:09:57.392+01:00</atom:updated>
        <atom:summary>updated a second time</atom:summary>
    </ar:comment>
    <atom:link rel="self" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
    <atom:link rel="edit" type="application/atom+xml;type=entry" href="http://localhost:8081/atombeat/service/content/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom"/>
    <atom:link rel="history" href="http://localhost:8081/atombeat/service/history/my-versioned-collection/e056b4fa-fb39-442f-8d94-3838537f10d1.atom" type="application/atom+xml;type=feed"/>
</atom:entry>
```

Note that in the examples above the `atom:author/atom:name` elements within `ar:revision` elements are empty, because we are using a distribution of AtomBeat without support for authentication - the security-enabled AtomBeat distributions will automatically populate this element for you.

Note also that AtomBeat will **not** allow arbitrary insertion or modification of `ar:comment` elements in an Atom entry, rather it will automatically insert a single `ar:comment` after each update to the member, using the value of the `X-Atom-Revision-Comment` header if present to populate the comment summary, and replacing the previous `ar:comment` element or any additional ones added by the client. I.e., the `ar:comment` elements are exclusively under server control, which means that you are guaranteed that the information in the `ar:comment` element corresponds to the version in which it is found. This behaviour is not specified in James Snell's IETF draft, but we found this was the only way to reliably represent the author of the revision (i.e., who made that particular change) without creating further extension elements.

# Further Reading #

For a list of other AtomBeat tutorials available, see the [AtomBeat wiki home page](AtomBeat.md).

The AtomBeat versioning implementation tries to stick as closely as possible to [James Snell's 2006 IETF draft](http://tools.ietf.org/html/draft-snell-atompub-revision-00).