
<img src="./docs/logo.png"/>

crums-core
==========

Data model, parsers, and utilities for creating and verifying tamper-proof witness proofs,
and timestamping records. This same library is used by the servers to implement
the [crums.io](https://crums.io) time chain.

## Maven

To use this module add this dependency in your POM file:

```
  <dependency>
    <groupId>io.crums</groupId>
    <artifactId>crums-core</artifactId>
    <version>1.0.0</version>
  </dependency>
```

## Requirements

JDK 17+ required.

## Building


To build the project from source clone the repo and then invoke

```
$ mvn clean install -DskipTests
```

To run the tests (w/o the `-DskipTests` option), you must first clone and install this
[junit-io](https://github.com/gnahraf/junit-io) library locally.
(This will be fixed in the next version.)

## Documentation

The project documentation site is [here](https://crums-io.github.io/crums-pub/).


