require.config({
  paths: {
    'ace': '../lib/ace/lib/ace',
    'prototype': '../lib/ketcher/prototype-min'
  },
  wrap: {
    start: "(function() {",
    end: "}());"
  }
});

require(['websocket', 'config', 'handler', 'ketcher-editor'],
  function (WebSocket, config, Handler, ketcher) {

  var handler = new Handler();
  var socket = new WebSocket(config.webSocketAbsUrl, handler);

  ketcher.demo();

});
