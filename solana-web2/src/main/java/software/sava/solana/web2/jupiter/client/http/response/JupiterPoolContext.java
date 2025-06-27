package software.sava.solana.web2.jupiter.client.http.response;

import software.sava.core.accounts.PublicKey;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.time.Instant;

import static software.sava.rpc.json.PublicKeyEncoding.parseBase58Encoded;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record JupiterPoolContext(PublicKey address, Instant createdAt) {

  public static JupiterPoolContext parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.create();
  }

  private static final class Parser implements FieldBufferPredicate {

    private PublicKey address;
    private Instant createdAt;

    private Parser() {
    }

    private JupiterPoolContext create() {
      return new JupiterPoolContext(address, createdAt);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("id", buf, offset, len)) {
        this.address = parseBase58Encoded(ji);
      } else if (fieldEquals("createdAt", buf, offset, len)) {
        this.createdAt = Instant.parse(ji.readString());
      } else {
        ji.skip();
      }
      return true;
    }
  }
}
