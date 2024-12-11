/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.server.main;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;

import com.sun.net.httpserver.HttpServer;

import io.crums.tc.ChainParams;
import io.crums.tc.Constants;
import io.crums.tc.NotaryPolicy;
import io.crums.tc.TimeBinner;
import io.crums.tc.notary.Notary;
import io.crums.tc.notary.NotaryLog;
import io.crums.tc.notary.NotarySettings;
import io.crums.tc.notary.d.NotaryD;
import io.crums.tc.notary.server.NotaryLogger;
import io.crums.tc.notary.server.UriHandler;
import io.crums.util.TaskStack;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

/**
 * Main launch class.
 */
@Command(
    name = Ergd.NAME,
    mixinStandardHelpOptions = true,
    version = Ergd.VERSION,
    description = {
      "",
      "Timechain notary REST service configuration and launcher.",
      "See also",
      "    https://crums-io.github.io/timechain/ergd_guide.html%n"
    },
    subcommands = {
        HelpCommand.class,
        Run.class,
        Incept.class
    })
public class Ergd {

  final static String NAME = "ergd";
  final static String VERSION = NAME + " " + Constants.VERSION;

  private final Object lock = new Object();

  private boolean shutdown;

  public Ergd() throws IOException {
    try (InputStream confStream = getClass().getResourceAsStream("/logging.properties")) {
      LogManager.getLogManager().readConfiguration(confStream);
    }
  }

  public void awaitShutdown() throws InterruptedException {
    synchronized (lock) {
      while (!shutdown) {
        lock.wait();
      }
    }
  }


  public NotaryLog log() {
    return new NotaryLogger(NAME);
  }


  private void signalShutdown() {
    synchronized (lock) {
      shutdown = true;
      lock.notifyAll();
    }
  }
  
  
  public static void main(String[] args) throws IOException {
    int exitCode = new CommandLine(new Ergd()).execute(args);
    System.exit(exitCode);
  }
  
  
  void launch(Notary notary, int port, boolean withUi) throws IOException, InterruptedException {
    
    log().info("Launching REST server listening on port " + port);
    try (var onFail = new TaskStack()) {
      
      final var fin = new TaskStack();

      fin.pushRun(this::signalShutdown);
      
      onFail.pushClose(notary);

      var notaryD = new NotaryD(notary);
      onFail.pushClose(notaryD);
      fin.pushClose(notaryD);

      var server = HttpServer.create(new InetSocketAddress(port), 0);

      for (var uh : UriHandler.all(notary, withUi))
        server.createContext(uh.uri(), uh.handler());
      
      var es = Executors.newVirtualThreadPerTaskExecutor();
      onFail.pushClose(es);
      fin.pushClose(es);

      server.setExecutor(es);
      server.start();

      Runtime.getRuntime().addShutdownHook(
        new Thread(Ergd.class.getSimpleName() + ".shutdown-hook") {
          @Override
          public void run() {
            try (fin) {
              server.stop(0);
            }
          }
        } );
      
      
      onFail.clear();
    }

    log().info(
      "\n Ergd launched.. Signal CTRL-C to stop.\n" +
      " (or you may kill this process by any means; abnormal shutdown OK)\n");
    awaitShutdown();
    
  }
  

}


class ReqRootDir {
  
  @Spec
  CommandSpec spec;
  
  private File rootDir;
  
  @Option(
      names = { "--dir", "-d" },
      paramLabel = "PATH",
      description = "Root directory of the notary / timechain",
      required = true
      )
  public void setRootDir(File dir) {
    if (dir.isFile())
      throw new ParameterException(
          spec.commandLine(), "cannot overwrite file: " + dir);
    this.rootDir = dir;
  }
  
  
  public File rootDir() {
    return rootDir;
  }
  
}


class PortOpt {
  @Spec
  CommandSpec spec;
  
  
  private int port = 8080;
  
  
  
  @Option(
    names = { "--port", "-p" },
    paramLabel = "PORT",
    description = {
      "Port no. REST service listens on",
      "Default: 8080"
    }
  )
  public void setPort(int port) {
    if (port <= 0 || port > Short.MAX_VALUE)
      throw new ParameterException(
          spec.commandLine(), "out-of-bounds: --port " + port);
    this.port = port;
  }
  
  
  public int no() {
    return port;
  }
  
}


class DemoOpt {

  @Option(
    names = "--demo",
    description = "Include demo website (for testing only)"
  )
  boolean demo;
}


@Command(
    name = "incept",
    description = {
        "Incepts a new notary in the given directory path",
        "and immediately starts it."
    }
    )
class Incept implements Callable<Integer> {
  
  /** About a 1/4 second. */
  public final static int MIN_EXP = 8;
  /** About 18h:38m. */
  public final static int MAX_EXP = TimeBinner.MAX_EXP;
  
  @ParentCommand
  private Ergd ergd;
  
  @Spec
  CommandSpec spec;
  
  
  @Mixin
  private ReqRootDir dirOpt;
  
  
  
  private int binExponent;
  
  
  
  @Option(
      names = {
          "--binExponent",
          "-b"
      },
      paramLabel = "EXP",
      description = {
          "Sets the time boundary and duration of each block",
          "Duration: 2^EXP milliseconds",
          "Examples:",
          "  13  ->   8.192s",
          "  16  ->   1m:06s",
          "  18  ->   4m:56s",
          "Valid range: [8-26] (inclusive)",
      },
      required = true
      )
  public void setBinExponent(int binExponent) {
    if (binExponent < MIN_EXP || binExponent > MAX_EXP)
      throw new ParameterException(
          spec.commandLine(),
          "out-of-bounds: --binExponent " + binExponent);
    this.binExponent = binExponent;
  }
  
  
  
  
  
  
  
  
  
  private int blocksSearched = MIN_BLOCKS_SEARCHED;
  
  private final static int MIN_BLOCKS_SEARCHED =
      NotaryPolicy.DEFAULT_BLOCKS_SEARCHED;
  
  @Option(
      names = {
          "--blocksSearched",
          "-s"
      },
      paramLabel = "BLOCKS",
      description = {
          "No. of cargo blocks searched on witnessing a hash",
          "Minimum: 3",
          "Default: 3"
      }
      )
  public void setBlocksSearched(int blocksSearched) {
    if (blocksSearched < MIN_BLOCKS_SEARCHED)
      throw new ParameterException(
          spec.commandLine(),
          "out-of-bounds: --blocksSearched " + blocksSearched);
    
    this.blocksSearched = blocksSearched;
  }
  
  
  private final static int MIN_BLOCKS_RETAINED = 3;
  
  
  private int blocksRetained = 64;
  
  
  @Option(
      names = {
          "--blocksRetained",
          "-r",
      },
      paramLabel = "BLOCKS",
      description = {
          "No. of committed cargo blocks retained",
          "Minimum: 3",
          "Default: 64"
      }
      )
  public void setBlocksRetained(int blocksRetained) {
    if (blocksRetained < MIN_BLOCKS_RETAINED)
      throw new ParameterException(
          spec.commandLine(),
          "out-of-bounds: --blocksRetained " + blocksRetained);
    this.blocksRetained = blocksRetained;
  }
  
  
  
  @Mixin
  private PortOpt port;
  
  @Mixin
  private DemoOpt demo;
  
  
  @Override
  public Integer call() throws Exception {
    
    var binner = TimeBinner.forExponent(binExponent);
    long now = System.currentTimeMillis();
    var chainParams = ChainParams.forStartUtc(binner, now);
    
    var settings  = new NotarySettings(
        chainParams, blocksRetained, blocksSearched);
    
    var out = System.out;
    out.println(" C H A I N    I N C E P T I O N");
    out.println(" ==============================");
    // out.println("Settings:");
    settings.toProperties().store(out, "Settings:");
    out.println();
    out.println(" ==============================");

    var notary = Notary.incept(dirOpt.rootDir(), settings, ergd.log());
    boolean withUi = demo != null && demo.demo;
    ergd.launch(notary, port.no(), withUi);
    return 0;
  }
  
}




@Command(
    name = "run",
    description = {
        "Starts a REST service on an exising notary directory",
        "By design, OK if other instances of this program (or",
        "library) are concurrent users of the directory (e.g.",
        "network-mounted and accessed from other machines)."
    }
    )
class Run implements Callable<Integer> {
  
  @ParentCommand
  private Ergd ergd;
  
  @Spec
  CommandSpec spec;
  
  
  @Mixin
  private ReqRootDir dirOpt;

  
  @Mixin
  private PortOpt port;

  @Mixin
  private DemoOpt demo;

  @Override
  public Integer call() throws Exception {
    var out = System.out;
    out.print("Loading notary..");
    var notary = Notary.load(dirOpt.rootDir(), ergd.log());
    out.println();
    notary.settings().toProperties().store(out, "Chain/notary settings");
    out.println();
    boolean withUi = demo != null && demo.demo;
    ergd.launch(notary, port.no(), withUi);
    return 0;
  }
  
}

















