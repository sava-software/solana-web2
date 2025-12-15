module software.sava.solana_web2 {
  requires java.net.http;

  requires transitive systems.comodal.json_iterator;

  requires transitive software.sava.core;
  requires transitive software.sava.rpc;

  exports software.sava.solana.web2.helius.client.http;
  exports software.sava.solana.web2.helius.client.http.response;
  exports software.sava.solana.web2.helius.client.http.request;

  exports software.sava.solana.web2.jito.client.http;
  exports software.sava.solana.web2.jito.client.http.response;
}
