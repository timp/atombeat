xquery version "1.0";

module namespace unzip-config = "http://purl.org/atombeat/xquery/unzip-config";

(: XML namespace declarations :)
declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace app = "http://www.w3.org/2007/app" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;
declare namespace f = "http://purl.org/atompub/features/1.0" ;

(: eXist function module imports :)
import module namespace util = "http://exist-db.org/xquery/util" ;

(: AtomBeat function module imports :)
import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;




declare function unzip-config:feed-template(
    $collection-path-info as xs:string ,
    $entry as element(atom:entry)
) as element(atom:feed)
{
    <atom:feed
        atombeat:enable-versioning="false"
        atombeat:exclude-entry-content="false"
        atombeat:recursive="false"
        atombeat:enable-tombstones="false">
        <atom:title type='text'>Unzipped Entries for: {$entry/atom:title/string()}</atom:title>
        <app:collection>
		    <f:features xmlns:f="http://purl.org/atompub/features/1.0">
			    <f:feature ref="http://purl.org/atombeat/feature/HiddenFromServiceDocument"/>
			</f:features>
        </app:collection>
    </atom:feed>
};