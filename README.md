rubygems-servlets
=================

webapp which hosts rubygems or a proxy to rubygems. delivers gem maven artifacts as well. the proxy can be configured to be caching proxy or non-caching proxy

build
--

the regular webapp with hosted and (caching-)proxy can be built with

     mvn package

or just run it in place with

     mvn jetty:run

where the hosted rubygems are located under <http://localhost:8989/hosted> and the proxy with <https://rubygems.org> as source is reachable under <http://localhost:8989/hosted>.

usage
--

add them to your gem command

* ```gem sources add <http://localhost:8989/hosted>```
* ```gem sources add <http://localhost:8989/caching>```

or use the (caching-)proxy with bundler

* ```bundler config mirror.http://rubygems.org http://localhost:8989/caching```
* ```bundler config mirror.https://rubygems.org http://localhost:8989/caching```

the Gem-Artifacts are accessible via

*  <http://localhost:8989/hosted/maven/releases>
*  <http://localhost:8989/hosted/maven/prereleases>
*  <http://localhost:8989/caching/maven/releases>
*  <http://localhost:8989/caching/maven/prereleases>

as mirror to <http://rubygems-proxy.torquebox.org/releases> and <http://rubygems-proxy.torquebox.org/prereleases> add to your settings.xml

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

for more details about Gem-Artifacts see <https://github.com/sonatype/nexus-ruby-support/wiki/Gem-Artifacts>. for a solution with access control, more advanced proxy features and merging (group) to repositories see <https://github.com/sonatype/nexus-ruby-support>.

rubygems-proxy.torquebox.org
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

