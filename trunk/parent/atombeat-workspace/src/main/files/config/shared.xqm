xquery version "1.0";

module namespace config = "http://purl.org/atombeat/xquery/config";

declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;


import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace system = "http://exist-db.org/xquery/system" ;

import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;



(:~
 : The eXist user to run the AtomBeat queries as. Will require privileges to
 : create collections.
 :)
declare variable $config:exist-user as xs:string := "admin" ;
declare variable $config:exist-password as xs:string := "" ;


(:~
 : The base URL for this workspace service, used in atom IDs and edit link URIs.
 :)
declare variable $config:service-url-base as xs:string := concat( "http://" , request:get-server-name() , ":" , request:get-server-port() , request:get-context-path() , "/workspace" ) ;
(: declare variable $config:service-url-base as xs:string := "http://example.org/atombeat/workspace" ; :)


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
declare variable $config:base-collection-path as xs:string := "/db/atombeat/content" ;


(:
 : The base collection within which to store access control lists.
 :)
declare variable $config:base-security-collection-path as xs:string := "/db/atombeat/security" ;


(: 
 : The resource name used to store feed documents in the database.
 :)
declare variable $config:feed-doc-name as xs:string := ".feed" ;


(:~
 : Set the strategy to use when storing media resources. If set to "FILE", new
 : media resources will be stored on the file system as ordinary files. If set
 : to "DB", new media resources will be stored as binary objects in the eXist
 : database. The "FILE" mode is more scalable, as file data is streamed to and
 : from the file system, so AtomBeat can handle larger files. The "DB" mode is
 : less scalable because file data is read into memory before being stored in
 : the database, but is more convenient for backup and restore because you don't
 : have to worry about keeping backups of the filesystem and the eXist database
 : in sync.
 :)
declare variable $config:media-storage-mode as xs:string := "FILE" (: "DB" :) ;


(:~ 
 : Only relevant if media-storage-mode is "FILE". The base directory where all
 : media files will be stored. N.B. the process running AtomBeat MUST have 
 : permission to create this directory and any child directories.
 :)
(: declare variable $config:media-storage-dir as xs:string := "/srv/atombeat/media" ; :)
declare variable $config:media-storage-dir as xs:string :=
    let $home := system:get-exist-home()
    return
        if (ends-with($home, "WEB-INF")) then
            concat($home, "/media" ) 
        else
            concat($home, "/webapp/WEB-INF/media" )
; 

(:~
 : The function that generates an identifier token to use when constructing the
 : Atom ID for a new collection member.
 :)
declare function config:generate-identifier(
    $collection-path-info as xs:string
) as xs:string
{
    util:uuid()
    (: xutil:random-alphanumeric( 6 ) :) (: N.B. it's OK to use randoms because atomdb will automatically check for collisions within a collection :)
    (: xutil:random-alphanumeric( 7 , 21 , "0123456789abcdefghijk" , "abcdefghjkmnpqrstuxyz" ) :) 
};


