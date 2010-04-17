xquery version "1.0";

module namespace plugin = "http://atombeat.org/xquery/plugin";


import module namespace logger-plugin = "http://atombeat.org/xquery/logger-plugin" at "../plugins/logger-plugin.xqm" ;
import module namespace security-plugin = "http://atombeat.org/xquery/security-plugin" at "../plugins/security-plugin.xqm" ;
import module namespace history-plugin = "http://atombeat.org/xquery/history-plugin" at "../plugins/history-plugin.xqm" ;



declare variable $plugin:before as function* := (
	util:function( QName( "http://atombeat.org/xquery/logger-plugin" , "logger-plugin:before" ) , 4 ) ,
	util:function( QName( "http://atombeat.org/xquery/security-plugin" , "security-plugin:before" ) , 4 ) ,
	util:function( QName( "http://atombeat.org/xquery/history-plugin" , "history-plugin:before" ) , 4 ) 
);


declare variable $plugin:after as function* := (
	util:function( QName( "http://atombeat.org/xquery/history-plugin" , "history-plugin:after" ) , 4 ) ,
	util:function( QName( "http://atombeat.org/xquery/security-plugin" , "security-plugin:after" ) , 4 ) ,
	util:function( QName( "http://atombeat.org/xquery/logger-plugin" , "logger-plugin:after" ) , 4 )
);
