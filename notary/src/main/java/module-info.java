module io.crums.tc.notary {
  
  requires transitive io.crums.tc;
  
  
  requires io.crums.io.store;
  requires io.crums.stowkwik;
  
  exports io.crums.tc.notary;
  exports io.crums.tc.notary.except;
  
}