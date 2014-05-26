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

* v0.4.0-SNAPSHOT

  * Introducing a more flexible and "generation friendly" architecture.
    Complete refactoring.

  * Update Xitrum 3.7 to 3.11 to 3.12 to 3.13.

  * Update Scala 2.10 to 2.11.1.

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
