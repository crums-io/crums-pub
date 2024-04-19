module io.crums.legacy.tc1 {
  
  requires transitive io.crums.util.mrkl;
  requires transitive io.crums.jsonimple;
  
  requires io.crums.io.store;
  requires java.net.http;
  
  // following to be purged after TC-2 redesign..
  exports io.crums.client;
  exports io.crums.client.repo;
  exports io.crums.model;
  exports io.crums.model.hashing;
  exports io.crums.model.json;
  exports io.crums.util.mrkl.hashing;
  
}