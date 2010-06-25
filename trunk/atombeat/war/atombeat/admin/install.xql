declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;

import module namespace response = "http://exist-db.org/xquery/response" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;
import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace atomsec = "http://purl.org/atombeat/xquery/atom-security" at "../lib/atom-security.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace atom-protocol = "http://purl.org/atombeat/xquery/atom-protocol" at "../lib/atom-protocol.xqm" ;
import module namespace common-protocol = "http://purl.org/atombeat/xquery/common-protocol" at "../lib/common-protocol.xqm" ;
import module namespace config-collections = "http://purl.org/atombeat/xquery/config-collections" at "collections.xqm" ;





declare function local:do-get() as item()*
{
    let $response-code-set := response:set-status-code( $CONSTANT:STATUS-SUCCESS-OK )
    let $response-content-type-set := response:set-header( $CONSTANT:HEADER-CONTENT-TYPE , "text/html" )
    return local:content()
};



declare function local:content() as item()*
{
    <html>
        <head>
            <title>AtomBeat Installation</title>
            <style type="text/css">
            
                th {{
                    text-align: left;
                }}
                
                table {{ 
                    border: 1px solid black;
                }}
                
                td {{ 
                    border: 1px solid black;
                    padding: .3em;
                }}
                
                th {{ 
                    border: 1px solid black;
                    padding: .3em;
                }}
                
                form {{
                    display: inline;
                }}
                
            </style>
        </head>
        <body>
            <h1>AtomBeat Installation</h1>
            <h2>Atom Collections</h2>
                <table>
                    <tr>
                        <th>Title</th>
                        <th>Path</th>
                        <th>Enable History</th>
                        <th>Exclude Entry Content in Feed</th>
                        <th>Expand Security Descriptors</th>
                        <th>Recursive</th>
                        <th>Available</th>
                    </tr>
                    {
                        for $collection in $config-collections:collection-spec/collection
                        let $title := $collection/title/text()
                        let $path-info := $collection/path-info/text()
                        let $enable-history := $collection/enable-history/text()
                        let $exclude-entry-content := $collection/exclude-entry-content/text()
                        let $expand-security-descriptors := $collection/expand-security-descriptors/text()
                        let $recursive := $collection/recursive/text()
                        let $available := atomdb:collection-available($path-info)
                        return
                            <tr>
                                <td>{$title}</td>
                                <td><a href="../content{$path-info}">{$path-info}</a></td>
                                <td>{$enable-history}</td>
                                <td>{$exclude-entry-content}</td>
                                <td>{$expand-security-descriptors}</td>
                                <td>{$recursive}</td>
                                <td><strong>{$available}</strong></td>
                            </tr>
                    }
                </table>
                <p>
                    <form method="post" action="">
                        <input type="submit" value="Install"></input>
                    </form>
                    <form method="get" action="">
                        <input type="submit" value="Refresh"></input>
                    </form>
                </p>
        </body>
    </html>
};



declare function local:do-post() as item()*
{

    (: INSTALL THE workspace ACL :)
    let $workspace-descriptor-installed := atomsec:store-workspace-descriptor( $config:default-workspace-security-descriptor )
    
    (: INSTALL THE COLLECTIONS :)
    let $collections-installed :=

        for $collection in $config-collections:collection-spec/collection
        let $title := $collection/title/text()
        let $path-info := $collection/path-info/text()

        (: CREATE THE COLLECTION :)
        let $feed-doc := 
            <atom:feed 
                atombeat:exclude-entry-content="{$collection/exclude-entry-content/text()}"
                atombeat:recursive="{$collection/recursive/text()}"
                atombeat:enable-versioning="{$collection/enable-history/text()}"
                atombeat:expand-security-descriptors="{$collection/expand-security-descriptors/text()}">
                <atom:title>{$title}</atom:title>
            </atom:feed>
            
        let $put-feed-response := atom-protocol:do-put-atom-feed( $path-info , $feed-doc )                                    
        return $put-feed-response
                
    (: SEND RESPONSE :)        
    let $response-code-set := response:set-status-code( $CONSTANT:STATUS-SUCCESS-OK )
    let $response-content-type-set := response:set-header( $CONSTANT:HEADER-CONTENT-TYPE , "text/html" )
    return local:content()
};




let $login := xmldb:login( "/" , "admin" , "" )

return 

    if ( request:get-method() = $CONSTANT:METHOD-GET ) 
    
    then local:do-get()
    
    else if ( request:get-method() = $CONSTANT:METHOD-POST ) 
    
    then local:do-post()
    
    else common-protocol:respond( common-protocol:do-method-not-allowed( "/admin/install.xql" , ( "GET" , "POST" ) ) )
    
    

