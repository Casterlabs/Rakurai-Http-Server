# Http-Server 
This serves to wrap existing http server libraries and provide them in an easy-to-consume sdk.

## Adding to your project

### Maven
```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>com.github.casterlabs.rakurai</groupId>
            <artifactId>Http-Server</artifactId>
            <version>VERSION</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>
```

### Gradle
```gradle
    allprojects {
        repositories {
            maven { url 'https://jitpack.io' }
        }
	}

    dependencies {
        implementation 'com.github.casterlabs.rakurai:Http-Server:VERSION'
    }
```

## Example Code
```java
HttpServer server = HttpServerBuilder
    .getUndertowBuilder()
    .setPort(8080)
    .build(new HttpListener() {

    @Override
    public @Nullable HttpResponse serveSession(@NonNull String host, @NonNull HttpSession session, boolean secure) {
        String body = String.format("Hello %s!", session.getRemoteIpAddress());

        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, body);
    }

    @Override
    public @Nullable WebsocketListener serveWebsocketSession(@NonNull String host, @NonNull WebsocketSession session, boolean secure) {
        // Returning null will drop the connection.
        return null;
    }
});

server.start(); // Open up http://127.0.0.1:8080
```