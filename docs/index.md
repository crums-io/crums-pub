<img src="./logo.png"/>

# crums-core

Data model, parsers, and utilities for creating and verifying tamper-proof data, witness proofs, timestamping, and custom-defined tamper-proof structures.

## Overview

Documentation for the public interfaces and data abstractions used in the crums.io project are gathered here. While the [service](https://crums.io/docs/rest.html) is supposed to operate with no down time, the digital products it dispenses are designed to be *verifiable independent* of the service. The goal here is to provide some basic tools to make that possible as well as provide some programmatic wrappers for calling the API.

### About the Name

**Crum.** archaic variant of *crumb*.

(Well, not anymore.)


## Maven

To use this module add this dependency in your POM file:

```
<dependency>
  <groupId>io.crums</groupId>
  <artifactId>crums-core</artifactId>
  <version>1.0.0</version>
</dependency>
```


## Merkle Trees

The service leans heavy on Merkle trees. It uses a standalone library (included in the distribution) that aids in navigating a Merkle tree's nodes. This is used both to generate and validate Merkle proofs. The library's doc page is [here](https://crums-io.github.io/merkle-tree/).

### Configuration

In the crums.io use case, the above library is configured to use SHA-256 with fixed-width, 32-byte leaves. Each 32-byte leaf is itself the SHA-256 hash of something else--of exactly what, the library doesn't know.

## Data Model

Every time the service witnesses a hash it doesn't remember seeing, it computes a 32-byte hash of the concatenation that hash and the current time in milliseconds expressed as an 8-byte long value (big endian byte order). The service then places this 32-byte hash as a leaf in the next Merkle tree that it publishes (roughly once a minute under adequate load).  If we use `(x)` to represent the SHA-256 hash of a byte string *x*, then if the newly witnessed hash were 7f7f5d03de1e5e2c8a2a358d8d70238a93b16df85a15e01cbd08b8c671133e77 (in hex notation) and the UTC time in milliseconds were 1593604800123, then the leaf in this notation would be

```
( 7f7f5d03de1e5e2c8a2a358d8d70238a93b16df85a15e01cbd08b8c671133e77 000001730a3f7e7b )
```

ignoring white space. We call this tuple (and its derivative SHA-256 hash) a *crum*.

After the crum has found its way into the next Merkle tree, the service can then pair this crum with its Merkle proof. The combination produced by this pairing is called *crumtrail*.

Excepting the last, the value for every leaf in the Merkle tree is computed this way.

## Audit Trail

The very last leaf in each of crums.io's Merkle trees is the root hash of the previous tree. The Merkle proofs of these last leaves, in turn, form an unbroken chronological chain. While the service does not maintain the full Merkle trees indefinitely, it does maintain and backup meta data about the trees it has published for posterity. This meta data includes these tree-linking Merkle proofs. And since any 2 Merkle proofs from the same tree intersect (at the root node, at least), Merkle proofs remembered by 3rd parties but forgotton at the service can still be verified against this one Merkle proof the service remembers.


## Hash Grammar

A small experimental parser for a [hash grammar DSL](./protoHashGrammar.md) is still included in the distribution. This will be removed in future versions.


## Repo

The repo for this project is located [here](https://github.com/crums-io/crums-pub).

## API

The javadoc bundled with distribution from Maven Central complements the [REST API](https://crums.io/docs/rest.html).

~ Sept 8 2022

