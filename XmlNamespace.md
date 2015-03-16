

# @atombeat:allow #

This extension attribute MAY be placed on `atom:link` elements. The syntax of the attribute value MUST conform the syntax for the `Allow` HTTP header.

The attribute indicates the set of HTTP methods which may be used in requests to the link URL at the time the representation was generated.

Typically useful when the security system is enabled, to support clients adapting a UI to users with different permissions.

# TODO #