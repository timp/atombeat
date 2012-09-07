xquery version "1.0";

module namespace tombstone-db = "http://purl.org/atombeat/xquery/tombstone-db";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;
declare namespace at = "http://purl.org/atompub/tombstones/1.0";

import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace xmldb = "http://exist-db.org/xquery/xmldb" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace atombeat-util = "http://purl.org/atombeat/xquery/atombeat-util" at "java:org.atombeat.xquery.functions.util.AtomBeatUtilModule";

import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;
import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;






declare function tombstone-db:tombstone-available(
	$request-path-info as xs:string
) as xs:boolean
{


	(:
	 : Map the request path info, e.g., "/foo/bar", to a database resource path,
	 : e.g., "/db/foo/bar".
	 :)
	 
	let $member-db-path := concat( atomdb:request-path-info-to-db-path( $request-path-info ) , ".atom" )
		
	return ( not( util:binary-doc-available( $member-db-path ) ) and exists( doc( $member-db-path )/at:deleted-entry ) )
	
};




declare function tombstone-db:retrieve-tombstone(
    $request-path-info as xs:string
) as element(at:deleted-entry)?
{

    if ( not( tombstone-db:tombstone-available( $request-path-info ) ) )
    
    then () 
    
    else 
    
        let $member-db-path := concat( atomdb:request-path-info-to-db-path( $request-path-info ) , ".atom" )
            
        return doc( $member-db-path )/at:deleted-entry

};




declare function tombstone-db:retrieve-tombstones(
    $collection-path-info as xs:string ,
    $recursive as xs:boolean?
) as element(at:deleted-entry)*
{
    
    let $db-collection-path := atomdb:request-path-info-to-db-path( $collection-path-info )
    return
        if ( $recursive )
        then collection( $db-collection-path )/at:deleted-entry (: recursive :)
        else xmldb:xcollection( $db-collection-path )/at:deleted-entry (: not recursive :)
    
};




declare function tombstone-db:create-deleted-entry( 
    $member-path-info as xs:string , $user-name as xs:string? , $comment as xs:string?
) as element(at:deleted-entry)?
{

    if ( atomdb:member-available( $member-path-info ) )
    
    then 

        let $condemned := atomdb:retrieve-member( $member-path-info )
        let $self := $condemned/atom:link[@rel="self"]/@href cast as xs:string
        let $ref := $condemned/atom:id
        let $when := current-dateTime()
        let $collection-path-info := let $entry-path-info := substring-after($condemned/atom:link[@rel='edit']/@href/string(), $config:edit-link-uri-base) return text:groups($entry-path-info, "^(.+)/[^/]+$")[2]
        let $feed := atomdb:retrieve-feed-without-entries( $collection-path-info )
        let $ghost-atom-elements := $feed/atombeat:config-tombstones/atombeat:config/atombeat:param[@name="ghost-atom-elements"]/@value
        
        return
        
            <at:deleted-entry
                ref="{$ref}"
                when="{$when}">
                <at:by>
                {
                    if ( $config:user-name-is-email )
                    then <atom:email>{$user-name}</atom:email>
                    else <atom:name>{$user-name}</atom:name>
                }
                </at:by>
                <at:comment>{$comment}</at:comment>
                <atom:link rel="self" type="application/atomdeleted+xml" href="{$self}"/>
                {
                    if ( exists( $ghost-atom-elements ) )
                    then
                        <atombeat:ghost>
                        {
                            let $tokens := tokenize( $ghost-atom-elements cast as xs:string , " " )
                            for $child in $condemned/child::*
                            where (
                                namespace-uri( $child ) = $CONSTANT:ATOM-NSURI
                                and local-name( $child ) = $tokens
                            )
                            return util:deep-copy( $child )
                        }
                        </atombeat:ghost>
                    else ()
                }
            </at:deleted-entry>
    
    else ()
    
};




declare function tombstone-db:erect-tombstone(
    $member-path-info as xs:string , $deleted-entry as element(at:deleted-entry)
) as xs:string?
{
 
    let $member-db-path := concat( atomdb:request-path-info-to-db-path( $member-path-info ) , ".atom" )
    let $split := text:groups( $member-db-path , "^(.+)/([^/]+)$" )
    let $collection-db-path := $split[2]
    let $resource-name := $split[3]
    let $tombstone-erected := xmldb:store( $collection-db-path , $resource-name , $deleted-entry )
    return $tombstone-erected

};











