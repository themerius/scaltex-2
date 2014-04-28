require.config({
  paths: {
    'ace': 'bower_components/ace/lib/ace'
  }
});

require(["ace/ace"], function (ace) {

  console.log("hello");
  var editor = ace.edit("editor");
editor.setTheme("ace/theme/monokai");
editor.getSession().setMode("ace/mode/javascript");

});