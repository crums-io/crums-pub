# crum User Guide (ALPHA)

`crum` is a command line program for generating key-less, cryptographic
timestamps on remote timechains and archiving them in a local repo.
This guide is meant to compliment information available thru the `help` command.

## peek Command

Looks up the timechain at the given URL and prints its properties and
current block no. after validating its latest block proof. (It's actually
2 REST calls to the remote server.) Here's the response against a
test timechain:

>   
    $ /opt/crum/bin/crum peek http://localhost:8080

    Inception:         2024-10-07T15:18:30.912-0600
    Block rate:        every 2.048 seconds
    Block commit lag:  1.024 seconds
    Blocks retained:   64
    Blocks searched:   3
    Current block:     1940432
    Blocks committed:  1940430
    $

We'll use this example to explain what these statistics mean..

### Inception

This is the date of the timechain's genesis block. It marks the smallest
UTC value that could fall in that first time block. Note the genesis block
is numbered 1, not 0.

### Block rate

The block rate together with the inception time (above) defines a mapping for
any UTC (after the chain's inception) to a chain block no.

Note this figure actually expresses each block's *duration*, not the rate
(which is actually the reciprocal of this figure). The word *rate* in this
command line interface is being used loosely. As for the real rate a timechain
generates new blocks, it is expected to fluctuate over short stretches, but
be very accurate over longer stretches (e.g. the moving average over 100 blocks).

The block rate is set at the chain's inception and predetermines
the span of time each block no. represents. Specifically, 
a block numbered `block_no` spans an interval of time beginning at
>   
    inception_utc + (block_no - 1) x (block_duration)
(inclusive), and ending at
>   
    inception_utc + (block_no) x (block_duration)
(exclusive).

Notice, this figure, expressed in milliseconds is a power of 2
(2048 in the example above). It is always that way: the
block rate is expressed as the *exponent* this power of 2 takes
(the `binExponent` in the server's JSON).


### Block commit lag

Block (no.s) become *eligible* for addition (commitment) only after
the end of the block plus this many seconds.

### Blocks retained

A timechain doesn't keep the witness receipts it generates (crumtrails) indefinitely.
Once a witnessed hash is committed to a timechain block, it survives this
many new blocks before it's purged. Clients are expected to retrieve
their receipts in this window of time from the server.

In the example above, this translates to a TTL (after block commit) of
>   
    64 x 2.048s
or, about 131 seconds.

### Blocks searched

Whenever a SHA-256 hash is submitted to the remote chain it creates an
entry in that block (technically, a cargo block). But if an entry with
that hash already exists in that block, then the server returns the
existing entry instead. So at minimum, a timechain server searches 1
block before returning the receipt. This figure says how many blocks
are actually searched (typically 3).

### Current block

This translates (local) system time to the chain's current block no.
Recall, this mapping is defined solely by the chain's inception time and
block rate.

### Blocks commited

This is the last block no. presently in the timechain. This figure is
derived from timechain's latest block proof (retrieved thru a 2nd call
to the server). Typically, this no. should be no more than 2 or 3 behind
the current block no. (above).

## Local repos

Most `crum` commands interact a local repo (all but the `peek` command
in the current release). Every local repo supports archiving information
and recording *crumtrails* (witness receipts) from *multiple* timechains.
Each local repo maintains information about each timechain in indepedent
sub-repos called *chain repos*.

Crumtrails from the same timechain share common (hash) information and
it is advantageous to manage them together. Crumtrails
take collectively less disk space in a repo than they do individually.
But more importantly, every time `crum` drops a new (just witnessed)
crumtrail in the repo, it automatically also updates the block proofs for
every existing crumtrail in the repo (for that timechain).

Repos do not index crumtrails by date: crumtrails are only indexed by
hashes (lexicographic order of hash witnessed).

### About Hostnames

Local repos maintain alias names for the remote timechains they know about.
Presently, these aliases are just the timechain's hostname. So, unless hostnames
are aliased, `crum` recognizes at most one timechain per host (per repo). The
plan is for future versions of `crum` to support this "aliasing" internally;
for now you should know about this limitation.

#### Default host

Every repo optionally defines a default timechain host. If the repo only knows
about a single timechain, then that timechain's hostname is the the default
host.

### Default repo

Every `crum` user has (or potentially has) a default repo in their home directory
named `.crums`.

## wit

Creating a timestamp (crumtrail) is a 2 step process. You first drop the hash to the
timechain using this command and receive a receipt. You then *wait* for the (time) block
in which your hash was witnessed to commit on the timechain. Finally, you retrieve
the "cured" crumtrail from the timechain using the [seal](#seal) command.
(That's 3 steps, if we count waiting as a step.) On a "fast" timechains,
`crum` performs these steps automatically. When it doesn't (either because
the `--no-wait` option is used, or because the timechain is too "slow" for
the default wait behavior to kick in), you'll have to follow up with a `seal` command.

The first time you run this command, it might look like something like below
run against a test server..

>   
    $ crum wit --origin http://localhost:8080 -F myFile.md 
    Initializing repo chain for http://localhost:8080
    [WIT]    63eed3e18d4407595608d613a0fb71a6dc7363fc7d653d6c4a6c82de5942b093
             2024-11-22T22:56:46.227-0700   (Block 1955809)
             sealable in about 4 seconds
    waiting..
    sealing trails..
    [SEALED] 63eed3e18d4407595608d613a0fb71a6dc7363fc7d653d6c4a6c82de5942b093
             2024-11-22T22:56:46.227-0700   (Block 1955809)
    $ 

`crum` creates a [default user repo](#default-repo) and stores a crumtrail
in it. Since `example.crums.io` is the only timechain server the repo knows about,
it is for now, the repo's [default host](#default-host). From here on, if you
omit the `--origin <URL>` option with this command, `crum` will assume you meant the
timechain at the same URL.

Note, `crum` will not witness a hash on a timechain, if the local chain repo already
contains a crumtrail for that hash.


## seal

When a timechain witnesses a hash thru the `wit` command, a temporary receipt
is first saved in the repo. It contains the UTC time it was witnessed at the server
(and which block no. it is to be committed to). After waiting for that block no. to
commit, the crumtrail (witness proof) for that hash is ready at the server and
may be retrieved using this command.

On success, this both records new crumtrails in the chain repo, and also *patches*
every crumtrail in that chain repo to the latest state (block) of the timechain.

This patching behavior is a free side-effect of witnessing stuff.

## patch

You don't have to `wit` (and `seal`) in order to bring the local state of a
chain repo forward to the present. Indeed, if there is nothing new to witness,
then this command is way more efficient, both in (disk) space and time (single,
read-only, REST "state" call).

The state of a timechain is meant to be distributed through the block proofs
it has dispensed. Tho you can always delay recording the (near) current state
of a timechain (relying on its REST network interface), it's always a good idea
to keep chain operators honest by monitoring their chain state.

## find

The option to emit JSON arguably does not serve any useful purpose in this first version.
The next version will support *ingesting* (recording) crumtrails in a local repo,
directly from JSON input. This is intended to make crumtrails easy to share with others.



