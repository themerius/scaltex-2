define('ketcher-editor', ['prototype'], function($) {

  function makeKetcherIFrame() {
    if (! document.getElementById("ketcherFrame")) {  // but make it only once
      var iframe = document.createElement("iframe");
      iframe.id = "ketcherFrame";
      iframe.name = "ketcherFrame";
      iframe.src = "lib/ketcher/ketcher.html";
      iframe.scrolling = "no";
      //iframe.style.display = "none";  // bug with firefox, because "using display:none will NOT have a rendering context (computedStyle)"" 
      // see: http://www.sencha.com/forum/showthread.php?132187
      iframe.style.height = 0;
      iframe.style.width = 0;
      document.body.appendChild(iframe);
    }
  }

  makeKetcherIFrame();

  function getKetcher() {
    var frame = null;

    if ('frames' in window && 'ketcherFrame' in window.frames)
      frame = window.frames['ketcherFrame'];
    else
      return null;
      
    if ('window' in frame)
      return frame.window.ketcher;
  }

  function renderMolFormat(targetElement, molAsString) {
    function render() {
      var renderOpts = {
        'autoScale':true,
        'debug':true,
        'autoScaleMargin':20,
        'ignoreMouseEvents':true
      };
      var ketcher = getKetcher();
      try {
        ketcher.showMolfileOpts(targetElement, molAsString, 20, renderOpts);
      } catch (e) {
        targetElement.innerHTML = "Invalid <i>mol</i> format? Click here to edit!";
      }
      
    }

    // Poll every 100ms if ketcherFrame is ready. Ugly but it works!
    var intervalCount = 0;
    var interval = setInterval(function() {
      if (intervalCount >= 25) {  // avoid endless loops
        clearInterval(interval);
        console.log("renderMolFormat waited too long. Maybe corrupted mol format?")
      }

      intervalCount += 1;

      if (window.frames.ketcherFrame.document.readyState == "complete") {
        render();
        clearInterval(interval);
        console.log("(renderMolFormat polled " + intervalCount + " times)");
      }
    }, 100);
  }

  function enableFancybox(id, input, sendSocketCallback) {
    var id = "#" + id;

    require(['jquery', 'fancybox/jquery.fancybox'], function(jQuery, fancybox) {
      jQuery.noConflict();

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
        $(id).click(function() {
          $(document).bind('DOMSubtreeModified', function() {
            if ($(".fancybox-iframe").length > 0) {
              var iframeID = $(".fancybox-iframe")[0].id;
              var ketcher = window.frames[iframeID].window.ketcher;

              if (ketcher)
                ketcher.setMolecule(input);
            }
          });
        });

        $(id).fancybox({
          maxWidth  : 800,
          maxHeight : 600,
          fitToView : false,
          width   : '70%',
          height    : '70%',
          autoSize  : false,
          closeClick  : false,
          openEffect  : 'none',
          closeEffect : 'none',
          beforeClose : function() {
            var iframeID = $(".fancybox-iframe")[0].id;
            var ketcher = window.frames[iframeID].window.ketcher;
            var mol = ketcher.getMolfile();
            sendSocketCallback(mol);
          }
        });
      });

    });
  }

  return {
    renderMolFormat: renderMolFormat,
    enableFancybox: enableFancybox
  };

});