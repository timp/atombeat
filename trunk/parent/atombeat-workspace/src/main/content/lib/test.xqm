xquery version "1.0";

module namespace test = "http://purl.org/atombeat/xquery/test";




declare variable $test:pass as xs:string := "pass" ;
declare variable $test:fail as xs:string := "fail" ;




declare function test:assert-true( $value as item()? , $message as xs:string? ) as xs:string
{
    if ( xs:boolean( $value ) ) then $test:pass else concat( $test:fail , ": " , $message )
};




declare function test:assert-equals( $expected as item()? , $actual as item()? , $message as xs:string? ) as xs:string
{
    let $value := ( $expected = $actual )
    let $message := concat( "expected (" , xs:string($expected) , "), actual (" , xs:string($actual) , "); " , $message )
    return test:assert-true( $value , $message )
};



