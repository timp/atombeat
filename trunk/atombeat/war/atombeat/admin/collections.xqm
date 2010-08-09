xquery version "1.0";

module namespace config-collections = "http://purl.org/atombeat/xquery/config-collections";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;

declare variable $config-collections:collection-spec := 
    <spec>
        <collection path-info="/foo">
            <atom:feed
                atombeat:enable-history="true"
                atombeat:exclude-entry-content="true"
                atombeat:recursive="true">
                <atom:title type="text">Foo Collection</atom:title>
                <atombeat:config-link-extensions>
                    <atombeat:extension-attribute
                        name="allow"
                        namespace="http://purl.org/atombeat/xmlns">
                        <atombeat:config context="feed">
                            <atombeat:param name="match-rels" value="self http://purl.org/atombeat/rel/security-descriptor"/>
                        </atombeat:config>
                        <atombeat:config context="entry">
                            <atombeat:param name="match-rels" value="*"/>
                        </atombeat:config>
                        <atombeat:config context="entry-in-feed">
                            <atombeat:param name="match-rels" value="edit edit-media"/>
                        </atombeat:config>
                    </atombeat:extension-attribute>
                </atombeat:config-link-extensions>
                <atombeat:config-link-expansion>
                    <atombeat:config context="feed">
                        <atombeat:param name="match-rels" value="http://purl.org/atombeat/rel/security-descriptor"/>
                    </atombeat:config>
                    <atombeat:config context="entry">
                        <atombeat:param name="match-rels" value="*"/>
                    </atombeat:config>
                    <atombeat:config context="entry-in-feed">
                        <atombeat:param name="match-rels" value="http://purl.org/atombeat/rel/security-descriptor"/>
                    </atombeat:config>
                </atombeat:config-link-expansion>
            </atom:feed>
        </collection>
        <collection path-info="/test">
            <atom:feed
                atombeat:enable-history="false"
                atombeat:exclude-entry-content="false"
                atombeat:recursive="false">
                <atom:title type="text">Test Collection</atom:title>
            </atom:feed>
        </collection>
    </spec>
;
