xquery version "1.0";

module namespace foo = "http://example.org/foo";

import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace bar = "http://example.org/bar" at "bar.xqm" ;

declare namespace atom = "http://www.w3.org/2005/Atom" ;

declare function foo:retrieve() as item()* {

    let $f := util:function( QName( "http://example.org/bar" , "bar:retrieve" ) , 0 )
    return util:call( $f )
    
};

declare function foo:update-1() as item()* {

    let $f := util:function( QName( "http://example.org/bar" , "bar:update-1" ) , 0 )
    return util:call( $f )
    
};

declare function foo:update-2() as item()* {

    let $f := util:function( QName( "http://example.org/bar" , "bar:update-2" ) , 0 )
    return util:call( $f )
    
};
