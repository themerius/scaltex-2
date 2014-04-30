define("handler", ["jquery", "jquery.atwho"], function($) {

  var Handler = function () {
    this.lastCreatedEntityElemId = 0;
  }

  Handler.prototype.handle = function (jsonMsg, socket) {
    var entityElem = this.getOrCreateEntityElem(jsonMsg.from, socket);

    // pass things to scaltex.js for actual rendering:
    if (jsonMsg.classDef == "Text")
      entityElem.innerHTML = "<p>" + jsonMsg.text + "</p>";
    else if (jsonMsg.classDef == "Section")
      entityElem.innerHTML = "<h1>" + jsonMsg.nr + " " + jsonMsg.heading + "</h1>";
    else if (jsonMsg.classDef == "SubSection")
      entityElem.innerHTML = "<h2>" + jsonMsg.nr + " " + jsonMsg.heading + "</h2>";
    else if (jsonMsg.classDef == "SubSubSection")
      entityElem.innerHTML = "<h3>" + jsonMsg.nr + " " + jsonMsg.heading + "</h3>";
    else if (jsonMsg.classDef == "Figure")
      entityElem.innerHTML = "<img style=\"max-width: 480px\" src=\"" +
        jsonMsg.url + "\">" + "<p> Abb. " +
        jsonMsg.nr + ": " + jsonMsg.desc + "</p>";
    else if (jsonMsg.classDef == "PythonCode")
      entityElem.innerHTML = "Python Code get my calculation with id.returned";
    else if (jsonMsg.classDef == "ChemistryMolFormat")
      entityElem.innerHTML = "<p>TODO: Chemistry</p>";
    else
      entityElem.innerHTML = JSON.stringify(jsonMsg);
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
      }
    }
    return entityElem;
  }

  return Handler;

});