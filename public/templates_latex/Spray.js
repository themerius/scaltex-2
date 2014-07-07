define(['jquery'], function ($) {

  var SprayWrapper = function (id, uuid) {
    this.id = id;
    this.uuid = uuid;
    this.iframe = document.getElementById(id);
  }

  SprayWrapper.prototype.drawEditor = function () {
    if (!this.iframe) {
      this.iframe = document.createElement("iframe");
      this.iframe.id = this.id;
      this.iframe.name = this.id;
      this.iframe.src = "http://141.37.31.44:9000/editor/PetriNet/" + this.uuid;
      this.iframe.scrolling = "no";
      // TODO: bootstrap uses display none for modals!
      // iframe.style.display = "none";
      // bug with firefox, because "using display:none will NOT have
      // a rendering context (computedStyle)"" 
      // see: http://www.sencha.com/forum/showthread.php?132187
      this.iframe.style.height = "550px";
      this.iframe.style.width = "100%";
      this.iframe.setAttribute("seamless", "seamless");
      // todo: sandbox="allow-scripts allow-same-origin"
      return this.iframe;
    }
  }

  SprayWrapper.prototype.getSpray = function () {
    return this.iframe.contentWindow.app;
  }

  SprayWrapper.prototype.getModel = function () {
    return undefined;
  }

  SprayWrapper.prototype.whenSprayReady = function (func) {
    // Poll every 150ms if ketcherFrame is ready. Ugly but it works!
    var wrapper = this;
    var intervalCount = 0;
    var interval = setInterval(function() {
      if (intervalCount >= 25) {  // avoid endless loops
        clearInterval(interval);
        console.log("whenSprayReady: waited too long. Maybe corrupted mol format?")
      }

      intervalCount += 1;

      if (wrapper.iframe.contentDocument.readyState == "complete") {
        clearInterval(interval);
        console.log("whenSprayReady: polled " + intervalCount + " times.");
        return func();
      }
    }, 150);
  }


  SprayWrapper.prototype.drawSvg = function () {

    // var svg = null;
    // var targetElem = document.createElement("div");
    // document.body.appendChild(targetElem);

    // $.get("http://141.37.31.44:9000/editor/PetriNet/" + this.uuid + "/svg", function (data) {
    //   console.log(data);
    //   targetElem.innerHTML = data;
    //   svg = targetElem.innerHTML;
    //   targetElem.parentNode.removeChild(targetElem);
    // });

    return '<svg height="100" width="100"><circle cx="50" cy="50" r="40" stroke="black" stroke-width="3" fill="red" /></svg> ';
  }

  return SprayWrapper;

});