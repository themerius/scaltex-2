define("websocket", function() {

  // class Websocket
  var WebSocket = function (wsURI, handler) {

    var WebSocketConstructor = window.WebSocket;
    if (!window.WebSocket)
      WebSocketConstructor = window.MozWebSocket;

    this.sock = new WebSocketConstructor(wsURI);
    this.handler = handler;
    this.ready = false;

    this.msgCount = 0;

    var self = this;  // for correct closure
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
    var jsonMsg = JSON.parse(event.data);
    this.handler.handle(jsonMsg, this);

    this.msgCount++;
    window.status = "WebSocket handled " + this.msgCount + " messages.";
  }

  WebSocket.prototype.onerror = function (event) {
    console.log("WebSocket got error: " + event.data);
  }

  WebSocket.prototype.send = function (msg) {
    try {
      this.sock.send(msg);
      return msg;
    } catch(e) {
      console.log(e);
    }

    this.msgCount++;
    window.status = "WebSocket handled " + this.msgCount + " messages.";
  }

  WebSocket.prototype.sendJson = function (msg) {
    try {
      this.sock.send(JSON.stringify(msg));
      return msg;
    } catch(e) {
      console.log(e);
    }
  }

  return WebSocket;

});