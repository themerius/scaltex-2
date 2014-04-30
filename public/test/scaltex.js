module.exports = {
  'Page title is correct': function (test) {
    test
      .open('http://localhost:8000/')
      .assert.title().is('Welcome to scaltex', 'It has a title')
      .done();
  }
};