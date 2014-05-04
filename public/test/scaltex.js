module.exports = {

  'Page title is correct': function (test) {
    test
      .open('http://localhost:8000/')
      .wait(1000)
      .assert.title().is('Welcome to scaltex', 'It has a title')
      .done();
  },
  'Test elements are present': function (test) {
     test
      .open('http://localhost:8000/')
      .wait(1000)//.waitForElement('.visible:nth-child(7)')
      .assert.numberOfElements('.visible')
        .is(7, '7 elements of class visible loaded')
      .done();

    test
      .open('http://localhost:8000/')
      .wait(1000)
      .$('.visible:first-child')
        .assert.visible()
        .assert.text('1 Introduction')
      .$('.visible:last-child')
        .assert.visible()
        .assert.text('3 Summary')
      done();
  },
  'Click on elements to show semantic editor': function (test) {
    test
      .open('http://localhost:8000/')
      .wait(1000)
      .assert.notVisible('#modal-2')
      .click('#entity2')
      .assert.visible('#modal-2')
      .done();
  }

};