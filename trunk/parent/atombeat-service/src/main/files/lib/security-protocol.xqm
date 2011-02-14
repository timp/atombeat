xquery version "1.0";

module namespace security-protocol = "http://purl.org/atombeat/xquery/security-protocol";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;

import module namespace request = "http://exist-db.org/xquery/request" ;
import module namespace response = "http://exist-db.org/xquery/response" ;
import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "constants.xqm" ;

import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "xutil.xqm" ;
import module namespace mime = "http://purl.org/atombeat/xquery/mime" at "mime.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "atomdb.xqm" ;
import module namespace atomsec = "http://purl.org/atombeat/xquery/atom-security" at "atom-security.xqm" ;
import module namespace common-protocol = "http://purl.org/atombeat/xquery/common-protocol" at "common-protocol.xqm" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
 
 
 
 
declare function security-protocol:main() as item()*
{
    let $request := common-protocol:get-request()
    let $response := security-protocol:do-service( $request )
    return common-protocol:respond( $request , $response )
};




(:
 : TODO doc me
 :)
declare function security-protocol:do-service(
    $request as element(request)
)
as element(response)
{

	let $request-path-info := $request/path-info/text()
	let $request-method := $request/method/text()
	
	return
	
        if (
            not( $request-path-info = "/" )
            and not( atomdb:collection-available( $request-path-info ) )
            and not( atomdb:member-available( $request-path-info ) )
            and not( atomdb:media-resource-available( $request-path-info ) )
        )
        
        then common-protocol:do-not-found( $CONSTANT:OP-SECURITY-PROTOCOL-ERROR , $request )
        
		else if ( $request-method = $CONSTANT:METHOD-GET )
		
		then security-protocol:do-get( $request )
		
		else if ( $request-method = $CONSTANT:METHOD-PUT )
		
		then security-protocol:do-put( $request , request:get-data() )
		
		else common-protocol:do-method-not-allowed( $CONSTANT:OP-SECURITY-PROTOCOL-ERROR , $request , ( "GET" , "PUT" ) )

};



(:
 : TODO doc me 
 :)
declare function security-protocol:do-get(
    $request as element(request)
) as element(response)
{

    let $request-path-info := $request/path-info/text() 

    let $op-name := 
        if ( $request-path-info = "/" ) then $CONSTANT:OP-RETRIEVE-WORKSPACE-ACL
        else if ( atomdb:collection-available( $request-path-info ) ) then $CONSTANT:OP-RETRIEVE-COLLECTION-ACL
        else if ( atomdb:member-available( $request-path-info ) ) then $CONSTANT:OP-RETRIEVE-MEMBER-ACL
        else if ( atomdb:media-resource-available( $request-path-info ) ) then $CONSTANT:OP-RETRIEVE-MEDIA-ACL
        else () (: should never be reached :)
    
    let $op := util:function( QName( "http://purl.org/atombeat/xquery/security-protocol" , "atom-protocol:op-retrieve-descriptor" ) , 2 )
    
    return common-protocol:apply-op( $op-name , $op , $request , () )

};




declare function security-protocol:op-retrieve-descriptor(
	$request as element(request) ,
	$entity as item()* (: expect this to be empty, but have to include to get consistent function signature :)
) as element(response)
{

    let $request-path-info := $request/path-info/text()
    let $descriptor := atomsec:retrieve-descriptor( $request-path-info )
    return security-protocol:response-with-descriptor( $request , $descriptor )

};




(:
 : TODO doc me 
 :)
declare function security-protocol:do-put(
	$request as element(request) ,
	$entity as item()* (: assume nothing about request entity yet :) 
) as element(response)
{

	let $request-content-type := xutil:get-header( $CONSTANT:HEADER-CONTENT-TYPE , $request )
	
	return
	
	    if ( not( starts-with( $request-content-type, $CONSTANT:MEDIA-TYPE-ATOM ) ) ) then
	    
	        common-protocol:do-unsupported-media-type( $CONSTANT:OP-SECURITY-PROTOCOL-ERROR , $request , "Only application/atom+xml is supported." )
	        
	    else if ( not( $entity instance of element(atom:entry) ) ) then

            common-protocol:do-bad-request( $CONSTANT:OP-SECURITY-PROTOCOL-ERROR , $request , "Request entity must be well-formed XML and the root element must be an Atom entry element." )

	    else 

            let $request-path-info := $request/path-info/text()
        
            let $op-name := 
                if ( $request-path-info = "/" ) then $CONSTANT:OP-UPDATE-WORKSPACE-ACL
                else if ( atomdb:collection-available( $request-path-info ) ) then $CONSTANT:OP-UPDATE-COLLECTION-ACL
                else if ( atomdb:member-available( $request-path-info ) ) then $CONSTANT:OP-UPDATE-MEMBER-ACL
                else if ( atomdb:media-resource-available( $request-path-info ) ) then $CONSTANT:OP-UPDATE-MEDIA-ACL
                else () (: should never be reached :)
        
            let $op := util:function( QName( "http://purl.org/atombeat/xquery/security-protocol" , "atom-protocol:op-update-descriptor" ) , 2 )
            
            return common-protocol:apply-op( $op-name , $op , $request , $entity )
        
};




declare function security-protocol:op-update-descriptor(
	$request as element(request) ,
	$entity as element(atom:entry)  
) as element(response)
{

    let $descriptor := $entity/atom:content[@type="application/vnd.atombeat+xml"]/atombeat:security-descriptor[exists(atombeat:acl)]

    return 
        
        if ( empty( $descriptor ) )
        then security-protocol:do-bad-descriptor( $request )
        
        else

            let $request-path-info := $request/path-info/text()
            let $descriptor-updated := atomsec:store-descriptor( $request-path-info , $descriptor )
            let $descriptor := atomsec:retrieve-descriptor( $request-path-info )
            return security-protocol:response-with-descriptor( $request , $descriptor )

};




declare function security-protocol:response-with-descriptor(
	$request as element(request) ,
    $descriptor as element(atombeat:security-descriptor)?
) as element(response)
{

    let $request-path-info := $request/path-info/text()
    let $entry := atomsec:wrap-with-entry( $request-path-info , $descriptor )

    return
    
        <response>
            <status>{$CONSTANT:STATUS-SUCCESS-OK}</status>
            <headers>
                <header>
                    <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                    <value>{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}</value>
                </header>
            </headers>
            <body type='xml'>{$entry}</body>
        </response>
};




declare function security-protocol:do-bad-descriptor(
	$request as element(request) 
) as element(response)
{

    let $message := "Request entity must match /atom:entry/atom:content[@type='application/vnd.atombeat+xml']/atombeat:security-descriptor/atombeat:acl."
    return common-protocol:do-bad-request( $CONSTANT:OP-SECURITY-PROTOCOL-ERROR , $request , $message )
    
};


