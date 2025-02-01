package co.casterlabs.rhs.protocol.api.endpoints;

/**
 * <pre>
 * &#64;HttpEndpoint(uri = "/hello")
 * public HttpResponse onHelloRequest(HttpSession session, EndpointData&lt;Void&gt; data) {
 *     // Do what you want, keeping in mind that returning null
 *     // or setting the status to NO_RESPONSE will cause the
 *     // connection will be dropped without a response.
 *     return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, "Hello world!");
 * }
 * </pre>
 * 
 * <pre>
 * &#64;WebsocketEndpoint(uri = "/echo")
 * public WebsocketListener onEchoRequest(WebsocketSession session, EndpointData&lt;Void&gt; data) {
 *     // Do what you want, keeping in mind that returning null
 *     // will cause the connection will be dropped without a response.
 *     return new WebsocketListener() {
 *         &#64;Override
 *         public void onText(Websocket websocket, String message) throws IOException {
 *             websocket.send(message);
 *         }
 * 
 *         &#64;Override
 *         public void onBinary(Websocket websocket, byte[] bytes) throws IOException {
 *             websocket.send(bytes);
 *         }
 *
 *     };
 * }
 * </pre>
 */
public interface EndpointProvider {
}
