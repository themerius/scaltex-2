define("handler", ["mustache", "jquery", "jquery.bootstrap", "jquery.atwho"], function(Mustache, $) {

  var Handler = function () {
    this.autocompleteData = [];
    this.socket = undefined;
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
    console.log(jsonMsg);

    this.autocompleteData.push({
      name: jsonMsg._id, 
      classDef: jsonMsg.classDef
    });

    $.get("templates/" + jsonMsg.classDef + ".html", function(tpl) {
      var rendered = Mustache.render(tpl, jsonMsg);
      entityElem.innerHTML = rendered;
      handler.generateSemanticEditorModals(jsonMsg, socket);
      handler.enableHoverEffectForAnnotations();
    });
  }

  Handler.prototype.getElem = function (id) {
    return document.getElementById("entity-" + id);
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
    else
      tmpElem.innerHTML = "pending ...";
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
      for (idx in json.ids) {
        var id = json.ids[idx];
        var newElem = this.createElem(id);
        $(elem).after(newElem);
        $(newElem).after(this.createEmptyLine(json.newId));
      }
    }
  }

  Handler.prototype.remove = function (id) {
    $("#entity-" + id).remove();
    $("#empty-line-" + id).remove();
  }

  Handler.prototype.enableHoverEffectForAnnotations = function () {
    var handler = this;

    var tmp="&nbsp;";
    tmp += "  <div class=\"new-element\">";
    tmp += "    <div class=\"btn-group\">";
    tmp += "      <button type=\"button\" class=\"btn btn-default btn-xs dropdown-toggle\" data-toggle=\"dropdown\">";
    tmp += "        <span class=\"glyphicon glyphicon-plus\"><\/span> <span class=\"caret\"><\/span>";
    tmp += "      <\/button>";
    tmp += "      <ul class=\"dropdown-menu\" role=\"menu\">";
    tmp += "        <li role=\"presentation\" class=\"dropdown-header\">@next</li>"
    tmp += "        <li class=\"insertNext\"><a href=\"#\">Paragraph<\/a><\/li>";
    tmp += "        <li class=\"insertNext\"><a href=\"#\">Section<\/a><\/li>";
    tmp += "        <li class=\"divider\">insert/update first child<\/li>";
    tmp += "        <li role=\"presentation\" class=\"dropdown-header\">@firstChild</li>"
    tmp += "        <li class=\"insertFirstChild\"><a href=\"#\">Section<\/a><\/li>";
    tmp += "      <\/ul>";
    tmp += "    <\/div>";
    tmp += "  <\/div>";

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
    $(".visible").on({
      mouseenter: function () {
        $(this).addClass("annotation-hover");
      },
      mouseleave: function () {
        $(this).removeClass("annotation-hover")
      }
    });
    $(".math").on({
      mouseenter: function () {
        $(".math").addClass("math-hover");
      },
      mouseleave: function () {
        $(".math").removeClass("math-hover")
      }
    });
    $(".chem").on({
      mouseenter: function () {
        $(".chem").addClass("chem-hover");
      },
      mouseleave: function () {
        $(".chem").removeClass("chem-hover")
      }
    });
    $(".val").on({
      mouseenter: function () {
        $(".val").addClass("val-hover");
      },
      mouseleave: function () {
        $(".val").removeClass("val-hover")
      }
    });
  };

  Handler.prototype.generateSemanticEditorModals = function (view, socket) {
    var handler = this;

    // generate html code
    $.get("templates/EditorModal.html", function(tpl) {
      var rendered = Mustache.render(tpl, view);
      var modal = $("#modal-" + view._id).remove();
      $("body").append(rendered);

      // and listen on save button
      $("#modal-" + view._id + "-button").on("click", function (event) {
        var contentElem = $("#modal-" + view._id + "-matter");
        contentElem.find("div").prepend("\n");
        contentElem.find("br").replaceWith("\n");
        var content = contentElem.text();
        console.log(content, contentElem);
        $("#modal-" + view._id).modal("hide");
        socket.sendJson({
          "function": "changeContentAndDocElem",
          "params": {
            "_id": view._id,
            "contentSrc": content,
            "documentElement": $("#modal-" + view._id + "-classDef").val() || view.classDef
          }
        });
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

      handler.enableAutocomplete("#modal-" + view._id + "-matter");

    });
  };

  Handler.prototype.enableAutocomplete = function (id) {
    var handler = this;
    var isit= $(id).atwho({
      at: "@",
      data: handler.autocompleteData,
      tpl: "<li data-value='@${name}'>${name} <small>${classDef}</small></li>",
      insert_tpl: "id_${name}_id"
    });
  }

  return Handler;

});
