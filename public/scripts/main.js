require.config({
  paths: {
    'ace': 'bower_components/ace/lib/ace',
  }
});

require(['websocket', 'config', 'ace/ace'], function(WebSocket, config, ace) {
  var editor;
  var session;

  var editor = ace.edit("editor");
  editor.setTheme("ace/theme/monokai");
  var session = editor.getSession();
  session.setMode("ace/mode/text");
  session.setValue("hello world");

  var socket = new WebSocket(config.webSocketAbsUrl, session);

  var updateEntity = function () {
    socket.send(session.getValue());
  }

  var el = document.getElementById("click");
  el.addEventListener("click", updateEntity, false);
});
