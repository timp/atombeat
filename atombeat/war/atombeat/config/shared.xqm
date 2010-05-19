xquery version "1.0";

module namespace config = "http://atombeat.org/xquery/config";

declare namespace atombeat = "http://atombeat.org/xmlns" ;


import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace xutil = "http://atombeat.org/xquery/xutil" at "../lib/xutil.xqm" ;


(:
 : The base URL for the Atom service. This URL will be prepended to all edit
 : and self link href values.
 :)
declare variable $config:service-url as xs:string := "http://localhost:8081/atombeat/atombeat/content" ;


(:
 : The base URL for the History service. This URL will be prepended to all 
 : history link href values.
 :)
declare variable $config:history-service-url as xs:string := "http://localhost:8081/atombeat/atombeat/history" ;
 

declare variable $config:security-service-url as xs:string := "http://localhost:8081/atombeat/atombeat/security" ;


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
    (: xutil:random-alphanumeric( 6 ) :) (: N.B. it's OK to use randoms because atomdb will automatically check for collisions :)
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



(: TODO :)

(:
 : A default workspace ACL, customise for your environment.
 :)
declare variable $config:default-workspace-security-descriptor := 
    <atombeat:security-descriptor>
        <atombeat:acl>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>CREATE_COLLECTION</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>UPDATE_COLLECTION</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>LIST_COLLECTION</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>CREATE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>DELETE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>CREATE_MEDIA</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEDIA</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>UPDATE_MEDIA</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>DELETE_MEDIA</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>UPDATE_ACL</atombeat:permission>
            </atombeat:ace>
            <!-- you could also use a wildcard -->
            <!--
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>*</atombeat:permission>
            </atombeat:ace>
            -->
        </atombeat:acl>
    </atombeat:security-descriptor>
;


(: TODO :)

(:
 : A function to generate default collection security descriptor for any new
 : collection created via HTTP, customise for your environment.
 :)
declare function config:default-collection-security-descriptor(
    $request-path-info as xs:string ,
    $user as xs:string?
) as element(atombeat:security-descriptor)
{ 
    <atombeat:security-descriptor>
        <atombeat:acl>
        
            <!--  
            Authors can create entries and media, and can list the collection,
            but can only retrieve resources they have created.
            -->
            
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_AUTHOR</atombeat:recipient>
                <atombeat:permission>CREATE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_AUTHOR</atombeat:recipient>
                <atombeat:permission>CREATE_MEDIA</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_AUTHOR</atombeat:recipient>
                <atombeat:permission>LIST_COLLECTION</atombeat:permission>
            </atombeat:ace>
            
            <!--
            Editors can list the collection, retrieve and update any member.
            -->
            
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_EDITOR</atombeat:recipient>
                <atombeat:permission>LIST_COLLECTION</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_EDITOR</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_EDITOR</atombeat:recipient>
                <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_EDITOR</atombeat:recipient>
                <atombeat:permission>DELETE_MEMBER</atombeat:permission>
            </atombeat:ace>

            <!-- Media editors -->
            
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_MEDIA_EDITOR</atombeat:recipient>
                <atombeat:permission>LIST_COLLECTION</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_MEDIA_EDITOR</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_MEDIA_EDITOR</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEDIA</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_MEDIA_EDITOR</atombeat:recipient>
                <atombeat:permission>UPDATE_MEDIA</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_MEDIA_EDITOR</atombeat:recipient>
                <atombeat:permission>DELETE_MEDIA</atombeat:permission>
            </atombeat:ace>
            
            <!--
            Readers can list the collection and retrieve any member.
            -->
            
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_READER</atombeat:recipient>
                <atombeat:permission>LIST_COLLECTION</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_READER</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_READER</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEDIA</atombeat:permission>
            </atombeat:ace>
            
            <!--
            Data authors can only create media resources with a specific media
            type.
            -->
            
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_DATA_AUTHOR</atombeat:recipient>
                <atombeat:permission>CREATE_MEDIA</atombeat:permission>
                <atombeat:conditions>
                    <atombeat:condition type="mediarange">application/vnd.ms-excel</atombeat:condition>
                </atombeat:conditions>
            </atombeat:ace>
            
        </atombeat:acl>
    </atombeat:security-descriptor>
};


(: TODO :)

(:
 : A function to generate default resource security descriptor for any new
 : collection members or media resources, customise for your environment.
 :)
declare function config:default-resource-security-descriptor(
    $request-path-info as xs:string ,
    $user as xs:string
) as element(atombeat:security-descriptor)
{

    <atombeat:security-descriptor>
        <atombeat:acl>
		
		    <!-- 
		    The user who created the resource has full rights.
		    -->
		    
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="user">{$user}</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="user">{$user}</atombeat:recipient>
                <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="user">{$user}</atombeat:recipient>
                <atombeat:permission>DELETE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="user">{$user}</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEDIA</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="user">{$user}</atombeat:recipient>
                <atombeat:permission>UPDATE_MEDIA</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="user">{$user}</atombeat:recipient>
                <atombeat:permission>DELETE_MEDIA</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="user">{$user}</atombeat:recipient>
                <atombeat:permission>UPDATE_ACL</atombeat:permission>
            </atombeat:ace>
            
        </atombeat:acl>
    </atombeat:security-descriptor>

	(: you could also use groups, which makes it a bit easier to add more owners :)
		
	(:
    <atombeat:security-descriptor>
		<atombeat:groups>
			<atombeat:group id="owners">
                <atombeat:member>{$user}</atombeat:member>
			</atombeat:group>
		</atombeat:groups>
        <atombeat:acl>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="group">owners</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="group">owners</atombeat:recipient>
                <atombeat:permission>UPDATE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="group">owners</atombeat:recipient>
                <atombeat:permission>DELETE_MEMBER</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="group">owners</atombeat:recipient>
                <atombeat:permission>UPDATE_ACL</atombeat:permission>
            </atombeat:ace>
		</atombeat:acl>
	</atombeat:security-descriptor>
	:)
};

