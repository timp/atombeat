xquery version "1.0";

import module namespace foo = "http://example.org/foo" at "foo.xqm";
    
let $login := xmldb:login( "/" , "admin" , "" )

return foo:do-service()

    
