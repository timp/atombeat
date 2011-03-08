xquery version "1.0";

module namespace logger-plugin = "http://purl.org/atombeat/xquery/logger-plugin";
declare namespace atom = "http://www.w3.org/2005/Atom" ;
import module namespace util = "http://exist-db.org/xquery/util" ;




declare function logger-plugin:before(
	$operation as xs:string ,
	$request as element(request) ,
	$entity as item()*
) as item()*
{

    let $request-path-info := $request/path-info/text()
	let $message := concat( "before: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "info" , $message )
	let $log := util:log( "info" , $request )
	let $log := util:log( "info" , $entity )
	
	return $entity
	
};



declare function logger-plugin:after(
	$operation as xs:string ,
	$request as element(request) ,
	$response as element(response)
) as item()*
{

    let $request-path-info := $request/path-info/text()
	let $message := concat( "after: " , $operation , ", request-path-info: " , $request-path-info ) 
	let $log := util:log( "info" , $message )
	let $log := util:log( "info" , $request )
	let $log := util:log( "info" , $response )
	
	return $response

}; 



