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
    let $response := security-protocol:do-service()
    return common-protocol:respond( $response )
};




(:
 : TODO doc me
 :)
declare function security-protocol:do-service()
as element(response)
{

	let $request-path-info := request:get-attribute( $common-protocol:param-request-path-info )
	let $request-method := request:get-method()
	
	return
	
        if (
            not( $request-path-info = "/" )
            and not( atomdb:collection-available( $request-path-info ) )
            and not( atomdb:member-available( $request-path-info ) )
            and not( atomdb:media-resource-available( $request-path-info ) )
        )
        
        then common-protocol:do-not-found( $request-path-info )
        
		else if ( $request-method = $CONSTANT:METHOD-GET )
		
		then security-protocol:do-get( $request-path-info )
		
		else if ( $request-method = $CONSTANT:METHOD-PUT )
		
		then security-protocol:do-put( $request-path-info )
		
		else common-protocol:do-method-not-allowed( $request-path-info , ( "GET" , "PUT" ) )

};



(:
 : TODO doc me 
 :)
declare function security-protocol:do-get(
	$request-path-info as xs:string 
) as element(response)
{

    let $op := util:function( QName( "http://purl.org/atombeat/xquery/security-protocol" , "atom-protocol:op-retrieve-descriptor" ) , 3 )
    
    return common-protocol:apply-op( $CONSTANT:OP-RETRIEVE-ACL , $op , $request-path-info , () )

};




declare function security-protocol:op-retrieve-descriptor(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry)? ,
	$request-media-type as xs:string?
) as element(response)
{

    let $descriptor := atomsec:retrieve-descriptor( $request-path-info )

    return security-protocol:response-with-descriptor( $request-path-info , $descriptor )

};




(:
 : TODO doc me 
 :)
declare function security-protocol:do-put(
	$request-path-info as xs:string 
) as element(response)
{

    let $request-data := request:get-data()

    let $op := util:function( QName( "http://purl.org/atombeat/xquery/security-protocol" , "atom-protocol:op-update-descriptor" ) , 3 )
    
    return common-protocol:apply-op( $CONSTANT:OP-UPDATE-ACL , $op , $request-path-info , $request-data )

};




declare function security-protocol:op-update-descriptor(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry) ,
	$request-media-type as xs:string?
) as element(response)
{

    let $descriptor := $request-data/atom:content[@type="application/vnd.atombeat+xml"]/atombeat:security-descriptor[exists(atombeat:acl)]

    return 
        
        if ( empty( $descriptor ) )
        then security-protocol:do-bad-descriptor( $request-path-info )
        
        else

            let $descriptor-updated := atomsec:store-descriptor( $request-path-info , $descriptor )
            let $descriptor := atomsec:retrieve-descriptor( $request-path-info )
            return security-protocol:response-with-descriptor( $request-path-info , $descriptor )

};




declare function security-protocol:response-with-descriptor(
    $request-path-info as xs:string ,
    $descriptor as element(atombeat:security-descriptor)?
) as element(response)
{
    let $entry := atomsec:wrap-with-entry( $request-path-info , $descriptor )

    return
    
        <response>
            <status>{$CONSTANT:STATUS-SUCCESS-OK}</status>
            <headers>
                <header>
                    <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                    <value>{$CONSTANT:MEDIA-TYPE-ATOM}</value>
                </header>
            </headers>
            <body>{$entry}</body>
        </response>
};




declare function security-protocol:do-bad-descriptor(
    $request-path-info as xs:string
) as item()*
{

    let $message := "Request entity must match atom:entry/atom:content[@type='application/vnd.atombeat+xml']/atombeat:security-descriptor/atombeat:acl."
    return common-protocol:do-bad-request( $request-path-info , $message )
    
};


