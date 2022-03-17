module io.crums.core {
  requires transitive io.crums.util.mrkl;
  requires transitive io.crums.jsonimple;
  requires io.crums.io.store;
  requires java.net.http;
  
  exports io.crums.client;
  exports io.crums.client.repo;
  exports io.crums.model;
  exports io.crums.model.hashing;
  exports io.crums.model.json;
  exports io.crums.util.mrkl.hashing;
  
}