/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.server.main;


import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

import io.crums.tc.ChainParams;
import io.crums.tc.TimeBinner;
import io.crums.tc.notary.Notary;
import io.crums.tc.notary.NotaryPolicy;
import io.crums.tc.notary.NotarySettings;
import io.crums.tc.notary.d.NotaryD;
import io.crums.tc.notary.server.HttpServerHelp;
import io.crums.util.TaskStack;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Help.Ansi;

/**
 * Main launch class.
 */
@Command(
    name = "ergd",
    mixinStandardHelpOptions = true,
    version = "ergd 0.1",
    description = {
      "Notary REST service configuration and launcher.%n",
    },
    subcommands = {
        HelpCommand.class,
        Incept.class
    })
public class Ergd {
  
  
  static NotaryD notary;
  
  public static void main(String[] args) {
    int exitCode = new CommandLine(new Ergd()).execute(args);
    System.exit(exitCode);
  }
  
  
  static void launch(Notary notary, int port) throws IOException {
    
    try (var onFail = new TaskStack()) {
      var notaryD = new NotaryD(notary);
      onFail.pushClose(notaryD);
      var server = HttpServer.create(new InetSocketAddress(80), 0);
      onFail.close();
      server.setExecutor(
          Executors.newVirtualThreadPerTaskExecutor());
      
      onFail.clear();
    }
    
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
  
  
  private int port = 80;
  
  
  
  @Option(
      names = { "--port", "-p" },
      paramLabel = "PORT",
      description = {
          "Port no. REST service listens on",
          "Default: 80"
      }
      )
  public void setPort(int port) {
    if (port <= 0 || port > Short.MAX_VALUE)
      throw new ParameterException(
          spec.commandLine(), "out-of-bounds: --port " + port);
  }
  
  
  public int no() {
    return port;
  }
  
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
  
  
  
  
  @Override
  public Integer call() throws Exception {
    
    var binner = TimeBinner.forExponent(binExponent);
    long now = System.currentTimeMillis();
    var chainParams = ChainParams.forStartUtc(binner, now);
    
    var settings  = new NotarySettings(
        chainParams, blocksRetained, blocksSearched);
    
    try (var closeOnFail = new TaskStack()) {
      var notary = Notary.incept(dirOpt.rootDir(), settings);
      closeOnFail.pushClose(notary);
      Ergd.launch(notary, port.no());
      
      closeOnFail.clear();
    }
    
    
    return 0;
  }
  
}




















