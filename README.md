
### RubySonar - an advanced semantic indexer for Ruby

RubySonar is a semantic indexer for Ruby, which does
interprocedural analysis to infer types. It is one of the underlying
technologies that powers the code search site <a
href="https://sourcegraph.com/github.com/rails/rails">Sourcegraph</a>.

RubySonar is modeled after <a
href="https://github.com/yinwang0/pysonar2">PySonar2</a>, which does a similar
analysis for Python and has been in use by Sourcegraph and Google. To understand
its technical properties, please refer to my blog posts:

- http://yinwang0.wordpress.com/2010/09/12/pysonar


#### Demo

<img src="http://www.yinwang.org/images/rubysonar.gif" width="80%">



#### How to build

    mvn package



#### System Requirements

* irb

RubySonar uses the `irb` interpreter to parse Ruby code, so please make sure you
have it installed and pointed to by the `PATH` environment variable.



#### How to use

RubySonar is mainly designed as a library for IDEs and other developer tools, so
its interface may not be as appealing as an end-user tool, but for your
understanding of the library's capabilities, a reasonably nice demo program has
been built.

You can build a simple "code-browser" of your ruby code with the following
command line:

    java -jar target/rubysonar-0.1-SNAPSHOT.jar /path/to/project ./html

This will take a few minutes. You should find some interactive HTML files inside
the _html_ directory after this process.



#### License (BSD Style)

RubySonar - an advanced semantic indexer for Ruby

Copyright (c) 2013-2019 Yin Wang

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

1. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

1. The name of the author may not be used to endorse or promote products derived
   from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
OF SUCH DAMAGE.