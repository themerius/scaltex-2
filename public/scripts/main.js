require.config({
  paths: {
    "jquery": "../lib/jQuery/dist/jquery",
    "jquery.caret": "../lib/Caret.js/dist/jquery.caret.min",
    "jquery.atwho": "../lib/jquery.atwho/dist/js/jquery.atwho",
    "jquery.bootstrap": "../lib/bootstrap/dist/js/bootstrap",
    "mustache": "../lib/mustache.js/mustache",
    "models": "../templates"
  },
  "shim": {
    "jquery.caret": ["jquery"],
    "jquery.atwho": ["jquery", "jquery.caret"],
    "jquery.bootstrap": ["jquery"]
  }
});

require(["config", "websocket", "handler"],
  function (config, WebSocket, Handler, m) {

  var handler = new Handler(config.templates);
  var socket = new WebSocket(config.webSocketAbsUrl, handler);

});
