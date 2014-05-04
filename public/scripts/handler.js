define("handler", ["mustache", "jquery", "jquery.bootstrap", "jquery.atwho"], function(Mustache, $) {

  var Handler = function () {
    this.lastCreatedEntityElemId = 0;
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

    var entityElem = this.getOrCreateEntityElem(jsonMsg.from);
    var handler = this;

    this.autocompleteData.push({
      name: "entity"+jsonMsg.from, 
      classDef: jsonMsg.classDef
    });

    $.get("templates/" + jsonMsg.classDef + ".html", function(tpl) {
      var rendered = Mustache.render(tpl, jsonMsg);
      entityElem.innerHTML = rendered;
      handler.generateSemanticEditorModals(jsonMsg, socket);
      handler.enableHoverEffectForAnnotations();
    });
  }

  Handler.prototype.getOrCreateEntityElem = function (id) {
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

        var tmpElem = document.createElement("div");
        tmpElem.className = "empty-line";
        tmpElem.innerHTML = "&nbsp;";
        entities.appendChild(tmpElem);
      }
    }
    return entityElem;
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
    tmp += "        <li><a href=\"#\">Text<\/a><\/li>";
    tmp += "        <li><a href=\"#\">Section<\/a><\/li>";
    // tmp += "        <li><a href=\"#\">SubSubSection<\/a><\/li>";
    // tmp += "        <li class=\"divider\"><\/li>";
    // tmp += "        <li><a href=\"#\">Chemistry<\/a><\/li>";
    tmp += "      <\/ul>";
    tmp += "    <\/div>";
    tmp += "  <\/div>";

    $(".empty-line").on({
      mouseenter: function () {
        $(this).html(tmp);
        $(".dropdown-menu li").on("click", function (event) {
          var selectedClassDef = $(event.currentTarget).text();
          handler.send({
            "function": "createEntityAndAppend",
            "params": {
              "content": "Edit me!",
              "cls": selectedClassDef
            }
          });
        })
      },
      mouseleave: function () {
        $(this).html("&nbsp;");
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
      var modal = $("#modal-" + view.from).remove();
      $("body").append(rendered);

      // and listen on save button
      $("#modal-" + view.from + "-button").on("click", function (event) {
        var contentElem = $("#modal-" + view.from + "-matter");
        contentElem.find("div").prepend("\n");
        contentElem.find("br").replaceWith("\n");
        var content = contentElem.text();
        console.log(content, contentElem);
        $("#modal-" + view.from).modal("hide");
        socket.sendJson({
          "function": "updateEntity",
          "params": {
            "id": view.from,
            "content": content,
            "cls": $("#modal-" + view.from + "-classDef").val() || view.classDef
          }
        });
      });

      handler.enableAutocomplete("#modal-" + view.from + "-matter");

    });
  };

  Handler.prototype.enableAutocomplete = function (id) {
    var handler = this;
    var isit= $(id).atwho({
      at: "@",
      data: handler.autocompleteData,
      tpl: "<li data-value='@${name}'>${name} <small>${classDef}</small></li>",
      insert_tpl: "${name}"
    });
  }

  return Handler;

});