# maven-semver-bumper
Simple groovy script to bump semver major version before branching.

If version is SNAPSHOT, then bumped version is SNAPSHOT also.
  
E.g. 
    1.2.3 -> 2.0.0
    2.3.4-SNAPSHOT -> 3.0.0-SNAPSHOT
