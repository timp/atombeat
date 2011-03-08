import module namespace util = "http://exist-db.org/xquery/util" ; 

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace atomsec = "http://purl.org/atombeat/xquery/atom-security" at "../lib/atom-security.xqm" ;
import module namespace atom-protocol = "http://purl.org/atombeat/xquery/atom-protocol" at "../lib/atom-protocol.xqm" ;


declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;
declare namespace app = "http://www.w3.org/2007/app";


declare variable $entry :=
    <atom:entry xmlns:atom="http://www.w3.org/2005/Atom">
        <atom:title type="text">foo</atom:title>
    </atom:entry>
;

    
    
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
    return "TODO"
};
    
    
    
declare function local:test7() { 
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
                    <value>test7</value>
                </header>
            </headers>
        </request>
    let $response1 := atom-protocol:do-post-atom-entry($request1, $entry)
    let $entry := $response1/body/atom:entry
    let $log := util:log("debug", atomdb:deprecated-edit-path-info($entry))
    return "TODO"
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
    return $foo-response



let $log := util:log("debug", $setup)
    
let $type := response:set-header('Content-Type', 'text/plain')
return (
    "= test7 =", local:test7(), 
    "= test5b =", local:test5b() 
)
