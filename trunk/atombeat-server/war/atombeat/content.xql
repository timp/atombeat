xquery version "1.0";

import module namespace ap = "http://www.cggh.org/2010/xquery/atom-protocol" at "lib/atom-protocol.xqm" ;

let $login := xmldb:login( "/" , "admin" , "" )

return ap:do-service()