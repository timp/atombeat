xquery version "1.0";

module namespace unzip-plugin = "http://purl.org/atombeat/xquery/unzip-plugin";
declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace app = "http://www.w3.org/2007/app" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns";
import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;
import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace unzip-config = "http://purl.org/atombeat/xquery/unzip-config" at "../config/unzip.xqm" ;
import module namespace mime = "http://purl.org/atombeat/xquery/mime" at "../lib/mime.xqm" ;
import module namespace atombeat-util = "http://purl.org/atombeat/xquery/atombeat-util" at "java:org.atombeat.xquery.functions.util.AtomBeatUtilModule";




declare function unzip-plugin:before(
	$operation as xs:string ,
	$request as element(request) ,
	$entity as item()*
) as item()*
{
    let $request-path-info := $request/path-info/text()
	let $message := concat( "unzip-plugin:before... " , $operation , ", request-path-info: " , $request-path-info ) 
	return
        if ( $entity instance of element(atom:entry) ) then local:filter-entry( $entity )
        else if ( $entity instance of element(atom:feed) ) then local:filter-feed( $entity )
        else if ( empty( $entity ) ) then <void/>
        else $entity
};




declare function local:filter-entry(
    $entry as element(atom:entry)
) as element(atom:entry)
{

    let $reserved :=
        <reserved>
            <atom-links>
                <link rel="http://purl.org/atombeat/rel/package-members"/>
                <link rel="http://purl.org/atombeat/rel/member-of-package"/>
                <link rel="http://purl.org/atombeat/rel/member-of-package-media"/>
            </atom-links>
        </reserved>
    
    let $filtered-entry := atomdb:filter( $entry , $reserved )
    
    return $filtered-entry

};




declare function local:filter-feed(
    $feed as element(atom:feed)
) as element(atom:feed)
{
    <atom:feed>
    {
        $feed/attribute::*,
        $feed/child::*[not(. instance of element(atom:entry))],
        for $entry in $feed/atom:entry return local:filter-entry($entry)
    }
    </atom:feed>
};




declare function unzip-plugin:after(
	$operation as xs:string ,
	$request as element(request) ,
	$response as element(response)
) as item()*
{

    let $request-path-info := $request/path-info/text()
	let $message := concat( "unzip-plugin:after... " , $operation , ", request-path-info: " , $request-path-info ) 
	
	return
	
	    if ($config:media-storage-mode eq 'FILE') then
	
        	let $side-effects := 
        	    if ( $operation = $CONSTANT:OP-CREATE-MEDIA ) then local:create-media-side-effects($request, $response)
        	    else if ( $operation = $CONSTANT:OP-DELETE-MEDIA ) then local:delete-media-side-effects($request, $response)
        	    else true()
        	
        	return 
        	    
        	    if ( $operation = (   
        	            $CONSTANT:OP-LIST-COLLECTION ,
                        $CONSTANT:OP-CREATE-MEMBER ,
                        $CONSTANT:OP-RETRIEVE-MEMBER ,
                        $CONSTANT:OP-UPDATE-MEMBER , 
                        $CONSTANT:OP-CREATE-MEDIA ,
                        $CONSTANT:OP-UPDATE-MEDIA 
        	    ) ) then local:augment-response($response)
        	    else $response
        	    
        else $response

}; 



declare function local:create-media-side-effects(
	$request as element(request) ,
	$response as element(response)
) as xs:boolean
{
    let $entry := $response/body/atom:entry
    let $entry-path-info := substring-after($entry/atom:link[@rel='edit']/@href/string(), $config:edit-link-uri-base)
	let $collection-path-info := let $entry-path-info := substring-after($entry/atom:link[@rel='edit']/@href/string(), $config:edit-link-uri-base) return text:groups($entry-path-info, "^(.+)/[^/]+$")[2]
    let $feed := atomdb:retrieve-feed-without-entries( $collection-path-info )
    return 
        if (exists($feed/@atombeat:enable-unzip) and $feed/@atombeat:enable-unzip castable as xs:boolean and $feed/@atombeat:enable-unzip cast as xs:boolean eq true() and $entry/atom:link[@rel='edit-media']/@type eq 'application/zip') then local:unzip($request, $response)
        else true()
    
};




declare function local:unzip(
	$request as element(request) ,
	$response as element(response)
) as xs:boolean
{
    let $entry := $response/body/atom:entry
    let $entry-path-info := substring-after($entry/atom:link[@rel='edit']/@href/string(), $config:edit-link-uri-base)
    let $unzip-collection-path-info := concat($entry-path-info, "/unzip")
    let $feed-template := unzip-config:feed-template($unzip-collection-path-info, $entry)
    let $unzip-feed :=
        <atom:feed>
        {
            $feed-template/attribute::*,
            $feed-template/child::*[not(. instance of element(app:collection))]
        }
			<app:collection xmlns:app="http://www.w3.org/2007/app">
            {
                $feed-template/app:collection/attribute::*,
                $feed-template/app:collection/child::*[not(. instance of element(app:accept))]
            }
                <app:accept/>
            </app:collection>
        </atom:feed>
    let $unzip-collection-created := atomdb:create-collection($unzip-collection-path-info, $unzip-feed, $request/user/string())
    let $zip-file-path := concat($config:media-storage-dir, $entry-path-info, ".media")
    let $zip-entries := atombeat-util:get-zip-entries($zip-file-path)
    let $zip-members-created :=
        for $zip-entry in $zip-entries
        let $unzip-url := concat($config:service-url-base, '/unzip', $entry-path-info, ".media?zip-entry=", $zip-entry)
        let $title := $zip-entry
        let $summary := 'zip file entry'
        let $extension := text:groups( $zip-entry , "\.([^.]+)$" )[2]
        let $media-type := $mime:mappings//mime-mapping[extension=$extension]/mime-type
        let $media-type := if ( empty( $media-type ) ) then "application/octet-stream" else $media-type
        let $user := $request/user/string()
        let $size := atombeat-util:get-zip-entry-size($zip-file-path, $zip-entry)
        let $hash := concat("crc-32:", atombeat-util:get-zip-entry-crc($zip-file-path, $zip-entry) cast as xs:string)
        let $links := (
            <atom:link rel="http://purl.org/atombeat/rel/member-of-package" type="application/atom+xml;type=entry" href="{$entry/atom:link[@rel='edit']/@href/string()}"/>,
            <atom:link rel="http://purl.org/atombeat/rel/member-of-package-media" type="{$entry/atom:link[@rel='edit-media']/@type/string()}" href="{$entry/atom:link[@rel='edit-media']/@href/string()}"/>
        )
        return atomdb:create-virtual-media-resource($unzip-collection-path-info, $unzip-url, $media-type, $size, $hash, $user, $title, $summary, (), $links)
    return exists($unzip-collection-created)
};




declare function local:delete-media-side-effects(
	$request as element(request) ,
	$response as element(response)
) as xs:boolean
{

    let $request-path-info := $request/path-info/string()
    
    let $member-path-info :=
        if ( ends-with( $request-path-info , ".media" ) )
        then replace( $request-path-info , "^(.*)\.media$" , "$1" )
        else $request-path-info

    let $collection-path-info := concat($member-path-info, '/unzip')
    
    let $collection-deleted := atomdb:delete-collection($collection-path-info, true())
    
    return exists($collection-deleted)
    
};





declare function local:augment-response(
    $response as element(response)
) as element(response) {

    let $entity := $response/body/*
    
    let $augmented-body :=
        if ( $entity instance of element(atom:feed) )
        then <body type='xml'>{local:augment-feed($entity)}</body>
        else if ( $entity instance of element(atom:entry) )
        then <body type='xml'>{local:augment-entry($entity)}</body>
        else $response/body
        
    let $modified-response :=
        <response>
        {
            $response/status ,
            $response/headers ,
            $augmented-body
        }
        </response>

    return $modified-response
    
};




declare function local:augment-feed(
    $feed as element(atom:feed) 
) as element(atom:feed)
{

    if (exists($feed/@atombeat:enable-unzip) and $feed/@atombeat:enable-unzip castable as xs:boolean and $feed/@atombeat:enable-unzip cast as xs:boolean eq true()) then
        <atom:feed>
        {
            $feed/attribute::* ,
            $feed/child::*[ not( . instance of element(atom:entry) ) ] ,
            for $entry in $feed/atom:entry return local:augment-entry($entry, $feed)
        }        
        </atom:feed>
    else $feed
    
};



declare function local:augment-entry(
    $entry as element(atom:entry)
) as element(atom:entry)
{
    let $entry-path-info := substring-after($entry/atom:link[@rel='edit']/@href/string(), $config:edit-link-uri-base)
	let $collection-path-info := let $entry-path-info := substring-after($entry/atom:link[@rel='edit']/@href/string(), $config:edit-link-uri-base) return text:groups($entry-path-info, "^(.+)/[^/]+$")[2]
    let $feed := atomdb:retrieve-feed-without-entries( $collection-path-info )
    return local:augment-entry($entry, $feed)
};



declare function local:augment-entry(
    $entry as element(atom:entry) ,
    $feed as element(atom:feed) 
) as element(atom:entry)
{
    if (exists($feed/@atombeat:enable-unzip) and $feed/@atombeat:enable-unzip castable as xs:boolean and $feed/@atombeat:enable-unzip cast as xs:boolean eq true() and $entry/atom:link[@rel='edit-media']/@type eq 'application/zip') then
        <atom:entry>
        {
            $entry/attribute::*,
            $entry/child::*,
            <atom:link rel="http://purl.org/atombeat/rel/package-members" type="application/atom+xml;type=feed" href="{$entry/atom:link[@rel='edit']/@href}/unzip"/>
        }        
        </atom:entry>
    else $entry
};

