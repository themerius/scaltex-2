define('ketcher-editor', ['prototype'], function($) {

  function makeKetcherIFrame() {
    var iframe = document.createElement("iframe");
    iframe.id = "ketcherFrame";
    iframe.name = "ketcherFrame";
    iframe.src = "lib/ketcher/ketcher.html";
    iframe.scrolling = "no";
    iframe.style.display = "none";
    document.body.appendChild(iframe);
  }

  function makeView(id) {
    var view = document.createElement("div");
    view.id = "ketcherChemistryView-" + id;
    view.style.height = "200px";
    document.body.appendChild(view);
  }

  function getKetcher() {
    var frame = null;

    if ('frames' in window && 'ketcherFrame' in window.frames)
      frame = window.frames['ketcherFrame'];
    else
      return null;
      
    if ('window' in frame)
      return frame.window.ketcher;
  }

  function loadMol () {
    initialMolecule = 
    [
      "",
      "  Ketcher 02151213522D 1   1.00000     0.00000     0",
      "",
      "  6  6  0     0  0            999 V2000",
      "   -1.1750    1.7500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0",
      "   -0.3090    1.2500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0",
      "   -0.3090    0.2500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0",
      "   -1.1750   -0.2500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0",
      "   -2.0410    0.2500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0",
      "   -2.0410    1.2500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0",
      "  1  2  1  0     0  0",
      "  2  3  2  0     0  0",
      "  3  4  1  0     0  0",
      "  4  5  2  0     0  0",
      "  5  6  1  0     0  0",
      "  6  1  2  0     0  0",
      "M  END"
    ].join("\n");
    return initialMolecule;
  }

  function render (molfileId) {
    var molfile = loadMol();
    var targetElement = document.getElementById("ketcherChemistryView-" + molfileId);
    var renderOpts = {
      'autoScale':true,
      'debug':true,
      'autoScaleMargin':20,
      'ignoreMouseEvents':true
    };
    var ketcher = getKetcher();
    ketcher.showMolfileOpts(targetElement, molfile, 20, renderOpts);
  }

  makeKetcherIFrame();
  makeView(0);

  return render;

});