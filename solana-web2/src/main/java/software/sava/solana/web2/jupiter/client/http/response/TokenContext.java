package software.sava.solana.web2.jupiter.client.http.response;

import software.sava.core.accounts.PublicKey;
import software.sava.core.util.DecimalInteger;
import software.sava.solana.web2.jupiter.client.http.request.JupiterTokenTag;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public interface TokenContext extends DecimalInteger {

  PublicKey address();

  int decimals();

  String name();

  String symbol();

  String logoURI();

  @Deprecated
  Set<JupiterTokenTag> tags();

  @Deprecated
  BigDecimal dailyVolume();

  @Deprecated
  PublicKey freezeAuthority();

  @Deprecated
  PublicKey mintAuthority();

  @Deprecated
  PublicKey permanentDelegate();

  @Deprecated
  Instant createdAt();

  @Deprecated
  Instant mintedAt();

  @Deprecated
  Map<TokenExtension, String> extensions();
}
