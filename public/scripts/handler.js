define("handler", ["mustache", "jquery", "jquery.bootstrap"], function(Mustache, $) {

  var Handler = function () {
    this.lastCreatedEntityElemId = 0;
  }

  Handler.prototype.handle = function (jsonMsg, socket) {
    var entityElem = this.getOrCreateEntityElem(jsonMsg.from, socket);
    var handler = this;

    $.get("templates/" + jsonMsg.classDef + ".html", function(tpl) {
      var rendered = Mustache.render(tpl, jsonMsg);
      entityElem.innerHTML = rendered;
      handler.enableHoverEffectForAnnotations();
      handler.showEditorModal(jsonMsg);
    });
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

        var tmpElem = document.createElement("div");
        tmpElem.className = "empty-line";
        tmpElem.innerHTML = "&nbsp;";
        entities.appendChild(tmpElem);
      }
    }
    return entityElem;
  }

  Handler.prototype.enableHoverEffectForAnnotations = function () {
    var tmp="&nbsp;";
    tmp += "  <div class=\"new-element\">";
    tmp += "    <div class=\"btn-group\">";
    tmp += "      <button type=\"button\" class=\"btn btn-default btn-xs dropdown-toggle\" data-toggle=\"dropdown\">";
    tmp += "        <span class=\"glyphicon glyphicon-plus\"><\/span> <span class=\"caret\"><\/span>";
    tmp += "      <\/button>";
    tmp += "      <ul class=\"dropdown-menu\" role=\"menu\">";
    tmp += "        <li><a href=\"#\">Paragraph<\/a><\/li>";
    tmp += "        <li><a href=\"#\">Section<\/a><\/li>";
    tmp += "        <li><a href=\"#\">SubSubSection<\/a><\/li>";
    tmp += "        <li class=\"divider\"><\/li>";
    tmp += "        <li><a href=\"#\">Chemistry<\/a><\/li>";
    tmp += "      <\/ul>";
    tmp += "    <\/div>";
    tmp += "  <\/div>";

    $(".empty-line").on({
      mouseenter: function () {
        $(this).html(tmp);
      },
      mouseleave: function () {
        $(this).html("&nbsp;");
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

  Handler.prototype.showEditorModal = function (view) {
    var handler = this;

    $.get("templates/EditorModal.html", function(tpl) {
      var rendered = Mustache.render(tpl, view);
      $("body").append(rendered);
      handler.enableHoverEffectForAnnotations();
    });
  };

  return Handler;

});