xquery version "1.0";

import module namespace ap = "http://purl.org/atombeat/xquery/atom-protocol" at "lib/atom-protocol.xqm" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "config/shared.xqm" ;

declare function local:error() {
    let $status := response:set-status-code(500)
    let $content-type := response:set-header( "Content-Type" , "text/plain" )
    return ( $util:exception , $util:exception-message )
};

let $login := xmldb:login( "/" , $config:exist-user , $config:exist-password )

return util:catch( '*' , ap:main() , local:error() )