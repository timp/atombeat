import module namespace foo = "http://example.org/foo" at "foo.xqm" ;

if (request:get-method() = "GET")
then
<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title></title></head>
    <body>
        <ul>
            <li>
                <form action="" method="post" target="f">
                    <input type="hidden" name="action" value="retrieve"/>
                    <input type="submit" value="retrieve"/>
                </form>
            </li>
            <li>
                <form action="" method="post" target="f">
                    <input type="hidden" name="action" value="update-1"/>
                    <input type="submit" value="update-1"/>
                </form>
            </li>
            <li>
                <form action="" method="post" target="f">
                    <input type="hidden" name="action" value="update-2"/>
                    <input type="submit" value="update-2"/>
                </form>
            </li>
        </ul>
        <iframe id="f" name="f" width="600" height="600"></iframe>
    </body>
</html>
else if (request:get-method() = "POST" and request:get-parameter("action", "") = "retrieve")
then foo:retrieve()
else if (request:get-method() = "POST" and request:get-parameter("action", "") = "update-1")
then foo:update-1()
else if (request:get-method() = "POST" and request:get-parameter("action", "") = "update-2")
then foo:update-2()
else () 
