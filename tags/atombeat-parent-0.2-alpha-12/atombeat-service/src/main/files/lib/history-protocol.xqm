xquery version "1.0";

module namespace history-protocol = "http://purl.org/atombeat/xquery/history-protocol";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;
declare namespace at = "http://purl.org/atompub/tombstones/1.0";

(: see http://tools.ietf.org/html/draft-snell-atompub-revision-00 :)
declare namespace ar = "http://purl.org/atompub/revision/1.0" ;

import module namespace request = "http://exist-db.org/xquery/request" ;
import module namespace response = "http://exist-db.org/xquery/response" ;
import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace v="http://exist-db.org/versioning" ;

import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;

import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace mime = "http://purl.org/atombeat/xquery/mime" at "../lib/mime.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace tombstone-db = "http://purl.org/atombeat/xquery/tombstone-db" at "../lib/tombstone-db.xqm" ;
import module namespace common-protocol = "http://purl.org/atombeat/xquery/common-protocol" at "../lib/common-protocol.xqm" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;

declare variable $history-protocol:param-name-revision-index as xs:string := "revision" ;


(: 
 : TODO media history
 :)
 
 


declare function history-protocol:main() as item()*
{

    let $request := common-protocol:get-request()
        
    (: process the request :)
    let $response := history-protocol:do-service( $request )
    
    (: return a response :)
    return common-protocol:respond( $request, $response )

};


 
 
(:
 : TODO doc me
 :)
declare function history-protocol:do-service(
    $request as element(request)
)
as element(response)
{

	if ( $request/method = $CONSTANT:METHOD-GET )
	
	then history-protocol:do-get( $request )
	
	else common-protocol:do-method-not-allowed( $CONSTANT:OP-HISTORY-PROTOCOL-ERROR , $request , ( "GET" ) )

};


 

(:
 : TODO doc me 
 :)
declare function history-protocol:do-get(
    $request as element(request)
) as element(response)
{

    let $request-path-info := $request/path-info/text() 
    
    return
    
    	if ( atomdb:member-available( $request-path-info ) or tombstone-db:tombstone-available( $request-path-info ) )
    	
    	then history-protocol:do-get-member( $request )
    	
    	else common-protocol:do-not-found( $CONSTANT:OP-HISTORY-PROTOCOL-ERROR , $request )
	
};




declare function history-protocol:do-get-member(
    $request as element(request)
) as element(response)
{

    let $revision-index := xutil:get-parameter( $history-protocol:param-name-revision-index , $request )
	
	return
	
		if ( $revision-index = "" or empty( $revision-index ) )
		
		then history-protocol:do-get-member-history( $request )
		
		else if ( $revision-index castable as xs:integer )
		
		then history-protocol:do-get-member-revision( $request , xs:integer( $revision-index ) )
		
		else common-protocol:do-bad-request( $CONSTANT:OP-HISTORY-PROTOCOL-ERROR , $request , "Revision index parameter must be an integer." )

};




(:
 : TODO doc me
 :)
declare function history-protocol:do-get-member-history(
    $request as element(request)
) as element(response)
{

    let $op := util:function( QName( "http://purl.org/atombeat/xquery/history-protocol" , "history-protocol:op-retrieve-member-history" ) , 2 )
    
    (: enable plugins to intercept the request :)
    return common-protocol:apply-op( $CONSTANT:OP-RETRIEVE-HISTORY , $op , $request , () )

};




declare function history-protocol:op-retrieve-member-history(
	$request as element(request) ,
	$entity as item()* (: expect this to be empty, but have to include to get consistent function signature :)
) as element(response)
{

    let $request-path-info := $request/path-info/text()
    let $self-uri := concat( $config:history-service-url , $request-path-info )
    let $versioned-uri := concat( $config:self-link-uri-base , $request-path-info )
    
    let $updated := 
        if ( atomdb:member-available( $request-path-info ) ) 
        then atomdb:retrieve-member( $request-path-info )/atom:updated
        else (: tombstone :)
            <atom:updated>{tombstone-db:retrieve-tombstone( $request-path-info )/@when cast as xs:string}</atom:updated>
    
    let $doc-path := concat( atomdb:request-path-info-to-db-path( $request-path-info ) , ".atom" )

    let $doc := doc( $doc-path )
    
(:    let $vhist := v:history( $doc ) :)
    
(:    let $vvers := v:versions( $doc ) :)

    let $revisions := v:revisions( $doc )
    
    let $feed := 

		<atom:feed atombeat:exclude-entry-content="true">
		    <atom:id>{$self-uri}</atom:id>
		    <atom:link rel="self" href="{$self-uri}" type="{$CONSTANT:MEDIA-TYPE-ATOM-FEED}"/>
		    <atom:link rel="http://purl.org/atombeat/rel/versioned" href="{$versioned-uri}" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}"/>
			<atom:title type="text">Version History</atom:title>
			{

                $updated ,
                
(:				$vhist , :)
				
(:				$vvers , :)
				
				for $i in 1 to ( count( $revisions ) + 1 )
				return history-protocol:construct-member-revision( $request , $doc , $i , $revisions , true() )
				
			}
		</atom:feed>

    return 
    
        <response>
            <status>{$CONSTANT:STATUS-SUCCESS-OK}</status>
            <headers>
                <header>
                    <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                    <value>{$CONSTANT:MEDIA-TYPE-ATOM-FEED}</value>
                </header>
            </headers>
            <body type='xml'>{$feed}</body>
        </response>
        
};




declare function history-protocol:do-get-member-revision(
	$request as element(request) ,
	$revision-index as xs:integer 
) as element(response)
{

    let $op := util:function( QName( "http://purl.org/atombeat/xquery/history-protocol" , "history-protocol:op-retrieve-member-revision" ) , 2 )
    
    (: enable plugins to intercept the request :)
    return common-protocol:apply-op( $CONSTANT:OP-RETRIEVE-REVISION , $op , $request , () )

};



declare function history-protocol:op-retrieve-member-revision(
	$request as element(request) ,
	$entity as item()* (: expect this to be empty, but have to include to get consistent function signature :)
) as element(response)
{

    let $request-path-info := $request/path-info/text()
    let $revision-index := xutil:get-parameter( $history-protocol:param-name-revision-index , $request ) cast as xs:integer
    let $entry-doc-path := concat( atomdb:request-path-info-to-db-path( $request-path-info ) , ".atom" )
    let $entry-doc := doc( $entry-doc-path )

	(: 
	 : N.B. we need to map from sequential revision index per entry (e.g., 1, 2, 3)
	 : to eXist's revision numbers (e.g., 6, 21, 27) which are per collection.
	 :)
	
	(: 
	 : Also note that we will index the base revision at 1, but eXist only returns
	 : revisions subsequent to the base, so we will have to map revision index 2 to 
	 : the first eXist revision.
	 :)
	
    let $revision-numbers as xs:integer* := v:revisions( $entry-doc )

    return 
    
        if ( $revision-index <= 0 )
        
        then common-protocol:do-bad-request( $CONSTANT:OP-HISTORY-PROTOCOL-ERROR , $request , "Revision index parameter must be an integer equal to or greater than 1." )
        
        else if ( $revision-index > ( count($revision-numbers) + 1 ) )
        
        then common-protocol:do-not-found( $CONSTANT:OP-HISTORY-PROTOCOL-ERROR , $request )
        
        else 
        
        	let $entry-revision := history-protocol:construct-member-revision( $request , $entry-doc , $revision-index , $revision-numbers , false() )

            return 
            
                <response>
                    <status>{$CONSTANT:STATUS-SUCCESS-OK}</status>
                    <headers>
                        <header>
                            <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
                            <value>
                            {
                                if ( $entry-revision instance of element(atom:entry) ) then $CONSTANT:MEDIA-TYPE-ATOM-ENTRY
                                else if ( $entry-revision instance of element(at:deleted-entry) ) then "application/atomdeleted+xml"
                                else "application/xml" (: just in case :)
                            }
                            </value>
                        </header>
                    </headers>
                    <body type='xml'>{$entry-revision}</body>
                </response>

};



declare function history-protocol:construct-member-revision(
	$request as element(request) ,
	$entry-doc as node() ,
	$revision-index as xs:integer ,
	$revision-numbers as xs:integer* ,
	$exclude-content as xs:boolean?
) as element(atom:entry)
{
    
    (: 
     : Handle revision index 1 in a special way
     :)
     
    if ( $revision-index = 1 )
    
    then history-protocol:construct-member-base-revision( $request , $entry-doc , $revision-numbers , $exclude-content )
    
    else history-protocol:construct-member-specified-revision( $request , $entry-doc , $revision-index , $revision-numbers , $exclude-content )
    
};




declare function history-protocol:construct-member-base-revision(
	$request as element(request) ,
	$entry-doc as node() ,
	$revision-numbers as xs:integer* ,
	$exclude-content as xs:boolean?
) as element(atom:entry)
{

    let $request-path-info := $request/path-info/text()

    (: 
     : N.B. if no updates on the doc yet, then base revision won't have been
     : created by eXist versioning module, so we'll just grab the head.
     :)

	let $revision := 
	    if ( empty( $revision-numbers ) ) then $entry-doc/atom:entry
	    else v:doc( $entry-doc , () )/atom:entry (: N.B. this only works with the AtomBeat patch to the versioning trigger :)

    let $when := $revision/atom:updated

	let $this-revision-href :=
		concat( $config:history-service-url , $request-path-info , "?" , $history-protocol:param-name-revision-index , "=" , xs:string( 1 ) )	

	let $next-revision-href :=
		if ( count( $revision-numbers ) > 0 ) 
		then concat( $config:history-service-url , $request-path-info , "?" , $history-protocol:param-name-revision-index , "=" , xs:string( 2 ) )	
		else ()

	let $current-revision-href :=
		concat( $config:self-link-uri-base , $request-path-info )

	let $initial-revision-href := $this-revision-href

	(: N.B. don't need history link because already in entry :)
	
	return 
		<atom:entry>
			<ar:revision 
				number="1"
				when="{$when}"
				initial="yes">
			</ar:revision>
			<atom:link rel="current-revision" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}" href="{$current-revision-href}"/>
			<atom:link rel="initial-revision" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}" href="{$initial-revision-href}"/>
			<atom:link rel="this-revision" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}" href="{$this-revision-href}"/>
		{
			if ( $next-revision-href ) then
			<atom:link rel="next-revision" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}" href="{$next-revision-href}"/> 
			else () ,
			for $ec in $revision/* 
			return 
				if ( local-name( $ec ) = $CONSTANT:ATOM-CONTENT and namespace-uri( $ec ) = $CONSTANT:ATOM-NSURI and $exclude-content )
				then <atom:content>{$ec/attribute::*}</atom:content>
				else $ec
		}
		</atom:entry>
    
};





declare function history-protocol:construct-member-specified-revision(
	$request as element(request) ,
	$entry-doc as node() ,
	$revision-index as xs:integer ,
	$revision-numbers as xs:integer* ,
	$exclude-content as xs:boolean?
) as element(atom:entry)
{ 

    let $request-path-info := $request/path-info/text()
    let $revision-number := $revision-numbers[$revision-index -1] 
	let $revision := v:doc( $entry-doc , $revision-number )
	
	return
	
	    if ( $revision instance of element(atom:entry) )
	    
	    then

            let $when := $revision/atom:updated
            
            let $initial := "no"
            
            let $this-revision-href :=
            	concat( $config:history-service-url , $request-path-info , "?" , $history-protocol:param-name-revision-index , "=" , xs:string( $revision-index ) )	
            
            let $next-revision-href :=
            	if ( $revision-index <= count( $revision-numbers ) ) 
            	then concat( $config:history-service-url , $request-path-info , "?" , $history-protocol:param-name-revision-index , "=" , xs:string( $revision-index + 1 ) )	
            	else ()
            
            let $previous-revision-href :=
            	if ( $revision-index > 1 ) 
            	then concat( $config:history-service-url , $request-path-info , "?" , $history-protocol:param-name-revision-index , "=" , xs:string( $revision-index - 1 ) )	
            	else ()
            	
            let $current-revision-href :=
            	concat( $config:self-link-uri-base , $request-path-info )
            
            let $initial-revision-href :=
            	concat( $config:history-service-url , $request-path-info , "?" , $history-protocol:param-name-revision-index , "=" , xs:string( 1 ) )	
            
            (: N.B. don't need history link because already in entry :)
            
            return 
            	<atom:entry>
            		<ar:revision 
            			number="{$revision-index}"
            			when="{$when}"
            			initial="{$initial}">
            		</ar:revision>
            		<atom:link rel="current-revision" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}" href="{$current-revision-href}"/>
            		<atom:link rel="initial-revision" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}" href="{$initial-revision-href}"/>
            		<atom:link rel="this-revision" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}" href="{$this-revision-href}"/>
            	{
            		if ( $next-revision-href ) then
            		<atom:link rel="next-revision" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}" href="{$next-revision-href}"/> 
            		else () ,
            		if ( $previous-revision-href ) then
            		<atom:link rel="previous-revision" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}" href="{$previous-revision-href}"/> 
            		else () ,
            		for $ec in $revision/child::* 
            		return 
            			if ( local-name( $ec ) = $CONSTANT:ATOM-CONTENT and namespace-uri( $ec ) = $CONSTANT:ATOM-NSURI and $exclude-content )
            			then <atom:content>{$ec/attribute::*}</atom:content>
            			else $ec
            	}
            	</atom:entry>
            	
         else if ( $revision instance of element(at:deleted-entry) ) (: deleted entry :)
         
         then
         
            let $when := $revision/@when cast as xs:string
            
            let $initial := "no"
            
            let $this-revision-href :=
                concat( $config:history-service-url , $request-path-info , "?" , $history-protocol:param-name-revision-index , "=" , xs:string( $revision-index ) ) 
            
            let $previous-revision-href :=
                if ( $revision-index > 1 ) 
                then concat( $config:history-service-url , $request-path-info , "?" , $history-protocol:param-name-revision-index , "=" , xs:string( $revision-index - 1 ) )    
                else ()
                
            let $current-revision-href :=
                concat( $config:self-link-uri-base , $request-path-info )
            
            let $initial-revision-href :=
                concat( $config:history-service-url , $request-path-info , "?" , $history-protocol:param-name-revision-index , "=" , xs:string( 1 ) )   
            
            return 
                <at:deleted-entry>
                    <ar:revision 
                        number="{$revision-index}"
                        when="{$when}"
                        initial="no"
                        final="yes"
                        significant="yes">
                    </ar:revision>
                    <atom:link rel="current-revision" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}" href="{$current-revision-href}"/>
                    <atom:link rel="initial-revision" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}" href="{$initial-revision-href}"/>
                    <atom:link rel="this-revision" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}" href="{$this-revision-href}"/>
                {
                    if ( $previous-revision-href ) then
                    <atom:link rel="previous-revision" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}" href="{$previous-revision-href}"/> 
                    else () ,
                    $revision/child::* 
                }
                </at:deleted-entry>
                
        else () (: just in case :)
         
};





