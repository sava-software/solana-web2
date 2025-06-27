package software.sava.solana.web2.jupiter.client.http.response;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.time.Duration;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record JupiterTokenStats(Duration duration,
                         double priceChange,
                         double liquidityChange,
                         double volumeChange,
                         double buyVolume, double sellVolume,
                         double buyOrganicVolume, double sellOrganicVolume,
                         int numBuys, int numSells,
                         int numTraders,
                         int numOrganicBuyers,
                         int numNetBuyers) {

  public static JupiterTokenStats parse(final JsonIterator ji, final Duration duration) {
    final var parser = new Parser(duration);
    ji.testObject(parser);
    return parser.create();
  }

  private static final class Parser implements FieldBufferPredicate {

    private final Duration duration;
    private double priceChange;
    private double liquidityChange;
    private double volumeChange;
    private double buyVolume;
    private double sellVolume;
    private double buyOrganicVolume;
    private double sellOrganicVolume;
    private int numBuys;
    private int numSells;
    private int numTraders;
    private int numOrganicBuyers;
    private int numNetBuyers;

    private Parser(final Duration duration) {
      this.duration = duration;
    }

    private JupiterTokenStats create() {
      return new JupiterTokenStats(duration, priceChange, liquidityChange, volumeChange,
          buyVolume, sellVolume, buyOrganicVolume, sellOrganicVolume,
          numBuys, numSells, numTraders, numOrganicBuyers, numNetBuyers
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("priceChange", buf, offset, len)) {
        this.priceChange = ji.readDouble();
      } else if (fieldEquals("liquidityChange", buf, offset, len)) {
        this.liquidityChange = ji.readDouble();
      } else if (fieldEquals("volumeChange", buf, offset, len)) {
        this.volumeChange = ji.readDouble();
      } else if (fieldEquals("buyVolume", buf, offset, len)) {
        this.buyVolume = ji.readDouble();
      } else if (fieldEquals("sellVolume", buf, offset, len)) {
        this.sellVolume = ji.readDouble();
      } else if (fieldEquals("buyOrganicVolume", buf, offset, len)) {
        this.buyOrganicVolume = ji.readDouble();
      } else if (fieldEquals("sellOrganicVolume", buf, offset, len)) {
        this.sellOrganicVolume = ji.readDouble();
      } else if (fieldEquals("numBuys", buf, offset, len)) {
        this.numBuys = ji.readInt();
      } else if (fieldEquals("numSells", buf, offset, len)) {
        this.numSells = ji.readInt();
      } else if (fieldEquals("numTraders", buf, offset, len)) {
        this.numTraders = ji.readInt();
      } else if (fieldEquals("numOrganicBuyers", buf, offset, len)) {
        this.numOrganicBuyers = ji.readInt();
      } else if (fieldEquals("numNetBuyers", buf, offset, len)) {
        this.numNetBuyers = ji.readInt();
      } else {
        ji.skip();
      }
      return true;
    }
  }
}
