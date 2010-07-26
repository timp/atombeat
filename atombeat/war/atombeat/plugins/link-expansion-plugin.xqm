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








declare function link-expansion-plugin:after(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{
    
    let $response-data := $response/body/*
    
    let $augmented-response-data :=
        if ( $response-data instance of element(atom:feed) )
        then link-expansion-plugin:augment-feed( $response-data )
        else if ( $response-data instance of element(atom:entry) )
        then link-expansion-plugin:augment-entry( $response-data )
        else $response-data
        
    return
    
        <response>
        {
            $response/status ,
            $response/headers
        }
            <body>{$augmented-response-data}</body>
        </response>
            
}; 




declare function link-expansion-plugin:augment-feed(
    $feed as element(atom:feed)
) as element(atom:feed)
{

    let $match-feed-rels := tokenize( $feed/atombeat:config-link-expansion/atombeat:config[@context="feed"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
    
    let $match-entry-in-feed-rels := tokenize( $feed/atombeat:config-link-expansion/atombeat:config[@context="entry-in-feed"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
    
    return

        <atom:feed>
        {
            $feed/attribute::* ,
            $feed/child::*[ not( . instance of element(atom:link) ) and not( . instance of element(atom:entry) ) ] ,
            link-expansion-plugin:expand-links( $feed/atom:link , $match-feed-rels ) ,
            for $entry in $feed/atom:entry
            return link-expansion-plugin:augment-entry( $entry , $match-entry-in-feed-rels )
        }        
        </atom:feed>

};



declare function link-expansion-plugin:augment-entry(
    $entry as element(atom:entry)
) as element(atom:entry)
{
    if ( starts-with( $entry/atom:link[@rel="edit"]/@href , $config:content-service-url ) ) then
        let $collection-path-info := atomdb:collection-path-info( $entry )
        let $feed := atomdb:retrieve-feed-without-entries( $collection-path-info )
        let $match-entry-rels := tokenize( $feed/atombeat:config-link-expansion/atombeat:config[@context="entry"]/atombeat:param[@name="match-rels"]/@value , "\s+" )
            
        return link-expansion-plugin:augment-entry( $entry , $match-entry-rels )

    else $entry
};




declare function link-expansion-plugin:augment-entry(
    $entry as element(atom:entry) ,
    $match-rels as xs:string*
) as element(atom:entry)
{
    <atom:entry>
    {
        $entry/attribute::* ,
        $entry/child::*[ not( . instance of element(atom:link) ) ] ,
        link-expansion-plugin:expand-links( $entry/atom:link , $match-rels )
    }        
    </atom:entry>
};




declare function link-expansion-plugin:expand-links(
    $links as element(atom:link)* ,
    $match-rels as xs:string*
) as element(atom:link)*
{

    for $link in $links
    return
        
        if ( $match-rels = "*" or $link/@rel = $match-rels ) then
        
            if ( starts-with( $link/@href , $config:content-service-url ) ) then

                let $path-info := substring-after( $link/@href , $config:content-service-url )
                
                return 
                
                    if ( 
                        atomdb:member-available( $path-info ) 
                        and atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-MEMBER , $path-info , () )
                    ) then
                    
                        <atom:link>
                        {
                            $link/attribute::* ,
                            $link/child::* ,
                            <ae:inline>{atomdb:retrieve-member( $path-info )}</ae:inline>
                        }
                        </atom:link>
                        
                    else if ( 
                        atomdb:collection-available( $path-info ) 
                        and atomsec:is-allowed( $CONSTANT:OP-LIST-COLLECTION , $path-info , () )
                    ) then

                        <atom:link>
                        {
                            $link/attribute::* ,
                            $link/child::* ,
                            <ae:inline>{atomsec:filter-feed(atomdb:retrieve-feed( $path-info ))}</ae:inline>
                        }
                        </atom:link>

                    else $link

            else if ( starts-with( $link/@href , $config:security-service-url ) ) then

                let $path-info := substring-after( $link/@href , $config:security-service-url )
                
                return
                
                    if ( 
                        atomdb:member-available( $path-info ) 
                        and atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-MEMBER-ACL , $path-info , () )
                    ) then
                    
                        <atom:link>
                        {
                            $link/attribute::* ,
                            $link/child::* ,
                            <ae:inline>
                            { atomsec:wrap-with-entry( $path-info , atomsec:retrieve-descriptor( $path-info ) ) }
                            </ae:inline>
                        }
                        </atom:link>
                        
                    else if ( 
                        atomdb:media-resource-available( $path-info ) 
                        and atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-MEDIA-ACL , $path-info , () )
                    ) then
                    
                        <atom:link>
                        {
                            $link/attribute::* ,
                            $link/child::* ,
                            <ae:inline>
                            { atomsec:wrap-with-entry( $path-info , atomsec:retrieve-descriptor( $path-info ) ) }
                            </ae:inline>
                        }
                        </atom:link>
                        
                    else if ( 
                        atomdb:collection-available( $path-info ) 
                        and atomsec:is-allowed( $CONSTANT:OP-RETRIEVE-COLLECTION-ACL , $path-info , () )
                    ) then

                        <atom:link>
                        {
                            $link/attribute::* ,
                            $link/child::* ,
                            <ae:inline>
                            { atomsec:wrap-with-entry( $path-info , atomsec:retrieve-descriptor( $path-info ) ) }
                            </ae:inline>
                        }
                        </atom:link>

                    else $link
                    
            else $link
            
        else $link        
  

};







