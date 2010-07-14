declare namespace atom = "http://www.w3.org/2005/Atom" ;

import module namespace response = "http://exist-db.org/xquery/response" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace security-config = "http://purl.org/atombeat/xquery/security-config" at "../config/security.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace atomsec = "http://purl.org/atombeat/xquery/atom-security" at "../lib/atom-security.xqm" ;

let $login := xmldb:login( "/" , "admin" , "" )

let $install-workspace-descriptor := atomsec:store-workspace-descriptor( $security-config:default-workspace-security-descriptor )

let $delete-test-collection := atomdb:delete-collection( "/test" , true() )

let $create-test-collection := atomdb:create-collection( "/test" , <atom:feed><atom:title>Test Collection</atom:title></atom:feed> )

let $status-set := response:set-status-code( 200 )

let $response-content-type-set := response:set-header( "Content-Type" , "text/plain" )

return "OK" 