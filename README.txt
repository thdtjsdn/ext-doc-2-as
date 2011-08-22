ext-doc is a ExtJS-style JavaScript comments processor
by Andrey Zubkov aka oxymoron
http://ext-doc.googlecode.com

ext-doc-2-as is a Jangaroo tweak of the original ext-doc tool that,
instead of generating HTML, generates AS3 API code.
We use the tool to generate an AS3 API for Ext JS (called Ext AS).
https://github.com/CoreMedia/jangaroo-libs/tree/master/ext-as

Requires Java 1.6 to run.

How to update Ext AS:

1) Download latest ExtJS 3.x source code from Sencha
2) Merge into https://github.com/CoreMedia/ext-js-doc-fixes
3) Create "sample/ext" folder
4) Copy ExtJS source to "sample/ext" (ex.: sample/ext/src/core/Ext.js)
5) Build ext-doc-2-as using ant
6) Goto "build/dist/ext-doc-snapshot/sample/" and run "ext-doc.(bat|sh)"
   ("build/dist/ext-doc-snapshot/output" directory will be created)
7) Copy "build/dist/ext-doc-snapshot/output/output" to jangaroo-libs/ext-as:
   * "output/ext/config" to "src/main/joo"
   * "output/ext/*" (but not config) to "src/main/joo-api"
8) Check the resulting diffs!
9) Commit changes into jangaroo-libs

If you change the tool rather than update to latest Ext JS documentation,
just leave out steps 1) and 2) and use source from ext-js-doc-fixes as-is.
