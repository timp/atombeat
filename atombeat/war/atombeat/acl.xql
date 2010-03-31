xquery version "1.0";

import module namespace acl-protocol = "http://www.cggh.org/2010/atombeat/xquery/acl-protocol" at "lib/acl-protocol.xqm" ;

let $login := xmldb:login( "/" , "admin" , "" )

return acl-protocol:do-service()