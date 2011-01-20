xquery version "1.0";

module namespace conneg-config = "http://purl.org/atombeat/xquery/conneg-config";

(: XML namespace declarations :)
declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;

(: eXist function module imports :)
import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace transform = "http://exist-db.org/xquery/transform" ;
import module namespace json = "http://www.json.org" ;

(: AtomBeat function module imports :)
import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;




(:~
 : Define alternate representations.
 :)
declare variable $conneg-config:alternates := 
    <alternates>
        <alternate>
            <output-key>html</output-key>
            <media-type>text/html</media-type>
            <output-type>xml</output-type>
        </alternate>
        <alternate>
            <output-key>json</output-key>
            <media-type>application/json</media-type>
            <output-type>text</output-type>
        </alternate>
        <alternate>
            <output-key>atom</output-key>
            <media-type>application/atom+xml</media-type>
            <output-type>xml</output-type>
        </alternate>
        <alternate>
            <output-key>xml</output-key>
            <media-type>application/xml</media-type>
            <output-type>xml</output-type>
        </alternate>
    </alternates>
;



(:~
 : Define transformers for alternate representations. There must be one transformer
 : for each alternate, and they must occur in the same position within the sequence
 : as the corresponding alternate definition above.
 :)
declare variable $conneg-config:transformers := (
    <stylesheet>/stylesheets/atom2html4.xslt</stylesheet> , (: will be concatenated with $config:service-url-base :)
    util:function( QName( "http://www.json.org" , "json:xml-to-json" ) , 1 ) , (: if you use a function as a transformer, then the function's module MUST be imported into this module, see imports at the top of this file :)
    util:function( QName( "http://purl.org/atombeat/xquery/xutil" , "xutil:identity" ) , 1 ) ,
    util:function( QName( "http://purl.org/atombeat/xquery/xutil" , "xutil:identity" ) , 1 ) 
);




(:
declare function conneg-config:legacy(
    $output-key as xs:string ,
    $response as element(response)
) as item()*

{

    (: map output key to transformation :)
    
    if ( $output-key eq "html" and exists( $response/body/atom:entry ) ) then 
    
        let $entry := $response/body/atom:entry
        let $stylesheet := xs:anyURI( concat( $config:service-url-base , "/stylesheets/atom2html4.xslt" ) )
        let $html := transform:transform( $entry , $stylesheet , () )
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
                                <value>text/html</value>
                            </header>

                        (: override content-location header :)
                        else if ( $header/name eq "Content-Location" ) then
                            <header>
                                <name>Content-Location</name>
                                <value>{concat( $header/value , if ( contains( $header/value , "?" ) ) then "&amp;" else "?" , "output=html")}</value>
                            </header>
                            
                        (: pass through all other headers :)
                        else $header
                }
                </headers>
                <body>{$html}</body>
            </response>
            
    else if ( $output-key eq "json" ) then
    
        let $json := json:xml-to-json( $response/body/* )
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
                                <value>application/json</value>
                            </header>
    
                        (: override content-location header :)
                        else if ( $header/name eq "Content-Location" ) then
                            <header>
                                <name>Content-Location</name>
                                <value>{concat( $header/value , if ( contains( $header/value , "?" ) ) then "&amp;" else "?" , "output=json")}</value>
                            </header>
                            
                        (: pass through all other headers :)
                        else $header
                }
                </headers>
                <body type="text">{$json}</body>
            </response>
            
    else if ( $output-key eq "xml" ) then 
    
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
                            <value>application/xml</value>
                        </header>

                    (: override content-location header :)
                    else if ( $header/name eq "Content-Location" ) then
                        <header>
                            <name>Content-Location</name>
                            <value>{concat( $header/value , if ( contains( $header/value , "?" ) ) then "&amp;" else "?" , "output=xml")}</value>
                        </header>
                        
                    (: pass through all other headers :)
                    else $header
            }
            </headers>
            {$response/body} (: pass through as-is :)
        </response>
            
    else if ( $output-key eq "atom" ) then 
    
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
                            <value>application/atom+xml</value>
                        </header>

                    (: override content-location header :)
                    else if ( $header/name eq "Content-Location" ) then
                        <header>
                            <name>Content-Location</name>
                            <value>{concat( $header/value , if ( contains( $header/value , "?" ) ) then "&amp;" else "?" , "output=atom")}</value>
                        </header>
                        
                    (: pass through all other headers :)
                    else $header
            }
            </headers>
            {$response/body} (: pass through as-is :)
        </response>
            
    else ()
    
};
:)