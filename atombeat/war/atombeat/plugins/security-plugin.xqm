xquery version "1.0";

module namespace sp = "http://atombeat.org/xquery/security-plugin";

declare namespace atom = "http://www.w3.org/2005/Atom" ;


import module namespace request = "http://exist-db.org/xquery/request" ;
import module namespace response = "http://exist-db.org/xquery/response" ;
import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace CONSTANT = "http://atombeat.org/xquery/constants" at "../lib/constants.xqm" ;

import module namespace config = "http://atombeat.org/xquery/config" at "../config/shared.xqm" ;

import module namespace xutil = "http://atombeat.org/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace mime = "http://atombeat.org/xquery/mime" at "../lib/mime.xqm" ;
import module namespace atomdb = "http://atombeat.org/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace atomsec = "http://atombeat.org/xquery/atom-security" at "../lib/atom-security.xqm" ;




declare function local:debug(
    $message as item()*
) as empty()
{
    util:log-app( "debug" , "org.atombeat.xquery.plugin.security" , $message )
};





declare function sp:before(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string?
) as item()*
{
	
	let $message := ( "security plugin, before: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := local:debug( $message )

	let $forbidden := sp:is-operation-forbidden( $operation , $request-path-info , $request-media-type )
	
	return 
	
		if ( $forbidden )
		
		then 
		
			let $status-code := $CONSTANT:STATUS-CLIENT-ERROR-FORBIDDEN (: override request processing :)
		    let $response-data := "The server understood the request, but is refusing to fulfill it. Authorization will not help and the request SHOULD NOT be repeated."
			let $response-content-type := "text/plain"
			
			return ( $status-code , $response-data , $response-content-type )
			
        else if ( 
            $operation = $CONSTANT:OP-CREATE-MEMBER 
            or $operation = $CONSTANT:OP-UPDATE-MEMBER 
            or $operation = $CONSTANT:OP-CREATE-COLLECTION 
            or $operation = $CONSTANT:OP-UPDATE-COLLECTION
        )
        
        then 
        
            let $request-data := sp:strip-acl-links( $request-data )
			let $status-code := 0 (: we don't want to interrupt request processing :)
			return ( $status-code , $request-data )
			
		else
		
			let $status-code := 0 (: we don't want to interrupt request processing :)
			return ( $status-code , $request-data )

};




declare function sp:strip-acl-links(
    $request-data as element()
) as element()
{
    let $log := local:debug( "== sp:strip-acl-links ==" )
    let $log := local:debug( $request-data )
    let $request-data :=
        element { node-name( $request-data ) }
        {
            $request-data/attribute::* ,
            for $child in $request-data/child::*
            let $ln := local-name( $child )
            let $ns := namespace-uri( $child )
            let $rel := $child/@rel
            where (
                not(
                    $ln = $CONSTANT:ATOM-LINK
                    and $ns = $CONSTANT:ATOM-NSURI 
                    and ( $rel = "edit-acl" or $rel = "edit-media-acl" )
                )
            )
            return $child
        }
    let $log := local:debug( $request-data )
    return $request-data
};




declare function sp:after(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$response-data as item()* ,
	$response-content-type as xs:string?
) as item()*
{

	let $message := ( "security plugin, after: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := local:debug( $message )

	return
		
		if ( $operation = $CONSTANT:OP-CREATE-MEMBER )
		
		then sp:after-create-member( $request-path-info , $response-data , $response-content-type )

		else if ( $operation = $CONSTANT:OP-CREATE-MEDIA )
		
		then sp:after-create-media( $request-path-info , $response-data , $response-content-type )

		else if ( $operation = $CONSTANT:OP-CREATE-COLLECTION )
		
		then sp:after-create-collection( $request-path-info , $response-data , $response-content-type )

		else if ( $operation = $CONSTANT:OP-UPDATE-COLLECTION )
		
		then sp:after-update-collection( $request-path-info , $response-data , $response-content-type )

		else if ( $operation = $CONSTANT:OP-LIST-COLLECTION )
		
		then sp:after-list-collection( $request-path-info , $response-data , $response-content-type )
		
		else if ( $operation = $CONSTANT:OP-RETRIEVE-MEMBER )
		
		then sp:after-retrieve-member( $request-path-info , $response-data , $response-content-type )

		else if ( $operation = $CONSTANT:OP-UPDATE-MEMBER )
		
		then sp:after-update-member( $request-path-info , $response-data , $response-content-type )

		else 

			(: pass response data and content type through, we don't want to modify response :)
			( $response-data , $response-content-type )
}; 




declare function sp:after-create-member(
	$request-path-info as xs:string ,
	$response-data as item()* ,
	$response-content-type as xs:string?
) as item()*
{

	let $entry-uri := $response-data/atom:link[@rel="edit"]/@href
	let $log := local:debug( concat( "$entry-uri: " , $entry-uri ) )
	
	let $entry-path-info := substring-after( $entry-uri , $config:service-url )
	let $log := local:debug( concat( "$entry-path-info: " , $entry-path-info ) )

	let $entry-doc-db-path := atomdb:request-path-info-to-db-path( $entry-path-info )
	let $log := local:debug( concat( "$entry-doc-db-path: " , $entry-doc-db-path ) )
	
	(: if security is enabled, install default resource ACL :)
	let $resource-acl-installed := sp:install-resource-acl( $request-path-info , $entry-doc-db-path )
	let $log := local:debug( concat( "$resource-acl-installed: " , $resource-acl-installed ) )
	
    let $response-data := sp:append-edit-acl-links( $request-path-info , $response-data )

	return ( $response-data , $response-content-type )

};



declare function sp:after-update-member(
	$request-path-info as xs:string ,
	$response-data as item()* ,
	$response-content-type as xs:string?
) as item()*
{

    let $response-data := sp:append-edit-acl-links( $request-path-info , $response-data )

	return ( $response-data , $response-content-type )

};




declare function sp:after-create-media(
	$request-path-info as xs:string ,
	$response-data as item()* ,
	$response-content-type as xs:string?
) as item()*
{

	let $entry-uri := $response-data/atom:link[@rel="edit"]/@href
	let $log := local:debug( concat( "$entry-uri: " , $entry-uri ) )
	
	let $entry-path-info := substring-after( $entry-uri , $config:service-url )
	let $log := local:debug( concat( "$entry-path-info: " , $entry-path-info ) )

	let $entry-doc-db-path := atomdb:request-path-info-to-db-path( $entry-path-info )
	let $log := local:debug( concat( "$entry-doc-db-path: " , $entry-doc-db-path ) )
	
	(: if security is enabled, install default resource ACL :)
	let $resource-acl-installed := sp:install-resource-acl( $request-path-info , $entry-doc-db-path )
	let $log := local:debug( concat( "$resource-acl-installed: " , $resource-acl-installed ) )

	let $media-uri := $response-data/atom:link[@rel="edit-media"]/@href
	let $log := local:debug( concat( "$media-uri: " , $media-uri ) )
	
	let $media-path-info := substring-after( $media-uri , $config:service-url )
	let $log := local:debug( concat( "$media-path-info: " , $media-path-info ) )

	let $media-resource-db-path := atomdb:request-path-info-to-db-path( $media-path-info )
	let $log := local:debug( concat( "$media-resource-db-path: " , $media-resource-db-path ) )
	
	(: if security is enabled, install default resource ACL :)
	let $resource-acl-installed := sp:install-resource-acl( $request-path-info , $media-resource-db-path )
	let $log := local:debug( concat( "$resource-acl-installed: " , $resource-acl-installed ) )
	
	(: need to workaround html response for create media with multipart request :)
    let $response-data := 
        if ( starts-with( $response-content-type , $CONSTANT:MEDIA-TYPE-ATOM ) )
        then sp:append-edit-acl-links( $entry-path-info , $response-data )
        else $response-data

	return ( $response-data , $response-content-type )

};




declare function sp:after-create-collection(
	$request-path-info as xs:string ,
	$response-data as item()* ,
	$response-content-type as xs:string?
) as item()*
{

	(: if security is enabled, install default collection ACL :)
	let $collection-acl-installed := sp:install-collection-acl( $request-path-info )
	
	(: no filtering necessary because no members yet, but adds edit-acl link :)
	let $response-data := sp:filter-feed-by-acls( $request-path-info , $response-data )

	return ( $response-data , $response-content-type )

};




declare function sp:after-update-collection(
	$request-path-info as xs:string ,
	$response-data as item()* ,
	$response-content-type as xs:string?
) as item()*
{

	let $response-data := sp:filter-feed-by-acls( $request-path-info , $response-data )

	return ( $response-data , $response-content-type )

};




declare function sp:after-list-collection(
	$request-path-info as xs:string ,
	$response-data as item()* ,
	$response-content-type as xs:string?
) as item()*
{

	let $response-data := sp:filter-feed-by-acls( $request-path-info , $response-data )

	return ( $response-data , $response-content-type )

};




declare function sp:after-retrieve-member(
	$request-path-info as xs:string ,
	$response-data as item()* ,
	$response-content-type as xs:string?
) as item()*
{

	let $log := local:debug("== sp:after-retrieve-member ==" )

    let $response-data := sp:append-edit-acl-links( $request-path-info , $response-data )

	return ( $response-data , $response-content-type )

};




declare function sp:append-edit-acl-links(
    $request-path-info as xs:string ,
    $response-data as element(atom:entry)
) as element(atom:entry)
{

    (: N.B. cannot use request-path-info to check if update-acl allowed, because request-path-info might be a collection URI if the operation was create-member :)
    
    let $entry-path-info := substring-after( $response-data/atom:link[@rel="self"]/@href , $config:service-url )
    let $can-update-member-acl := not( sp:is-operation-forbidden( $CONSTANT:OP-UPDATE-ACL , $entry-path-info , () ) )
    
    let $edit-acl-link :=     
        if ( $can-update-member-acl )
        then <atom:link rel="edit-acl" href="{concat( $config:acl-service-url , $entry-path-info )}" type="application/atom+xml"/>
        else ()
        
    let $log := local:debug( concat( "$edit-acl-link: " , $edit-acl-link ) )
    
    let $edit-media-acl-link :=
        if ( atomdb:media-link-available( $request-path-info ) )
        then
            let $media-path-info := substring-after( $response-data/atom:link[@rel="edit-media"]/@href , $config:service-url )
            let $can-update-media-acl := not( sp:is-operation-forbidden( $CONSTANT:OP-UPDATE-ACL , $media-path-info , () ) )
            return 
                if ( $can-update-media-acl )
                then 
                    let $edit-media-acl-href := concat( $config:acl-service-url , $media-path-info )
                    return <atom:link rel="edit-media-acl" href="{$edit-media-acl-href}" type="application/atom+xml"/>
                else ()                
        else ()
        
    let $response-data := 
        if ( empty( $edit-acl-link ) and empty( $edit-media-acl-link ) ) then $response-data
        else
            <atom:entry>
            {
                $response-data/attribute::* ,
                $response-data/child::* ,
                $edit-acl-link ,
                $edit-media-acl-link
            }
            </atom:entry>
            
    return $response-data
    
};




declare function sp:install-resource-acl(
    $request-path-info as xs:string,
    $entry-doc-db-path as xs:string
) as xs:string?
{
    if ( $config:enable-security )
    then 
        let $user := request:get-attribute( $config:user-name-request-attribute-key )
        let $acl := config:default-resource-acl( $request-path-info , $user )
        let $entry-path-info := atomdb:db-path-to-request-path-info( $entry-doc-db-path )
        let $acl-db-path := atomsec:store-resource-acl( $entry-path-info , $acl )
    	return $acl-db-path
    else ()
};




declare function sp:install-collection-acl( $request-path-info as xs:string ) as xs:string?
{
    if ( $config:enable-security )
    then 
        let $user := request:get-attribute( $config:user-name-request-attribute-key )
        let $acl := config:default-collection-acl( $request-path-info , $user )
        return atomsec:store-collection-acl( $request-path-info , $acl )
    else ()
};




declare function sp:filter-feed-by-acls(
    $request-path-info as xs:string ,
    $feed as element(atom:feed)
) as element(atom:feed)
{
    if ( not( $config:enable-security ) )
    then $feed
    else
        let $can-update-collection-acl := not( sp:is-operation-forbidden( $CONSTANT:OP-UPDATE-ACL , $request-path-info , () ) )
        let $edit-acl-link :=     
            if ( $can-update-collection-acl )
            then <atom:link rel="edit-acl" href="{concat( $config:acl-service-url , $request-path-info )}" type="application/atom+xml"/>
            else ()
        let $filtered-feed :=
            <atom:feed>
                {
                    $feed/attribute::* ,
                    $feed/child::*[ not( local-name(.) = $CONSTANT:ATOM-ENTRY and namespace-uri(.) = $CONSTANT:ATOM-NSURI ) ] ,
                    $edit-acl-link ,
                    for $entry in $feed/atom:entry
                    let $entry-path-info := substring-after( $entry/atom:link[@rel="edit"]/@href , $config:service-url )
                    let $log := local:debug( concat( "checking permission to retrieve member for entry-path-info: " , $entry-path-info ) )
                    let $forbidden := sp:is-operation-forbidden( $CONSTANT:OP-RETRIEVE-MEMBER , $entry-path-info , () )
                    return 
                        if ( not( $forbidden ) ) 
                        then 
                            let $can-update-acl := not( sp:is-operation-forbidden( $CONSTANT:OP-UPDATE-ACL , $entry-path-info , () ) )
                            return
                                if ( $can-update-acl ) then sp:append-edit-acl-links( $entry-path-info , $entry ) 
                                else $entry
                        else ()
                }
            </atom:feed>
        let $log := local:debug( $filtered-feed )
        return $filtered-feed
};







declare function sp:is-operation-forbidden(
    $operation as xs:string ,
    $request-path-info as xs:string ,
    $request-media-type as xs:string?
) as xs:boolean
{

    let $user := request:get-attribute( $config:user-name-request-attribute-key )
    let $roles := request:get-attribute( $config:user-roles-request-attribute-key )
    
    let $forbidden := 
        if ( not( $config:enable-security ) ) then false()
        else ( atomsec:decide( $user , $roles , $request-path-info, $operation , $request-media-type ) = $atomsec:decision-deny )
        
    return $forbidden 
    
};






