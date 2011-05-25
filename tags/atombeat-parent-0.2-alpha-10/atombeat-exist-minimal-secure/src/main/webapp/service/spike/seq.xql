let $a := <a><b/></a>
let $b := ( $a )
let $c := ( $a , <c/> )
let $d := ( () , $a )

let $reponse := response:set-header( "Content-Type" , "text/plain" )

return (
    count( $a ) ,
    count( $b ) ,
    count( $c ) ,
    count( $d ) ,
    $a instance of element()* ,
    $b instance of element()* ,
    $c instance of element()* ,
    $d instance of element()* ,
    $a[1] ,
    $b[1] ,
    $c[1] ,
    $c[2] ,
    $d[1] ,
    $d[2] 
)