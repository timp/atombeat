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




declare function xutil:transform-with-stylesheet(
    $node-tree as node()?, $stylesheet as item(), $parameters as node()?
) as item()*
{
    transform:transform( $node-tree , $stylesheet , $parameters )
};



declare function xutil:identity(
    $i as item()*
) as item()*
{
    $i
};



declare function xutil:get-header(
    $header-name as xs:string ,
    $request as element(request)
) as xs:string?
{

    let $value := $request/headers/header[name = lower-case($header-name)]/value/text()
    return if ( $value castable as xs:string ) then xs:string( $value ) else () 

};




declare function xutil:get-parameter(
    $parameter-name as xs:string ,
    $request as element(request)
) as xs:string?
{

    let $value := $request/parameters/parameter[name = lower-case($parameter-name)]/value/text()
    return if ( $value castable as xs:string ) then xs:string( $value ) else () 
    
};




declare function xutil:get-request-headers() as element(headers)
{
    <headers>
    {
        for $header-name in request:get-header-names()
        return
            <header>
                <name>{lower-case($header-name)}</name>
                <value>{request:get-header($header-name)}</value>
            </header>
    }
    </headers>
};




declare function xutil:get-request-parameters() as element(parameters)
{
    <parameters>
    {
        for $parameter-name in request:get-parameter-names()
        return
            <parameter>
                <name>{lower-case($parameter-name)}</name>
                <value>{request:get-parameter($parameter-name, '')}</value>
            </parameter>
    }
    </parameters>
};




declare function xutil:get-request-attributes() as element(parameters)
{
    <attributes>
    {
        for $name in request:attribute-names()
        return
            <attribute>
                <name>{lower-case($name)}</name>
                <value>{request:get-attribute($name)}</value>
            </attribute>
    }
    </attributes>
};




declare function xutil:match-etag(
    $header-value as xs:string ,
    $etag as xs:string
) as xs:string*
{

    let $match-etags := tokenize( $header-value , "\s*,\s*" )
    
    let $matches :=
        for $match-etag in $match-etags
        where (
            $match-etag = "*"
            or ( 
                starts-with( $match-etag , '"' ) 
                and ends-with( $match-etag , '"' )
                and $etag = substring( $match-etag , 2 , string-length( $match-etag ) - 2 )
            )  
        )
        return $match-etag
        
    return $matches

};


