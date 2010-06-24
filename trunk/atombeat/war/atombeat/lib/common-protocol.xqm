xquery version "1.0";

module namespace common-protocol = "http://purl.org/atombeat/xquery/common-protocol";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;
 
import module namespace request = "http://exist-db.org/xquery/request" ;
import module namespace response = "http://exist-db.org/xquery/response" ;
import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "constants.xqm" ;
import module namespace mime = "http://purl.org/atombeat/xquery/mime" at "mime.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "atomdb.xqm" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace plugin = "http://purl.org/atombeat/xquery/plugin" at "../config/plugins.xqm" ;

declare variable $common-protocol:param-request-path-info := "request-path-info" ;
declare variable $common-protocol:logger-name := "org.atombeat.xquery.lib.common-protocol" ;


declare function local:debug(
    $message as item()*
) as empty()
{
    util:log-app( "debug" , $common-protocol:logger-name , $message )
};




declare function local:info(
    $message as item()*
) as empty()
{
    util:log-app( "info" , $common-protocol:logger-name , $message )
};

 


declare function common-protocol:do-not-modified(
    $request-path-info
) as element(response)
{

    <response>
        <status>{$CONSTANT:STATUS-REDIRECT-NOT-MODIFIED}</status>
        <headers/>
        <body/>
    </response>
    
};





declare function common-protocol:do-not-found(
    $request-path-info
) as element(response)
{

    let $message := "The server has not found anything matching the Request-URI."

    return
    
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

};



declare function common-protocol:do-precondition-failed(
    $request-path-info ,
    $message
) as element(response)
{

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

};



declare function common-protocol:do-bad-request(
	$request-path-info as xs:string ,
	$message as xs:string 
) as element(response)
{

    let $message := concat( $message , " The request could not be understood by the server due to malformed syntax. The client SHOULD NOT repeat the request without modifications." )

    return 
    
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

};





declare function common-protocol:do-method-not-allowed(
	$request-path-info as xs:string ,
	$allow as xs:string*
) as element(response)
{

    let $message := "The method specified in the Request-Line is not allowed for the resource identified by the Request-URI."

    return 
    
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
    
};

 



declare function common-protocol:do-forbidden(
	$request-path-info as xs:string
) as element(response)
{

    let $message := "The server understood the request, but is refusing to fulfill it. Authorization will not help and the request SHOULD NOT be repeated."

    return

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

};




declare function common-protocol:do-unsupported-media-type(
	$message as xs:string? ,
	$request-path-info as xs:string
) as element(response)
{

    let $message := concat( $message , " The server is refusing to service the request because the entity of the request is in a format not supported by the requested resource for the requested method." )

    return
    
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

};




declare function common-protocol:do-unsupported-media-type(
	$request-path-info as xs:string 
) as element(response)
{

    common-protocol:do-unsupported-media-type( $request-path-info , () )

};




(:
 : Main request processing function.
 :)
declare function common-protocol:apply-op(
	$op-name as xs:string ,
	$op as function ,
	$request-path-info as xs:string ,
	$request-data as item()*
) as element(response)
{

	common-protocol:apply-op( $op-name , $op , $request-path-info , $request-data , () )
	
};




(:
 : Main request processing function.
 :)
declare function common-protocol:apply-op(
	$op-name as xs:string ,
	$op as function ,
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string?
) as element(response)
{

	let $log := local:debug( "call plugin functions before main operation" )
	
	let $before-advice := common-protocol:apply-before( plugin:before() , $op-name , $request-path-info , $request-data , $request-media-type )
	
	let $log := local:debug( "done calling before plugins" )
	let $log := local:debug( $before-advice )
	
	return 
	 
		if ( $before-advice instance of element(response) ) (: interrupt request processing :)
		
		then 
		
			let $log := local:info( ( "bail out - plugin has overridden default behaviour, status: " , $before-advice/status ) )
		
			return $before-advice
		  
		else
		
			let $log := local:debug( "carry on as normal - execute main operation" )
			
			let $request-data := $before-advice (: request data may have been modified by plugins :)

			let $response := util:call( $op , $request-path-info , $request-data , $request-media-type )
			let $log := local:debug( "call plugin functions after main operation" ) 
			 
			let $after-advice := common-protocol:apply-after( plugin:after() , $op-name , $request-path-info , $response )
			
			let $response := $after-advice
					    
			return $response

};





(:
 : Recursively call the sequence of plugin functions.
 :)
declare function common-protocol:apply-before(
	$functions as function* ,
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string?
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
	
	then $request-data
	
	else
	
		let $advice := util:call( $functions[1] , $operation , $request-path-info , $request-data , $request-media-type )
		
		(: what happens next depends on advice :)
		
		return
		
			if ( $advice instance of element(response) )
			
			then $advice (: bail out, no further calling of before functions :)

			else 
			
			    let $request-data := $advice
			    
			    (: recursively call until before functions are exhausted :)
			    return common-protocol:apply-before( subsequence( $functions , 2 ) , $operation , $request-path-info , $request-data , $request-media-type )

};




(:
 : Recursively call the sequence of plugin functions.
 :)
declare function common-protocol:apply-after(
	$functions as function* ,
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$response as element(response)
) as item()* {
	
	if ( empty( $functions ) )
	
	then $response
	
	else
	
		let $advice := util:call( $functions[1] , $operation , $request-path-info , $response )
		
		let $response := $advice
		
		return
		
			common-protocol:apply-after( subsequence( $functions , 2 ) , $operation , $request-path-info , $response )

};




declare function common-protocol:do-internal-server-error(
	$request-path-info as xs:string ,
	$message as xs:string 
) as element(response)
{

    let $message := concat( $message , " The server encountered an unexpected condition which prevented it from fulfilling the request." )

    return 
    
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

};









declare function common-protocol:respond( $response as element(response) ) as item()*
{

    let $response := common-protocol:augment-errors( $response )
    
    let $log := local:debug( $response )
    
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
    
        if ( $response/body/@type = "media" )
        then
            let $binary-doc := atomdb:retrieve-media( $response/body/text() )
            return response:stream-binary( $binary-doc , $response/headers/header[name=$CONSTANT:HEADER-CONTENT-TYPE]/value/text() )
        else if ( $response/body/@type = "text" )
        then $response/body/text()
        else $response/body/*
        
};




declare function common-protocol:augment-errors(
	$response as element(response)
) as element(response)
{

    let $status := xs:integer( $response/status )
    let $content-type := $response/headers/header[name=$CONSTANT:HEADER-CONTENT-TYPE]/value/text()
	let $request-path-info := request:get-attribute( $common-protocol:param-request-path-info )
    
    let $log := util:log( "debug" , "== error-plugin:after ==" )
    let $log := util:log( "debug" , $status )
    let $log := util:log( "debug" , $content-type )
    

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
    	        <body>
            		<error>
            		    <status>{$status}</status>
            			<message>{$response/body/text()}</message>
            			<request>
                			<method>{request:get-method()}</method>
                			<path-info>{$request-path-info}</path-info>
                			<parameters>
                			{
                				for $parameter-name in request:get-parameter-names()
                				return
                				    <parameter>
                				        <name>{$parameter-name}</name>
                				        <value>{request:get-parameter( $parameter-name , "" )}</value>						
                					</parameter>
                			}
                			</parameters>
                			<headers>
                			{
                				for $header-name in request:get-header-names()
                				return
                				    <header>
                				        <name>{$header-name}</name>
                				        <value>{request:get-header( $header-name )}</value>						
                					</header>
                			}
                			</headers>
                			<user>{request:get-attribute($config:user-name-request-attribute-key)}</user>
                			<roles>{string-join(request:get-attribute($config:user-roles-request-attribute-key), " ")}</roles>
                        </request>    		
            		</error>
    	        </body>
    	    </response>
    	    
        else $response        

}; 



