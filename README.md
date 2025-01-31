# IRIXClient

IRIXClient accepts reports as JSON, builds corresponding IRIX XML reports and
send them to IRIXWebservice. Furthermore, it it fetches and includes external
images and gets maps from mapfish-print.

further information:

 - [Changelog](Changelog.md)
 - [License](LICENSE)

## Building

To install the IRIXclient simply build it with:

```bash
mvn clean package
```

and install the packaged webapplication from:

```bash
target/irix-webservice.war
```

in the Servlet Container of your choice.

## VSCode and wsimport

If classes generated by wsimport (`de.intevation.irixservice.Upload...`)
are reported as missing, please run `mvn package` via the terminal.

## Testing

The current tests do not require any server, so you can simply
start tests with:

```bash
mvn test
```

## Deployment

Configure the print-url of a mapfish-print service in the web.xml
of the application.

You can find an example request under examples/irix-test.json that
should illustrate the structure of the request.

This request should be posted to the IRIXClient servlet.

E.g.:

```bash
    curl -H "Content-Type: application/json; charset=utf-8" -X POST --data \
        @examples/irix-test.json http://localhost:8080/irix-client/IRIXClient
```
or only for images (png, jpg, svg, pdf) see examples/img-test.json

```bash
curl -H "Content-Type: application/json; charset=utf-8" -X POST --data \
        @examples/img-test.json http://localhost:8080/irix-client/IRIXClient
```

The WSDL URL of the irix-webservice needs to be configured in the pom.xml

### web.xml Configuration params

- `<param-name>user-header</param-name>`
  If this param is configured, irix-client uses the Header set in param-value as the users id. This value overrides DokpoolDocumentOwner in DokpoolMeta while processing IRIX.!
- `<param-name>roles-header</param-name>`
  If this param is configured, irix-client uses the Header set in param-value as the users roles. the roles are used to check for permissions.
- `<param-name>user-displayname-header</param-name>`
  If this param is configured, irix-client uses the Header set in param-value as the users displayname. This value overrides User in irix to set PersonContactInfo in while processing IRIX.!
- `<param-name>roles-permission</param-name>`
  If this param is configured, irix-client uses the roles set in param-value to check if the user is allowed to use this service by comparing it with the roles in roles-header. This configuration only makes sense, if roles-header is set as well.

## Examples

Currently not all examples correspond to the current IRIX schema and/or Dokpool 2.0 and may produce errors.
This will be fixed in later versions.

