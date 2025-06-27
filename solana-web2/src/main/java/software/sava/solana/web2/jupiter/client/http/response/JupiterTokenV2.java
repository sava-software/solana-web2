package software.sava.solana.web2.jupiter.client.http.response;

import software.sava.core.accounts.PublicKey;
import software.sava.solana.web2.jupiter.client.http.request.JupiterTokenTag;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static software.sava.rpc.json.PublicKeyEncoding.parseBase58Encoded;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;
import static systems.comodal.jsoniter.JsonIterator.fieldStartsWith;

public record JupiterTokenV2(PublicKey address,
                             String name,
                             String symbol,
                             String icon,
                             int decimals,
                             BigDecimal circSupply,
                             BigDecimal totalSupply,
                             PublicKey tokenProgram,
                             JupiterPoolContext jupiterPoolContext,
                             long holderCount,
                             JupiterTokenAudit jupiterTokenAudit,
                             double organicScore,
                             String organicScoreLabel,
                             boolean verified,
                             Collection<String> cexes,
                             Collection<String> tagStrings,
                             BigDecimal fdv,
                             BigDecimal mcap,
                             double usdPrice,
                             BigInteger priceBlockId,
                             BigDecimal liquidity,
                             Collection<JupiterTokenStats> tokenStats,
                             int ctLikes,
                             int smartCtLikes,
                             Instant updatedAt) implements TokenContext {

  public static JupiterTokenV2 parseToken(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.create();
  }

  public static Map<PublicKey, JupiterTokenV2> parseTokens(final JsonIterator ji) {
    final var tokens = HashMap.<PublicKey, JupiterTokenV2>newHashMap(1_024);
    while (ji.readArray()) {
      final var token = parseToken(ji);
      tokens.put(token.address(), token);
    }
    return tokens;
  }

  public String logoURI() {
    return icon;
  }

  @Override
  public Set<JupiterTokenTag> tags() {
    final var tags = EnumSet.noneOf(JupiterTokenTag.class);
    for (final var tag : tagStrings) {
      try {
        tags.add(JupiterTokenTag.valueOf(tag));
      } catch (final RuntimeException ex) {
        // ignore
      }
    }
    return tags;
  }

  @Override
  public BigDecimal dailyVolume() {
    final var oneDay = Duration.ofDays(1);
    for (final var tokenStats : tokenStats) {
      if (tokenStats.duration().compareTo(oneDay) == 0) {
        return BigDecimal.valueOf(tokenStats.buyVolume());
      }
    }
    return null;
  }

  @Override
  public PublicKey freezeAuthority() {
    return null;
  }

  @Override
  public PublicKey mintAuthority() {
    return null;
  }

  @Override
  public PublicKey permanentDelegate() {
    return null;
  }

  @Override
  public Instant createdAt() {
    return null;
  }

  @Override
  public Instant mintedAt() {
    return null;
  }

  private static final Map<TokenExtension, String> EMPTY_EXTENSIONS = Map.of();

  @Override
  public Map<TokenExtension, String> extensions() {
    return EMPTY_EXTENSIONS;
  }

  private static final class Parser implements FieldBufferPredicate {

    private PublicKey address;
    private String name;
    private String symbol;
    private String icon;
    private int decimals;
    private BigDecimal circSupply;
    private BigDecimal totalSupply;
    private PublicKey tokenProgram;
    private JupiterPoolContext jupiterPoolContext;
    private long holderCount;
    private JupiterTokenAudit jupiterTokenAudit;
    private double organicScore;
    private String organicScoreLabel;
    private boolean verified;
    private Collection<String> cexes;
    private Collection<String> tags;
    private BigDecimal fdv;
    private BigDecimal mcap;
    private double usdPrice;
    private BigInteger priceBlockId;
    private BigDecimal liquidity;
    private final List<JupiterTokenStats> tokenStats;
    private int ctLikes;
    private int smartCtLikes;
    private Instant updatedAt;

    private Parser() {
      this.tokenStats = new ArrayList<>();
    }

    private JupiterTokenV2 create() {
      return new JupiterTokenV2(address, name, symbol, icon, decimals, circSupply, totalSupply,
          tokenProgram, jupiterPoolContext, holderCount, jupiterTokenAudit, organicScore, organicScoreLabel,
          verified, cexes, tags, fdv, mcap, usdPrice, priceBlockId, liquidity,
          tokenStats, ctLikes, smartCtLikes, updatedAt
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("id", buf, offset, len)) {
        this.address = parseBase58Encoded(ji);
      } else if (fieldEquals("name", buf, offset, len)) {
        this.name = ji.readString();
      } else if (fieldEquals("symbol", buf, offset, len)) {
        this.symbol = ji.readString();
      } else if (fieldEquals("icon", buf, offset, len)) {
        this.icon = ji.readString();
      } else if (fieldEquals("decimals", buf, offset, len)) {
        this.decimals = ji.readInt();
      } else if (fieldEquals("circSupply", buf, offset, len)) {
        this.circSupply = ji.readBigDecimal();
      } else if (fieldEquals("totalSupply", buf, offset, len)) {
        this.totalSupply = ji.readBigDecimal();
      } else if (fieldEquals("tokenProgram", buf, offset, len)) {
        this.tokenProgram = parseBase58Encoded(ji);
      } else if (fieldEquals("firstPool", buf, offset, len)) {
        this.jupiterPoolContext = JupiterPoolContext.parse(ji);
      } else if (fieldEquals("holderCount", buf, offset, len)) {
        this.holderCount = ji.readLong();
      } else if (fieldEquals("audit", buf, offset, len)) {
        this.jupiterTokenAudit = JupiterTokenAudit.parse(ji);
      } else if (fieldEquals("organicScore", buf, offset, len)) {
        this.organicScore = ji.readDouble();
      } else if (fieldEquals("organicScoreLabel", buf, offset, len)) {
        this.organicScoreLabel = ji.readString();
      } else if (fieldEquals("isVerified", buf, offset, len)) {
        this.verified = ji.readBoolean();
      } else if (fieldEquals("cexes", buf, offset, len)) {
        final var cexes = new ArrayList<String>();
        while (ji.readArray()) {
          cexes.add(ji.readString());
        }
        this.cexes = cexes;
      } else if (fieldEquals("tags", buf, offset, len)) {
        final var tags = new ArrayList<String>();
        while (ji.readArray()) {
          tags.add(ji.readString());
        }
        this.tags = tags;
      } else if (fieldEquals("fdv", buf, offset, len)) {
        this.fdv = ji.readBigDecimal();
      } else if (fieldEquals("mcap", buf, offset, len)) {
        this.mcap = ji.readBigDecimal();
      } else if (fieldEquals("usdPrice", buf, offset, len)) {
        this.usdPrice = ji.readDouble();
      } else if (fieldEquals("priceBlockId", buf, offset, len)) {
        this.priceBlockId = ji.readBigInteger();
      } else if (fieldEquals("liquidity", buf, offset, len)) {
        this.liquidity = ji.readBigDecimal();
      } else if (fieldStartsWith("stats", buf, offset, len)) {
        final int unitOffset = offset + len - 1;
        final var unit = switch (buf[unitOffset]) {
          case 'm' -> MINUTES;
          case 'h' -> HOURS;
          default -> null;
        };
        if (unit == null) {
          ji.skip();
        } else {
          final int from = offset + 5;
          final int duration = Integer.parseInt(new String(buf, from, unitOffset - from));
          final var tokenStats = JupiterTokenStats.parse(ji, Duration.of(duration, unit));
          this.tokenStats.add(tokenStats);
        }
      } else if (fieldEquals("ctLikes", buf, offset, len)) {
        this.ctLikes = ji.readInt();
      } else if (fieldEquals("smartCtLikes", buf, offset, len)) {
        this.smartCtLikes = ji.readInt();
      } else if (fieldEquals("updatedAt", buf, offset, len)) {
        this.updatedAt = Instant.parse(ji.readString());
      } else {
        ji.skip();
      }
      return true;
    }
  }
}
