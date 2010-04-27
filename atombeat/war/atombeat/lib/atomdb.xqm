xquery version "1.0";

module namespace atomdb = "http://atombeat.org/xquery/atomdb";

declare namespace atom = "http://www.w3.org/2005/Atom" ;

import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace xmldb = "http://exist-db.org/xquery/xmldb" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace CONSTANT = "http://atombeat.org/xquery/constants" at "constants.xqm" ;

import module namespace xutil = "http://atombeat.org/xquery/xutil" at "xutil.xqm" ;
import module namespace config = "http://atombeat.org/xquery/config" at "../config/shared.xqm" ;



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
            let $entry-doc := atomdb:retrieve-entry( $request-path-info )
            let $log := util:log( "debug" , $entry-doc )
            let $edit-media-link := $entry-doc/*/atom:link[@rel="edit-media"]
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
	$request-path-info as xs:string ,
	$request-data as element(atom:feed)
) as xs:string
{

	if ( not( atomdb:collection-available( $request-path-info ) ) )
	
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

		let $feed := atomdb:update-feed( doc( $feed-doc-db-path )/* , $request-data )
		
		return xmldb:store( $collection-db-path , $config:feed-doc-name , $feed , $CONSTANT:MEDIA-TYPE-ATOM )
			
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



declare function atomdb:create-member(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry) 
) as xs:string?
{

	if ( not( atomdb:collection-available( $request-path-info ) ) )
	
	then ()
	
	else

		let $log := util:log( "debug" , "atomdb:create-member()" )
		let $log := util:log( "debug" , $request-data )
				
	    let $member-id := atomdb:generate-member-identifier( $request-path-info ) 
	    
	    let $entry := atomdb:create-entry( $request-path-info, $request-data , $member-id )
		let $log := util:log( "debug" , $entry )
	    
		(:
		 : Map the request path info, e.g., "/foo", to a database collection path,
		 : e.g., "/db/foo".
		 :)
		 
        let $entry-doc-db-path := atomdb:store-member( $request-path-info , concat( $member-id , ".atom" ) , $entry )
        
	    return $entry-doc-db-path
		
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




declare function atomdb:update-member(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry) 
) as item()?
{

	if ( not( atomdb:member-available( $request-path-info ) ) )
	
	then ()
	
	else
		
		(:
		 : Map the request path info, e.g., "/foo/bar", to a database path,
		 : e.g., "/db/foo/bar".
		 :)
		 
		let $member-db-path := atomdb:request-path-info-to-db-path( $request-path-info )
	
		let $entry := atomdb:update-entry( doc( $member-db-path )/* , $request-data )

		let $groups := text:groups( $request-path-info , "^(.*)/([^/]+)$" )
		
		let $collection-db-path := atomdb:request-path-info-to-db-path( $groups[2] )
		
		let $entry-resource-name := $groups[3]
		
		let $entry-db-path := xmldb:store( $collection-db-path , $entry-resource-name , $entry , $CONSTANT:MEDIA-TYPE-ATOM )

		(: 
		 : N.B. we return the entry here, rather than just the path, because of
		 : a weird interaction with the versioning module and retrieving a document
		 : that has been updated but not seeing the update within the same
		 : query.
		 :)
		 
		return $entry

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



declare function atomdb:mutable-feed-children(
    $request-data as element(atom:feed)
) as element()*
{
    for $child in $request-data/*
    let $namespace-uri := namespace-uri($child)
    let $local-name := local-name($child)
    where
        not( $namespace-uri = $CONSTANT:ATOM-NSURI and $local-name = $CONSTANT:ATOM-ID ) and
        not( $namespace-uri = $CONSTANT:ATOM-NSURI and $local-name = $CONSTANT:ATOM-UPDATED ) and
        not( $namespace-uri = $CONSTANT:ATOM-NSURI and $local-name = $CONSTANT:ATOM-LINK and $child/@rel = "self" ) and
        not( $namespace-uri = $CONSTANT:ATOM-NSURI and $local-name = $CONSTANT:ATOM-LINK and $child/@rel = "edit" ) and
        not( $namespace-uri = $CONSTANT:ATOM-NSURI and $local-name = $CONSTANT:ATOM-ENTRY )
    return $child
};




declare function atomdb:mutable-entry-children(
    $request-data as element(atom:entry)
) as element()*
{
    for $child in $request-data/*
    let $namespace-uri := namespace-uri($child)
    let $local-name := local-name($child)
    where
        not( $namespace-uri = $CONSTANT:ATOM-NSURI and $local-name = $CONSTANT:ATOM-ID ) and
        not( $namespace-uri = $CONSTANT:ATOM-NSURI and $local-name = $CONSTANT:ATOM-UPDATED ) and
        not( $namespace-uri = $CONSTANT:ATOM-NSURI and $local-name = $CONSTANT:ATOM-PUBLISHED ) and
        not( $namespace-uri = $CONSTANT:ATOM-NSURI and $local-name = $CONSTANT:ATOM-LINK and $child/@rel = "self" ) and
        not( $namespace-uri = $CONSTANT:ATOM-NSURI and $local-name = $CONSTANT:ATOM-LINK and $child/@rel = "edit" ) and
        not( $namespace-uri = $CONSTANT:ATOM-NSURI and $local-name = $CONSTANT:ATOM-LINK and $child/@rel = "edit-media" )
    return $child
};



declare function atomdb:create-feed( 
    $request-path-info as xs:string ,
    $request-data as element(atom:feed)
) as element(atom:feed) 
{

    (: TODO validate input data :)
    
    let $id := concat( $config:service-url , $request-path-info )
    let $updated := current-dateTime()
    let $self-uri := $id
    let $edit-uri := $id
    
    return
    
        <atom:feed>
            <atom:id>{$id}</atom:id>
            <atom:updated>{$updated}</atom:updated>
            <atom:link rel="self" type="application/atom+xml" href="{$self-uri}"/>
            <atom:link rel="edit" type="application/atom+xml" href="{$edit-uri}"/>
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

    let $id := concat( $config:service-url , $request-path-info , "/" , $member-id , ".atom" )
    let $published := current-dateTime()
    let $updated := $published
    let $self-uri := $id
    let $edit-uri := $id
    
    (: TODO review this, maybe provide user as function arg, rather than interrogate request here :)
    let $user-name := request:get-attribute( $config:user-name-request-attribute-key )
    
    return
	
        <atom:entry>
            <atom:id>{$id}</atom:id>
            <atom:published>{$published}</atom:published>
            <atom:updated>{$updated}</atom:updated>
            <atom:link rel="self" type="application/atom+xml" href="{$self-uri}"/>
            <atom:link rel="edit" type="application/atom+xml" href="{$edit-uri}"/>
            {
                atomdb:mutable-entry-children($request-data)
            }
        </atom:entry>  
     
};




declare function atomdb:create-media-link-entry(
	$request-path-info as xs:string ,
    $member-id as xs:string ,
    $media-type as xs:string ,
    $media-link-title as xs:string? ,
    $media-link-summary as xs:string? 
) as element(atom:entry)
{

    let $id := concat( $config:service-url , $request-path-info , "/" , $member-id , ".atom" )
    let $log := util:log( "debug", $id )
    
    let $published := current-dateTime()
    let $updated := $published
    let $self-uri := $id
    let $edit-uri := $id
    let $media-uri := concat( $config:service-url , $request-path-info , "/" , $member-id , ".media" )
    
    let $title :=
    	if ( $media-link-title ) then $media-link-title
    	else concat( "download-" , $member-id , ".media" )
    	
    let $summary :=
    	if ( $media-link-summary ) then $media-link-summary
    	else concat( "media resource (" , $media-type , ")" )
    	    
	return
	
        <atom:entry>
            <atom:id>{$id}</atom:id>
            <atom:published>{$published}</atom:published>
            <atom:updated>{$updated}</atom:updated>
            <atom:link rel="self" type="application/atom+xml" href="{$self-uri}"/>
            <atom:link rel="edit" type="application/atom+xml" href="{$edit-uri}"/>
            <atom:link rel="edit-media" type="{$media-type}" href="{$media-uri}"/>
            <atom:content src="{$media-uri}" type="{$media-type}"/>
            <atom:title type="text">{$title}</atom:title>
            <atom:summary type="text">{$summary}</atom:summary>
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
                atomdb:mutable-entry-children($request-data)
            }
        </atom:entry>  
};



declare function atomdb:create-media-resource(
	$request-path-info as xs:string , 
	$request-data as item()* , 
	$media-type as xs:string ,
	$media-link-title as xs:string? ,
	$media-link-summary as xs:string? 
) as xs:string
{

	let $collection-db-path := atomdb:request-path-info-to-db-path( $request-path-info )

    let $member-id := atomdb:generate-member-identifier( $request-path-info ) 
    
	let $media-resource-name := concat( $member-id , ".media" )

	let $media-resource-db-path := xmldb:store( $collection-db-path , $media-resource-name , $request-data , $media-type )
	
    let $media-link-entry := atomdb:create-media-link-entry( $request-path-info, $member-id , $media-type , $media-link-title , $media-link-summary )
    
    let $media-link-entry-doc-db-path := xmldb:store( $collection-db-path , concat( $member-id , ".atom" ) , $media-link-entry , $CONSTANT:MEDIA-TYPE-ATOM )    
    
    return $media-link-entry-doc-db-path
	 
};




declare function atomdb:update-media-resource(
	$request-path-info as xs:string , 
	$request-data as xs:base64Binary , 
	$request-content-type as xs:string
) as xs:string
{

	if ( not( atomdb:media-resource-available( $request-path-info ) ) )
	
	then ()
	
	else

		let $media-type := text:groups( $request-content-type , "^([^;]+)" )[2]
	
		let $groups := text:groups( $request-path-info , "^(.*)/([^/]+)$" )
		
		let $collection-db-path := atomdb:request-path-info-to-db-path( $groups[2] )
		
		let $media-doc-name := $groups[3]

		let $media-doc-db-path := xmldb:store( $collection-db-path , $media-doc-name , $request-data , $media-type )
	
		return $media-doc-db-path
};




declare function atomdb:retrieve-feed(
	$request-path-info as xs:string 
) as element(atom:feed)
{
	
	if ( not( atomdb:collection-available( $request-path-info ) ) )
	
	then ()
	
	else
	
		(:
		 : Map the request path info, e.g., "/foo", to a database collection path,
		 : e.g., "/db/foo".
		 :)
		
		let $log := util:log( "debug" , $request-path-info )
		let $db-collection-path := atomdb:request-path-info-to-db-path( $request-path-info )
		let $log := util:log( "debug" , $db-collection-path )
	
		(:
		 : Obtain the database path for the atom feed document in the given database
		 : collection. Currently, this appends ".feed" to the database collection
		 : path.
		 :)
		 
		let $feed-doc-db-path := atomdb:feed-doc-db-path( $db-collection-path )
		let $log := util:log( "debug" , $feed-doc-db-path )

        let $feed := doc( $feed-doc-db-path )/*
        let $log := util:log( "debug" , $feed )
		
		let $complete-feed :=
		
			<atom:feed>	
			{
				for $a in $feed/attribute::* return $a ,
				for $c in $feed/child::* return $c ,
				for $child in xmldb:get-child-resources( $db-collection-path )
				let $is-entry-doc := ( not( ends-with( $child, ".media" ) ) and not( ends-with( $child , ".feed" ) ) )
				let $entry := if ( $is-entry-doc ) then doc( concat( $db-collection-path , "/" , $child ) )/* else ()
				order by $entry/atom:updated descending
				return $entry
			}
			</atom:feed>

        let $log := util:log( "debug" , $complete-feed )
        
		return $complete-feed

};




declare function atomdb:retrieve-entry(
	$request-path-info as xs:string 
) as item()?
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
		
		return $entry-doc

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
	return doc( $media-link-doc-db-path )/*

};