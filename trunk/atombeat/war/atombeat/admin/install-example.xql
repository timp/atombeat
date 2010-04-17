declare namespace atom = "http://www.w3.org/2005/Atom" ;

import module namespace response = "http://exist-db.org/xquery/response" ;

import module namespace config = "http://atombeat.org/xquery/config" at "../config/shared.xqm" ;
import module namespace atomsec = "http://atombeat.org/xquery/atom-security" at "../lib/atom-security.xqm" ;

let $login := xmldb:login( "/" , "admin" , "" )

let $global-acl-installed := atomsec:store-global-acl( $config:default-global-acl )

let $status-set := response:set-status-code( 200 )

let $response-content-type-set := response:set-header( "Content-Type" , "text/plain" )

return "OK" 