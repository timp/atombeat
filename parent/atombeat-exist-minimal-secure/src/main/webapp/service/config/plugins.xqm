xquery version "1.0";

module namespace plugin = "http://purl.org/atombeat/xquery/plugin";


import module namespace logger-plugin = "http://purl.org/atombeat/xquery/logger-plugin" at "../plugins/logger-plugin.xqm" ;
import module namespace conneg-plugin = "http://purl.org/atombeat/xquery/conneg-plugin" at "../plugins/conneg-plugin.xqm" ;
import module namespace tagger-plugin = "http://purl.org/atombeat/xquery/tagger-plugin" at "../plugins/tagger-plugin.xqm" ;
import module namespace security-plugin = "http://purl.org/atombeat/xquery/security-plugin" at "../plugins/security-plugin.xqm" ;
import module namespace link-extensions-plugin = "http://purl.org/atombeat/xquery/link-extensions-plugin" at "../plugins/link-extensions-plugin.xqm" ;
import module namespace link-expansion-plugin = "http://purl.org/atombeat/xquery/link-expansion-plugin" at "../plugins/link-expansion-plugin.xqm" ;
import module namespace tombstones-plugin = "http://purl.org/atombeat/xquery/tombstones-plugin" at "../plugins/tombstones-plugin.xqm" ;
import module namespace history-plugin = "http://purl.org/atombeat/xquery/history-plugin" at "../plugins/history-plugin.xqm" ;




declare function plugin:before() as function* {
	(
		util:function( QName( "http://purl.org/atombeat/xquery/logger-plugin" , "logger-plugin:before" ) , 3 ) ,
		util:function( QName( "http://purl.org/atombeat/xquery/security-plugin" , "security-plugin:before" ) , 3 ) , 
		util:function( QName( "http://purl.org/atombeat/xquery/conneg-plugin" , "conneg-plugin:before" ) , 3 ) , 
		util:function( QName( "http://purl.org/atombeat/xquery/tagger-plugin" , "tagger-plugin:before" ) , 3 ) , 
        util:function( QName( "http://purl.org/atombeat/xquery/tombstones-plugin" , "tombstones-plugin:before" ) , 3 ) ,  
		util:function( QName( "http://purl.org/atombeat/xquery/link-expansion-plugin" , "link-expansion-plugin:before" ) , 3 ) ,  
        util:function( QName( "http://purl.org/atombeat/xquery/link-extensions-plugin" , "link-extensions-plugin:before" ) , 3 ) ,  
		util:function( QName( "http://purl.org/atombeat/xquery/history-plugin" , "history-plugin:before" ) , 3 )   
	)
};




declare function plugin:after() as function* {
	(
		util:function( QName( "http://purl.org/atombeat/xquery/security-plugin" , "security-plugin:after" ) , 3 ) , 
        util:function( QName( "http://purl.org/atombeat/xquery/tombstones-plugin" , "tombstones-plugin:after" ) , 3 ) ,
        util:function( QName( "http://purl.org/atombeat/xquery/history-plugin" , "history-plugin:after" ) , 3 ) ,
		util:function( QName( "http://purl.org/atombeat/xquery/link-extensions-plugin" , "link-extensions-plugin:after" ) , 3 ) , 
		util:function( QName( "http://purl.org/atombeat/xquery/link-expansion-plugin" , "link-expansion-plugin:after" ) , 3 ) , 
		util:function( QName( "http://purl.org/atombeat/xquery/conneg-plugin" , "conneg-plugin:after" ) , 3 ) ,
		util:function( QName( "http://purl.org/atombeat/xquery/logger-plugin" , "logger-plugin:after" ) , 3 )
	)
};



declare function plugin:after-error() as function* {
    (
        util:function( QName( "http://purl.org/atombeat/xquery/tombstones-plugin" , "tombstones-plugin:after-error" ) , 3 ) 
    )
};
