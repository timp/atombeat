xquery version "1.0";

import module namespace hp = "http://www.cggh.org/2010/atombeat/xquery/history-protocol" at "lib/history-protocol.xqm" ;

let $login := xmldb:login( "/" , "admin" , "" )

return hp:do-service()