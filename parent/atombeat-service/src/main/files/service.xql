xquery version "1.0";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace app = "http://www.w3.org/2007/app" ;
declare namespace xhtml = "http://www.w3.org/1999/xhtml" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "config/shared.xqm" ;

let $login := xmldb:login( "/" , $config:exist-user , $config:exist-password )

let $set := response:set-header( "Content-Type" , "application/atomsvc+xml" )

return 

    <app:service 
        xmlns:app="http://www.w3.org/2007/app"
        xmlns:atom="http://www.w3.org/2005/Atom">
        <app:workspace>
        {
            if ( $config:workspace-title instance of xs:string )
            then <atom:title type="text">{$config:workspace-title}</atom:title>
            else if ( $config:workspace-title instance of element(xhtml:div) )
            then <atom:title type="xhtml">{$config:workspace-title}</atom:title>
            else <atom:title type="text">unnamed workspace</atom:title>
            ,
            if ( $config:workspace-summary instance of xs:string )
            then <atom:summary type="text">{$config:workspace-summary}</atom:summary>
            else if ( $config:workspace-summary instance of element(xhtml:div) )
            then <atom:summary type="xhtml">{$config:workspace-summary}</atom:summary>
            else ()            
        }
            {
                collection( $config:base-collection-path )/atom:feed/app:collection
            }
        </app:workspace>
    </app:service>
    