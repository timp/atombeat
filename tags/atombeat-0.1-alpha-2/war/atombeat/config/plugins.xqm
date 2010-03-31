xquery version "1.0";

module namespace plugin = "http://www.cggh.org/2010/atombeat/xquery/plugin";


import module namespace logger-plugin = "http://www.cggh.org/2010/atombeat/xquery/logger-plugin" at "../plugins/logger-plugin.xqm" ;
import module namespace security-plugin = "http://www.cggh.org/2010/atombeat/xquery/security-plugin" at "../plugins/security-plugin.xqm" ;
import module namespace history-plugin = "http://www.cggh.org/2010/atombeat/xquery/history-plugin" at "../plugins/history-plugin.xqm" ;



declare variable $plugin:before as function* := (
	util:function( QName( "http://www.cggh.org/2010/atombeat/xquery/logger-plugin" , "logger-plugin:before" ) , 4 ) ,
	util:function( QName( "http://www.cggh.org/2010/atombeat/xquery/security-plugin" , "security-plugin:before" ) , 4 ) ,
	util:function( QName( "http://www.cggh.org/2010/atombeat/xquery/history-plugin" , "history-plugin:before" ) , 4 ) 
);


declare variable $plugin:after as function* := (
	util:function( QName( "http://www.cggh.org/2010/atombeat/xquery/history-plugin" , "history-plugin:after" ) , 4 ) ,
	util:function( QName( "http://www.cggh.org/2010/atombeat/xquery/security-plugin" , "security-plugin:after" ) , 4 ) ,
	util:function( QName( "http://www.cggh.org/2010/atombeat/xquery/logger-plugin" , "logger-plugin:after" ) , 4 )
);
