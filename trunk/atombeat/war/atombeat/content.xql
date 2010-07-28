xquery version "1.0";

import module namespace ap = "http://purl.org/atombeat/xquery/atom-protocol" at "lib/atom-protocol.xqm" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "config/shared.xqm" ;

let $login := xmldb:login( "/" , $config:exist-user , $config:exist-password )

return ap:main()