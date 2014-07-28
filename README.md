[![Build Status](https://travis-ci.org/themerius/scaltex-2.png?branch=master)](https://travis-ci.org/themerius/scaltex-2)

# Codename: scaltex-2

Install [`bower`](http://bower.io/) for javascript dependency management.
Fetch the js dependencies with:

    cd public
    bower install

Note: `public/lib` is managed by `bower`.
Note: If you fetch the dependencies "freshly", you should apply the At.js patch!
See section *Patching At.js*.
Note: The bower dependencies are commited to git, to preserve the entire code.
If you are using the already commited client side javascript dependencies,
you don't have to apply the At.js patch.

Install [`CouchDB`](http://couchdb.apache.org/), which should (for example) listen
to http://127.0.0.1:5964/.

Install [`sbt`](http://www.scala-sbt.org/) to start the server,
then simply run:

    cd scaltex-2
    sbt "run --init --home http://127.0.0.1:5964/mydocument"

View the [Main Document](http://localhost:8000/), [Meta Document](http://localhost:8000/meta)
or the [LaTeX Code](http://localhost:8000/latex).
The `init` flag pushes a simple test document to `mydocument` on the database,
don't set init to load an already existing document.
With the `home` flag you can specify, where your document resides.

## Changelog

* v0.6.0: 2014-07-28

  * This version gets a DOI (digital object identifier),
    therefore added bower dependencies for preservation purposes.

  * Added new documentelements: Code, Math, Quotation, Reference, Footnote,
    ArabicList, RomanList, List and Spray

  * Added LaTeX-Code projection under http://localhost:8000/latex

  * Various Bugfixes

  * Switch between 'send actor state updates every time' and 
    'send updates only on state change'. Do a HTTP GET on http://localhost:8000/switch.
    Attention: 'on change only' doesn't doen't reload the entire document,
    but it is faster because the browser isn't flooded by updates.

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


## Contributions and Patches

My contributes to other open source projects used with scaltex:

* [dijon - Dynamic Json in Scala](https://github.com/pathikrit/dijon)
  * [Commited Bugfixes](https://github.com/pathikrit/dijon/commits?author=themerius)
  * Issue reporting
* [Xitrum](http://xitrum-framework.github.io)
  * Commited to documentation
  * Issue reporting
* [Ketcher]()
  * Branching and made available to bower dependency management

### Patching At.js

Via default At.js adds after a autocompleted term an space character (&nbsp;).
To avoid this in v0.4.10 you must alter line 340 in "jquery.atwho.js" to:

    content_node = "" + content;

The orginal line was:

    content_node = "" + content + "<span contenteditable='false'>&nbsp;<span>";

Note: This should be contributed to future versions of At.js.
This patch maybe should introduced as new settings flag, to be
enable or disable spaces after autocompleted terms.
[Here's the discussion on github](https://github.com/ichord/At.js/issues/186).

## Appendix

Emphasized technologies, hints or other useful stuff which made
implementation easier.
And the documentation about found sources of technologies,
which put out to be unsuitable or may useful (promising) for future work.

### Used

#### Bibtex

The References documentelement uses the scala bibtex parser from bibimbap.io.
I've resected it, so that only the bibtex parser is left.
This bibtex parser is in `src-bibtex` and is compiled together with scaltex.
Note: It needs as dependency Apache commons-lang3.

#### JSON on Scala side

* https://github.com/pathikrit/dijon

#### Browser: ContentEditable

* http://stackoverflow.com/questions/4705848/rendering-html-inside-textarea

#### Browser: Modals / Lightbox

* http://nakupanda.github.io/bootstrap3-dialog/

#### Browser: Autocomplete

* http://ichord.github.io/At.js/

#### Browser: White Space Handling

* http://code.stephenmorley.org/html-and-css/white-space-handling/

#### Browser: IFrame

* https://developer.mozilla.org/en-US/docs/Web/HTML/Element/iframe

#### Browser: Inject Scripts

* With jQuery.html() http://api.jquery.com/html/

#### My stackoverflow.com questions

Scala: General

* http://stackoverflow.com/questions/23993603/string-diffs-as-list
* http://stackoverflow.com/questions/22357778/using-scala-generics-in-function-definition

Scala: Meta Programming

* http://stackoverflow.com/questions/22251294/imain-valueofterm-only-gets-last-value
* http://stackoverflow.com/questions/22484748/getclass-out-of-string-and-using-within-generics
* http://stackoverflow.com/questions/22245079/transform-string-to-stringcontext-to-evaluate-deferred
* http://stackoverflow.com/questions/22330243/sbt-runs-imain-and-play-makes-errors

Scala: Regex

* http://stackoverflow.com/questions/24613398/multiline-regex-overjumps-items
* http://stackoverflow.com/questions/24507391/replace-all-invalid-escape-characters
* http://stackoverflow.com/questions/23289327/scala-regex-extract-expression-from-string

Javascript: Bower

* http://stackoverflow.com/questions/23082798/using-packages-dynamically-loogking-for-a-pattern

### Unsuitable

Technologies tried, but didn't entirely match the requirements.

#### As Editor View

* http://aloha-editor.org
* http://codemirror.net

#### As Autocomplete

* http://brianreavis.github.io/selectize.js/
* http://autocomplete.meteor.com
* http://ivaynberg.github.io/select2/
* http://complete-ly.appspot.com
* http://twitter.github.io/typeahead.js/

#### Modal / Lightbox

* http://lokeshdhakar.com/projects/lightbox2/
* http://fancybox.net

### Promising

#### Browser GUI Test Automation

* http://dalekjs.com/
* http://phantomjs.org/
* http://slimerjs.org/index.html
* http://casperjs.org/
* http://zombie.labnotes.org/

#### Programming Languages

* http://www.jolie-lang.org/
* http://julialang.org/
* http://julia.readthedocs.org/en/latest/manual/metaprogramming/
* http://www.rascal-mpl.org/

#### Scala Meta Programming "Hot Code"

[„Metaprogramming refers to a variety of ways a program has knowledge of itself or can manipulate itself.“](http://stackoverflow.com/questions/514644/what-exactly-is-metaprogramming)

Frameworks:

* https://github.com/xitrum-framework/scalive
* https://code.google.com/p/scalascriptengine/
* http://eed3si9n.com/treehugger/index.html

Mastering Reflection and the Scala Compiler Library:

* http://stackoverflow.com/questions/6839830/how-to-set-up-classpath-for-the-scala-interpreter-in-a-managed-environment
* http://stackoverflow.com/questions/18298077/scala-v-2-10-how-to-get-a-new-instance-of-a-class-object-starting-from-the-cl
* http://stackoverflow.com/questions/19330026/is-there-an-overview-of-the-nsc-compiler-api-for-scala-2-11
* http://stackoverflow.com/questions/12122939/generating-a-class-from-string-and-instantiating-it-in-scala-2-10
* http://stackoverflow.com/questions/2752206/dynamically-create-class-in-scala-should-i-use-interpreter
* http://stackoverflow.com/questions/1469958/scala-how-do-i-dynamically-instantiate-an-object-and-invoke-a-method-using-refl

Mastering ClassLoaders:

* http://stackoverflow.com/questions/728140/can-i-dynamically-unload-and-reload-other-versions-of-the-same-jar
* http://stackoverflow.com/questions/9819318/create-new-classloader-to-reload-class
* http://jimlife.wordpress.com/2007/12/19/java-adding-new-classpath-at-runtime/

#### Browser: Coordinates and Cursor positions

* http://stackoverflow.com/questions/6846230/coordinates-of-selected-text-in-browser-page
* http://stackoverflow.com/questions/2213376/how-to-find-cursor-position-in-a-contenteditable-div
* http://stackoverflow.com/questions/4834793/set-caret-position-right-after-the-inserted-element-in-a-contenteditable-div

#### Browser: Key Events

* http://stackoverflow.com/questions/4604057/jquery-keypress-ctrlc-or-some-combo-like-that

#### Browser: OCR

* http://projectnaptha.com
* http://antimatter15.com/ocrad.js/demo.html

#### Browser: Code Highlight

* http://highlightjs.org

#### Broser: Tag Input Box

* http://ioncache.github.io/Tag-Handler/

#### Scaling: clustering and modularization

How could scaltex be modularized?
Wrap every (bigger) document element in an Docker instance,
to automatically compile, deploy document elements on meta model changes?

Hold the meta model in the database?
Generate code of the actor system on meta model basis and deploy it (with docker)?
The so started actor system could expose Akka's communication protocols,
for distributed documents.

* https://www.docker.com
* https://coreos.com
* http://osv.io

Or use a plugin architecture like OSGi to inject other meta models:

* http://felix.apache.org/
* [iPOJO](http://felix.apache.org/documentation/subprojects/apache-felix-ipojo.html)
