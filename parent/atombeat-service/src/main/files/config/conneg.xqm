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
 : Define variant representations.
 :)
declare variable $conneg-config:variants := 
    <variants>
        <variant>
            <output-key>atom</output-key>
            <media-type>application/atom+xml</media-type>
            <output-type>xml</output-type>
            <qs>0.8</qs>
        </variant>
        <variant>
            <output-key>html</output-key>
            <media-type>text/html</media-type>
            <output-type>xml</output-type>
            <qs>0.9</qs>
        </variant>
        <variant>
            <output-key>json</output-key>
            <media-type>application/json</media-type>
            <output-type>text</output-type>
            <qs>0.5</qs>
        </variant>
        <variant>
            <output-key>xml</output-key>
            <media-type>application/xml</media-type>
            <output-type>xml</output-type>
            <qs>0.3</qs>
        </variant>
        <variant>
            <output-key>textxml</output-key>
            <media-type>text/xml</media-type>
            <output-type>xml</output-type>
            <qs>0.2</qs>
        </variant>
        <variant>
            <output-key>text</output-key>
            <media-type>text/plain</media-type>
            <output-type>xml</output-type>
            <qs>0.1</qs>
        </variant>
    </variants>
;



(:~
 : Define transformers for variant representations. There MUST be one transformer
 : for each variant, and they must occur in the same position within the sequence
 : as the corresponding variant definition above.
 :)
declare variable $conneg-config:transformers := (
    <identity/> ,
    <stylesheet>/stylesheets/atom2html4.xslt</stylesheet> , (: will be concatenated with $config:service-url-base :)
    util:function( QName( "http://www.json.org" , "json:xml-to-json" ) , 1 ) , (: if you use a function as a transformer, then the function's module MUST be imported into this module, see imports at the top of this file :)
    <identity/> ,
    <identity/> ,
    <identity/> 
);



