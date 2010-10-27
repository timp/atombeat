xquery version "1.0";

declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;

import module namespace text = "http://exist-db.org/xquery/text" ;
import module namespace xmldb = "http://exist-db.org/xquery/xmldb" ;
import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "constants.xqm" ;

import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "xutil.xqm" ;
import module namespace test = "http://purl.org/atombeat/xquery/test" at "test.xqm" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "atomdb.xqm" ;
import module namespace atomsec = "http://purl.org/atombeat/xquery/atom-security" at "atom-security.xqm" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "../config/shared.xqm" ;




declare variable $test-collection-path as xs:string := "/test-security" ;
declare variable $test-member-name as xs:string := "test-member.atom" ;
declare variable $test-member-path as xs:string := concat( $test-collection-path , "/" , $test-member-name ) ;
declare variable $workspace-uri := concat( $config:content-service-url , "/" ) ;
declare variable $test-collection-uri := concat( $config:content-service-url , $test-collection-path ) ; 
declare variable $test-member-uri := concat( $config:content-service-url , $test-member-path ) ;




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
        
    let $member-db-path := atomdb:store-member( $test-collection-path , $test-member-name , $entry )    
    
    return ()

};




declare function local:test-workspace-descriptor() as item()*
{

    let $output := ( "test-workspace-descriptor..." )
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:acl>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">alice</atombeat:recipient>
                    <atombeat:permission>CREATE_COLLECTION</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">administrator</atombeat:recipient>
                    <atombeat:permission>CREATE_COLLECTION</atombeat:permission>
                </atombeat:ace>
            </atombeat:acl>
        </atombeat:security-descriptor>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)
    
    (: test allow by user name :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-COLLECTION
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "alice should be allowed to create collections" ) )
    
    (: test deny by user name :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-COLLECTION
    let $media-type := ()
    let $user := "bob"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "bob should not be allowed to create collections" ) )
    
    (: test deny by operation :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "alice should not be allowed to create collection members" ) )
    
    (: test allow by role :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-COLLECTION
    let $media-type := ()
    let $user := ()
    let $roles := ( "administrator" , "user" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "administrators should be allowed to create collections" ) )
    
    (: test deny by role :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-COLLECTION
    let $media-type := ()
    let $user := ()
    let $roles := ( "user" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "users should not be allowed to create collections" ) )
    
    return $output
    
};




declare function local:test-collection-descriptor() as item()*
{

    let $output := ( "test-collection-descriptor..." )
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:acl>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">administrator</atombeat:recipient>
                    <atombeat:permission>CREATE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>DENY</atombeat:type>
                    <atombeat:recipient type="user">bob</atombeat:recipient>
                    <atombeat:permission>CREATE_MEMBER</atombeat:permission>
                </atombeat:ace>
            </atombeat:acl>
        </atombeat:security-descriptor>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)

    let $collection-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:acl>
            
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">alice</atombeat:recipient>
                    <atombeat:permission>CREATE_MEMBER</atombeat:permission>
                </atombeat:ace>
                
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">author</atombeat:recipient>
                    <atombeat:permission>CREATE_MEMBER</atombeat:permission>
                </atombeat:ace>
                
                <!-- this should be overridden by workspace ACL -->
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">bob</atombeat:recipient>
                    <atombeat:permission>CREATE_MEMBER</atombeat:permission>
                </atombeat:ace>
                
                <!-- this should be overridden by workspace ACL -->
                <atombeat:ace>
                    <atombeat:type>DENY</atombeat:type>
                    <atombeat:recipient type="role">administrator</atombeat:recipient>
                    <atombeat:permission>CREATE_MEMBER</atombeat:permission>
                </atombeat:ace>
                
            </atombeat:acl>
            
        </atombeat:security-descriptor>
        
    let $collection-descriptor-doc-db-path := atomsec:store-collection-descriptor( $test-collection-path , $collection-descriptor )
    
    (: test allow by user name from collection acl :)
    
    let $request-path-info := $test-collection-path
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "alice should be allowed to create members of the /test-security collection" ) )
  
    (: test implicit deny by user name :)
    
    let $request-path-info := $test-collection-path
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := "fred"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "fred should not be allowed to create members of the /test-security collection (implicit deny)" ) )
  
    (: test deny by path :)
    
    let $request-path-info := "/another"
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "alice should not be allowed to create members of the /another collection" ) )
  
    (: test allow by role from collection acl :)
    
    let $request-path-info := $test-collection-path
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := ()
    let $roles := ( "author" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "authors should be allowed to create members of the /test-security collection" ) )
  
    (: test implicit deny by role :)
    
    let $request-path-info := $test-collection-path
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := ()
    let $roles := ( "user" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "users should not be allowed to create members of the /test-security collection" ) )
  
    (: test allow by role from workspace acl :)
    
    let $request-path-info := $test-collection-path
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := ()
    let $roles := ( "administrator" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "administrators should be allowed to create members of any collection" ) )
  
    (: test workspace acl overrides collection acl :)
  
    let $request-path-info := $test-collection-path
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := "bob"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "bob should not be allowed to create members of the /test-security collection (explicit deny)" ) )
  
    return $output
    
};




declare function local:test-resource-descriptor()
{

    let $output := ( "test-resource-descriptor..." )
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:acl>

                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">administrator</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>

                <atombeat:ace>
                    <atombeat:type>DENY</atombeat:type>
                    <atombeat:recipient type="role">troublemaker</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>

            </atombeat:acl>
        </atombeat:security-descriptor>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)

    let $collection-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:acl>

                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">editor</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>

                <atombeat:ace>
                    <atombeat:type>DENY</atombeat:type>
                    <atombeat:recipient type="user">bob</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>

                <!-- this should be overridden by workspace ACL -->
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">troublemaker</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>

                <!-- this should be overridden by workspace ACL -->
                <atombeat:ace>
                    <atombeat:type>DENY</atombeat:type>
                    <atombeat:recipient type="role">administrator</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>

            </atombeat:acl>
        </atombeat:security-descriptor>
        
    let $collection-descriptor-doc-db-path := atomsec:store-collection-descriptor( $test-collection-path , $collection-descriptor )

    let $resource-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:acl>

                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">alice</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>

                <!-- this should be overridden by collection ACL -->
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">bob</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>

                <!-- this should be overridden by collection ACL -->
                <atombeat:ace>
                    <atombeat:type>DENY</atombeat:type>
                    <atombeat:recipient type="role">editor</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>

            </atombeat:acl>
        </atombeat:security-descriptor>
        
    let $resource-descriptor-doc-db-path := atomsec:store-resource-descriptor( $test-member-path , $resource-descriptor )  
    
    (: test allow by user name from resource acl :)
  
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "alice should be allowed to update the resource" ) )
 
    (: test implicit deny by user name :)
   
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := "fred"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "fred should not be allowed to update the resource" ) )
  
    (: test deny by operation :)
   
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-DELETE-MEMBER
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "alice should not be allowed to delete the resource" ) )
   
    (: test allow by role from collection acl :)
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := ()
    let $roles := ( "editor" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "editors should be allowed to update any member of the collection" ) )
   
    (: test allow by role from workspace acl :)
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := ()
    let $roles := ( "administrator" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "administrators should be allowed to update any member of any collection" ) )
    
    (: test deny by role :)
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := ()
    let $roles := ( "user" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "users should not be allowed to update the resource" ) )
   
    (: test deny by operation :)
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-DELETE-MEMBER
    let $media-type := ()
    let $user := ()
    let $roles := ( "editor" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "editors should not be allowed to delete the resource" ) )
   
    (: test deny by operation :)

    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-DELETE-MEMBER
    let $media-type := ()
    let $user := ()
    let $roles := ( "administrator" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "administrators should not be allowed to delete the resource" ) )
 
    (: test collection acl overrides resource acl :)
  
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := "bob"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "bob should not be allowed to update members of the /test-security collection (explicit deny)" ) )
  
    (: test workspace acl overrides collection acl :)
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := ()
    let $roles := ( "troublemaker" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "troublemakers should not be allowed to update any member of the collection" ) )
  
    return $output
};




declare function local:test-wildcards() as item()*
{

    let $output := ( "test-wildcards..." )
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:acl>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">*</atombeat:recipient>
                    <atombeat:permission>CREATE_COLLECTION</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">administrator</atombeat:recipient>
                    <atombeat:permission>*</atombeat:permission>
                </atombeat:ace>
            </atombeat:acl>
        </atombeat:security-descriptor>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)
    
    (: test allow by wildcard user name :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-COLLECTION
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "any user should be allowed to create collections" ) )
    
    (: test allow by wildcard operation :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := ()
    let $roles := ( "administrator" )
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "administrators should be allowed any operation" ) )
    
    return $output

};




declare function local:test-media-range-condition() as item()*
{

    let $output := ( "test-media-range-condition..." )
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:acl>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">alice</atombeat:recipient>
                    <atombeat:permission>CREATE_MEDIA</atombeat:permission>
                    <atombeat:conditions>
                        <atombeat:condition type="mediarange">*/*</atombeat:condition>
                    </atombeat:conditions>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">bob</atombeat:recipient>
                    <atombeat:permission>CREATE_MEDIA</atombeat:permission>
                    <atombeat:conditions>
                        <atombeat:condition type="mediarange">text/*</atombeat:condition>
                    </atombeat:conditions>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">jane</atombeat:recipient>
                    <atombeat:permission>CREATE_MEDIA</atombeat:permission>
                    <atombeat:conditions>
                        <atombeat:condition type="mediarange">application/xml</atombeat:condition>
                    </atombeat:conditions>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">alice</atombeat:recipient>
                    <atombeat:permission>CREATE_MEMBER</atombeat:permission>
                    <atombeat:conditions>
                        <!-- should be ignored -->
                        <atombeat:condition type="mediarange">foo/bar</atombeat:condition>
                    </atombeat:conditions> 
                </atombeat:ace>
            </atombeat:acl>
        </atombeat:security-descriptor>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)

    (: test allow by any media type/subtype :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-MEDIA
    let $media-type := $CONSTANT:MEDIA-TYPE-XML
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "alice should be allowed to create media with any media type/subtype" ) )
    
    (: test allow by any media subtype :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-MEDIA
    let $media-type := "text/plain"
    let $user := "bob"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "bob should be allowed to create media type text with any media subtype" ) )
    
    (: test deny by media type :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-MEDIA
    let $media-type := $CONSTANT:MEDIA-TYPE-XML
    let $user := "bob"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "bob should only be allowed to create media type text" ) )
    
    (: test allow by media type/subtype :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-MEDIA
    let $media-type := $CONSTANT:MEDIA-TYPE-XML
    let $user := "jane"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "jane should be allowed to create media type application/xml" ) )
    
    (: test deny by media type/subtype :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-MEDIA
    let $media-type := "application/foo"
    let $user := "jane"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "jane should only be allowed to create media type application/xml" ) )
    
    (: test deny by media type/subtype :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-MEDIA
    let $media-type := "foo/xml"
    let $user := "jane"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "jane should only be allowed to create media type application/xml" ) )
    
    (: test deny by media type/subtype :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-MEDIA
    let $media-type := "foo/bar"
    let $user := "jane"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "jane should only be allowed to create media type application/xml" ) )
    
    (: test media range is ignored if operation does not involve media :)
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "media range should be ignored for operations not on media" ) )
    
    return $output
    
};




declare function local:test-match-request-path-info-condition() as item()*
{

    let $output := ( "test-match-request-path-info-condition..." )
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:acl>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">alice</atombeat:recipient>
                    <atombeat:permission>CREATE_COLLECTION</atombeat:permission>
                    <atombeat:conditions>
                        <atombeat:condition type="match-request-path-info">^/foo/</atombeat:condition>
                    </atombeat:conditions>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">bob</atombeat:recipient>
                    <atombeat:permission>CREATE_COLLECTION</atombeat:permission>
                    <atombeat:conditions>
                        <atombeat:condition type="match-request-path-info">^/foo/[^/]+$</atombeat:condition>
                    </atombeat:conditions>
                </atombeat:ace>
            </atombeat:acl>
        </atombeat:security-descriptor>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)
    
    let $request-path-info := "/foo/bar"
    let $permission := $CONSTANT:OP-CREATE-COLLECTION
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "alice should be allowed to create collections with path beginning /foo/" ) )
        
    let $request-path-info := "/foo/bar/baz"
    let $permission := $CONSTANT:OP-CREATE-COLLECTION
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "alice should be allowed to create collections with path beginning /foo/" ) )
        
    let $request-path-info := "/bar/baz"
    let $permission := $CONSTANT:OP-CREATE-COLLECTION
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "alice should only be allowed to create collections with path beginning /foo/" ) )
        
    let $request-path-info := "/foo/bar"
    let $permission := $CONSTANT:OP-CREATE-COLLECTION
    let $media-type := ()
    let $user := "bob"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "bob should be allowed to create collections with path matching /foo/*" ) )
        
    let $request-path-info := "/foo/bar/baz"
    let $permission := $CONSTANT:OP-CREATE-COLLECTION
    let $media-type := ()
    let $user := "bob"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "bob should only be allowed to create collections with path matching /foo/*" ) )
                
    let $request-path-info := "/bar/baz"
    let $permission := $CONSTANT:OP-CREATE-COLLECTION
    let $media-type := ()
    let $user := "bob"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "bob should only be allowed to create collections with path matching /foo/*" ) )
                
    return $output
    
};




declare function local:test-inline-groups() as item()*
{

    let $output := ( "test-inline-groups..." )
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:groups>
                <atombeat:group id="readers">
                    <atombeat:member>richard</atombeat:member>
                    <atombeat:member>rebecca</atombeat:member>
                </atombeat:group>
                <atombeat:group id="authors">
                    <atombeat:member>alice</atombeat:member>
                    <atombeat:member>austin</atombeat:member>
                </atombeat:group>
                <atombeat:group id="editors">
                    <atombeat:member>emma</atombeat:member>
                    <atombeat:member>edward</atombeat:member>
                </atombeat:group>
            </atombeat:groups>
            <atombeat:acl>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="group">readers</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="group">authors</atombeat:recipient>
                    <atombeat:permission>CREATE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="group">editors</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>
            </atombeat:acl>
        </atombeat:security-descriptor>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)

    (: test readers :)
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-RETRIEVE-MEMBER
    let $media-type := ()
    let $user := "richard"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "readers should be allowed to retrieve collection members" ) )
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-RETRIEVE-MEMBER
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "readers should be allowed to retrieve collection members" ) )
    
    let $request-path-info := $test-collection-path
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := "richard"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "readers should not be allowed to create collection members" ) )
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "readers should not be allowed to update collection members" ) )
    
    (: test authors :)
    
    let $request-path-info := $test-collection-path
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "authors should be allowed to create collection members" ) )
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := "austin"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "authors should not be allowed to update collection members" ) )
    
    (: test editors :)
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := "emma"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "editors should be allowed to update collection members" ) )
    
    let $request-path-info := $test-collection-path
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := "edward"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "editors should not be allowed to create collection members" ) )
    
    return $output
};



declare function local:test-reference-workspace-groups() as item()*
{

    let $output := ( "test-reference-workspace-groups..." )
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:groups>
                <atombeat:group id="readers">
                    <atombeat:member>richard</atombeat:member>
                    <atombeat:member>rebecca</atombeat:member>
                </atombeat:group>
                <atombeat:group id="authors">
                    <atombeat:member>alice</atombeat:member>
                    <atombeat:member>austin</atombeat:member>
                </atombeat:group>
                <atombeat:group id="editors">
                    <atombeat:member>emma</atombeat:member>
                    <atombeat:member>edward</atombeat:member>
                </atombeat:group>
            </atombeat:groups>
        </atombeat:security-descriptor>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)
    
    let $collection-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:groups>
                <atombeat:group id="readers" src="{$workspace-uri}"/>
                <atombeat:group id="authors" src="{$workspace-uri}"/>
                <atombeat:group id="editors" src="{$workspace-uri}"/>
            </atombeat:groups>
            <atombeat:acl>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="group">readers</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="group">authors</atombeat:recipient>
                    <atombeat:permission>CREATE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="group">editors</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>
            </atombeat:acl>
        </atombeat:security-descriptor>
    
    let $collection-descriptor-doc-db-path := atomsec:store-collection-descriptor( $test-collection-path , $collection-descriptor )
    
    (: test readers :)
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-RETRIEVE-MEMBER
    let $media-type := ()
    let $user := "richard"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "readers should be allowed to retrieve collection members" ) )
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-RETRIEVE-MEMBER
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "readers should be allowed to retrieve collection members" ) )
    
    let $request-path-info := $test-collection-path
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := "richard"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "readers should not be allowed to create collection members" ) )
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "readers should not be allowed to update collection members" ) )
    
    (: test authors :)
    
    let $request-path-info := $test-collection-path
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "authors should be allowed to create collection members" ) )
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := "austin"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "authors should not be allowed to update collection members" ) )
    
    (: test editors :)
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := "emma"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "editors should be allowed to update collection members" ) )
    
    let $request-path-info := $test-collection-path
    let $permission := $CONSTANT:OP-CREATE-MEMBER
    let $media-type := ()
    let $user := "edward"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "editors should not be allowed to create collection members" ) )
    
    return $output
};



declare function local:test-reference-collection-groups() as item()*
{

    (: TODO update for new ACL syntax :)

    let $output := ( "test-reference-collection-groups..." )
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor>
        </atombeat:security-descriptor>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)
    
    let $collection-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:groups>
                <atombeat:group id="readers">
                    <atombeat:member>richard</atombeat:member>
                    <atombeat:member>rebecca</atombeat:member>
                </atombeat:group>
                <atombeat:group id="editors">
                    <atombeat:member>emma</atombeat:member>
                    <atombeat:member>edward</atombeat:member>
                </atombeat:group>
            </atombeat:groups>
        </atombeat:security-descriptor>
        
    let $collection-descriptor-doc-db-path := atomsec:store-collection-descriptor( $test-collection-path , $collection-descriptor )
    
    let $resource-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:groups>
                <atombeat:group id="readers" src="{$test-collection-uri}"/>
                <atombeat:group id="editors" src="{$test-collection-uri}"/>
            </atombeat:groups>
            <atombeat:acl>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="group">readers</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="group">editors</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>
            </atombeat:acl>
        </atombeat:security-descriptor>

    let $resource-descriptor-doc-db-path := atomsec:store-resource-descriptor( $test-member-path , $resource-descriptor )  
    
    (: test readers :)
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-RETRIEVE-MEMBER
    let $media-type := ()
    let $user := "richard"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "readers should be allowed to retrieve the resource" ) )
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-RETRIEVE-MEMBER
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "readers should be allowed to retrieve the resource" ) )
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "readers should not be allowed to update the resource" ) )
    
    (: test editors :)
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := "emma"
    let $roles := ()
    let $decision := atomsec:decide($user , $roles , $request-path-info , $permission , $media-type)
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "editors should be allowed to update the resource" ) )
    
    let $request-path-info := $test-collection-path
    let $permission := $CONSTANT:OP-DELETE-MEMBER
    let $media-type := ()
    let $user := "edward"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "editors should not be allowed to delete the resource" ) )
    
    return $output
};



declare function local:test-reference-resource-groups() as item()*
{

    let $output := ( "test-reference-resource-groups..." )
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor>
        </atombeat:security-descriptor>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)
    
    let $collection-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:groups>
                <atombeat:group id="readers" src="{$test-member-uri}"/>
                <atombeat:group id="editors" src="{$test-member-uri}"/>
            </atombeat:groups>
            <atombeat:acl>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="group">readers</atombeat:recipient>
                    <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="group">editors</atombeat:recipient>
                    <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
                </atombeat:ace>
            </atombeat:acl>
        </atombeat:security-descriptor>
        
    let $collection-descriptor-doc-db-path := atomsec:store-collection-descriptor( $test-collection-path , $collection-descriptor )
    
    let $resource-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:groups>
                <atombeat:group id="readers">
                    <atombeat:member>richard</atombeat:member>
                    <atombeat:member>rebecca</atombeat:member>
                </atombeat:group>
                <atombeat:group id="editors">
                    <atombeat:member>emma</atombeat:member>
                    <atombeat:member>edward</atombeat:member>
                </atombeat:group>
            </atombeat:groups>
        </atombeat:security-descriptor>

    let $resource-descriptor-doc-db-path := atomsec:store-resource-descriptor( $test-member-path , $resource-descriptor )  
    
    (: test readers :)
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-RETRIEVE-MEMBER
    let $media-type := ()
    let $user := "richard"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "readers should be allowed to retrieve the resource" ) )
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-RETRIEVE-MEMBER
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "readers should be allowed to retrieve the resource" ) )
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := "rebecca"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "readers should not be allowed to update the resource" ) )
    
    (: test editors :)
    
    let $request-path-info := $test-member-path
    let $permission := $CONSTANT:OP-UPDATE-MEMBER
    let $media-type := ()
    let $user := "emma"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "editors should be allowed to update the resource" ) )
    
    let $request-path-info := $test-collection-path
    let $permission := $CONSTANT:OP-DELETE-MEMBER
    let $media-type := ()
    let $user := "edward"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "editors should not be allowed to delete the resource" ) )
    
    return $output
};




declare function local:test-update-workspace-descriptor() as item()*
{

    let $output := ( "test-update-workspace-descriptor..." )
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:acl>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">alice</atombeat:recipient>
                    <atombeat:permission>CREATE_COLLECTION</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="role">administrator</atombeat:recipient>
                    <atombeat:permission>CREATE_COLLECTION</atombeat:permission>
                </atombeat:ace>
            </atombeat:acl>
        </atombeat:security-descriptor>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)
    
    let $acl := atomsec:retrieve-workspace-descriptor()
    
    let $output := ( $output , test:assert-equals( 2 , count($acl/atombeat:acl/*) , "expect 2 ACEs" ) )
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor/>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)
    
    let $acl := atomsec:retrieve-workspace-descriptor()
    
    let $output := ( $output , test:assert-equals( 0 , count($acl/atombeat:acl/*) , "expect 0 ACEs" ) )
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:acl>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">alice</atombeat:recipient>
                    <atombeat:permission>CREATE_COLLECTION</atombeat:permission>
                </atombeat:ace>
            </atombeat:acl>
        </atombeat:security-descriptor>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)
    
    let $acl := atomsec:retrieve-workspace-descriptor()
    
    let $output := ( $output , test:assert-equals( 1 , count($acl/atombeat:acl/*) , "expect 1 ACE" ) )
    
    return $output
    
};




declare function local:test-processing-order() as item()*
{

    let $output := ( "test-processing-order..." )
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:acl>
                <atombeat:ace>
                    <atombeat:type>ALLOW</atombeat:type>
                    <atombeat:recipient type="user">alice</atombeat:recipient>
                    <atombeat:permission>CREATE_COLLECTION</atombeat:permission>
                </atombeat:ace>
                <atombeat:ace>
                    <atombeat:type>DENY</atombeat:type>
                    <atombeat:recipient type="user">alice</atombeat:recipient>
                    <atombeat:permission>CREATE_COLLECTION</atombeat:permission>
                </atombeat:ace>
            </atombeat:acl>
        </atombeat:security-descriptor>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)

    (: first matching ACE should be chosen :)
    
    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-COLLECTION
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "ALLOW" , $decision , "alice should be allowed to create collections" ) )
    
    return $output

};



declare function local:test-whitespace() as item()*
{

    let $output := ( "test-whitespace..." )
    
    (: to improve efficiency of acl processing, whitespace is no longer allowed :)
    
    let $workspace-descriptor :=
        <atombeat:security-descriptor>
            <atombeat:acl>
                <atombeat:ace>
                    <atombeat:type> 
                            ALLOW       
                    </atombeat:type>
                    <atombeat:recipient type="user">
                         alice      
                    </atombeat:recipient>
                    <atombeat:permission>
                             CREATE_COLLECTION      
                    </atombeat:permission>
                </atombeat:ace>
            </atombeat:acl>
        </atombeat:security-descriptor>
        
    let $workspace-descriptor-doc-db-path := atomsec:store-workspace-descriptor($workspace-descriptor)

    let $request-path-info := "/foo"
    let $permission := $CONSTANT:OP-CREATE-COLLECTION
    let $media-type := ()
    let $user := "alice"
    let $roles := ()
    let $decision := atomsec:decide( $user , $roles , $request-path-info , $permission , $media-type )
    let $output := ( $output , test:assert-equals( "DENY" , $decision , "alice should not be allowed to create collections" ) )
    
    return $output

};



declare function local:main() as item()*
{

    let $setup := local:setup()
    let $output := (
        local:test-workspace-descriptor() ,
        local:test-collection-descriptor(),
        local:test-resource-descriptor() ,
        local:test-wildcards() ,
        local:test-media-range-condition() ,
        local:test-match-request-path-info-condition() ,
        local:test-inline-groups() ,
        local:test-reference-workspace-groups() ,
        local:test-reference-collection-groups() ,
        local:test-reference-resource-groups() ,
        local:test-update-workspace-descriptor() ,
        local:test-processing-order() ,
        local:test-whitespace()
    )
    let $response-type := response:set-header( "Content-Type" , "text/plain" )
    return $output
    
};




local:main()

