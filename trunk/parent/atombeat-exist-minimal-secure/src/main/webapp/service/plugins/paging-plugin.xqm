xquery version "1.0";

module namespace paging-plugin = "http://purl.org/atombeat/xquery/paging-plugin";
declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;
import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;
import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;




declare function paging-plugin:before(
	$operation as xs:string ,
	$request as element(request) ,
	$entity as item()*
) as item()*
{

    (:
    let $request-path-info := $request/path-info/string()
    let $message := concat( "before: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "info" , $message )
	let $log := util:log( "info" , $request )
	let $log := util:log( "info" , $entity )
	return
	:)
	
	(: TODO make sure we strip all paging links on update collection :)
	
	$entity
	
};



declare function paging-plugin:after(
	$operation as xs:string ,
	$request as element(request) ,
	$response as element(response)
) as item()*
{
    
    (:
    let $request-path-info := $request/path-info/string()
	let $message := concat( "after: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "info" , $message )
	let $log := util:log( "info" , $request )
	let $log := util:log( "info" , $response )
	return 
	:)
	
    if ( $operation eq $CONSTANT:OP-LIST-COLLECTION ) then paging-plugin:after-list-collection( $request, $response )
    else $response

}; 



declare function paging-plugin:after-list-collection(
	$request as element(request) ,
	$response as element(response)
) as item()*
{
    if ( $response/body/atom:feed/@atombeat:enable-paging eq "true" ) then 
    
        <response>
        {
            $response/status ,
            $response/headers
        }
            <body type='xml'>{paging-plugin:page-feed( $request, $response/body/atom:feed )}</body>
        </response>
        
    else $response
};



declare function paging-plugin:page-feed(
	$request as element(request) ,
	$feed as element(atom:feed)
) as element(atom:feed)
{

    let $total-results := count($feed/atom:entry)
    let $default-page-size := $feed/atombeat:config-paging/@default-page-size    
    let $max-page-size := $feed/atombeat:config-paging/@max-page-size    
    let $count-param := xutil:get-parameter( "count" , $request )
    let $count := if ( $count-param castable as xs:integer ) then xs:integer( $count-param ) else ()
    let $items-per-page :=
        if ( empty( $count ) and empty( $default-page-size ) ) then 20 (: TODO make this a global configuration variable :)
        else if ( empty( $count ) and exists( $default-page-size ) ) then $default-page-size cast as xs:integer
        else if ( exists( $count ) and empty( $max-page-size ) ) then $count (: client gets what they want :)
        else if ( exists( $count ) and $count le $max-page-size ) then $count (: client gets what they want :)
        else $max-page-size
    let $last-page := ceiling( $total-results / $count )
    let $start-page-param = xutil:get-parameter( "startPage" , $request )
    let $start-page := if ( $start-page-param castable as xs:integer ) then xs:integer( $start-page-param ) else ()
    let $start-index :=
        if ( empty( $start-page ) or $start-page le 0 ) then 1 (: default to the beginning :)
        else if ( ( ( $start-page - 1 ) * $items-per-page ) le $total-results ) then ( ( $start-page - 1 ) * $items-per-page ) + 1 (: client asked for a reasonable start page :)
        else ( $last-page - 1 ) * $items-per-page (: client asked for a non-existent start page, so give them the last page :)
    let $self := $feed/atom:link[@rel="self"]/@href/string()
        
    return
    
        <atom:feed>
        {
            $feed/attribute::* ,
            $feed/child::*[not( . instance of element(atom:entry)) and not( . instance of element(atom:link) ) and ./@rel eq "self" )]
        }
            <atom:link rel="first" href="{$self}{if (contains( $self , '?' ) ) then '&' else '?'}startPage=1&amp;count={$items-per-page}"/>
            <atom:link rel="last" href="{$self}{if (contains( $self , '?' ) ) then '&' else '?'}startPage={$last-page}&amp;count={$items-per-page}"/>
            (: TODO next link :)
            (: TODO previous link :)
            (: TODO opensearch elements :)
            (: TODO slice the entries :)
        </atom:feed>
};
