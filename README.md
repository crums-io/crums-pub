# Crums Timechain (Alpha)

This is the source repo for the Crums timechain. I'm developing a new version
of the timechain. Unlike its previous incarnation, this repo now includes both
client- *and* server-side code. (The client for the legacy chain is archived 
under the [TC-1](https://github.com/crums-io/crums-pub/tree/main/TC-1) subdirectory.)

The [project documentation page](https://crums-io.github.io/crums-pub/) details
a conceptual overview of timechains and their proof structures.

The implementation is organized in 3 modules, each layered atop the other.

1. [timechain](https://github.com/crums-io/crums-pub/tree/main/timechain) - Defines
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

## Status

The first (alpha) version is nearing release. It works.

### What's missing

The most glaring TODOs:

* Command line timechain client. (There'll be *something* on first release.)
    * witness
    * verify crumtrail
    * update block proof (in crumtrail) from timechain
* Client-side storage and archival of crumtrails (witness proofs) needs work. As a chain evolves (as it accumulates new blocks) the block proofs in archived crumtrails can be updated *en mass*.
With a bit of planning, this can be made efficient, since crumtrails from the same chain share
the same lineage and therefore share common information.
* Need to work out details about how otherwise independent timechains on the network can choose to record one another's state in order to assert each others' bona fides.
* Broken landing page (To be fixed before release).
* Snapshot build script.

## Building the SNAPSHOT

The project's build tool is Maven. It uses Java's new virtual threads, so JDK 22
(at the time of this writing, the latest) is a minimum requirement. Presently, SNAPSHOT versions are not published
anywhere. Much has been refactored across projects and much remains before the SNAPSHOT
moniker can be dropped. To build this project, you'll have to clone and build a number
of dependencies yourself. Clone the following projects in the suggested order, and build
each using

>   $ mvn clean install



1. [merkle-tree](https://github.com/crums-io/merkle-tree) - Merkle tree implementation. Dependencies: none.
1. [io-util](https://github.com/crums-io/io-util) - Small, multi-module, utility library. Dependencies: none.
1. `$ cd TC-1` - Build the legacy client (the TC-2 subdir in *this* repo). This is slated for removal after refactorings across this and the next project (skipledger) are completed. Dependencies: `merkle-tree`, `io-util`.
1. [skipledger](https://github.com/crums-io/skipledger) - Base module defining the data
structure and other modules for packaging proofs from general ledgers. This project used to
know about this repo. The code was refactored so that the base layer no longer knows about
this project (the relationship is in fact now reversed). Dependencies: `merkle-tree`, `io-util`, `tc-1`
1. Clone *this* project and build:

    1. [timechain](https://github.com/crums-io/crums-pub/tree/main/timechain). Dependencies: `skipledger-base`.
    2. [notary](https://github.com/crums-io/crums-pub/tree/main/notary). Dependencies: `timechain`, `stowkwik`.
    3. [ergd](https://github.com/crums-io/crums-pub/tree/main/ergd). Dependencies: `notary`.


To build the last project `ergd` (the REST server)

>   $ mvn clean package appassembler:assemble

Then give this a try..

>   $ ./target/binary/bin/ergd -h



~ Babak

July 2024


