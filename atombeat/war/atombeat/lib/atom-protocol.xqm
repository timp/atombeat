xquery version "1.0";

module namespace ap = "http://www.cggh.org/2010/xquery/atom-protocol";

declare namespace atom = "http://www.w3.org/2005/Atom" ;

import module namespace request = "http://exist-db.org/xquery/request" ;
import module namespace response = "http://exist-db.org/xquery/response" ;
import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace CONSTANT = "http://www.cggh.org/2010/atombeat/xquery/constants" at "constants.xqm" ;
import module namespace mime = "http://www.cggh.org/2010/atombeat/xquery/mime" at "mime.xqm" ;
import module namespace atomdb = "http://www.cggh.org/2010/atombeat/xquery/atomdb" at "atomdb.xqm" ;
 
import module namespace config = "http://www.cggh.org/2010/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace plugin = "http://www.cggh.org/2010/atombeat/xquery/plugin" at "../config/plugins.xqm" ;

declare variable $ap:param-request-path-info := "request-path-info" ; 

 


(:
 : TODO doc me  
 :)
declare function ap:do-service()
as item()*
{

	let $request-path-info := request:get-attribute( $ap:param-request-path-info )
	let $request-method := request:get-method()
	
	return
	
		if ( $request-method = $CONSTANT:METHOD-POST )

		then ap:do-post( $request-path-info )
		
		else if ( $request-method = $CONSTANT:METHOD-PUT )
		
		then ap:do-put( $request-path-info )
		
		else if ( $request-method = $CONSTANT:METHOD-GET )
		
		then ap:do-get( $request-path-info )
		
		else if ( $request-method = $CONSTANT:METHOD-DELETE )
		
		then ap:do-delete( $request-path-info )
		
		else ap:do-method-not-allowed( $request-path-info )

};




(:
 : TODO doc me
 :)
declare function ap:do-post(
	$request-path-info as xs:string 
) as item()*
{

	let $request-content-type := request:get-header( $CONSTANT:HEADER-CONTENT-TYPE )

	return 

		if ( starts-with( $request-content-type, $CONSTANT:MEDIA-TYPE-ATOM ) )
		
		then ap:do-post-atom( $request-path-info )
		
		else if ( starts-with( $request-content-type, $CONSTANT:MEDIA-TYPE-MULTIPART-FORM-DATA ) )
		
		then ap:do-post-multipart( $request-path-info )
		
		else ap:do-post-media( $request-path-info , $request-content-type )

};




(:
 : TODO doc me
 :)
declare function ap:do-post-atom(
	$request-path-info as xs:string 
) as item()*
{

	let $request-data := request:get-data()

	return
	
		if (
			local-name( $request-data ) = $CONSTANT:ATOM-FEED and 
			namespace-uri( $request-data ) = $CONSTANT:ATOM-NSURI
		)
		
		then ap:do-post-atom-feed( $request-path-info , $request-data )

		else if (
			local-name( $request-data ) = $CONSTANT:ATOM-ENTRY and 
			namespace-uri( $request-data ) = $CONSTANT:ATOM-NSURI
		)
		
		then ap:do-post-atom-entry( $request-path-info , $request-data )
		
		else ap:do-bad-request( $request-path-info , "Request entity must be either atom feed or atom entry." )

};




declare function ap:do-post-atom-feed(
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

		then ap:apply-op( $CONSTANT:OP-CREATE-COLLECTION , $ap:op-create-collection , $request-path-info , $request-data )
		
		else ap:do-bad-request( $request-path-info , "A collection already exists at the given location." )
        	
};




(:
 : TODO doc me 
 :)
declare function ap:op-create-collection(
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




declare variable $ap:op-create-collection as function :=
	util:function( QName( "http://www.cggh.org/2010/xquery/atom-protocol" , "ap:op-create-collection" ) , 3 )
;




(:
 : TODO doc me
 :)
declare function ap:do-post-atom-entry(
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

		then ap:do-not-found( $request-path-info )
		
		else
		
            (: 
             : Here we bottom out at the "create-member" operation.
             :)
             
            ap:apply-op( $CONSTANT:OP-CREATE-MEMBER , $ap:op-create-member , $request-path-info , $request-data )
        
};






(:
 : TODO doc me
 :)
declare function ap:op-create-member(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry) ,
	$request-media-type as xs:string?
) as item()*
{

	let $log := util:log( "debug" , "op-create-member" )
	let $log := util:log( "debug" , $request-data )
	
	let $entry-doc-db-path := atomdb:create-member( $request-path-info , $request-data )

	let $entry-doc := doc( $entry-doc-db-path )
            
    let $location := $entry-doc/atom:entry/atom:link[@rel="self"]/@href
        	
	let $header-location := response:set-header( $CONSTANT:HEADER-LOCATION, $location )
	
	return ( $CONSTANT:STATUS-SUCCESS-CREATED , $entry-doc/atom:entry , $CONSTANT:MEDIA-TYPE-ATOM )
		
};




(:
 : TODO doc me
 :)
declare variable $ap:op-create-member as function :=
	util:function( QName( "http://www.cggh.org/2010/xquery/atom-protocol" , "ap:op-create-member" ) , 3 )
;





(:
 : TODO doc me
 :)
declare function ap:do-post-media(
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

		then ap:do-not-found( $request-path-info )
		
		else
		
            (: 
             : Here we bottom out at the "create-media" operation.
             :)
             
        	let $media-type := text:groups( $request-content-type , "^([^;]+)" )[2]
	
            return ap:apply-op( $CONSTANT:OP-CREATE-MEDIA , $ap:op-create-media , $request-path-info , request:get-data() , $media-type )
                        			
};




(:
 : TODO doc me
 :)
declare function ap:op-create-media(
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
			    
	return ( $CONSTANT:STATUS-SUCCESS-CREATED , $media-link-doc/atom:entry , $CONSTANT:MEDIA-TYPE-ATOM )

};




declare variable $ap:op-create-media as function :=
	util:function( QName( "http://www.cggh.org/2010/xquery/atom-protocol" , "ap:op-create-media" ) , 3 )
;




(:
 : TODO doc me
 :)
declare function ap:do-post-multipart(
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

		then ap:do-not-found( $request-path-info )
		
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
             
            return ap:apply-op( $CONSTANT:OP-CREATE-MEDIA , $ap:op-create-media-from-multipart-form-data , $request-path-info , $request-data , $media-type )

};





(: 
 : TODO doc me 
 :)
declare function ap:op-create-media-from-multipart-form-data (
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

    (:
     : Although the semantics of 201 Created would be more appropriate 
     : to the operation performed, we'll respond with 200 OK because the
     : request is most likely to originate from an HTML form submission
     : and I don't know how all browsers handle 201 responses.
     :)
     
	return ( $CONSTANT:STATUS-SUCCESS-OK , $response-data , $response-content-type )

};




declare variable $ap:op-create-media-from-multipart-form-data as function :=
	util:function( QName( "http://www.cggh.org/2010/xquery/atom-protocol" , "ap:op-create-media-from-multipart-form-data" ) , 3 )
;



(:
 : TODO doc me
 :)
declare function ap:do-put (
	$request-path-info as xs:string 
) as item()*
{

	let $request-content-type := request:get-header( $CONSTANT:HEADER-CONTENT-TYPE )

	return 

		if ( starts-with( $request-content-type, $CONSTANT:MEDIA-TYPE-ATOM ) )
		
		then ap:do-put-atom( $request-path-info )

		else ap:do-put-media( $request-path-info , $request-content-type )

};




declare function ap:do-put-atom(
	$request-path-info as xs:string 
) as item()*
{

	let $request-data := request:get-data()

	return
	
		if (
			local-name( $request-data ) = $CONSTANT:ATOM-FEED and 
			namespace-uri( $request-data ) = $CONSTANT:ATOM-NSURI
		)
		
		then ap:do-put-atom-feed( $request-path-info , $request-data )

		else if (
			local-name( $request-data ) = $CONSTANT:ATOM-ENTRY and 
			namespace-uri( $request-data ) = $CONSTANT:ATOM-NSURI
		)
		
		then ap:do-put-atom-entry( $request-path-info , $request-data )
		
		else ap:do-bad-request( $request-path-info , "Request entity must be either atom feed or atom entry." )

};




(:
 : TODO doc me 
 :)
declare function ap:do-put-atom-feed(
	$request-path-info as xs:string ,
	$request-data as element(atom:feed)
) as item()*
{

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
		then ap:do-put-atom-feed-to-create-collection( $request-path-info , $request-data )
		else ap:do-put-atom-feed-to-update-collection( $request-path-info , $request-data )	

};




(: 
 : TODO doc me
 :)
declare function ap:do-put-atom-feed-to-create-collection(
	$request-path-info as xs:string ,
	$request-data as element(atom:feed)
) as item()*
{

    (: 
     : Here we bottom out at the "create-collection" operation.
     :)
     
    ap:apply-op( $CONSTANT:OP-CREATE-COLLECTION , $ap:op-create-collection , $request-path-info , $request-data )
        		
};




(:
 : TODO doc me
 :)
declare function ap:do-put-atom-feed-to-update-collection(
	$request-path-info as xs:string ,
	$request-data as element(atom:feed)
) as item()*
{

    (: 
     : Here we bottom out at the "update-collection" operation, so we need to 
     : apply a security decision.
     :)
     
    ap:apply-op( $CONSTANT:OP-UPDATE-COLLECTION , $ap:op-update-collection , $request-path-info , $request-data )

};




(:
 : TODO doc me 
 :)
declare function ap:op-update-collection(
	$request-path-info as xs:string ,
	$request-data as element(atom:feed) ,
	$request-media-type as xs:string?
) as item()*
{

	let $feed-doc-db-path := atomdb:update-collection( $request-path-info , $request-data )
		
	let $feed := doc( $feed-doc-db-path )/atom:feed
            
	return ( $CONSTANT:STATUS-SUCCESS-OK , $feed , $CONSTANT:MEDIA-TYPE-ATOM )

};




declare variable $ap:op-update-collection as function :=
	util:function( QName( "http://www.cggh.org/2010/xquery/atom-protocol" , "ap:op-update-collection" ) , 3 )
;





(:
 : TODO doc me
 :)
declare function ap:do-put-atom-entry(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry)
) as item()*
{

	(: 
	 : First we need to know whether an atom entry exists at the 
	 : request path.
	 :)
	 
	let $member-available := atomdb:member-available( $request-path-info )
	
	return 
	
		if ( not( $member-available ) ) 

		then ap:do-not-found( $request-path-info )
		
		else
		
		    (: 
		     : Here we bottom out at the "update-member" operation.
		     :)
            
            ap:apply-op( $CONSTANT:OP-UPDATE-MEMBER , $ap:op-update-member , $request-path-info , $request-data ) 
        
};




(:
 : TODO doc me
 :)
declare function ap:op-update-member(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry) ,
	$request-media-type as xs:string?
) as item()*
{
    
	let $entry := atomdb:update-member( $request-path-info , $request-data )

	(: 
	 : N.B. we return the entry here, rather than trying to retrieve the updated
	 : entry from the database, because of a weird interaction with the versioning
	 : module, not seeing updates within the same query.
	 :)
	 
	return ( $CONSTANT:STATUS-SUCCESS-OK , $entry , $CONSTANT:MEDIA-TYPE-ATOM )

};




declare variable $ap:op-update-member as function :=
	util:function( QName( "http://www.cggh.org/2010/xquery/atom-protocol" , "ap:op-update-member" ) , 3 )
;



(: 
 : TODO doc me
 :)
declare function ap:do-put-media(
	$request-path-info as xs:string ,
	$request-content-type
) as item()*
{

	(: 
	 : First we need to know whether a media resource exists at the 
	 : request path.
	 :)
	 
	let $found := atomdb:media-resource-available( $request-path-info )
	
	return 
	
		if ( not( $found ) ) 

		then ap:do-not-found( $request-path-info )
		
		else
		
			(: here we bottom out at the "update-media" operation :)
			
			let $request-data := request:get-data()
			
			return ap:apply-op( $CONSTANT:OP-UPDATE-MEDIA , $ap:op-update-media , $request-path-info , $request-data , $request-content-type )
			
};




declare function ap:op-update-media(
	$request-path-info as xs:string ,
	$request-data as item()? ,
	$request-content-type as xs:string?
) as item()*
{
	
	let $media-doc-db-path := atomdb:update-media-resource( $request-path-info , $request-data , $request-content-type )
	
    let $status-code := response:set-status-code( $CONSTANT:STATUS-SUCCESS-OK )
    
    (:
     : TODO review whether we really want to echo the file back to client
     : or rather return media-link entry (with content-location header), or 
     : rather return nothing.
     :)
     
    (: media type :)
    
    let $mime-type := atomdb:get-mime-type( $request-path-info )
    
    (: title as filename :)
    
    let $media-link := atomdb:get-media-link( $request-path-info )
    let $title := $media-link/atom:title
    let $content-disposition :=
    	if ( $title ) then response:set-header( $CONSTANT:HEADER-CONTENT-DISPOSITION , concat( "attachment; filename=" , $title ) )
    	else ()
    
    (: decoding from base 64 binary :)
    
    let $response-stream := response:stream-binary( atomdb:retrieve-media( $request-path-info ) , $mime-type )

	(: don't return status code, because already set :)
	(: don't return response data, because streaming binary :)
	
	return ( () , () , () )

};




declare variable $ap:op-update-media as function :=
	util:function( QName( "http://www.cggh.org/2010/xquery/atom-protocol" , "ap:op-update-media" ) , 3 )
;







(: 
 : TODO doc me 
 :)
declare function ap:do-get(
	$request-path-info as xs:string 
) as item()*
{

	if ( atomdb:media-resource-available( $request-path-info ) )
	
	then ap:do-get-media( $request-path-info )
	
	else if ( atomdb:member-available( $request-path-info ) )
	
	then ap:do-get-entry( $request-path-info )
	
	else if ( atomdb:collection-available( $request-path-info ) )
	
	then ap:do-get-feed( $request-path-info )

	else ap:do-not-found( $request-path-info )
	
};




(:
 : TODO doc me
 :)
declare function ap:do-get-entry(
	$request-path-info
)
{
    
    (: 
     : Here we bottom out at the "retrieve-member" operation.
     :)

    ap:apply-op( $CONSTANT:OP-RETRIEVE-MEMBER , $ap:op-retrieve-member , $request-path-info , () )

};




(:
 : TODO doc me
 :)
declare function ap:op-retrieve-member(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry)? ,
	$request-media-type as xs:string?
) as item()*
{

	let $entry-doc := atomdb:retrieve-entry( $request-path-info )

	return ( $CONSTANT:STATUS-SUCCESS-OK , $entry-doc/atom:entry , $CONSTANT:MEDIA-TYPE-ATOM )

};




declare variable $ap:op-retrieve-member as function :=
	util:function( QName( "http://www.cggh.org/2010/xquery/atom-protocol" , "ap:op-retrieve-member" ) , 3 )
;




(:
 : TODO doc me
 :)
declare function ap:do-get-media(
	$request-path-info
)
{

    ap:apply-op( $CONSTANT:OP-RETRIEVE-MEDIA , $ap:op-retrieve-media , $request-path-info , () )

};




declare function ap:op-retrieve-media(
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
    	if ( $title ) then response:set-header( $CONSTANT:HEADER-CONTENT-DISPOSITION , concat( "attachment; filename=" , $title ) )
    	else ()
    
    (: decoding from base 64 binary :)
    
    let $response-stream := response:stream-binary( atomdb:retrieve-media( $request-path-info ) , $mime-type )

	(: don't return status code, because already set :)
	
	return ( () , () , () )

};




declare variable $ap:op-retrieve-media as function :=
	util:function( QName( "http://www.cggh.org/2010/xquery/atom-protocol" , "ap:op-retrieve-media" ) , 3 )
;




declare function ap:do-get-feed(
	$request-path-info
)
{

    (: 
     : Here we bottom out at the "list-collection" operation.
     :)

    ap:apply-op( $CONSTANT:OP-LIST-COLLECTION , $ap:op-list-collection , $request-path-info , () )
    
};


 

declare function ap:op-list-collection(
	$request-path-info as xs:string ,
	$request-data as item()? ,
	$request-media-type as xs:string?
) as item()*
{

    let $feed := atomdb:retrieve-feed( $request-path-info ) 
    
	return ( $CONSTANT:STATUS-SUCCESS-OK , $feed , $CONSTANT:MEDIA-TYPE-ATOM )

};





declare variable $ap:op-list-collection as function :=
	util:function( QName( "http://www.cggh.org/2010/xquery/atom-protocol" , "ap:op-list-collection" ) , 3 )
;




declare function ap:do-delete(
	$request-path-info as xs:string
) as item()*
{
	(: TODO :)
	
	(: 
	 : We first need to know whether we are deleting a collection, a collection
	 : member entry, a media-link entry, or a media resource.
	 :)
	 
	if ( atomdb:collection-available( $request-path-info ) )
	then ap:do-delete-collection( $request-path-info )
	else if ( atomdb:member-available( $request-path-info ) )
	then ap:do-delete-member( $request-path-info )
	else if ( atomdb:media-resource-available( $request-path-info ) )
	then ap:do-delete-media( $request-path-info )
	else ap:do-not-found( $request-path-info )
	
};




declare function ap:do-delete-collection(
	$request-path-info as xs:string
) as item()*
{

    (: for now, do not support this operation :)
    ap:do-method-not-allowed( $request-path-info , ( "GET" , "POST" , "PUT" ) )
    
};




declare function ap:do-delete-member(
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
    then ap:apply-op( $CONSTANT:OP-DELETE-MEDIA , $ap:op-delete-media, $request-path-info, () )
    else ap:apply-op( $CONSTANT:OP-DELETE-MEMBER , $ap:op-delete-member , $request-path-info , () )
			
};





(:
 : TODO doc me 
 :)
declare function ap:op-delete-member(
	$request-path-info as xs:string ,
	$request-data as item()? ,
	$request-media-type as xs:string?
) as item()*
{

    let $member-deleted := atomdb:delete-member( $request-path-info ) 
	return ( $CONSTANT:STATUS-SUCCESS-NO-CONTENT , () , () )

};




declare variable $ap:op-delete-member as function :=
	util:function( QName( "http://www.cggh.org/2010/xquery/atom-protocol" , "ap:op-delete-member" ) , 3 )
;





declare function ap:do-delete-media(
	$request-path-info as xs:string
) as item()*
{

    (: here we bottom out at the "delete-media" operation :)
	ap:apply-op( $CONSTANT:OP-DELETE-MEDIA , $ap:op-delete-media , $request-path-info , () )

};




(:
 : TODO doc me 
 :)
declare function ap:op-delete-media(
	$request-path-info as xs:string ,
	$request-data as item()? ,
	$request-media-type as xs:string?
) as item()*
{

    let $media-deleted := atomdb:delete-media( $request-path-info ) 
	return ( $CONSTANT:STATUS-SUCCESS-NO-CONTENT , () , () )

};
 


declare variable $ap:op-delete-media as function :=
	util:function( QName( "http://www.cggh.org/2010/xquery/atom-protocol" , "ap:op-delete-media" ) , 3 )
;





declare function ap:do-not-found(
	$request-path-info
) as item()?
{

    let $message := "The server has not found anything matching the Request-URI."
    
    return ap:send-error( $CONSTANT:STATUS-CLIENT-ERROR-NOT-FOUND , $message , $request-path-info )

};



declare function ap:do-bad-request(
	$request-path-info as xs:string ,
	$message as xs:string 
) as item()?
{

    let $message := concat( $message , " The request could not be understood by the server due to malformed syntax. The client SHOULD NOT repeat the request without modifications." )

    return ap:send-error( $CONSTANT:STATUS-CLIENT-ERROR-BAD-REQUEST , $message , $request-path-info )

};




declare function ap:do-method-not-allowed(
	$request-path-info
) as item()?
{

    ap:do-method-not-allowed( $request-path-info , ( "GET" , "POST" , "PUT" ) )
    
};




declare function ap:do-method-not-allowed(
	$request-path-info as xs:string ,
	$allow as xs:string*
) as item()?
{

    let $message := "The method specified in the Request-Line is not allowed for the resource identified by the Request-URI."

	let $header-allow := response:set-header( $CONSTANT:HEADER-ALLOW , string-join( $allow , " " ) )

    return ap:send-error( $CONSTANT:STATUS-CLIENT-ERROR-METHOD-NOT-ALLOWED , $message , $request-path-info )

};

 



declare function ap:do-forbidden(
	$request-path-info as xs:string
) as item()?
{

    let $message := "The server understood the request, but is refusing to fulfill it. Authorization will not help and the request SHOULD NOT be repeated."

    return ap:send-error( $CONSTANT:STATUS-CLIENT-ERROR-FORBIDDEN , $message , $request-path-info )

};





declare function ap:send-atom(
    $status as xs:integer ,
    $data as item()
) as item()*
{

    let $status-code := response:set-status-code( $status )
    
    let $header-content-type := response:set-header( $CONSTANT:HEADER-CONTENT-TYPE , $CONSTANT:MEDIA-TYPE-ATOM )
    
    return $data

};



declare function ap:send-response(
    $status as xs:integer? ,
    $data as item()? ,
    $content-type as xs:string?
) as item()*
{

	if ( $status >= 400 and $status < 600 )
	
	then (: override to wrap response with useful debugging information :)
		let $request-path-info := request:get-attribute( $ap:param-request-path-info )
		return ap:send-error( $status , $data , $request-path-info )
		
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
declare function ap:send-error(
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
declare function ap:apply-op(
	$op-name as xs:string ,
	$op as function ,
	$request-path-info as xs:string ,
	$request-data as item()*
) as item()*
{

	ap:apply-op( $op-name , $op , $request-path-info , $request-data , () )
	
};




(:
 : Main request processing function.
 :)
declare function ap:apply-op(
	$op-name as xs:string ,
	$op as function ,
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string?
) as item()*
{

	let $log := util:log( "debug" , "EXPERIMENTAL: call plugin functions before main operation" )
	
	let $before-advice := ap:apply-before( $plugin:before , $op-name , $request-path-info , $request-data , $request-media-type )
	let $log := util:log( "debug" , count( $before-advice ) )
	
	let $status-code as xs:integer := $before-advice[1]
	
	return 
	 
		if ( $status-code > 0 ) (: interrupt request processing :)
		
		then 
		
			let $log := util:log( "debug" , "bail out - plugin has overridden default behaviour" )
		
			let $response-data := $before-advice[2]
			let $response-content-type := $before-advice[3]
			let $log := util:log( "debug" , concat( "$status-code: " , $status-code ) )
			let $log := util:log( "debug" , concat( "$response-data: " , $response-data ) )
			let $log := util:log( "debug" , concat( "$response-content-type: " , $response-content-type ) )
			return ap:send-response( $status-code , $response-data , $response-content-type ) 
		  
		else
		
			let $log := util:log( "debug" , "carry on as normal - execute main operation" )
			
			let $request-data := $before-advice[2] (: request data may have been modified by plugins :)

			let $result := util:call( $op , $request-path-info , $request-data , $request-media-type )
			let $response-status := $result[1]
			let $response-data := $result[2]
			let $response-content-type := $result[3]

			let $log := util:log( "debug" , "EXPERIMENTAL: call plugin functions after main operation" ) 
			 
			let $after-advice := ap:apply-after( $plugin:after , $op-name , $request-path-info , $response-data , $response-content-type )
			
			let $response-data := $after-advice[1]
			let $response-content-type := $after-advice[2]
					    
			return ap:send-response( $response-status , $response-data , $response-content-type )

};





(:
 : Recursively call the sequence of plugin functions.
 :)
declare function ap:apply-before(
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
			    return ap:apply-before( subsequence( $functions , 2 ) , $operation , $request-path-info , $request-data , $request-media-type )

};




(:
 : Recursively call the sequence of plugin functions.
 :)
declare function ap:apply-after(
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
		
			ap:apply-after( subsequence( $functions , 2 ) , $operation , $request-path-info , $response-data , $content-type )

};






