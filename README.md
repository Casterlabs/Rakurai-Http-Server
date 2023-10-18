# Rakurai-Http-Server

Our very own Http server.

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
            <groupId>co.casterlabs</groupId>
            <artifactId>Rakurai-Http-Server</artifactId>
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
        implementation 'co.casterlabs:Rakurai-Http-Server:VERSION'
    }
```

## Example Code

```java
HttpServer server = new HttpServerBuilder()
    .setPort(8080)
    .build(new HttpListener() {
        @Override
        public @Nullable HttpResponse serveHttpSession(@NonNull HttpSession session) {
            String body = String.format("Hello %s!", session.getRemoteIpAddress());
            return HttpResponse
                .newFixedLengthResponse(StandardHttpStatus.OK, body)
                .setMimeType("text/plain");
        }
        @Override
        public @Nullable WebsocketListener serveWebsocketSession(@NonNull WebsocketSession session) {
            // Returning null will drop the connection.
            return null;
        }
    });
server.start(); // Open up http://127.0.0.1:8080
```
