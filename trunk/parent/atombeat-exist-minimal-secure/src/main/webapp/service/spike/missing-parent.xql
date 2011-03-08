(: see...
 : http://markmail.org/message/oc6fnewyrwpsh57w 
 : http://markmail.org/message/cayewxghqycff7n3
 : http://exist.2174344.n4.nabble.com/Error-SENR0001-when-querying-for-an-attribute-td3236513.html
 : 
 :)

declare variable $iterations := 136;

let $Result := <TestDoc> { for $i in 1 to $iterations return <Item id="{$i}"/> } </TestDoc> let $temp := xmldb:store("/db", 'TestDoc.xml', $Result) let $doc := doc('/db/TestDoc.xml')/TestDoc return for $item in $doc/Item let $temp := xmldb:store("/db", 'TestDoc.xml', $doc) return $item 