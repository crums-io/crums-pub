![](./logo.png)
# `ergd` User Guide (ALPHA)

`ergd` is a command line program for incepting and launching a timechain service.
This guide is meant to compliment information available thru the `help` command.

#### About the name

Ergo daemon.

## `--demo` option

Use this option to get a better sense of how the API works. This provides a web UI
(static reactive HTML) that uses the API and demo's its use (as for example at crums.io).
Presently, this is only for testing purposes. (Static pages ought to be served some other way).

## `incept` Command

Incepts a *new* timechain in a target path and launches the server immediately.
A number of parameters can be set at inception (see `ergd help incept`).

### -b --binExponent

This required option parameter controls the rate at which the timechain generates new blocks.
Each block in the timechain spans 2 to the power of this many milliseconds.
The lower this number is, the more blocks the chain produces, the quicker the
time to commit. Though each block takes only 64 bytes, a long-lived chain
accumulates disk space over time. Here's a table to aid back-of-envelop
calculations for analyzing the tradeoffs. As a rule of thumb, the worst-case
time-to-commit is slightly more than the block duration.

| bin exponent | Block Duration | Blocks per yr | MBs per yr |
| :---: | --- | --- | --- |
| 10 | 1.024 s | 30796875 | 1880 |
| 11 | 2.048 s | 15398438 | 940 |
| 12 | 4.096 s | 7699219 | 470 |
| 13 | 8.192 s | 3849609 | 235 |
| 14 | 16.384 s | 1924805 | 117 |
| 15 | 32.768 s | 962402 | 58 |
| 16 | 65.536 s | 481201 | 29 |
| 17 | 2m:11s | 240600 | 15 |
| 18 | 4m:22s | 120300 | 7.3 |
| 19 | 8m:44s | 60150 | 3.7 |
| 20 | 17m:29s | 30075 | 1.8 |
| 21 | 34m:57s | 15037 | 940kB |


If a timechain's clients span the geographic globe bin exponents
below 11 (block durations less than 2 seconds) are not recommended.

This parameter is *fixed at inception*.

### -r --blocksRetained

This option controls how long built crumtrails (receipts) remain in the timechain's
ephemeral buffer (called the cargo chain). The larger this no., the longer a
client has to retrieve a receipt once its committed. The system is not designed
to have a large buffer. It defaults to 64 blocks. In real time, this amounts
to 64 x the block duration (in turn, determined by the chain's bin exponent--see above).

## `run` Command

Launches a timechain REST server from an existing timechain directory.
There are 2 scenarios in which a timechain is launched:

### "Restart" Scenario

The timechain directory was dormant: compared to system time,
the timechain had missing [time] blocks at start time. In this scenario, those
missing blocks are first appended to the chain as empty blocks, before new [time]
blocks are eligible to be committed. This being the first release, "restart" is
the usual scenario.

### "Join" Scenario

The timechain directory is already being "serviced" by another running instance of
`ergd`. The other instance may be executing on the same machine (but listening in on
a different port no.), or the instance may be executing on a different machine
with a shared network mount point for the timechain directory. This is designed to
support both scaling load, and upgrades (without bringing the service down).

## Stopping

There is no stop command. The server is stopped via the kill signal (Ctrl-C
in most terminals), or by any other means that kills the process (but not the file
system the timechain is mounted on).

## About Zeroes

When a timechain is restarted it uses the "sentinel", zeroed hash as input
(called cargo hash) for the missing [time] blocks (a "nothing-up-my-sleeve"
thing). These show up as zeroes in the chain's block proofs. There's nothing
wrong with such chains: whether a chain is always up or not, is a
quality-of-service / type-of-service issue.

## Entropy Daemon

There is a daemon thread that drops secure, pseudo-random SHA-256 hashes
to be witnessed, roughly twice per block. A chain gathers entropy with use:
the entropy daemon is meant as a stop-gap mechanism for the chain to gather entropy during periods of user inactivity.

Note there are actually 2 sources of entropy for each hash dropped by the daemon:
the pseudo-random hash itself, and the UTC time it was dropped in. The latter
factor is influenced by a myriad of other processes running on the same machine
(or virtual machines). (Future versions, will support an opt-in to gather entropy
using state from other timechains.)

## About Hostnames

Presently, in the multi-chain repos it maintains, the client CLI `crum` identifies
timechains only by their hostnames. So, unless hostnames are
aliased, `crum` recognizes at most one timechain per host (per repo). The plan
is for future versions of `crum` to support this "aliasing" internally;
for now you should know about this limitation.


