[![Build Status](https://travis-ci.org/themerius/scaltex-2.png?branch=master)](https://travis-ci.org/themerius/scaltex-2)

scaltex-2
=========

Install [`bower`](http://bower.io/) for javascript dependency management.
Fetch the js dependencies with:

    cd public
    bower install

Note: `public/lib` is managed by `bower`.

Install `sbt` (http://www.scala-sbt.org/) to start the server,
then simply run:

    cd ../..
    sbt run

View [localhost:8000](http://localhost:8000/).

If you want to run the tests for the javascript frontend then you should
install [`DalekJS`](http://dalekjs.com/).

    cd public
    npm install
    dalek test/*.js

Or if testing on Google Chrome:

    dalek test/*.js -b chrome

Note: The tests need the server on [localhost:8000](http://localhost:8000/).

Changelog
---------

* v0.6.0-SNAPSHOT

  * ...

* v0.5.0: 2014-06-17

  * Introducing meta document at location http://localhost:8000/meta
    You can use it for hidden elements, which are only interesting as reference
    within the actual document.

  * Remove elements within the hierarchy.

  * New document elements: Figure, Python Code, Table of Contents, Chemistry (Ketcher).

  * Added a Command Line Interface. init creates a new little example document,
    home specifies where the document is stored.
    `sbt "run --init --home http://127.0.0.1:5984/mydocument"`

* v0.4.0: 2014-06-05

  * Introducing a more flexible and "generation friendly" architecture.
    Complete refactoring.

  * Update Xitrum 3.7 to 3.11 to 3.12 to 3.13.

  * Update Scala 2.10 to 2.11.1.

  * Now with hierarchy.

  * Insert and Move document elements within the hierarchy.

  * Better CouchDB persistance: load and save document at any couchdb-url.

  * Every document element can now have a short name for referencing
    (a "projectional variable")

  * Highlight the docuement element type on mouseover (on a with scala evaluated reference).

* v0.3.0: 2014-05-08

  * Update Xitrum 3.4 to 3.5 to 3.7

  * New editor gui (but without Ketcher). Based on `misc/semantic-editor/mock_2`.
    Highligts annotations, support for auto completion.

  * CouchDB persistance support: can load and save test_document

* v0.2.0: 2014-04-03

  * New Entities: SubSection, SubSubSection, Figure, PythonCode, ChemistryMolFormat.

  * Python code "live" runable, can pipe a result to an other entity actor.

  * Ketcher JavaScript Chemistry Editor added. "Live" edit of chemical formulars.

  * Sub-Sections can resolve it's section number.

* v0.1.0: 2014-03-20

  * Actors as AST. Available Entities: Section, Text.

  * Entities can reference to each other.

  * Each Section can resolve it's section number.

  * Multiple Browers can edit the document.

  * Using bower to manage javascript dependencies.

  * Requirejs manages javascripts "imports" (AMD).
