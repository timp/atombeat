xquery version "1.0";

declare namespace atom = "http://www.w3.org/2005/Atom";
declare namespace app = "http://www.w3.org/2007/app";

import module namespace config = "http://purl.org/atombeat/xquery/config" at "config/shared.xqm" ;

let $login := xmldb:login( "/" , $config:exist-user , $config:exist-password )

let $set := response:set-header( "Content-Type" , "application/atomsvc+xml" )

return 

    <app:service 
        xmlns:app="http://www.w3.org/2007/app"
        xmlns:atom="http://www.w3.org/2005/Atom">
        <app:workspace>
            <atom:title type="text">AtomBeat Collections</atom:title>
            {
                collection( $config:base-collection-path )/atom:feed/app:collection
            }
        </app:workspace>
    </app:service>
    