xquery version "1.0";

module namespace html-plugin = "http://purl.org/atombeat/xquery/html-plugin";
declare namespace atom = "http://www.w3.org/2005/Atom" ;
declare namespace atombeat = "http://purl.org/atombeat/xmlns" ;

import module namespace util = "http://exist-db.org/xquery/util" ;
import module namespace atomdb = "http://purl.org/atombeat/xquery/atomdb" at "../lib/atomdb.xqm" ;
import module namespace CONSTANT = "http://purl.org/atombeat/xquery/constants" at "../lib/constants.xqm" ;

(: A post-processing atombeat plugin which adds an HTML rendering of an Atom entry and 
   adds 'alternate' links to it from an Atom feed :)    



(: Strip out any 'alternate' links which we have inserted :)
(: Not needed yet, as we do not currently augment the entry, only the feed :)
(:
declare function html-plugin:before(
  $operation as xs:string ,
  $request-path-info as xs:string ,
  $request-data as item()* ,
  $request-media-type as xs:string?
) as item()*
{
  if ( $operation = $CONSTANT:OP-CREATE-MEMBER or $operation = $CONSTANT:OP-UPDATE-MEMBER )
  then 
    html-plugin:before-modify-member( $request-path-info , $request-data )
  else
    $request-data

};


declare function html-plugin:before-modify-member( 
  $request-path-info as xs:string ,
  $request-data as element(atom:entry) 
) as element(atom:entry) 
{ 
  <atom:entry>
  {
    $request-data/attribute::* ,
    for $child in $request-data/child::* 
          return
              if ( $child instance of element(atom:link) )
              then 
                if ($child/@rel = "alternate" and $child/@type = $CONSTANT:MEDIA-TYPE-HTML )
                then () 
                else $child
              else $child
    
  }
  </atom:entry>
};
:)

declare function html-plugin:after(
  $operation as xs:string ,
  $request-path-info as xs:string ,
  $response as element(response)
) as element(response)
{
  
  let $transform := request:get-parameter("transform", "none")
  let $body := $response/body 
    
  let $augmented-body :=
      if ( $body/atom:feed )
      then 
        <body>{html-plugin:add-alternate-links( $body/atom:feed )}</body>
      else 
        if ( $body/atom:entry )
        then 
          if ($transform = "html") then 
            <body>{html-plugin:transform-entry( $body/atom:entry )}</body>
          else 
            $body
        else $body
        
        
        
  let $transformed-headers := 
      if ( $body/atom:entry and $transform = "html")
      then html-plugin:set-html-contentType( $response/headers )
      else $response/headers
    
  return
        <response>
        {
            $response/status ,
            $transformed-headers ,
            $augmented-body
        }
        </response>
            
}; 

declare function html-plugin:set-html-contentType(
    $headers as element(headers)    
) as element(headers)
{
  <headers>
  {
    $headers/header[not(name/text()=$CONSTANT:HEADER-CONTENT-TYPE)] ,
    <header>
      <name>{$CONSTANT:HEADER-CONTENT-TYPE}</name>
      <value>{$CONSTANT:MEDIA-TYPE-HTML}</value>
    </header>
  }
  </headers>
}; 

declare function html-plugin:add-alternate-links(
    $feed as element(atom:feed)
) as element(atom:feed)
{
    <atom:feed>
    { 
        $feed/attribute::* ,
        for $child in $feed/child::* 
        return
            if ( $child instance of element(atom:entry) )
            then html-plugin:add-alternate-link( $child )
            else $child
    }
    </atom:feed>
};

declare function html-plugin:add-alternate-link(
    $entry as element(atom:entry)
) as element(atom:entry)
{
  let $href := $entry/atom:link[@rel="edit"]/@href
  let $tag:= <atom:link rel="alternate" type="{$CONSTANT:MEDIA-TYPE-HTML}" href="{$href}?transform=html" />
     
  return 
    <atom:entry>
    { 
        $entry/attribute::* ,
        $entry/child::*, 
        $tag
    }
    </atom:entry>
};

declare function html-plugin:transform-entry(
    $entry as element(atom:entry)
) as element(html)
{
 let $title := $entry/atom:title/text()
 return
 <html>
  <head>
    <title>{$title}</title>
    <style type="text/css">
     .atom {{
       color: green;
       font-family: Arial;
     }}
     .atom_title {{
       color: green; 
     }}
     .atom_id {{
       color: blue; 
     }}
     .atom_published {{
       color: Indigo;
     }}
     .atom_updated {{
       color: Indigo;
     }}
     .atom_author {{
       color: Indigo;
     }}
     .atom_link {{
       color: purple;
     }}

     .atom_summary {{
       color: purple;
     }}
     .atom_content {{ 
       border: 1px solid black;
       min-height:200px;
       width:600px;
       color: purple;
     }}
     
    </style>
  </head>
  <body>
   <div class="atom">
    <h1 class='atom_title'>{$title}</h1>
 {
      for $child in $entry/child::* 
      return
          if ( $child instance of element(atom:title) )
          then () (: Dealt with already :)
          else if ( $child instance of element(atom:id) )
          then ()
          else if ( $child instance of element(atom:published) )
          then <p class='atom_published'>Published: {$child/text()}</p>
          else if ( $child instance of element(atom:updated) )
          then <p class='atom_updated'>Updated: {$child/text()}</p>
          else if ( $child instance of element(atom:author) )
          then <p class='atom_author'>Author: {$child/atom:name/text()}</p>
          else if ( $child instance of element(atom:summary) )
          then <h3 class='atom_summary'>{$child/text()}</h3>
          else if ( $child instance of element(atom:content) )
          then <div class='atom_content'>{$child/child::*}</div>
          else if ( $child instance of element(atom:link) )
          then
           
            let $href := $child[@rel="edit"]/@href
            return 
            if ($href)
            then 
              <a href='{$href}' class='atom_id'>Raw</a>
            else 
              ()
          else $child
 }
   </div>
  </body>
 </html>
};






