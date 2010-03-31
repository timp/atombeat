xquery version "1.0";

module namespace CONSTANT = "http://www.cggh.org/2010/atombeat/xquery/constants";

declare variable $CONSTANT:METHOD-GET as xs:string 						:= "GET" ;
declare variable $CONSTANT:METHOD-POST as xs:string 					:= "POST" ;
declare variable $CONSTANT:METHOD-PUT as xs:string 						:= "PUT" ;
declare variable $CONSTANT:METHOD-DELETE as xs:string 					:= "DELETE" ;

declare variable $CONSTANT:STATUS-SUCCESS-OK as xs:integer 						:= 200 ;
declare variable $CONSTANT:STATUS-SUCCESS-CREATED as xs:integer 				:= 201 ;
declare variable $CONSTANT:STATUS-SUCCESS-NO-CONTENT as xs:integer 				:= 204 ;

declare variable $CONSTANT:STATUS-CLIENT-ERROR-BAD-REQUEST as xs:integer 		:= 400 ;
declare variable $CONSTANT:STATUS-CLIENT-ERROR-FORBIDDEN as xs:integer 			:= 403 ;
declare variable $CONSTANT:STATUS-CLIENT-ERROR-NOT-FOUND as xs:integer 			:= 404 ;
declare variable $CONSTANT:STATUS-CLIENT-ERROR-METHOD-NOT-ALLOWED as xs:integer := 405 ;

declare variable $CONSTANT:STATUS-SERVER-ERROR-INTERNAL-SERVER-ERROR as xs:integer     := 500 ;
declare variable $CONSTANT:STATUS-SERVER-ERROR-NOT-IMPLEMENTED as xs:integer           := 501 ;

declare variable $CONSTANT:HEADER-ALLOW as xs:string 					:= "Allow" ;
declare variable $CONSTANT:HEADER-ACCEPT as xs:string 					:= "Accept" ;
declare variable $CONSTANT:HEADER-CONTENT-TYPE as xs:string 			:= "Content-Type" ;
declare variable $CONSTANT:HEADER-CONTENT-DISPOSITION as xs:string 		:= "Content-Disposition" ;
declare variable $CONSTANT:HEADER-LOCATION as xs:string 				:= "Location" ;
declare variable $CONSTANT:HEADER-SLUG as xs:string		 				:= "Slug" ;
declare variable $CONSTANT:HEADER-IF-MODIFIED-SINCE as xs:string		:= "If-Modified-Since" ;

declare variable $CONSTANT:MEDIA-TYPE-MULTIPART-FORM-DATA as xs:string	:= "multipart/form-data" ;
declare variable $CONSTANT:MEDIA-TYPE-XML as xs:string					:= "application/xml" ;
declare variable $CONSTANT:MEDIA-TYPE-ATOM as xs:string					:= "application/atom+xml" ;

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

declare variable $CONSTANT:OP-CREATE-COLLECTION as xs:string     := "create-collection" ;
declare variable $CONSTANT:OP-UPDATE-COLLECTION as xs:string     := "update-collection" ;
declare variable $CONSTANT:OP-LIST-COLLECTION as xs:string       := "list-collection" ;
declare variable $CONSTANT:OP-CREATE-MEMBER as xs:string         := "create-member" ;
declare variable $CONSTANT:OP-RETRIEVE-MEMBER as xs:string       := "retrieve-member" ;
declare variable $CONSTANT:OP-UPDATE-MEMBER as xs:string         := "update-member" ;
declare variable $CONSTANT:OP-DELETE-MEMBER as xs:string         := "delete-member" ;
declare variable $CONSTANT:OP-CREATE-MEDIA as xs:string          := "create-media" ;
declare variable $CONSTANT:OP-RETRIEVE-MEDIA as xs:string        := "retrieve-media" ;
declare variable $CONSTANT:OP-UPDATE-MEDIA as xs:string          := "update-media" ;
declare variable $CONSTANT:OP-DELETE-MEDIA as xs:string          := "delete-media" ;
declare variable $CONSTANT:OP-UPDATE-ACL as xs:string            := "update-acl" ;
