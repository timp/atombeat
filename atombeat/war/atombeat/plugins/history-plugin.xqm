xquery version "1.0";

module namespace history-plugin = "http://purl.org/atombeat/xquery/history-plugin";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;

(: see http://tools.ietf.org/html/draft-snell-atompub-revision-00 :)
declare namespace ar = "http://purl.org/atompub/revision/1.0" ;

import module namespace request = "http://exist-db.org/xquery/request" ;
import module namespace response = "http://exist-db.org/xquery/response" ;
import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace v="http://exist-db.org/versioning" ;

import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "constants.xqm" ;

import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace mime = "http://purl.org/atombeat/xquery/mime" at "../lib/mime.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace atomsec = "http://purl.org/atombeat/xquery/atom-security" at "../lib/atom-security.xqm" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;




declare function history-plugin:before(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string?
) as item()*
{
	
	let $message := concat( "history plugin, before: " , $operation , ", request-path-info: " , $request-path-info ) 
	
	return
	
		if ( $operation = $CONSTANT:OP-CREATE-COLLECTION )
		
		then history-plugin:before-create-collection( $request-path-info , $request-data )
		
		else if ( $operation = $CONSTANT:OP-CREATE-MEMBER )
		
		then history-plugin:before-create-member( $request-path-info , $request-data )
		
		else if ( $operation = $CONSTANT:OP-UPDATE-MEMBER )
		
		then history-plugin:before-update-member( $request-path-info , $request-data )
		
		else
	
			$request-data
};




declare function history-plugin:before-create-collection(
	$request-path-info as xs:string ,
	$request-data as element(atom:feed)
) as item()*
{

	let $message := concat( "history plugin, before create-collection, request-path-info: " , $request-path-info ) 

	let $enable-history := xs:boolean( $request-data/@atombeat:enable-versioning )
	
	let $history-enabled :=
		
		if ( $enable-history )
		
		then
		
			let $collection-db-path := atomdb:request-path-info-to-db-path( $request-path-info )
			
			return xutil:enable-versioning( $collection-db-path )

		else ()		
	
	return $request-data

};




declare function history-plugin:before-create-member(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry)
) as item()*
{

    let $collection-db-path := atomdb:request-path-info-to-db-path( $request-path-info )
    let $versioning-enabled := xutil:is-versioning-enabled( $collection-db-path )

    return 
    
        if ( $versioning-enabled )
        
        then
        
        	(: add a revision comment to the incoming entry :)
        	
            let $comment := request:get-header("X-Atom-Revision-Comment")
            let $comment := if ( empty( $comment ) or $comment = "" ) then "initial revision" else $comment
            
        	let $prepared-entry := history-plugin:prepare-entry( $request-data , $comment )
        	
        	return $prepared-entry

        else $request-data

};




declare function history-plugin:before-update-member(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry)
) as item()*
{

    let $collection-path-info := text:groups( $request-path-info , "^(.+)/[^/]+$" )[2]
    let $collection-db-path := atomdb:request-path-info-to-db-path( $collection-path-info )
    let $versioning-enabled := xutil:is-versioning-enabled( $collection-db-path )

    return 
    
        if ( $versioning-enabled )
        
        then
        
        	(: add a revision comment to the incoming entry :)
        	
            let $comment := request:get-header("X-Atom-Revision-Comment")
        	let $prepared-entry := history-plugin:prepare-entry( $request-data , $comment )
        	
        	return $prepared-entry

        else $request-data

};




declare function history-plugin:prepare-entry(
    $request-data as element(atom:entry) ,
    $comment as xs:string?
) as element(atom:entry)
{

    (: filter incoming request data :)
    
    let $reserved :=
        <reserved>
            <elements namespace-uri="http://purl.org/atompub/revision/1.0">
                <element>comment</element>
            </elements>
            <atom-links>
                <link rel="history"/>
            </atom-links>
        </reserved>
    
    let $filtered-entry := atomdb:filter( $request-data , $reserved )
    
    (: modify incoming entry to include revision comment :)
    
    let $user-name := request:get-attribute( $config:user-name-request-attribute-key )
    let $published := current-dateTime()

    let $revision-comment := 
        <ar:comment>
            <atom:author>
                {
                    if ( $config:user-name-is-email ) then
                    <atom:email>{$user-name}</atom:email>
                    else
                    <atom:name>{$user-name}</atom:name>                    
                }
            </atom:author>
            <atom:updated>{$published}</atom:updated>
            <atom:summary>{$comment}</atom:summary>
        </ar:comment>
    
    let $prepared-entry :=    
        <atom:entry>
        {
            $filtered-entry/attribute::* ,
            $filtered-entry/child::* ,
            $revision-comment
        }
        </atom:entry>  
        
    return $prepared-entry    

};




declare function history-plugin:after(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$response as element(response) 
) as element(response)
{

	let $message := concat( "history plugin, after: " , $operation , ", request-path-info: " , $request-path-info ) 

	return
		
		if ( $operation = $CONSTANT:OP-RETRIEVE-MEMBER )
		
		then history-plugin:after-retrieve-member( $request-path-info , $response )

		else if ( $operation = $CONSTANT:OP-CREATE-MEMBER )
		
		then history-plugin:after-create-member( $request-path-info , $response )

		else if ( $operation = $CONSTANT:OP-UPDATE-MEMBER )
		
		then history-plugin:after-update-member( $request-path-info , $response )
		
		else if ( $operation = $CONSTANT:OP-LIST-COLLECTION )
		
		then history-plugin:after-list-collection( $request-path-info , $response )

		else 

			$response

}; 




declare function history-plugin:after-retrieve-member(
	$request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

	let $response-data := history-plugin:append-history-link( $response/body/atom:entry )

	return 
	    
	    <response>
	    {
	        $response/status ,
	        $response/headers
	    }
	        <body>{$response-data}</body>
	    </response>

};




declare function history-plugin:after-create-member(
	$request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

	let $response-data := history-plugin:append-history-link( $response/body/atom:entry )

    return
    
	    <response>
	    {
	        $response/status ,
	        $response/headers
	    }
	        <body>{$response-data}</body>
	    </response>

};




declare function history-plugin:after-update-member(
	$request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

	let $response-data := history-plugin:append-history-link( $response/body/atom:entry ) 

	return
	
    	<response>
        {
            $response/status ,
            $response/headers
        }
            <body>{$response-data}</body>
        </response>
	
};




declare function history-plugin:after-list-collection(
	$collection-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

    let $collection-db-path := atomdb:request-path-info-to-db-path( $collection-path-info )
    let $versioning-enabled := xutil:is-versioning-enabled( $collection-db-path )
    
    (: check whether versioning here, to reduce function calls if not :)
    
    return

        if ( $versioning-enabled ) then
        
            let $response-data := $response/body/atom:feed
            
        	let $response-data := 
        		<atom:feed>
        		{
        			$response-data/attribute::* ,
        			$response-data/child::*[not( local-name(.) = $CONSTANT:ATOM-ENTRY and namespace-uri(.) = $CONSTANT:ATOM-NSURI )] ,
        			for $entry in $response-data/atom:entry
        			return history-plugin:append-history-link( $entry )
        		}
        		</atom:feed>
        	
        	return
        	
            	<response>
                {
                    $response/status ,
                    $response/headers
                }
                    <body>{$response-data}</body>
                </response>
                
        else $response        
    	
};




declare function history-plugin:append-history-link (
	$response-entry as element(atom:entry)
) as element(atom:entry)
{

    let $entry-path-info := atomdb:edit-path-info( $response-entry )
	let $collection-path-info := atomdb:collection-path-info( $response-entry )
	
	return
	
	    if ( exists( $entry-path-info ) )
	    
	    then
	
            let $collection-db-path := atomdb:request-path-info-to-db-path( $collection-path-info )
            let $versioning-enabled := xutil:is-versioning-enabled( $collection-db-path )
            
        	let $history-uri := concat( $config:history-service-url , $entry-path-info )
        	
        	let $response-entry :=
        	
        	    if ( $versioning-enabled )
        	   
        	    then
        	   
        	        let $history-link := <atom:link rel="history" href="{$history-uri}" type="{$CONSTANT:MEDIA-TYPE-ATOM-FEED}"/>
        	       
        	        return

                        <atom:entry>
                		{
                			$response-entry/attribute::* ,
                			$response-entry/child::* ,
                			$history-link
                		}
                		</atom:entry>
            		
        		else $response-entry
        
        	return $response-entry
        	
        else $response-entry                        
	
};




