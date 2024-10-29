module io.crums.tc.cli.crum {
  
  requires transitive io.crums.tc;
  
  requires io.crums.util;
  requires io.crums.stowkwik;
  requires info.picocli;
  opens io.crums.tc.cli.crum to info.picocli;
  
}