module io.crums.tc.notary.server {
  
  requires transitive io.crums.tc.notary;
  
  requires jdk.httpserver;
  requires java.logging;
  requires info.picocli;
  opens io.crums.tc.notary.server.main to info.picocli;
  
}