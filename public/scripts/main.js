require.config({
  paths: {
    'ace': 'bower_components/ace/lib/ace',
  }
});

require(['websocket'], function(ws) {
  new ws;
});
