package software.sava.solana.web2.jupiter.client.http.response;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record JupiterTokenAudit(boolean mintAuthorityDisabled,
                         boolean freezeAuthorityDisabled,
                         double topHoldersPercentage) {

  public static JupiterTokenAudit parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.create();
  }

  private static final class Parser implements FieldBufferPredicate {

    private boolean mintAuthorityDisabled;
    private boolean freezeAuthorityDisabled;
    private double topHoldersPercentage;

    private Parser() {
    }

    private JupiterTokenAudit create() {
      return new JupiterTokenAudit(mintAuthorityDisabled, freezeAuthorityDisabled, topHoldersPercentage);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("mintAuthorityDisabled", buf, offset, len)) {
        this.mintAuthorityDisabled = ji.readBoolean();
      } else if (fieldEquals("freezeAuthorityDisabled", buf, offset, len)) {
        this.freezeAuthorityDisabled = ji.readBoolean();
      } else if (fieldEquals("topHoldersPercentage", buf, offset, len)) {
        this.topHoldersPercentage = ji.readDouble();
      } else {
        ji.skip();
      }
      return true;
    }
  }
}
