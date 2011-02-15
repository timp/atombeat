xquery version "1.0";

module namespace tagger-plugin = "http://purl.org/atombeat/xquery/tagger-plugin";
declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;
import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;




declare function tagger-plugin:before(
	$operation as xs:string ,
	$request as element(request) ,
	$entity as item()*
) as item()*
{

    let $request-path-info := $request/path-info/text()
	let $message := concat( "tagger-plugin before: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "info" , $message )
	
	let $modified-entity :=
	
	    if ( 
	    	$operation = ( 
                $CONSTANT:OP-CREATE-MEMBER ,
                $CONSTANT:OP-UPDATE-MEMBER  
            ) 
	    ) then local:before-create-or-update-member( $operation , $request , $entity )
	
	    else $entity
	    
	return $modified-entity
	
};



declare function local:before-create-or-update-member(
    $operation as xs:string ,
    $request as element(request) ,
    $entry as element(atom:entry)
) as element(atom:entry)
{

    let $request-path-info := $request/path-info/text()
    let $collection-path-info := 
        if ( $operation = $CONSTANT:OP-CREATE-MEMBER ) then $request-path-info
        else text:groups( $request-path-info , "^(.+)/[^/]+$" )[2]
    let $feed := atomdb:retrieve-feed-without-entries( $collection-path-info )
    let $taggers := $feed/atombeat:config-taggers/atombeat:tagger[@type='fixed-exclusive' and @scope='member']
    let $modified-entry :=
        if ( exists( $taggers ) ) then local:fixed-exclusive( $entry , $taggers )
        else $entry
    return $modified-entry

};



declare function local:fixed-exclusive(
    $entry as element(atom:entry) ,
    $taggers as element(atombeat:tagger)?
) as element(atom:entry)
{
    <atom:entry>
    {
        $entry/attribute::* ,
        $entry/child::*[not( . instance of element(atom:category) )] ,
        $entry/atom:category[not( @scheme = $taggers/@scheme )] ,
        for $tagger in $taggers return
            <atom:category scheme="{$tagger/@scheme}" term="{$tagger/@term}" label="{$tagger/@label}"/>
    }
    </atom:entry>
};



declare function tagger-plugin:after(
	$operation as xs:string ,
	$request as element(request) ,
	$response as element(response)
) as element(response)
{

    let $request-path-info := $request/path-info/text()
	let $message := concat( "tagger-plugin after: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "info" , $message )
	
	return $response

}; 



