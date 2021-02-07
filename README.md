# crums-pub

Data model, parsers, and utilities for creating and verifying tamper-proof data, witness proofs, timestamping, and custom-defined tamper-proof structures.

## Overview

Repo for the public interfaces and data abstractions used in the crums.io project.


## Dependencies

Project dependencies are listed in the maven `pom.xml` file. A few of these have no publicly distributed artifacts, and must be installed manually:

* [junit-io](https://github.com/gnahraf/junit-io)
* [merkle-tree](https://github.com/crums-io/merkle-tree)
* [io-util](https://github.com/crums-io/io-util)

To build to these, clone the above repos (in the suggested order) and invoke

> mvn clean install -DskipTests=true

in each of their directories. Omit the last argument above, to include unit tests.

## Documentation

The project documentation site is [here](https://crums-io.github.io/crums-pub/).


