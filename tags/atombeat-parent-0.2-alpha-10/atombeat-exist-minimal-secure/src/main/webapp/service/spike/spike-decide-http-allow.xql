xquery version '1.0';

declare namespace atom = 'http://www.w3.org/2005/Atom' ;
declare namespace atombeat = 'http://purl.org/atombeat/xmlns' ;

import module namespace text = 'http://exist-db.org/xquery/text' ;
import module namespace xmldb = 'http://exist-db.org/xquery/xmldb' ;
import module namespace util = 'http://exist-db.org/xquery/util' ;

import module namespace CONSTANT = 'http://purl.org/atombeat/xquery/constants' at '../lib/constants.xqm' ;

import module namespace xutil = 'http://purl.org/atombeat/xquery/xutil' at '../lib/xutil.xqm' ;
import module namespace test = 'http://purl.org/atombeat/xquery/test' at '../lib/test.xqm' ;
import module namespace atomdb = 'http://purl.org/atombeat/xquery/atomdb' at '../lib/atomdb.xqm' ;
import module namespace atomsec = 'http://purl.org/atombeat/xquery/atom-security' at '../lib/atom-security.xqm' ;
import module namespace config = 'http://purl.org/atombeat/xquery/config' at '../config/shared.xqm' ;



let $login := xmldb:login( '/' , 'admin' , '' )

let $test-collection-path as xs:string := '/spike-decide-http-allow' 
let $test-member-id as xs:string := 'xyz' 
let $test-member-path as xs:string := concat( $test-collection-path , '/' , $test-member-id ) 
let $workspace-uri := concat( $config:edit-link-uri-base , '/' ) 
let $test-collection-uri := concat( $config:edit-link-uri-base , $test-collection-path )  
let $test-member-uri := concat( $config:edit-link-uri-base , $test-member-path ) 

let $feed :=     
    <atom:feed>
        <atom:title>TEST COLLECTION</atom:title>
    </atom:feed>
    
let $collection-db-path := atomdb:create-collection( $test-collection-path, $feed , 'adam' )
    
let $entry :=
    <atom:entry>
        <atom:title>TEST ENTRY</atom:title>
    </atom:entry>
        
let $entry-created := atomdb:create-member( $test-collection-path , $test-member-id , $entry , 'adam' )    
    
let $workspace-descriptor :=
    <atombeat:security-descriptor>
        <atombeat:acl>

            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">administrator</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
            </atombeat:ace>

            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">administrator</atombeat:recipient>
                <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
            </atombeat:ace>

            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">administrator</atombeat:recipient>
                <atombeat:permission>DELETE_MEMBER</atombeat:permission>
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

            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">troublemaker</atombeat:recipient>
                <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
            </atombeat:ace>

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

            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="user">bob</atombeat:recipient>
                <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
            </atombeat:ace>

            <atombeat:ace>
                <atombeat:type>DENY</atombeat:type>
                <atombeat:recipient type="role">editor</atombeat:recipient>
                <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
            </atombeat:ace>

        </atombeat:acl>
    </atombeat:security-descriptor>
    
let $resource-descriptor-doc-db-path := atomsec:store-resource-descriptor( $test-member-path , $resource-descriptor )  

let $foo := response:set-header( 'Content-Type' , 'text/plain' )
return ( 
    atomsec:decide-http-allow( $test-member-uri , 'alice' , () ) ,
    atomsec:decide-http-allow( $test-member-uri , () , ('administrator') )
)


