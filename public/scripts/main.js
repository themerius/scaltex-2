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

  editor.on("change", function () {
    document.getElementById("editor").style.height = 16 * session.getLength() + "px";
    editor.resize();
  });

  // Make Editors
  var editors = document.getElementById("editors");

  var editorContentElem = document.createElement("div");
  editorContentElem.id = "editorContent-1";
  editorContentElem.className += "grid_7";
  editorContentElem.style.height = "16px";

  var editorClassDefElem = document.createElement("div");
  editorClassDefElem.id = "editorClassDef-1";
  editorClassDefElem.className += "grid_3";
  editorClassDefElem.style.height = "16px";

  var updateButtonElem = document.createElement("input");
  updateButtonElem.id = "updateEntity-1";
  updateButtonElem.type = "button";
  updateButtonElem.value = "Update";
  updateButtonElem.addEventListener("click", function() {
    socket.sendJson({
      "function": "updateEntity",
      "params": {id: 1, content: sessionContent.getValue()}
    });
  }, false);

  var plusButtonElem = document.createElement("input");
  plusButtonElem.id = "addEntity-1";
  plusButtonElem.type = "button";
  plusButtonElem.value = "+";
  plusButtonElem.addEventListener("click", function() {
    socket.sendJson({
      "function": "createEntityAndAppend",
      "params": {cls: "Section", content: "xy"}
    });
  }, false);

  var clearElem = document.createElement("div");
  clearElem.className = "clear";

  editors.appendChild(editorContentElem);
  editors.appendChild(editorClassDefElem);
  editors.appendChild(updateButtonElem);
  editors.appendChild(plusButtonElem);
  editors.appendChild(clearElem);

  var editorContent = ace.edit("editorContent-1");
  editorContent.setTheme("ace/theme/monokai");
  var sessionContent = editorContent.getSession();
  sessionContent.setMode("ace/mode/text");
  sessionContent.setValue("n/a");

  var editorClassDef = ace.edit("editorClassDef-1");
  editorClassDef.setTheme("ace/theme/solarized_light");
  var sessionClassDef = editorClassDef.getSession();
  sessionClassDef.setMode("ace/mode/text");
  sessionClassDef.setValue("n/a");

  // END Make Editors

  var socket = new WebSocket(config.webSocketAbsUrl, session);

  var updateEntity = function () {
    socket.send(session.getValue());
  }

  var el = document.getElementById("click");
  el.addEventListener("click", updateEntity, false);

});
