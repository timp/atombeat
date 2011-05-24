xquery version "1.0";

module namespace paging-plugin = "http://purl.org/atombeat/xquery/paging-plugin";
declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;
declare namespace opensearch = "http://a9.com/-/spec/opensearch/1.1/" ;
import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;
import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;




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
	
	if ( $operation eq $CONSTANT:OP-UPDATE-COLLECTION ) then paging-plugin:filter-entity( $entity )
	else $entity
	
};



declare function paging-plugin:filter-entity(
    $entity as item()*
) as item()*
{
    (: filter incoming request data :)
    
    let $reserved :=
        <reserved>
            <elements namespace-uri="http://a9.com/-/spec/opensearch/1.1/">
                <element>totalResults</element>
                <element>startIndex</element>
                <element>itemsPerPage</element>
            </elements>
            <elements namespace-uri="http://purl.org/atombeat/xmlns">
                <element>tlink</element>
            </elements>
            <atom-links>
                <link rel="first"/>
                <link rel="last"/>
                <link rel="previous"/>
                <link rel="next"/>
            </atom-links>
        </reserved>
    
    let $filtered-entity := atomdb:filter( $entity , $reserved )
    
    return $filtered-entity
    
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
    let $count-requested := if ( $count-param castable as xs:integer ) then xs:integer( $count-param ) else ()
    let $page-param := xutil:get-parameter( "page" , $request )
    let $page-requested := if ( $page-param castable as xs:integer ) then xs:integer( $page-param ) else ()

    (: decide the actual items-per-page we will use :)
    let $items-per-page :=
        if ( empty( $count-requested ) and empty( $default-page-size ) ) then 20 (: TODO make this a global configuration variable :)
        else if ( empty( $count-requested ) and exists( $default-page-size ) ) then xs:integer( $default-page-size ) 
        else if ( exists( $count-requested ) and empty( $max-page-size ) ) then $count-requested (: client gets what they want :)
        else if ( exists( $count-requested ) and $count-requested le xs:integer( $max-page-size ) ) then $count-requested (: client gets what they want :)
        else xs:integer( $max-page-size )
        
    (: calculate the last page based on the decided page size :)
    let $last-page := ceiling( $total-results div $items-per-page )
    
    (: decide what start page (and hence start index) we will use :)
    let $start-page :=
        if ( empty( $page-requested ) or $page-requested le 0 ) then 1 (: default to the beginning :)
        else if ( ( ( $page-requested - 1 ) * $items-per-page ) le $total-results ) then $page-requested (: client asked for a reasonable start page :)
        else $last-page (: client asked for a non-existent start page, so give them the last page :)
    let $start-index := ( ( $start-page - 1 ) * $items-per-page ) + 1
        
    let $self-uri := $feed/atom:link[@rel="self"]/@href/string()
    let $self-base := concat( $self-uri , if (contains( $self-uri , '?' ) ) then '&amp;' else '?' )
    let $new-self-uri := concat( $self-base , 'page=' , $start-page , '&amp;count=' , $items-per-page )
    let $first-uri := concat( $self-base , 'page=' , 1 , '&amp;count=' , $items-per-page )
    let $last-uri := concat( $self-base , 'page=' , $last-page , '&amp;count=' , $items-per-page )
    let $next-uri :=
        if ( $total-results gt ( $start-index + $items-per-page ) ) then 
            concat( $self-base , 'page=' , $start-page + 1 , '&amp;count=' , $items-per-page )
        else ()
    let $previous-uri :=
        if ( $start-page gt 1 ) then 
            concat( $self-base , 'page=' , $start-page - 1 , '&amp;count=' , $items-per-page )
        else ()
    
        
    return
    
        <atom:feed>
        {
            $feed/attribute::* ,
            $feed/child::*[
                not( . instance of element(atom:entry)) (: exclude entries - we're going to select a subsequence as the current page :)
                and not( . instance of element(atom:link) and ./@rel eq "self" ) (: exclude the self link, we're going to adjust it :)
            ]
        }
            <atom:link rel="self" href="{$new-self-uri}"/>
            <atom:link rel="first" href="{$first-uri}"/>
            <atom:link rel="last" href="{$last-uri}"/>
        {
            if ( exists( $next-uri ) ) then
                <atom:link rel="next" href="{$next-uri}"/>
            else (),
            if ( exists( $previous-uri ) ) then
                <atom:link rel="previous" href="{$previous-uri}"/>
            else ()
        }
            <opensearch:totalResults>{$total-results}</opensearch:totalResults>
            <opensearch:startIndex>{$start-index}</opensearch:startIndex>
            <opensearch:itemsPerPage>{$items-per-page}</opensearch:itemsPerPage>
            <atombeat:tlink rel="http://purl.org/atombeat/rel/page" href="{$self-base}page={{startPage}}&amp;count={{count}}"/>
        {
            subsequence( $feed/atom:entry , $start-index , $items-per-page )
        }
        </atom:feed>
};
