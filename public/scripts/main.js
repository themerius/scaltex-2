var socket;
var editor;
var session;

if (!window.WebSocket) {
  window.WebSocket = window.MozWebSocket;
}

if (window.WebSocket) {
  socket = new WebSocket("ws://localhost:8000/echo");
  socket.onmessage = function(event) {
    console.log(event.data);
    var json = JSON.parse(event.data);
    if (json.from == 2)
      document.getElementById("inputbx").value = json.contentUnresolved;  // here should the content with variables...
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

var updateEntity = function (form) {
  //socket.send(form.inputbox.value);
  socket.send(session.getValue());
}
