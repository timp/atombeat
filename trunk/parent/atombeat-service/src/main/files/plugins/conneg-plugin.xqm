xquery version "1.0";

module namespace conneg-plugin = "http://purl.org/atombeat/xquery/conneg-plugin";
declare namespace atom = "http://www.w3.org/2005/Atom" ;
import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace request = "http://exist-db.org/xquery/request" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "config.xqm" ;
import module namespace conneg-config = "http://purl.org/atombeat/xquery/conneg-config" at "../config/conneg.xqm" ;




declare function conneg-plugin:before(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string?
) as item()*
{

	let $message := concat( "before: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "info" , $message )
	
	return $request-data
	
};




declare function conneg-plugin:after(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$response as element(response)
) as element(response)
{

	let $message := concat( "after: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "info" , $message )
	
	let $output-key := request:get-parameter( "output" , "" )
	
	return 
	
	    if ( exists( $response/body/atom:entry) ) then
	    
	        let $entry := $response/body/atom:entry
	        let $augmented-entry := conneg-plugin:augment-entry( $entry )
	        let $augmented-response := conneg-plugin:replace-response-body( $response, $augmented-entry )
	        return 
                if ( not( $output-key eq "" ) ) then
                    conneg-plugin:transform-response( $output-key , $augmented-response ) 
                else $augmented-response
	            
	    else if ( exists( $response/body/atom:feed ) ) then
	    
	        let $feed := $response/body/atom:feed
	        let $augmented-feed := conneg-plugin:augment-feed( $feed )
	        let $augmented-response := conneg-plugin:replace-response-body( $response, $augmented-feed )
	        return 
                if ( not( $output-key eq "" ) ) then
                    conneg-plugin:transform-response( $output-key , $augmented-response ) 
                else $augmented-response
	                
	    else $response
	
}; 



declare function conneg-plugin:augment-entry(
    $entry as element(atom:entry)
) as element(atom:entry)
{
    <atom:entry>
    {
        $entry/attribute::* ,
        $entry/child::* ,
        for $alternate in $conneg-config:alternates/alternate
        let $baseuri := $entry/atom:link[@rel='edit']/@href cast as xs:string
        let $alturi := concat( $baseuri , if ( contains( $baseuri , "?" ) ) then "&amp;" else "?" , "output=" , $alternate/output-key )
        return
            <atom:link rel="alternate" type="{$alternate/media-type}" href="{$alturi}"/>
    }    
    </atom:entry>
};



declare function conneg-plugin:augment-feed(
    $feed as element(atom:feed)
) as element(atom:feed)
{
    <atom:feed>
    {
        $feed/attribute::* ,
        $feed/child::*[not( . instance of element(atom:entry) ) ] ,
        for $alternate in $conneg-config:alternates/alternate
        let $baseuri := $feed/atom:link[@rel='self']/@href cast as xs:string
        let $alturi := concat( $baseuri , if ( contains( $baseuri , "?" ) ) then "&amp;" else "?" , "output=" , $alternate/output-key )
        return
            <atom:link rel="alternate" type="{$alternate/media-type}" href="{$alturi}"/> ,
        for $entry in $feed/atom:entry return conneg-plugin:augment-entry( $entry )
    }    
    </atom:feed>
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
        <body>{$new-body}</body>
    </response>
};




declare function conneg-plugin:transform-response(
    $output-key as xs:string ,
    $response as element(response)
) as item()*
{
    
    let $alternate := $conneg-config:alternates/alternate[output-key eq $output-key]
    let $index := index-of( $conneg-config:alternates/alternate , $alternate )
    let $transformer := $conneg-config:transformers[$index]
    let $media-type := $alternate/media-type cast as xs:string
    let $output-type := $alternate/output-type cast as xs:string
    let $data := $response/body/* 
    
    let $transformed-response :=

        if ( exists( $alternate ) and exists( $transformer ) and exists( $data ) ) then
        
            let $transformed-entry := 
                if ( $transformer instance of element(stylesheet) ) then
                    let $stylesheet := concat( $config:service-url-base , $transformer/text() )
                    return transform:transform( $data , $stylesheet , () )
                else
                    util:call( $transformer , $data )
            
            return
            
                <response>
                    {$response/status}
                    <headers>
                    {
                        for $header in $response/headers/header
                        return
                        
                            (: override content-type header :)
                            if ( $header/name eq "Content-Type" ) then 
                                <header>
                                    <name>Content-Type</name>
                                    <value>{$media-type}</value>
                                </header>
    
                            (: override content-location header :)
                            else if ( $header/name eq "Content-Location" ) then
                                <header>
                                    <name>Content-Location</name>
                                    <value>{concat( $header/value , if ( contains( $header/value , "?" ) ) then "&amp;" else "?" , "output=" , $alternate/output-key )}</value>
                                </header>
                                
                            (: pass through all other headers :)
                            else $header
                    }
                    </headers>
                    <body type="{$output-type}">{$transformed-entry}</body>
                </response>
                
        else $response

    return $transformed-response
            
};





