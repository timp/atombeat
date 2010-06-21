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
	
		if ( $request-method = $CONSTANT:METHOD-GET )
		
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
    
    if ( $request-path-info = "/" )
    
    then security-protocol:do-get-workspace-descriptor()
    
    else if ( atomdb:collection-available( $request-path-info ) )
    
    then security-protocol:do-get-collection-descriptor( $request-path-info )
    
    else if ( atomdb:member-available( $request-path-info ) )
    
    then security-protocol:do-get-member-descriptor( $request-path-info )
    
    else if ( atomdb:media-resource-available( $request-path-info ) )
    
    then security-protocol:do-get-media-descriptor( $request-path-info )
    
    else common-protocol:do-not-found( $request-path-info )
	
};




declare function security-protocol:do-get-workspace-descriptor() as element(response)
{
    (: 
     : We will only allow retrieval of workspace ACL if user is allowed
     : to update the workspace ACL.
     :)
     
    let $allowed := security-protocol:is-retrieve-acl-allowed( "/" )
    
    return
    
        if ( not( $allowed ) )
        
        then common-protocol:do-forbidden( "/" ) (: TODO factor these utility methods out :)
        
        else
        
            let $descriptor := atomsec:retrieve-workspace-descriptor()
            return security-protocol:send-descriptor( "/" , $descriptor )

};





declare function security-protocol:do-get-collection-descriptor(
    $request-path-info as xs:string
) as element(response)
{

    let $allowed := security-protocol:is-retrieve-acl-allowed( $request-path-info )
    
    return
    
        if ( not( $allowed ) )
        
        then common-protocol:do-forbidden( $request-path-info ) (: TODO factor these utility methods out :)
        
        else
        
            let $descriptor := atomsec:retrieve-collection-descriptor( $request-path-info )
            return security-protocol:send-descriptor( $request-path-info , $descriptor )

};




declare function security-protocol:do-get-member-descriptor(
    $request-path-info as xs:string
) as element(response)
{

    let $allowed := security-protocol:is-retrieve-acl-allowed( $request-path-info )
    
    return
    
        if ( not( $allowed ) )
        
        then common-protocol:do-forbidden( $request-path-info ) (: TODO factor these utility methods out :)
        
        else
        
            let $descriptor := atomsec:retrieve-resource-descriptor( $request-path-info )
            return security-protocol:send-descriptor( $request-path-info , $descriptor )

};




declare function security-protocol:do-get-media-descriptor(
    $request-path-info as xs:string
) as element(response)
{
     
    let $allowed := security-protocol:is-retrieve-acl-allowed( $request-path-info )
    
    return
    
        if ( not( $allowed ) )
        
        then common-protocol:do-forbidden( $request-path-info ) (: TODO factor these utility methods out :)
        
        else
        
            let $descriptor := atomsec:retrieve-resource-descriptor( $request-path-info )
            return security-protocol:send-descriptor( $request-path-info , $descriptor )

};




(:
 : TODO doc me 
 :)
declare function security-protocol:do-put(
	$request-path-info as xs:string 
) as element(response)
{
    
    if ( $request-path-info = "/" )
    
    then security-protocol:do-put-workspace-descriptor()
    
    else if ( atomdb:collection-available( $request-path-info ) )
    
    then security-protocol:do-put-collection-descriptor( $request-path-info )
    
    else if ( atomdb:member-available( $request-path-info ) )
    
    then security-protocol:do-put-member-descriptor( $request-path-info )
    
    else if ( atomdb:media-resource-available( $request-path-info ) )
    
    then security-protocol:do-put-media-descriptor( $request-path-info )
    
    else common-protocol:do-not-found( $request-path-info )
	
};






declare function security-protocol:do-put-workspace-descriptor() as element(response)
{
    
    let $allowed := security-protocol:is-update-acl-allowed( "/" )
    
    return
    
        if ( not( $allowed ) )
        
        then common-protocol:do-forbidden( "/" ) (: TODO factor these utility methods out :)
        
        else
        
            let $descriptor := security-protocol:get-descriptor-from-request-data()

            return 
                
                if ( empty( $descriptor ) )
                then security-protocol:do-bad-descriptor( "/" )
                
                else

                    let $descriptor-updated := atomsec:store-workspace-descriptor( $descriptor )
                    let $descriptor := atomsec:retrieve-workspace-descriptor()
                    return security-protocol:send-descriptor( "/" , $descriptor )

};

 



declare function security-protocol:do-put-collection-descriptor(
    $request-path-info as xs:string
) as element(response)
{
    (: 
     : We will only allow retrieval of collection ACL if user is allowed
     : to update the collection ACL.
     :)
     
    let $allowed := security-protocol:is-update-acl-allowed( $request-path-info )
    
    return
    
        if ( not( $allowed ) )
        
        then common-protocol:do-forbidden( $request-path-info ) (: TODO factor these utility methods out :)
        
        else
        
            let $descriptor := security-protocol:get-descriptor-from-request-data()

            return 
                
                if ( empty( $descriptor ) )
                then security-protocol:do-bad-descriptor( $request-path-info )
                
                else

                    let $descriptor-updated := atomsec:store-collection-descriptor( $request-path-info , $descriptor )
                    let $descriptor := atomsec:retrieve-collection-descriptor( $request-path-info )
                    return security-protocol:send-descriptor( $request-path-info , $descriptor )

};




declare function security-protocol:do-put-member-descriptor(
    $request-path-info as xs:string
) as element(response)
{
    (: 
     : We will only allow retrieval of member ACL if user is allowed
     : to update the member ACL.
     :)
     
    let $allowed := security-protocol:is-update-acl-allowed( $request-path-info )
    
    return
    
        if ( not( $allowed ) )
        
        then common-protocol:do-forbidden( $request-path-info ) (: TODO factor these utility methods out :)
        
        else
        
            let $descriptor := security-protocol:get-descriptor-from-request-data()

            return 
                 
                if ( empty( $descriptor ) )
                then security-protocol:do-bad-descriptor( $request-path-info )
                
                else

                    let $descriptor-updated := atomsec:store-resource-descriptor( $request-path-info , $descriptor )
                    let $descriptor := atomsec:retrieve-resource-descriptor( $request-path-info )
                    return security-protocol:send-descriptor( $request-path-info , $descriptor )

};



declare function security-protocol:do-put-media-descriptor(
    $request-path-info as xs:string
) as element(response)
{
    (: 
     : We will only allow retrieval of media ACL if user is allowed
     : to update the media ACL.
     :)
     
    let $allowed := security-protocol:is-update-acl-allowed( $request-path-info )
    
    return
    
        if ( not( $allowed ) )
        
        then common-protocol:do-forbidden( $request-path-info ) (: TODO factor these utility methods out :)
        
        else
        
            let $descriptor := security-protocol:get-descriptor-from-request-data()

            return 
                 
                if ( empty( $descriptor ) )
                then security-protocol:do-bad-descriptor( $request-path-info )
                
                else

                    let $descriptor-updated := atomsec:store-resource-descriptor( $request-path-info , $descriptor )
                    let $descriptor := atomsec:retrieve-resource-descriptor( $request-path-info )
                    return security-protocol:send-descriptor( $request-path-info , $descriptor )

};



declare function security-protocol:is-update-acl-allowed(
    $request-path-info as xs:string
) as xs:boolean 
{

    let $allowed as xs:boolean := atomsec:is-allowed( $CONSTANT:OP-UPDATE-ACL , $request-path-info , () )
    return $allowed

};




declare function security-protocol:is-retrieve-acl-allowed(
    $request-path-info as xs:string
) as xs:boolean 
{

    let $allowed as xs:boolean := atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-ACL , $request-path-info , () )
    return $allowed

};




declare function security-protocol:get-descriptor-from-request-data(
) as element(atombeat:security-descriptor)?
{
    let $request-data := request:get-data()
    let $descriptor := $request-data/atom:content[@type="application/vnd.atombeat+xml"]/atombeat:security-descriptor[exists(atombeat:acl)]
    return $descriptor
};




declare function security-protocol:send-descriptor(
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

