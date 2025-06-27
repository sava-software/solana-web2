package software.sava.solana.web2.jupiter.client.http.response;

import software.sava.core.accounts.PublicKey;
import software.sava.solana.web2.jupiter.client.http.request.JupiterTokenTag;
import systems.comodal.jsoniter.CharBufferFunction;
import systems.comodal.jsoniter.ContextFieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static java.lang.System.Logger.Level.WARNING;
import static software.sava.rpc.json.PublicKeyEncoding.parseBase58Encoded;
import static software.sava.solana.web2.jupiter.client.http.request.JupiterTokenTag.*;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record TokenContextRecord(PublicKey address,
                                 int decimals,
                                 String name,
                                 String symbol,
                                 String logoURI,
                                 Set<JupiterTokenTag> tags,
                                 BigDecimal dailyVolume,
                                 PublicKey freezeAuthority,
                                 PublicKey mintAuthority,
                                 PublicKey permanentDelegate,
                                 Instant createdAt,
                                 Instant mintedAt,
                                 Map<TokenExtension, String> extensions) implements TokenContext {

  private static final System.Logger log = System.getLogger(TokenContextRecord.class.getName());

  public static TokenContext parseToken(final JsonIterator ji) {
    return ji.testObject(new Builder(), PARSER).create();
  }

  public static Map<PublicKey, TokenContext> parseTokens(final JsonIterator ji) {
    final var tokens = HashMap.<PublicKey, TokenContext>newHashMap(2_048);
    while (ji.readArray()) {
      final var token = parseToken(ji);
      tokens.put(token.address(), token);
    }
    return tokens;
  }

  private static final CharBufferFunction<JupiterTokenTag> TAG_PARSER = (buf, offset, len) -> {
    if (fieldEquals("verified", buf, offset, len)) {
      return verified;
    } else if (fieldEquals("community", buf, offset, len)) {
      return community;
    } else if (fieldEquals("token-2022", buf, offset, len) || fieldEquals("token_2022", buf, offset, len)) {
      return token_2022;
    } else if (fieldEquals("launchpad", buf, offset, len)) {
      return launchpad;
    } else if (fieldEquals("lst", buf, offset, len)) {
      return lst;
    } else if (fieldEquals("lst-new", buf, offset, len)) {
      return lst_new;
    } else if (fieldEquals("unknown", buf, offset, len)) {
      return unknown;
    } else if (fieldEquals("birdeye-trending", buf, offset, len) || fieldEquals("birdeye_trending", buf, offset, len)) {
      return birdeye_trending;
    } else if (fieldEquals("clone", buf, offset, len)) {
      return clone;
    } else if (fieldEquals("pump", buf, offset, len)) {
      return pump;
    } else if (fieldEquals("strict", buf, offset, len)) {
      return strict;
    } else if (fieldEquals("moonshot", buf, offset, len)) {
      return moonshot;
    } else if (fieldEquals("deduplicated", buf, offset, len)) {
      return deduplicated;
    } else if (fieldEquals("deprecated", buf, offset, len)) {
      return deprecated;
    } else if (fieldEquals("internal", buf, offset, len)) {
      return internal;
    } else if (fieldEquals("community-assist", buf, offset, len)) {
      return community_assist;
    } else {
      log.log(WARNING,
          "Failed to parse unknown Jupiter token tag [{0}].",
          new String(buf, offset, len)
      );
      return null;
    }
  };

  private static final ContextFieldBufferPredicate<Builder> PARSER = (builder, buf, offset, len, ji) -> {
    if (fieldEquals("address", buf, offset, len)) {
      builder.address = parseBase58Encoded(ji);
    } else if (fieldEquals("decimals", buf, offset, len)) {
      builder.decimals = ji.readInt();
    } else if (fieldEquals("name", buf, offset, len)) {
      builder.name = ji.readString();
    } else if (fieldEquals("symbol", buf, offset, len)) {
      builder.symbol = ji.readString();
    } else if (fieldEquals("logoURI", buf, offset, len)) {
      builder.logoURI = ji.readString();
    } else if (fieldEquals("tags", buf, offset, len)) {
      if (ji.readArray()) {
        builder.tags = EnumSet.noneOf(JupiterTokenTag.class);
        do {
          final var tag = ji.applyChars(TAG_PARSER);
          if (tag != null) {
            builder.tags.add(tag);
          }
        } while (ji.readArray());
      }
    } else if (fieldEquals("daily_volume", buf, offset, len)) {
      builder.dailyVolume = ji.readBigDecimalDropZeroes();
    } else if (fieldEquals("freeze_authority", buf, offset, len)) {
      builder.freezeAuthority = parseBase58Encoded(ji);
    } else if (fieldEquals("mint_authority", buf, offset, len)) {
      builder.mintAuthority = parseBase58Encoded(ji);
    } else if (fieldEquals("permanent_delegate", buf, offset, len)) {
      builder.permanentDelegate = parseBase58Encoded(ji);
    } else if (fieldEquals("extensions", buf, offset, len)) {
      final var extensions = new EnumMap<TokenExtension, String>(TokenExtension.class);
      for (String extension, value; (extension = ji.readObjField()) != null; ) {
        value = ji.readString();
        try {
          extensions.put(TokenExtension.valueOf(extension), value);
        } catch (final RuntimeException ex) {
          log.log(WARNING,
              "Failed to parse unknown Jupiter token extension [{0}={1}].", extension, value
          );
        }
      }
      builder.extensions = extensions;
    } else if (fieldEquals("created_at", buf, offset, len)) {
      final var createdAt = ji.readString();
      if (createdAt != null) {
        builder.createdAt = Instant.parse(createdAt);
      }
    } else if (fieldEquals("minted_at", buf, offset, len)) {
      final var mintedAt = ji.readString();
      if (mintedAt != null) {
        builder.mintedAt = Instant.parse(mintedAt);
      }
    } else {
      ji.skip();
      log.log(WARNING,
          "Failed to parse unknown Jupiter token context field [{0}].", new String(buf, offset, len)
      );
    }
    return true;
  };

  private static final class Builder {

    private static final Set<JupiterTokenTag> NO_TAGS = Set.of();

    private PublicKey address;
    private int decimals;
    private String name;
    private String symbol;
    private String logoURI;
    private Set<JupiterTokenTag> tags;
    private BigDecimal dailyVolume;
    private PublicKey freezeAuthority;
    private PublicKey mintAuthority;
    private PublicKey permanentDelegate;
    private Instant createdAt;
    private Instant mintedAt;
    private Map<TokenExtension, String> extensions;

    private Builder() {
    }

    private TokenContext create() {
      return new TokenContextRecord(
          address, decimals, name, symbol, logoURI,
          tags == null || tags.isEmpty() ? NO_TAGS : tags,
          dailyVolume,
          freezeAuthority,
          mintAuthority,
          permanentDelegate,
          createdAt,
          mintedAt,
          extensions
      );
    }
  }
}
