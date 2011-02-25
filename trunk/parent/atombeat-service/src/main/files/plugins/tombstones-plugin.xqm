xquery version "1.0";

module namespace tombstones-plugin = "http://purl.org/atombeat/xquery/tombstones-plugin";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;
declare namespace at = "http://purl.org/atompub/tombstones/1.0";

import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;
import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace tombstone-db = "http://purl.org/atombeat/xquery/tombstone-db" at "../lib/tombstone-db.xqm" ;




declare function tombstones-plugin:before(
	$operation as xs:string ,
	$request as element(request) ,
	$entity as item()*
) as item()*
{

    let $request-path-info := $request/path-info/text()
	let $message := concat( "before: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "info" , $message )
	
	return 
	
        if ( $operation = ( $CONSTANT:OP-DELETE-MEMBER , $CONSTANT:OP-DELETE-MEDIA ) )
        then tombstones-plugin:before-delete-member-or-media( $request )
            
        else if ( $entity instance of element(atom:feed) )
        then tombstones-plugin:filter-feed($entity) 
        
	    else $entity
	
};




declare function tombstones-plugin:before-delete-member-or-media(
	$request as element(request) 
) as item()*
{

    let $request-path-info := $request/path-info/text()
    
    let $member-path-info :=
        if ( ends-with( $request-path-info , ".media" ) )
(:        then replace( $request-path-info , "^(.*)\.media$" , "$1.atom" ) :)
        then replace( $request-path-info , "^(.*)\.media$" , "$1" )
        else $request-path-info

    let $collection-path-info := text:groups( $request-path-info , "^(.+)/[^/]+$" )[2]
    
    let $feed := atomdb:retrieve-feed-without-entries( $collection-path-info )
    
    let $set-request-attributes :=
    
        if ( xs:boolean( $feed/@atombeat:enable-tombstones ) )
        
        then
    
            let $user-name := $request/user/text()
            let $comment := xutil:get-header( "X-Atom-Tombstone-Comment" , $request )
            let $deleted-entry := tombstone-db:create-deleted-entry( $member-path-info , $user-name , $comment )
            (: pass to after phase via request attribute so tombstone can be stored :)
            return 
                <attributes>
                    <attribute>
                        <name>atombeat.tombstone-plugin.tombstone</name>
                        <value>{$deleted-entry}</value>
                    </attribute>
                </attributes>
            
        else ()
        
    return ( <void/> , $set-request-attributes )
    
};




declare function tombstones-plugin:filter-feed(
    $feed as element(atom:feed)
) as element(atom:feed)
{
    <atom:feed>
    {
        $feed/attribute::* ,
        $feed/child::*[not( . instance of element(at:deleted-entry) )]
    }
    </atom:feed>
};




declare function tombstones-plugin:after(
	$operation as xs:string ,
	$request as element(request) ,
	$response as element(response)
) as item()*
{

    let $request-path-info := $request/path-info/text()
	let $message := concat( "after: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "info" , $message )
	
    return 
    
        if ( $operation = $CONSTANT:OP-DELETE-MEMBER or $operation = $CONSTANT:OP-DELETE-MEDIA )
        then tombstones-plugin:after-delete-member-or-media( $request , $response )
        else if ( $operation = $CONSTANT:OP-LIST-COLLECTION ) 
        then tombstones-plugin:after-list-collection( $request-path-info , $response )
        else $response
    
}; 




declare function tombstones-plugin:after-delete-member-or-media(
	$request as element(request) ,
    $response as element(response)
) as item()*
{

    let $request-path-info := $request/path-info/text()

    let $member-path-info :=
        if ( ends-with( $request-path-info , ".media" ) )
        (: then replace( $request-path-info , "^(.*)\.media$" , "$1.atom" ) :)
        then replace( $request-path-info , "^(.*)\.media$" , "$1" )
        else $request-path-info

    let $deleted-entry := $request/attributes/attribute[name eq 'atombeat.tombstone-plugin.tombstone']/value/at:deleted-entry
    
    return 
    
        if ( exists( $deleted-entry ) )
        
        then

            let $tombstone-stored := tombstone-db:erect-tombstone( $member-path-info , $deleted-entry )
            
            return
        
                <response>
                    <status>200</status>
                    <headers>
                        <header>
                            <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                            <value>application/atomdeleted+xml</value>
                        </header>
                    </headers>
                    <body type='xml'>{$deleted-entry}</body>
                </response>
                
        else $response

};




declare function tombstones-plugin:after-list-collection(
    $request-path-info as xs:string ,
    $response as element(response)
) as item()*
{

    let $feed := $response/body/atom:feed
    
    return 
    
        if ( exists( $feed) and xs:boolean( $feed/@atombeat:enable-tombstones ) )
        
        then
        
            let $augmented-feed :=
            
                <atom:feed>
                {
                    $feed/attribute::* ,
                    $feed/child::* ,
                    tombstone-db:retrieve-tombstones( $request-path-info , xs:boolean( $feed/atombeat:recursive ) ) 
                }
                </atom:feed>
                
            return 
            
                <response>
                {
                    $response/status ,
                    $response/headers
                }
                    <body type='xml'>{$augmented-feed}</body>
                </response>
        
        else $response
        
};




declare function tombstones-plugin:after-error(
	$operation as xs:string ,
	$request as element(request) ,
	$response as element(response)
) as item()*
{

    let $request-path-info := $request/path-info/text()
    
    return
        
        if ( 
            $operation = $CONSTANT:OP-ATOM-PROTOCOL-ERROR
            and $response/status = 404
            and tombstone-db:tombstone-available( $request-path-info )
            (: TODO check tombstones enabled on collection? :)
        )
        then
    
            let $deleted-entry := tombstone-db:retrieve-tombstone( $request-path-info )
            
            return
        
                (: modify the response from 404 to 410 :)
                <response>
                    <status>410</status>
                    <headers>
                        <header>
                            <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                            <value>application/atomdeleted+xml</value>
                        </header>
                    </headers>
                    <body type='xml'>{$deleted-entry}</body>
                </response>
            
        else $response
    
}; 





