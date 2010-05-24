xquery version "1.0";

module namespace atom-protocol = "http://purl.org/atombeat/xquery/atom-protocol";
 
declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;

import module namespace request = "http://exist-db.org/xquery/request" ;
import module namespace response = "http://exist-db.org/xquery/response" ;
import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace util = "http://exist-db.org/xquery/util" ;
 
import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "constants.xqm" ;
import module namespace mime = "http://purl.org/atombeat/xquery/mime" at "mime.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "atomdb.xqm" ;
 
import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace plugin = "http://purl.org/atombeat/xquery/plugin" at "../config/plugins.xqm" ;

declare variable $atom-protocol:param-request-path-info := "request-path-info" ;
declare variable $atom-protocol:logger-name := "org.atombeat.xquery.lib.atom-protocol" ;



declare function local:debug(
    $message as item()*
) as empty()
{
    util:log-app( "debug" , $atom-protocol:logger-name , $message )
};




declare function local:info(
    $message as item()*
) as empty()
{
    util:log-app( "info" , $atom-protocol:logger-name , $message )
};





(:
 : TODO doc me  
 :)
declare function atom-protocol:do-service()
as item()*
{

	let $request-path-info := request:get-attribute( $atom-protocol:param-request-path-info )
	let $request-method := request:get-method()
	
	return
	
		if ( $request-method = $CONSTANT:METHOD-POST )

		then atom-protocol:do-post( $request-path-info )
		
		else if ( $request-method = $CONSTANT:METHOD-PUT )
		
		then atom-protocol:do-put( $request-path-info )
		
		else if ( $request-method = $CONSTANT:METHOD-GET )
		
		then atom-protocol:do-get( $request-path-info )
		
		else if ( $request-method = $CONSTANT:METHOD-DELETE )
		
		then atom-protocol:do-delete( $request-path-info )
		
		else atom-protocol:do-method-not-allowed( $request-path-info )

};




(:
 : TODO doc me
 :)
declare function atom-protocol:do-post(
	$request-path-info as xs:string 
) as item()*
{

	let $request-content-type := request:get-header( $CONSTANT:HEADER-CONTENT-TYPE )

	return 

		if ( starts-with( $request-content-type, $CONSTANT:MEDIA-TYPE-ATOM ) )
		
		then atom-protocol:do-post-atom( $request-path-info )
		
		else if ( starts-with( $request-content-type, $CONSTANT:MEDIA-TYPE-MULTIPART-FORM-DATA ) )
		
		then atom-protocol:do-post-multipart( $request-path-info )
		
		else atom-protocol:do-post-media( $request-path-info , $request-content-type )

};




(:
 : TODO doc me
 :)
declare function atom-protocol:do-post-atom(
	$request-path-info as xs:string 
) as item()*
{

	let $request-data := request:get-data()

	return
	
		if (
			local-name( $request-data ) = $CONSTANT:ATOM-FEED and 
			namespace-uri( $request-data ) = $CONSTANT:ATOM-NSURI
		)
		
		then atom-protocol:do-post-atom-feed( $request-path-info , $request-data )

		else if (
			local-name( $request-data ) = $CONSTANT:ATOM-ENTRY and 
			namespace-uri( $request-data ) = $CONSTANT:ATOM-NSURI
		)
		
		then atom-protocol:do-post-atom-entry( $request-path-info , $request-data )
		
		else atom-protocol:do-bad-request( $request-path-info , "Request entity must be either atom feed or atom entry." )

};




declare function atom-protocol:do-post-atom-feed(
	$request-path-info as xs:string ,
	$request-data as element(atom:feed)
) as item()*
{

    (: 
     : Here we bottom out at the "create-collection" operation.
     :)
     
	(: 
	 : We need to know whether an atom collection already exists at the 
	 : request path, in which case the request will be treated as an error,
	 : or whether no atom collection exists at the request path, in which case
	 : the request will create a new atom collection and initialise the atom
	 : feed document with the given feed metadata.
	 :)
	 
	let $create := not( atomdb:collection-available( $request-path-info ) )
	
	return 
	
		if ( $create ) 

		then 
			
			let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-create-collection" ) , 3 )
			return atom-protocol:apply-op( $CONSTANT:OP-CREATE-COLLECTION , $op , $request-path-info , $request-data )
		
		else atom-protocol:do-bad-request( $request-path-info , "A collection already exists at the given location." )
        	
};




(:
 : TODO doc me 
 :)
declare function atom-protocol:op-create-collection(
	$request-path-info as xs:string ,
	$request-data as element(atom:feed) ,
	$request-media-type as xs:string?
) as item()*
{

	let $feed-doc-db-path := atomdb:create-collection( $request-path-info , $request-data )

	let $feed := doc( $feed-doc-db-path )/atom:feed
            
	let $header-content-type := response:set-header( $CONSTANT:HEADER-CONTENT-TYPE , $CONSTANT:MEDIA-TYPE-ATOM )
	
    let $location := $feed/atom:link[@rel="self"]/@href
        	
	let $header-location := response:set-header( $CONSTANT:HEADER-LOCATION, $location )

	return ( $CONSTANT:STATUS-SUCCESS-CREATED , $feed , $CONSTANT:MEDIA-TYPE-ATOM )

};





(:
 : TODO doc me
 :)
declare function atom-protocol:do-post-atom-entry(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry)
) as item()*
{

	(: 
	 : First we need to know whether an atom collection exists at the 
	 : request path.
	 :)
	 
	let $collection-available := atomdb:collection-available( $request-path-info )
	
	return 
	
		if ( not( $collection-available ) ) 

		then atom-protocol:do-not-found( $request-path-info )
		
		else
		
            (: 
             : Here we bottom out at the "create-member" operation.
             :)
             
			let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-create-member" ) , 3 )
			
            return atom-protocol:apply-op( $CONSTANT:OP-CREATE-MEMBER , $op , $request-path-info , $request-data )
        
};




(:
 : TODO doc me
 :)
declare function atom-protocol:op-create-member(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry) ,
	$request-media-type as xs:string?
) as item()*
{

	let $log := local:debug( "op-create-member" )
	let $log := local:debug( $request-data )
	
	let $entry-doc-db-path := atomdb:create-member( $request-path-info , $request-data )

	let $entry-doc := doc( $entry-doc-db-path )
            
    let $location := $entry-doc/atom:entry/atom:link[@rel="self"]/@href
        	
	let $header-location := response:set-header( $CONSTANT:HEADER-LOCATION, $location )
    let $header-content-location := response:set-header( $CONSTANT:HEADER-CONTENT-LOCATION , $location )
	
	let $entry-path-info := atomdb:db-path-to-request-path-info( $entry-doc-db-path )

    let $etag := concat( '"' , atomdb:generate-etag( $entry-path-info ) , '"' )
    
    let $etag-header-set := 
        if ( exists( $etag ) ) then response:set-header( "ETag" , $etag ) else ()
        
    let $feed-date-updated := atomdb:touch-collection( $request-path-info )
    	
	return ( $CONSTANT:STATUS-SUCCESS-CREATED , $entry-doc/atom:entry , $CONSTANT:MEDIA-TYPE-ATOM )
		
};




(:
 : TODO doc me
 :)
declare function atom-protocol:do-post-media(
	$request-path-info as xs:string ,
	$request-content-type
) as item()*
{

	(: 
	 : First we need to know whether an atom collection exists at the 
	 : request path.
	 :)
	 
	let $collection-available := atomdb:collection-available( $request-path-info )
	
	return 
	
		if ( not( $collection-available ) ) 

		then atom-protocol:do-not-found( $request-path-info )
		
		else
		
            (: 
             : Here we bottom out at the "create-media" operation.
             :)
             
        	let $media-type := text:groups( $request-content-type , "^([^;]+)" )[2]
        	let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-create-media" ) , 3 )
	
            return atom-protocol:apply-op( $CONSTANT:OP-CREATE-MEDIA , $op , $request-path-info , request:get-data() , $media-type )
                        			
};




(:
 : TODO doc me
 :)
declare function atom-protocol:op-create-media(
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string
) as item()*
{

	(: check for slug to use as title :)
	
	let $slug := request:get-header( $CONSTANT:HEADER-SLUG )
	
	(: check for summary :) 
	
	let $summary := request:get-header( "X-Atom-Summary" )
	
	let $media-link-doc-db-path := atomdb:create-media-resource( $request-path-info , $request-data , $request-media-type , $slug , $summary )
	
	let $media-link-doc := doc( $media-link-doc-db-path )
            
    let $location := $media-link-doc/atom:entry/atom:link[@rel="self"]/@href
        	
	let $header-location := response:set-header( $CONSTANT:HEADER-LOCATION, $location )
    let $header-content-location := response:set-header( $CONSTANT:HEADER-CONTENT-LOCATION , $location )

    let $feed-date-updated := atomdb:touch-collection( $request-path-info )
        
	return ( $CONSTANT:STATUS-SUCCESS-CREATED , $media-link-doc/atom:entry , $CONSTANT:MEDIA-TYPE-ATOM )

};




(:
 : TODO doc me
 :)
declare function atom-protocol:do-post-multipart(
	$request-path-info as xs:string 
) as item()*
{

	(: 
	 : First we need to know whether an atom collection exists at the 
	 : request path.
	 :)
	 
	let $collection-available := atomdb:collection-available( $request-path-info )
	
	return 
	
		if ( not( $collection-available ) ) 

		then atom-protocol:do-not-found( $request-path-info )
		
		else

			(: check for file name to use as title :)
			
			let $file-name := request:get-uploaded-file-name( "media" )
			
			(:
			 : Unfortunately eXist's function library doesn't give us any way
			 : to retrieve the content type for the uploaded file, so we'll
			 : work around by using a mapping from file name extensions to
			 : mime types.
			 :)
			 
			let $extension := text:groups( $file-name , "\.([^.]+)$" )[2]
			 
			let $media-type := $mime:mappings//mime-mapping[extension=$extension]/mime-type
			
			let $media-type := if ( empty( $media-type ) ) then "application/octet-stream" else $media-type
			
			let $request-data := request:get-uploaded-file-data( "media" )
			
            (: 
             : Here we bottom out at the "create-media" operation.
             :)
             
            let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-create-media-from-multipart-form-data" ) , 3 ) 
            return atom-protocol:apply-op( $CONSTANT:OP-CREATE-MEDIA , $op , $request-path-info , $request-data , $media-type )

};





(: 
 : TODO doc me 
 :)
declare function atom-protocol:op-create-media-from-multipart-form-data (
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string
) as item()*
{

    (: TODO bad request if expected form parts are missing :)
    
	(: check for file name to use as title :)
	
	let $file-name := request:get-uploaded-file-name( "media" )

	(: check for summary param :)
	
	let $summary := request:get-parameter( "summary" , "" )

	let $media-link-doc-db-path := atomdb:create-media-resource( $request-path-info , $request-data , $request-media-type , $file-name , $summary )
	
	let $media-link-doc := doc( $media-link-doc-db-path )
            
    let $location := $media-link-doc/atom:entry/atom:link[@rel="self"]/@href
        	
	let $header-location := response:set-header( $CONSTANT:HEADER-LOCATION, $location )

    let $feed-date-updated := atomdb:touch-collection( $request-path-info )
        
	let $accept := request:get-header( $CONSTANT:HEADER-ACCEPT )
	
	let $response-data :=
	
		(: 
		 : Do very naive processing of accept header. If header is set
		 : and is exactly "application/atom+xml" then return the media-
		 : link entry, otherwise fall back to text/html output to 
		 : support browser applications programmatically manipulating
		 : HTML forms.
		 :)
		 
		if ( $accept = "application/atom+xml" )
		
		then $media-link-doc/atom:entry
	
		else 
		
			<html>
				<head>
					<title>{$CONSTANT:STATUS-SUCCESS-OK}</title>
				</head>
				<body>{ comment { util:serialize( $media-link-doc/atom:entry , () ) } }</body>
			</html>
				
	let $response-content-type :=

		if ( $accept = "application/atom+xml" )
		
		then $CONSTANT:MEDIA-TYPE-ATOM 
	
		else "text/html"

    let $header-content-location := 
        if ( $accept = "application/atom+xml" )
        then response:set-header( $CONSTANT:HEADER-CONTENT-LOCATION , $media-link-doc/atom:entry/atom:link[@rel='self']/@href )
        else ()

    (:
     : Although the semantics of 201 Created would be more appropriate 
     : to the operation performed, we'll respond with 200 OK because the
     : request is most likely to originate from an HTML form submission
     : and I don't know how all browsers handle 201 responses.
     :)
     
	return ( $CONSTANT:STATUS-SUCCESS-OK , $response-data , $response-content-type )

};





(:
 : TODO doc me
 :)
declare function atom-protocol:do-put (
	$request-path-info as xs:string 
) as item()*
{

	let $request-content-type := request:get-header( $CONSTANT:HEADER-CONTENT-TYPE )

	return 

		if ( starts-with( $request-content-type, $CONSTANT:MEDIA-TYPE-ATOM ) )
		
		then atom-protocol:do-put-atom( $request-path-info )

		else atom-protocol:do-put-media( $request-path-info , $request-content-type )

};




declare function atom-protocol:do-put-atom(
	$request-path-info as xs:string 
) as item()*
{

	let $request-data := request:get-data()

	return
	
		if (
			local-name( $request-data ) = $CONSTANT:ATOM-FEED and 
			namespace-uri( $request-data ) = $CONSTANT:ATOM-NSURI
		)
		
		then atom-protocol:do-put-atom-feed( $request-path-info , $request-data )

		else if (
			local-name( $request-data ) = $CONSTANT:ATOM-ENTRY and 
			namespace-uri( $request-data ) = $CONSTANT:ATOM-NSURI
		)
		
		then atom-protocol:do-put-atom-entry( $request-path-info , $request-data )
		
		else atom-protocol:do-bad-request( $request-path-info , "Request entity must be either atom feed or atom entry." )

};




(:
 : TODO doc me 
 :)
declare function atom-protocol:do-put-atom-feed(
	$request-path-info as xs:string ,
	$request-data as element(atom:feed)
) as item()*
{

	(: TODO what if $request-path-info points to a member or media resource? :)
	
	(: 
	 : We need to know whether an atom collection already exists at the 
	 : request path, in which case the request will update the feed metadata,
	 : or whether no atom collection exists at the request path, in which case
	 : the request will create a new atom collection and initialise the atom
	 : feed document with the given feed metadata.
	 :)
	 
	let $create := not( atomdb:collection-available( $request-path-info ) )

	return
	
		if ( $create )
		then atom-protocol:do-put-atom-feed-to-create-collection( $request-path-info , $request-data )
		else atom-protocol:do-put-atom-feed-to-update-collection( $request-path-info , $request-data )	

};




(: 
 : TODO doc me
 :)
declare function atom-protocol:do-put-atom-feed-to-create-collection(
	$request-path-info as xs:string ,
	$request-data as element(atom:feed)
) as item()*
{

    (: 
     : Here we bottom out at the "create-collection" operation.
     :)
     
	let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-create-collection" ) , 3 )
	
    return atom-protocol:apply-op( $CONSTANT:OP-CREATE-COLLECTION , $op , $request-path-info , $request-data )
        		
};




(:
 : TODO doc me
 :)
declare function atom-protocol:do-put-atom-feed-to-update-collection(
	$request-path-info as xs:string ,
	$request-data as element(atom:feed)
) as item()*
{

    (: 
     : Here we bottom out at the "update-collection" operation, so we need to 
     : apply a security decision.
     :)

	let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-update-collection" ) , 3 )
	
    return atom-protocol:apply-op( $CONSTANT:OP-UPDATE-COLLECTION , $op , $request-path-info , $request-data )

};




(:
 : TODO doc me 
 :)
declare function atom-protocol:op-update-collection(
	$request-path-info as xs:string ,
	$request-data as element(atom:feed) ,
	$request-media-type as xs:string?
) as item()*
{

	let $feed-doc-db-path := atomdb:update-collection( $request-path-info , $request-data )
		
	let $feed := doc( $feed-doc-db-path )/atom:feed
            
	return ( $CONSTANT:STATUS-SUCCESS-OK , $feed , $CONSTANT:MEDIA-TYPE-ATOM )

};





(:
 : TODO doc me
 :)
declare function atom-protocol:do-put-atom-entry(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry)
) as item()*
{

	(:
	 : Check for bad request.
	 :)
	 
 	 if ( atomdb:collection-available( $request-path-info ) )
 	 then atom-protocol:do-bad-request( $request-path-info , "You cannot PUT and atom:entry to a collection URI." )
 	 
 	 else if ( atomdb:media-resource-available( $request-path-info ) )
 	 then atom-protocol:do-unsupported-media-type( $request-path-info )
 	 
 	 else
 	  
		(: 
		 : First we need to know whether an atom entry exists at the 
		 : request path.
		 :)
		 
		let $member-available := atomdb:member-available( $request-path-info )
		
		return 
		
			if ( not( $member-available ) ) 
	
			then atom-protocol:do-not-found( $request-path-info )
			
			else
			
			    let $header-if-match := request:get-header( "If-Match" )
			    
			    return 
			    
			         if ( exists( $header-if-match ) )
			         
			         then atom-protocol:do-conditional-put-atom-entry( $request-path-info , $request-data )
			         
			         else
        			     
        			    (: 
        			     : Here we bottom out at the "update-member" operation.
        			     :)
        	            let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-update-member" ) , 3 )
        	            return atom-protocol:apply-op( $CONSTANT:OP-UPDATE-MEMBER , $op , $request-path-info , $request-data ) 
        
};




(:
 : TODO doc me
 :)
declare function atom-protocol:do-conditional-put-atom-entry(
    $request-path-info as xs:string ,
    $request-data as element(atom:entry)
) as item()*
{

    let $header-if-match := request:get-header( "If-Match" )
    
    let $match-etags := tokenize( $header-if-match , "\s*,\s*" )
    
    let $etag := atomdb:generate-etag( $request-path-info ) 
    
    let $matches :=
        for $match-etag in $match-etags
        where (
            $match-etag = "*"
            or ( 
                starts-with( $match-etag , '"' ) 
                and ends-with( $match-etag , '"' )
                and $etag = substring( $match-etag , 2 , string-length( $match-etag ) - 2 )
            )  
        )
        return $match-etag
        
    return
    
        if ( exists( $matches ) )
        then

            (: 
             : Here we bottom out at the "update-member" operation.
             :)
            let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-update-member" ) , 3 )
            return atom-protocol:apply-op( $CONSTANT:OP-UPDATE-MEMBER , $op , $request-path-info , $request-data ) 
        
        else atom-protocol:do-precondition-failed( $request-path-info , "The entity tag does not match." )
        
};


(:
 : TODO doc me
 :)
declare function atom-protocol:op-update-member(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry) ,
	$request-media-type as xs:string?
) as item()*
{
    
	let $entry := atomdb:update-member( $request-path-info , $request-data )

    let $etag := concat( '"' , atomdb:generate-etag( $request-path-info ) , '"' )
    
    let $etag-header-set := 
        if ( exists( $etag ) ) then response:set-header( "ETag" , $etag ) else ()

    let $collection-path-info := text:groups( $request-path-info , "^(.*)/[^/]+$" )[2]
    
    let $feed-date-updated := atomdb:touch-collection( $collection-path-info )
    
	return ( $CONSTANT:STATUS-SUCCESS-OK , $entry , $CONSTANT:MEDIA-TYPE-ATOM )

};




(: 
 : TODO doc me
 :)
declare function atom-protocol:do-put-media(
	$request-path-info as xs:string ,
	$request-content-type
) as item()*
{

	
 	 if ( atomdb:collection-available( $request-path-info ) )
 	 then atom-protocol:do-unsupported-media-type( $request-path-info )
 	 
 	 else if ( atomdb:member-available( $request-path-info ) )
 	 then atom-protocol:do-unsupported-media-type( $request-path-info )
 	 
 	 else

		(: 
		 : First we need to know whether a media resource exists at the 
		 : request path.
		 :)
		 
		let $found := atomdb:media-resource-available( $request-path-info )
		
		return 
		
			if ( not( $found ) ) 
	
			then atom-protocol:do-not-found( $request-path-info )
			
			else
			
				(: here we bottom out at the "update-media" operation :)
				
				let $request-data := request:get-data()
				
				let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-update-media" ) , 3 )
				
				return atom-protocol:apply-op( $CONSTANT:OP-UPDATE-MEDIA , $op , $request-path-info , $request-data , $request-content-type )
				
};




declare function atom-protocol:op-update-media(
	$request-path-info as xs:string ,
	$request-data as item()? ,
	$request-content-type as xs:string?
) as item()*
{
	
	let $media-doc-db-path := atomdb:update-media-resource( $request-path-info , $request-data , $request-content-type )
	
    let $collection-path-info := text:groups( $request-path-info , "^(.*)/[^/]+$" )[2]
    
    let $feed-date-updated := atomdb:touch-collection( $collection-path-info )
    
    (: return the media-link entry :)
    
    let $media-link-entry := atomdb:get-media-link( $request-path-info )
    
    let $content-location-header-set := response:set-header( $CONSTANT:HEADER-CONTENT-LOCATION , $media-link-entry/atom:link[@rel='edit']/@href )

    return ( $CONSTANT:STATUS-SUCCESS-OK , $media-link-entry , $CONSTANT:MEDIA-TYPE-ATOM )

(:
    
    let $status-code := response:set-status-code( $CONSTANT:STATUS-SUCCESS-OK )
    
    let $mime-type := atomdb:get-mime-type( $request-path-info )
    
    let $media-link := atomdb:get-media-link( $request-path-info )
    let $title := $media-link/atom:title
    let $content-disposition :=
    	if ( $title ) then response:set-header( $CONSTANT:HEADER-CONTENT-DISPOSITION , concat( 'attachment; filename="' , $title , '"' ) )
    	else ()
    
    let $response-stream := response:stream-binary( atomdb:retrieve-media( $request-path-info ) , $mime-type )
	
	return ( () , () , () )

:)

};





(: 
 : TODO doc me 
 :)
declare function atom-protocol:do-get(
	$request-path-info as xs:string 
) as item()*
{

	if ( atomdb:media-resource-available( $request-path-info ) )
	
	then atom-protocol:do-get-media( $request-path-info )
	
	else if ( atomdb:member-available( $request-path-info ) )
	
	then atom-protocol:do-get-entry( $request-path-info )
	
	else if ( atomdb:collection-available( $request-path-info ) )
	
	then atom-protocol:do-get-feed( $request-path-info )

	else atom-protocol:do-not-found( $request-path-info )
	
};




(:
 : TODO doc me
 :)
declare function atom-protocol:do-get-entry(
	$request-path-info
) as item()*
{
    
    let $header-if-none-match := request:get-header( "If-None-Match" )
    
    return 
    
        if ( exists( $header-if-none-match ) )
        
        then atom-protocol:do-conditional-get-entry( $request-path-info )
        
        else

            (: 
             : Here we bottom out at the "retrieve-member" operation.
             :)
        
        	let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-retrieve-member" ) , 3 )
        	
            return atom-protocol:apply-op( $CONSTANT:OP-RETRIEVE-MEMBER , $op , $request-path-info , () )

};



(:
 : TODO doc me
 :)
declare function atom-protocol:do-conditional-get-entry(
    $request-path-info
) as item()*
{
    
    (: TODO is this a security risk? i.e., could someone probe for changes to a 
     : resource even if they don't have permission to retrieve it? If so, should
     : the conditional processing be pushed into the main operation? :)
     
    let $header-if-none-match := request:get-header( "If-None-Match" )
    
    let $match-etags := tokenize( $header-if-none-match , "\s*,\s*" )
    
    let $etag := atomdb:generate-etag( $request-path-info ) 
    
    let $matches :=
        for $match-etag in $match-etags
        where (
            $match-etag = "*"
            or ( 
                starts-with( $match-etag , '"' ) 
                and ends-with( $match-etag , '"' )
                and $etag = substring( $match-etag , 2 , string-length( $match-etag ) - 2 )
            )  
        )
        return $match-etag
        
    return
    
        if ( exists( $matches ) )
        
        then atom-protocol:do-not-modified( $request-path-info )
        
        else
        
            (: 
             : Here we bottom out at the "retrieve-member" operation.
             :)
        
            let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-retrieve-member" ) , 3 )
            
            return atom-protocol:apply-op( $CONSTANT:OP-RETRIEVE-MEMBER , $op , $request-path-info , () )

};



(:
 : TODO doc me
 :)
declare function atom-protocol:op-retrieve-member(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry)? ,
	$request-media-type as xs:string?
) as item()*
{

	let $entry-doc := atomdb:retrieve-entry( $request-path-info )
	
	let $log := local:debug( $entry-doc )

    let $etag := concat( '"' , atomdb:generate-etag( $request-path-info ) , '"' )
    
    let $etag-header-set := 
        if ( exists( $etag ) ) then response:set-header( "ETag" , $etag ) else ()
    
	return ( $CONSTANT:STATUS-SUCCESS-OK , $entry-doc/atom:entry , $CONSTANT:MEDIA-TYPE-ATOM )

};





(:
 : TODO doc me
 :)
declare function atom-protocol:do-get-media(
	$request-path-info
)
{

	let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-retrieve-media" ) , 3 )
	
    return atom-protocol:apply-op( $CONSTANT:OP-RETRIEVE-MEDIA , $op , $request-path-info , () )

};




declare function atom-protocol:op-retrieve-media(
	$request-path-info as xs:string ,
	$request-data as item()? ,
	$request-media-type as xs:string?
) as item()*
{

	(: set status here, because we have to stream binary :)
	
    let $status-code := response:set-status-code( $CONSTANT:STATUS-SUCCESS-OK )
    
    (: media type :)
    
    let $mime-type := atomdb:get-mime-type( $request-path-info )
    
    (: title as filename :)
    
    let $media-link := atomdb:get-media-link( $request-path-info )
    let $title := $media-link/atom:title
    let $content-disposition :=
        if ( $title ) then response:set-header( $CONSTANT:HEADER-CONTENT-DISPOSITION , concat( 'attachment; filename="' , $title , '"' ) )
    	else ()
    
    (: decoding from base 64 binary :)
    
    let $response-stream := response:stream-binary( atomdb:retrieve-media( $request-path-info ) , $mime-type )

	(: don't return status code, because already set :)
	
	return ( () , () , () )

};





declare function atom-protocol:do-get-feed(
	$request-path-info
)
{

    (: 
     : Here we bottom out at the "list-collection" operation.
     :)

	let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-list-collection" ) , 3 )
	
    return atom-protocol:apply-op( $CONSTANT:OP-LIST-COLLECTION , $op , $request-path-info , () )
    
};


 

declare function atom-protocol:op-list-collection(
	$request-path-info as xs:string ,
	$request-data as item()? ,
	$request-media-type as xs:string?
) as item()*
{

    let $feed := atomdb:retrieve-feed( $request-path-info ) 
    
	return ( $CONSTANT:STATUS-SUCCESS-OK , $feed , $CONSTANT:MEDIA-TYPE-ATOM )

};





declare function atom-protocol:do-delete(
	$request-path-info as xs:string
) as item()*
{
	
	(: 
	 : We first need to know whether we are deleting a collection, a collection
	 : member entry, a media-link entry, or a media resource.
	 :)
	 
	if ( atomdb:collection-available( $request-path-info ) )
	then atom-protocol:do-delete-collection( $request-path-info )
	
	else if ( atomdb:member-available( $request-path-info ) )
	then atom-protocol:do-delete-member( $request-path-info )
	
	else if ( atomdb:media-resource-available( $request-path-info ) )
	then atom-protocol:do-delete-media( $request-path-info )
	
	else atom-protocol:do-not-found( $request-path-info )
	
};




declare function atom-protocol:do-delete-collection(
	$request-path-info as xs:string
) as item()*
{

    (: for now, do not support this operation :)
    atom-protocol:do-method-not-allowed( $request-path-info , ( "GET" , "POST" , "PUT" ) )
    
};




declare function atom-protocol:do-delete-member(
	$request-path-info as xs:string
) as item()*
{

    (: 
     : This is a little bit tricky...
     :
     : We need to know if this is a simple atom entry, or if this a media-link
     : entry. If it is a simple atom entry, then do the obvious, which is to
     : bottom out at the "delete-member" operation. However, if it is a media-link
     : entry, we will treat this as a "delete-media" operation, because the
     : delete on the media-link also causes a delete on the associated media
     : resource.
     :)
     
    if ( atomdb:media-link-available( $request-path-info ) )
    
    then 
    	let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-delete-media" ) , 3 )
    	return atom-protocol:apply-op( $CONSTANT:OP-DELETE-MEDIA , $op, $request-path-info, () )
    
    else 
    	let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-delete-member" ) , 3 )
    	return atom-protocol:apply-op( $CONSTANT:OP-DELETE-MEMBER , $op , $request-path-info , () )
			
};





(:
 : TODO doc me 
 :)
declare function atom-protocol:op-delete-member(
	$request-path-info as xs:string ,
	$request-data as item()? ,
	$request-media-type as xs:string?
) as item()*
{

    let $member-deleted := atomdb:delete-member( $request-path-info ) 

    let $collection-path-info := text:groups( $request-path-info , "^(.*)/[^/]+$" )[2]
    
    let $feed-date-updated := atomdb:touch-collection( $collection-path-info )
    
	return ( $CONSTANT:STATUS-SUCCESS-NO-CONTENT , () , () )

};





declare function atom-protocol:do-delete-media(
	$request-path-info as xs:string
) as item()*
{

    (: here we bottom out at the "delete-media" operation :)
    
	let $op := util:function( QName( "http://purl.org/atombeat/xquery/atom-protocol" , "atom-protocol:op-delete-media" ) , 3 )
	
	return atom-protocol:apply-op( $CONSTANT:OP-DELETE-MEDIA , $op , $request-path-info , () )

};




(:
 : TODO doc me 
 :)
declare function atom-protocol:op-delete-media(
	$request-path-info as xs:string ,
	$request-data as item()? ,
	$request-media-type as xs:string?
) as item()*
{

    let $media-deleted := atomdb:delete-media( $request-path-info ) 

    let $collection-path-info := text:groups( $request-path-info , "^(.*)/[^/]+$" )[2]
    
    let $feed-date-updated := atomdb:touch-collection( $collection-path-info )
    
	return ( $CONSTANT:STATUS-SUCCESS-NO-CONTENT , () , () )

};
 


declare function atom-protocol:do-not-modified(
    $request-path-info
) as item()?
{

    let $status-code-set := response:set-status-code( $CONSTANT:STATUS-REDIRECT-NOT-MODIFIED )
    
    return ()
    
};



declare function atom-protocol:do-not-found(
    $request-path-info
) as item()?
{

    let $message := "The server has not found anything matching the Request-URI."
    
    return atom-protocol:send-error( $CONSTANT:STATUS-CLIENT-ERROR-NOT-FOUND , $message , $request-path-info )

};



declare function atom-protocol:do-precondition-failed(
    $request-path-info ,
    $message
) as item()?
{

    atom-protocol:send-error( $CONSTANT:STATUS-CLIENT-ERROR-PRECONDITION-FAILED , concat( $message , " The precondition given in one or more of the request-header fields evaluated to false when it was tested on the server." ) , $request-path-info )

};



declare function atom-protocol:do-bad-request(
	$request-path-info as xs:string ,
	$message as xs:string 
) as item()?
{

    let $message := concat( $message , " The request could not be understood by the server due to malformed syntax. The client SHOULD NOT repeat the request without modifications." )

    return atom-protocol:send-error( $CONSTANT:STATUS-CLIENT-ERROR-BAD-REQUEST , $message , $request-path-info )

};




declare function atom-protocol:do-method-not-allowed(
	$request-path-info
) as item()?
{

    atom-protocol:do-method-not-allowed( $request-path-info , ( "GET" , "POST" , "PUT" ) )
    
};




declare function atom-protocol:do-method-not-allowed(
	$request-path-info as xs:string ,
	$allow as xs:string*
) as item()?
{

    let $message := "The method specified in the Request-Line is not allowed for the resource identified by the Request-URI."

	let $header-allow := response:set-header( $CONSTANT:HEADER-ALLOW , string-join( $allow , " " ) )

    return atom-protocol:send-error( $CONSTANT:STATUS-CLIENT-ERROR-METHOD-NOT-ALLOWED , $message , $request-path-info )

};

 



declare function atom-protocol:do-forbidden(
	$request-path-info as xs:string
) as item()?
{

    let $message := "The server understood the request, but is refusing to fulfill it. Authorization will not help and the request SHOULD NOT be repeated."

    return atom-protocol:send-error( $CONSTANT:STATUS-CLIENT-ERROR-FORBIDDEN , $message , $request-path-info )

};




declare function atom-protocol:do-unsupported-media-type(
	$request-path-info as xs:string
) as item()?
{

    let $message := "The server is refusing to service the request because the entity of the request is in a format not supported by the requested resource for the requested method."

    return atom-protocol:send-error( $CONSTANT:STATUS-CLIENT-ERROR-UNSUPPORTED-MEDIA-TYPE , $message , $request-path-info )

};





declare function atom-protocol:send-atom(
    $status as xs:integer ,
    $data as item()
) as item()*
{

    let $status-code := response:set-status-code( $status )
    
    let $header-content-type := response:set-header( $CONSTANT:HEADER-CONTENT-TYPE , $CONSTANT:MEDIA-TYPE-ATOM )
    
    return $data

};



declare function atom-protocol:send-response(
    $status as xs:integer? ,
    $data as item()? ,
    $content-type as xs:string?
) as item()*
{

	if ( $status >= 400 and $status < 600 )
	
	then (: override to wrap response with useful debugging information :)
		let $request-path-info := request:get-attribute( $atom-protocol:param-request-path-info )
		return atom-protocol:send-error( $status , $data , $request-path-info )
		
	else
	
	    let $status-code-set := 
	    	if ( exists( $status ) ) then response:set-status-code( $status )
	    	else ()

	    let $header-content-type := 
	    	if ( exists( $content-type ) ) then response:set-header( $CONSTANT:HEADER-CONTENT-TYPE , $content-type )
			else ()
			
	    return $data

};





 
(:
 : TODO doc me
 :)
declare function atom-protocol:send-error(
    $status-code as xs:integer , 
    $content as item()? ,
    $request-path-info as xs:string?
) as item()*
{

	let $status-code-set := response:set-status-code( $status-code )

	let $header-content-type := response:set-header( $CONSTANT:HEADER-CONTENT-TYPE , $CONSTANT:MEDIA-TYPE-XML )

	let $response := 
	
		<error>
		    <status>{$status-code}</status>
			<content>{$content}</content>
			<method>{request:get-method()}</method>
			<path-info>{$request-path-info}</path-info>
			<parameters>
			{
				for $parameter-name in request:get-parameter-names()
				return
				    <parameter>
				        <name>{$parameter-name}</name>
				        <value>{request:get-parameter( $parameter-name , "" )}</value>						
					</parameter>
			}
			</parameters>
			<headers>
			{
				for $header-name in request:get-header-names()
				return
				    <header>
				        <name>{$header-name}</name>
				        <value>{request:get-header( $header-name )}</value>						
					</header>
			}
			</headers>
			<user>{request:get-attribute($config:user-name-request-attribute-key)}</user>
			<roles>{string-join(request:get-attribute($config:user-roles-request-attribute-key), " ")}</roles>
		</error>
			
	return $response

};




(:
 : Main request processing function.
 :)
declare function atom-protocol:apply-op(
	$op-name as xs:string ,
	$op as function ,
	$request-path-info as xs:string ,
	$request-data as item()*
) as item()*
{

	atom-protocol:apply-op( $op-name , $op , $request-path-info , $request-data , () )
	
};




(:
 : Main request processing function.
 :)
declare function atom-protocol:apply-op(
	$op-name as xs:string ,
	$op as function ,
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string?
) as item()*
{

	let $log := local:debug( "call plugin functions before main operation" )
	
	let $before-advice := atom-protocol:apply-before( plugin:before() , $op-name , $request-path-info , $request-data , $request-media-type )
	let $log := local:debug( count( $before-advice ) )
	
	let $status-code as xs:integer := $before-advice[1]
	
	return 
	 
		if ( $status-code > 0 ) (: interrupt request processing :)
		
		then 
		
			let $log := local:info( ( "bail out - plugin has overridden default behaviour, status: " , $status-code ) )
		
			let $response-data := $before-advice[2]
			let $response-content-type := $before-advice[3]
			let $log := local:debug( concat( "$status-code: " , $status-code ) )
			let $log := local:debug( concat( "$response-data: " , $response-data ) )
			let $log := local:debug( concat( "$response-content-type: " , $response-content-type ) )
			return atom-protocol:send-response( $status-code , $response-data , $response-content-type ) 
		  
		else
		
			let $log := local:debug( "carry on as normal - execute main operation" )
			
			let $request-data := $before-advice[2] (: request data may have been modified by plugins :)

			let $result := util:call( $op , $request-path-info , $request-data , $request-media-type )
			let $response-status := $result[1]
			let $response-data := $result[2]
			let $response-content-type := $result[3]
			
			let $log := local:debug( $response-status )
			let $log := local:debug( $response-data )
			let $log := local:debug( $response-content-type )

			let $log := local:debug( "call plugin functions after main operation" ) 
			 
			let $after-advice := atom-protocol:apply-after( plugin:after() , $op-name , $request-path-info , $response-data , $response-content-type )
			
			let $response-data := $after-advice[1]
			let $response-content-type := $after-advice[2]
					    
			return atom-protocol:send-response( $response-status , $response-data , $response-content-type )

};





(:
 : Recursively call the sequence of plugin functions.
 :)
declare function atom-protocol:apply-before(
	$functions as function* ,
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string?
) as item()* {
	
	(:
	 : Plugin functions applied during the before phase can have no side-effects,
	 : in which case they will return the request data unaltered, or they can
	 : modify the request data, or they can interrupt the processing of the
	 : request and cause a response to be sent, without calling any subsequent
	 : plugin functions or carrying out the target operation.
	 :)
	 
	if ( empty( $functions ) )
	
	then ( 0 , $request-data )
	
	else
	
		let $advice := util:call( $functions[1] , $operation , $request-path-info , $request-data , $request-media-type )
		
		(: what happens next depends on advice :)
		
		let $status-code as xs:integer := $advice[1]
		
		return
		
			if ( $status-code > 0 )
			
			then $advice (: bail out, no further calling of before functions :)

			else 
			
			    let $request-data := $advice[2]
			    
			    (: recursively call until before functions are exhausted :)
			    return atom-protocol:apply-before( subsequence( $functions , 2 ) , $operation , $request-path-info , $request-data , $request-media-type )

};




(:
 : Recursively call the sequence of plugin functions.
 :)
declare function atom-protocol:apply-after(
	$functions as function* ,
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$response-data as item()* ,
	$content-type as xs:string?
) as item()* {
	
	if ( empty( $functions ) )
	
	then ( $response-data , $content-type )
	
	else
	
		(:
		 : The after functions can modify the response data and response content
		 : type, but cannot alter the status code.
		 :)
		 
		let $advice := util:call( $functions[1] , $operation , $request-path-info , $response-data , $content-type )
		
		let $response-data := $advice[1]
		let $content-type := $advice[2]
		
		return
		
			atom-protocol:apply-after( subsequence( $functions , 2 ) , $operation , $request-path-info , $response-data , $content-type )

};






