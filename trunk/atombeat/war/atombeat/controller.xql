xquery version "1.0";

import module namespace request="http://exist-db.org/xquery/request";
import module namespace text="http://exist-db.org/xquery/text";
import module namespace xmldb = "http://exist-db.org/xquery/xmldb";

let $groups := text:groups( $exist:path , "^/([^/]+)(.*)$" )
let $module := $groups[2]
let $request-path-info := $groups[3]

return

	if ( $module = "content" ) then

		<dispatch xmlns="http://exist.sourceforge.net/NS/exist">
		    <forward url="/atombeat/content.xql">
		        <set-attribute name="request-path-info" value="{$request-path-info}"/>
		    </forward>
		</dispatch>

	else if ( $module = "history" ) then

		<dispatch xmlns="http://exist.sourceforge.net/NS/exist">
		    <forward url="/atombeat/history.xql">
		        <set-attribute name="request-path-info" value="{$request-path-info}"/>
		    </forward>
		</dispatch>

	else if ( $module = "acl" ) then

		<dispatch xmlns="http://exist.sourceforge.net/NS/exist">
		    <forward url="/atombeat/acl.xql">
		        <set-attribute name="request-path-info" value="{$request-path-info}"/>
		    </forward>
		</dispatch>

    else if ( $module = "expansion" ) then

        <dispatch xmlns="http://exist.sourceforge.net/NS/exist">
            <forward url="/atombeat/expansion.xql">
                <set-attribute name="request-path-info" value="{$request-path-info}"/>
            </forward>
        </dispatch>

	else 

		<ignore xmlns="http://exist.sourceforge.net/NS/exist">
            <cache-control cache="yes"/>
		</ignore>
	