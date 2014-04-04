[![Build Status](https://travis-ci.org/themerius/scaltex-2.png?branch=master)](https://travis-ci.org/themerius/scaltex-2)

scaltex-2
=========

Install `bower` (http://bower.io/) for javascript dependency management.
Fetch the js dependencies with:

    cd public
    bower install

Note: `public/lib` is managed by `bower`.

Install `sbt` (http://www.scala-sbt.org/), then simply run:

    cd ../..
    sbt run

View `http://localhost:8000/`


Changelog
---------

* v0.3.0-SNAPSHOT

  * Update Xitrum 3.4 to 3.5

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
