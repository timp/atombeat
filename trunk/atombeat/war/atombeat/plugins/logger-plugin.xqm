xquery version "1.0";

module namespace logger-plugin = "http://atombeat.org/xquery/logger-plugin";
declare namespace atom = "http://www.w3.org/2005/Atom" ;
import module namespace util = "http://exist-db.org/xquery/util" ;




declare function logger-plugin:before(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$request-data as item()* ,
	$request-media-type as xs:string?
) as item()*
{
	let $message := concat( "before: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "info" , $message )
	
	let $status-code := 0 (: we don't want to interrupt request processing :)
	return ( $status-code , $request-data )
	
};



declare function logger-plugin:after(
	$operation as xs:string ,
	$request-path-info as xs:string ,
	$response-data as item()* ,
	$content-type as xs:string?
) as item()*
{
	let $message := concat( "after: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "info" , $message )
	
	(: pass response data and content type through, we don't want to modify response :)
	return ( $response-data , $content-type )
}; 



