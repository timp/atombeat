xquery version "1.0";

module namespace plugin = "http://purl.org/atombeat/xquery/plugin";


import module namespace logger-plugin = "http://purl.org/atombeat/xquery/logger-plugin" at "../plugins/logger-plugin.xqm" ;
import module namespace security-plugin = "http://purl.org/atombeat/xquery/security-plugin" at "../plugins/security-plugin.xqm" ;
import module namespace history-plugin = "http://purl.org/atombeat/xquery/history-plugin" at "../plugins/history-plugin.xqm" ;



declare function plugin:before() as function* {
	(
		util:function( QName( "http://purl.org/atombeat/xquery/logger-plugin" , "logger-plugin:before" ) , 4 ) ,
		util:function( QName( "http://purl.org/atombeat/xquery/security-plugin" , "security-plugin:before" ) , 4 ) , 
		util:function( QName( "http://purl.org/atombeat/xquery/history-plugin" , "history-plugin:before" ) , 4 )   
	)
};




declare function plugin:after() as function* {
	(
		util:function( QName( "http://purl.org/atombeat/xquery/history-plugin" , "history-plugin:after" ) , 3 ) ,
		util:function( QName( "http://purl.org/atombeat/xquery/security-plugin" , "security-plugin:after" ) , 3 ) , 
		util:function( QName( "http://purl.org/atombeat/xquery/logger-plugin" , "logger-plugin:after" ) , 3 )
	)
};
