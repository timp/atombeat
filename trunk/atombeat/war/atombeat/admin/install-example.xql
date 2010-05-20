declare namespace atom = "http://www.w3.org/2005/Atom" ;

import module namespace response = "http://exist-db.org/xquery/response" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace atomsec = "http://purl.org/atombeat/xquery/atom-security" at "../lib/atom-security.xqm" ;

let $login := xmldb:login( "/" , "admin" , "" )

let $workspace-descriptor-installed := atomsec:store-workspace-descriptor( $config:default-workspace-security-descriptor )

let $status-set := response:set-status-code( 200 )

let $response-content-type-set := response:set-header( "Content-Type" , "text/plain" )

return "OK" 