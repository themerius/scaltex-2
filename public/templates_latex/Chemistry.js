define(["../../lib/ketcher/prototype-min"], function (pt) {

  var KetcherWrapper = function (id) {
    this.id = id;
    this.iframe = document.getElementById(id);
  }

  KetcherWrapper.prototype.drawEditor = function () {
    if (!this.iframe) {
      this.iframe = document.createElement("iframe");
      this.iframe.id = this.id;
      this.iframe.name = this.id;
      this.iframe.src = "lib/ketcher/ketcher.html";
      this.iframe.scrolling = "no";
      // TODO: bootstrap uses display none for modals!
      // iframe.style.display = "none";
      // bug with firefox, because "using display:none will NOT have
      // a rendering context (computedStyle)"" 
      // see: http://www.sencha.com/forum/showthread.php?132187
      this.iframe.style.height = "502px";
      this.iframe.style.width = "900px";
      this.iframe.setAttribute("seamless", "seamless");
      // todo: sandbox="allow-scripts allow-same-origin"
      return this.iframe;
    }
  }

  KetcherWrapper.prototype.getKetcher = function () {
    return this.iframe.contentWindow.ketcher;
  }

  KetcherWrapper.prototype.whenKetcherReady = function (func) {
    // Poll every 150ms if ketcherFrame is ready. Ugly but it works!
    var wrapper = this;
    var intervalCount = 0;
    var interval = setInterval(function() {
      if (intervalCount >= 25) {  // avoid endless loops
        clearInterval(interval);
        console.log("whenKetcherReady: waited too long. Maybe corrupted mol format?")
      }

      intervalCount += 1;

      if (wrapper.iframe.contentDocument.readyState == "complete") {
        clearInterval(interval);
        console.log("whenKetcherReady: polled " + intervalCount + " times.");
        return func();
      }
    }, 150);
  }

  KetcherWrapper.prototype.setModel = function (molFileString) {
    var wrapper = this;
    this.whenKetcherReady( function () {
      wrapper.getKetcher().setMolecule(molFileString)
    });
  }

  KetcherWrapper.prototype.getModel = function () {
    return "\n" + this.getKetcher().getMolfile();
  }

  KetcherWrapper.prototype.drawSvg = function () {
    var renderOpts = {
      'autoScale':true,
      'debug':true,
      'autoScaleMargin':20,
      'ignoreMouseEvents':true
    };

    var targetElem = document.createElement("div");
    document.body.appendChild(targetElem);

    this.getKetcher().showMolfileOpts(targetElem, this.getKetcher().getMolfile(), 20, renderOpts);

    var svg = targetElem.innerHTML;
    targetElem.parentNode.removeChild(targetElem);

    return svg;
  }

  return KetcherWrapper;

});