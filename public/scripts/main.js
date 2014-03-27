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

require(['websocket', 'config', 'handler', 'jquery'],
  function (WebSocket, config, Handler, jQuery) {

  jQuery.noConflict();

  var handler = new Handler();
  var socket = new WebSocket(config.webSocketAbsUrl, handler);

  require(['fancybox/jquery.fancybox'], function(fancybox) {
    function loadCss(url) {
      var link = document.createElement("link");
      link.type = "text/css";
      link.rel = "stylesheet";
      link.href = url;
      document.getElementsByTagName("head")[0].appendChild(link);
    }
    loadCss('../lib/fancybox/source/jquery.fancybox.css');

    var $ = jQuery;
    $(document).ready(function() {
      $(".various").click(function() {
        $(document).bind('DOMSubtreeModified', function() {
          if ($(".fancybox-iframe").length > 0) {
            var iframeID = $(".fancybox-iframe")[0].id;
            var ketcher = window.frames[iframeID].window.ketcher;

            if (ketcher)
              ketcher.setMolecule("\n  Ketcher 02151213522D 1   1.00000     0.00000     0\n\n  6  6  0     0  0            999 V2000\n   -1.1750    1.7500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -0.3090    1.2500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -0.3090    0.2500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -1.1750   -0.2500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -2.0410    0.2500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n   -2.0410    1.2500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n  1  2  1  0     0  0\n  2  3  2  0     0  0\n  3  4  1  0     0  0\n  4  5  2  0     0  0\n  5  6  1  0     0  0\n  6  1  2  0     0  0\nM  END");
          }
        });
      });

      $(".various").fancybox({
        maxWidth  : 800,
        maxHeight : 600,
        fitToView : false,
        width   : '70%',
        height    : '70%',
        autoSize  : false,
        closeClick  : false,
        openEffect  : 'none',
        closeEffect : 'none'
      });
    });

  });

});
