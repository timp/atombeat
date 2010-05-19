xquery version "1.0";

import module namespace security-protocol = "http://atombeat.org/xquery/security-protocol" at "lib/security-protocol.xqm" ;

let $login := xmldb:login( "/" , "admin" , "" )

return security-protocol:do-service()