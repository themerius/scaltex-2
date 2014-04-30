require.config({
  paths: {
    "jquery": "../lib/jquery/dist/jquery",
    "jquery.caret": "../lib/Caret.js/dist/jquery.caret.min",
    "jquery.atwho": "../lib/jquery.atwho/dist/js/jquery.atwho",
    "jquery.bootstrap": "../lib/bootstrap/dist/js/bootstrap"
  },
  "shim": {
    "jquery.caret": ["jquery"],
    "jquery.atwho": ["jquery", "jquery.caret"],
    "jquery.bootstrap": ["jquery"]
  }
});

require(["config", "websocket", "handler"],
  function (config, WebSocket, Handler) {

  var handler = new Handler();
  var socket = new WebSocket(config.webSocketAbsUrl, handler);

});
