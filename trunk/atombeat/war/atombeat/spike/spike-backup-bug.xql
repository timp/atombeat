xquery version "1.0";


declare function local:page() as item()*
{
    let $set-status-code := response:set-status-code( 200 )
    let $set-content-type := response:set-header( "Content-Type" , "text/html" )
    return 
    <html xmlns="http://www.w3.org/1999/xhtml">
      <head><title>Spike Backup Bug</title></head>
        <body>
            <p>
                This script isolates code required to generate a warning message during backup. 
            </p>
            <p>
                To demonstrate the bug, click on the button below, then examine the logs.
            </p>
            <p>
                <form action="" method="post">
                    <input type="submit" value="Go"/>
                </form>
            </p>
        </body>
    </html>

};



declare function local:isolate() as item()*
{
    let $collection-path := "/db/test"
    let $config-collection-path := concat( "/db/system/config" , "/db/test" )
    let $versions-collection-path := concat( "/db/system/versions" , "/db/test" )


    let $clean :=
        for $p in ( $collection-path , $config-collection-path , $versions-collection-path )
        return
            if (xmldb:collection-available( $p )) then xmldb:remove( $p ) else ()

    let $collection-created := xmldb:create-collection( "/db" , "test" )
    
    let $collection-config :=

        <collection xmlns="http://exist-db.org/collection-config/1.0">
            <triggers>
                <trigger event="store,remove,update" class="org.atombeat.versioning.VersioningTrigger">
                    <parameter name="overwrite" value="yes"/>
                </trigger>
            </triggers>
        </collection>
        
    let $base-config-collection-created := xmldb:create-collection( "/db/system/config" , "db" )
    let $config-collection-created := xmldb:create-collection( "/db/system/config/db" , "test" )
    let $config-stored := xmldb:store( $config-collection-path , "collection.xconf" , $collection-config , "application/xml" )

    let $doc :=
        <foo><bar>baz</bar></foo>
        
    let $stored := xmldb:store( "/db/test" , "foo.xml" , $doc )   
    

    let $doc2 := 
        <foo><bar>spong</bar></foo>

    let $udpated := xmldb:store( "/db/test" , "foo.xml" , $doc2 )   

    let $params :=
     <parameters>
       <param name="output" value="export"/>
        <param name="backup" value="yes"/>
        <param name="incremental" value="no"/>
     </parameters>
    let $backup := system:trigger-system-task("org.exist.storage.ConsistencyCheckTask", $params)
    
    (: expose another problem - xpath queries don't work against the base revision unless you use wildcards :)
    
    return doc( "/db/system/versions/db/test/foo.xml.base" )/foo (: try -- doc( "/db/system/versions/db/test/foo.xml.base" )/* -- instead :)
    
};


declare function local:do-service() as item()*
{

    if (request:get-method() = "GET")
    then local:page()
    
    else if (request:get-method() = "POST")
    then 
        let $isolate := local:isolate()
        return $isolate
    
    else ()

};




let $login := xmldb:login( "/" , "admin" , "" )

return local:do-service()
    
