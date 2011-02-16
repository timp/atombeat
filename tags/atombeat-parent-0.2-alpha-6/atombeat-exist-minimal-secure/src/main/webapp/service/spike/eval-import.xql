import module namespace util = "http://exist-db.org/xquery/util" ; 
import module namespace plugin-util = "http://purl.org/atombeat/xquery/plugin-util" at "../lib/plugin-util.xqm" ;

declare namespace atom = "http://www.w3.org/2005/Atom" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;

let $login := xmldb:login( "/" , $config:exist-user , $config:exist-password )

let $request := 
    <request>
        <path-info>/tes</path-info>
        <headers/>
        <parameters/>
        <user>adam</user>
        <roles>
            <role>ROLE_ADMINISTRATOR</role>
        </roles>
    </request>
    
let $entity :=
    <atom:entry>
        <atom:title>devious cunning</atom:title>
    </atom:entry>
    
let $x := response:set-header( "Content-Type" , "text/plain" )

return plugin-util:atom-protocol-do-post-atom-entry( $request , $entity )