xquery version "1.0";

declare namespace atom = "http://www.w3.org/2005/Atom" ;

import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace xmldb = "http://exist-db.org/xquery/xmldb" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace CONSTANT = "http://www.cggh.org/2010/atombeat/xquery/constants" at "constants.xqm" ;

import module namespace xutil = "http://www.cggh.org/2010/atombeat/xquery/xutil" at "xutil.xqm" ;
import module namespace test = "http://www.cggh.org/2010/atombeat/xquery/test" at "test.xqm" ;
import module namespace atomdb = "http://www.cggh.org/2010/atombeat/xquery/atomdb" at "atomdb.xqm" ;
import module namespace atomsec = "http://www.cggh.org/2010/atombeat/xquery/atom-security" at "atom-security.xqm" ;
import module namespace config = "http://www.cggh.org/2010/atombeat/xquery/config" at "../config/shared.xqm" ;




declare variable $test-collection-path as xs:string := "/test-security" ;
declare variable $test-member-path as xs:string := concat( $test-collection-path , "/test-member.atom" ) ;





declare function local:test() as item()*
{
    let $output := ()
    
    let $acl := atomsec:retrieve-global-acl()
    
    let $output := ( $output , $acl )
    
    let $output := ( $output , count($acl/rules/*) )
    
    return $output
    
};




declare function local:main() as item()*
{

    let $output := (
        local:test()
    )
    let $response-type := response:set-header( "Content-Type" , "text/plain" )
    return $output
    
};




local:main()

