xquery version "1.0";

declare namespace atom = "http://www.w3.org/2005/Atom" ;

import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace xmldb = "http://exist-db.org/xquery/xmldb" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace CONSTANT = "http://atombeat.org/xquery/constants" at "constants.xqm" ;

import module namespace xutil = "http://atombeat.org/xquery/xutil" at "xutil.xqm" ;
import module namespace test = "http://atombeat.org/xquery/test" at "test.xqm" ;
import module namespace atomdb = "http://atombeat.org/xquery/atomdb" at "atomdb.xqm" ;
import module namespace atomsec = "http://atombeat.org/xquery/atom-security" at "atom-security.xqm" ;
import module namespace config = "http://atombeat.org/xquery/config" at "../config/shared.xqm" ;




declare variable $test-collection-path as xs:string := "/test-security" ;
declare variable $test-member-path as xs:string := concat( $test-collection-path , "/test-member.atom" ) ;




declare function local:setup() as empty()
{

    let $login := xmldb:login( "/" , "admin" , "" )
    
    let $feed :=     
        <atom:feed>
            <atom:title>TEST COLLECTION</atom:title>
        </atom:feed>
    
    let $collection-db-path := atomdb:create-collection( $test-collection-path, $feed )
    
    let $entry :=
        <atom:entry>
            <atom:title>TEST ENTRY</atom:title>
        </atom:entry>
        
    let $member-db-path := atomdb:store-member( $test-collection-path , "test-member.atom" , $entry )    
    
    return ()

};




declare function local:test-global-acl() as item()*
{

    let $output := ( "test-global-acl..." )
    
    let $global-acl :=
        <acl>
            <rules>
                <allow>
                    <user>alice</user>
                    <operation>create-collection</operation>
                </allow>
                <allow>
                    <role>administrator</role>
                    <operation>create-collection</operation>
                </allow>
            </rules>
        </acl>
        
    let $global-acl-doc-db-path := atomsec:store-global-acl($global-acl)
    
    (: test allow by user name :)
    
    let $request-path-info := "/foo"
    let $operation := "create-collection"
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "alice should be allowed to create collections" ) )
    
    (: test deny by user name :)
    
    let $request-path-info := "/foo"
    let $operation := "create-collection"
    let $media-type := ()
    let $user := "bob"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "bob should not be allowed to create collections" ) )
    
    (: test deny by operation :)
    
    let $request-path-info := "/foo"
    let $operation := "create-member"
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "alice should not be allowed to create collection members" ) )
    
    (: test allow by role :)
    
    let $request-path-info := "/foo"
    let $operation := "create-collection"
    let $media-type := ()
    let $user := ()
    let $roles := ( "administrator" , "user" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "administrators should be allowed to create collections" ) )
    
    (: test deny by role :)
    
    let $request-path-info := "/foo"
    let $operation := "create-collection"
    let $media-type := ()
    let $user := ()
    let $roles := ( "user" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "users should not be allowed to create collections" ) )
    
    return $output
    
};




declare function local:test-collection-acl() as item()*
{

    let $output := ( "test-collection-acl..." )
    
    let $global-acl :=
        <acl>
            <rules>
                <allow>
                    <role>administrator</role>
                    <operation>create-member</operation>
                </allow>
            </rules>
        </acl>
        
    let $global-acl-doc-db-path := atomsec:store-global-acl($global-acl)

    let $collection-acl :=
        <acl>
            <rules>
                <allow>
                    <user>alice</user>
                    <operation>create-member</operation>
                </allow>
                <allow>
                    <role>author</role>
                    <operation>create-member</operation>
                </allow>
            </rules>
        </acl>
        
    let $collection-acl-doc-db-path := atomsec:store-collection-acl( $test-collection-path , $collection-acl )
    
    (: test allow by user name from collection acl :)
    
    let $request-path-info := $test-collection-path
    let $operation := "create-member"
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "alice should be allowed to create members of the /test-security collection" ) )
  
    (: test deny by user name :)
    
    let $request-path-info := $test-collection-path
    let $operation := "create-member"
    let $media-type := ()
    let $user := "bob"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "bob should not be allowed to create members of the /test-security collection" ) )
  
    (: test deny by path :)
    
    let $request-path-info := "/another"
    let $operation := "create-member"
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "alice should not be allowed to create members of the /another collection" ) )
  
    (: test allow by role from collection acl :)
    
    let $request-path-info := $test-collection-path
    let $operation := "create-member"
    let $media-type := ()
    let $user := ()
    let $roles := ( "author" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "authors should be allowed to create members of the /test-security collection" ) )
  
    (: test deny by role :)
    
    let $request-path-info := $test-collection-path
    let $operation := "create-member"
    let $media-type := ()
    let $user := ()
    let $roles := ( "user" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "users should not be allowed to create members of the /test-security collection" ) )
  
    (: test allow by role from global acl :)
    
    let $request-path-info := $test-collection-path
    let $operation := "create-member"
    let $media-type := ()
    let $user := ()
    let $roles := ( "administrator" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "administrators should be allowed to create members of any collection" ) )
  
    return $output
    
};




declare function local:test-resource-acl()
{

    let $output := ( "test-resource-acl..." )
    
    let $global-acl :=
        <acl>
            <rules>
                <allow>
                    <role>administrator</role>
                    <operation>update-member</operation>
                </allow>
            </rules>
        </acl>
        
    let $global-acl-doc-db-path := atomsec:store-global-acl($global-acl)

    let $collection-acl :=
        <acl>
            <rules>
                <allow>
                    <role>editor</role>
                    <operation>update-member</operation>
                </allow>
            </rules>
        </acl>
        
    let $collection-acl-doc-db-path := atomsec:store-collection-acl( $test-collection-path , $collection-acl )

    let $resource-acl :=
        <acl>
            <rules>
                <allow>
                    <user>alice</user>
                    <operation>update-member</operation>
                </allow>
            </rules>
        </acl>
        
    let $resource-acl-doc-db-path := atomsec:store-resource-acl( $test-member-path , $resource-acl )  
    
    (: test allow by user name from resource acl :)
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "alice should be allowed to update the resource" ) )
    
    (: test deny by user name :)
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := "bob"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "bob should not be allowed to update the resource" ) )
    
    (: test deny by operation :)
    
    let $request-path-info := $test-member-path
    let $operation := "delete-member"
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "alice should not be allowed to delete the resource" ) )
    
    (: test allow by role from collection acl :)
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := ()
    let $roles := ( "editor" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "editors should be allowed to update any member of the collection" ) )
    
    (: test allow by role from global acl :)
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := ()
    let $roles := ( "administrator" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "administrators should be allowed to update any member of any collection" ) )
    
    (: test deny by role :)
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := ()
    let $roles := ( "user" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "users should not be allowed to update the resource" ) )
    
    (: test deny by operation :)
    
    let $request-path-info := $test-member-path
    let $operation := "delete-member"
    let $media-type := ()
    let $user := ()
    let $roles := ( "editor" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "editors should not be allowed to delete the resource" ) )
    
    (: test deny by operation :)
    
    let $request-path-info := $test-member-path
    let $operation := "delete-member"
    let $media-type := ()
    let $user := ()
    let $roles := ( "administrator" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "administrators should not be allowed to delete the resource" ) )
    
    return $output
};




declare function local:test-wildcards() as item()*
{

    let $output := ( "test-wildcards..." )
    
    let $global-acl :=
        <acl>
            <rules>
                <allow>
                    <user>*</user>
                    <operation>create-collection</operation>
                </allow>
                <allow>
                    <role>administrator</role>
                    <operation>*</operation>
                </allow>
            </rules>
        </acl>
        
    let $global-acl-doc-db-path := atomsec:store-global-acl($global-acl)
    
    (: test allow by wildcard user name :)
    
    let $request-path-info := "/foo"
    let $operation := "create-collection"
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "any user should be allowed to create collections" ) )
    
    (: test allow by wildcard operation :)
    
    let $request-path-info := "/foo"
    let $operation := "create-member"
    let $media-type := ()
    let $user := ()
    let $roles := ( "administrator" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "administrators should be allowed any operation" ) )
    
    return $output

};




declare function local:test-media-ranges() as item()*
{

    let $output := ( "test-media-ranges..." )
    
    let $global-acl :=
        <acl>
            <rules>
                <allow>
                    <user>alice</user>
                    <operation>create-media</operation>
                    <media-range>*/*</media-range>
                </allow>
                <allow>
                    <user>bob</user>
                    <operation>create-media</operation>
                    <media-range>text/*</media-range>
                </allow>
                <allow>
                    <user>jane</user>
                    <operation>create-media</operation>
                    <media-range>application/xml</media-range>
                </allow>
                <allow>
                    <user>alice</user>
                    <operation>create-member</operation>
                    <media-range>foo/bar</media-range> <!-- should be ignored -->
                </allow>
            </rules>
        </acl>
        
    let $global-acl-doc-db-path := atomsec:store-global-acl($global-acl)

    (: test allow by any media type/subtype :)
    
    let $request-path-info := "/foo"
    let $operation := "create-media"
    let $media-type := $CONSTANT:MEDIA-TYPE-XML
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "alice should be allowed to create media with any media type/subtype" ) )
    
    (: test allow by any media subtype :)
    
    let $request-path-info := "/foo"
    let $operation := "create-media"
    let $media-type := "text/plain"
    let $user := "bob"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "bob should be allowed to create media type text with any media subtype" ) )
    
    (: test deny by media type :)
    
    let $request-path-info := "/foo"
    let $operation := "create-media"
    let $media-type := $CONSTANT:MEDIA-TYPE-XML
    let $user := "bob"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "bob should only be allowed to create media type text" ) )
    
    (: test allow by media type/subtype :)
    
    let $request-path-info := "/foo"
    let $operation := "create-media"
    let $media-type := $CONSTANT:MEDIA-TYPE-XML
    let $user := "jane"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "jane should be allowed to create media type application/xml" ) )
    
    (: test deny by media type/subtype :)
    
    let $request-path-info := "/foo"
    let $operation := "create-media"
    let $media-type := "application/foo"
    let $user := "jane"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "jane should only be allowed to create media type application/xml" ) )
    
    (: test deny by media type/subtype :)
    
    let $request-path-info := "/foo"
    let $operation := "create-media"
    let $media-type := "foo/xml"
    let $user := "jane"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "jane should only be allowed to create media type application/xml" ) )
    
    (: test deny by media type/subtype :)
    
    let $request-path-info := "/foo"
    let $operation := "create-media"
    let $media-type := "foo/bar"
    let $user := "jane"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "jane should only be allowed to create media type application/xml" ) )
    
    (: test media range is ignored if operation does not involve media :)
    let $request-path-info := "/foo"
    let $operation := "create-member"
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "media range should be ignored for operations not on media" ) )
    
    return $output
    
};




declare function local:test-inline-groups() as item()*
{

    let $output := ( "test-inline-groups..." )
    
    let $global-acl :=
        <acl>
            <groups>
                <group name="readers">
                    <user>richard</user>
                    <user>rebecca</user>
                </group>
                <group name="authors">
                    <user>alice</user>
                    <user>austin</user>
                </group>
                <group name="editors">
                    <user>emma</user>
                    <user>edward</user>
                </group>
            </groups>
            <rules>
                <allow>
                    <group>readers</group>
                    <operation>retrieve-member</operation>
                </allow>
                <allow>
                    <group>authors</group>
                    <operation>create-member</operation>
                </allow>
                <allow>
                    <group>editors</group>
                    <operation>update-member</operation>
                </allow>
            </rules>
        </acl>
        
    let $global-acl-doc-db-path := atomsec:store-global-acl($global-acl)

    (: test readers :)
    
    let $request-path-info := $test-member-path
    let $operation := "retrieve-member"
    let $media-type := ()
    let $user := "richard"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "readers should be allowed to retrieve collection members" ) )
    
    let $request-path-info := $test-member-path
    let $operation := "retrieve-member"
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "readers should be allowed to retrieve collection members" ) )
    
    let $request-path-info := $test-collection-path
    let $operation := "create-member"
    let $media-type := ()
    let $user := "richard"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "readers should not be allowed to create collection members" ) )
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "readers should not be allowed to update collection members" ) )
    
    (: test authors :)
    
    let $request-path-info := $test-collection-path
    let $operation := "create-member"
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "authors should be allowed to create collection members" ) )
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := "austin"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "authors should not be allowed to update collection members" ) )
    
    (: test editors :)
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := "emma"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "editors should be allowed to update collection members" ) )
    
    let $request-path-info := $test-collection-path
    let $operation := "create-member"
    let $media-type := ()
    let $user := "edward"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "editors should not be allowed to create collection members" ) )
    
    return $output
};



declare function local:test-reference-global-groups() as item()*
{

    let $output := ( "test-reference-global-groups..." )
    
    let $global-acl :=
        <acl>
            <groups>
                <group name="readers">
                    <user>richard</user>
                    <user>rebecca</user>
                </group>
                <group name="authors">
                    <user>alice</user>
                    <user>austin</user>
                </group>
                <group name="editors">
                    <user>emma</user>
                    <user>edward</user>
                </group>
            </groups>
        </acl>
        
    let $global-acl-doc-db-path := atomsec:store-global-acl($global-acl)
    
    let $collection-acl :=
        <acl>
            <groups>
                <group name="readers" src="/"/>
                <group name="authors" src="/"/>
                <group name="editors" src="/"/>
            </groups>
            <rules>
                <allow>
                    <group>readers</group>
                    <operation>retrieve-member</operation>
                </allow>
                <allow>
                    <group>authors</group>
                    <operation>create-member</operation>
                </allow>
                <allow>
                    <group>editors</group>
                    <operation>update-member</operation>
                </allow>
            </rules>
        </acl>
    
    let $collection-acl-doc-db-path := atomsec:store-collection-acl( $test-collection-path , $collection-acl )
    
    (: test readers :)
    
    let $request-path-info := $test-member-path
    let $operation := "retrieve-member"
    let $media-type := ()
    let $user := "richard"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "readers should be allowed to retrieve collection members" ) )
    
    let $request-path-info := $test-member-path
    let $operation := "retrieve-member"
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "readers should be allowed to retrieve collection members" ) )
    
    let $request-path-info := $test-collection-path
    let $operation := "create-member"
    let $media-type := ()
    let $user := "richard"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "readers should not be allowed to create collection members" ) )
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "readers should not be allowed to update collection members" ) )
    
    (: test authors :)
    
    let $request-path-info := $test-collection-path
    let $operation := "create-member"
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "authors should be allowed to create collection members" ) )
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := "austin"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "authors should not be allowed to update collection members" ) )
    
    (: test editors :)
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := "emma"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "editors should be allowed to update collection members" ) )
    
    let $request-path-info := $test-collection-path
    let $operation := "create-member"
    let $media-type := ()
    let $user := "edward"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "editors should not be allowed to create collection members" ) )
    
    return $output
};



declare function local:test-reference-collection-groups() as item()*
{

    let $output := ( "test-reference-collection-groups..." )
    
    let $global-acl :=
        <acl>
        </acl>
        
    let $global-acl-doc-db-path := atomsec:store-global-acl($global-acl)
    
    let $collection-acl :=
        <acl>
            <groups>
                <group name="readers">
                    <user>richard</user>
                    <user>rebecca</user>
                </group>
                <group name="editors">
                    <user>emma</user>
                    <user>edward</user>
                </group>
            </groups>
        </acl>
        
    let $collection-acl-doc-db-path := atomsec:store-collection-acl( $test-collection-path , $collection-acl )
    
    let $resource-acl :=
        <acl>
            <groups>
                <group name="readers" src="/test-security"/>
                <group name="editors" src="/test-security"/>
            </groups>
            <rules>
                <allow>
                    <group>readers</group>
                    <operation>retrieve-member</operation>
                </allow>
                <allow>
                    <group>editors</group>
                    <operation>update-member</operation>
                </allow>
            </rules>
        </acl>

    let $resource-acl-doc-db-path := atomsec:store-resource-acl( $test-member-path , $resource-acl )  
    
    (: test readers :)
    
    let $request-path-info := $test-member-path
    let $operation := "retrieve-member"
    let $media-type := ()
    let $user := "richard"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "readers should be allowed to retrieve the resource" ) )
    
    let $request-path-info := $test-member-path
    let $operation := "retrieve-member"
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "readers should be allowed to retrieve the resource" ) )
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "readers should not be allowed to update the resource" ) )
    
    (: test editors :)
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := "emma"
    let $roles := ()
    let $decision := atomsec:decide($user , $roles , $request-path-info , $operation , $media-type)
    let $output := ( $output , test:assert-equals( "allow" , $decision , "editors should be allowed to update the resource" ) )
    
    let $request-path-info := $test-collection-path
    let $operation := "delete-member"
    let $media-type := ()
    let $user := "edward"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "editors should not be allowed to delete the resource" ) )
    
    return $output
};



declare function local:test-reference-resource-groups() as item()*
{

    let $output := ( "test-reference-resource-groups..." )
    
    let $global-acl :=
        <acl>
        </acl>
        
    let $global-acl-doc-db-path := atomsec:store-global-acl($global-acl)
    
    let $collection-acl :=
        <acl>
            <groups>
                <group name="readers" src="/test-security/test-member.atom"/>
                <group name="editors" src="/test-security/test-member.atom"/>
            </groups>
            <rules>
                <allow>
                    <group>readers</group>
                    <operation>retrieve-member</operation>
                </allow>
                <allow>
                    <group>editors</group>
                    <operation>update-member</operation>
                </allow>
            </rules>
        </acl>
        
    let $collection-acl-doc-db-path := atomsec:store-collection-acl( $test-collection-path , $collection-acl )
    
    let $resource-acl :=
        <acl>
            <groups>
                <group name="readers">
                    <user>richard</user>
                    <user>rebecca</user>
                </group>
                <group name="editors">
                    <user>emma</user>
                    <user>edward</user>
                </group>
            </groups>
        </acl>

    let $resource-acl-doc-db-path := atomsec:store-resource-acl( $test-member-path , $resource-acl )  
    
    (: test readers :)
    
    let $request-path-info := $test-member-path
    let $operation := "retrieve-member"
    let $media-type := ()
    let $user := "richard"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "readers should be allowed to retrieve the resource" ) )
    
    let $request-path-info := $test-member-path
    let $operation := "retrieve-member"
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "readers should be allowed to retrieve the resource" ) )
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "readers should not be allowed to update the resource" ) )
    
    (: test editors :)
    
    let $request-path-info := $test-member-path
    let $operation := "update-member"
    let $media-type := ()
    let $user := "emma"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "allow" , $decision , "editors should be allowed to update the resource" ) )
    
    let $request-path-info := $test-collection-path
    let $operation := "delete-member"
    let $media-type := ()
    let $user := "edward"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $operation , $media-type )
    let $output := ( $output , test:assert-equals( "deny" , $decision , "editors should not be allowed to delete the resource" ) )
    
    return $output
};




declare function local:test-update-global-acl() as item()*
{
    let $output := ( "test-update-global-acl..." )
    
    let $global-acl :=
        <acl>
            <rules>
                <allow>
                    <user>alice</user>
                    <operation>create-collection</operation>
                </allow>
                <allow>
                    <role>administrator</role>
                    <operation>create-collection</operation>
                </allow>
            </rules>
        </acl>
        
    let $global-acl-doc-db-path := atomsec:store-global-acl($global-acl)
    
    let $acl := atomsec:retrieve-global-acl()
    
    let $output := ( $output , test:assert-equals( 2 , count($acl/rules/*) , "expect 2 rules" ) )
    
    let $global-acl :=
        <acl/>
        
    let $global-acl-doc-db-path := atomsec:store-global-acl($global-acl)
    
    let $acl := atomsec:retrieve-global-acl()
    
    let $output := ( $output , test:assert-equals( 0 , count($acl/rules/*) , "expect 0 rules" ) )
    
    let $global-acl :=
        <acl>
            <rules>
                <allow>
                    <user>alice</user>
                    <operation>create-collection</operation>
                </allow>
            </rules>
        </acl>
        
    let $global-acl-doc-db-path := atomsec:store-global-acl($global-acl)
    
    let $acl := atomsec:retrieve-global-acl()
    
    let $output := ( $output , test:assert-equals( 1 , count($acl/rules/*) , "expect 1 rule" ) )
    
    return $output
    
};




declare function local:main() as item()*
{

    let $setup := local:setup()
    let $output := (
        local:test-global-acl() ,
        local:test-collection-acl() ,
        local:test-resource-acl() ,
        local:test-wildcards() ,
        local:test-media-ranges() ,
        local:test-inline-groups() ,
        local:test-reference-global-groups() ,
        local:test-reference-collection-groups() ,
        local:test-reference-resource-groups() ,
        local:test-update-global-acl()
    )
    let $response-type := response:set-header( "Content-Type" , "text/plain" )
    return $output
    
};




local:main()

