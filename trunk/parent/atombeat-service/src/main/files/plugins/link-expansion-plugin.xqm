xquery version "1.0";

module namespace link-expansion-plugin = "http://purl.org/atombeat/xquery/link-expansion-plugin";

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
import module namespace plugin-util = "http://purl.org/atombeat/xquery/plugin-util" at "../lib/plugin-util.xqm" ;





declare function link-expansion-plugin:before(
	$operation as xs:string ,
	$request as element(request) ,
	$entity as item()*
) as item()*
{
	
	if ( $entity instance of element(atom:entry) )
	
	then link-expansion-plugin:filter-entry( $entity )
	
	else if ( $entity instance of element(atom:feed) )
	
	then link-expansion-plugin:filter-feed( $entity )
	
	else

		$entity

};




declare function link-expansion-plugin:filter-entry(
    $entry as element(atom:entry)
) as element(atom:entry)
{ 
    <atom:entry>
    { 
        $entry/attribute::* ,
        for $child in $entry/child::* 
        return
            if ( $child instance of element(atom:link) )
            then link-expansion-plugin:unexpand-link( $child )
            else $child
    }
    </atom:entry>
};




declare function link-expansion-plugin:filter-feed(
    $feed as element(atom:feed)
) as element(atom:feed)
{ 
    <atom:feed>
    { 
        $feed/attribute::* ,
        for $child in $feed/child::* 
        return
            if ( $child instance of element(atom:link) )
            then link-expansion-plugin:unexpand-link( $child )
            else if ( $child instance of element(atom:entry) )
            then link-expansion-plugin:filter-entry( $child )
            else $child
    }
    </atom:feed>
};




declare function link-expansion-plugin:unexpand-link(
    $link as element(atom:link)
) as element(atom:link)
{
    <atom:link>
    { 
        $link/attribute::* ,
        $link/child::*[not( . instance of element(ae:inline) )]
    }
    </atom:link>
};




declare function link-expansion-plugin:after(
	$operation as xs:string ,
	$request as element(request) ,
	$response as element(response)
) as element(response)
{
    
    let $body := $response/body
    let $user := $request/user/text()
    let $roles := for $role in $request/roles/role return $role cast as xs:string
    
    let $augmented-body :=
        if ( $body/atom:feed )
        then <body>{link-expansion-plugin:augment-feed( $body/atom:feed , $user , $roles )}</body>
        else if ( $body/atom:entry )
        then <body>{link-expansion-plugin:augment-entry( $body/atom:entry , $user , $roles )}</body>
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




declare function link-expansion-plugin:augment-feed(
    $feed as element(atom:feed) , 
    $user as xs:string? ,
    $roles as xs:string*
) as element(atom:feed)
{

    let $match-feed-rels := tokenize( $feed/atombeat:config-link-expansion/atombeat:config[@context="feed"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
    
    let $match-entry-in-feed-rels := tokenize( $feed/atombeat:config-link-expansion/atombeat:config[@context="entry-in-feed"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
    
    return

        <atom:feed>
        {
            $feed/attribute::* ,
            $feed/child::*[ not( . instance of element(atom:link) ) and not( . instance of element(atom:entry) ) ] ,
            link-expansion-plugin:expand-links( $feed/atom:link , $match-feed-rels , $user , $roles ) ,
            for $entry in $feed/atom:entry
            return link-expansion-plugin:augment-entry( $entry , $match-entry-in-feed-rels , $user , $roles )
        }        
        </atom:feed>

};



declare function link-expansion-plugin:augment-entry(
    $entry as element(atom:entry) , 
    $user as xs:string? ,
    $roles as xs:string*
) as element(atom:entry)
{
    if ( starts-with( $entry/atom:link[@rel="edit"]/@href , $config:edit-link-uri-base ) ) then
        let $collection-path-info := atomdb:collection-path-info( $entry )
        let $feed := atomdb:retrieve-feed-without-entries( $collection-path-info )
        let $match-entry-rels := tokenize( $feed/atombeat:config-link-expansion/atombeat:config[@context="entry"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
            
        return link-expansion-plugin:augment-entry( $entry , $match-entry-rels , $user , $roles )

    else $entry
};




declare function link-expansion-plugin:augment-entry(
    $entry as element(atom:entry) ,
    $match-rels as xs:string* ,
    $user as xs:string? ,
    $roles as xs:string*
) as element(atom:entry)
{
    <atom:entry>
    {
        $entry/attribute::* ,
        $entry/child::*[ not( . instance of element(atom:link) ) ] ,
        link-expansion-plugin:expand-links( $entry/atom:link , $match-rels , $user , $roles )
    }        
    </atom:entry>
};




declare function link-expansion-plugin:expand-links(
    $links as element(atom:link)* ,
    $match-rels as xs:string* ,
    $user as xs:string? ,
    $roles as xs:string*
) as element(atom:link)*
{

    for $link in $links
    return
        
        if ( $match-rels = "*" or $link/@rel = $match-rels ) then
        
            if ( starts-with( $link/@href , $config:self-link-uri-base ) ) then

                link-expansion-plugin:expand-atom-link( $link , $user , $roles )
                
            else if ( starts-with( $link/@href , $config:security-service-url ) ) then
            
                link-expansion-plugin:expand-security-link( $link , $user , $roles )

            else $link
            
        else $link        
  
};




declare function link-expansion-plugin:expand-atom-link(
    $link as element(atom:link) ,
    $user as xs:string? ,
    $roles as xs:string*
) as element(atom:link)
{

    (: deal with cyclic expansion :)
    
    let $log := util:log( "debug" , "expand-atom-link" )
    let $log := util:log( "debug" , $link )
    let $visited := request:get-attribute( 'atombeat.link-expansion-plugin.visited' )
    let $uri := $link/@href cast as xs:string
    let $log := util:log( "debug" , $visited )
    
    return
    
        if ( $uri = $visited ) then 
        
            let $log := util:log( "debug" , "not expanding, already visited" )
            return $link (: do not expand :) 
        
        else
        
            (: TODO what if href uri has query params, need to parse out? :)
            
            let $path-info := substring-after( $link/@href , $config:self-link-uri-base )
            
            let $request :=
                <request>
                    <path-info>{$path-info}</path-info>
                    <method>GET</method>
                    <headers>
                        <header>
                            <name>Accept</name>
                            <value>application/atom+xml</value>
                        </header>
                    </headers>
                    <parameters/>
                    <user>{$user}</user>
                    <roles>
                    {
                        for $role in $roles return <role>{$role}</role>
                    }
                    </roles>
                </request>
                
            let $remember := request:set-attribute( 'atombeat.link-expansion-plugin.visited' , ( $visited , $uri ) )
            let $response := plugin-util:atom-protocol-do-get( $request )
            
            return
                
                if (
                    $response/status = 200 
                    and $response/body/@type='xml'        
                ) then
                    <atom:link>
                    {
                        $link/attribute::* ,
                        $link/child::* ,
                        <ae:inline>{$response/body/*}</ae:inline>
                    }
                    </atom:link>
                else $link

};





declare function link-expansion-plugin:expand-security-link(
    $link as element(atom:link) ,
    $user as xs:string? ,
    $roles as xs:string*
) as element(atom:link)
{

    (: deal with cyclic expansion :)
    
    let $log := util:log( "debug" , "expand-security-link" )
    let $log := util:log( "debug" , $link )
    let $visited := request:get-attribute( 'atombeat.link-expansion-plugin.visited' )
    let $uri := $link/@href cast as xs:string
    let $log := util:log( "debug" , $visited )
    
    return
    
        if ( $uri = $visited ) then 
        
            let $log := util:log( "debug" , "not expanding, already visited" )
            return $link (: do not expand :) 
        
        else
        
            (: TODO what if href uri has query params, need to parse out? :)
            
            let $path-info := substring-after( $link/@href , $config:security-service-url )
            
            let $request :=
                <request>
                    <path-info>{$path-info}</path-info>
                    <method>GET</method>
                    <headers>
                        <header>
                            <name>Accept</name>
                            <value>application/atom+xml</value>
                        </header>
                    </headers>
                    <parameters/>
                    <user>{$user}</user>
                    <roles>
                    {
                        for $role in $roles return <role>{$role}</role>
                    }
                    </roles>
                </request>
                
            let $remember := request:set-attribute( 'atombeat.link-expansion-plugin.visited' , ( $visited , $uri ) )
            let $response := plugin-util:security-protocol-do-get( $request )
            let $log := util:log( "debug" , $response )
            
            return
                
                if (
                    $response/status = 200 
                    and $response/body/@type='xml'        
                ) then
                    <atom:link>
                    {
                        $link/attribute::* ,
                        $link/child::* ,
                        <ae:inline>{$response/body/*}</ae:inline>
                    }
                    </atom:link>
                else $link

};





