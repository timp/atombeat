xquery version "1.0";

import module namespace security-protocol = "http://purl.org/atombeat/xquery/security-protocol" at "lib/security-protocol.xqm" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "config/shared.xqm" ;

let $login := xmldb:login( "/" , $config:exist-user , $config:exist-password )

return security-protocol:main()