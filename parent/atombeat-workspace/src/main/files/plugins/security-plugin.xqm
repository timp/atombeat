xquery version "1.0";

module namespace security-plugin = "http://purl.org/atombeat/xquery/security-plugin";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;
(: see http://tools.ietf.org/html/draft-mehta-atom-inline-01 :)
declare namespace ae = "http://purl.org/atom/ext/" ;


import module namespace request = "http://exist-db.org/xquery/request" ;
import module namespace response = "http://exist-db.org/xquery/response" ;
import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace security-config = "http://purl.org/atombeat/xquery/security-config" at "../config/security.xqm" ;

import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace mime = "http://purl.org/atombeat/xquery/mime" at "../lib/mime.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace atomsec = "http://purl.org/atombeat/xquery/atom-security" at "../lib/atom-security.xqm" ;




declare function security-plugin:before(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string?
) as item()*
{
	
	if ( $security-config:enable-security )
	
	then

    	let $forbidden := atomsec:is-denied( $operation , $request-path-info , $request-media-type )
    	
    	return 
    	
    		if ( $forbidden )
    		
    		then 
    		
    		    let $response-data := "The server understood the request, but is refusing to fulfill it. Authorization will not help and the request SHOULD NOT be repeated."
    			
    			return 
    			
    			    <response>
    			        <status>{$CONSTANT:STATUS-CLIENT-ERROR-FORBIDDEN}</status>
    			        <headers>
    			            <header>
    			                <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
    			                <value>{$CONSTANT:MEDIA-TYPE-TEXT}</value>
    			            </header>
    			        </headers>
    			        <body>{$response-data}></body>
    			    </response>
    			
            else if ( 
                $operation = $CONSTANT:OP-CREATE-MEMBER 
                or $operation = $CONSTANT:OP-UPDATE-MEMBER 
                or $operation = $CONSTANT:OP-CREATE-COLLECTION 
                or $operation = $CONSTANT:OP-UPDATE-COLLECTION
            )
            
            then 
            
                let $request-data := security-plugin:strip-descriptor-links( $request-data )
    			return $request-data
    			
            else if (
                $operation = $CONSTANT:OP-MULTI-CREATE
            )
            
            then 

                (: TODO check permissions to retrieve media locally :)
                
                let $request-data := 
                    <atom:feed>
                    {
                        for $entry in $request-data/atom:entry
                        return security-plugin:strip-descriptor-links( $entry )
                    }
                    </atom:feed>

                return $request-data
    			
    		else $request-data

    else $request-data

};




declare function security-plugin:strip-descriptor-links(
    $request-data as element()
) as element()
{

    let $reserved :=
        <reserved>
            <atom-links>
                <link rel="http://purl.org/atombeat/rel/security-descriptor"/>
                <link rel="http://purl.org/atombeat/rel/media-security-descriptor"/>
            </atom-links>
        </reserved>
        
    let $filtered := atomdb:filter( $request-data , $reserved )
    
    return $filtered
};




declare function security-plugin:after(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

    if ( $security-config:enable-security )

    then
            
    	if ( $operation = $CONSTANT:OP-CREATE-MEMBER )
    	
    	then security-plugin:after-create-member( $request-path-info , $response)

        else if ( $operation = $CONSTANT:OP-CREATE-MEDIA )
        
        then security-plugin:after-create-media( $request-path-info , $response )

        else if ( $operation = $CONSTANT:OP-UPDATE-MEDIA )
        
        then security-plugin:after-update-media( $request-path-info , $response )

    	else if ( $operation = $CONSTANT:OP-CREATE-COLLECTION )
    	
    	then security-plugin:after-create-collection( $request-path-info , $response )

    	else if ( $operation = $CONSTANT:OP-UPDATE-COLLECTION )
    	
    	then security-plugin:after-update-collection( $request-path-info , $response )

    	else if ( $operation = $CONSTANT:OP-LIST-COLLECTION )
    	
    	then security-plugin:after-list-collection( $request-path-info , $response )
    	
    	else if ( $operation = $CONSTANT:OP-RETRIEVE-MEMBER )
    	
    	then security-plugin:after-retrieve-member( $request-path-info , $response )

    	else if ( $operation = $CONSTANT:OP-UPDATE-MEMBER )
    	
    	then security-plugin:after-update-member( $request-path-info , $response )
    	
    	else if ( $operation = $CONSTANT:OP-MULTI-CREATE ) 
    	
    	then security-plugin:after-multi-create( $request-path-info , $response )

        else $response

    else $response

}; 




declare function security-plugin:after-create-member(
	$request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

    let $response-data := $response/body/atom:entry
    
	let $entry-uri := $response-data/atom:link[@rel="edit"]/@href
	
	let $entry-path-info := substring-after( $entry-uri , $config:content-service-url )

	(: if security is enabled, install default resource ACL :)
	let $member-descriptor-installed := security-plugin:install-member-descriptor( $request-path-info , $entry-path-info )
	
    let $response-data := security-plugin:augment-entry( $response-data )

	return
	
    	<response>
        {
            $response/status ,
            $response/headers
        }
            <body>{$response-data}</body>
        </response>
	
};



declare function security-plugin:after-multi-create(
	$request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

    let $response-data := $response/body/atom:feed
	
    let $response-data := 
        <atom:feed>
        {
            for $entry in $response-data/atom:entry
            let $entry-uri := $entry/atom:link[@rel="edit"]/@href
            let $entry-path-info := substring-after( $entry-uri , $config:content-service-url )
            (: if security is enabled, install default resource ACL :)
            let $member-descriptor-installed := security-plugin:install-member-descriptor( $request-path-info , $entry-path-info )
            let $media-uri := $entry/atom:link[@rel="edit-media"]/@href
            let $media-path-info := substring-after( $media-uri , $config:content-service-url )
            (: if security is enabled, install default resource ACL :)
            let $media-descriptor-installed := 
                if ( exists( $media-path-info ) and $media-path-info != "" ) then security-plugin:install-media-descriptor( $request-path-info , $media-path-info )
                else () 
            return security-plugin:augment-entry( $entry )
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
	
};



declare function security-plugin:after-update-member(
	$request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

    let $response-data := $response/body/atom:entry
    let $response-data := security-plugin:augment-entry( $response-data )

	return
	
    	<response>
        {
            $response/status ,
            $response/headers
        }
            <body>{$response-data}</body>
        </response>
	
};




declare function security-plugin:after-create-media(
    $request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

    let $response-data := $response/body/atom:entry

    let $entry-uri := $response-data/atom:link[@rel="edit"]/@href
    
    let $entry-path-info := substring-after( $entry-uri , $config:content-service-url )

    (: if security is enabled, install default resource ACL :)
    let $member-descriptor-installed := security-plugin:install-member-descriptor( $request-path-info , $entry-path-info )

    let $media-uri := $response-data/atom:link[@rel="edit-media"]/@href
    
    let $media-path-info := substring-after( $media-uri , $config:content-service-url )

    (: if security is enabled, install default resource ACL :)
    let $media-descriptor-installed := security-plugin:install-media-descriptor( $request-path-info , $media-path-info )
    
    let $response-data := security-plugin:augment-entry( $response-data )

	return
	
    	<response>
        {
            $response/status ,
            $response/headers
        }
            <body>{$response-data}</body>
        </response>

};



declare function security-plugin:after-update-media(
    $request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

    let $response-data := $response/body/atom:entry

    let $response-data := security-plugin:augment-entry( $response-data )

	return
	
    	<response>
        {
            $response/status ,
            $response/headers
        }
            <body>{$response-data}</body>
        </response>

};




declare function security-plugin:after-create-collection(
	$request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

    if ( $response/status cast as xs:integer = $CONSTANT:STATUS-SUCCESS-CREATED )
    
    then

        let $response-data := $response/body/atom:feed
    
    	(: if security is enabled, install default collection ACL :)
    	let $collection-descriptor-installed := security-plugin:install-collection-descriptor( $request-path-info )
    	
    	(: no filtering necessary because no members yet, but adds acl link :)
    	let $response-data := security-plugin:filter-feed-by-permissions( $request-path-info , $response-data )
    
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




declare function security-plugin:after-update-collection(
	$request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

    let $response-data := $response/body/atom:feed
    
	let $response-data := security-plugin:filter-feed-by-permissions( $request-path-info , $response-data )

	return
	
    	<response>
        {
            $response/status ,
            $response/headers
        }
            <body>{$response-data}</body>
        </response>

};




declare function security-plugin:after-list-collection(
	$request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

    let $response-data := $response/body/atom:feed
    
    let $response-data := security-plugin:filter-feed-by-permissions( $request-path-info , $response-data )

	return
	
    	<response>
        {
            $response/status ,
            $response/headers
        }
            <body>{$response-data}</body>
        </response>

};




declare function security-plugin:after-retrieve-member(
	$request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

    let $response-data := $response/body/atom:entry
    
    let $response-data := security-plugin:augment-entry( $response-data )

	return
	
    	<response>
        {
            $response/status ,
            $response/headers
        }
            <body>{$response-data}</body>
        </response>

};




declare function security-plugin:augment-entry(
    $response-data as element(atom:entry) 
) as element(atom:entry)
{

    (: N.B. cannot use request-path-info to check if update-descriptor allowed, because request-path-info might be a collection URI if the operation was create-member :)
    
    let $entry-uri := $response-data/atom:link[@rel="self"]/@href
    let $entry-path-info := substring-after( $entry-uri , $config:content-service-url )
    let $media-uri := $response-data/atom:link[@rel="edit-media"]/@href
    let $media-path-info := substring-after( $media-uri , $config:content-service-url )

    let $descriptor-link :=     
        <atom:link 
            rel="http://purl.org/atombeat/rel/security-descriptor" 
            href="{concat( $config:security-service-url , $entry-path-info )}" 
            type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}"/>
        
    let $media-descriptor-link :=
        if ( exists( $media-uri ) ) then
            let $media-descriptor-href := concat( $config:security-service-url , $media-path-info )
            return 
                <atom:link 
                    rel="http://purl.org/atombeat/rel/media-security-descriptor" 
                    href="{$media-descriptor-href}" 
                    type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}"/>
        else ()
        
    let $augmented-entry :=
    
        <atom:entry>
        {
            $response-data/attribute::* ,
            $response-data/child::* ,
            $descriptor-link ,
            $media-descriptor-link
        }
        </atom:entry>
    
    return $augmented-entry
    
};





declare function security-plugin:install-member-descriptor(
    $request-path-info as xs:string,
    $resource-path-info as xs:string
) as xs:string?
{
    if ( $security-config:enable-security )
    then 
        let $user := request:get-attribute( $config:user-name-request-attribute-key )
        let $acl := security-config:default-member-security-descriptor( $request-path-info , $user )
        let $acl-db-path := atomsec:store-descriptor( $resource-path-info , $acl )
    	return $acl-db-path
    else ()
};




declare function security-plugin:install-media-descriptor(
    $request-path-info as xs:string,
    $resource-path-info as xs:string
) as xs:string?
{
    if ( $security-config:enable-security )
    then 
        let $user := request:get-attribute( $config:user-name-request-attribute-key )
        let $acl := security-config:default-media-security-descriptor( $request-path-info , $user )
        let $acl-db-path := atomsec:store-descriptor( $resource-path-info , $acl )
    	return $acl-db-path
    else ()
};




declare function security-plugin:install-collection-descriptor( $request-path-info as xs:string ) as xs:string?
{
    if ( $security-config:enable-security )
    then 
        let $user := request:get-attribute( $config:user-name-request-attribute-key )
        let $acl := security-config:default-collection-security-descriptor( $request-path-info , $user )
        return atomsec:store-descriptor( $request-path-info , $acl )
    else ()
};




declare function security-plugin:filter-feed-by-permissions(
    $request-path-info as xs:string ,
    $feed as element(atom:feed)
) as element(atom:feed)
{
    if ( not( $security-config:enable-security ) )
    then $feed
    else
    
        let $descriptor-link :=     
            <atom:link
                rel="http://purl.org/atombeat/rel/security-descriptor" 
                href="{concat( $config:security-service-url , $request-path-info )}" 
                type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}"/>
            
        let $filtered-feed := atomsec:filter-feed( $feed )
        
        let $augmented-feed :=
            <atom:feed>
                {
                    $filtered-feed/attribute::* ,
                    $filtered-feed/child::*[ not( . instance of element(atom:entry) ) ] ,
                    $descriptor-link ,
                    for $entry in $filtered-feed/atom:entry
                    return security-plugin:augment-entry( $entry ) 
                }
            </atom:feed>
            
        return $augmented-feed
        
};







