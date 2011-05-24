xquery version "1.0";

module namespace plugin = "http://purl.org/atombeat/xquery/plugin";


import module namespace logger-plugin = "http://purl.org/atombeat/xquery/logger-plugin" at "../plugins/logger-plugin.xqm" ;
import module namespace conneg-plugin = "http://purl.org/atombeat/xquery/conneg-plugin" at "../plugins/conneg-plugin.xqm" ;
import module namespace security-plugin = "http://purl.org/atombeat/xquery/security-plugin" at "../plugins/security-plugin.xqm" ;
import module namespace link-extensions-plugin = "http://purl.org/atombeat/xquery/link-extensions-plugin" at "../plugins/link-extensions-plugin.xqm" ;
import module namespace link-expansion-plugin = "http://purl.org/atombeat/xquery/link-expansion-plugin" at "../plugins/link-expansion-plugin.xqm" ;
import module namespace tombstones-plugin = "http://purl.org/atombeat/xquery/tombstones-plugin" at "../plugins/tombstones-plugin.xqm" ;
import module namespace history-plugin = "http://purl.org/atombeat/xquery/history-plugin" at "../plugins/history-plugin.xqm" ;
import module namespace unzip-plugin = "http://purl.org/atombeat/xquery/unzip-plugin" at "../plugins/unzip-plugin.xqm" ;
import module namespace paging-plugin = "http://purl.org/atombeat/xquery/paging-plugin" at "../plugins/paging-plugin.xqm" ;




declare function plugin:before() as function* {
	(
		util:function( QName( "http://purl.org/atombeat/xquery/logger-plugin" , "logger-plugin:before" ) , 3 ) ,
		util:function( QName( "http://purl.org/atombeat/xquery/security-plugin" , "security-plugin:before" ) , 3 ) , 
		util:function( QName( "http://purl.org/atombeat/xquery/conneg-plugin" , "conneg-plugin:before" ) , 3 ) , 
        util:function( QName( "http://purl.org/atombeat/xquery/tombstones-plugin" , "tombstones-plugin:before" ) , 3 ) ,  
		util:function( QName( "http://purl.org/atombeat/xquery/link-expansion-plugin" , "link-expansion-plugin:before" ) , 3 ) ,  
        util:function( QName( "http://purl.org/atombeat/xquery/link-extensions-plugin" , "link-extensions-plugin:before" ) , 3 ) ,  
		util:function( QName( "http://purl.org/atombeat/xquery/unzip-plugin" , "unzip-plugin:before" ) , 3 ) , 
		util:function( QName( "http://purl.org/atombeat/xquery/paging-plugin" , "paging-plugin:before" ) , 3 ) , 
		util:function( QName( "http://purl.org/atombeat/xquery/history-plugin" , "history-plugin:before" ) , 3 )   
	)
};




(:~
 : The sequence of plugin functions to execute during the "after" phase of request processing.
 : 
 : Note that listing a collection (i.e., retrieving a feed) is by far the most computationally
 : expensive protocol operation, and the order of plugin functions in the "after" phase can make
 : a big difference. Also, some plugin functions need to be placed in a particular order to 
 : make any sense. Hopefully the comments below are self-explanatory.
 :)
declare function plugin:after() as function* {
	(
		util:function( QName( "http://purl.org/atombeat/xquery/security-plugin" , "security-plugin:after" ) , 3 ) , 
		(: any plugins that might filter entries from a feed MUST come prior to the paging plugin :)
		util:function( QName( "http://purl.org/atombeat/xquery/paging-plugin" , "paging-plugin:after" ) , 3 ) , 
		(: the paging plugin should come as early as possible, to limit work done by downstream plugins when processing a feed :)
        util:function( QName( "http://purl.org/atombeat/xquery/tombstones-plugin" , "tombstones-plugin:after" ) , 3 ) ,
        util:function( QName( "http://purl.org/atombeat/xquery/history-plugin" , "history-plugin:after" ) , 3 ) ,
        util:function( QName( "http://purl.org/atombeat/xquery/unzip-plugin" , "unzip-plugin:after" ) , 3 ) ,
		util:function( QName( "http://purl.org/atombeat/xquery/link-extensions-plugin" , "link-extensions-plugin:after" ) , 3 ) , 
		util:function( QName( "http://purl.org/atombeat/xquery/link-expansion-plugin" , "link-expansion-plugin:after" ) , 3 ) , 
		(: the conneg plugin MUST come after all other plugins that might modify the response :)
		util:function( QName( "http://purl.org/atombeat/xquery/conneg-plugin" , "conneg-plugin:after" ) , 3 ) ,
		(: the logger plugin has no effect on the response :)
		util:function( QName( "http://purl.org/atombeat/xquery/logger-plugin" , "logger-plugin:after" ) , 3 )
	)
};



declare function plugin:after-error() as function* {
    (
        util:function( QName( "http://purl.org/atombeat/xquery/tombstones-plugin" , "tombstones-plugin:after-error" ) , 3 ) 
    )
};
