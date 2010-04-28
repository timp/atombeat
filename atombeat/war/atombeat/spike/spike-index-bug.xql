xquery version "1.0";

declare namespace foo = "http://example.org/foo";
declare namespace atom = "http://www.w3.org/2005/Atom";




declare function foo:do-retrieve-entry(
    $path as xs:string
) as item()*
{

    let $f := $foo:op-retrieve-entry

    let $result := util:call( $f , $path )
    let $set-status-code := response:set-status-code( $result[1] )
    let $set-content-type := response:set-header ( "Content-Type" , $result[3] )
    return $result[2]
        
};



declare function foo:op-retrieve-entry(
    $path as xs:string
) as item()*
{
    let $entry-doc := doc($path)
    let $entry := $entry-doc/atom:entry
    return ( 200 , $entry , "text/plain" ) 
};



declare variable $foo:op-retrieve-entry as function := util:function( QName( "http://example.org/foo" , "foo:op-retrieve-entry" ) , 1 ) ;



declare function foo:do-retrieve-entry-dynamic(
    $path as xs:string
) as item()*
{

    let $f := util:function( QName( "http://example.org/foo" , "foo:op-retrieve-entry" ) , 1 )
    
    let $result := util:call( $f , $path )
    let $set-status-code := response:set-status-code( $result[1] )
    let $set-content-type := response:set-header ( "Content-Type" , $result[3] )
    return $result[2]
        
};



declare function foo:do-store-entry(
    $path as xs:string , 
    $entry as element(atom:entry)
) as item()*
{

    let $groups := text:groups( $path , "^(.*)/([^/]+)$" )
    let $collection-path := $groups[2]
    let $resource-name := $groups[3]
    let $stored := xmldb:store( $collection-path , $resource-name , $entry , "application/atom+xml" )
    let $set-status-code := response:set-status-code( 200 )
    let $set-content-type := response:set-header( "Content-Type" , "text/plain" )
    return "ok"
        
};



declare function foo:do-bootstrap-with-versioning() as item()*
{

    let $collection-path := "/db/test"
    let $config-collection-path := concat( "/db/system/config" , "/db/test" )
    let $versions-collection-path := concat( "/db/system/versions" , "/db/test" )
    
    let $clean :=
        for $p in ( $collection-path , $config-collection-path , $versions-collection-path )
        return
            if (xmldb:collection-available( $p )) then xmldb:remove( $p ) else ()
            
    let $collection-config :=

        <collection xmlns="http://exist-db.org/collection-config/1.0">
            <triggers>
                <trigger event="store,remove,update" class="org.exist.versioning.VersioningTrigger">
                    <parameter name="overwrite" value="yes"/>
                </trigger>
            </triggers>
        </collection>
        
    let $base-config-collection-created := xmldb:create-collection( "/db/system/config" , "db" )
    let $config-collection-created := xmldb:create-collection( "/db/system/config/db" , "test" )
    let $config-stored := xmldb:store( $config-collection-path , "collection.xconf" , $collection-config , "application/xml" )
    
    let $collection-created := xmldb:create-collection( "/db" , "test" )

    let $set-status-code := response:set-status-code( 200 )
    let $set-content-type := response:set-header( "Content-Type" , "text/plain" )
    return "ok"
        
};




declare function foo:do-bootstrap-without-versioning() as item()*
{

    let $collection-path := "/db/test"
    let $config-collection-path := concat( "/db/system/config" , "/db/test" )
    let $versions-collection-path := concat( "/db/system/versions" , "/db/test" )
    
    let $clean :=
        for $p in ( $collection-path , $config-collection-path , $versions-collection-path )
        return
            if (xmldb:collection-available( $p )) then xmldb:remove( $p ) else ()
            
    let $collection-created := xmldb:create-collection( "/db" , "test" )

    let $set-status-code := response:set-status-code( 200 )
    let $set-content-type := response:set-header( "Content-Type" , "text/plain" )
    return "ok"

};



declare function foo:page() as item()*
{
    let $set-status-code := response:set-status-code( 200 )
    let $set-content-type := response:set-header( "Content-Type" , "text/html" )
    return 
    <html xmlns="http://www.w3.org/1999/xhtml">
      <head><title></title></head>
        <body>
            <p>
                <form action="" method="post" target="f">
                    <input type="hidden" name="action" value="bootstrap-with-versioning"/>
                    <input type="submit" value="bootstrap with versioning"/>
                </form>
            </p>
            <p>
                <form action="" method="post" target="f">
                    <input type="hidden" name="action" value="bootstrap-without-versioning"/>
                    <input type="submit" value="bootstrap without versioning"/>
                </form>
            </p>
            <p>
                <form action="" method="post" target="f">
                    <input type="hidden" name="action" value="store-entry"/>
                    title: <input type="text" name="title"></input>
                    <input type="submit" value="store entry"/>
                </form>
            </p>
            <p>
                <form action="" method="post" target="f">
                    <input type="hidden" name="action" value="retrieve-entry"/>
                    <input type="submit" value="retrieve entry"/>
                </form>
                <form action="" method="post" target="f">
                    <input type="hidden" name="action" value="retrieve-entry-dynamic"/>
                    <input type="submit" value="retrieve entry (function dynamically created)"/>
                </form>
            </p>
            <iframe name="f" style="width: 600px; height: 400px;"/>
        </body>
    </html>

};



declare function foo:do-service() as item()*
{

    if (request:get-method() = "GET")
    then foo:page()
    
    else if (request:get-method() = "POST" and request:get-parameter("action", "") = "bootstrap-with-versioning")
    then foo:do-bootstrap-with-versioning()
    
    else if (request:get-method() = "POST" and request:get-parameter("action", "") = "bootstrap-without-versioning")
    then foo:do-bootstrap-without-versioning()
    
    else if (request:get-method() = "POST" and request:get-parameter("action", "") = "store-entry")
    then foo:do-store-entry("/db/test/test.atom", <atom:entry><atom:title>{request:get-parameter( "title" , "" )}</atom:title></atom:entry>)
    
    else if (request:get-method() = "POST" and request:get-parameter("action", "") = "retrieve-entry")
    then foo:do-retrieve-entry("/db/test/test.atom") 
    
    else if (request:get-method() = "POST" and request:get-parameter("action", "") = "retrieve-entry-dynamic")
    then foo:do-retrieve-entry-dynamic("/db/test/test.atom") 
    
    else ()

};

    
    
let $login := xmldb:login( "/" , "admin" , "" )

return foo:do-service()

    
