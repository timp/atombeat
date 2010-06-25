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

import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace mime = "http://purl.org/atombeat/xquery/mime" at "../lib/mime.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace atomsec = "http://purl.org/atombeat/xquery/atom-security" at "../lib/atom-security.xqm" ;




declare function local:debug(
    $message as item()*
) as empty()
{
    util:log-app( "debug" , "org.atombeat.xquery.plugin.security" , $message )
};





declare function security-plugin:before(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string?
) as item()*
{
	
	if ( $config:enable-security )
	
	then

    	let $message := ( "security plugin, before: " , $operation , ", request-path-info: " , $request-path-info ) 
    	let $log := local:debug( $message )
    
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
    let $log := local:debug( "== security-plugin:strip-descriptor-links ==" )

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

    if ( $config:enable-security )

    then
            
    	let $message := ( "security plugin, after: " , $operation , ", request-path-info: " , $request-path-info ) 
    	let $log := local:debug( $message )
    
    	return
    		
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
	let $log := local:debug( concat( "$entry-uri: " , $entry-uri ) )
	
	let $entry-path-info := substring-after( $entry-uri , $config:content-service-url )
	let $log := local:debug( concat( "$entry-path-info: " , $entry-path-info ) )

	(: if security is enabled, install default resource ACL :)
	let $resource-descriptor-installed := security-plugin:install-resource-descriptor( $request-path-info , $entry-path-info )
	let $log := local:debug( concat( "$resource-descriptor-installed: " , $resource-descriptor-installed ) )
	
    let $response-data := security-plugin:augment-entry( $request-path-info , $response-data )

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
            let $resource-descriptor-installed := security-plugin:install-resource-descriptor( $request-path-info , $entry-path-info )
            let $media-uri := $entry/atom:link[@rel="edit-media"]/@href
            let $media-path-info := substring-after( $media-uri , $config:content-service-url )
            (: if security is enabled, install default resource ACL :)
            let $resource-descriptor-installed := 
                if ( exists( $media-path-info ) and $media-path-info != "" ) then security-plugin:install-resource-descriptor( $request-path-info , $media-path-info )
                else () 
            return security-plugin:augment-entry( $entry-path-info , $entry )
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
    let $response-data := security-plugin:augment-entry( $request-path-info , $response-data )

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
    let $log := local:debug( concat( "$entry-uri: " , $entry-uri ) )
    
    let $entry-path-info := substring-after( $entry-uri , $config:content-service-url )
    let $log := local:debug( concat( "$entry-path-info: " , $entry-path-info ) )

    (: if security is enabled, install default resource ACL :)
    let $resource-descriptor-installed := security-plugin:install-resource-descriptor( $request-path-info , $entry-path-info )
    let $log := local:debug( concat( "$resource-descriptor-installed: " , $resource-descriptor-installed ) )

    let $media-uri := $response-data/atom:link[@rel="edit-media"]/@href
    let $log := local:debug( concat( "$media-uri: " , $media-uri ) )
    
    let $media-path-info := substring-after( $media-uri , $config:content-service-url )
    let $log := local:debug( concat( "$media-path-info: " , $media-path-info ) )

    (: if security is enabled, install default resource ACL :)
    let $resource-descriptor-installed := security-plugin:install-resource-descriptor( $request-path-info , $media-path-info )
    let $log := local:debug( concat( "$resource-descriptor-installed: " , $resource-descriptor-installed ) )
    
    let $response-data := security-plugin:augment-entry( $entry-path-info , $response-data )

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

    let $response-data := security-plugin:augment-entry( $request-path-info , $response-data )

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

	let $log := local:debug("== security-plugin:after-retrieve-member ==" )

    let $response-data := $response/body/atom:entry
    
    let $response-data := security-plugin:augment-entry( $request-path-info , $response-data )

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
    $request-path-info as xs:string ,
    $response-data as element(atom:entry) 
) as element(atom:entry)
{
    security-plugin:augment-entry( $request-path-info , $response-data , false() )
};



declare function security-plugin:augment-entry(
    $request-path-info as xs:string ,
    $response-data as element(atom:entry) ,
    $expand-descriptors as xs:boolean?
) as element(atom:entry)
{

    (: N.B. cannot use request-path-info to check if update-descriptor allowed, because request-path-info might be a collection URI if the operation was create-member :)
    
    let $entry-uri := $response-data/atom:link[@rel="self"]/@href
    let $entry-path-info := substring-after( $entry-uri , $config:content-service-url )
    let $media-uri := $response-data/atom:link[@rel="edit-media"]/@href
    let $media-path-info := substring-after( $media-uri , $config:content-service-url )

    let $can-retrieve-member-descriptor := atomsec:is-allowed(  $CONSTANT:OP-RETRIEVE-ACL , $entry-path-info , ()  )
    let $can-update-member-descriptor := atomsec:is-allowed( $CONSTANT:OP-UPDATE-ACL , $entry-path-info , () )
    
    let $can-retrieve-media-descriptor := atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-ACL , $media-path-info , () )
    let $can-update-media-descriptor := atomsec:is-allowed( $CONSTANT:OP-UPDATE-ACL , $media-path-info , () )
    
    let $allow := string-join((
        if ( $can-retrieve-member-descriptor ) then "GET" else () ,
        if ( $can-update-member-descriptor ) then "PUT" else ()
    ) , ", " )
    
    let $descriptor-link :=     
        if ( $can-update-member-descriptor or $can-retrieve-member-descriptor )
        then 
            <atom:link atombeat:allow="{$allow}" rel="http://purl.org/atombeat/rel/security-descriptor" href="{concat( $config:security-service-url , $entry-path-info )}" type="application/atom+xml;type=entry">
            {
                if ( $can-retrieve-member-descriptor and $expand-descriptors )
                then 
                    <ae:inline>
                    { atomsec:wrap-with-entry( $entry-path-info , atomsec:retrieve-descriptor( $entry-path-info ) ) }
                    </ae:inline>
                else ()                
            }
            </atom:link>
        else ()
        
    let $log := local:debug( concat( "$descriptor-link: " , $descriptor-link ) )
    
    let $media-descriptor-link :=
        if ( exists( $media-uri ) )
        then
            let $allow := string-join((
                if ( $can-retrieve-media-descriptor ) then "GET" else () ,
                if ( $can-update-media-descriptor ) then "PUT" else ()
            ) , ", " )
            return 
                if ( $can-update-media-descriptor or $can-retrieve-media-descriptor )
                then 
                    let $media-descriptor-href := concat( $config:security-service-url , $media-path-info )
                    return 
                        <atom:link atombeat:allow="{$allow}" rel="http://purl.org/atombeat/rel/media-security-descriptor" href="{$media-descriptor-href}" type="application/atom+xml;type=entry">
                        {
                            if ( $can-retrieve-media-descriptor and $expand-descriptors )
                            then 
                                <ae:inline>
                                { atomsec:wrap-with-entry( $media-path-info , atomsec:retrieve-descriptor( $media-path-info ) ) }
                                </ae:inline>
                            else ()
                        }
                        </atom:link>
                else ()                
        else ()
        
    let $augmented-entry :=
    
        <atom:entry>
        {
            $response-data/attribute::* ,
            $response-data/child::*[not(. instance of element(atom:link))] ,
            
            (: decorate other links with atombeat:allow :)
            security-plugin:decorate-links( $response-data/atom:link ) ,
            
            $descriptor-link ,
            $media-descriptor-link
                        
        }
        </atom:entry>
    
    return $augmented-entry
    
};




declare function security-plugin:decorate-links(
    $links as element(atom:link)*
) as element(atom:link)*
{

    for $link in $links
    return
        if ( not( starts-with( $link/@href , $config:content-service-url ) ) )
        then $link
        else 
            let $path-info := substring-after( $link/@href , $config:content-service-url )
            return 
                if ( atomdb:member-available( $path-info ) )
                then
                    let $can-get := atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-MEMBER , $path-info , () )
                    let $can-put := atomsec:is-allowed( $CONSTANT:OP-UPDATE-MEMBER , $path-info , () )
                    let $can-delete := atomsec:is-allowed( $CONSTANT:OP-DELETE-MEMBER , $path-info , () )
                    let $allow := string-join( (
                        if ( $can-get ) then "GET" else () ,
                        if ( $can-put ) then "PUT" else () ,
                        if ( $can-delete ) then "DELETE" else ()
                    ) , ", " )
                    return <atom:link atombeat:allow="{$allow}">{$link/attribute::* , $link/child::*}</atom:link>
                else if ( atomdb:media-resource-available( $path-info ) )
                then 
                    let $can-get := atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-MEDIA , $path-info , () )
                    let $can-put := atomsec:is-allowed( $CONSTANT:OP-UPDATE-MEDIA , $path-info , () )
                    let $can-delete := atomsec:is-allowed( $CONSTANT:OP-DELETE-MEDIA , $path-info , () )
                    let $allow := string-join( (
                        if ( $can-get ) then "GET" else () ,
                        if ( $can-put ) then "PUT" else () ,
                        if ( $can-delete ) then "DELETE" else ()
                    ) , ", " )
                    return <atom:link atombeat:allow="{$allow}">{$link/attribute::* , $link/child::*}</atom:link>
                else if ( atomdb:collection-available( $path-info ) )
                then 
                    let $can-get := atomsec:is-allowed( $CONSTANT:OP-LIST-COLLECTION , $path-info , () )
                    let $can-put := atomsec:is-allowed( $CONSTANT:OP-UPDATE-COLLECTION , $path-info , () )
                    let $can-post := (
                        atomsec:is-allowed( $CONSTANT:OP-CREATE-MEMBER , $path-info , () )
                        or atomsec:is-allowed( $CONSTANT:OP-CREATE-MEDIA , $path-info , () )
                        or atomsec:is-allowed( $CONSTANT:OP-MULTI-CREATE , $path-info , () )
                    )
                    let $allow := string-join( (
                        if ( $can-get ) then "GET" else () ,
                        if ( $can-put ) then "PUT" else () ,
                        if ( $can-post ) then "POST" else ()
                    ) , ", " )
                    return <atom:link atombeat:allow="{$allow}">{$link/attribute::* , $link/child::*}</atom:link>
                else $link

};




declare function security-plugin:install-resource-descriptor(
    $request-path-info as xs:string,
    $resource-path-info as xs:string
) as xs:string?
{
    if ( $config:enable-security )
    then 
        let $user := request:get-attribute( $config:user-name-request-attribute-key )
        let $acl := config:default-resource-security-descriptor( $request-path-info , $user )
        let $acl-db-path := atomsec:store-descriptor( $resource-path-info , $acl )
    	return $acl-db-path
    else ()
};




declare function security-plugin:install-collection-descriptor( $request-path-info as xs:string ) as xs:string?
{
    if ( $config:enable-security )
    then 
        let $user := request:get-attribute( $config:user-name-request-attribute-key )
        let $acl := config:default-collection-security-descriptor( $request-path-info , $user )
        return atomsec:store-descriptor( $request-path-info , $acl )
    else ()
};




declare function security-plugin:filter-feed-by-permissions(
    $request-path-info as xs:string ,
    $feed as element(atom:feed)
) as element(atom:feed)
{
    if ( not( $config:enable-security ) )
    then $feed
    else
        let $expand-descriptors := xs:boolean( $feed/@atombeat:expand-security-descriptors )

        let $can-retrieve-collection-descriptor := atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-ACL , $request-path-info , () )
        let $can-update-collection-descriptor := atomsec:is-allowed( $CONSTANT:OP-UPDATE-ACL , $request-path-info , () )
        let $allow := string-join((
            if ( $can-retrieve-collection-descriptor ) then "GET" else () ,
            if ( $can-update-collection-descriptor ) then "PUT" else ()
        ) , ", " )
        let $descriptor-link :=     
            if ( $can-retrieve-collection-descriptor or $can-update-collection-descriptor )
            then 
                <atom:link atombeat:allow="{$allow}" rel="http://purl.org/atombeat/rel/security-descriptor" href="{concat( $config:security-service-url , $request-path-info )}" type="application/atom+xml;type=entry">
                {
                    if ( $can-retrieve-collection-descriptor and $expand-descriptors )
                    then 
                        <ae:inline>
                        { atomsec:wrap-with-entry( $request-path-info , atomsec:retrieve-descriptor( $request-path-info ) ) }
                        </ae:inline>
                    else ()                
                }
                </atom:link>
            else ()
        let $filtered-feed :=
            <atom:feed>
                {
                    $feed/attribute::* ,
                    $feed/child::*[ not( . instance of element(atom:entry) ) and not( . instance of element(atom:link) ) ] ,
                    for $entry in $feed/atom:entry
                    let $entry-path-info := substring-after( $entry/atom:link[@rel="edit"]/@href , $config:content-service-url )
                    let $log := local:debug( concat( "checking permission to retrieve member for entry-path-info: " , $entry-path-info ) )
                    let $forbidden := atomsec:is-denied( $CONSTANT:OP-RETRIEVE-MEMBER , $entry-path-info , () )
                    return 
                        if ( not( $forbidden ) ) 
                        then security-plugin:augment-entry( $entry-path-info , $entry , $expand-descriptors ) 
                        else ()
                    ,
                    (: decorate other links with atombeat:allow :)
                    security-plugin:decorate-links( $feed/atom:link ) ,
                    $descriptor-link
                }
            </atom:feed>
        let $log := local:debug( $filtered-feed )
        return $filtered-feed
};







