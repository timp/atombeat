xquery version "1.0";

module namespace unzip-protocol = "http://purl.org/atombeat/xquery/unzip-protocol";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;

import module namespace request = "http://exist-db.org/xquery/request" ;
import module namespace response = "http://exist-db.org/xquery/response" ;
import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;

import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace mime = "http://purl.org/atombeat/xquery/mime" at "../lib/mime.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace common-protocol = "http://purl.org/atombeat/xquery/common-protocol" at "../lib/common-protocol.xqm" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace atombeat-util = "http://purl.org/atombeat/xquery/atombeat-util" at "java:org.atombeat.xquery.functions.util.AtomBeatUtilModule";
 
 
 
 
declare function unzip-protocol:main() as item()*
{
    let $request := common-protocol:get-request()
    let $response := unzip-protocol:do-service( $request )
    return common-protocol:respond( $request , $response )
};




(:
 : TODO doc me
 :)
declare function unzip-protocol:do-service(
    $request as element(request)
)
as element(response)
{

	let $request-path-info := $request/path-info/text()
	let $request-method := $request/method/text()
	
	return
	
		if ( $request-method = $CONSTANT:METHOD-GET and $config:media-storage-mode eq "FILE" )
		
		then unzip-protocol:do-get( $request )
		
		else common-protocol:do-method-not-allowed( $CONSTANT:OP-SECURITY-PROTOCOL-ERROR , $request , ( "GET" , "PUT" ) )

};



(:
 : TODO doc me 
 :)
declare function unzip-protocol:do-get(
    $request as element(request)
) as element(response)
{

    let $request-path-info := $request/path-info/string() 
    let $op-name := $CONSTANT:OP-RETRIEVE-MEDIA
    let $available := atomdb:media-resource-available($request-path-info)
    let $entry-param := xutil:get-parameter("entry", $request)
    return    
        if (not($available)) then common-protocol:do-not-found("UNZIP_PROTOCOL_ERROR" , $request)
        else if (empty($entry-param)) then common-protocol:do-bad-request("UNZIP_PROTOCOL_ERROR", $request, "expected 'entry' URL query parameter, found none")
        else
            let $op := util:function( QName( "http://purl.org/atombeat/xquery/unzip-protocol" , "unzip-protocol:op-retrieve-media" ) , 2 )
            return common-protocol:apply-op( $op-name , $op , $request , () )

};




declare function unzip-protocol:op-retrieve-media(
	$request as element(request) ,
	$entity as item()* (: expect this to be empty, but have to include to get consistent function signature :)
) as element(response)
{

    let $request-path-info := $request/path-info/string()
    let $entry-param := xutil:get-parameter("entry", $request)
    let $file-path := concat($config:media-storage-dir, $request-path-info)
    let $zip-entries := atombeat-util:get-zip-entries($file-path)
    return
        if ($entry-param = $zip-entries) then
            let $mime-type := atomdb:get-mime-type($request-path-info)
            let $title := text:groups($entry-param, "([^/]+)$")[2]
            let $content-disposition-header :=
                if ( $title ) then 
                    <header>
                        <name>{$CONSTANT:HEADER-CONTENT-DISPOSITION}</name>
                        <value>{concat( 'attachment; filename="' , $title , '"' )}</value>
                    </header>
                else ()
            return 
                <response>
                    <status>200</status>
                    <headers>
                        <header>
                            <name>Content-Type</name>
                            <value>{$mime-type}</value>
                        </header>
                    {
                        $content-disposition-header
                    }
                    </headers>
                    <body type="unzip-media" zip-entry="{$entry-param}">{$request-path-info}</body>
                </response>
                
        else
            <response>
                <status>404</status>
                <headers>
                    <header>
                        <name>Content-Type</name>
                        <value>text/plain</value>
                    </header>
                </headers>
                <body type='text'>zip entry not found in zip archive</body>
            </response>

};






