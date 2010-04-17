xquery version "1.0";

module namespace bar = "http://example.org/bar";
declare namespace atom = "http://www.w3.org/2005/Atom" ;

declare function bar:retrieve() as item()* {

    let $login := xmldb:login( "/" , "admin" , "" )
    
    let $returndoc := doc("/db/atom/content/studies/test.xml")/atom:entry
    let $h := response:set-header( "Content-Type" , "text/plain" )
    
    return $returndoc
    
};

declare function bar:update-1() as item()* {

    let $login := xmldb:login( "/" , "admin" , "" )
    
    let $doc1 := <atom:entry><title>test 1</title><a>A</a></atom:entry>
    
    let $store1 := xmldb:store("/db/atom/content/studies", "test.xml", $doc1, "application/atom+xml")
    let $returndoc1 := doc("/db/atom/content/studies/test.xml")/atom:entry
    let $h := response:set-header( "Content-Type" , "text/plain" )
    
    return $returndoc1

};

declare function bar:update-2() as item()* {

    let $login := xmldb:login( "/" , "admin" , "" )
    
    let $doc2 := <atom:entry></atom:entry>
    
    let $store2 := xmldb:store("/db/atom/content/studies", "test.xml", $doc2, "application/atom+xml")
    let $returndoc2 := doc("/db/atom/content/studies/test.xml")/atom:entry
    let $h := response:set-header( "Content-Type" , "text/plain" )
    
    return $returndoc2
    
};
