# Notary Module

The notary module is can be thought of the timechain engine.
It is responsible for managing the ephemeral (conveyer belt)
cargo chain and committing the hash of cargo blocks to the
timechain.

The library leans entirely on the file system to implement
atomic, durable, concurrent operations. In particular, it leans on the
following properties of the file system:

1. File moves (renaming) are all-or-nothing.
2. File lengths (in bytes) are never more than the last byte written.

These properties are not just used for durability.
The library handles concurrency at the *process* level
using its file naming strategies. Since commits from a cargo block
to its corresponding timechain occur well after new crums are added
to the cargo block, these operations occur deterministically. That is,
if 2 running instances of this module attempt to commit the same cargo
block, their commits are idempotent (because they write the same bytes
at the same offset in the timechain).

Certain very corner case races are treated as acceptable, such as
when 2 clients race to witness the same hash in the same block of time
(in such a case, one client might see the witness time jump backwards,
but still in the same time-block).

