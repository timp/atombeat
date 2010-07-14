xquery version "1.0";

module namespace security-config = "http://purl.org/atombeat/xquery/security-config";

declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;


import module namespace util = "http://exist-db.org/xquery/util" ;

import module namespace xutil = "http://purl.org/atombeat/xquery/xutil" at "../lib/xutil.xqm" ;
import module namespace config = "http://purl.org/atombeat/xquery/config" at "config.xqm" ;



(:
 : A default workspace ACL, customise for your environment.
 :)
declare variable $security-config:default-workspace-security-descriptor := 
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
                <atombeat:permission>RETRIEVE_WORKSPACE_ACL</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>RETRIEVE_COLLECTION_ACL</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEMBER_ACL</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEDIA_ACL</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>UPDATE_WORKSPACE_ACL</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>UPDATE_COLLECTION_ACL</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>UPDATE_MEMBER_ACL</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>UPDATE_MEDIA_ACL</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>MULTI_CREATE</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>RETRIEVE_HISTORY</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_ADMINISTRATOR</atombeat:recipient>
                <atombeat:permission>RETRIEVE_REVISION</atombeat:permission>
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



(:
 : A function to generate default collection security descriptor for any new
 : collection created via HTTP, customise for your environment.
 :)
declare function security-config:default-collection-security-descriptor(
    $collection-path-info as xs:string ,
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
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_EDITOR</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEMBER_ACL</atombeat:permission>
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
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_READER</atombeat:recipient>
                <atombeat:permission>RETRIEVE_HISTORY</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="role">ROLE_READER</atombeat:recipient>
                <atombeat:permission>RETRIEVE_REVISION</atombeat:permission>
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




(:
 : A function to generate default security descriptor for any new
 : collection members, customise for your environment.
 :)
declare function security-config:default-member-security-descriptor(
    $collection-path-info as xs:string ,
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
                <atombeat:permission>RETRIEVE_MEMBER_ACL</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="user">{$user}</atombeat:recipient>
                <atombeat:permission>UPDATE_MEMBER_ACL</atombeat:permission>
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
                <atombeat:recipient type="user">{$user}</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEMBER_ACL</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="user">{$user}</atombeat:recipient>
                <atombeat:permission>UPDATE_MEMBER_ACL</atombeat:permission>
            </atombeat:ace>
		</atombeat:acl>
	</atombeat:security-descriptor>
	:)
};



(:
 : A function to generate default security descriptor for any new
 : media resources, customise for your environment.
 :)
declare function security-config:default-media-security-descriptor(
    $collection-path-info as xs:string ,
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
                <atombeat:permission>RETRIEVE_MEDIA_ACL</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="user">{$user}</atombeat:recipient>
                <atombeat:permission>UPDATE_MEDIA_ACL</atombeat:permission>
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
                <atombeat:permission>RETRIEVE_MEDIA</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="group">owners</atombeat:recipient>
                <atombeat:permission>UPDATE_MEDIA</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="group">owners</atombeat:recipient>
                <atombeat:permission>DELETE_MEDIA</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="user">{$user}</atombeat:recipient>
                <atombeat:permission>RETRIEVE_MEDIA_ACL</atombeat:permission>
            </atombeat:ace>
            <atombeat:ace>
                <atombeat:type>ALLOW</atombeat:type>
                <atombeat:recipient type="user">{$user}</atombeat:recipient>
                <atombeat:permission>UPDATE_MEDIA_ACL</atombeat:permission>
            </atombeat:ace>
		</atombeat:acl>
	</atombeat:security-descriptor>
	:)
};



