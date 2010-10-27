xquery version "1.0";

module namespace tombstones-plugin = "http://purl.org/atombeat/xquery/tombstones-plugin";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;
declare namespace at = "http://purl.org/atompub/tombstones/1.0";

import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace tombstone-db = "http://purl.org/atombeat/xquery/tombstone-db" at "../lib/tombstone-db.xqm" ;




declare function tombstones-plugin:before(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string?
) as item()*
{

	let $message := concat( "before: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "info" , $message )
	
	return 
	
	    if ( $operation = $CONSTANT:OP-DELETE-MEMBER )
	    then 
	        let $prepare := tombstones-plugin:before-delete-member( $request-path-info )
	        return $request-data
	    else $request-data
	
};




declare function tombstones-plugin:before-delete-member(
    $request-path-info as xs:string 
) as empty()
{

    let $user-name := request:get-attribute( $config:user-name-request-attribute-key )
    let $comment := request:get-header( "X-Atom-Tombstone-Comment" )
    let $deleted-entry := tombstone-db:create-deleted-entry( $request-path-info , $user-name , $comment )
    let $prepared := request:set-attribute( "tombstone" , $deleted-entry )
    return ()
    
};



declare function tombstones-plugin:after(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

	let $message := concat( "after: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "info" , $message )
	
    return 
    
        if ( $operation = $CONSTANT:OP-DELETE-MEMBER )
        then tombstones-plugin:after-delete-member( $request-path-info , $response )
        else $response
    
}; 




declare function tombstones-plugin:after-delete-member(
    $request-path-info as xs:string ,
    $response as element(response)
) as element(response)
{

    let $deleted-entry := request:get-attribute("tombstone")
    let $tombstone-stored := tombstone-db:erect-tombstone( $request-path-info , $deleted-entry )
    
    return

        <response>
            <status>200</status>
            <headers>
                <header>
                    <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                    <value>application/atomdeleted+xml</value>
                </header>
            </headers>
            <body>{$deleted-entry}</body>
        </response>

};



declare function tombstones-plugin:after-error(
    $operation as xs:string ,
    $request-path-info as xs:string ,
    $response as element(response)
) as element(response)
{

    (: TODO :)
    $response
    
}; 





