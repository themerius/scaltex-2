// Topology Datastructure
// ----------------------

var topo = {
  "root": {
    "next": "",
    "firstChild": "front matter"
  },

  "front matter": {
    "next": "body matter",
    "firstChild": "sec a"
  },

  "sec a": {
    "next": "par a",
    "firstChild": ""
  },

  "par a": {
    "next": "",
    "firstChild": ""
  },

  "body matter": {
    "next": "back matter",
    "firstChild": "intro"
  },

  "intro": {
    "next": "concl",
    "firstChild": "sec b"
  },

  "sec b": {
    "next": "par b",
    "firstChild": ""
  },

  "par b": {
    "next": "",
    "firstChild": ""
  },

  "concl": {
    "next": "",
    "firstChild": "sec c"
  },

  "sec c": {
    "next": "par c",
    "firstChild": ""
  },

  "par c": {
    "next": "par d",
    "firstChild": ""
  },

  "back matter": {
    "next": "",
    "firstChild": "sec e"
  },

  "sec e": {
    "next": "",
    "firstChild": ""
  }

}


// Algorithms
// ----------

// for depth-first search

function diggFirstChild(firstChild, stack) {
  if (topo[firstChild]) {
    if (topo[firstChild].firstChild) {
      stack.push(topo[firstChild].firstChild);
    }
    return diggFirstChild(topo[firstChild].firstChild, stack);
  } else {
    return stack;
  }
}


// for breadth-first search

function diggNext(next, stack) {  // breiten
  if (topo[next]) {
    if (topo[next].next) {
      stack.push(topo[next].next);
    }
    return diggNext(topo[next].next, stack);
  } else {
    return stack;
  }
}


// get the order of the document elements

var res = [];

function digg(begin) {

  var fcList = diggFirstChild(begin, []);

  if (fcList.length > 1)
    res.push(fcList[0]);

  while (fcList.length > 0) {

    var fc = fcList.pop();
    if (!topo[fc].firstChild)
      res.push(fc);
    var nList = diggNext(fc, []);

    while (nList.length > 0) {
      var next = nList.shift();
      res.push(next);
      digg(next);
    }
  }
  
}

digg("root");      // starting point is the root element
console.log(res);  // view the result
