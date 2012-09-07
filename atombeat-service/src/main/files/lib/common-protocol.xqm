xquery version "1.0";

module namespace common-protocol = "http://purl.org/atombeat/xquery/common-protocol";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;
 
import module namespace request = "http://exist-db.org/xquery/request" ;
import module namespace response = "http://exist-db.org/xquery/response" ;
import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace atombeat-util = "http://purl.org/atombeat/xquery/atombeat-util" at "java:org.atombeat.xquery.functions.util.AtomBeatUtilModule";

import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;
import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace mime = "http://purl.org/atombeat/xquery/mime" at "../lib/mime.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace plugin = "http://purl.org/atombeat/xquery/plugin" at "../config/plugins.xqm" ;

declare variable $common-protocol:param-request-path-info := "request-path-info" ;





declare function common-protocol:get-request() as element(request)
{

    (: build a representation of the request, except for request entity (allow functions to consume directly to support streaming) :)
    
	let $request-method := upper-case( request:get-method() )
	let $request-path-info := request:get-attribute( $common-protocol:param-request-path-info )
	let $request-headers := xutil:get-request-headers()
	let $request-parameters := xutil:get-request-parameters()
	let $request-attributes := xutil:get-request-attributes()
    let $user := request:get-attribute( $config:user-name-request-attribute-key )
    let $roles :=
        <roles>
        {
            for $role in request:get-attribute($config:user-roles-request-attribute-key) return <role>{$role}</role>
        }
        </roles>
    
    let $request :=
        <request>
            <method>{$request-method}</method>
            <path-info>{$request-path-info}</path-info>
        {
            $request-headers ,
            $request-parameters ,
            $request-attributes ,
            if ( exists( $user ) ) then <user>{$user}</user> else () ,
            $roles
        }
        </request>
        
    return $request
    
};





declare function common-protocol:do-not-modified(
    $op-name as xs:string? ,
    $request as element(request)
) as element(response)
{

    <response>
        <status>{$CONSTANT:STATUS-REDIRECT-NOT-MODIFIED}</status>
        <headers/>
        <body/>
    </response>
    
};





declare function common-protocol:do-not-found(
    $op-name as xs:string? ,
    $request as element(request)
) as element(response)
{

    let $message := "The server has not found anything matching the Request-URI."

    let $response :=
    
        <response>
            <status>{$CONSTANT:STATUS-CLIENT-ERROR-NOT-FOUND}</status>
            <headers>
                <header>
                    <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                    <value>{$CONSTANT:MEDIA-TYPE-TEXT}</value>
                </header>
            </headers>
            <body type="text">{$message}</body>
        </response>

    return common-protocol:apply-after( plugin:after-error() , $op-name , $request , $response )

};



declare function common-protocol:do-precondition-failed(
    $op-name as xs:string? ,
    $request as element(request) ,
    $message as xs:string?
) as element(response)
{

    let $response :=
    
        <response>
            <status>{$CONSTANT:STATUS-CLIENT-ERROR-PRECONDITION-FAILED}</status>
            <headers>
                <header>
                    <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                    <value>{$CONSTANT:MEDIA-TYPE-TEXT}</value>
                </header>
            </headers>
            <body type="text">{concat( $message , " The precondition given in one or more of the request-header fields evaluated to false when it was tested on the server." )}</body>
        </response>

    return common-protocol:apply-after( plugin:after-error() , $op-name , $request , $response )

};



declare function common-protocol:do-bad-request(
    $op-name as xs:string? ,
    $request as element(request) ,
    $message as xs:string?
) as element(response)
{

    let $message := concat( $message , " The request could not be understood by the server due to malformed syntax. The client SHOULD NOT repeat the request without modifications." )

    let $response :=
    
        <response>
            <status>{$CONSTANT:STATUS-CLIENT-ERROR-BAD-REQUEST}</status>
            <headers>
                <header>
                    <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                    <value>{$CONSTANT:MEDIA-TYPE-TEXT}</value>
                </header>
            </headers>
            <body type="text">{$message}</body>
        </response>
        
    return common-protocol:apply-after( plugin:after-error() , $op-name , $request , $response )

};





declare function common-protocol:do-method-not-allowed(
    $op-name as xs:string? ,
    $request as element(request) ,
	$allow as xs:string*
) as element(response)
{

    let $message := "The method specified in the Request-Line is not allowed for the resource identified by the Request-URI."

    let $response :=
    
        <response>
            <status>{$CONSTANT:STATUS-CLIENT-ERROR-METHOD-NOT-ALLOWED}</status>
            <headers>
                <header>
                    <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                    <value>{$CONSTANT:MEDIA-TYPE-TEXT}</value>
                </header>
	            <header>
	                <name>{$CONSTANT:HEADER-ALLOW}</name>
	                <value>{string-join( $allow , " " )}</value>
	            </header>
            </headers>
            <body type="text">{$message}</body>
        </response>
    
    return common-protocol:apply-after( plugin:after-error() , $op-name , $request , $response )
    
};

 



declare function common-protocol:do-forbidden(
    $op-name as xs:string? ,
    $request as element(request) 
) as element(response)
{

    let $message := "The server understood the request, but is refusing to fulfill it. Authorization will not help and the request SHOULD NOT be repeated."

    let $response :=

        <response>
            <status>{$CONSTANT:STATUS-CLIENT-ERROR-FORBIDDEN}</status>
            <headers>
                <header>
                    <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                    <value>{$CONSTANT:MEDIA-TYPE-TEXT}</value>
                </header>
            </headers>
            <body type="text">{$message}</body>
        </response>

    return common-protocol:apply-after( plugin:after-error() , $op-name , $request , $response )
    
};




declare function common-protocol:do-unsupported-media-type(
    $op-name as xs:string? ,
    $request as element(request) ,
	$message as xs:string? 
) as element(response)
{

    let $message := concat( $message , " The server is refusing to service the request because the entity of the request is in a format not supported by the requested resource for the requested method." )

    let $response :=
    
        <response>
            <status>{$CONSTANT:STATUS-CLIENT-ERROR-UNSUPPORTED-MEDIA-TYPE}</status>
            <headers>
                <header>
                    <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                    <value>{$CONSTANT:MEDIA-TYPE-TEXT}</value>
                </header>
            </headers>
            <body type="text">{$message}</body>
        </response>

    return common-protocol:apply-after( plugin:after-error() , $op-name , $request , $response )

};




declare function common-protocol:do-unsupported-media-type(
    $op-name as xs:string? ,
    $request as element(request)
) as element(response)
{

    common-protocol:do-unsupported-media-type( $op-name , $request-path-info , () )

};




(:
 : Main request processing function.
 :)
declare function common-protocol:apply-op(
	$op-name as xs:string ,
	$op as function ,
	$request as element(request) ,
	$entity as item()* 
) as element(response)
{

	let $before-advice := common-protocol:apply-before( plugin:before() , $op-name , $request , $entity )
	
	return 
	 
		if ( $before-advice instance of element(response) ) (: interrupt request processing :)
		
		then $before-advice
		  
		else
		
		    let $request-advice := $before-advice[1] 

		    let $entity-advice := $before-advice[2] 
		        
			let $response := util:call( $op , $request-advice , $entity-advice )
			 
			let $after-advice := common-protocol:apply-after( plugin:after() , $op-name , $request-advice , $response )
			
			let $response := $after-advice
					    
			return $response

};





(:
 : Recursively call the sequence of plugin functions.
 :)
declare function common-protocol:apply-before(
	$functions as function* ,
	$op-name as xs:string ,
	$request as element(request) ,
	$entity as item()* 
) as item()* 
{
	
	(:
	 : Plugin functions applied during the before phase can have no side-effects,
	 : in which case they will return the request data unaltered, or they can
	 : modify the request data, or they can interrupt the processing of the
	 : request and cause a response to be sent, without calling any subsequent
	 : plugin functions or carrying out the target operation.
	 :)
	 
	if ( empty( $functions ) )
	
	then ( $request , $entity )
	
	else
	
		let $advice := util:call( $functions[1] , $op-name , $request , $entity )
		
		(: what happens next depends on advice :)
		
		return
		
			if ( $advice instance of element(response) )
			
			then $advice (: bail out, no further calling of before functions :)

			else 
			
			    let $entity-advice := 
			        if ( $advice[1] instance of element(void) ) then () else $advice[1]
			        
			    let $request-advice := 
			        if ( $advice[2] instance of element(attributes) ) then 
			            common-protocol:set-request-attributes( $request , $advice[2] )
			        else $request
			        
			    (: recursively call until before functions are exhausted :)
			    return common-protocol:apply-before( subsequence( $functions , 2 ) , $op-name , $request-advice , $entity-advice )

};



declare function common-protocol:set-request-attributes(
    $request as element(request) ,
    $set-attributes as element(attributes)
) as element(request)
{
    <request>
    {
        $request/attribute::* ,
        $request/child::*[not( . instance of element(attributes) )] ,
        <attributes>
        {
            for $attribute in $request/attributes/attribute
            where empty( $set-attributes/attribute[name eq $attribute/name] )
            return $attribute ,
            $set-attributes/attribute
        }
        </attributes>
    }
    </request>
};



(:
 : Recursively call the sequence of plugin functions.
 :)
declare function common-protocol:apply-after(
	$functions as function* ,
	$op-name as xs:string ,
	$request as element(request) ,
	$response as element(response)
) as item()* {
	
	if ( empty( $functions ) )
	
	then $response
	
	else
	
		let $advice := util:call( $functions[1] , $op-name , $request , $response )
		
		let $modified-response := $advice[1]

	    let $modified-request := 
	        if ( $advice[2] instance of element(attributes) ) then 
	            common-protocol:set-request-attributes( $request , $advice[2] )
	        else $request

		return
		
		    if ( exists( $modified-response ) and $modified-response instance of element(response) ) 
		    
		    then common-protocol:apply-after( subsequence( $functions , 2 ) , $op-name , $modified-request , $modified-response )
		    
		    else common-protocol:do-internal-server-error( $op-name , $request , "A plugin function failed to return a valid response; expected element(response)." )

};




declare function common-protocol:do-internal-server-error(
    $op-name as xs:string? ,
	$request as element(request) ,
	$message as xs:string 
) as element(response)
{

    let $message := concat( $message , " The server encountered an unexpected condition which prevented it from fulfilling the request." )

    let $response := 
    
        <response>
            <status>{$CONSTANT:STATUS-SERVER-ERROR-INTERNAL-SERVER-ERROR}</status>
            <headers>
                <header>
                    <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                    <value>{$CONSTANT:MEDIA-TYPE-TEXT}</value>
                </header>
            </headers>
            <body type="text">{$message}</body>
        </response>

    return $response
    (: do not send through after-error plugins, make this final - otherwise, could end up
       going in circles if plugin causes error :)
(:    return common-protocol:apply-after( plugin:after-error() , $op-name , $request-path-info , $response ) :)

};




declare function common-protocol:respond( 
	$request as element(request) ,
    $response as element(response) 
) as item()*
{

    (: TODO migrate augment errors to after-error plugin :)
    let $response := common-protocol:augment-errors( $request, $response )
    
    let $set-headers :=
        for $header in $response/headers/header
        return 
            let $name := $header/name/text()
            let $value := $header/value/text()
            return 
                if ( exists( $name ) and exists( $value ) ) 
                then response:set-header( $name , $value )
                else ()
    
    let $set-status := response:set-status-code( xs:integer( $response/status ) )

    return
    
        if ( $response/body/@type = "media" and $config:media-storage-mode = "DB" )
        then
            let $path := $response/body/string()
            let $binary-doc := atomdb:retrieve-media( $path )
            return response:stream-binary( $binary-doc , $response/headers/header[name=$CONSTANT:HEADER-CONTENT-TYPE]/value/text() )

        else if ( $response/body/@type = "media" and $config:media-storage-mode = "FILE" )
        then
            let $path := concat( $config:media-storage-dir , $response/body/string() )
            return atombeat-util:stream-file-to-response( $path , $response/headers/header[name=$CONSTANT:HEADER-CONTENT-TYPE]/value/text() )

        else if ( $response/body/@type = "unzip-media" and $config:media-storage-mode = "FILE" )
        then
            let $path := concat( $config:media-storage-dir , $response/body/string() )
            return atombeat-util:stream-zip-entry-to-response( $path , $response/body/@zip-entry/string(), $response/headers/header[name=$CONSTANT:HEADER-CONTENT-TYPE]/value/string() )

        else if ( $response/body/@type = "text" )
        then $response/body/string()

        else if ( $response/body/@type = "xml" )
        then
            let $serialize-option := string-join(
                ( 
                    'method=xml' , 
                    if ( exists( $response/body/@doctype-public ) ) then concat( 'doctype-public=' , $response/body/@doctype-public cast as xs:string ) else () ,
                    if ( exists( $response/body/@doctype-system ) ) then concat( 'doctype-system=' , $response/body/@doctype-system cast as xs:string ) else () 
                ) ,
                ' '
            )
            let $set-serialize-option := util:declare-option( 'exist:serialize' , $serialize-option )
            return $response/body/*
            
        else $response/body/*
        
};




declare function common-protocol:augment-errors(
	$request as element(request) ,
	$response as element(response)
) as element(response)
{

    let $status := xs:integer( $response/status )
    let $content-type := $response/headers/header[name=$CONSTANT:HEADER-CONTENT-TYPE]/value/text()
	let $request-path-info := request:get-attribute( $common-protocol:param-request-path-info )
    
    return 
	
        if ( 
            $status ge 400 
            and $status lt 600 
            and $content-type = $CONSTANT:MEDIA-TYPE-TEXT
        )
        
        then 

            <response>
    	        <status>{$response/status}</status>
    	        <headers>
    	        {
    	            $response/headers/header[name != $CONSTANT:HEADER-CONTENT-TYPE]
    	        }
    	            <header>
    	                <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
    	                <value>{$CONSTANT:MEDIA-TYPE-XML}</value>
    	            </header>
    	        </headers>
    	        <body type='xml'>
            		<error>
            		    <status>{$status}</status>
            			<message>{$response/body/text()}</message>
            			{$request}
            		</error>
    	        </body>
    	    </response>
    	    
        else $response        

}; 




