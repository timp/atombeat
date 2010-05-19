module namespace atomsec = "http://atombeat.org/xquery/atom-security";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://atombeat.org/xmlns" ;

import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace xmldb = "http://exist-db.org/xquery/xmldb" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace CONSTANT = "http://atombeat.org/xquery/constants" at "constants.xqm" ;
import module namespace xutil = "http://atombeat.org/xquery/xutil" at "xutil.xqm" ;
import module namespace atomdb = "http://atombeat.org/xquery/atomdb" at "atomdb.xqm" ;

import module namespace config = "http://atombeat.org/xquery/config" at "../config/shared.xqm" ;

declare variable $atomsec:decision-deny as xs:string            := "DENY" ;
declare variable $atomsec:decision-allow as xs:string           := "ALLOW" ;
declare variable $atomsec:descriptor-suffix as xs:string        := ".descriptor" ;



declare variable $atomsec:logger-name := "org.atombeat.xquery.lib.atom-security" ;



declare function local:debug(
    $message as item()*
) as empty()
{
    util:log-app( "debug" , $atomsec:logger-name , $message )
};




declare function local:info(
    $message as item()*
) as empty()
{
    util:log-app( "info" , $atomsec:logger-name , $message )
};





declare function atomsec:store-workspace-descriptor(
    $descriptor as element(atombeat:security-descriptor)
) as item()*
{
    
    let $log := local:debug(  "== atomsec:store-workspace-descriptor ==" )
    let $log := local:debug(  $descriptor )
    
    let $base-security-collection-db-path := xutil:get-or-create-collection( $config:base-security-collection-path )
    
    let $workspace-descriptor-doc-db-path := xmldb:store( $base-security-collection-db-path , $atomsec:descriptor-suffix , $descriptor , $CONSTANT:MEDIA-TYPE-XML )
    
    return $workspace-descriptor-doc-db-path
    
};




declare function atomsec:store-collection-descriptor(
    $request-path-info as xs:string ,
    $descriptor as element(atombeat:security-descriptor)
) as xs:string?
{

    if ( atomdb:collection-available( $request-path-info ) )
    
    then 

        let $descriptor-collection-db-path := concat( $config:base-security-collection-path , atomdb:request-path-info-to-db-path( $request-path-info ) )
        
        let $descriptor-collection-db-path := xutil:get-or-create-collection( $descriptor-collection-db-path )
        
        let $descriptor-doc-db-path := xmldb:store( $descriptor-collection-db-path , $atomsec:descriptor-suffix , $descriptor , $CONSTANT:MEDIA-TYPE-XML )
        
        return $descriptor-doc-db-path

    else ()
};



declare function atomsec:descriptor-updated(
    $request-path-info as xs:string
) as xs:dateTime?
{

    if ( $request-path-info = "/" )
    
    then 
    
        let $collection-db-path := $config:base-security-collection-path
        let $descriptor-doc-name := $atomsec:descriptor-suffix
        return xmldb:last-modified( $collection-db-path , $descriptor-doc-name )
    
    else if ( atomdb:collection-available( $request-path-info ) )
    
    then 
    
        let $collection-db-path := concat( $config:base-security-collection-path , atomdb:request-path-info-to-db-path( $request-path-info ) )
        let $descriptor-doc-name := $atomsec:descriptor-suffix
        return xmldb:last-modified( $collection-db-path , $descriptor-doc-name )
    
    else if ( atomdb:member-available( $request-path-info ) or atomdb:media-resource-available( $request-path-info ) )
    
    then 
    
        let $groups := text:groups( $request-path-info , "^(.*)/([^/]+)$" )
        let $collection-db-path := atomdb:request-path-info-to-db-path( $groups[2] )
        let $descriptor-collection-db-path := concat( $config:base-security-collection-path , $collection-db-path )
        let $resource-name := $groups[3]
        let $descriptor-doc-name := concat( $resource-name , $atomsec:descriptor-suffix )
        return xmldb:last-modified( $collection-db-path , $descriptor-doc-name )
        
    else ()
    
};




declare function atomsec:store-resource-descriptor(
    $request-path-info as xs:string ,
    $descriptor as element(atombeat:security-descriptor)
) as xs:string?
{

    if ( atomdb:media-resource-available( $request-path-info ) or atomdb:member-available( $request-path-info ) )
    
    then

    	let $groups := text:groups( $request-path-info , "^(.*)/([^/]+)$" )
    	
    	let $collection-db-path := atomdb:request-path-info-to-db-path( $groups[2] )
    	
    	let $descriptor-collection-db-path := concat( $config:base-security-collection-path , $collection-db-path )
    	
        let $descriptor-collection-db-path := xutil:get-or-create-collection( $descriptor-collection-db-path )
        
    	let $resource-name := $groups[3]
    	
    	let $descriptor-doc-name := concat( $resource-name , $atomsec:descriptor-suffix )
    	
    	let $descriptor-doc-db-path := xmldb:store( $descriptor-collection-db-path , $descriptor-doc-name , $descriptor , $CONSTANT:MEDIA-TYPE-XML )
    	
    	return $descriptor-doc-db-path
        
    else ()

};




declare function atomsec:retrieve-workspace-descriptor() as element(atombeat:security-descriptor)?
{

    let $descriptor-doc-db-path := concat( $config:base-security-collection-path , "/" , $atomsec:descriptor-suffix )

    let $descriptor-doc := doc( $descriptor-doc-db-path )
    
    return $descriptor-doc/atombeat:security-descriptor
        
};




declare function atomsec:retrieve-collection-descriptor(
    $request-path-info as xs:string
) as element(atombeat:security-descriptor)?
{

    if ( atomdb:collection-available( $request-path-info ) )
    
    then

        (: TODO what if collection path is given with trailing slash? :)
        
        let $descriptor-doc-db-path := concat( $config:base-security-collection-path , atomdb:request-path-info-to-db-path( $request-path-info ) , "/" , $atomsec:descriptor-suffix )
    
        let $descriptor-doc := doc( $descriptor-doc-db-path )
        
        return $descriptor-doc/atombeat:security-descriptor

    else if ( atomdb:media-resource-available( $request-path-info ) or atomdb:member-available( $request-path-info ) )
    
    then 
    
        let $groups := text:groups( $request-path-info , "^(.*)/([^/]+)$" )
    	
    	return atomsec:retrieve-collection-descriptor( $groups[2] )
    
    else
    
        ()
        
};




declare function atomsec:retrieve-resource-descriptor(
    $request-path-info as xs:string
) as element(atombeat:security-descriptor)?
{

    if ( atomdb:media-resource-available( $request-path-info ) or atomdb:member-available( $request-path-info ) )
    
    then

        let $descriptor-doc-db-path := concat( $config:base-security-collection-path , atomdb:request-path-info-to-db-path( $request-path-info ) , $atomsec:descriptor-suffix )
    
        let $descriptor-doc := doc( $descriptor-doc-db-path )
        
        return $descriptor-doc/atombeat:security-descriptor
        
    else
    
        ()
        
};




declare function atomsec:decide(
    $user as xs:string? ,
    $roles as xs:string* ,
    $request-path-info as xs:string ,
    $operation as xs:string
) as xs:string
{
    atomsec:decide( $user , $roles , $request-path-info , $operation , () )
};




declare function atomsec:decide(
    $user as xs:string? ,
    $roles as xs:string* ,
    $request-path-info as xs:string ,
    $operation as xs:string ,
    $media-type as xs:string?
) as xs:string
{

    let $log := local:debug( "== atomsec:decide ==" )
    
    (: first we need to find the relevant ACLs :)
    
    (: if the request path identifies a atom collection member or media resource
     : then we need to find the resource ACL first :)
     
    let $resource-descriptor := atomsec:retrieve-resource-descriptor( $request-path-info )
    let $log := local:debug( $resource-descriptor )
    
    (: we also need the collection ACL :)
    
    let $collection-descriptor := atomsec:retrieve-collection-descriptor( $request-path-info )
    let $log := local:debug( $collection-descriptor )
    
    (: we also need the workspace ACL :)
    
    let $workspace-descriptor := atomsec:retrieve-workspace-descriptor()
    let $log := local:debug( $workspace-descriptor )
    
    (: process ACLs :)
    
    let $resource-decision := atomsec:apply-acl( $resource-descriptor , $operation , $media-type , $user , $roles )
    
    let $collection-decision := atomsec:apply-acl( $collection-descriptor , $operation , $media-type , $user , $roles )   

    let $workspace-decision := atomsec:apply-acl( $workspace-descriptor , $operation , $media-type , $user , $roles )  
    
    let $log := local:debug( concat( "$resource-decision: " , $resource-decision ) )
    let $log := local:debug( concat( "$collection-decision: " , $collection-decision ) )
    let $log := local:debug( concat( "$workspace-decision: " , $workspace-decision ) )

    (: order decision :)
    
    let $decisions :=
        for $level in $config:security-priority
        return
            if ($level = "WORKSPACE") then $workspace-decision
            else if ($level = "COLLECTION") then $collection-decision
            else if ($level = "RESOURCE") then $resource-decision
            else ()
            
    (: take first decision, or default if no decision :)
    
    let $decision :=
        if (empty($decisions)) then $config:default-security-decision
        else $decisions[1]
    
    let $message := ( "security decision (" , $decision , ") for user (" , $user , "), roles (" , string-join( $roles , " " ) , "), request-path-info (" , $request-path-info , "), operation(" , $operation , "), media-type (" , $media-type , ")" )
    let $log := local:info( $message )  
    
    return $decision
    
};




declare function atomsec:apply-acl( 
    $descriptor as element(atombeat:security-descriptor)? ,
    $operation as xs:string ,
    $media-type as xs:string? ,
    $user as xs:string? ,
    $roles as xs:string*
) as xs:string?
{

    let $matching-aces := atomsec:match-acl($descriptor, $operation, $media-type, $user, $roles)
    
    let $decision := 
        if ( exists( $matching-aces ) ) then $matching-aces[1]/atombeat:type 
        else ()
    
    return $decision
    
};



declare function atomsec:match-acl( 
    $descriptor as element(atombeat:security-descriptor)? ,
    $operation as xs:string ,
    $media-type as xs:string? ,
    $user as xs:string? ,
    $roles as xs:string*
) as element(atombeat:ace)*
{

    let $log := local:debug( "== atomsec:match-acl ==" )
    let $log := local:debug( $descriptor )
    
    let $matching-aces :=
    
        for $ace in $descriptor/atombeat:acl/* 

        let $log := local:debug( $ace )
        
        return
        
            if (
            
                atomsec:match-operation($ace , $operation)
            
                and ( 
                    atomsec:match-user( $ace , $user ) or    
                    atomsec:match-role( $ace , $roles ) or
                    atomsec:match-group( $ace , $user , $descriptor )
                ) 
                
                and atomsec:match-media-type( $ace , $media-type )
                
            ) 
            
            then $ace
            
            else ()
            
    let $log := local:debug( $matching-aces )
    
    return $matching-aces
    
};




declare function atomsec:match-operation(
    $ace as element(atombeat:ace) ,
    $operation as xs:string
) as xs:boolean
{
    ( xs:string( $ace/atombeat:permission ) = "*" ) or ( xs:string( $ace/atombeat:permission ) = $operation  ) 
};




declare function atomsec:match-user(
    $ace as element(atombeat:ace) ,
    $user as xs:string?
) as xs:boolean
{
    ( xs:string( $ace/atombeat:recipient[@type="user"] ) = "*" ) or ( xs:string( $ace/atombeat:recipient[@type="user"] ) = $user  ) 
};




declare function atomsec:match-role(
    $ace as element(atombeat:ace) ,
    $roles as xs:string*
) as xs:boolean
{
    ( xs:string( $ace/atombeat:recipient[@type="role"] ) = "*" ) or 
    ( exists( $ace/atombeat:recipient[@type="role"]) and exists( index-of( $roles , xs:string( $ace/atombeat:recipient[@type="role"] ) ) ) )
};




declare function atomsec:match-group(
    $ace as element(atombeat:ace) ,
    $user as xs:string? ,
    $descriptor as element(atombeat:security-descriptor)
) as xs:boolean
{

    let $log := local:debug( "== atomsec:match-group() ==" )
    let $log := local:debug( $ace )
    
    let $group := $ace/atombeat:recipient[@type="group"]
    let $log := local:debug( concat( "found group in ace: " , $group ) ) 
    
    return 
    
        if ( empty( $group ) ) then false()
        
        else if ( xs:string( $group ) = "*" ) then true()
        
        else
    
            let $groups :=
            
                for $group in $descriptor/atombeat:groups/atombeat:group
                let $src := $group/@src
                let $id := $group/@id
                return
                    if ( exists( $src) ) then atomsec:dereference-group( $id , $src )
                    else $group
        
            let $groups-for-user := $groups[atombeat:member=$user]/@id
            
            let $group-has-user := exists( index-of( $groups-for-user , xs:string( $group ) ) )
            let $log := local:debug( concat( "$group-has-user: " , $group-has-user ) )
            
            return $group-has-user
    
};




declare function atomsec:dereference-group(
    $id as xs:string ,
    $src as xs:string
) as element(group)?
{
    
    let $src := substring-after( $src , $config:service-url )
    
    let $descriptor :=
        if ( $src = "/" )
        then atomsec:retrieve-workspace-descriptor()
        else if ( atomdb:collection-available( $src ) )
        then atomsec:retrieve-collection-descriptor( $src )
        else atomsec:retrieve-resource-descriptor( $src )
        
    return $descriptor/atombeat:groups/atombeat:group[@id=$id]  
    
};




declare function atomsec:match-media-type(
    $ace as element(atombeat:ace) ,
    $media-type as xs:string*
) as xs:boolean
{

    let $operation := $ace/atombeat:permission
    let $expected-range := $ace/atombeat:conditions/atombeat:condition[@type="mediarange"]
    
    return
    
        (: if operation is not on media, do not attempt to match media type :)
        if ( not( ends-with( $operation , "MEDIA" ) ) ) then true()
         
        (: if no expectation defined, match any media type :)
        else if ( empty( $expected-range ) ) then true()

        else

            let $expected-groups := text:groups( $expected-range , "^([^/]*)/(.*)$" )
            let $expected-type := $expected-groups[2]
            let $expected-subtype := $expected-groups[3]
        
            let $actual-groups := 
                if ( exists( $media-type ) ) then text:groups( $media-type , "^([^/]*)/(.*)$" )
                else ()
                
            let $actual-type := $actual-groups[2]
            let $actual-subtype := $actual-groups[3]
            
            return
            
                ( $expected-range = "*/*" )
                or ( ( $expected-type = $actual-type )  and ( $expected-subtype = "*" ) )
                or ( ( $expected-type = $actual-type )  and ( $expected-subtype = $actual-subtype ) )
                    
};


