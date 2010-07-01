xquery version "1.0";

module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;

import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace xmldb = "http://exist-db.org/xquery/xmldb" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "constants.xqm" ;

import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "xutil.xqm" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;



declare variable $atomdb:logger-name := "org.atombeat.xquery.lib.atomdb" ;



declare function local:debug(
    $message as item()*
) as empty()
{
    util:log-app( "debug" , $atomdb:logger-name , $message )
};




declare function local:info(
    $message as item()*
) as empty()
{
    util:log-app( "info" , $atomdb:logger-name , $message )
};




declare function atomdb:collection-available(
	$request-path-info as xs:string
) as xs:boolean
{

	(:
	 : We need to know two things to determine whether an atom collection is
	 : available (i.e., exists) for the given request path info. First, does
	 : a database collection exist at the corresponding path? Second, does an
	 : atom feed document exist within that collection?
	 :)
	 
	(:
	 : Map the request path info, e.g., "/foo", to a database collection path,
	 : e.g., "/db/foo".
	 :)
	 
	let $db-collection-path := atomdb:request-path-info-to-db-path( $request-path-info )

	(:
	 : Obtain the database path for the atom feed document in the given database
	 : collection. Currently, this appends ".feed" to the database collection
	 : path.
	 :)
	 
	let $feed-doc-db-path := atomdb:feed-doc-db-path( $db-collection-path )
	
	let $available := 
		( xmldb:collection-available( $db-collection-path ) and exists( doc( $feed-doc-db-path ) ) )
	
	return $available
	
};




declare function atomdb:member-available(
	$request-path-info as xs:string
) as xs:boolean
{

	(:
	 : To determine whether an atom collection member is available (i.e., exists) 
	 : for the given request path info, we map the request path info to a resource
	 : path and check the document exists.
	 :)
	 
	(:
	 : Map the request path info, e.g., "/foo/bar", to a database resource path,
	 : e.g., "/db/foo/bar".
	 :)
	 
	let $member-db-path := atomdb:request-path-info-to-db-path( $request-path-info )
		
	return ( not( util:binary-doc-available( $member-db-path ) ) and exists( doc( $member-db-path ) ) )
	
};




declare function atomdb:media-link-available(
    $request-path-info as xs:string
) as xs:boolean
{
    let $log := util:log( "debug" , "== atomdb:media-link-available() ==" )
    
    return 

        if ( not( atomdb:member-available( $request-path-info ) ) ) 
        then 
            let $log := util:log( "debug" , "not a member, returning false" )
            return false()
        
        else
            let $entry := atomdb:retrieve-member( $request-path-info )
            let $log := util:log( "debug" , $entry )
            let $edit-media-link := $entry/atom:link[@rel="edit-media"]
            let $log := util:log( "debug" , $edit-media-link )
            let $available := exists( $edit-media-link )
            let $log := util:log( "debug" , $available )
            return $available
};


declare function atomdb:media-resource-available(
	$request-path-info as xs:string
) as xs:boolean
{

	(:
	 : To determine whether a media resource is available (i.e., exists) 
	 : for the given request path info, we map the request path info to a resource
	 : path and check the document exists.
	 :)
	 
	(:
	 : Map the request path info, e.g., "/foo/bar", to a database resource path,
	 : e.g., "/db/foo/bar".
	 :)
	 
	let $member-db-path := atomdb:request-path-info-to-db-path( $request-path-info )
		
	return util:binary-doc-available( $member-db-path ) 
	
};




declare function atomdb:request-path-info-to-db-path( 
	$request-path-info as xs:string
) as xs:string
{
	concat( $config:base-collection-path , $request-path-info )
};



declare function atomdb:db-path-to-request-path-info(
	$db-path as xs:string
) as xs:string
{
	if ( starts-with( $db-path , $config:base-collection-path ) )
	then substring-after( $db-path , $config:base-collection-path )
	else ()
};



declare function atomdb:edit-path-info( $entry as element(atom:entry) ) as xs:string?
{
    let $uri := $entry/atom:link[@rel='edit']/@href
    return
        if ( starts-with( $uri , $config:content-service-url ) )
        then substring-after( $uri , $config:content-service-url )
        else ()
};



declare function atomdb:edit-media-path-info( $entry as element(atom:entry) ) as xs:string?
{
    let $uri := $entry/atom:link[@rel='edit-media']/@href
    return
        if ( starts-with( $uri , $config:content-service-url ) )
        then substring-after( $uri , $config:content-service-url )
        else ()
};



declare function atomdb:collection-path-info( $entry as element(atom:entry) ) as xs:string?
{
    let $entry-path-info := atomdb:edit-path-info( $entry )
    return
        if ( exists( $entry-path-info ) )
        then text:groups( $entry-path-info , "^(.+)/[^/]+$" )[2]
        else ()
};




declare function atomdb:feed-doc-db-path(
	$db-collection-path as xs:string
) as xs:string
{

	if ( ends-with( $db-collection-path , "/" ) )
	then concat( $db-collection-path , $config:feed-doc-name )
	else concat( $db-collection-path , "/", $config:feed-doc-name )
	
};




declare function atomdb:create-collection(
	$request-path-info as xs:string ,
	$request-data as element(atom:feed) 
) as xs:string?
{

	if ( atomdb:collection-available( $request-path-info ) )
	
	then ()
	
	else
		
		(:
		 : Map the request path info, e.g., "/foo", to a database collection path,
		 : e.g., "/db/foo".
		 :)
		 
		let $collection-db-path := atomdb:request-path-info-to-db-path( $request-path-info )
	
		let $collection-db-path := xutil:get-or-create-collection( $collection-db-path )
		
		(:
		 : Obtain the database path for the atom feed document in the given database
		 : collection. Currently, this appends ".feed" to the database collection
		 : path.
		 :)
		 
		let $feed-doc-db-path := atomdb:feed-doc-db-path( $collection-db-path )

		let $feed := atomdb:create-feed( $request-path-info , $request-data )
		
		let $feed-doc-db-path := xmldb:store( $collection-db-path , $config:feed-doc-name , $feed , $CONSTANT:MEDIA-TYPE-ATOM )
		
		return $feed-doc-db-path
			
};




declare function atomdb:update-collection(
	$collection-path-info as xs:string ,
	$request-data as element(atom:feed)
) as element(atom:feed)?
{

	if ( not( atomdb:collection-available( $collection-path-info ) ) )
	
	then ()
	
	else
		
		(:
		 : Map the request path info, e.g., "/foo", to a database collection path,
		 : e.g., "/db/foo".
		 :)
		 
		let $collection-db-path := atomdb:request-path-info-to-db-path( $collection-path-info )

		let $collection-db-path := xutil:get-or-create-collection( $collection-db-path )

		(:
		 : Obtain the database path for the atom feed document in the given database
		 : collection. Currently, this appends ".feed" to the database collection
		 : path.
		 :)
		 
		let $feed-doc-db-path := atomdb:feed-doc-db-path( $collection-db-path )

		let $feed := atomdb:update-feed( doc( $feed-doc-db-path )/atom:feed , $request-data )
		
		let $store := xmldb:store( $collection-db-path , $config:feed-doc-name , $feed , $CONSTANT:MEDIA-TYPE-ATOM )
		
		return atomdb:retrieve-feed( $collection-path-info )
			
};



declare function atomdb:touch-collection(
    $request-path-info as xs:string
) as xs:dateTime
{

    let $updated := current-dateTime()
    
    let $collection-db-path := atomdb:request-path-info-to-db-path( $request-path-info )
    let $feed-doc-db-path := atomdb:feed-doc-db-path( $collection-db-path )
    let $feed := doc( $feed-doc-db-path )/atom:feed
    
    let $feed-updated :=
        <atom:feed>
        {
            $feed/attribute::* ,
            for $child in $feed/child::*
            return
                if ( $child instance of element(atom:updated) ) 
                then <atom:updated>{$updated}</atom:updated>
                else $child
        }
        </atom:feed>

    let $feed-stored := xmldb:store( $collection-db-path , $config:feed-doc-name , $feed-updated , $CONSTANT:MEDIA-TYPE-ATOM )
    
    return $updated
    
};



declare function atomdb:generate-member-identifier(
    $collection-path-info as xs:string
) as xs:string
{
    let $id := config:generate-identifier( $collection-path-info )
    return
        let $member-path-info := concat( $collection-path-info , "/" , $id , ".atom" )
        return 
            if ( atomdb:member-available( $member-path-info ) )
            then atomdb:generate-member-identifier( $collection-path-info ) (: try again :)
            else $id
};



(:~
 : Create a new Atom collection member.
 :
 : @param $collection-path-info the path info (e.g., "/studies") for the collection where the new member will be created.
 : @param $request-data the Atom entry data to use to create the new member.
 : @return the eXist database path where the new member is stored, or empty if the collection is not available.
 :)
declare function atomdb:create-member(
	$collection-path-info as xs:string ,
	$request-data as element(atom:entry) 
) as element(atom:entry)?
{

    let $member-id := atomdb:generate-member-identifier( $collection-path-info ) 

    return atomdb:create-member( $collection-path-info , $member-id , $request-data )
		
};



(:~
 : Create a new Atom collection member.
 :
 : @param $collection-path-info the path info (e.g., "/studies") for the collection where the new member will be created.
 : @param $member-id the string token to use when generating the member URI
 : @param $request-data the Atom entry data to use to create the new member.
 : @return the eXist database path where the new member is stored, or empty if the collection is not available.
 :)
declare function atomdb:create-member(
	$collection-path-info as xs:string ,
	$member-id as xs:string ,
	$request-data as element(atom:entry) 
) as element(atom:entry)?
{

	if ( not( atomdb:collection-available( $collection-path-info ) ) )
	
	then ()
	
	else

		let $log := util:log( "debug" , "atomdb:create-member()" )
		let $log := util:log( "debug" , $request-data )
				
	    let $entry := atomdb:create-entry( $collection-path-info, $request-data , $member-id )
		let $log := util:log( "debug" , $entry )
	    
		(:
		 : Map the request path info, e.g., "/foo", to a database collection path,
		 : e.g., "/db/foo".
		 :)
		 
        let $entry-doc-db-path := atomdb:store-member( $collection-path-info , concat( $member-id , ".atom" ) , $entry )
        
	    return doc( $entry-doc-db-path )/atom:entry
		
};



declare function atomdb:store-member(
    $collection-path-info as xs:string ,
    $resource-name as xs:string ,
    $entry as element(atom:entry)
) as xs:string?
{

    let $collection-db-path := atomdb:request-path-info-to-db-path( $collection-path-info )

    let $entry-doc-db-path := xmldb:store( $collection-db-path , $resource-name , $entry , $CONSTANT:MEDIA-TYPE-ATOM )    
    
    return $entry-doc-db-path

};



(:~
 : Update an Atom collection member.
 : 
 : @param $member-path-info the path info identifying the member to be updated.
 : @param $request-data the Atom entry data to use to update the member.
 : @return the updated Atom entry, or empty if no member is available at that location.
 :)
declare function atomdb:update-member(
	$member-path-info as xs:string ,
	$request-data as element(atom:entry) 
) as element(atom:entry)?
{

	if ( not( atomdb:member-available( $member-path-info ) ) )
	
	then ()
	
	else
		
		(:
		 : Map the request path info, e.g., "/foo/bar", to a database path,
		 : e.g., "/db/foo/bar".
		 :)
		
		let $current := atomdb:retrieve-member( $member-path-info )
	
		let $new := atomdb:update-entry( $current , $request-data )

		let $groups := text:groups( $member-path-info , "^(.*)/([^/]+)$" )
		
		let $collection-path-info := $groups[2]
		let $entry-resource-name := $groups[3]
		
		let $entry-doc-db-path := atomdb:store-member( $collection-path-info , $entry-resource-name , $new )

		return doc( $entry-doc-db-path )/atom:entry

};




declare function atomdb:delete-member(
    $request-path-info as xs:string
) as empty()
{
	if ( not( atomdb:member-available( $request-path-info ) ) )
	
	then ()
	
	else
	
		let $groups := text:groups( $request-path-info , "^(.*)/([^/]+)$" )
		let $collection-db-path := atomdb:request-path-info-to-db-path( $groups[2] )
		let $entry-resource-name := $groups[3]
        let $entry-removed := xmldb:remove( $collection-db-path , $entry-resource-name )	    

        (: is member a media-link entry? if so, delete also :)
        let $media-resource-name := replace( $entry-resource-name , "^(.*)\.atom$" , "$1.media" )
        let $media-resource-db-path := concat( $collection-db-path , "/" , $media-resource-name )
        let $media-removed := 
            if ( util:binary-doc-available( $media-resource-db-path ) )
            then xmldb:remove( $collection-db-path , $media-resource-name )	   
            else ()
        
        return ()
};



declare function atomdb:delete-media(
    $request-path-info as xs:string
) as empty()
{

    (: 
     : N.B. this function handles a request-path-info identifying a media resource
     : OR identifying a media-link entry. The consequences are the same, both
     : are removed from the database.
     :)
     
    if ( atomdb:media-resource-available( $request-path-info ) )
    
    then
    
		let $groups := text:groups( $request-path-info , "^(.*)/([^/]+)$" )
		let $collection-db-path := atomdb:request-path-info-to-db-path( $groups[2] )
		let $media-resource-name := $groups[3]
		let $media-link-resource-name := replace( $media-resource-name , "^(.*)\.media$" , "$1.atom" )
		let $media-removed := xmldb:remove( $collection-db-path , $media-resource-name )	
		let $media-link-removed := xmldb:remove( $collection-db-path , $media-link-resource-name )	
        return ()

    else if ( atomdb:media-link-available( $request-path-info ) )
    
    then

		let $groups := text:groups( $request-path-info , "^(.*)/([^/]+)$" )
		let $collection-db-path := atomdb:request-path-info-to-db-path( $groups[2] )
		let $media-link-resource-name := $groups[3]
		let $media-resource-name := replace( $media-link-resource-name , "^(.*)\.atom$" , "$1.media" )
		let $media-link-removed := xmldb:remove( $collection-db-path , $media-link-resource-name )	
		let $media-removed := xmldb:remove( $collection-db-path , $media-resource-name )	
        return ()

	else ()
        
};



declare function atomdb:delete-collection(
    $collection-path-info as xs:string ,
    $hard as xs:boolean?
) as empty()
{

    if ( atomdb:collection-available( $collection-path-info ) )
    
    then
    
        let $collection-db-path := atomdb:request-path-info-to-db-path( $collection-path-info )
        let $remove-collection := 
            if ( $hard ) then xmldb:remove( $collection-db-path )
            else xmldb:remove( $collection-db-path , $config:feed-doc-name )
        return ()
    
    else ()

};




declare function atomdb:mutable-feed-children(
    $request-data as element(atom:feed)
) as element()*
{
    for $child in $request-data/*
    where
        not( $child instance of element(atom:id) )
        and not( $child instance of element(atom:updated) )
        and not( $child instance of element(atom:published) )
        and not( $child instance of element(atom:link) and $child/@rel = "self" )
        and not( $child instance of element(atom:link) and $child/@rel = "edit" )
        and not( $child instance of element(atom:entry) )
        and not( $config:auto-author and $child instance of element(atom:author) )
    return $child
};




declare function atomdb:mutable-entry-children(
    $request-path-info as xs:string ,
    $request-data as element(atom:entry)
) as element()*
{
    for $child in $request-data/*
    where
        not( $child instance of element(atom:id) )
        and not( $child instance of element(atom:updated) )
        and not( $child instance of element(atom:published) )
        and not( $child instance of element(atom:link) and $child/@rel = "self" )
        and not( $child instance of element(atom:link) and $child/@rel = "edit" )
        and not( $child instance of element(atom:link) and $child/@rel = "edit-media" )
        and not( $config:auto-author and $child instance of element(atom:author) )
        and not( atomdb:media-link-available( $request-path-info ) and $child instance of element(atom:content) )
    return $child
};



declare function atomdb:create-feed( 
    $request-path-info as xs:string ,
    $request-data as element(atom:feed)
) as element(atom:feed) 
{

    (: TODO validate input data :)
    
    let $id := concat( $config:content-service-url , $request-path-info )
    let $updated := current-dateTime()
    let $self-uri := $id
    let $edit-uri := $id
    
    (: TODO review this, maybe provide user as function arg, rather than interrogate request here :)
    let $user-name := request:get-attribute( $config:user-name-request-attribute-key )
    
    return
    
        <atom:feed>
    	{
    		$request-data/attribute::*
    	}
            <atom:id>{$id}</atom:id>
            <atom:updated>{$updated}</atom:updated>
            <atom:link rel="self" href="{$self-uri}" type="{$CONSTANT:MEDIA-TYPE-ATOM};type=feed"/>
            <atom:link rel="edit" href="{$edit-uri}" type="{$CONSTANT:MEDIA-TYPE-ATOM};type=feed"/>
        {
            if ( $config:auto-author )
            then
                <atom:author>
                {
                    if ( $config:user-name-is-email ) then <atom:email>{$user-name}</atom:email>
                    else <atom:name>{$user-name}</atom:name>
                }                
                </atom:author>
            else ()
        }
        {
            atomdb:mutable-feed-children($request-data)
        }
        </atom:feed>  

};




declare function atomdb:update-feed( 
    $feed as element(atom:feed) ,
    $request-data as element(atom:feed)
) as element(atom:feed) 
{

    (: TODO validate input data :)
    
    let $updated := current-dateTime()
    let $log := util:log( "debug" , concat( "$updated: " , $updated ) )

    return
    
        <atom:feed>
            {
                $feed/atom:id
            }
            <atom:updated>{$updated}</atom:updated>
            {
                $feed/atom:link[@rel="self"] ,
                $feed/atom:link[@rel="edit"] ,
                if ( $config:auto-author ) then $feed/atom:author else () ,
                atomdb:mutable-feed-children($request-data)
            }
        </atom:feed>  
};




declare function atomdb:create-entry(
	$request-path-info as xs:string ,
    $request-data as element(atom:entry) ,
    $member-id as xs:string 
) as element(atom:entry)
{

    let $id := concat( $config:content-service-url , $request-path-info , "/" , $member-id , ".atom" )
    let $published := current-dateTime()
    let $updated := $published
    let $self-uri := $id
    let $edit-uri := $id
    let $path-info := substring-after( $id , $config:content-service-url )
    
    (: TODO review this, maybe provide user as function arg, rather than interrogate request here :)
    let $user-name := request:get-attribute( $config:user-name-request-attribute-key )
    
    return
	
        <atom:entry>
            <atom:id>{$id}</atom:id>
            <atom:published>{$published}</atom:published>
            <atom:updated>{$updated}</atom:updated>
            <atom:link rel="self" type="application/atom+xml;type=entry" href="{$self-uri}"/>
            <atom:link rel="edit" type="application/atom+xml;type=entry" href="{$edit-uri}"/>
        {
            if ( $config:auto-author )
            then
                <atom:author>
                {
                    if ( $config:user-name-is-email ) then <atom:email>{$user-name}</atom:email>
                    else <atom:name>{$user-name}</atom:name>
                }                
                </atom:author>
            else ()
        }
        {
            atomdb:mutable-entry-children( $path-info , $request-data )
        }
        </atom:entry>  
     
};




declare function atomdb:create-media-link-entry(
	$request-path-info as xs:string ,
    $member-id as xs:string ,
    $media-type as xs:string ,
    $media-link-title as xs:string? ,
    $media-link-summary as xs:string? ,
    $media-link-category as xs:string?
) as element(atom:entry)
{

    let $id := concat( $config:content-service-url , $request-path-info , "/" , $member-id , ".atom" )
    let $log := util:log( "debug", $id )
    
    let $published := current-dateTime()
    let $updated := $published
    let $self-uri := $id
    let $edit-uri := $id
    let $media-uri := concat( $config:content-service-url , $request-path-info , "/" , $member-id , ".media" )
    
    (: TODO review this, maybe provide user as function arg, rather than interrogate request here :)
    let $user-name := request:get-attribute( $config:user-name-request-attribute-key )

    let $title :=
    	if ( $media-link-title ) then $media-link-title
    	else concat( "download-" , $member-id , ".media" )
    	
    let $summary :=
    	if ( $media-link-summary ) then $media-link-summary
    	else "media resource"
    	
    let $category :=
        if ( exists( $media-link-category ) )
        then 
            let $scheme := text:groups( $media-link-category , 'scheme="([^"]+)"' )[2]
            let $term := text:groups( $media-link-category , 'term="([^"]+)"' )[2]
            let $label := text:groups( $media-link-category , 'label="([^"]+)"' )[2]
            return <atom:category scheme="{$scheme}" term="{$term}" label="{$label}"/>
        else ()        
    	
	let $media-size :=
		xmldb:size( atomdb:request-path-info-to-db-path( $request-path-info ) , concat( $member-id , ".media" ) )
    	    
	return
	
        <atom:entry>
            <atom:id>{$id}</atom:id>
            <atom:published>{$published}</atom:published>
            <atom:updated>{$updated}</atom:updated>
            <atom:link rel="self" type="application/atom+xml;type=entry" href="{$self-uri}"/>
            <atom:link rel="edit" type="application/atom+xml;type=entry" href="{$edit-uri}"/>
            <atom:link rel="edit-media" type="{$media-type}" href="{$media-uri}" length="{$media-size}"/>
            <atom:content src="{$media-uri}" type="{$media-type}"/>
            <atom:title type="text">{$title}</atom:title>
            <atom:summary type="text">{$summary}</atom:summary>
        {
            $category , 
            if ( $config:auto-author )
            then
                <atom:author>
                {
                    if ( $config:user-name-is-email ) then <atom:email>{$user-name}</atom:email>
                    else <atom:name>{$user-name}</atom:name>
                }                
                </atom:author>
            else ()
        }
        </atom:entry>  
     
};




declare function atomdb:update-entry( 
    $entry as element(atom:entry) ,
    $request-data as element(atom:entry)
) as element(atom:entry) 
{

    (: TODO validate input data :)
    
    let $updated := current-dateTime()
    let $log := util:log( "debug" , concat( "$updated: " , $updated ) )
    let $path-info := substring-after( $entry/atom:link[@rel='edit']/@href , $config:content-service-url )

    return
    
        <atom:entry>
            {
                $entry/atom:id ,
                $entry/atom:published 
            }
            <atom:updated>{$updated}</atom:updated>
            {
                $entry/atom:link[@rel="self"] ,
                $entry/atom:link[@rel="edit"] ,
                $entry/atom:link[@rel="edit-media"] ,
                if ( $config:auto-author ) then $entry/atom:author else () ,
                if ( atomdb:media-link-available( $path-info ) ) then $entry/atom:content else () ,
                atomdb:mutable-entry-children( $path-info , $request-data )
            }
        </atom:entry>  
};



declare function atomdb:create-media-resource(
	$request-path-info as xs:string , 
	$request-data as item()* , 
	$media-type as xs:string 
) as element(atom:entry)?
{
    atomdb:create-media-resource( $request-path-info , $request-data , $media-type , () , () , () )
};




declare function atomdb:create-media-resource(
	$request-path-info as xs:string , 
	$request-data as item()* , 
	$media-type as xs:string ,
	$media-link-title as xs:string? ,
	$media-link-summary as xs:string? ,
	$media-link-category as xs:string?
) as element(atom:entry)?
{

	let $collection-db-path := atomdb:request-path-info-to-db-path( $request-path-info )

    let $member-id := atomdb:generate-member-identifier( $request-path-info ) 
    
	let $media-resource-name := concat( $member-id , ".media" )

	let $media-resource-db-path := xmldb:store( $collection-db-path , $media-resource-name , $request-data , $media-type )
	
    let $media-link-entry := atomdb:create-media-link-entry( $request-path-info, $member-id , $media-type , $media-link-title , $media-link-summary , $media-link-category )
    
    let $media-link-entry-doc-db-path := xmldb:store( $collection-db-path , concat( $member-id , ".atom" ) , $media-link-entry , $CONSTANT:MEDIA-TYPE-ATOM )    
    
    return doc( $media-link-entry-doc-db-path )/atom:entry
	 
};




declare function atomdb:update-media-resource(
	$request-path-info as xs:string , 
	$request-data as xs:base64Binary , 
	$request-content-type as xs:string
) as element(atom:entry)?
{

	if ( not( atomdb:media-resource-available( $request-path-info ) ) )
	
	then ()
	
	else

		let $media-type := text:groups( $request-content-type , "^([^;]+)" )[2]
	
		let $groups := text:groups( $request-path-info , "^(.*)/([^/]+)\.media$" )
		
		let $collection-db-path := atomdb:request-path-info-to-db-path( $groups[2] )
		let $id := $groups[3]
		let $media-doc-name := concat( $id , ".media" )
		let $media-link-doc-name := concat( $id , ".atom" )

		let $media-doc-db-path := xmldb:store( $collection-db-path , $media-doc-name , $request-data , $media-type )
		
		let $media-link-doc-db-path := concat( $collection-db-path , "/" , $media-link-doc-name )
		
		let $media-size := xmldb:size( $collection-db-path , $media-doc-name )
			
		let $log := local:debug( concat( "size: " , $media-size ) )

		let $media-link := 
			<atom:entry>
			{
				for $child in doc($media-link-doc-db-path)/atom:entry/*
				return
				    if ( $child instance of element(atom:updated) )
				    then <atom:updated>{current-dateTime()}</atom:updated>
				    else if ( $child instance of element(atom:link) and $child/@rel='edit-media' )
					then <atom:link rel='edit-media' type='{$media-type}' href='{$child/@href}' length='{$media-size}'/>
				    else if ( $child instance of element(atom:content) )
					then <atom:content type='{$media-type}' src='{$child/@src}'/>
					else $child
				
			}
			</atom:entry>
			
		let $log := local:debug( $media-link )
			
		let $media-link-updated := xmldb:store( $collection-db-path , $media-link-doc-name , $media-link )
	
		return doc( $media-link-doc-db-path )/atom:entry
};




declare function atomdb:retrieve-feed(
	$collection-path-info as xs:string 
) as element(atom:feed)?
{
	
	if ( not( atomdb:collection-available( $collection-path-info ) ) )
	
	then ()
	
	else
	
        let $feed := atomdb:retrieve-feed-without-entries( $collection-path-info )
		
		let $complete-feed :=
		
			<atom:feed>	
			{
				$feed/attribute::* ,
				$feed/child::* ,
				let $recursive := xs:boolean( $feed/@atombeat:recursive )
				let $entries := atomdb:retrieve-members( $collection-path-info , $recursive )
				return
				    for $entry in $entries
    				order by $entry/atom:updated descending
    				return 
    					if ( exists( $entry ) and xs:boolean( $feed/@atombeat:exclude-entry-content ) )
    					then atomdb:exclude-entry-content( $entry )
    					else $entry
			}
			</atom:feed>

        let $log := util:log( "debug" , $complete-feed )
        
		return $complete-feed

};




declare function atomdb:retrieve-feed-without-entries(
	$collection-path-info as xs:string 
) as element(atom:feed)?
{
	
	if ( not( atomdb:collection-available( $collection-path-info ) ) )
	
	then ()
	
	else
	
		(:
		 : Map the request path info, e.g., "/foo", to a database collection path,
		 : e.g., "/db/atom/content/foo".
		 :)
		
		let $db-collection-path := atomdb:request-path-info-to-db-path( $collection-path-info )
	
		(:
		 : Obtain the database path for the atom feed document in the given database
		 : collection. Currently, this appends ".feed" to the database collection
		 : path.
		 :)
		 
		let $feed-doc-db-path := atomdb:feed-doc-db-path( $db-collection-path )

        let $feed := doc( $feed-doc-db-path )/atom:feed
		
		return $feed

};




declare function atomdb:retrieve-members(
    $collection-path-info as xs:string ,
    $recursive as xs:boolean?
) as element(atom:entry)*
{
    
    let $db-collection-path := atomdb:request-path-info-to-db-path( $collection-path-info )
    return
        if ( $recursive )
        then collection( $db-collection-path )/atom:entry (: recursive :)
        else xmldb:xcollection( $db-collection-path )/atom:entry (: not recursive :)

};



declare function atomdb:exclude-entry-content(
    $entry as element(atom:entry)
) as element(atom:entry)
{
    <atom:entry>
    {
        $entry/attribute::* ,
        for $ec in $entry/child::* 
        return 
            if ( $ec instance of element(atom:content) )
            then <atom:content>{$ec/attribute::*}</atom:content>
            else $ec
    }   
    </atom:entry>
};



declare function atomdb:retrieve-member(
	$request-path-info as xs:string 
) as element(atom:entry)?
{

	if ( not( atomdb:member-available( $request-path-info ) ) )
	
	then ()
	
	else
	
		(:
		 : Map the request path info, e.g., "/foo", to a database collection path,
		 : e.g., "/db/foo".
		 :)
		 
		let $entry-doc-db-path := atomdb:request-path-info-to-db-path( $request-path-info )
		let $log := local:debug( concat( "entry-doc-db-path: " , $entry-doc-db-path ) )
		
		let $entry-doc := doc( $entry-doc-db-path )
		let $log := local:debug( $entry-doc )
		
		return $entry-doc/atom:entry

};




declare function atomdb:retrieve-media(
	$request-path-info as xs:string 
) as item()*
{

	if ( not( atomdb:media-resource-available( $request-path-info ) ) )
	
	then ()
	
	else
	
		(:
		 : Map the request path info, e.g., "/foo", to a database collection path,
		 : e.g., "/db/foo".
		 :)
		 
		let $media-doc-db-path := atomdb:request-path-info-to-db-path( $request-path-info )
		
		return util:binary-doc( $media-doc-db-path )

};




declare function atomdb:get-mime-type(
	$request-path-info as xs:string 
) as xs:string
{

	let $media-doc-db-path := atomdb:request-path-info-to-db-path( $request-path-info )

	return xmldb:get-mime-type( xs:anyURI( $media-doc-db-path ) )
	
};



declare function atomdb:get-media-link(
	$request-path-info as xs:string
) as element(atom:entry)
{

	(: assume path info identifies a media resource :)
	
	let $media-doc-db-path := atomdb:request-path-info-to-db-path( $request-path-info )
	let $media-link-doc-db-path := replace( $media-doc-db-path , "^(.*)\.media$" , "$1.atom" )
	return doc( $media-link-doc-db-path )/atom:entry

};



declare function atomdb:generate-etag(
    $request-path-info as xs:string
) as xs:string
{
    
    (: TODO consider alternative means of generating etag, e.g., hash
     : of timestamp and file size? :)
     
    if ( atomdb:member-available( $request-path-info ) )
    then
        let $entry := atomdb:retrieve-member( $request-path-info )
        return util:hash( $entry , "md5" )
        
    else ()
};




declare function atomdb:filter(
    $new as element() ,
    $reserved as element(reserved)
) as element() 
{

(:
    <reserved>
        <attributes namespace-uri="">
            <attribute>baz</attribute>
        </attributes>
        <attributes namespace-uri="http://example.org/foo">
            <attribute>bar</attribute>
        </attributes>
        <elements namespace-uri="">
            <element>baz</element>
        </elements>
        <elements namespace-uri="http://example.org/foo">
            <element>spong</element>
        </elements>
        <atom-elements>
            <element>id</element>
        </atom-elements>
        <atom-links>
            <link rel="http://example.org/rel/see-also"/>
        </atom-links>
    </reserved>
:)

    (: filter out reserved attributes :)
    
    let $attributes :=
        for $a in $new/attribute::*
        where empty( $reserved/attributes/attribute[../@namespace-uri=namespace-uri($a) and text()=local-name($a)] )
        return $a

    (: filter out reserved elements :)
    
    let $children1 :=
        for $c in $new/child::*
        where empty( $reserved/elements/element[../@namespace-uri=namespace-uri($c) and text()=local-name($c)] )
        return $c
        
    (: filter out reserved atom elements :)    
        
    let $children2 :=
        for $c in $children1
        where namespace-uri($c) != $CONSTANT:ATOM-NSURI
        or empty( $reserved/atom-elements/element[text()=local-name($c)] )
        return $c
        
    (: filter out reserved atom links :)
    
    let $children3 :=
        for $c in $children2
        where not( $c instance of element(atom:link) )
        or empty( $reserved/atom-links/link[ ( empty(@rel) or @rel=$c/@rel ) and ( empty(@type) or @type=$c/@type ) ] )
        return $c
        
    (: construct filtered data :)    
        
    return 

        element { node-name( $new ) }
        {
            $attributes ,
            $children3
        }

};




declare function atomdb:filter(
    $old as element() ,
    $new as element() ,
    $reserved as element(reserved)
) as element() 
{

    let $reserved-attributes := 
        for $a in $old/attribute::*
        where exists( $reserved/attributes/attribute[../@namespace-uri=namespace-uri($a) and text()=local-name($a)] )
        return $a

    let $reserved-elements :=
        for $c in $old/child::*
        where exists( $reserved/elements/element[../@namespace-uri=namespace-uri($c) and text()=local-name($c)] )
        return $c
        
    let $reserved-atom-elements :=
        for $c in $old/child::*
        where namespace-uri($c) = $CONSTANT:ATOM-NSURI
        and exists( $reserved/atom-elements/element[text()=local-name($c)] )
        return $c
        
    let $reserved-atom-links :=
        for $c in $old/child::*
        where ( $c instance of element(atom:link) )
        and exists( $reserved/atom-links/link[ ( empty(@rel) or @rel=$c/@rel ) and ( empty(@type) or @type=$c/@type ) ] )
        return $c
        
    let $reserved-children := ( $reserved-elements , $reserved-atom-elements , $reserved-atom-links )

    let $new-filtered := atomdb:filter( $new , $reserved )
            
    return 
    
        element { node-name( $new ) }
        {
            $reserved-attributes ,
            $new-filtered/attribute::* ,
            $reserved-children ,
            $new-filtered/child::*
        }
    
};