import module namespace util = "http://exist-db.org/xquery/util" ; 

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace atomsec = "http://purl.org/atombeat/xquery/atom-security" at "../lib/atom-security.xqm" ;
import module namespace atom-protocol = "http://purl.org/atombeat/xquery/atom-protocol" at "../lib/atom-protocol.xqm" ;


declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;
declare namespace app = "http://www.w3.org/2007/app";


(: this query demonstrates that it is not safe to call atomdb:deprecated-edit-path-info() because of issues in eXist relating to use of temporary fragments stored in the database :)

declare variable $entry :=
    <atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
        <atom:title type="text">Efficacy of chloroquine in P. falciparum malaria </atom:title>
        <atom:content type="application/vnd.chassis-manta+xml">
            <study profile="http://www.cggh.org/2010/chassis/manta/1.1">
                <study-is-published/>
                <publications/>
                <acknowledgements>
                    <person>
                        <first-name/>
                        <middle-name/>
                        <family-name/>
                        <email-address>neenavalecha@gmail.com</email-address>
                        <institution/>
                        <person-is-contactable/>
                    </person>
                </acknowledgements>
                <curator-notes/>
                <study-status>new</study-status>
            </study>
        </atom:content>
    </atom:entry>
;

declare variable $revised-entry :=
    <atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
        <atom:title type="text">Efficacy of chloroquine in P. falciparum malaria </atom:title>
        <atom:content type="application/vnd.chassis-manta+xml">
            <atombeat:groups>
                <atombeat:group id="foo">
                    <atombeat:member>jill</atombeat:member>
                </atombeat:group>
            </atombeat:groups>
        </atom:content>
        <atom:author>
            <atom:name>foo</atom:name>
        </atom:author>
    </atom:entry>
;

    
declare function local:test1() {
    let $entry := 
        <atom:entry>
            <atom:link rel='edit' href='{$config:self-link-uri-base}/foo/bar'/>
        </atom:entry>
    (: check it works on an in-memory XML fragment :)
    return atomdb:deprecated-edit-path-info( $entry ) eq '/foo/bar'
};
    


declare function local:test2() {
    let $member-created := atomdb:create-member('/foo', 'test2', $entry, ())
    let $retrieved-entry := atomdb:retrieve-member('/foo/test2')
    return (atomdb:deprecated-edit-path-info( $retrieved-entry ) eq '/foo/test2', $retrieved-entry)
};



declare function local:test3() {
    let $member-created := atomdb:create-member('/bar', 'test3', $entry, ())
    let $retrieved-entry := atomdb:retrieve-member('/bar/test3')
    return (atomdb:deprecated-edit-path-info( $retrieved-entry ) eq '/bar/test3', $retrieved-entry)
};



declare function local:test4() {
    let $member-created := atomdb:create-member('/bar', 'test4', $entry, ())
    let $member-updated := atomdb:update-member('/bar/test4', $revised-entry)
    let $retrieved-entry := atomdb:retrieve-member('/bar/test4')
    return (atomdb:deprecated-edit-path-info( $retrieved-entry ) eq '/bar/test4', $retrieved-entry)
};


declare function local:test5() {
    let $request1 :=
        <request>
            <method>POST</method>
            <path-info>/bar</path-info>
            <headers>
                <header>
                    <name>Content-Type</name>
                    <value>application/atom+xml</value>
                </header>
                <header>
                    <name>Accept</name>
                    <value>application/atom+xml</value>
                </header>
                <header>
                    <name>Slug</name>
                    <value>test5</value>
                </header>
            </headers>
        </request>
    let $response1 := atom-protocol:do-post-atom-entry($request1, $entry)
    let $log := util:log("debug", $response1)
    let $request2 :=
        <request>
            <method>GET</method>
            <path-info>/bar/test5</path-info>
            <headers>
                <header>
                    <name>Accept</name>
                    <value>application/atom+xml</value>
                </header>
            </headers>
        </request>
    let $response2 := atom-protocol:do-get-member($request2)
    let $log := util:log("debug", $response2)
    let $retrieved-entry := $response2/body/atom:entry
    return (atomdb:deprecated-edit-path-info($retrieved-entry) eq '/bar/test5', $retrieved-entry)
};
    
    
declare function local:test5b() {
    let $request1 :=
        <request>
            <method>POST</method>
            <path-info>/foo</path-info>
            <headers>
                <header>
                    <name>Content-Type</name>
                    <value>application/atom+xml</value>
                </header>
                <header>
                    <name>Accept</name>
                    <value>application/atom+xml</value>
                </header>
                <header>
                    <name>Slug</name>
                    <value>test5b</value>
                </header>
            </headers>
        </request>
    let $response1 := atom-protocol:do-post-atom-entry($request1, $entry)
(:    let $log := util:log("debug", $response1)
    let $request2 :=
        <request>
            <method>GET</method>
            <path-info>/foo/test5b</path-info>
            <headers>
                <header>
                    <name>Accept</name>
                    <value>application/atom+xml</value>
                </header>
            </headers>
        </request>
    let $response2 := atom-protocol:do-get-member($request2)
    let $log := util:log("debug", $response2)
    let $retrieved-entry := $response2/body/atom:entry
    return (atomdb:deprecated-edit-path-info($retrieved-entry) eq '/foo/test5b', $retrieved-entry):)
    return "TODO"
};
    
    
declare function local:test6() {
    let $request1 :=
        <request>
            <method>POST</method>
            <path-info>/bar</path-info>
            <headers>
                <header>
                    <name>Content-Type</name>
                    <value>application/atom+xml</value>
                </header>
                <header>
                    <name>Accept</name>
                    <value>application/atom+xml</value>
                </header>
                <header>
                    <name>Slug</name>
                    <value>test6</value>
                </header>
            </headers>
        </request>
    let $response1 := atom-protocol:do-post-atom-entry($request1, $entry)
    let $log := util:log("debug", $response1)
    let $request2 :=
        <request>
            <method>PUT</method>
            <path-info>/bar/test6</path-info>
            <headers>
                <header>
                    <name>Content-Type</name>
                    <value>application/atom+xml</value>
                </header>
                <header>
                    <name>Accept</name>
                    <value>application/atom+xml</value>
                </header>
            </headers>
        </request>
    let $response2 := atom-protocol:do-put-atom-entry($request2, $revised-entry)
    let $log := util:log("debug", $response2)
    let $request3 :=
        <request>
            <method>GET</method>
            <path-info>/bar/test6</path-info>
            <headers>
                <header>
                    <name>Accept</name>
                    <value>application/atom+xml</value>
                </header>
            </headers>
        </request>
    let $response3 := atom-protocol:do-get-member($request3)
    let $log := util:log("debug", $response3)
    let $retrieved-entry := $response3/body/atom:entry
    return (atomdb:deprecated-edit-path-info($retrieved-entry) eq '/bar/test6', $retrieved-entry)
};
    
    
declare function local:test7() { 
    let $members := atomdb:retrieve-members('/bar', false())
    let $request1 :=
        <request>
            <method>POST</method>
            <path-info>/bar</path-info>
            <headers>
                <header>
                    <name>Content-Type</name>
                    <value>application/atom+xml</value>
                </header>
                <header>
                    <name>Accept</name>
                    <value>application/atom+xml</value>
                </header>
                <header>
                    <name>Slug</name>
                    <value>test7</value>
                </header>
            </headers>
        </request>
    let $response1 := atom-protocol:do-post-atom-entry($request1, $entry)
    let $log := util:log("debug", $response1)
    let $entry := $response1/body/atom:entry
    let $new :=
        <atom:entry>
        {
            $entry/atom:id,
            <atom:published>234987243879243</atom:published>,
            $entry/atom:updated
        }
            <atom:link rel="self" type="application/atom+xml;type=entry" href="{$entry/atom:link[@rel='self']/@href}"/>
            <atom:link rel="edit" type="application/atom+xml;type=entry" href="{$entry/atom:link[@rel='edit']/@href}"/>    
        {
            <atom:author><atom:name>jill</atom:name></atom:author>,
            $entry/atom:title,
            $entry/app:control
        }
            <atom:content type="application/vnd.chassis-manta+xml">
                <atombeat:groups>
                    <atombeat:group id="foo">
                        <atombeat:member>jill</atombeat:member>
                    </atombeat:group>
                </atombeat:groups>
            </atom:content>
        </atom:entry>
    let $log := util:log("debug", $new)
    let $log := util:log("debug", atomdb:deprecated-edit-path-info($new)) 
    let $log := util:log("debug", atomdb:deprecated-edit-path-info($entry))
    let $updated := atomdb:update-member('/bar/test7', $new)
    let $log := atomdb:deprecated-edit-path-info($new)
    let $log := atomdb:deprecated-edit-path-info($entry)
    return (atomdb:deprecated-edit-path-info($entry) eq '/bar/test7', atomdb:deprecated-edit-path-info($new) eq '/bar/test7', $entry, $new)
};
    

    

let $log := util:log("debug", "hello world")
let $login := xmldb:login("/" , 'admin' , '')



    
let $setup :=

    let $collections-to-remove := ('/db/atombeat', '/db/system/versions', '/db/config/db')
    let $clean :=
        for $c in $collections-to-remove return if (xmldb:collection-available($c)) then xmldb:remove($c) else ()
    let $descriptor := 
    <atombeat:security-descriptor>
        <atombeat:acl>
            <atombeat:ace>
                <atombeat:type>allow</atombeat:type>
                <atombeat:recipient type="user">*</atombeat:recipient>
                <atombeat:permission>*</atombeat:permission>
            </atombeat:ace>
        </atombeat:acl>
    </atombeat:security-descriptor>
    let $security-setup := atomsec:store-workspace-descriptor($descriptor)
    
    let $foo-request := 
        <request>
            <method>put</method>
            <path-info>/foo</path-info>
            <headers>
                <header>
                    <name>content-type</name>
                    <value>application/atom+xml</value>
                </header>
                <header>
                    <name>accept</name>
                    <value>application/atom+xml</value>
                </header>
            </headers>
        </request>
    let $foo-feed :=
        <atom:feed>
            <atom:title>test collection without versioning</atom:title>
        </atom:feed>
    let $foo-response := atom-protocol:do-put-atom-feed($foo-request, $foo-feed)

    let $bar-request := 
        <request>
            <method>put</method>
            <path-info>/bar</path-info>
            <headers>
                <header>
                    <name>content-type</name>
                    <value>application/atom+xml</value>
                </header>
                <header>
                    <name>accept</name>
                    <value>application/atom+xml</value>
                </header>
            </headers>
        </request>
    let $bar-feed :=
        <atom:feed atombeat:enable-versioning="true">
            <atom:title>test collection with versioning</atom:title>
        </atom:feed>
    let $bar-response := atom-protocol:do-put-atom-feed($bar-request, $bar-feed)
    
    return ($foo-response, $bar-response)



let $log := util:log("debug", $setup)
    
let $type := response:set-header('Content-Type', 'text/plain')
return (
    "= test1 =", local:test1(),
    "= test2 =", local:test2(),
    "= test3 =", local:test3(),
    "= test4 =", local:test4(),
    "= test5 =", local:test5(), 
    "= test6 =", local:test6(),
    "= test7 =", local:test7(), 
    "= test5b =", local:test5b() 
)
