xquery version "1.0";

import module namespace request="http://exist-db.org/xquery/request";
import module namespace text="http://exist-db.org/xquery/text";
import module namespace xmldb = "http://exist-db.org/xquery/xmldb";

let $groups := text:groups( $exist:path , "^/([^/]+)(.*)$" )
let $module := $groups[2]
let $request-path-info := $groups[3]
let $service-path := "/service"

return

	if ( $module = "content" ) then

		<dispatch xmlns="http://exist.sourceforge.net/NS/exist">
		    <forward url="{$service-path}/content.xql">
		        <set-attribute name="request-path-info" value="{$request-path-info}"/>
		    </forward>
		</dispatch>

	else if ( $module = "history" ) then

		<dispatch xmlns="http://exist.sourceforge.net/NS/exist">
		    <forward url="{$service-path}/history.xql">
		        <set-attribute name="request-path-info" value="{$request-path-info}"/>
		    </forward>
		</dispatch>

	else if ( $module = "security" ) then

		<dispatch xmlns="http://exist.sourceforge.net/NS/exist">
		    <forward url="{$service-path}/security.xql">
		        <set-attribute name="request-path-info" value="{$request-path-info}"/>
		    </forward>
		</dispatch>
		
    else if ( empty( $module ) or $module = "" ) then

        <dispatch xmlns="http://exist.sourceforge.net/NS/exist">
            <forward url="{$service-path}/service.xql">
            </forward>
        </dispatch>
        
	else 

		<ignore xmlns="http://exist.sourceforge.net/NS/exist">
            <cache-control cache="yes"/>
		</ignore>
	