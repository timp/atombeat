xquery version "1.0";

import module namespace security-protocol = "http://purl.org/atombeat/xquery/security-protocol" at "lib/security-protocol.xqm" ;

let $login := xmldb:login( "/" , "admin" , "" )

return security-protocol:main()