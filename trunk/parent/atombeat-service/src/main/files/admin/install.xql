declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;

import module namespace response = "http://exist-db.org/xquery/response" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace security-config = "http://purl.org/atombeat/xquery/security-config" at "../config/security.xqm" ;
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
            <title>AtomBeat - Pre-configured Collections</title>
            <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/combo?3.3.0/build/cssreset/reset-min.css&amp;3.3.0/build/cssfonts/fonts-min.css&amp;3.3.0/build/cssgrids/grids-min.css&amp;3.3.0/build/cssbase/base-min.css"/>
            <style type="text/css">
      body {{
        margin: auto;
        width: 960px;
      }}
      #icon {{
        float: right;
      }}
            </style>
        </head>
        <body>
          <div id="icon">
            <a href="http://code.google.com/p/atombeat/"><img src="http://farm6.static.flickr.com/5051/5415906232_a26853fd64_o.png" alt="AtomBeat logo"/></a>
          </div>
            <h1>AtomBeat - Pre-configured Collections</h1>
            <p>This page is a utility for managing pre-configured Atom collections.</p>
            <p>Note this page <strong>does not</strong> show all Atom collections available, only those declared explicitly in the service/admin/collections.xqm file.</p>
                <table>
                    <tr>
                        <th>Title</th>
                        <th>Path</th>
                        <th>Versioned</th>
                        <th>Recursive</th>
                        <th>Available</th>
                    </tr>
                    {
                        for $collection in $config-collections:collection-spec/collection
                        let $title := $collection/atom:feed/atom:title/text()
                        let $path-info := $collection/@path-info cast as xs:string
                        let $enable-versioning := $collection/atom:feed/@atombeat:enable-versioning cast as xs:string
                        let $exclude-entry-content := $collection/atom:feed/@atombeat:exclude-entry-content cast as xs:string
                        let $recursive := $collection/atom:feed/@atombeat:recursive cast as xs:string
                        let $available := atomdb:collection-available($path-info)
                        return
                            <tr>
                                <td>{$title}</td>
                                <td><a href="../content{$path-info}">{$path-info}</a></td>
                                <td>{$enable-versioning}</td>
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
    let $workspace-descriptor-installed := atomsec:store-workspace-descriptor( $security-config:default-workspace-security-descriptor )
    
    (: INSTALL THE COLLECTIONS :)
    let $collections-installed :=

        for $collection in $config-collections:collection-spec/collection
        let $path-info := $collection/@path-info cast as xs:string

        (: CREATE THE COLLECTION :)
        let $feed := $collection/atom:feed
        let $put-feed-response := atom-protocol:do-put-atom-feed( $path-info , $feed )                                    
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
    
    else common-protocol:respond( common-protocol:do-method-not-allowed( "/admin/install.xql" , "/admin/install.xql" , ( "GET" , "POST" ) ) )
    
    

