# Rakurai-Http-Server

Our very own Http server.

## Adding to your project

### Maven

```xml
    <repositories>
        <repository>
            <id>casterlabs-maven</id>
            <url>https://repo.casterlabs.co/maven</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>co.casterlabs.rakurai-http-server</groupId>
            <artifactId>core</artifactId>
            <version>VERSION</version>
            <scope>compile</scope>
        </dependency>
        <dependency>
            <groupId>co.casterlabs.rakurai-http-server.proto</groupId>
            <artifactId>http</artifactId>
            <version>VERSION</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>
```

## Example Code

```java
HttpServer server = new HttpServerBuilder()
    .withPort(8080)
    .with(
        new HttpProtocol(), (session) -> {
            String str = String.format("Hello %s!", session.remoteNetworkAddress());
            if (session.uri().path.startsWith("/chunked")) {
                return HttpResponse.newChunkedResponse(StandardHttpStatus.OK, new ByteArrayInputStream(str.getBytes()));
            } else {
                return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, str);
            }
        }
    )
    .build();

server.start(); // Open up http://127.0.0.1:8080
```
