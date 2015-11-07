rubygems-servlets
=================

[![Build Status](https://secure.travis-ci.org/torquebox/rubygems-servlets.png)](http://travis-ci.org/torquebox/rubygems-servlets)

webapp which hosts rubygems or a proxy to rubygems. delivers gem maven artifacts as well. the proxy can be configured to be caching proxy or non-caching proxy

build
--

the regular webapp with hosted and (caching-)proxy can be built with

     mvn package

or just run it in place with

     mvn jetty:run

where the hosted rubygems are located under <http://localhost:8989/hosted> and the proxy with <https://rubygems.org> as source is reachable under <http://localhost:8989/caching> or <http://localhost:8989/proxy>. and the  <http://localhost:8989/merge> will combine all three to on rubygems repository.

executable standalone
---

you also can start the war like this (using embedded jetty):

     mvn package -Pexecutable
     java -jar target/rubygems.war

and adjust the <rubygems.properties> to your liking.

usage
--

add them to your gem command

* ```gem sources add <http://localhost:8989/hosted>```
* ```gem sources add <http://localhost:8989/caching>```
* ```gem sources add <http://localhost:8989/proxy>```
* ```gem sources add <http://localhost:8989/merged>```

or use the (caching-)proxy with bundler (example only for https://rubygems.org)

* ```bundler config mirror.https://rubygems.org http://localhost:8989/proxy```
* ```bundler config mirror.https://rubygems.org http://localhost:8989/caching```
* ```bundler config mirror.https://rubygems.org http://localhost:8989/merged```

the Gem-Artifacts are accessible via

*  <http://localhost:8989/hosted/maven/releases>
*  <http://localhost:8989/hosted/maven/prereleases>
*  <http://localhost:8989/caching/maven/releases>
*  <http://localhost:8989/caching/maven/prereleases>
*  <http://localhost:8989/proxy/maven/releases>
*  <http://localhost:8989/proxy/maven/prereleases>
*  <http://localhost:8989/merged/maven/releases>
*  <http://localhost:8989/merged/maven/prereleases>

you need a mirror declaration <http://rubygems-proxy.torquebox.org/releases> and <http://rubygems-proxy.torquebox.org/prereleases> in your settings.xml

    <settings>
      <mirrors>
        <mirror>
          <id>gems</id>
          <name>Rubygems</name>
          <url>http://localhost:8989/caching/maven/releases</url>
          <mirrorOf>rubygems-releases</mirrorOf>
        </mirror>
        <mirror>
          <id>pregems</id>
          <name>Rubygems Prereleases</name>
          <url>http://localhost:8989/caching/maven/prereleases</url>
          <mirrorOf>rubygems-prereleases</mirrorOf>
        </mirror>

since some old gem-artifacts use the those repositories (old in sense they originally came from rubygems-proxy.torquebox.org)

for more details about Gem-Artifacts see <https://github.com/sonatype/nexus-ruby-support/wiki/Gem-Artifacts>. for a solution with access control, more advanced proxy features and merging (group) to repositories see <https://github.com/sonatype/nexus-ruby-support>.

non-caching proxy
---

this proxy configuration does not cache the gem-files itself but instead sends a redirect to <rubygems.org>. all other files are cached the same way as the caching proxy:

    mvn jetty:run -P proxy

with url <http://localhost:8989/proxy>

rubygems-proxy.torquebox.org (not yet installed)
--

the webapp for this rubygems-proxy is under the profile **legacy**

     mvn clean package -Plegacy

which is just a proxy (mvn jetty:run -Plegacy)

*  <http://localhost:8989/releases>
*  <http://localhost:8989/prereleases>


tests
====

some integration tests for proxy feature can be executed with

    mvn -P run-its
	
    mvn -P run-its -Plegacy

deploy to maven central
-----------------------

    mvn versions:set
    git ci -m 'prepare release' pom.xml
    mvn -Prelease,executable
    git tag ...
    mvn versions:set
    git ci -m 'next dev version' pom.xml
    git push
    git push --tags

contributing
------------

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

meta-fu
-------

enjoy :) 
