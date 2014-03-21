define('handler', ['ace/ace'], function(ace) {

  var Handler = function () {
    this.lastCreatedEntityElemId = 0;
    this.aceSessions = {};
  }

  Handler.prototype.handle = function (jsonMsg, socket) {
    var entityElem = this.getOrCreateEntityElem(jsonMsg.from, socket);

    // pass things to scaltex.js for actual rendering:
    if (jsonMsg.classDef == "Text")
      entityElem.innerHTML = "<p>" + jsonMsg.text + "</p>";
    else if (jsonMsg.classDef == "Section")
      entityElem.innerHTML = "<h1>" + jsonMsg.nr + " " + jsonMsg.heading + "</h1>";
    else
      entityElem.innerHTML = "<img style=\"max-width: 480px\" src=\"" +
        jsonMsg.url + "\">" + "<p> Abb. " +
        jsonMsg.nr + ": " + jsonMsg.desc + "</p>";

    if (this.aceSessions[jsonMsg.from]) {
      this.aceSessions[jsonMsg.from].content.setValue(jsonMsg.content);
      this.aceSessions[jsonMsg.from].classDef.setValue(jsonMsg.classDef);
    }
  }

  Handler.prototype.getOrCreateEntityElem = function (id, socket) {
    var entityElem = document.getElementById("entity" + id);
    if (!entityElem) {
      var entities = document.getElementById("entities");
      while (this.lastCreatedEntityElemId < id) {
        this.lastCreatedEntityElemId += 1;
        var tmpElem = document.createElement("div");
        tmpElem.id = "entity" + this.lastCreatedEntityElemId;
        tmpElem.innerHTML = "pending ...";
        entities.appendChild(tmpElem);
        entityElem = tmpElem;
        this.createEditors(this.lastCreatedEntityElemId, socket);  // TODO put this direct into handle
      }
    }
    return entityElem;
  }

  Handler.prototype.createEditors = function (id, socket) {
    var editors = document.getElementById("editors");

    var editorContentElem = document.createElement("div");
    editorContentElem.id = "editorContent" + id;
    editorContentElem.className += "grid_7";
    editorContentElem.style.height = "16px";

    var editorClassDefElem = document.createElement("div");
    editorClassDefElem.id = "editorClassDef" + id;
    editorClassDefElem.className += "grid_3";
    editorClassDefElem.style.height = "16px";

    var updateButtonElem = document.createElement("input");
    updateButtonElem.id = "updateEntity" + id;
    updateButtonElem.type = "button";
    updateButtonElem.value = "Update (" + id + ")";
    var self = this
    updateButtonElem.addEventListener("click", function() {
      socket.sendJson({
        "function": "updateEntity",
        "params": {
          "id": id,
          "content": self.aceSessions[id].content.getValue(),
          "cls": self.aceSessions[id].classDef.getValue()
        }
      });
    }, false);

    var plusButtonElem = document.createElement("input");
    plusButtonElem.id = "addEntity" + id;
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

    var brElem = document.createElement("br");

    editors.appendChild(editorContentElem);
    editors.appendChild(editorClassDefElem);
    editors.appendChild(updateButtonElem);
    editors.appendChild(plusButtonElem);
    editors.appendChild(clearElem);
    editors.appendChild(brElem);

    var editorContent = ace.edit("editorContent" + id);
    editorContent.setTheme("ace/theme/monokai");
    var sessionContent = editorContent.getSession();
    sessionContent.setMode("ace/mode/text");
    sessionContent.setUseWrapMode(true);
    sessionContent.setWrapLimitRange(null, null);
    sessionContent.setValue("n/a");

    editorContent.on("change", function () {
      editorContentElem.style.height = 16 * sessionContent.getLength() + "px";
      editorContent.resize();
    });

    var editorClassDef = ace.edit("editorClassDef" + id);
    editorClassDef.setTheme("ace/theme/solarized_light");
    var sessionClassDef = editorClassDef.getSession();
    sessionClassDef.setMode("ace/mode/text");
    sessionClassDef.setUseWrapMode(true);
    sessionClassDef.setWrapLimitRange(null, null);
    sessionClassDef.setValue("n/a");

    editorClassDef.on("change", function () {
      editorClassDefElem.style.height = 16 * sessionClassDef.getLength() + "px";
      editorClassDef.resize();
    });

    this.updateAceSession(id, {
      "content": sessionContent,
      "classDef": sessionClassDef
    });
  }

  Handler.prototype.updateAceSession = function (id, session) {
    this.aceSessions[id] = session;
  }

  return Handler;

});