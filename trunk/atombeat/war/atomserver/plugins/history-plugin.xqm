xquery version "1.0";

module namespace hp = "http://www.cggh.org/2010/atombeat/xquery/history-plugin";

declare namespace atom = "http://www.w3.org/2005/Atom" ;

(: see http://tools.ietf.org/html/draft-snell-atompub-revision-00 :)
declare namespace ar = "http://purl.org/atompub/revision/1.0" ;

import module namespace request = "http://exist-db.org/xquery/request" ;
import module namespace response = "http://exist-db.org/xquery/response" ;
import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace v="http://exist-db.org/versioning" ;

import module namespace CONSTANT = "http://www.cggh.org/2010/atombeat/xquery/constants" at "constants.xqm" ;

import module namespace xutil = "http://www.cggh.org/2010/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace mime = "http://www.cggh.org/2010/atombeat/xquery/mime" at "../lib/mime.xqm" ;
import module namespace atomdb = "http://www.cggh.org/2010/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;

import module namespace config = "http://www.cggh.org/2010/atombeat/xquery/config" at "../config/shared.xqm" ;




declare function hp:before(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string?
) as item()*
{
	
	(: TODO :)

	let $message := concat( "history plugin, before: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "debug" , $message )
	
	return
	
		if ( $operation = $CONSTANT:OP-CREATE-COLLECTION )
		
		then hp:before-create-collection( $request-path-info , $request-data )
		
		else if ( $operation = $CONSTANT:OP-CREATE-MEMBER )
		
		then hp:before-create-member( $request-path-info , $request-data )
		
		else if ( $operation = $CONSTANT:OP-UPDATE-MEMBER )
		
		then hp:before-update-member( $request-path-info , $request-data )
		
		else
	
			let $status-code := 0 (: we don't want to interrupt request processing :)
			return ( $status-code , $request-data )
};




declare function hp:before-create-collection(
	$request-path-info as xs:string ,
	$request-data as element(atom:feed)
) as item()*
{

	let $message := concat( "history plugin, before create-collection, request-path-info: " , $request-path-info ) 
	let $log := util:log( "debug" , $message )

	let $enable-history := request:get-header( "X-Atom-Enable-History" )
	
	let $enable-history := 
		if ( $enable-history castable as xs:boolean ) then xs:boolean( $enable-history )
		else false()

	let $history-enabled :=
		
		if ( $enable-history )
		
		then
		
			let $collection-db-path := atomdb:request-path-info-to-db-path( $request-path-info )

			let $collection-config :=
		
				<collection xmlns="http://exist-db.org/collection-config/1.0">
				    <triggers>
				        <trigger event="store,remove,update" class="org.exist.versioning.VersioningTrigger">
				            <parameter name="overwrite" value="yes"/>
				        </trigger>
				    </triggers>
				</collection>
				
			let $config-collection-path := concat( "/db/system/config" , $collection-db-path )
			let $log := util:log( "debug" , concat( "$config-collection-path: " , $config-collection-path ) )
			
			let $config-collection-path := xutil:get-or-create-collection( $config-collection-path )
			let $log := util:log( "debug" , concat( "$config-collection-path: " , $config-collection-path ) )
			
			let $config-resource-path := xmldb:store( $config-collection-path , "collection.xconf" , $collection-config )
			let $log := util:log( "debug" , concat( "$config-resource-path: " , $config-resource-path ) )
			
			return $config-resource-path

		else ()		
	
	let $status-code := 0 (: we don't want to interrupt request processing :)
	return ( $status-code , $request-data )

};




declare function hp:before-create-member(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry)
) as item()*
{

	(: add a revision comment to the incoming entry :)
	
	let $log := util:log( "debug" , "history-protocol, before-create-member")
	
    let $comment := request:get-header("X-Atom-Revision-Comment")
	let $log := util:log( "debug" , $comment )
    
    let $comment := if ( empty( $comment ) or $comment = "" ) then "initial revision" else $comment
	let $log := util:log( "debug" , $comment )
    
    let $user-name := request:get-attribute( $config:user-name-request-attribute-key )
	let $log := util:log( "debug" , $user-name )
    
    let $published := current-dateTime()
	let $log := util:log( "debug" , $published )

    let $revision-comment := 
        <ar:comment>
            <atom:author>
                {
                    if ( $config:user-name-is-email ) then
                    <atom:email>{$user-name}</atom:email>
                    else
                    <atom:name>{$user-name}</atom:name>                    
                }
            </atom:author>
            <atom:updated>{$published}</atom:updated>
            <atom:summary>{$comment}</atom:summary>
        </ar:comment>
	
	let $log := util:log( "debug" , $revision-comment )
	
	(: modify incoming entry to include revision comment :)
	
	let $request-data :=    
        <atom:entry>
        {
        	$request-data/attribute::* ,
        	for $child in $request-data/child::*
        	where (
        		not( 
	        		namespace-uri( $child ) = $CONSTANT:ATOM-NSURI
	        		and local-name( $child ) = $CONSTANT:ATOM-LINK
	        		and $child/@rel = "history"
        		)
        		and not(
	        		namespace-uri( $child ) = "http://purl.org/atompub/revision/1.0"
	        		and local-name( $child ) = "comment"
        		)
        	)
        	return $child ,

        	(: TODO exclude revision comments? :)
			
            $revision-comment
        }
        </atom:entry>  

	let $log := util:log( "debug" , $request-data )
	
	let $status-code := 0 (: we don't want to interrupt request processing :)
	return ( $status-code , $request-data )

};




declare function hp:before-update-member(
	$request-path-info as xs:string ,
	$request-data as element(atom:entry)
) as item()*
{

	(: add a revision comment to the incoming entry :)
	
	let $log := util:log( "debug" , "history-protocol, before-update-member")
	
    let $comment := request:get-header("X-Atom-Revision-Comment")
	let $log := util:log( "debug" , $comment )
    
    let $user-name := request:get-attribute( $config:user-name-request-attribute-key )
	let $log := util:log( "debug" , $user-name )
    
    let $published := current-dateTime()
	let $log := util:log( "debug" , $published )

    let $revision-comment := 
        <ar:comment>
            <atom:author>
                {
                    if ( $config:user-name-is-email ) then
                    <atom:email>{$user-name}</atom:email>
                    else
                    <atom:name>{$user-name}</atom:name>                    
                }
            </atom:author>
            <atom:updated>{$published}</atom:updated>
            <atom:summary>{$comment}</atom:summary>
        </ar:comment>
	
	let $log := util:log( "debug" , $revision-comment )
	
	(: modify incoming entry to include revision comment :)
	
	let $request-data :=    
        <atom:entry>
        {
        	$request-data/attribute::* ,
        	for $child in $request-data/child::*
        	where (
        		not( 
	        		namespace-uri( $child ) = $CONSTANT:ATOM-NSURI
	        		and local-name( $child ) = $CONSTANT:ATOM-LINK
	        		and $child/@rel = "history"
        		)
        		and not(
	        		namespace-uri( $child ) = "http://purl.org/atompub/revision/1.0"
	        		and local-name( $child ) = "comment"
        		)
        	)
        	return $child ,
			
            $revision-comment
        }
        </atom:entry>  

	let $log := util:log( "debug" , $request-data )
	
	let $status-code := 0 (: we don't want to interrupt request processing :)
	return ( $status-code , $request-data )

};




declare function hp:after(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$response-data as item()* ,
	$content-type as xs:string?
) as item()*
{

	(: TODO :)

	let $message := concat( "history plugin, after: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "debug" , $message )

	return
		
		if ( $operation = $CONSTANT:OP-RETRIEVE-MEMBER )
		
		then hp:after-retrieve-member( $request-path-info , $response-data , $content-type )

		else if ( $operation = $CONSTANT:OP-CREATE-MEMBER )
		
		then 
			let $log := util:log( "debug" , "found create-member" )
			return hp:after-create-member( $request-path-info , $response-data , $content-type )

		else if ( $operation = $CONSTANT:OP-UPDATE-MEMBER )
		
		then hp:after-update-member( $request-path-info , $response-data , $content-type )

		else 

			(: pass response data and content type through, we don't want to modify response :)
			( $response-data , $content-type )
}; 




declare function hp:after-retrieve-member(
	$request-path-info as xs:string ,
	$response-data as item()* ,
	$content-type as xs:string?
) as item()*
{

	let $response-data := hp:append-history-link( $response-data )

	return ( $response-data , $content-type )
};




declare function hp:after-create-member(
	$request-path-info as xs:string ,
	$response-data as item()* ,
	$content-type as xs:string?
) as item()*
{

	let $response-data := hp:append-history-link( $response-data )

	return ( $response-data , $content-type )
};




declare function hp:after-update-member(
	$request-path-info as xs:string ,
	$response-data as item()* ,
	$content-type as xs:string?
) as item()*
{

	let $response-data := hp:append-history-link( $response-data ) (: N.B. workaround here!!! :)

	return ( $response-data , $content-type )
};




declare function hp:append-history-link (
	$response-entry as element(atom:entry)
) as element(atom:entry)
{
	let $log := util:log( "debug" , $response-entry )
	
	let $self-uri := $response-entry/atom:link[@rel="self"]/@href
	let $log := util:log( "debug" , concat( "$self-uri: " , $self-uri ) )
	
	let $entry-path-info := substring-after( $self-uri , $config:service-url )
	let $log := util:log( "debug" , concat( "$entry-path-info: " , $entry-path-info ) )
	
	let $history-uri := concat( $config:history-service-url , $entry-path-info )
	let $log := util:log( "debug" , $history-uri )
	
	let $response-entry :=
	
		<atom:entry>
		{
			$response-entry/attribute::* ,
			$response-entry/child::*
		}
			<atom:link rel="history" href="{$history-uri}"/>			
		</atom:entry>

	let $log := util:log( "debug" , $response-entry )
	return $response-entry
	
};




