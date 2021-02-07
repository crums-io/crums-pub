<img src="./logo.png"/>

# crums-pub

Data model, parsers, and utilities for creating and verifying tamper-proof data, witness proofs, timestamping, and custom-defined tamper-proof structures.

## Overview

Documentation for the public interfaces and data abstractions used in the crums.io project are gathered here. While the [service](https://crums.io/docs/rest.html) is supposed to operate with no down time, the digital products it dispenses are designed to be *verifiable independent* of the service. The goal here is to provide some basic tools to make that possible.

#### About the Name

We wanted crumbs, and 'b' was obviously taken. But if the service is about leaving trails using crumbs, we optimistically reasoned, then a crum with a missing 'b' is just the crumb of another crumb. Hope this helps remember our name 🙃.


## Merkle Trees

The service leans heavy on Merkle trees. It uses a standalone library (included in the distribution) that aids in navigating a Merkle tree's nodes. This is used both to generate and validate Merkle proofs. The library's doc page is [here](https://crums-io.github.io/merkle-tree/).

### Configuration

In the crums.io use case, the above library is configured to use SHA-256 with fixed-width, 32-byte leaves. Each 32-byte leaf is itself the SHA-256 hash of something else--of exactly what, the library doesn't know.

## Data Model

Every time the service witnesses a hash it doesn't remember seeing, it computes a 32-byte hash of the concatenation that hash and the current time in milliseconds expressed as an 8-byte long value (big endian byte order). The service then places this 32-byte hash as a leaf in the next Merkle tree that it publishes (roughly once a minute under adequate load).  If we use `(x)` to represent the SHA-256 hash of a byte string *x*, then if the newly witnessed hash were 7f7f5d03de1e5e2c8a2a358d8d70238a93b16df85a15e01cbd08b8c671133e77 (in hex notation) and the UTC time in milliseconds were 1593604800123, then the leaf in this notation would be

> ( 7f7f5d03de1e5e2c8a2a358d8d70238a93b16df85a15e01cbd08b8c671133e77 000001730a3f7e7b )

ignoring white space. We call this tuple (and its derivative SHA-256 hash) a *crum*.

After the crum has found its way into the next Merkle tree, the service can then pair this crum with its Merkle proof. The combination produced by this pairing is called *crumtrail*.

Excepting the last, the value for every leaf in the Merkle tree is computed this way.

## Audit Trail

The very last leaf in each of crums.io's Merkle trees is the root hash of the previous tree. The Merkle proofs of these last leaves, in turn, form an unbroken chronological chain. While the service does not maintain the full Merkle trees indefinitely, it does maintain and backup meta data about the trees it has published for posterity. This meta data includes these tree-linking Merkle proofs. And since any 2 Merkle proofs from the same tree intersect (at the root node, at least), Merkle proofs remembered by 3rd parties but forgotton at the service can still be verified against this one Merkle proof the service remembers.


## Hash Grammar

*This development is new and is not yet incorporated across the API.*

A Merkle tree is an example of a tamper-proof data structure. More generally, *the contents of any acyclic data structure can be rendered tamper-proof by employing hash pointers*. Besides crums, binary (Merkle) trees, we may be motivated to use other tamper-proof structures. Blockchains, for example, model linked lists. While the designs of such structures will be varied, some surely yet to be imagined, the principles behind their verifications are fundamentally the same. To this end, we define a super simple hashing grammar.

The general recipe for proving that an object is untampered involves computing its hash and then checking the result of this computation against its "advertised" hash: if the 2 match (and you trust the advertisement) then the object is indeed what you expect it to be.  As is the case with Merkle trees, computing this hash is typically not as simple as say just computing the hash of a file that contains the tree: you start from a leaf hash (in the case of crums.io the *constituents* of the leaf), and by means of a series concatentations and hashings arrive at the value at the root of the tree.

### Objectives

1. If a tamper-proof data structure advertises its proof using the grammar, then a user can still verify its proof even if they don't know about the details of the data structure.

2. A common tool for verifying such tamper-proof structures.

3. Human readibility is paramount. At a glance, a user should be able to spot the data that is being proven and what the expected result of the hash proof computed is.

### Syntax

### Entities

Entities (expressions) evaluate to a sequence of bytes. The default (implicit) operation in the grammar is concatenation. That is, if 2 expressions are separated by white space or otherwise delimited by the rules of the grammar, then the resulting expression evaluates to the concatenation of the evaluation of the subexpressions. Evaluation is greedy, left-to-right.

* *Hex/byte literals* - The simplest entities are byte literals expressed as *even* lengthed hexadecimal strings. For clarity, literals may be separated with white space, however every contiguous sequence must still be of even length (describing whole bytes).
* *Hash/parentheses* - An entity enclosed in a pair of matching parantheses `(`   `)` is to be hashed (SHA-256). The byte string evaluated to is 32 bytes (64 hexadecimal digits) wide.
* *Group/bracket* - Sequences of entities can be grouped together when enclosed in matching brackets `[` `]`. It's use case is in the following operation.
* *Flip/colon* - If 2 entities are separated by a colon `:` then the 2 entities are transposed (flipped) in position. The operator is greedy.

### Statements

A statement is just a pair of entities separated by an equal sign `=` that evaluates to a boolean true/false.

#### Example

The following is the hash proof in a crumtrail described by the grammar:

> ((((((((((((((((((7f7f5d03de1e5e2c8a2a358d8d70238a93b16df85a15e01cbd08b8c671133e77 000001730a3f7e7b) :00 00 368421a67b068bfcc8b4e05366adae380b26ce2d486abed8cffe404dea1cd9e3):01 :[01 e32f115aea479bbd1b5a10b47f8a19b17c43d00f931442298970bd3c699bf2d9]):01 :[01 23ba7b426557f8833e03f753cfe7ff3ef66c5f73dbb06ff07cb4f9ad789f60ee]):01 01 47b23492575bb67aed064077b438e170cc6624fdbf3348c385781561f225547c):01 01 f183933570e56792f83e3fa62f84eef2139c2f40fbf2a282cd9c1617f7665414):01 :[01 4db66d8156cac837a9dbf80ab20f49cf21fa9440e51c64197fec3e321f896197]):01 01 523b2dcb74c79aa0e0a42be3d4567d5a0b0da2eafdde3f74629ea20771a5b7ee):01 01 2e1a73635bc4068b400bf267c73e730ec87c0c83e3d0dec09d9cce059c5e27fa):01 01 5b9fa669d3557277108809d75449c5ea71cf6687adef5cd30ca1f008f043ed15):01 01 f29861f27e9068a516114b22b57331897d4873c785e94ff07490eea8876c7e63):01 01 8334a7958263aa7343423922e1123a2c9ad5927fa88ee568c86b07f27a5a41b4):01 01 7beef39f970c315dc474e0206052e73dbf716c4478defc4bdb41bdf1a39ea115):01 :[01 29ea769e2e63906d1861313cd62c37ff0ed59f1affb1acca29faa9a1d78730f7]):01 01 ac34fdb46ae74c40905f89cd351a580e14d7db7d9392bd2b92bd971b42dacea4):01 :[01 b85bddcbcad0c5d4b7fff7b2ab7d4a8512584b8b19f341a40e542619b5738f5e]):01 01 a2efb5157db6b94a4a3c74aef3a7e5c6bd7d1380425c876a6d7eeba258d10d8b):01 :[01 8d3ff2077fb1939799436fe53b2eb86f364443de8f7744b47a2d1c83aa51c856]) = 79820647d185b7a09055290814b48f2d7b8ea620c6601e32319cfb02d024947b

Here the statement has been arranged in a way that makes both crum and the root of the Merkle tree it's in readily apparent to the user. The crum itself is in the first few literals

> 7f7f5d03de1e5e2c8a2a358d8d70238a93b16df85a15e01cbd08b8c671133e77 000001730a3f7e7b

(the operand of the first hash); the root hash of the Merkle tree (the thing we have a reliable record of) is at the end of the statement. There's a good argument to swap the left- and righthand sides of the equal sign above: that way, *all* the byte literals a human is interested in occur at the beginning of the statement. TBD.


## Repo

The repo for this project is located [here](https://github.com/crums-io/crums-pub).

## API

The Java [API](./api/index.html) complements the [REST API](https://crums.io/docs/rest.html). More tools are forthcoming.

~ 6 Feb 2021

