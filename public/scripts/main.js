require.config({
  paths: {
    'ace': '../lib/ace/lib/ace',
    'prototype': '../lib/ketcher/prototype-min',
    'fancybox': '../lib/fancybox/source',
    'jquery': '../lib/fancybox/lib/jquery-1.10.1.min'
  },
  wrap: {
    start: "(function() {",
    end: "}());"
  }
});

require(['websocket', 'config', 'handler'],
  function (WebSocket, config, Handler, jQuery) {

  var handler = new Handler();
  var socket = new WebSocket(config.webSocketAbsUrl, handler);

});
