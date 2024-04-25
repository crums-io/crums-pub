module io.crums.tc {
  
  requires transitive io.crums.util.mrkl;
  requires transitive io.crums.jsonimple;
  requires transitive io.crums.sldg.base;
  
  
  requires io.crums.io.store;
//  requires java.net.http; (TBD)
  
  exports io.crums.tc;
  exports io.crums.tc.except;
  exports io.crums.tc.json;
  
}