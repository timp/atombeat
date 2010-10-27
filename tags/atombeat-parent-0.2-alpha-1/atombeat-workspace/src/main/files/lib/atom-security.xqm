module namespace atomsec = "http://purl.org/atombeat/xquery/atom-security";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;

import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace xmldb = "http://exist-db.org/xquery/xmldb" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "constants.xqm" ;
import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "xutil.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "atomdb.xqm" ;

import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;
import module namespace security-config = "http://purl.org/atombeat/xquery/security-config" at "../config/security.xqm" ;

declare variable $atomsec:decision-deny as xs:string            := "DENY" ;
declare variable $atomsec:decision-allow as xs:string           := "ALLOW" ;
declare variable $atomsec:descriptor-suffix as xs:string        := ".descriptor" ;




declare function atomsec:store-descriptor(
    $request-path-info as xs:string ,
    $descriptor as element(atombeat:security-descriptor)
) as xs:string?
{

    if ( $request-path-info = "/" )
    then atomsec:store-workspace-descriptor( $descriptor )
    else if ( atomdb:collection-available( $request-path-info ) )
    then atomsec:store-collection-descriptor( $request-path-info , $descriptor )
    else if ( atomdb:member-available( $request-path-info ) )
    then atomsec:store-resource-descriptor( $request-path-info , $descriptor )
    else if ( atomdb:media-resource-available( $request-path-info ) )
    then atomsec:store-resource-descriptor( $request-path-info , $descriptor )
    else ()

};




declare function atomsec:store-workspace-descriptor(
    $descriptor as element(atombeat:security-descriptor)
) as item()*
{
    
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
    
        let $descriptor-collection-db-path := $config:base-security-collection-path
        let $descriptor-doc-name := $atomsec:descriptor-suffix
        return xmldb:last-modified( $descriptor-collection-db-path , $descriptor-doc-name )
    
    else if ( atomdb:collection-available( $request-path-info ) )
    
    then 
    
        let $descriptor-collection-db-path := concat( $config:base-security-collection-path , atomdb:request-path-info-to-db-path( $request-path-info ) )
        let $descriptor-doc-name := $atomsec:descriptor-suffix
        return 
            if ( xmldb:collection-available( $descriptor-collection-db-path ) )
            then xmldb:last-modified( $descriptor-collection-db-path , $descriptor-doc-name )
            else ()
    
    else if ( atomdb:member-available( $request-path-info ) or atomdb:media-resource-available( $request-path-info ) )
    
    then 
    
        let $groups := text:groups( $request-path-info , "^(.*)/([^/]+)$" )
        let $collection-db-path := atomdb:request-path-info-to-db-path( $groups[2] )
        let $descriptor-collection-db-path := concat( $config:base-security-collection-path , $collection-db-path )
        let $resource-name := $groups[3]
        let $descriptor-doc-name := concat( $resource-name , $atomsec:descriptor-suffix )
        return 
            if ( xmldb:collection-available( $descriptor-collection-db-path ) )
            then xmldb:last-modified( $descriptor-collection-db-path , $descriptor-doc-name )
            else ()
        
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




declare function atomsec:retrieve-descriptor(
    $request-path-info as xs:string
) as element(atombeat:security-descriptor)?
{

    let $descriptor := 
        if ( $request-path-info = "/" )
        then atomsec:retrieve-workspace-descriptor()
        else if ( atomdb:collection-available( $request-path-info ) )
        then atomsec:retrieve-collection-descriptor( $request-path-info )
        else if ( atomdb:member-available( $request-path-info ) )
        then atomsec:retrieve-resource-descriptor( $request-path-info )
        else if ( atomdb:media-resource-available( $request-path-info ) )
        then atomsec:retrieve-resource-descriptor( $request-path-info )
        else ()

    return $descriptor
    
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




declare function atomsec:retrieve-resource-descriptor-nocheck(
    $request-path-info as xs:string
) as element(atombeat:security-descriptor)?
{

    let $descriptor-doc-db-path := concat( $config:base-security-collection-path , atomdb:request-path-info-to-db-path( $request-path-info ) , $atomsec:descriptor-suffix )

    let $descriptor-doc := doc( $descriptor-doc-db-path )
    
    return $descriptor-doc/atombeat:security-descriptor
    
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



(:~
 : Optimised function to filter a feed.
 :)
declare function atomsec:filter-feed(
    $feed as element(atom:feed)
) as element(atom:feed)
{

    let $user := request:get-attribute( $config:user-name-request-attribute-key )
    let $roles := request:get-attribute( $config:user-roles-request-attribute-key )
    let $collection-path-info := substring-after( $feed/atom:link[@rel="self"]/@href , $config:content-service-url )
    
    (: 
     : Try to reduce the number of times we apply an ACL.
     : The idea is, we only want to apply the workspace and collection level
     : ACLs once, if we can help it.
     : N.B., beware, this leads to quirks of match-request-path-info condition for
     : RETRIEVE_MEMBER!
     :)
     
    let $workspace-descriptor := atomsec:retrieve-workspace-descriptor()
    let $workspace-decision := atomsec:apply-acl( $workspace-descriptor , $CONSTANT:OP-RETRIEVE-MEMBER , () , $user , $roles , $collection-path-info )  

    let $collection-descriptor := atomsec:retrieve-collection-descriptor( $collection-path-info )
    let $collection-decision := atomsec:apply-acl( $collection-descriptor , $CONSTANT:OP-RETRIEVE-MEMBER , () , $user , $roles , $collection-path-info )  

    return
        <atom:feed>
        {
            $feed/attribute::* ,
            for $child in $feed/child::*
    
            return
            
                if ( $child instance of element(atom:entry) ) then
                
                    let $child-path-info := atomdb:edit-path-info( $child )
                    
                    (: cope with recursive collections :)
                    let $owner-collection-path-info := atomdb:collection-path-info( $child )
                    let $owner-collection-descriptor :=
                        if ( $owner-collection-path-info = $collection-path-info )
                        then $collection-descriptor
                        else atomsec:retrieve-collection-descriptor( $owner-collection-path-info )
                    let $owner-collection-decision :=            
                        if ( $owner-collection-path-info = $collection-path-info )
                        then $collection-decision
                        else atomsec:apply-acl( $owner-collection-descriptor , $CONSTANT:OP-RETRIEVE-MEMBER , () , $user , $roles , $owner-collection-path-info )
                        
                    let $resource-descriptor := atomsec:retrieve-resource-descriptor-nocheck( $child-path-info )
                    let $resource-decision := atomsec:apply-acl( $resource-descriptor , $CONSTANT:OP-RETRIEVE-MEMBER , () , $user , $roles , $owner-collection-path-info )
                                                    
                    let $decision := atomsec:decide-priority( $resource-decision , $owner-collection-decision ,$workspace-decision )
                    
                    return 
                        if ( $decision = $atomsec:decision-allow ) then $child else ()

                else $child
                
        }    
        </atom:feed>
};



declare function atomsec:decide(
    $user as xs:string? ,
    $roles as xs:string* ,
    $request-path-info as xs:string ,
    $operation as xs:string ,
    $media-type as xs:string?
) as xs:string
{
    
    let $resource-descriptor := atomsec:retrieve-resource-descriptor( $request-path-info )
    
    let $collection-descriptor := atomsec:retrieve-collection-descriptor( $request-path-info )
    
    let $workspace-descriptor := atomsec:retrieve-workspace-descriptor()
    
    return atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type , $resource-descriptor , $collection-descriptor , $workspace-descriptor )
    
};




declare function atomsec:decide(
    $user as xs:string? ,
    $roles as xs:string* ,
    $request-path-info as xs:string ,
    $operation as xs:string ,
    $media-type as xs:string? , 
    $resource-descriptor as element(atombeat:security-descriptor)? ,
    $collection-descriptor as element(atombeat:security-descriptor)? ,
    $workspace-descriptor as element(atombeat:security-descriptor)? 
) as xs:string
{
    
    (: process ACLs :)
    
    let $resource-decision := atomsec:apply-acl( $resource-descriptor , $operation , $media-type , $user , $roles , $request-path-info )
    
    let $collection-decision := atomsec:apply-acl( $collection-descriptor , $operation , $media-type , $user , $roles , $request-path-info )   

    let $workspace-decision := atomsec:apply-acl( $workspace-descriptor , $operation , $media-type , $user , $roles , $request-path-info )  
    
    return atomsec:decide-priority( $resource-decision , $collection-decision , $workspace-decision )
    
};






declare function atomsec:decide-priority(
    $resource-decision as xs:string? ,
    $collection-decision as xs:string? ,
    $workspace-decision as xs:string? 
) as xs:string
{
    
    (: order decision :)
    
    let $decisions :=
        for $level in $security-config:priority
        return
            if ($level = "WORKSPACE") then $workspace-decision
            else if ($level = "COLLECTION") then $collection-decision
            else if ($level = "RESOURCE") then $resource-decision
            else ()
            
    (: take first decision, or default if no decision :)
    
    let $decision :=
        if (empty($decisions)) then $security-config:default-decision
        else $decisions[1]
    
    return $decision
    
};




declare function atomsec:apply-acl( 
    $descriptor as element(atombeat:security-descriptor)? ,
    $operation as xs:string ,
    $media-type as xs:string? ,
    $user as xs:string? ,
    $roles as xs:string* ,
    $request-path-info as xs:string
) as xs:string?
{

    let $matching-aces := atomsec:match-acl($descriptor, $operation, $media-type, $user, $roles, $request-path-info )
    
    let $decision := 
        if ( exists( $matching-aces ) ) then $matching-aces[1]/atombeat:type/text()
        else ()
    
    return $decision
    
};



declare function atomsec:match-acl( 
    $descriptor as element(atombeat:security-descriptor)? ,
    $operation as xs:string ,
    $media-type as xs:string? ,
    $user as xs:string? ,
    $roles as xs:string* ,
    $request-path-info as xs:string
) as element(atombeat:ace)*
{

    let $matching-aces :=
    
        (:
         : This expression is optimised to improve efficiency and make use of indexes.
         : N.B. ACL processing is expensive especially on list collection operations.
         :)
         
        $descriptor/atombeat:acl/atombeat:ace
            [ (: match operation :)
                atombeat:permission = "*" or atombeat:permission = $operation
            ]
            [ (: match recipient :)
                (: match user :)
                atombeat:recipient = "*" 
                or atombeat:recipient[@type="user"] = $user
                or atombeat:recipient[@type="role"] = $roles 
                or atomsec:match-group( . , $user , $descriptor )
            ]
            [ 
                empty( atombeat:conditions/atombeat:condition[@type="mediarange"] )
                or atomsec:match-media-range-condition( . , $media-type ) ]
            [ 
                empty( atombeat:conditions/atombeat:condition[@type="match-request-path-info"] )
                or atomsec:match-request-path-info-condition( . , $request-path-info ) 
            ]

    return $matching-aces
    
};




declare function atomsec:match-group(
    $ace as element(atombeat:ace) ,
    $user as xs:string? ,
    $descriptor as element(atombeat:security-descriptor)
) as xs:boolean
{
    
    let $group := $ace/atombeat:recipient[@type="group"]
    
    return 
    
        if ( empty( $group ) ) then false()
        
        else
    
            let $groups :=
            
                for $group in $descriptor/atombeat:groups/atombeat:group
                let $src := $group/@src
                let $id := $group/@id
                return
                    if ( exists( $src) ) then atomsec:dereference-group( $id , $src )
                    else $group
        
            let $groups-for-user := $groups[ atombeat:member = $user ]/@id
            
            let $group-has-user := ( $group = $groups-for-user )
            
            return $group-has-user
    
};




declare function atomsec:dereference-group(
    $id as xs:string ,
    $src as xs:string
) as element(group)?
{
    
    let $src := substring-after( $src , $config:content-service-url )
    
    let $descriptor :=
        if ( $src = "/" )
        then atomsec:retrieve-workspace-descriptor()
        else if ( atomdb:collection-available( $src ) )
        then atomsec:retrieve-collection-descriptor( $src )
        else atomsec:retrieve-resource-descriptor( $src )
        
    return $descriptor/atombeat:groups/atombeat:group[@id=$id]  
    
};




declare function atomsec:match-media-range-condition(
    $ace as element(atombeat:ace) ,
    $media-type as xs:string*
) as xs:boolean
{

    let $operation := $ace/atombeat:permission
    let $expected-range := $ace/atombeat:conditions/atombeat:condition[@type="mediarange"]
    
    return
    
        (: if operation is not on media, do not attempt to match media type - TODO optimise here with an index, or a different strategy? :)
        if ( not( ends-with( $operation , "MEDIA" ) ) ) then true()
         
        (: if no expectation defined, match any media type :)
        else if ( empty( $expected-range ) or $expected-range = "" ) then true()

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




declare function atomsec:match-request-path-info-condition( 
    $ace as element(atombeat:ace) , 
    $request-path-info as xs:string?
) as xs:boolean
{

    let $pattern := $ace/atombeat:conditions/atombeat:condition[@type="match-request-path-info"]
    
    return
    
        if ( empty( $pattern ) or $pattern = "" ) then true() 
        
        else matches( $request-path-info , $pattern )
    
};



declare function atomsec:is-denied(
    $operation as xs:string ,
    $request-path-info as xs:string ,
    $request-media-type as xs:string?
) as xs:boolean
{

    let $user := request:get-attribute( $config:user-name-request-attribute-key )
    let $roles := request:get-attribute( $config:user-roles-request-attribute-key )
    
    let $denied := 
        if ( not( $security-config:enable-security ) ) then false()
        else ( atomsec:decide( $user , $roles , $request-path-info, $operation , $request-media-type ) = $atomsec:decision-deny )
        
    return $denied 
    
};




declare function atomsec:is-allowed(
    $operation as xs:string ,
    $request-path-info as xs:string ,
    $request-media-type as xs:string?
) as xs:boolean
{

    let $user := request:get-attribute( $config:user-name-request-attribute-key )
    let $roles := request:get-attribute( $config:user-roles-request-attribute-key )
    
    let $allowed := 
        if ( not( $security-config:enable-security ) ) then false()
        else ( atomsec:decide( $user , $roles , $request-path-info, $operation , $request-media-type ) = $atomsec:decision-allow )
        
    return $allowed 
    
};





declare function atomsec:wrap-with-entry(
    $request-path-info as xs:string ,
    $descriptor as element(atombeat:security-descriptor)?
) as element(atom:entry)
{
    let $id := concat( $config:security-service-url , $request-path-info )
    let $secured-uri := concat( $config:content-service-url , $request-path-info )
    let $secured-type :=
        if ( atomdb:collection-available( $request-path-info ) ) then concat( $CONSTANT:MEDIA-TYPE-ATOM , ";type=feed" )
        else if ( atomdb:member-available( $request-path-info ) ) then concat( $CONSTANT:MEDIA-TYPE-ATOM , ";type=entry" )
        else if ( atomdb:media-resource-available( $request-path-info ) ) then atomdb:get-mime-type( $request-path-info )
        else ()
    let $self-uri := $id
    let $updated := atomsec:descriptor-updated( $request-path-info )
    return
        <atom:entry>
            <atom:id>{$id}</atom:id>
            <atom:title type="text">Security Descriptor</atom:title>
            <atom:link rel="self" href="{$self-uri}" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}"/>
            <atom:link rel="edit" href="{$self-uri}" type="{$CONSTANT:MEDIA-TYPE-ATOM-ENTRY}"/>
            <atom:link rel="http://purl.org/atombeat/rel/secured" href="{$secured-uri}" type="{$secured-type}"/>
            <atom:updated>{$updated}</atom:updated>
            <atom:content type="application/vnd.atombeat+xml">
                { $descriptor }
            </atom:content>
        </atom:entry>
};








