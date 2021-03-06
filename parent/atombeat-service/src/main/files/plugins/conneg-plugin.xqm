xquery version "1.0";

module namespace conneg-plugin = "http://purl.org/atombeat/xquery/conneg-plugin" ;
declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace app = "http://www.w3.org/2007/app" ;
import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace request = "http://exist-db.org/xquery/request" ;
import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;
import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace conneg-config = "http://purl.org/atombeat/xquery/conneg-config" at "../config/conneg.xqm" ;




declare function conneg-plugin:before(
	$operation as xs:string ,
	$request as element(request) ,
	$entity as item()*
) as item()*
{

    let $request-path-info := $request/path-info/text()
	
	return
	
	    if ( 
	        (: operations we will negotiate response for :)
	        $operation = ( 
                $CONSTANT:OP-LIST-COLLECTION ,
                $CONSTANT:OP-CREATE-MEMBER ,
                $CONSTANT:OP-RETRIEVE-MEMBER ,
                $CONSTANT:OP-UPDATE-MEMBER , 
                $CONSTANT:OP-CREATE-MEDIA ,
                $CONSTANT:OP-UPDATE-MEDIA ,
                $CONSTANT:OP-CREATE-COLLECTION ,
                $CONSTANT:OP-UPDATE-COLLECTION , 
                $CONSTANT:OP-RETRIEVE-WORKSPACE-ACL , 
                $CONSTANT:OP-UPDATE-WORKSPACE-ACL ,
                $CONSTANT:OP-RETRIEVE-COLLECTION-ACL ,
                $CONSTANT:OP-UPDATE-COLLECTION-ACL ,
                $CONSTANT:OP-RETRIEVE-MEMBER-ACL ,
                $CONSTANT:OP-UPDATE-MEMBER-ACL ,
                $CONSTANT:OP-RETRIEVE-MEDIA-ACL ,
                $CONSTANT:OP-UPDATE-MEDIA-ACL , 
                $CONSTANT:OP-MULTI-CREATE ,
                $CONSTANT:OP-RETRIEVE-HISTORY ,
                $CONSTANT:OP-RETRIEVE-REVISION ,
                $CONSTANT:OP-RETRIEVE-SERVICE
            ) 
	    ) then

        	(: look for output param and accept header :)
        	let $output-param := xutil:get-parameter( "output" , $request )
        	let $accept := xutil:get-header( "Accept" , $request )
        	
        	(: N.B. it is VERY IMPORTANT to pull the accept header and output param from the supplied 
        	 : request fragment, and NOT directly from the request, because this might have been called
        	 : as part of another XQuery, not the main Atom protocol engine :)
        	
        	let $output-key :=
        	    if ( exists( $output-param ) and not( $output-param eq "" ) ) then $output-param (: output param trumps accept header :)
        	    else if ( $operation = $CONSTANT:OP-RETRIEVE-SERVICE ) then conneg-plugin:negotiate-service( $accept ) (: different variants for service document :)
        	    else conneg-plugin:negotiate( $accept )

            return
            
                if ( exists( $output-key ) ) then 
                
                    let $modified-entity := conneg-plugin:filter-request-data( $entity )
                	(: store output key for use in after phase :)
                	let $set-request-attributes :=
                        <attributes>
                            <attribute>
                                <name>atombeat.conneg-plugin.output-key</name>
                                <value>{$output-key}</value>
                            </attribute>
                        </attributes>
                    return ( $modified-entity , $set-request-attributes ) (: proceed with request processing, but filter alternate links from feeds and entries first :)
                
                else 

        		    let $response-data := "The resource identified by the request is only capable of generating response entities which have content characteristics not acceptable according to the accept headers sent in the request."
        			
        			return 
        			
        			    <response>
        			        <status>{$CONSTANT:STATUS-CLIENT-ERROR-NOT-ACCEPTABLE}</status>
        			        <headers>
        			            <header>
        			                <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
        			                <value>{$CONSTANT:MEDIA-TYPE-HTML}</value>
        			            </header>
        			        </headers>
        			        <body type='xml'>
        			            <html>
        			                <head><title>406 Not Acceptable</title></head>
        			                <body>
        			                    <h1>406 Not Acceptable</h1>
        			                    <p>{$response-data}</p>
        			                    <p>The following media types are available:</p>
        			                    <ul>
        			                    {
        			                        for $variant in $conneg-config:variants/variant
        			                        return
        			                            <li>{$variant/media-type cast as xs:string}</li>
        			                    }
        			                    </ul>
        			                </body>
        			            </html>
        			        </body>
        			    </response>

	    else $entity
	
};



declare function conneg-plugin:filter-request-data(
    $entity as item()*
) as item()*
{

    if ( $entity instance of element(atom:entry) ) then conneg-plugin:filter-entry( $entity )
    
    else if ( $entity instance of element(atom:feed) ) then conneg-plugin:filter-feed( $entity )
    
    else if ( empty( $entity ) ) then <void/>
    
    else $entity
    
};




declare function conneg-plugin:filter-entry(
    $entry as element(atom:entry)
) as element(atom:entry)
{

    let $reserved :=
        <reserved>
            <atom-links>
                <link rel="alternate"/>
            </atom-links>
        </reserved>
    
    let $filtered-entry := atomdb:filter( $entry , $reserved )
    
    return $filtered-entry

};




declare function conneg-plugin:filter-feed(
    $feed as element(atom:feed)
) as element(atom:feed)
{

    let $reserved :=
        <reserved>
            <atom-links>
                <link rel="alternate"/>
            </atom-links>
        </reserved>
    
    let $filtered-feed := atomdb:filter( $feed , $reserved )
    
    return
    
        <atom:feed>
        {
            $filtered-feed/attribute::* ,
            $filtered-feed/child::*[not( . instance of element(atom:entry) )] ,
            for $entry in $filtered-feed/atom:entry return conneg-plugin:filter-entry($entry)
        }
        </atom:feed>
    
};




declare function conneg-plugin:after(
	$operation as xs:string ,
	$request as element(request) ,
	$response as element(response)
) as item()*
{

    let $request-path-info := $request/path-info/text()
	let $atom-data := $response/body/(atom:entry|atom:feed|app:service)

    return
    
    	if ( empty( $atom-data ) ) then $response (: do nothing :)
    	
    	else
    	
        	let $augmented-data :=
        	    if ( $atom-data instance of element(atom:entry) ) then conneg-plugin:augment-entry( $atom-data )
        	    else if ( $atom-data instance of element(atom:feed) ) then conneg-plugin:augment-feed( $atom-data )
        	    else if ( $atom-data instance of element(app:service) ) then conneg-plugin:augment-service( $atom-data )
        	    else $atom-data
        	
            let $augmented-response := conneg-plugin:replace-response-body( $response, $augmented-data )
            
        	let $output-key := $request/attributes/attribute[name='atombeat.conneg-plugin.output-key']/value/text()
        	
            return 
                if ( exists( $output-key ) and not( $output-key eq "" ) ) then
                    conneg-plugin:transform-response( $output-key , $augmented-response ) 
                else $augmented-response
	            	
}; 



declare function conneg-plugin:augment-entry(
    $entry as element(atom:entry)
) as element(atom:entry)
{
    <atom:entry>
    {
        $entry/attribute::* ,
        $entry/child::* ,
        for $variant in $conneg-config:variants/variant
        let $baseuri := $entry/atom:link[@rel='edit']/@href cast as xs:string
        let $alturi := concat( $baseuri , if ( contains( $baseuri , "?" ) ) then "&amp;" else "?" , "output=" , $variant/output-key )
        return
            <atom:link rel="alternate" type="{$variant/media-type}" href="{$alturi}"/>
    }    
    </atom:entry>
};



declare function conneg-plugin:augment-feed(
    $feed as element(atom:feed)
) as element(atom:feed)
{
    let $baseuri := $feed/atom:link[@rel='self']/@href (: response to multi-create has no self link :)
    return
        if ( exists( $baseuri ) ) then
            <atom:feed>
            {
                $feed/attribute::* ,
                $feed/child::*[not( . instance of element(atom:entry) ) ] ,
                for $variant in $conneg-config:variants/variant
                let $alturi := concat( $baseuri , if ( contains( $baseuri , "?" ) ) then "&amp;" else "?" , "output=" , $variant/output-key )
                return
                    <atom:link rel="alternate" type="{$variant/media-type}" href="{$alturi}"/> ,
                for $entry in $feed/atom:entry return conneg-plugin:augment-entry( $entry )
            }    
            </atom:feed>
        else $feed
};



declare function conneg-plugin:augment-service(
    $service as element(app:service)
) as element(app:service)
{
    <app:service>
    {
        $service/attribute::* ,
        $service/child::* ,
        for $variant in $conneg-config:service-variants/variant
        let $baseuri := concat( $config:service-url-base , '/' )
        let $alturi := concat( $baseuri , if ( contains( $baseuri , "?" ) ) then "&amp;" else "?" , "output=" , $variant/output-key )
        return
            <atom:link rel="alternate" type="{$variant/media-type}" href="{$alturi}"/>
    }    
    </app:service>
};



declare function conneg-plugin:replace-response-body(
    $response as element(response) , $new-body as element()
) as element(response)
{
    <response>
    {    
        $response/status ,
        $response/headers
    }
        <body type='xml'>{$new-body}</body>
    </response>
};




declare function conneg-plugin:transform-response(
    $output-key as xs:string ,
    $response as element(response)
) as item()*
{
    
    let $variants := 
        if ( exists( $response/body/app:service ) ) then $conneg-config:service-variants
        else $conneg-config:variants
        
    let $transformers := 
        if ( exists( $response/body/app:service ) ) then $conneg-config:service-transformers
        else $conneg-config:transformers
        
    let $variant := $variants/variant[output-key eq $output-key]
    
    return 
    
        if ( exists( $variant ) ) then
        
            let $index := index-of( $variants/variant , $variant )
            let $transformer := $transformers[$index]
            let $media-type := $variant/media-type cast as xs:string
            let $output-type := $variant/output-type cast as xs:string
            let $data := $response/body/(atom:entry|atom:feed|app:service)
            
            let $transformed-response :=
        
                if ( exists( $transformer ) and exists( $data ) ) then
                
                    let $transformed-data := 
                        if ( $transformer instance of element(identity) ) then $data
                        else if ( $transformer instance of element(stylesheet) ) then
                            let $stylesheet := 
                                if ( matches( $transformer/text() , "^(http:|file:|ftp:)" ) ) then $transformer/text()
                                else concat( $config:service-url-base , $transformer/text() )
                            return transform:transform( $data , xs:anyURI($stylesheet) , () )
                        else
                            util:call( $transformer , $data )
                    
                    return
                    
                        <response>
                            {$response/status}
                            <headers>
                            {
                                for $header in $response/headers/header
                                return
                                
                                    (: if we're doing atom, pass the header through so we preserve the type parameter :)
                                    if ( $header/name eq "Content-Type" and $media-type eq "application/atom+xml" ) then $header
            
                                    (: override content-type header :)
                                    else if ( $header/name eq "Content-Type" ) then 
                                        <header>
                                            <name>Content-Type</name>
                                            <value>{$media-type}</value>
                                        </header>
            
                                    (: override content-location header :)
                                    else if ( $header/name eq "Content-Location" ) then
                                        <header>
                                            <name>Content-Location</name>
                                            <value>{concat( $header/value , if ( contains( $header/value , "?" ) ) then "&amp;" else "?" , "output=" , $variant/output-key )}</value>
                                        </header>
                                        
                                    (: pass through all other headers :)
                                    else $header
                            }
                                <header>
                                    <name>Vary</name>
                                    <value>Accept</value>
                                </header>
                            </headers>
                            <body type="{$output-type}">
                            {
                                if ( exists( $variant/doctype-public ) ) then attribute doctype-public { $variant/doctype-public/text() } else () ,
                                if ( exists( $variant/doctype-system ) ) then attribute doctype-system { $variant/doctype-system/text() } else () ,
                                $transformed-data
                            }
                            </body>
                        </response>
                        
                else $response
        
            return $transformed-response
            
        else $response 
                    
};



declare function conneg-plugin:negotiate(
    $accept-header as xs:string?
) as xs:string? 
{

    let $variants := $conneg-config:variants
    let $header := if ( empty( $accept-header ) or $accept-header = "" ) then $CONSTANT:MEDIA-TYPE-ATOM else $accept-header
    return conneg-plugin:negotiate-common( $header , $variants )
    
};



declare function conneg-plugin:negotiate-service(
    $accept-header as xs:string?
) as xs:string?
{

    let $variants := $conneg-config:service-variants
    let $header := if ( empty( $accept-header ) or $accept-header = "" ) then $CONSTANT:MEDIA-TYPE-ATOMSVC else $accept-header
    return conneg-plugin:negotiate-common( $header , $variants )

};



declare function conneg-plugin:negotiate-common(
    $accept-header as xs:string? ,
    $variants-config as element(variants)
) as xs:string? {

    (: first parse the accept header :)
    let $accepts :=
(:
        if ( empty( $accept-header ) ) then
            <accept>
                <media-range>application/atom+xml</media-range>
                <quality>1</quality>
            </accept>
        else
:)
            for $accept-token in tokenize( $accept-header , "," )
            let $normalized-accept-token := normalize-space( $accept-token )
            let $media-tokens := tokenize( $normalized-accept-token , ";" )
            let $media-range := normalize-space( $media-tokens[1] )
            let $q := 
                for $media-token in $media-tokens
                let $normalized-media-token := normalize-space( $media-token )
                where starts-with( $normalized-media-token , "q=" )
                return substring-after( $normalized-media-token , "q=" )
            let $quality := 
                if ( exists( $q ) and $q castable as xs:float ) then $q 
                else if ( $media-range = "*/*" and not( contains( $accept-header , "q=" ) ) ) then 0.01 (: fiddle as per http://httpd.apache.org/docs/current/content-negotiation.html#better - needed for abdera, interestingly enough! :)
                else if ( ends-with( $media-range , "/*" ) and not( contains( $accept-header , "q=" ) ) ) then 0.02 (: fiddle as per http://httpd.apache.org/docs/current/content-negotiation.html#better - needed for abdera, interestingly enough! :)
                else 1.0
            return
                <accept>
                    <media-range>{$media-range}</media-range>
                    <quality>{$quality}</quality>
                </accept>
                
    (: next, try to find variants that match the accept header :)
    let $variants :=
        for $variant in $variants-config/variant
        let $type := tokenize( $variant/media-type , "/" )[1]
        let $subtype := tokenize( $variant/media-type , "/" )[1]
        where 
            ( $variant/media-type = $accepts/media-range ) (: exact match :) 
            or ( $accepts/media-range = concat( $type , "/*" ) ) (: match any subtype :)
            or ( $accepts/media-range = '*/*' ) (: match any :)
        return $variant
        
    return
    
        if ( empty( $variants ) ) then () (: bug out, not acceptable :)
        
        else 
            
            (: next, compute a quality score for each variant :)
            let $scores := 
                for $variant in $variants
                let $type := tokenize( $variant/media-type , "/" )[1]
                let $subtype := tokenize( $variant/media-type , "/" )[1]
                let $accept := (
                    $accepts[media-range = $variant/media-type] ,
                    $accepts[media-range = concat( $type , "/*" )] ,
                    $accepts[media-range = '*/*']
                )[1]
                return xs:float( $variant/qs ) * xs:float( $accept/quality )
        
            let $top-score := max( $scores )
        
            let $indexes := index-of ( $scores , $top-score )
            
            let $index := 
                if ( $indexes instance of xs:integer ) then $indexes
                else $indexes[1]
                
            let $winner := $variants[$index]
        
            return $winner/output-key cast as xs:string
    
};



