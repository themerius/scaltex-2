require.config({
  paths: {
    'ace': 'bower_components/ace/lib/ace',
  }
});

require(['websocket', 'config', 'handler'],
  function (WebSocket, config, Handler) {

  var handler = new Handler();
  var socket = new WebSocket(config.webSocketAbsUrl, handler);

});
