/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.cli.crum;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import io.crums.tc.Constants;
import io.crums.tc.NotaryPolicy;
import io.crums.tc.Receipt;
import io.crums.tc.client.Client;
import io.crums.tc.client.RemoteChain;
import io.crums.tc.client.Repo;
import io.crums.util.IntegralStrings;
import io.crums.util.Base64_32;
import io.crums.util.Strings;


import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.ArgGroup;


/**
 * Main launch class.
 * 
 * TODO commands
 * 
 * list hosts
 * host info
 * peek host
 * witness, auto-wait for seal option
 * retrieve crum trail
 * chain repo update (w/o witnessing anything new)
 * 
 */
@Command(
  name = Main.NAME,
  mixinStandardHelpOptions = true,
  version = "crum 0.1.0",
  description = {
    "Records, archives, and retrieves SHA-256 timestamps proving within",
    "reasonable bounds when a hash was witnessed.",
  },
  subcommands = {
    HelpCommand.class,
    Witness.class,
    Seal.class,
  }
)
public class Main {

  final static String NAME = "crum";



  public static void main(String[] args) throws IOException {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }



  static void printf(String format, Object... args) {
    // process string markups with ANSI escape codes
    for (int index = args.length; index-- > 0; ) {
      if (args[index] instanceof String arg)
        args[index] = Ansi.AUTO.string(arg);
    }
    System.out.printf(format, args);
  }



  static void printSealed(Receipt rcpt) {
    Main.printf("[%s] %s%n", "@|bold,green SEALED|@", rcpt.crum().hashHex());
    Main.printf(
        // "%8s %s%n", "", dateFormatter().format(new Date(rcpt.crum().utc())));
        "%8s %s%n", "", ansiDateCode(rcpt.crum().utc()));
  }


  static void printAck(Receipt rcpt, NotaryPolicy policy) {
    Main.printf("[%s] %s%n", "@|bold,blue WITNESSED|@", rcpt.crum().hashHex());
    
    Main.printf("%11s sealable in about %s%n",
        "", Strings.nOf(sealInSeconds(rcpt, policy), "second"));
  }


  private static DateFormat dateFormatter() {
    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
  }


  static String ansiDateCode(long utc) {
    String plain = dateFormatter().format(new Date(utc));
    int dash = plain.indexOf('-') + 1;  // one beyond
    int t = plain.indexOf('T');
    int dot = plain.lastIndexOf('.');
    int zone = dot + 4;
    return
        "@|faint " + plain.substring(0, dash) + "|@" +
        plain.substring(dash, t) + "@|faint T|@" + plain.substring(t + 1, dot) +
        "@|faint " + plain.substring(dot, zone) + "|@@|faint,yellow " +
        plain.substring(zone) + "|@";
  }

  private static long sealInSeconds(Receipt rcpt, NotaryPolicy policy) {
    
    long ripeUtc =
        rcpt.chainParams().utcForBlockNo(rcpt.blockNo() + 1) +
        policy.blockCommitLag();

    return Math.max(0L,
        (ripeUtc - System.currentTimeMillis())/ 1000);
  }


  static void printChainInfo(Repo repo, String host) {
    var opt = repo.findChainRepo(host);
    if (!opt.isPresent())
      return;
    Repo.ChainRepo chainRepo = opt.get();
    printf("%s:%n", "@|bold " + host + "|@");
    printf("  Origin: %sn", chainRepo.origin());
    var policy = chainRepo.policy();
    var chainParams = policy.chainParams();
    printf("  Inception: %s%n", ansiDateCode(chainParams.inceptionUtc()));
    printf(
        "  Block Duration: %.3f seconds%n",
        chainParams.blockDuration() / 1000.0);
  }

}



class RepoRoot {
  
  @Spec
  CommandSpec spec;
  
  private File rootDir;
  
  @Option(
      names = { "--repo" },
      paramLabel = "PATH",
      description = {
        "Location of repo root directory",
        "Default: $HOME/.crums"
      },
      required = false
  )
  public void setRootDir(File dir) {
    if (dir.isFile())
      throw new ParameterException(
          spec.commandLine(), "cannot overwrite file: " + dir);
    this.rootDir = dir;
  }
  
  
  public Optional<File> rootDir() {
    return Optional.ofNullable(rootDir);
  }
  
}


class HashOpt {

  @Spec
  CommandSpec spec;

  File file;

  @Option(
    names = { "--file", "-F" },
    paramLabel = "FILE",
    description = "Sets hash to computed SHA-256 of FILE",
    required = true
  )
  void setFile(File file) {
    this.file = file;
    if (!file.isFile())
      throw new ParameterException(spec.commandLine(), "not a file: " + file);
    if (file.length() == 0)
      throw new ParameterException(spec.commandLine(), "empty file: " + file);
  }


  byte[] hash;


  @Option(
    names = { "--hash", "-H" },
    paramLabel = "SHA-256",
    description = {
      "SHA-256 hash expressed in hex or base64/32",
      "Exactly 64 digits in hex, or 43 digits b64"
    }
  )
  void setHash(String hash) {
    
    Exception err;
    try {
      this.hash = switch (hash.length()) {
        case 43 ->  Base64_32.decode(hash);
        case 64 ->  IntegralStrings.hexToBytes(hash);
        default -> null;
      };
      err = null;
    } catch (Exception x) { err = x; }

    if (this.hash == null) {
      var msg = "illegal hash";
      if (err == null)
        msg += " (length=" + hash.length() + "): " + hash;
      else
        msg += ": " + hash + " -- Detail: " + err.getMessage();
      throw new ParameterException(spec.commandLine(), msg);
    }
  }


}


class Origin {

  @Spec
  CommandSpec spec;

  @Option(
    names = { "--host" },
    paramLabel = "HOSTNAME",
    description = {
      "Timechain notary service identified by hostname only",
      "If not specified, then a @|italic default|@ host must",
      "be defined in the repo used."
    }
  )
  String hostname;

  
  URI orginUri;


  @Option(
    names = { "--origin" },
    paramLabel = "URL",
    description = {
      "Timechain notary service identified by host URL",
      "Presently, at most 1 timechain per hostname is modeled",
      "(a workaround is to define aliases). Examples:",
      "",
      "  http://127.0.0.1:8080",
      "  http://localhost:8080",
      "  https://example.crums.io",
      "",
      "Note hostnames are not resolved, so the first 2 examples",
      "generate independent sub-repos."
    }
  )
  void setOriginUri(String origin) {
    var uri = origin.endsWith("/") ?
        origin.substring(0, origin.length() - 1) :
        origin;
    try {
      orginUri = new URI(uri.toLowerCase());
      RemoteChain.checkHostUri(orginUri);
    } catch (Exception err) {
      throw new ParameterException(spec.commandLine(),
          "illegal origin URL: " + origin + " -- Detail: " + err.getMessage());

    }
  }


  boolean isSet() {
    return hostname != null || orginUri != null;
  }
  

}



class SealNow {


  @Option(
    names = { "--wait", "-w"},
    description = {
    "Wait for timechain commit",
    "Default: triggered automatically if timechain blocks span less 4.1 seconds",
    }
  )
  boolean waitAndSeal;

  @Option(
    names = { "--nowait", "-n"},
    description = {
    "Don't wait for timechain commit (overrides default waiting behavior)"
    }
  )
  boolean dontWait;

}


@Command(
  name = "wit",
  description = {
    "Witness a SHA-256 hash",
    "The hash is @|italic noted|@ at the timechain notary and an ephemeral receipt",
    "(a crum) is saved.",
    "",
    "See also @|bold seal|@",
    ""
  }
)
class Witness implements Runnable {

  
  @Mixin
  RepoRoot repoRoot;

  // @Mixin
  @ArgGroup(exclusive = true, multiplicity="0..1")
  Origin origin;

  private boolean originIsSet() {
    return origin != null && origin.isSet();
  }

  
  @Spec
  CommandSpec spec;



  @ArgGroup(exclusive = true, multiplicity="1")
  HashOpt hashOpt;

  @ArgGroup(exclusive = true, multiplicity="0..1")
  SealNow sealNow;


  @Override
  public void run() {
    File repoDir = repoRoot.rootDir().orElse(Repo.defaultUserRepoPath());
    if (!repoDir.exists() && !originIsSet())
      throw new ParameterException(spec.commandLine(),
          "--origin URL must be set when creating new a repo" );
    
    Client client = new Client(new Repo(repoDir));

    byte[] hash;
    if (hashOpt.hash != null)
      hash = hashOpt.hash;
    else if (hashOpt.file != null)
      hash = computeFileHash(hashOpt.file);
    else
      throw new AssertionError("neither -H or -F options set");

    if (!originIsSet() && client.getDefaultHost().isEmpty())
      throw new ParameterException(spec.commandLine(),
          "undefined default origin in repo " +
          client.repoDir() +
          "\n either --host or --origin must be given");

    
    NotaryPolicy policy;
    String host;

    if (originIsSet()) {

      if (origin.hostname != null) {
        policy = client.getPolicy(origin.hostname).orElseThrow(
            () -> new ParameterException(spec.commandLine(),
                "host " + origin.hostname + " is unknown to repo "+ client.repoDir() +
                "\neither --host or --origin must be specified"));
        host = origin.hostname;

      } else {
        policy = client.init(origin.orginUri);
        host = origin.orginUri.getHost();
      }

    } else {

      host = client.getDefaultHost().orElseThrow(
          () -> new ParameterException(spec.commandLine(),
          "default host is undefined in repo " + client.repoDir() +
          "\nspecify either --host or --origin"));
      policy = client.getPolicy(host).get();
    }

    final var bhash = ByteBuffer.wrap(hash);

    {
      var crumtrail = client.findTrail(bhash, host);
      if (crumtrail.isPresent()) {
        var trail = crumtrail.get();
        Main.printf(
            "Already sealed and notarized in block [%s] %s%n",
            "@|bold " + trail.blockNo() + "|@",
            Main.ansiDateCode(trail.crum().utc()));
        return;
      }
    }

    final boolean init = !client.listChainHosts().contains(host);
    if (init)
      Main.printf(
          "Initializing repo chain host", "@|bold " + origin.orginUri + "|@");

    var rcpt = client.witness(bhash.clear(), host);


    if (rcpt.hasTrail())
      Main.printSealed(rcpt);
    else {
      Main.printAck(rcpt, policy);
    }
  }


  private long sealInSeconds(Receipt rcpt, NotaryPolicy policy) {
    
    long ripeUtc =
        rcpt.chainParams().utcForBlockNo(rcpt.blockNo() + 1) +
        policy.blockCommitLag();

    return Math.max(0L,
        (ripeUtc - System.currentTimeMillis())/ 1000);
  }






  private byte[] computeFileHash(File file) throws UncheckedIOException {
    final long len = file.length();
    if (len <= 0)
      throw new ParameterException(spec.commandLine(),
        "bad filepath " + file);
    var buffer = ByteBuffer.allocate((int) Math.min(len, 64 * 1024));
    var digester = Constants.DIGEST.newDigest();
    try (var ch = new FileInputStream(file).getChannel()) {
      while (ch.read(buffer) != -1) {
        if (buffer.flip().hasRemaining())
          digester.update(buffer);
        buffer.clear();
      }
    } catch (IOException iox) {
      throw new UncheckedIOException("on reading " + file, iox);
    }
    return digester.digest();
  }


}



@Command(
  name = "seal",
  description = {
    "Retrieve and save permanent witness proofs",
    "",
    "See also @|bold wit|@"
  }
)
class Seal implements Runnable {
  
  
  @Spec
  CommandSpec spec;

  @Mixin
  RepoRoot repoRoot;


  @Override
  public void run() {
    File repoDir = repoRoot.rootDir().orElse(Repo.defaultUserRepoPath());
    
    if (!repoDir.exists()) {
      var msg = "repo not found at ";
      if (repoRoot.rootDir().isEmpty())
        msg += "default user directory ";
      msg += repoDir;
      throw new ParameterException(spec.commandLine(), msg);
    }

    Repo repo = new Repo(repoDir);

    var pending = repo.pendingByHost();
    if (pending.isEmpty()) {
      System.out.println("Up to date. Nothing to do.");
      return;
    }
    boolean multihost = pending.size() > 1;
    Client client = new Client(repo);

    // track how many crums remain to be trailed
    int pendingTally = 0;
    int tally = 0;

    for (var host : pending.keySet()) {
      int count = pending.get(host).size();
      Main.printf(
        "sealing %s from timechain %s%n  ..%n",
        Strings.nOf(count, "crum trail"),
        "@|bold " + host + "|@");
      var rcpts = client.updatePending(host);
      tally += rcpts.size();
      // how many crums remain to be trailed from this host
      int remaining = 0;
      for (var rcpt : rcpts) {
        if (rcpt.hasTrail()) {
          Main.printSealed(rcpt);
        } else {
          Main.printAck(rcpt, client.getPolicy(host).get());
          ++remaining;
        }
      }

      pendingTally += remaining;

      if (remaining > 0) {
        Main.printf(
          "%n%s sealed, %s remaining%n%n",
          Strings.nOf(rcpts.size() - remaining, "crum trail"),
          Strings.nOf(remaining, "crum"));
      } else {
        Main.printf("%n");
      }
    }

    if (multihost) {
      Main.printf(
        "Recap: %s sealed, %s remaining%n",
        Strings.nOf(tally - pendingTally, "crum trail"),
        Strings.nOf(pendingTally, "crum"));
    }

  }

}









