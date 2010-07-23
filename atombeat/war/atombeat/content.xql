xquery version "1.0";

import module namespace ap = "http://purl.org/atombeat/xquery/atom-protocol" at "lib/atom-protocol.xqm" ;

let $login := xmldb:login( "/" , "admin" , "" )

return ap:main()