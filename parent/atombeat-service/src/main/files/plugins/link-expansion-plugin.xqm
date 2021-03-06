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
) as item()*
{
    
    let $body := $response/body
    
    let $augmented-body :=
        if ( exists( $body/atom:feed ) )
        then <body type='xml'>{link-expansion-plugin:augment-feed( $body/atom:feed , $request )}</body>
        else if ( exists( $body/atom:entry ) )
        then <body type='xml'>{link-expansion-plugin:augment-entry( $body/atom:entry , $request )}</body>
        else $body
        
    let $modified-response :=
        <response>
        {
            $response/status ,
            $response/headers ,
            $augmented-body
        }
        </response>

    return $modified-response
    
}; 




declare function link-expansion-plugin:augment-feed(
    $feed as element(atom:feed) , 
	$request as element(request) 
) as element(atom:feed)
{

    let $match-feed-rels := tokenize( $feed/atombeat:config-link-expansion/atombeat:config[@context="feed"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
    let $match-entry-in-feed-rels := tokenize( $feed/atombeat:config-link-expansion/atombeat:config[@context="entry-in-feed"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
    
    return

        <atom:feed>
        {
            $feed/attribute::* ,
            $feed/child::*[ not( . instance of element(atom:link) ) and not( . instance of element(atom:entry) ) ] ,
            link-expansion-plugin:expand-links( $feed/atom:link , $match-feed-rels , $request ) ,
            for $entry in $feed/atom:entry
            return link-expansion-plugin:augment-entry( $entry , $match-entry-in-feed-rels , $request )
        }        
        </atom:feed>

};



declare function link-expansion-plugin:augment-entry(
    $entry as element(atom:entry) , 
	$request as element(request) 
) as element(atom:entry)
{
    if ( starts-with( $entry/atom:link[@rel="edit"]/@href , $config:edit-link-uri-base ) ) then
        let $collection-path-info := let $entry-path-info := substring-after($entry/atom:link[@rel='edit']/@href/string(), $config:edit-link-uri-base) return text:groups($entry-path-info, "^(.+)/[^/]+$")[2]
        let $feed := atomdb:retrieve-feed-without-entries( $collection-path-info )
        let $match-entry-rels := tokenize( $feed/atombeat:config-link-expansion/atombeat:config[@context="entry"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
        return link-expansion-plugin:augment-entry( $entry , $match-entry-rels , $request )

    else $entry
};




declare function link-expansion-plugin:augment-entry(
    $entry as element(atom:entry) ,
    $match-rels as xs:string* ,
	$request as element(request) 
) as element(atom:entry)
{
    <atom:entry>
    {
        $entry/attribute::* ,
        $entry/child::*[ not( . instance of element(atom:link) ) ] ,
        link-expansion-plugin:expand-links( $entry/atom:link , $match-rels , $request )
    }        
    </atom:entry>
};




declare function link-expansion-plugin:expand-links(
    $links as element(atom:link)* ,
    $match-rels as xs:string* ,
	$request as element(request) 
) as element(atom:link)*
{

    for $link in $links
    return
        
        if ( $match-rels = "*" or $link/@rel = $match-rels ) then
        
            if ( starts-with( $link/@href , $config:self-link-uri-base ) ) then
                link-expansion-plugin:expand-atom-link($link, $request, ())
                
            else if ( starts-with( $link/@href , $config:security-service-url ) ) then
                link-expansion-plugin:expand-security-link( $link , $request )

            else 
                (: try treating link @href as a member ID :)
                let $uri := atomdb:lookup-member-by-id($link/@href/string())
                return
                    if (exists($uri) and starts-with($uri, $config:self-link-uri-base)) then
                        link-expansion-plugin:expand-atom-link($link, $request, $uri)
                    else
                        $link
            
        else $link        
  
};




declare function link-expansion-plugin:expand-atom-link(
    $link as element(atom:link) ,
	$request as element(request) ,
	$override-uri as xs:string?
) as element(atom:link)
{

    let $depth-value := $request/attributes/attribute[name eq 'atombeat.link-expansion-plugin.depth']/value 
    let $depth := if (exists($depth-value)) then $depth-value cast as xs:integer else 0
    
    let $path := $request/attributes/attribute[name eq 'atombeat.link-expansion-plugin.path']/value/path
    
    (: deal with cyclic expansion :)
    let $visited := $request/attributes/attribute[name eq 'atombeat.link-expansion-plugin.visited']/value/visited
    let $uri := 
        if (exists($override-uri)) then $override-uri else $link/@href/string()
    
    return
    
        if ( $uri = $visited ) then $link (: do not expand :) 
        
        else if ( $depth gt 0 ) then $link (: keep conventional expansion to 1 deep :)
        
        else
        
            (: TODO what if href uri has query params, need to parse out? :)
            
            let $new-visited := (
                $visited ,
                <visited>{$uri}</visited>
            )
            let $new-depth := $depth + 1
            let $new-path := (
                $path ,
                <path>{$link/@rel/string()}</path>
            )
            let $path-info-with-query-string := substring-after( $uri , $config:self-link-uri-base )
            let $path-info := if ( contains( $path-info-with-query-string , '?' ) ) then substring-before( $path-info-with-query-string , '?' ) else $path-info-with-query-string
            let $query-string := substring-after ( $path-info-with-query-string , '?' )
            let $parameters :=
                for $component in tokenize( $query-string , '&amp;' )
                return
                    <parameter>
                        <name>{if ( contains( $component, '=') ) then substring-before( $component, '=' ) else $component}</name>
                        <value>{substring-after( $component, '=' )}</value>
                    </parameter>
            
            let $internal-request :=
                <request>
                    <path-info>{$path-info}</path-info>
                    <method>GET</method>
                    <headers>
                        <header>
                            <name>Accept</name>
                            <value>application/atom+xml</value>
                        </header>
                    </headers>
                    <parameters>{$parameters}</parameters>
                    <attributes>
                        <attribute>
                            <name>atombeat.link-expansion-plugin.visited</name>
                            <value>{$new-visited}</value>
                        </attribute>
                        <attribute>
                            <name>atombeat.link-expansion-plugin.depth</name>
                            <value>{$new-depth}</value>
                        </attribute>
                        <attribute>
                            <name>atombeat.link-expansion-plugin.path</name>
                            <value>{$new-path}</value>
                        </attribute>
                    </attributes>
                    {
                        $request/user ,
                        $request/roles
                    }
                </request>
                
            let $response := plugin-util:atom-protocol-do-get( $internal-request )
            
            return
                
                if (
                    $response/status = 200 
                    and exists( $response/body/element() )        
                ) then
                    <atom:link>
                    {
                        $link/attribute::* ,
                        $link/child::* ,
                        <ae:inline>{$response/body/element()}</ae:inline>
                    }
                    </atom:link>
                else $link

};





declare function link-expansion-plugin:expand-security-link(
    $link as element(atom:link) ,
	$request as element(request) 
) as element(atom:link)
{

    let $depth-value := $request/attributes/attribute[name eq 'atombeat.link-expansion-plugin.depth']/value 
    let $depth := if (exists($depth-value)) then $depth-value cast as xs:integer else 0
    
    let $path := $request/attributes/attribute[name eq 'atombeat.link-expansion-plugin.path']/value/path
    
    (: deal with cyclic expansion :)
    let $visited := $request/attributes/attribute[name eq 'atombeat.link-expansion-plugin.visited']/value/visited
    let $uri := $link/@href cast as xs:string
    
    return
    
        if ( $uri = $visited ) then $link (: do not expand :) 

        else if ( $depth gt 0 ) then $link (: keep conventional expansion to 1 deep :)

        else
        
            let $new-visited := (
                $visited ,
                <visited>{$uri}</visited>
            )
            let $new-depth := $depth + 1
            let $new-path := (
                $path ,
                <path>{$link/@rel/string()}</path>
            )
            let $path-info-with-query-string := substring-after( $uri , $config:security-service-url )
            let $path-info := if ( contains( $path-info-with-query-string , '?' ) ) then substring-before( $path-info-with-query-string , '?' ) else $path-info-with-query-string
            let $query-string := substring-after ( $path-info-with-query-string , '?' )
            let $parameters :=
                for $component in tokenize( $query-string , '&amp;' )
                return
                    <parameter>
                        <name>{if ( contains( $component, '=') ) then substring-before( $component, '=' ) else $component}</name>
                        <value>{substring-after( $component, '=' )}</value>
                    </parameter>
            
            let $internal-request :=
                <request>
                    <path-info>{$path-info}</path-info>
                    <method>GET</method>
                    <headers>
                        <header>
                            <name>Accept</name>
                            <value>application/atom+xml</value>
                        </header>
                    </headers>
                    <parameters>{$parameters}</parameters>
                    <attributes>
                        <attribute>
                            <name>atombeat.link-expansion-plugin.visited</name>
                            <value>{$new-visited}</value>
                        </attribute>
                        <attribute>
                            <name>atombeat.link-expansion-plugin.depth</name>
                            <value>{$new-depth}</value>
                        </attribute>
                        <attribute>
                            <name>atombeat.link-expansion-plugin.path</name>
                            <value>{$new-path}</value>
                        </attribute>
                    </attributes>
                    {
                        $request/user ,
                        $request/roles
                    }
                </request>
                
            let $response := plugin-util:security-protocol-do-get( $internal-request )
            
            return
                
                if (
                    $response/status = 200 
                    and exists( $response/body/element() )        
                ) then
                    <atom:link>
                    {
                        $link/attribute::* ,
                        $link/child::* ,
                        <ae:inline>{$response/body/element()}</ae:inline>
                    }
                    </atom:link>
                else $link

};





