import module namespace util = "http://exist-db.org/xquery/util" ; 
import module namespace plugin-util = "http://purl.org/atombeat/xquery/plugin-util" at "../lib/plugin-util.xqm" ;

declare namespace atom = "http://www.w3.org/2005/Atom" ;

let $request :=
    <request>
        <path-info>/test</path-info>
        <method>GET</method>
        <headers>
            <header>
                <name>Accept</name>
                <value>application/atom+xml</value>
            </header>
        </headers>
        <parameters/>
        <user>adam</user>
        <roles>
            <role>ROLE_ADMINISTRATOR</role>
            <role>ROLE_USER</role>
        </roles>
    </request>     

let $log := util:log( "debug" , $request )
let $response := plugin-util:atom-protocol-do-get( $request )
let $log := util:log( "debug" , $response )

let $x := response:set-header( "content-type" , "text/plain" )
let $x := response:set-status-code( 200 )
return $response
