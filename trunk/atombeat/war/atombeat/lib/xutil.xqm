xquery version "1.0";

module namespace xutil = "http://www.cggh.org/2010/atombeat/xquery/xutil";


declare namespace math="java:java.lang.Math";
declare namespace long="java:java.lang.Long";
declare namespace double="java:java.lang.Double";




declare function xutil:get-or-create-collection(
	$collection-path as xs:string 
) as xs:string?
{
	
	let $log := util:log( "debug" , concat( "$collection-path: " , $collection-path ) )
	 
	let $available := xmldb:collection-available( $collection-path )
	let $log := util:log( "debug" , concat( "$available: " , $available ) )
	 
	return 
		
		if ( $available ) then $collection-path
		
		else 
		
			let $groups := text:groups( $collection-path , "^(.*)/([^/]+)$" )
			let $log := util:log( "debug" , concat( "$groups: " , count( $groups ) ) )
			
			let $target-collection-uri := $groups[2]
			let $log := util:log( "debug" , concat( "$target-collection-uri: " , $target-collection-uri ) )
			
			let $new-collection := $groups[3]
			let $log := util:log( "debug" , concat( "$new-collection: " , $new-collection ) )

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

    let $collection-config :=

		<collection xmlns="http://exist-db.org/collection-config/1.0">
		    <triggers>
		        <trigger event="store,remove,update" class="org.exist.versioning.VersioningTrigger">
		            <parameter name="overwrite" value="yes"/>
		        </trigger>
		    </triggers>
		</collection>
		
	let $config-collection-path := concat( "/db/system/config" , $collection-db-path )
	let $log := util:log( "debug" , concat( "$config-collection-path: " , $config-collection-path ) )
	
	let $config-collection-path := xutil:get-or-create-collection( $config-collection-path )
	let $log := util:log( "debug" , concat( "$config-collection-path: " , $config-collection-path ) )
	
	let $config-resource-path := xmldb:store( $config-collection-path , "collection.xconf" , $collection-config )
	let $log := util:log( "debug" , concat( "$config-resource-path: " , $config-resource-path ) )
	
	return $config-resource-path
    
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




