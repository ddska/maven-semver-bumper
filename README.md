# maven-semver-bumper

Simple groovy script to bump semver major version in maven projects (with sub-modules) before branching.

If version is SNAPSHOT, then bumped version is also SNAPSHOT.
  
E.g. you have 

<pre>
root POM (1.2.3-SNAPSHOT)
|-- api-module (3.4.5)
|-- impl-module (5.6.7-SNAPSHOT)
|-- another-parent-module (1.0.1-SNAPSHOT)
|--|--sub-module1 (1.2.3)
|--|--sub-module2 (2.3.4)
</pre>

and after bumping you get

<pre>
root POM (2.0.0-SNAPSHOT)
|-- api-module (4.0.0)
|-- impl-module (6.0.0-SNAPSHOT)
|-- another-parent-module (2.0.0-SNAPSHOT)
|--|--sub-module1 (2.0.0)
|--|--sub-module2 (3.0.0)
</pre>
