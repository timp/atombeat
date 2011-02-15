xquery version "1.0";

import module namespace config = "http://purl.org/atombeat/xquery/config" at "config/shared.xqm" ;
import module namespace service-protocol = "http://purl.org/atombeat/xquery/service-protocol" at "lib/service-protocol.xqm" ;

let $login := xmldb:login( "/" , $config:exist-user , $config:exist-password )
return service-protocol:main()