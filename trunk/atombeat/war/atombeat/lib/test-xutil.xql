import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;

for $i in 1 to 10000
return xutil:random-alphanumeric( 4 , 21 , "0123456789abcdefghijk" , "abcdefghjkmnpqrstuxyz" )
