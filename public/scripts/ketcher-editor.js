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
      ketcher.showMolfileOpts(targetElement, molAsString, 20, renderOpts);
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

  return {
    renderMolFormat: renderMolFormat
  };

});