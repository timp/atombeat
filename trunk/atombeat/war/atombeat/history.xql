xquery version "1.0";

import module namespace hp = "http://purl.org/atombeat/xquery/history-protocol" at "lib/history-protocol.xqm" ;

let $login := xmldb:login( "/" , "admin" , "" )

return hp:main()