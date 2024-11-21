# Crums Timechain (Alpha)

Welcome to the first release of the Crums Timechain,
a key-less solution for generating cryptographic timestamps at scale.
Conceptually, a timechain is a chain of 64-byte blocks, generated at a constant,
steady rate (set at inception), using a cryptographic commitment scheme that
records evidence about when things (as represented by their SHA-256 hashes) occur.
The [project documentation page](https://crums-io.github.io/timechain/) details a
conceptual overview of timechains and how their proof structures work.

The project is organized in 4 modules, listed below.
The last 2, are end-user programs.

1. [timechain](./timechain) - Defines
the basic timechain, and its proof structures. Additionally, it provides client-side
code for accessing a timechain.
2. [notary](https://github.com/crums-io/crums-pub/tree/main/notary) - This module
implements the background daemon service that collects crums (witnessed hashes)
and publishes their collective hash per block (bin) interval as the cargo hash of
the corresponding timechain block. The notary is designed to be safe under concurrent
read/write access from multiple *processes* (not just threads).
3. [ergd](https://github.com/crums-io/crums-pub/tree/main/ergd) - This is a standaolone,
embedded HTTP REST server launched from the command line. New timechains can also be
incepted (created) thru this CLI.
4. [crum](https://github.com/crums-io/crums-pub/tree/main/crum) - This client-side CLI
posts SHA-256 hashes to timechain servers and archives permanent witness receipts
(called *crumtrails*) in a user repo.

## Status

The first (alpha) version is nearing release. It works.

### What's missing

TODOs:

* Need to work out details about how otherwise independent timechains on the network can choose to record one another's state in order to assert each others' bona fides.
* Multi-project build script.

## Building from source

The project's build tool is Maven. It uses Java's new virtual threads, so JDK 22
is a minimum requirement.

Clone the following repositories (in addition to this repo):

1. [merkle-tree](https://github.com/crums-io/merkle-tree) - Merkle tree implementation.
1. [io-util](https://github.com/crums-io/io-util) - Multi-module, utilies.
1. [stowkwik](https://github.com/crums-io/stowkwik) - Hash filepath object store.
1. [skipledger](https://github.com/crums-io/skipledger) - Commitment scheme.

Build these repos using in the above order, using

>   $ mvn clean install

The last build, the `skipledger` module, succeeds in building the base submodule
`skipledger-base` but *fails* to build related submodules. You can ignore those build
failures. (The skipledger submodules that don't build depend on an older versions of
this repo, now are archived under the `TC-1` subdirectory here in this repo.
If you build the legacy TC-1 in this repo first, then all `skipledger` should build.)

Next, build this project's library modules (in their respective subdir) in the
following order:

1. [timechain](./timechain/). Base lib. Both clients and server know about.
1. [notary](./notary). Server-side engine.

Finally, build the 2 deliverables

* [ergd](./ergd). Timechain server launch CLI, and
* [crum](./crum). Multi-chain client CLI with local (crumtrail) repo

using

  >   $ mvn clean package appassembler:assemble

 In their directories subdirectories, give these a try:

  >   ./target/binary/bin/ergd -h 

  >   ./target/binary/bin/crum -h





~ Babak

November 2024


