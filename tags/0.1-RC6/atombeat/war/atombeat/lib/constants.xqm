xquery version "1.0";

module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants";

declare variable $CONSTANT:METHOD-GET as xs:string 						:= "GET" ;
declare variable $CONSTANT:METHOD-POST as xs:string 					:= "POST" ;
declare variable $CONSTANT:METHOD-PUT as xs:string 						:= "PUT" ;
declare variable $CONSTANT:METHOD-DELETE as xs:string 					:= "DELETE" ;

declare variable $CONSTANT:STATUS-SUCCESS-OK as xs:integer 						:= 200 ;
declare variable $CONSTANT:STATUS-SUCCESS-CREATED as xs:integer 				:= 201 ;
declare variable $CONSTANT:STATUS-SUCCESS-NO-CONTENT as xs:integer 				:= 204 ;

declare variable $CONSTANT:STATUS-REDIRECT-NOT-MODIFIED as xs:integer           := 304 ;

declare variable $CONSTANT:STATUS-CLIENT-ERROR-BAD-REQUEST as xs:integer 			:= 400 ;
declare variable $CONSTANT:STATUS-CLIENT-ERROR-FORBIDDEN as xs:integer 				:= 403 ;
declare variable $CONSTANT:STATUS-CLIENT-ERROR-NOT-FOUND as xs:integer 				:= 404 ;
declare variable $CONSTANT:STATUS-CLIENT-ERROR-METHOD-NOT-ALLOWED as xs:integer     := 405 ;
declare variable $CONSTANT:STATUS-CLIENT-ERROR-PRECONDITION-FAILED as xs:integer    := 412 ;
declare variable $CONSTANT:STATUS-CLIENT-ERROR-UNSUPPORTED-MEDIA-TYPE as xs:integer := 415 ;

declare variable $CONSTANT:STATUS-SERVER-ERROR-INTERNAL-SERVER-ERROR as xs:integer     := 500 ;
declare variable $CONSTANT:STATUS-SERVER-ERROR-NOT-IMPLEMENTED as xs:integer           := 501 ;

declare variable $CONSTANT:HEADER-ALLOW as xs:string 					:= "Allow" ;
declare variable $CONSTANT:HEADER-ACCEPT as xs:string 					:= "Accept" ;
declare variable $CONSTANT:HEADER-CONTENT-TYPE as xs:string             := "Content-Type" ;
declare variable $CONSTANT:HEADER-CONTENT-LOCATION as xs:string         := "Content-Location" ;
declare variable $CONSTANT:HEADER-CONTENT-DISPOSITION as xs:string 		:= "Content-Disposition" ;
declare variable $CONSTANT:HEADER-LOCATION as xs:string 				:= "Location" ;
declare variable $CONSTANT:HEADER-SLUG as xs:string		 				:= "Slug" ;
declare variable $CONSTANT:HEADER-ETAG as xs:string		 				:= "ETag" ;
declare variable $CONSTANT:HEADER-IF-MODIFIED-SINCE as xs:string		:= "If-Modified-Since" ;

declare variable $CONSTANT:MEDIA-TYPE-MULTIPART-FORM-DATA as xs:string	:= "multipart/form-data" ;
declare variable $CONSTANT:MEDIA-TYPE-XML as xs:string					:= "application/xml" ;
declare variable $CONSTANT:MEDIA-TYPE-ATOM as xs:string					:= "application/atom+xml" ;
declare variable $CONSTANT:MEDIA-TYPE-TEXT as xs:string					:= "text/plain" ;

declare variable $CONSTANT:ATOM-NSURI 						:= "http://www.w3.org/2005/Atom" ;
declare variable $CONSTANT:ATOM-FEED as xs:string 			:= "feed" ;
declare variable $CONSTANT:ATOM-ENTRY as xs:string 			:= "entry" ;
declare variable $CONSTANT:ATOM-ID as xs:string 			:= "id" ;
declare variable $CONSTANT:ATOM-PUBLISHED as xs:string 		:= "published" ;
declare variable $CONSTANT:ATOM-UPDATED as xs:string 		:= "updated" ;
declare variable $CONSTANT:ATOM-LINK as xs:string 			:= "link" ;
declare variable $CONSTANT:ATOM-REL as xs:string 			:= "rel" ;
declare variable $CONSTANT:ATOM-HREF as xs:string 			:= "href" ;
declare variable $CONSTANT:ATOM-EDIT as xs:string 			:= "edit" ;
declare variable $CONSTANT:ATOM-EDIT-MEDIA as xs:string 	:= "edit-media" ;
declare variable $CONSTANT:ATOM-CONTENT as xs:string        := "content" ;
declare variable $CONSTANT:ATOM-AUTHOR as xs:string         := "author" ;

declare variable $CONSTANT:OP-CREATE-COLLECTION as xs:string     := "CREATE_COLLECTION" ;
declare variable $CONSTANT:OP-UPDATE-COLLECTION as xs:string     := "UPDATE_COLLECTION" ;
declare variable $CONSTANT:OP-LIST-COLLECTION as xs:string       := "LIST_COLLECTION" ;
declare variable $CONSTANT:OP-CREATE-MEMBER as xs:string         := "CREATE_MEMBER" ;
declare variable $CONSTANT:OP-RETRIEVE-MEMBER as xs:string       := "RETRIEVE_MEMBER" ;
declare variable $CONSTANT:OP-UPDATE-MEMBER as xs:string         := "UPDATE_MEMBER" ;
declare variable $CONSTANT:OP-DELETE-MEMBER as xs:string         := "DELETE_MEMBER" ;
declare variable $CONSTANT:OP-CREATE-MEDIA as xs:string          := "CREATE_MEDIA" ;
declare variable $CONSTANT:OP-RETRIEVE-MEDIA as xs:string        := "RETRIEVE_MEDIA" ;
declare variable $CONSTANT:OP-UPDATE-MEDIA as xs:string          := "UPDATE_MEDIA" ;
declare variable $CONSTANT:OP-DELETE-MEDIA as xs:string          := "DELETE_MEDIA" ;
declare variable $CONSTANT:OP-RETRIEVE-ACL as xs:string          := "RETRIEVE_ACL" ;
declare variable $CONSTANT:OP-UPDATE-ACL as xs:string            := "UPDATE_ACL" ;
declare variable $CONSTANT:OP-MULTI-CREATE as xs:string          := "MULTI_CREATE" ;
declare variable $CONSTANT:OP-RETRIEVE-HISTORY as xs:string      := "RETRIEVE_HISTORY" ;
declare variable $CONSTANT:OP-RETRIEVE-REVISION as xs:string     := "RETRIEVE_REVISION" ;
