Time Chain 2 Design Doc
=======================

# Background
The first version of the crums.io time chain (called Time Chain 1, posthumously)
consisted of a sequence of Merkle trees each strung to its predecessor by referencing
the previous tree's root hash in the last item of the successor tree. While this linking
made the time chain auditable, verifying the entire chain necessarily involved
verifying the Merkle proof for every link in the chain. Like every chain out there,
you had to verify *all* its blocks (each here a simple Merkle tree) in order to verify
its entirety. At the time, I took this property of verifying a chain (its being
computationally linear in the number of blocks the chain has) as a given. But then
there was a development.

## Skip Ledger

Following TC-1's release, I developed a library for calculating the cryptographic
hash of append-only (but otherwise immutable) lists for timestamping / witnesssing
purposes. It introduces a
new data structure called a [skip ledger](https://github.com/crums-io/skipledger),
from which the hash of successive items
(rows in a ledger) are computed. This way of computing hashes supports a
*commitment* protocol in which

- The hash of row (item) no. *r* also encodes the state of the ledger (list) containing
only its first *r* items.
- The declaration of the hash of row no. *r* amounts to a *commitment* to the contents
of the rows before it.
- There's a compact hash proof showing whether the contents of any row numbered less
than *r* belongs in the ledger with advertised *r*<sup>*th*</sup> row hash. The byte size
of the proof scales as the logarithm of *r*.
- The hash proof linking the hash of the first and last rows (includes the hash of a few
rows in between) serves as rich fingerprint encoding both the ledger's hash, as well as
how many rows are in the ledger. In skip ledger terminology, such proofs are called
*state* proofs.
- As the ledger grows, its new state proofs can be verified against its old state proofs.

As I was developing the skip ledger library, I kept noting to myself how employing this
data structure in the crums.io time chain itself could provide significant benefits.

# Motivation

While crums.io's backend time chain is implemented as a distributed service, it is
not a *decentralized* service. It doesn't need to be verified in a decentralized
manner because the chain's content (the block contents sans their linking mechanism) is opaque to all but its users, and all that the chain records are hashes (and when they were witnessed): there's little room for adverserial dynamics here. The time chain is like a
block chain that has no transaction cargo to verify. From a user's perspective, the only
requirement for the chain is that it be provably immutable and that it contain sufficient
information that any given witness receipt can be linked back to the block
in the time chain it belongs to.

With these considerations in mind, employing the skip ledger data structure as the
block linking mechanism for TC-2 offers a number of benefits.

## Self-verification

To the degree feasible every piece of data should be self verifying. This is a general
design objective, both on- and off-chain.

### State Declarations

In TC-2, as in TC-1, the hash of the last block encodes the state of the entire chain.
Neither TC-1, nor the proposed TC-2, are decentralized. In TC-1 the only way to verify
the hash of the last block was to replay all the Merkle link proofs from beginning to end.
And again, since the chain was not expected to be replicated (not decentralized), crums.io
was the only practical source were the state of the chain could be verified. (For audit, purposes TC-1 *did backup* at 3rd party data stores, but these were not documented
access methods.)

With TC-1, the hash of the last block, together with its number,
were periodically posted at 3rd party web sites
like Twitter. The posts' timestamps were meant as a means to verify the service was honest
about its bookkeeping. One could write a script to do this verification, but it would've
been cumbersome and a bit taxing, on both user and the service.

In TC-2 too, the state of the chain is periodically posted as timestamped artifacts
at 3rd party websites. But because the state is encoded in a self-contained,
self-verifying, [skip ledger] state proof, users won't need to check the provenance
of the hashes at the source (crums.io).

And since new state proofs can be verified against old state proofs, the evolution
of the state of TC-2 can independently be tracked by monitoring the chain's state proofs
on whatever 3rd party site they're posted on.

### Witness Proofs

Using the same off-chain methods that allow one to verify 2 state proofs belong
to the same evolving chain, old witness proofs can easily be patched (and verified)
against the chain's latest blocks.

### Storage

The long term storage overhead for maintaining the TC-2 chain is significantly less
than that for TC-1. Like its predecessor TC-2 relies on the user's saving their witnessed
hash receipts on the client side: it is designed to save as little as possible on the
chain itself. (The witness records are cached at the service for only a limited time.)

This storage savings in turn allows the TC-2 time chain to more easily (more efficiently)
churn out new blocks at faster rates. Indeed it should be possible to commit a new
witnessed hash to the chain in under a fraction of a minute.

Further storage savings are possible by limiting the blocks stored to only
those with block numbers that are a multiple of some fixed power of 2. For example,
the chain may only record every 16<sup>th</sup> block
in its long term storage, with the onus of recording the hash of intermediate blocks
(before being purged from the chain) on the owner of the witness proof.


# Block Structure

As indicated above, each block in TC-2 is encoded as an immutable skip ledger row.
Blocks are thus numberered consecutively, and each numbered block encodes hashes
that were witnessed in a fixed span of time.

## Cargo Data

Ignoring the hashpointers linking the blocks together, each block contains the
*hash of* a set of crums. That single hash is all that's recorded in the time
chain permanently. Recall a crum consists of
the submitted hash, and the time it was witnessed (in UTC milliseconds).
The hash of a single crum is just the hash of the concatenation of these 2 values.
If multiple crums are in the block, then the cargo hash is a Merkle root
of the set's crums; the hash of the empty set, by convention, is encoded as a sequence
of zeroed bytes.

## Block Hashpointers

The chain's blocks are threaded via hashpointers to previous blocks. The number of
these pointers is uniquely determined by the block number (1 plus the number of times
the block number is divisible by 2). The values of these hashpointers, however, are not
directly written into the block (their values are looked up in the previous block no.s
they point to); rather, it is the computed hash of the block itself that gets written to
the block. (The hash of the block is computed as the hash of the concatenation of the
cargo hash with the hash of blocks it "points" to.)


## Ballpark Calculations

64 bytes per block

    32      cargo hash (hash of witnessed crums)
    32      block hash
    ---
    64 bytes

Minutes in a year: 525,600

If the chain generates a new block every 8 seconds (7 or 8 new blocks per minute),
it storage overhead per year will be less than
    
    525,600 x 64 x 8 ~ 269Mb

So less than 1/4 Gb/yr. Quite reasonable.

# Witness Proofs (Crumtrails)

In TC-1 each witness proof (saved on the client side) linked both the hash
and the UTC time it was witnessed to the root hash of the Merkle tree for that block
(via a Merkle proof). Under TC-2 each crumtrail is augmented with a hash proof
that the Merkle tree (the cargo) belongs to a specific block number of the chain.






