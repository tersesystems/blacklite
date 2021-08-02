# JBang Scripts

[JBang](https://www.jbang.dev/) is an environment that lets you write Java programs with embedded dependencies and will run and compile directly without a build configuration tool like Maven or Gradle.  It's very useful for scripting.

You can install JBang with SDKMan or Homebrew, and it will take care of JDK dependencies as well.

## Reader

The `reader.java` script contains a simple version of the blacklite reader and builds on top of the functionality.  

## Import CSV

The `csvimport.java` uses opencsv to write entries from a CSV file into Blacklite.