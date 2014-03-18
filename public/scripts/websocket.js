define(['ace/ace', 'config'], function(ace, config) {

  var socket;
  var editor;
  var session;

  var editor = ace.edit("editor");
  editor.setTheme("ace/theme/monokai");
  var session = editor.getSession();
  session.setMode("ace/mode/text");
  session.setValue("hello world");


  if (!window.WebSocket) {
    window.WebSocket = window.MozWebSocket;
  }

  if (window.WebSocket) {
    socket = new WebSocket(config.webSocketAbsUrl);
    socket.onmessage = function(event) {
      console.log(event.data);
      var json = JSON.parse(event.data);
      if (json.from == 2)  // using ace editor
        session.setValue(json.contentUnresolved);
      document.getElementById("entity"+json.from).innerHTML = event.data;
    };
    socket.onopen = function(event) { 
      document.getElementById("response").innerHTML = "Opened"; 
    };
    socket.onclose = function(event) { 
      document.getElementById("response").innerHTML = "Closed"; 
    };
  } else {
    alert("Your browser does not support Web Socket.");
  }

  var updateEntity = function () {
    socket.send(session.getValue());
  }

  var el = document.getElementById("click");
  el.addEventListener("click", updateEntity, false);

});