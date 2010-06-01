xquery version "1.0";

module namespace exp = "http://purl.org/atombeat/xquery/expansion-protocol";

declare namespace atom = "http://www.w3.org/2005/Atom" ;

(: see http://tools.ietf.org/html/draft-mehta-atom-inline-01 :)
declare namespace ae = "http://purl.org/atom/ext/" ;

import module namespace request = "http://exist-db.org/xquery/request" ;
import module namespace response = "http://exist-db.org/xquery/response" ;
import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace v="http://exist-db.org/versioning" ;

import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "constants.xqm" ;

import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "xutil.xqm" ;
import module namespace mime = "http://purl.org/atombeat/xquery/mime" at "mime.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "atomdb.xqm" ;
import module namespace ap = "http://purl.org/atombeat/xquery/atom-protocol" at "atom-protocol.xqm" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;

 
(:
 : TODO doc me
 :)
declare function exp:do-service()
as item()*
{

	let $request-path-info := request:get-attribute( $ap:param-request-path-info )
	let $request-method := request:get-method()
	
	return
	
		if ( $request-method = $CONSTANT:METHOD-GET )
		
		then exp:do-get( $request-path-info )
		
		(: TODO configurable expansion via POST :)
		
		else ap:do-method-not-allowed( $request-path-info , ( "GET" ) )

};




(:
 : TODO doc me 
 :)
declare function exp:do-get(
	$request-path-info as xs:string 
) as item()*
{

	if ( atomdb:member-available( $request-path-info ) )
	
	then exp:do-get-entry( $request-path-info )
	
	else ap:do-not-found( $request-path-info )
	
};




declare function exp:do-get-entry(
	$request-path-info as xs:string 
) as item()*
{

	let $log := util:log( "debug" , "== exp:do-get-entry() ==" )
	let $log := util:log( "debug" , $request-path-info )

    let $entry := atomdb:retrieve-member( $request-path-info )
    
    return exp:default-expansion( $entry )
    
};




declare function exp:default-expansion(
    $entry as element(atom:entry)
) as element(atom:entry)
{
    <atom:entry>
    {
        $entry/attribute::* ,
        for $child in $entry/child::* 
        where ( not( local-name( $child ) = $CONSTANT:ATOM-LINK and namespace-uri( $child ) = $CONSTANT:ATOM-NSURI ) )
        return $child ,
        for $link in $entry/atom:link
        return
            if ( starts-with( $link/@href , $config:content-service-url ) )
            then 
                let $entry-path-info := substring-after( $link/@href , $config:content-service-url )
                (: TODO security - only inline if allowed to retrieve :)
                let $entry := atomdb:retrieve-member( $entry-path-info )
                return 
                    <atom:link>
                    {
                        $link/attribute::* ,
                        $link/child::* ,
                        <ae:inline>
                            { $entry }
                        </ae:inline>
                    }
                    </atom:link>
            else $link
     }
    </atom:entry>
};



