xquery version "1.0";

module namespace config = "http://purl.org/atombeat/xquery/config";

declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;


import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;


declare variable $config:service-url-base as xs:string := "http://localhost:8081/atombeat/atombeat" ;


(:
 : The base URL for the Atom service. This URL will be prepended to all edit
 : and self link href values.
 :)
declare variable $config:content-service-url as xs:string := concat( $config:service-url-base , "/content" ) ;


(:
 : The base URL for the History service. This URL will be prepended to all 
 : history link href values.
 :)
declare variable $config:history-service-url as xs:string := concat( $config:service-url-base , "/history" ) ;
 

declare variable $config:security-service-url as xs:string := concat( $config:service-url-base , "/security" ) ;


(:
 : The name of the request attribute where the currently authenticated user
 : is stored. N.B. it is assumed that authentication will have been carried out
 : prior to the Atom service handling the request, e.g., by a filter, and that
 : the filter will have stored the authenticated user's name (ID) in the given
 : request attribute.
 :)
declare variable $config:user-name-request-attribute-key as xs:string := "user-name" ; 


(:
 : The name of the request attribute where the currently authenticated user's
 : roles are stored. N.B. it is assumed that authentication and retrieval of
 : the authenticated user's roles will have been carried outprior to the Atom 
 : service handling the request, e.g., by a filter, and that the filter will 
 : have stored the authenticated user's roles in the given request attribute.
 : TODO explain expected type of attribute value.
 :)
declare variable $config:user-roles-request-attribute-key as xs:string := "user-roles" ; 


(:~
 : If set to true, atom:author elements will be automatically created and populated
 : with currently authenticated user upon create member and create media. This ensures
 : atom:author element will always carry the identity of the user who initially
 : created the resource.
 :)
declare variable $config:auto-author as xs:boolean := true() ;


(:
 : If usernames should be treated as email addresses, set this to true(). (I.e.,
 : if users are logging in with their email address as their user ID.)
 :)
declare variable $config:user-name-is-email as xs:boolean := false() ;


(:
 : The base collection within which to store Atom collections and resources.
 : All paths will be relative to this base collection path.
 :)
declare variable $config:base-collection-path as xs:string := "/db/atom/content" ;


(:
 : The base collection within which to store access control lists.
 :)
declare variable $config:base-security-collection-path as xs:string := "/db/atom/security" ;


(: 
 : The resource name used to store feed documents in the database.
 :)
declare variable $config:feed-doc-name as xs:string := ".feed" ;


declare function config:generate-identifier(
    $collection-path-info as xs:string
) as xs:string
{
    util:uuid()
    (: xutil:random-alphanumeric( 6 ) :) (: N.B. it's OK to use randoms because atomdb will automatically check for collisions within a collection :)
    (: xutil:random-alphanumeric( 7 , 21 , "0123456789abcdefghijk" , "abcdefghjkmnpqrstuxyz" ) :) 
};


(:
 : Enable or disable the ACL-based security system.
 :)
declare variable $config:enable-security := true() ;


(:
 : The default security decision which will be applied if no ACL rules match 
 : a request. Either "DENY" or "ALLOW".
 :)
declare variable $config:default-security-decision := "DENY" ;


(:
 : The order in which to process the relevant access control lists for
 : any given operation. E.g., if "WORKSPACE" comes before "COLLECTION" then 
 : ACEs in the workspace ACL will take precedence over ACEs in the collection
 : ACLs.
 :)
declare variable $config:security-priority := ( "WORKSPACE" , "COLLECTION" , "RESOURCE") ;
(: declare variable $config:security-priority := ( "RESOURCE" , "COLLECTION" , "WORKSPACE") ; :)



