xquery version "1.0";

import module namespace config = "http://purl.org/atombeat/xquery/config" at "config/shared.xqm" ;
import module namespace service-protocol = "http://purl.org/atombeat/xquery/service-protocol" at "lib/service-protocol.xqm" ;

declare function local:error() {
    let $status := response:set-status-code(500)
    let $content-type := response:set-header( "Content-Type" , "text/plain" )
    return ( $util:exception , $util:exception-message )
};

let $login := xmldb:login( "/" , $config:exist-user , $config:exist-password )

return util:catch( '*' , service-protocol:main() , local:error() )
