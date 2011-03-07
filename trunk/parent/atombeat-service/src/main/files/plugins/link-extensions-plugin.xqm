xquery version "1.0";

module namespace link-extensions-plugin = "http://purl.org/atombeat/xquery/link-extensions-plugin";

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





declare function link-extensions-plugin:before(
	$operation as xs:string ,
	$request as element(request) ,
	$entity as item()*
) as item()*
{
	
	if ( $entity instance of element(atom:entry) )
	
	then link-extensions-plugin:filter-entry( $entity )
	
	else if ( $entity instance of element(atom:feed) )
	
	then link-extensions-plugin:filter-feed( $entity )
	
	else

		$entity

};





declare function link-extensions-plugin:filter-entry(
    $entry as element(atom:entry)
) as element(atom:entry)
{ 
    <atom:entry>
    { 
        $entry/attribute::* ,
        for $child in $entry/child::* 
        return
            if ( $child instance of element(atom:link) )
            then link-extensions-plugin:undecorate-link( $child )
            else $child
    }
    </atom:entry>
};




declare function link-extensions-plugin:filter-feed(
    $feed as element(atom:feed)
) as element(atom:feed)
{ 
    <atom:feed>
    { 
        $feed/attribute::* ,
        for $child in $feed/child::* 
        return
            if ( $child instance of element(atom:link) )
            then link-extensions-plugin:undecorate-link( $child )
            else if ( $child instance of element(atom:entry) )
            then link-extensions-plugin:filter-entry( $child )
            else $child
    }
    </atom:feed>
};
 
 
 
 
declare function link-extensions-plugin:undecorate-link(
    $link as element(atom:link)
) as element(atom:link)
{
    <atom:link>
    { 
        $link/attribute::*[
            not( . instance of attribute(atombeat:allow) )
            and not( . instance of attribute(atombeat:count) )
        ] ,
        $link/child::*
    }
    </atom:link>
};




declare function link-extensions-plugin:after(
	$operation as xs:string ,
	$request as element(request) ,
	$response as element(response)
) as item()*
{
    
    let $body := $response/body
    let $user := $request/user/text()
    let $roles := for $role in $request/roles/role return $role cast as xs:string
    
    let $augmented-body :=
        if ( $body/atom:feed )
        then <body type='xml'>{link-extensions-plugin:augment-feed( $body/atom:feed , $user , $roles )}</body>
        else if ( $body/atom:entry )
        then <body type='xml'>{link-extensions-plugin:augment-entry( $body/atom:entry , $user , $roles )}</body>
        else $body
        
    return
    
        <response>
        {
            $response/status ,
            $response/headers ,
            $augmented-body
        }
        </response>
            
}; 




declare function link-extensions-plugin:augment-feed(
    $feed as element(atom:feed) ,
    $user as xs:string? ,
    $roles as xs:string*
) as element(atom:feed)
{

    let $match-feed-rels-allow := tokenize( $feed/atombeat:config-link-extensions/atombeat:extension-attribute[@name="allow" and @namespace="http://purl.org/atombeat/xmlns"]/atombeat:config[@context="feed"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
    
    let $match-entry-in-feed-rels-allow := tokenize( $feed/atombeat:config-link-extensions/atombeat:extension-attribute[@name="allow" and @namespace="http://purl.org/atombeat/xmlns"]/atombeat:config[@context="entry-in-feed"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
    
    let $match-feed-rels-count := tokenize( $feed/atombeat:config-link-extensions/atombeat:extension-attribute[@name="count" and @namespace="http://purl.org/atombeat/xmlns"]/atombeat:config[@context="feed"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
    
    let $match-entry-in-feed-rels-count := tokenize( $feed/atombeat:config-link-extensions/atombeat:extension-attribute[@name="count" and @namespace="http://purl.org/atombeat/xmlns"]/atombeat:config[@context="entry-in-feed"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
    
    return

        <atom:feed>
        {
            $feed/attribute::* ,
            $feed/child::*[ not( . instance of element(atom:link) ) and not( . instance of element(atom:entry) ) ] ,
            link-extensions-plugin:decorate-links( $feed/atom:link , $match-feed-rels-allow , $match-feed-rels-count , $user , $roles ) ,
            for $entry in $feed/atom:entry
            return link-extensions-plugin:augment-entry( $entry , $match-entry-in-feed-rels-allow , $match-entry-in-feed-rels-count , $user , $roles )
        }        
        </atom:feed>

};



declare function link-extensions-plugin:augment-entry(
    $entry as element(atom:entry) ,
    $user as xs:string? ,
    $roles as xs:string*
) as element(atom:entry)
{
    if ( starts-with( $entry/atom:link[@rel="edit"]/@href , $config:edit-link-uri-base ) ) then
        let $collection-path-info := atomdb:collection-path-info( $entry )
        let $feed := atomdb:retrieve-feed-without-entries( $collection-path-info )
        let $match-entry-rels-allow := tokenize( $feed/atombeat:config-link-extensions/atombeat:extension-attribute[@name="allow" and @namespace="http://purl.org/atombeat/xmlns"]/atombeat:config[@context="entry"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
        let $match-entry-rels-count := tokenize( $feed/atombeat:config-link-extensions/atombeat:extension-attribute[@name="count" and @namespace="http://purl.org/atombeat/xmlns"]/atombeat:config[@context="entry"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
            
        return link-extensions-plugin:augment-entry( $entry , $match-entry-rels-allow , $match-entry-rels-count , $user , $roles )

    else $entry
};




declare function link-extensions-plugin:augment-entry(
    $entry as element(atom:entry) ,
    $match-rels-allow as xs:string* ,
    $match-rels-count as xs:string* ,
    $user as xs:string? ,
    $roles as xs:string*
) as element(atom:entry)
{
    <atom:entry>
    {
        $entry/attribute::* ,
        $entry/child::*[ not( . instance of element(atom:link) ) ] ,
        link-extensions-plugin:decorate-links( $entry/atom:link , $match-rels-allow , $match-rels-count , $user , $roles )
    }        
    </atom:entry>
};




declare function link-extensions-plugin:decorate-links(
    $links as element(atom:link)* ,
    $match-rels-allow as xs:string* ,
    $match-rels-count as xs:string* ,
    $user as xs:string? ,
    $roles as xs:string*
) as element(atom:link)*
{

    let $links-with-allow :=
    
        for $link in $links return
            
            if ( $match-rels-allow = "*" or $link/@rel = $match-rels-allow ) then
            
                if ( starts-with( $link/@href , $config:self-link-uri-base ) ) then
    
                    let $path-info := substring-after( $link/@href , $config:self-link-uri-base )
                    
                    return 
                    
                        if ( atomdb:member-available( $path-info ) ) then
    
                            let $can-get := atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-MEMBER , $path-info , () , $user , $roles )
                            let $can-put := atomsec:is-allowed( $CONSTANT:OP-UPDATE-MEMBER , $path-info , () , $user , $roles )
                            let $can-delete := atomsec:is-allowed( $CONSTANT:OP-DELETE-MEMBER , $path-info , () , $user , $roles )
                            let $allow := string-join( (
                                if ( $can-get ) then "GET" else () ,
                                if ( $can-put ) then "PUT" else () ,
                                if ( $can-delete ) then "DELETE" else ()
                            ) , ", " )
                            return <atom:link atombeat:allow="{$allow}">{$link/attribute::* , $link/child::*}</atom:link>
                            
                        else if ( atomdb:media-resource-available( $path-info ) ) then
    
                            let $can-get := atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-MEDIA , $path-info , () , $user , $roles )
                            let $can-put := atomsec:is-allowed( $CONSTANT:OP-UPDATE-MEDIA , $path-info , () , $user , $roles )
                            let $can-delete := atomsec:is-allowed( $CONSTANT:OP-DELETE-MEDIA , $path-info , () , $user , $roles )
                            let $allow := string-join( (
                                if ( $can-get ) then "GET" else () ,
                                if ( $can-put ) then "PUT" else () ,
                                if ( $can-delete ) then "DELETE" else ()
                            ) , ", " )
                            return <atom:link atombeat:allow="{$allow}">{$link/attribute::* , $link/child::*}</atom:link>
                            
                        else if ( atomdb:collection-available( $path-info ) ) then
    
                            let $can-get := atomsec:is-allowed( $CONSTANT:OP-LIST-COLLECTION , $path-info , () , $user , $roles )
                            let $can-put := atomsec:is-allowed( $CONSTANT:OP-UPDATE-COLLECTION , $path-info , () , $user , $roles )
                            let $can-post := (
                                atomsec:is-allowed( $CONSTANT:OP-CREATE-MEMBER , $path-info , () , $user , $roles )
                                or atomsec:is-allowed( $CONSTANT:OP-CREATE-MEDIA , $path-info , () , $user , $roles )
                                or atomsec:is-allowed( $CONSTANT:OP-MULTI-CREATE , $path-info , () , $user , $roles )
                            )
                            let $allow := string-join( (
                                if ( $can-get ) then "GET" else () ,
                                if ( $can-put ) then "PUT" else () ,
                                if ( $can-post ) then "POST" else ()
                            ) , ", " )
                            return <atom:link atombeat:allow="{$allow}">{$link/attribute::* , $link/child::*}</atom:link>
    
                        else $link
    
                else if ( starts-with( $link/@href , $config:security-service-url ) ) then
    
                    let $path-info := substring-after( $link/@href , $config:security-service-url )
                    
                    return 
                    
                        if ( atomdb:member-available( $path-info ) ) then
    
                            let $can-get := atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-MEMBER-ACL , $path-info , () , $user , $roles )
                            let $can-put := atomsec:is-allowed( $CONSTANT:OP-UPDATE-MEMBER-ACL , $path-info , () , $user , $roles )
                            let $allow := string-join( (
                                if ( $can-get ) then "GET" else () ,
                                if ( $can-put ) then "PUT" else ()
                            ) , ", " )
                            return <atom:link atombeat:allow="{$allow}">{$link/attribute::* , $link/child::*}</atom:link>
                            
                        else if ( atomdb:media-resource-available( $path-info ) ) then
    
                            let $can-get := atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-MEDIA-ACL , $path-info , () , $user , $roles )
                            let $can-put := atomsec:is-allowed( $CONSTANT:OP-UPDATE-MEDIA-ACL , $path-info , () , $user , $roles )
                            let $allow := string-join( (
                                if ( $can-get ) then "GET" else () ,
                                if ( $can-put ) then "PUT" else ()
                            ) , ", " )
                            return <atom:link atombeat:allow="{$allow}">{$link/attribute::* , $link/child::*}</atom:link>
                            
                        else if ( atomdb:collection-available( $path-info ) ) then
    
                            let $can-get := atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-COLLECTION-ACL , $path-info , () , $user , $roles )
                            let $can-put := atomsec:is-allowed( $CONSTANT:OP-UPDATE-COLLECTION-ACL , $path-info , () , $user , $roles )
                            let $allow := string-join( (
                                if ( $can-get ) then "GET" else () ,
                                if ( $can-put ) then "PUT" else ()
                            ) , ", " )
                            return <atom:link atombeat:allow="{$allow}">{$link/attribute::* , $link/child::*}</atom:link>
    
                        else $link
                        
                else if ( $link/@rel = "history" ) then
    
                    let $path-info := substring-after( $link/@href , $config:history-service-url )
                    
                    return 
                    
                        if ( atomdb:member-available( $path-info ) ) then
    
                            let $can-get := atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-HISTORY , $path-info , () , $user , $roles )
                            let $allow := 
                                if ( $can-get ) then "GET" else () 
                            return <atom:link atombeat:allow="{$allow}">{$link/attribute::* , $link/child::*}</atom:link>
                            
                        else $link
                        
                else $link
                
            else $link        
    
    let $links-with-count :=
    
        for $link in $links-with-allow return

            if ( 
                ( $match-rels-count = "*" or $link/@rel = $match-rels-count )
                and starts-with( $link/@href , $config:self-link-uri-base )
            ) then
            
                let $path-info := substring-after( $link/@href , $config:self-link-uri-base )
                
                return 
                
                    if ( atomdb:collection-available( $path-info ) ) then

                        let $count := atomdb:count-members( $path-info )
                        return <atom:link atombeat:count="{$count}">{$link/attribute::* , $link/child::*}</atom:link>

                    else $link
                    
                else $link
                        
    return $links-with-count
            
};







