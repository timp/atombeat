xquery version "1.0";

module namespace service-protocol = "http://purl.org/atombeat/xquery/service-protocol";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace app = "http://www.w3.org/2007/app" ;
declare namespace xhtml = "http://www.w3.org/1999/xhtml" ;
declare namespace f = "http://purl.org/atompub/features/1.0" ;

import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "constants.xqm" ;
import module namespace common-protocol = "http://purl.org/atombeat/xquery/common-protocol" at "common-protocol.xqm" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;




declare function service-protocol:main() as item()*
{

    let $request := common-protocol:get-request()
    
    let $response := service-protocol:do-service( $request ) 
    
    return common-protocol:respond( $request , $response )

};




declare function service-protocol:do-service( 
    $request as element(request ) 
) as element(response) 
{

    if ( $request/method = $CONSTANT:METHOD-GET )
	
	then service-protocol:do-get( $request )
	
	else common-protocol:do-method-not-allowed( $CONSTANT:OP-ATOM-PROTOCOL-ERROR , $request , ( "GET" ) )

};




declare function service-protocol:do-get( 
    $request as element(request ) 
) as element(response) 
{

    (: 
     : Here we bottom out at the "retrieve-service" operation.
     :)

    let $op := util:function( QName( "http://purl.org/atombeat/xquery/service-protocol" , "service-protocol:op-retrieve-service" ) , 2 )
    
    return common-protocol:apply-op( $CONSTANT:OP-RETRIEVE-SERVICE , $op , $request , () )
    
};




(:
 : TODO doc me
 :)
declare function service-protocol:op-retrieve-service(
	$request as element(request) ,
	$entity as item()* (: expect this to be empty, but have to include to get consistent function signature :)
) as element(response)
{

    let $service :=

        <app:service 
            xmlns:app="http://www.w3.org/2007/app"
            xmlns:atom="http://www.w3.org/2005/Atom">
            <app:workspace>
            {
                if ( $config:workspace-title instance of xs:string )
                then <atom:title type="text">{$config:workspace-title}</atom:title>
                else if ( $config:workspace-title instance of element(xhtml:div) )
                then <atom:title type="xhtml">{$config:workspace-title}</atom:title>
                else <atom:title type="text">unnamed workspace</atom:title>
                ,
                if ( $config:workspace-summary instance of xs:string )
                then <atom:summary type="text">{$config:workspace-summary}</atom:summary>
                else if ( $config:workspace-summary instance of element(xhtml:div) )
                then <atom:summary type="xhtml">{$config:workspace-summary}</atom:summary>
                else ()            
            }
            {
                for $collection in collection( $config:base-collection-path )/atom:feed/app:collection
                where not( $collection/f:features/f:feature/@ref = 'http://purl.org/atombeat/feature/HiddenFromServiceDocument' )
                return $collection
            }
            </app:workspace>
        </app:service>
        
    let $response :=
    
        <response>
            <status>{$CONSTANT:STATUS-SUCCESS-OK}</status>
            <headers>
                <header>
                    <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                    <value>{$CONSTANT:MEDIA-TYPE-ATOMSVC}</value>
                </header>
            </headers>
            <body type='xml'>{$service}</body>
        </response>
        
    return $response

};

