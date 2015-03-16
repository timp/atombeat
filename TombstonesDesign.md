Notes on implementing http://tools.ietf.org/html/draft-snell-atompub-tombstones-11

N.B., this design has been implemented in 0.2-alpha-1, to build and test do:

```
$ svn checkout http://atombeat.googlecode.com/svn/tags/atombeat-parent-0.2-alpha-1 /local/path/to/atombeat-0.2-alpha-1
$ cd /local/path/to/atombeat-0.2-alpha-1
$ export MAVEN_OPTS="-Xmx1024M -XX:MaxPermSize=256M"
$ mvn clean install
```


---

**Table of Contents**




---

# Enabling Tombstones #

Tombstones are enabled on a per-collection basis. By default, tombstones are not enabled. Tombstones are enabled via an extension attribute on the root element of the collection feed document.

E.g., if the collection /foo does not already exist, the request:

```
PUT /atombeat/service/content/foo HTTP/1.1
Host: example.org
Content-Type: application/atom+xml

<atom:feed 
  xmlns:atom="http://www.w3.org/2005/Atom"
  xmlns:atombeat="http://purl.org/atombeat/xmlns"
  atombeat:enable-tombstones="true">
  <atom:title type="text">Example Collection with Tombstones</atom:title>
</atom:feed>
```

...expects...

```
HTTP/1.1 201 Created
Location: http://example.org/atombeat/service/content/foo
```

E.g., if the collection /foo already exists, the request:

```
PUT /atombeat/service/content/foo HTTP/1.1
Host: example.org
Content-Type: application/atom+xml

<atom:feed 
  xmlns:atom="http://www.w3.org/2005/Atom"
  xmlns:atombeat="http://purl.org/atombeat/xmlns"
  atombeat:enable-tombstones="true">
  <atom:title type="text">Example Collection with Tombstones</atom:title>
</atom:feed>
```

...expects...

```
HTTP/1.1 200 OK
```

In both cases, subsequent to the request, tombstones will be enabled on the collection /foo.

# Deleting Collection Members #

Assume that a collection member already exists at /foo/xyz.atom, with the following representation:

```
<atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
    <atom:id>http://example.org/atombeat/service/content/foo/xyz.atom</atom:id>
    <atom:published>2010-10-14T18:29:48.687+01:00</atom:published>
    <atom:updated>2010-10-14T18:29:48.687+01:00</atom:updated>
    <atom:author>
        <atom:name>audrey</atom:name>
    </atom:author>
    <atom:title type="text">Atom-Powered Robots Run Amok</atom:title>
    <atom:summary type="text">Some text.</atom:summary>
    <atom:link rel="self" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/content/foo/xyz.atom"/>
    <atom:link rel="edit" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/content/foo/xyz.atom"/>
</atom:entry>
```

Assume that this is the only member of the collection at /foo.

## Delete Member Operation Expectations ##

The collection member can be deleted with the request:

```
DELETE /atombeat/service/content/foo/xyz.atom HTTP/1.1
X-Atom-Tombstone-Comment: Removed comment spam
Host: example.org
```

If tombstones are **not** enabled on the collection, then this request expects:

```
HTTP/1.1 204 No Content
```

If tombstones **are** enabled on the collection, then this request expects:

```
HTTP/1.1 200 OK
Content-Type: application/atomdeleted+xml

<at:deleted-entry
  xmlns:atom="http://www.w3.org/2005/Atom"
  xmlns:atombeat="http://purl.org/atombeat/xmlns"
  xmlns:at="http://purl.org/atompub/tombstones/1.0"
  ref="http://example.org/atombeat/service/content/foo/xyz.atom"
  when="2010-10-26T18:29:48.687+01:00">
  <at:by>
    <atom:name>jdoe</atom:name>
  </at:by>
  <at:comment>Removed comment spam</at:comment>
</at:deleted-entry>
```

## Retrieve Member Expectations After Delete Member ##

If tombstones are **not** enabled, a subsequent request to retrieve the member:

```
GET /atombeat/service/content/foo/xyz.atom HTTP/1.1
Host: example.org
```

...expects:

```
HTTP/1.1 404 Not Found
```

If tombstones **are** enabled, a subsequent request to retrieve the member expects:

```
HTTP/1.1 410 Gone
Content-Type: application/atomdeleted+xml

<at:deleted-entry
  xmlns:atom="http://www.w3.org/2005/Atom"
  xmlns:atombeat="http://purl.org/atombeat/xmlns"
  xmlns:at="http://purl.org/atompub/tombstones/1.0"
  ref="http://example.org/atombeat/service/content/foo/xyz.atom"
  when="2010-10-26T18:29:48.687+01:00">
  <at:by>
    <atom:name>jdoe</atom:name>
  </at:by>
  <at:comment>Removed comment spam</at:comment>
</at:deleted-entry>
```

## List Collection Expectations After Delete Member ##

If tombstones are **not** enabled, a subsequent request to list the collection:

```
GET /atombeat/service/content/foo HTTP/1.1
Host: example.org
```

...expects:

```
HTTP/1.1 200 OK
Content-Type: application/atom+xml

<atom:feed xmlns:atom="http://www.w3.org/2005/Atom" xmlns:atombeat="http://purl.org/atombeat/xmlns">
    <atom:id>http://example.org/atombeat/service/content/foo</atom:id>
    <atom:updated>2010-10-26T18:29:48.687+01:00</atom:updated>
    <atom:title type="text">Test Collection</atom:title>
    <atom:link rel="self" href="http://example.org/atombeat/service/content/foo" type="application/atom+xml;type=feed"/>
    <atom:link rel="edit" href="http://example.org/atombeat/service/content/foo" type="application/atom+xml;type=feed"/>
</atom:feed>
```

If tombstones **are** enabled, a subsequent request to list the collection expects:

```
HTTP/1.1 200 OK
Content-Type: application/atom+xml

<atom:feed xmlns:atom="http://www.w3.org/2005/Atom" xmlns:atombeat="http://purl.org/atombeat/xmlns" atombeat:tombstones-enabled="true">
    <atom:id>http://example.org/atombeat/service/content/foo</atom:id>
    <atom:updated>2010-10-26T18:29:48.687+01:00</atom:updated>
    <atom:title type="text">Test Collection</atom:title>
    <atom:link rel="self" href="http://example.org/atombeat/service/content/foo" type="application/atom+xml;type=feed"/>
    <atom:link rel="edit" href="http://example.org/atombeat/service/content/foo" type="application/atom+xml;type=feed"/>
    <at:deleted-entry xmlns:at="http://purl.org/atompub/tombstones/1.0"
      ref="http://example.org/atombeat/service/content/foo/xyz.atom"
      when="2010-10-26T18:29:48.687+01:00">
      <at:by>
        <atom:name>jdoe</atom:name>
      </at:by>
      <at:comment>Removed comment spam</at:comment>
    </at:deleted-entry>
</atom:feed>
```

# Configuring Ghosts #

The behaviour of the tombstones implementation w.r.t. the atombeat:ghost element within at:deleted-entry elements can be configured on a per-collection basis. To configure the elements to be copied from the condemned entry into the ghost, update the collection feed document, e.g.:

```
PUT /atombeat/service/content/foo HTTP/1.1
Host: example.org
Content-Type: application/atom+xml

<atom:feed 
  xmlns:atom="http://www.w3.org/2005/Atom"
  xmlns:atombeat="http://purl.org/atombeat/xmlns"
  atombeat:enable-tombstones="true">
  <atom:title type="text">Example Collection with Tombstones</atom:title>
  <atombeat:config-tombstones>
    <atombeat:config>
      <atombeat:param name="ghost-atom-elements" value="title author"/>
    </atombeat:config>
  </atombeat:config-tombstones>
</atom:feed>
```

Then, deleting the collection member:

```
DELETE /atombeat/service/content/foo/xyz.atom HTTP/1.1
X-Atom-Tombstone-Comment: Removed comment spam
Host: example.org
```

...expects:

```
HTTP/1.1 200 OK
Content-Type: application/atomdeleted+xml

<at:deleted-entry
  xmlns:atom="http://www.w3.org/2005/Atom"
  xmlns:atombeat="http://purl.org/atombeat/xmlns"
  xmlns:at="http://purl.org/atompub/tombstones/1.0"
  ref="http://example.org/atombeat/service/content/foo/xyz.atom"
  when="2010-10-26T18:29:48.687+01:00">
  <at:by>
    <atom:name>jdoe</atom:name>
  </at:by>
  <at:comment>Removed comment spam</at:comment>
  <atombeat:ghost>
    <atom:author>
        <atom:name>audrey</atom:name>
    </atom:author>
    <atom:title type="text">Atom-Powered Robots Run Amok</atom:title>
  </atombeat:ghost>
</at:deleted-entry>
```

By default, ghosts will not be present in deleted entries.

The content model for the atombeat:ghost element is as for the atom:entry element.

# Deleting Media Resources #

Assume a collection at /foo with a member at /foo/xyz.atom that is the media link for the media resource at /foo/xyz.media.

Assume the member is then deleted (i.e., a DELETE request is sent to the member URI, /foo/xyz.atom). A side-effect of deleting the member is that the associated media resource is also deleted (this is standard Atom protocol behaviour).

A subsequent request to retrieve the member will behave as for deletion of normal (non-media-link) collection members as described above.

A subsequent request to list the collection will behave as for deletion of normal (non-media-link) collection members as described above.

A subsequent request to retrieve the media resource:

```
GET /atombeat/service/content/foo/xyz.media HTTP/1.1
Host: example.org
```

...expects:

```
HTTP/1.1 404 Not Found
```

...whether or not tombstones are enabled on the collection.

N.B., in AtomBeat, a DELETE request sent to a media resource URI is equivalent to a DELETE request sent to the member URI for the associated media-link. I.e., nothing above would be different if the DELETE request were sent to the media resource URI instead of the member URI.

# Interaction with History/Versioning Plugin #

Assume a collection at /my-versioned-collection with versioning enabled.

Assume a collection member has been created and then updated as described in the [versioning tutorial](TutorialVersioning#Retrieving_a_Member_History_Feed.md).

Assume the member is then deleted.

If tombstones are **not** enabled on the collection, then a subsequent request to the history link URI for the member:

```
GET /atombeat/service/history/my-versioned-collection/xyz.atom HTTP/1.1
Host: example.org
```

...expects:

```
HTTP/1.1 404 Not Found
```

If tombstones **are** enabled on the collection, then a subsequent request to the history link URI expects:

```
HTTP/1.1 200 OK
Server: Apache-Coyote/1.1
pragma: no-cache
Cache-Control: no-cache
Content-Type: application/atom+xml;type=feed;charset=UTF-8
Transfer-Encoding: chunked
Date: Fri, 15 Oct 2010 15:42:07 GMT

<atom:feed xmlns:atom="http://www.w3.org/2005/Atom" xmlns:atombeat="http://purl.org/atombeat/xmlns" atombeat:exclude-entry-content="true">
    <atom:id>http://example.org/atombeat/service/history/my-versioned-collection/xyz.atom</atom:id>
    <atom:title type="text">Version History</atom:title>
    <atom:updated>2010-10-15T16:39:35.649+01:00</atom:updated>
    <atom:link rel="self" href="http://example.org/atombeat/service/history/my-versioned-collection/xyz.atom" type="application/atom+xml;type=feed"/>
    <atom:link rel="http://purl.org/atombeat/rel/versioned" href="http://example.org/atombeat/service/content/my-versioned-collection/xyz.atom" type="application/atom+xml;type=entry"/>
    <atom:entry>
        <ar:revision xmlns:ar="http://purl.org/atompub/revision/1.0" number="1" when="2010-10-15T16:34:32.897+01:00" initial="yes"/>
        <atom:id>http://example.org/atombeat/service/content/my-versioned-collection/xyz.atom</atom:id>
        <atom:published>2010-10-15T16:34:32.897+01:00</atom:published>
        <atom:updated>2010-10-15T16:34:32.897+01:00</atom:updated>
        <atom:author>
            <atom:name>audrey</atom:name>
        </atom:author>
        <atom:title>Atom-Powered Robots Run Amok</atom:title>
        <atom:content/>
        <ar:comment xmlns:ar="http://purl.org/atompub/revision/1.0">
            <atom:author>
                <atom:name>audrey</atom:name>
            </atom:author>
            <atom:updated>2010-10-15T16:34:32.897+01:00</atom:updated>
            <atom:summary>initial revision</atom:summary>
        </ar:comment>
        <atom:link rel="current-revision" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/content/my-versioned-collection/xyz.atom"/>
        <atom:link rel="initial-revision" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/history/my-versioned-collection/xyz.atom?revision=1"/>
        <atom:link rel="this-revision" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/history/my-versioned-collection/xyz.atom?revision=1"/>
        <atom:link rel="next-revision" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/history/my-versioned-collection/xyz.atom?revision=2"/>
        <atom:link rel="self" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/content/my-versioned-collection/xyz.atom"/>
        <atom:link rel="edit" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/content/my-versioned-collection/xyz.atom"/>
    </atom:entry>
    <atom:entry>
        <ar:revision xmlns:ar="http://purl.org/atompub/revision/1.0" number="2" when="2010-10-15T16:39:35.649+01:00" initial="no"/>
        <atom:id>http://example.org/atombeat/service/content/my-versioned-collection/xyz.atom</atom:id>
        <atom:published>2010-10-15T16:34:32.897+01:00</atom:published>
        <atom:updated>2010-10-15T16:39:35.649+01:00</atom:updated>
        <atom:author>
            <atom:name>audrey</atom:name>
        </atom:author>
        <atom:title>Atom-Powered Robots Run Amok</atom:title>
        <atom:content type="xhtml"/>
        <ar:comment xmlns:ar="http://purl.org/atompub/revision/1.0">
            <atom:author>
                <atom:name>audrey</atom:name>
            </atom:author>
            <atom:updated>2010-10-15T16:39:35.649+01:00</atom:updated>
            <atom:summary/>
        </ar:comment>
        <atom:link rel="current-revision" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/content/my-versioned-collection/xyz.atom"/>
        <atom:link rel="initial-revision" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/history/my-versioned-collection/xyz.atom?revision=1"/>
        <atom:link rel="this-revision" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/history/my-versioned-collection/xyz.atom?revision=2"/>
        <atom:link rel="previous-revision" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/history/my-versioned-collection/xyz.atom?revision=1"/>
        <atom:link rel="next-revision" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/history/my-versioned-collection/xyz.atom?revision=3"/>
        <atom:link rel="self" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/content/my-versioned-collection/xyz.atom"/>
        <atom:link rel="edit" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/content/my-versioned-collection/xyz.atom"/>
    </atom:entry>
    <at:deleted-entry
      xmlns:at="http://purl.org/atompub/tombstones/1.0"
      ref="http://example.org/atombeat/service/content/foo/xyz.atom"
      when="2010-10-26T18:29:48.687+01:00">
      <ar:revision xmlns:ar="http://purl.org/atompub/revision/1.0" number="3" when="2010-10-26T18:29:48.687+01:00" initial="no" final="yes" significant="yes"/>
      <at:by>
        <name>jdoe</name>
      </at:by>
      <at:comment>Removed comment spam</at:comment>
      <atom:link rel="current-revision" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/content/my-versioned-collection/xyz.atom"/>
      <atom:link rel="initial-revision" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/history/my-versioned-collection/xyz.atom?revision=1"/>
      <atom:link rel="this-revision" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/history/my-versioned-collection/xyz.atom?revision=3"/>
      <atom:link rel="previous-revision" type="application/atom+xml;type=entry" href="http://example.org/atombeat/service/history/my-versioned-collection/xyz.atom?revision=2"/>
    </at:deleted-entry>
</atom:feed>
```

Note the presence of the ar:revision element within the at:deleted-entry element. Note also the use of the @final and @significant attributes.

Note that the ar:comment element is **not** present within the deleted entry. This would be redundant, as the same information is given in the @when, at:by and at:comment elements.

This is a bit of a mish-mash, which reflects the fact that the [atompub-revision draft](http://tools.ietf.org/html/draft-snell-atompub-revision-00) is much older than the [atompub-tombstones draft](http://tools.ietf.org/html/draft-snell-atompub-tombstones-11), and that they overlap in scope.