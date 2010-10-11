xquery version "1.0";

module namespace xutil = "http://purl.org/atombeat/xquery/xutil";


declare namespace math="java:java.lang.Math";
declare namespace long="java:java.lang.Long";
declare namespace double="java:java.lang.Double";
declare namespace collection-config = "http://exist-db.org/collection-config/1.0" ;




declare function xutil:get-or-create-collection(
	$collection-path as xs:string 
) as xs:string?
{
	
	let $available := xmldb:collection-available( $collection-path )
	 
	return 
		
		if ( $available ) then $collection-path
		
		else 
		
			let $groups := text:groups( $collection-path , "^(.*)/([^/]+)$" )
			
			let $target-collection-uri := $groups[2]
			
			let $new-collection := $groups[3]

			let $target-collection-uri := xutil:get-or-create-collection( $target-collection-uri )
			
            return xmldb:create-collection( $target-collection-uri , $new-collection )
			
};




(: return a deep copy of  the element and all sub elements :)
declare function xutil:copy(
    $element as element()
) as element() {
   element {node-name($element)}
      {$element/@*,
          for $child in $element/node()
              return
               if ($child instance of element())
                 then xutil:copy($child)
                 else $child
      }
};




declare function xutil:enable-versioning( 
    $collection-db-path as xs:string 
) as xs:string? 
{

    let $config := xutil:retrieve-collection-config( $collection-db-path )
    let $is-versioned := xutil:has-versioning-trigger( $config )
    
    return 
        if ( not( $is-versioned ) )
        then 
        
            let $new-trigger :=
                <collection-config:trigger event="store,remove,update" class="org.atombeat.versioning.VersioningTrigger">
                    <collection-config:parameter name="overwrite" value="yes"/>
                </collection-config:trigger>
            
            let $new-config :=

                <collection-config:collection>
                {
                    $config/attribute::* ,
                    $config/child::*[ not( . instance of element(collection-config:triggers) ) ] ,
                    if ( empty($config/collection-config:triggers) ) then
                        <collection-config:triggers>{$new-trigger}</collection-config:triggers>
                    else 
                        xutil:append-child( $config/collection-config:triggers , $new-trigger )
                }
                </collection-config:collection>
        
            return xutil:store-collection-config( $collection-db-path , $new-config )
            
        else ()        
    
};



declare function xutil:store-collection-config(
    $collection-db-path as xs:string ,
    $collection-config as element(collection-config:collection)
) as xs:string?
{

    let $config-collection-path := concat( "/db/system/config" , $collection-db-path )
    
    let $config-collection-path := xutil:get-or-create-collection( $config-collection-path )
    
    let $config-resource-path := xmldb:store( $config-collection-path , "collection.xconf" , $collection-config , "application/xml" )
    
    return $config-resource-path

};



declare function xutil:retrieve-collection-config(
    $collection-db-path as xs:string 
) as element(collection-config:collection)?
{
    let $config-resource-db-path := concat( "/db/system/config" , $collection-db-path , "/collection.xconf" )
    let $config := doc( $config-resource-db-path )/collection-config:collection
    return $config
};




declare function xutil:is-versioning-enabled(
    $collection-db-path as xs:string 
) as xs:boolean 
{
        
    let $config := xutil:retrieve-collection-config( $collection-db-path )
    
    let $enabled := xutil:has-versioning-trigger( $config )

    return $enabled
    
};




declare function xutil:has-versioning-trigger(
    $config as element(collection-config:collection)?
) as xs:boolean
{
    exists( 
        $config//collection-config:trigger[@class="org.atombeat.versioning.VersioningTrigger"] 
    )
};


declare function xutil:lpad(
    $value as xs:string ,
    $length as xs:integer ,
    $padder as xs:string 
) as xs:string
{
	if ( string-length( $value ) < $length )
	then
		let $value := concat( $padder , $value )
		return xutil:lpad( $value , $length , $padder )
	else $value
};



declare function xutil:random-alphanumeric(
    $num-chars as xs:integer
) as xs:string
{
    let $rnd := math:random()
    let $multiplier := math:pow( xs:double( 36 ) , xs:double( $num-chars ) )
    let $rnd := $rnd * $multiplier
    let $rnd := double:long-value( $rnd )
    let $rnd := long:to-string( $rnd , 36 )
    let $rnd := xutil:lpad ( $rnd , $num-chars , "0" ) 
    return $rnd
};



declare function xutil:random-alphanumeric(
    $num-chars as xs:integer ,
    $radix as xs:integer ,
    $map as xs:string ,
    $trans as xs:string 
) as xs:string
{
(:
    let $num-chars := 4
    let $radix := 21
    let $map := "0123456789abcdefghijklmnopqrstuvwxyz"
    let $trans := "abcdefghjkmnpqrstuxyz"
:)  
    let $rnd := math:random()
    let $multiplier := math:pow( xs:double( $radix ) , xs:double( $num-chars ) )
    let $rnd := $rnd * $multiplier
    let $rnd := double:long-value( $rnd )
    let $rnd := long:to-string( $rnd , $radix )
    let $rnd := xutil:lpad ( $rnd , $num-chars , "0" ) 
    let $rnd := translate( $rnd , $map , $trans )
    
    return $rnd
         
};



declare function xutil:append-child(
    $parent as element() ,
    $children as element()*
) as element() 
{
    
    element { node-name( $parent ) }
    {
        $parent/attribute::* ,
        $parent/child::* ,
        $children
    }

};



