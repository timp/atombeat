import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace xpu = "http://xproc.net/xproc/util" ;

declare function local:error() {
    $util:exception ,
    $util:exception-message
};

let $declarations := (
    util:declare-namespace('xhtml',xs:anyURI('http://www.w3.org/1999/xhtml')) ,
    util:declare-namespace('atom',xs:anyURI('http://www.w3.org/2005/Atom')) ,
    util:declare-namespace('p',xs:anyURI('http://www.w3.org/ns/xproc'))
)

let $xml :=
    <atom:foo xmlns:atom="http://www.w3.org/2005/Atom">
        <xhtml:bar xmlns:xhtml="http://www.w3.org/1999/xhtml" fizz="buzz">spong</xhtml:bar>
        <xhtml:bar xmlns:xhtml="http://www.w3.org/1999/xhtml" fizz="bizz">sping</xhtml:bar>
    </atom:foo>
    
let $qry := "let $foo := //* return $foo"
let $safe-qry := replace( $qry , '[()]' , '' )

return util:catch( '*' , xpu:evalXPATH( $safe-qry , $xml ) , local:error() )