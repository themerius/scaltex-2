define("handler", ["mustache", "jquery", "jquery.bootstrap", "jquery.atwho"], function(Mustache, $) {

  var Handler = function (templatesPath) {
    this.templatesPath = templatesPath;
    if (!this.templatesPath)
      this.templatesPath = "templates";
    this.autocompleteSet = {};
    this.socket = undefined;
    this.availableDocumentElements = [];
  }

  Handler.prototype.send = function (json) {
    if (this.socket)
      this.socket.sendJson(json);
  }

  Handler.prototype.setSocket = function (socket) {
    if (! this.socket)
      this.socket = socket;
  }

  Handler.prototype.handle = function (jsonMsg, socket) {
    this.setSocket(socket);

    var entityElem = this.getElem(jsonMsg._id);
    var handler = this;

    $.get(this.templatesPath + "/" + jsonMsg.classDef + ".html", function(tpl) {
      if (jsonMsg.svg) jsonMsg.svg = jsonMsg.svg.replace(/&#34;/g, "\"");
      var rendered = Mustache.render(tpl, jsonMsg);
      $(entityElem).html(rendered);  // jQuery evals also scripts.
      handler.updateAutocomplete(jsonMsg);
    });
  }

  Handler.prototype.updateAutocomplete = function (jsonMsg) {
    var autoCmpData = {
      name: jsonMsg.shortName || jsonMsg._id,
      id: jsonMsg._id,
      classDef: jsonMsg.classDef
    };

    this.autocompleteSet[autoCmpData.id] = autoCmpData;
    this.generateSemanticEditorModals(jsonMsg, this.socket);
  }

  Handler.prototype.getElem = function (id) {
    return document.getElementById("entity-" + id);
  }

  Handler.prototype.setAvailableDocumentElements = function (elements) {
    this.availableDocumentElements = elements;
  }

  Handler.prototype.initTopologyOrder = function (order) {
    for (idx in order) {
      var id = order[idx];
      var elem = this.createElem(id);
      $("#entities").append(elem);
      $(elem).after(this.createEmptyLine(id));
    }
  }

  Handler.prototype.createElem = function (id) {
    var tmpElem = document.createElement("div");
    tmpElem.id = "entity-" + id;
    if (id == "root")
      tmpElem.innerHTML = "<div class=\"semi-visible\">root</div>";
    else if (id == "meta")
      tmpElem.innerHTML = "<div class=\"semi-visible\">meta</div>";
    else
      tmpElem.innerHTML = "pending ...";

    if (this.templatesPath == "templates_latex")
      tmpElem.innerHTML = "<div class=\"semi-visible\"></div>";

    return tmpElem;
  }

  Handler.prototype.createEmptyLine = function (id) {
    var emptyLine = document.createElement("div");
    emptyLine.className = "empty-line";
    emptyLine.id = "empty-line-" + id;
    emptyLine.innerHTML = "&nbsp;";
    return emptyLine;
  }

  Handler.prototype.insertElement = function (json) {
    var elem = document.getElementById("empty-line-"+json.afterId);

    if (json.newId) {
      var newElem = this.createElem(json.newId);
      $(elem).after(newElem);
      $(newElem).after(this.createEmptyLine(json.newId));
    }

    if (json.ids) {
      json.ids = json.ids.reverse();
      for (idx in json.ids) {
        var id = json.ids[idx];
        if (typeof id != "function") {  // bugfix for prototype.js
          var newElem = this.createElem(id);
          $(elem).after(newElem);
          $(newElem).after(this.createEmptyLine(json.newId));
        }
      }
    }
  }

  Handler.prototype.remove = function (id) {
    $("#entity-" + id).remove();
    $("#empty-line-" + id).remove();
    $("#modal-" + id).remove();
  }

  Handler.prototype.enableHoverEffectForAnnotations = function () {
    var handler = this;

    var tmp="&nbsp;"; // TODO: create once and make visible/invisible
    tmp += "  <div class=\"new-element\">";
    tmp += "    <div class=\"btn-group\">";
    tmp += "      <button type=\"button\" class=\"btn btn-default btn-xs dropdown-toggle\" data-toggle=\"dropdown\">";
    tmp += "        <span class=\"glyphicon glyphicon-plus\"><\/span> <span class=\"caret\"><\/span>";
    tmp += "      <\/button>";
    tmp += "      <ul class=\"dropdown-menu\" role=\"menu\">";
    tmp += "        <li role=\"presentation\" class=\"dropdown-header\">@next</li>"
    for (idx in this.availableDocumentElements) {
      if (typeof this.availableDocumentElements[idx] != "function") {  // bugfix for prototype.js
        var elem = this.availableDocumentElements[idx];
        tmp += "      <li class=\"insertNext\"><a href=\"#\">" + elem + "<\/a><\/li>";
      }
    }
    tmp += "        <li class=\"divider\">insert/update first child<\/li>";
    tmp += "        <li role=\"presentation\" class=\"dropdown-header\">@firstChild</li>"
    for (idx in this.availableDocumentElements) {
      if (typeof this.availableDocumentElements[idx] != "function") {  // bugfix for prototype.js
        var elem = this.availableDocumentElements[idx];
        tmp += "      <li class=\"insertFirstChild\"><a href=\"#\">" + elem + "<\/a><\/li>";
      }
    }
    tmp += "      <\/ul>";
    tmp += "    <\/div>";
    tmp += "  <\/div>";

    $(".empty-line").off();
    $(".empty-line").on({
      mouseenter: function () {
        var currentEmptyLine = this;
        $(currentEmptyLine).html(tmp);
        $(".dropdown-menu li").on("click", function (event) {
          event.preventDefault();  // don't scroll to top (caused by #)
          var selectedClassDef = $(event.currentTarget).text();
          var func = event.currentTarget.className;
          handler.send({
            "function": func,
            "params": {
              "_id": $(currentEmptyLine).attr("id").split("empty-line-")[1],
              "contentSrc": "Edit me!",
              "documentElement": selectedClassDef
            }
          });
        })
      },
      mouseleave: function () {
        var currentEmptyLine = this;
          $(currentEmptyLine).html("&nbsp;");
          $(".dropdown-menu li").unbind("click");
      }
    });

    $(".visible").off();
    $(".visible").on({
      mouseenter: function () {
        $(this).addClass("annotation-hover");
      },
      mouseleave: function () {
        $(this).removeClass("annotation-hover")
      }
    });

    $.map(this.availableDocumentElements,
      function (elem, idx) {
        if (typeof elem != "function") {  // bugfix for prototype.js
          $("." + elem).off();
        }
      }
    );
    $.map(this.availableDocumentElements,
      function (elem, idx) {
        if (typeof elem != "function") {  // bugfix for prototype.js
          $("." + elem).on({
            mouseenter: function () {
              $("." + elem).addClass(elem + "-hover");
            },
            mouseleave: function () {
              $("." + elem).removeClass(elem + "-hover")
            }
          });
        }
      }
    );

    if (this.templatesPath == "templates_latex") {  // TODO: this is a hack!
      $(".Annotation").html(function(idx, oldHtml) {
        if (oldHtml.indexOf("\\emph") == 0)
          return oldHtml;
        else
          return "\\emph{" + oldHtml + "}";
      });
      $(".Reference").html(function(idx, oldHtml) {
        if (oldHtml.indexOf("\\citep") == 0)
          return oldHtml;
        else
          if (!oldHtml.match(/\((.*), (.*), S. (.*)\)/))
            return "\\citep{" + oldHtml.replace(/\((.*), .*\)/, "$1") + "}";
          else
            return "\\citep[S.~" + oldHtml.replace(/\((.*), (.*), S. (.*)\)/, "$3") + "]{" +
                   oldHtml.replace(/\((.*), (.*), S. (.*)\)/, "$1") + "}";
      });
      $(".Chapter").html(function(idx, oldHtml) {
        var id = $(".invisible", this).text();
        if (id && oldHtml.indexOf("\\ref") != 0)
          return "\\ref{" + id + "}";
        else
          return oldHtml;
      });
      $(".Section").html(function(idx, oldHtml) {
        var id = $(".invisible", this).text();
        if (id && oldHtml.indexOf("\\ref") != 0)
          return "\\ref{" + id + "}";
        else
          return oldHtml;
      });
      $(".SubSection").html(function(idx, oldHtml) {
        var id = $(".invisible", this).text();
        if (id && oldHtml.indexOf("\\ref") != 0)
          return "\\ref{" + id + "}";
        else
          return oldHtml;
      });
      $(".SubSubSection").html(function(idx, oldHtml) {
        var id = $(".invisible", this).text();
        if (id && oldHtml.indexOf("\\ref") != 0)
          return "\\ref{" + id + "}";
        else
          return oldHtml;
      });
      $(".Figure").html(function(idx, oldHtml) {
        var id = $(".invisible", this).text();
        if (id && oldHtml.indexOf("\\ref") != 0)
          return "\\ref{" + id + "}";
        else
          return oldHtml;
      });
      $(".Chemistry").html(function(idx, oldHtml) {
        var id = $(".invisible", this).text();
        if (id && oldHtml.indexOf("\\ref") != 0)
          return "\\ref{" + id + "}";
        else
          return oldHtml;
      });
      $(".Spray").html(function(idx, oldHtml) {
        var id = $(".invisible", this).text();
        if (id && oldHtml.indexOf("\\ref") != 0)
          return "\\ref{" + id + "}";
        else
          return oldHtml;
      });
      $("sup").html(function(idx, oldHtml) {  // Footnote
        if (oldHtml.indexOf("\\footnote") == 0)
          return oldHtml;
        else
          return "\\footnote{~" + oldHtml.replace(/_/g, "\\_") + "}";
      });
      $(".Math").html(function(idx, oldHtml) {  // Footnote
        if (oldHtml.indexOf("\\(") == 0)
          return oldHtml;
        else
          return "\\(" + oldHtml + "\\)";
      });
    }

  };

  Handler.prototype.generateSemanticEditorModals = function (view, socket) {
    var handler = this;

    // generate html code
    // TODO: every semantic editor should have it's own EditorModal?
    $.get(this.templatesPath + "/EditorModal.html", function(tpl) {
      if (view.domElem) view.domElem = view.domElem.replace(/&#34;/g, "\"");
      var newModal = Mustache.render(tpl, view);
      $("#modal-" + view._id).remove();  // TODO: instead of 'remove old modal', only update it!
      $("body").append(newModal);

      // and listen on save button
      $("#modal-" + view._id + "-button").on("click", function (event) {
        var contentElem = $("#modal-" + view._id + "-matter");
        contentElem.find("div").prepend("\n");
        contentElem.find("br").replaceWith("\n");
        $("#modal-" + view._id + "-matter .unique-name").remove();
        var content = contentElem.text();

        var domainEditorAvailable = require.specified("models/" + view.classDef);
        if (domainEditorAvailable) {
          require(["models/" + view.classDef], function (Editor) {
            var editor = new Editor(view._id);
            if (editor.getModel())
              content = editor.getModel();
            var svg = editor.drawSvg();

            console.log("CHANGE", content, contentElem);
            $("#modal-" + view._id).modal("hide");
            socket.sendJson({
              "function": "changeContentAndDocElem",
              "params": {
                "_id": view._id,
                "contentSrc": content,
                "documentElement": $("#modal-" + view._id + "-classDef").val() || view.classDef,
                "shortName": $("#modal-" + view._id + "-shortName").val() || view.shortName
              }
            });
            socket.sendJson({
              "function": "updateStateProperty",
              "params": {
                "_id": view._id,
                "property": {"svg": svg}
              }
            });
          });
        } else {
          console.log("CHANGE", content, contentElem);
          $("#modal-" + view._id).modal("hide");
          socket.sendJson({
            "function": "changeContentAndDocElem",
            "params": {
              "_id": view._id,
              "contentSrc": content,
              "documentElement": $("#modal-" + view._id + "-classDef").val() || view.classDef,
              "shortName": $("#modal-" + view._id + "-shortName").val() || view.shortName
            }
          });
        } 
      });

      $("#modal-" + view._id + "-movebutton").on("click", function (event) {
        var field = $("#modal-" + view._id + "-movefield");
        var ontoId = field.val();
        console.log("MOVE " + view._id + " onto " + ontoId);
        $("#modal-" + view._id).modal("hide");
        socket.sendJson({
          "function": "move",
          "params": {
            "_id": view._id,
            "onto": ontoId
          }
        });
      });

      $("#modal-" + view._id + "-removebutton").on("click", function (event) {
        console.log("REMOVE " + view._id);
        $("#modal-" + view._id).modal("hide");
        socket.sendJson({
          "function": "remove",
          "params": {
            "_id": view._id
          }
        });
      });

      handler.enableAutocomplete();
      handler.enableHoverEffectForAnnotations();

    });
  };

  Handler.prototype.enableAutocomplete = function () {
    var handler = this;
    var tpl = "<span class=\"projectional-variable\" contenteditable=\"false\">" +
                "<span class=\"unique-id invisible\">id_${id}_id</span>" +
                "<span class=\"unique-name ${classDef}\" title=\"${name}: ${classDef}\">" +
                "${name}</span>" +
              "</span>"

    var data = [];
    for(var key in handler.autocompleteSet)
      data.push(handler.autocompleteSet[key]);

    $(".modal-body").atwho({
      at: "@",
      data: data,
      tpl: "<li data-value='@${name}'>${name} <small>${classDef}</small></li>",
      insert_tpl: tpl
    });
  }

  return Handler;

});
