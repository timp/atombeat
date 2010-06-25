xquery version "1.0";

module namespace config-collections = "http://purl.org/atombeat/xquery/config-collections";

declare variable $config-collections:collection-spec := 
    <spec>
        <collection>
            <title>Foo Collection</title>
            <path-info>/foo</path-info>
            <enable-history>true</enable-history>
            <exclude-entry-content>true</exclude-entry-content>
            <expand-security-descriptors>true</expand-security-descriptors>
            <recursive>false</recursive>
        </collection>   
    </spec>
;
