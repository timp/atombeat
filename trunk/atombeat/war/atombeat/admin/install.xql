declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;

import module namespace response = "http://exist-db.org/xquery/response" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;
import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace atomsec = "http://purl.org/atombeat/xquery/atom-security" at "../lib/atom-security.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace ap = "http://purl.org/atombeat/xquery/atom-protocol" at "../lib/atom-protocol.xqm" ;



declare variable $collection-spec :=
    <spec>
        <collection>
            <title>Foo</title>
            <path-info>/foo</path-info>
            <enable-history>false</enable-history>
            <exclude-entry-content>false</exclude-entry-content>
        </collection>   
        <collection>
            <title>DataWiki</title>
            <path-info>/datawiki</path-info>
            <enable-history>true</enable-history>
            <exclude-entry-content>true</exclude-entry-content>
        </collection>   
    </spec>
;



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
                        <th>Available</th>
                    </tr>
                    {
                        for $collection in $collection-spec/collection
                        let $title := $collection/title/text()
                        let $path-info := $collection/path-info/text()
                        let $enable-history := $collection/enable-history/text()
                        let $exclude-entry-content := $collection/exclude-entry-content/text()
                        let $available := atomdb:collection-available($path-info)
                        return
                            <tr>
                                <td>{$title}</td>
                                <td><a href="../content{$path-info}">{$path-info}</a></td>
                                <td>{$enable-history}</td>
                                <td>{$exclude-entry-content}</td>
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
        for $collection in $collection-spec/collection
        let $title := $collection/title/text()
        let $path-info := $collection/path-info/text()
        let $enable-history := xs:boolean($collection/enable-history/text())
        return 
            if ( not( atomdb:collection-available( $path-info ) ) )
            then 
            
                (: CREATE THE COLLECTION :)
                let $feed-doc := 
                    <atom:feed atombeat:exclude-entry-content="{$collection/exclude-entry-content/text()}">
                        <atom:title>{$title}</atom:title>
                    </atom:feed>
                let $collection-created := atomdb:create-collection( $path-info , $feed-doc )
                let $collection-db-path := atomdb:request-path-info-to-db-path( $path-info )
                
                (: INSTALL ACL :)
                let $acl := config:default-collection-security-descriptor( $path-info , () )
                let $acl-stored := atomsec:store-collection-descriptor( $path-info , $acl )
                
                (: ENABLE HISTORY :)
                let $history-enabled :=
                    if ( $enable-history )
                    then xutil:enable-versioning( $collection-db-path )
                    else false()
                    
                return $collection-db-path
            else ()
            
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
    
    else ap:do-method-not-allowed( "/admin/install.xql" , ( "GET" , "POST" ) )
    
    

