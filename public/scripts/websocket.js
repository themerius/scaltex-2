define('websocket', function() {

  // class Websocket
  var WebSocket = function (wsURI, aceSession) {

    var WebSocketConstructor = window.WebSocket;
    if (!window.WebSocket)
      WebSocketConstructor = window.MozWebSocket;

    this.sock = new WebSocketConstructor(wsURI);
    this.aceSession = aceSession;
    this.ready = false;

    self = this;  // for correct closure
    this.sock.onopen = function(evt) { self.onopen(evt) };
    this.sock.onclose = function(evt) { self.onclose(evt) };
    this.sock.onmessage = function(evt) { self.onmessage(evt) };
    this.sock.onerror = function(evt) { self.onerror(evt) };

  }

  WebSocket.prototype.onopen = function(event) {
    console.log("WebSocket opened.");
    window.status = "WebSocket opened.";
    this.ready = true;
  }

  WebSocket.prototype.onclose = function(event) {
    console.log("WebSocket closed.");
    window.status = "WebSocket closed.";
    this.ready = false;
  }

  WebSocket.prototype.onmessage = function(event) {
    var json = JSON.parse(event.data);

    if (json.from == 2)  // using ace editor
      this.aceSession.setValue(json.contentUnresolved);

    document.getElementById("entity"+json.from).innerHTML = event.data;
  }

  WebSocket.prototype.onerror = function (event) {
    console.log("WebSocket got error: " + event.data);
  }

  WebSocket.prototype.send = function (msg) {
    try {
      this.sock.send(msg);  // JSON.stringify(msg)
      return msg;
    } catch(e) {
      console.log(e);
    }
  }

  return WebSocket;

});