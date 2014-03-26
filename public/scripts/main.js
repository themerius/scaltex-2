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

  // Poll every 100ms if ketcherFrame is ready. Ugly but it works!
  var intervalCount = 0;
  var interval = setInterval(function() {
    intervalCount += 1;
    if (window.frames.ketcherFrame.document.readyState === "complete") {
      ketcher(0);
      clearInterval(interval);
      console.log("(Interval polled " + intervalCount + " times)");
    }
  }, 100);

});
