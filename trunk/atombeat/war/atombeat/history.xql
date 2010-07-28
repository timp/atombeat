xquery version "1.0";

import module namespace hp = "http://purl.org/atombeat/xquery/history-protocol" at "lib/history-protocol.xqm" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "config/shared.xqm" ;

let $login := xmldb:login( "/" , $config:exist-user , $config:exist-password )

return hp:main()