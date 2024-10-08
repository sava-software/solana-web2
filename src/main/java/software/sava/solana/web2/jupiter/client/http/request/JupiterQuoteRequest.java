package software.sava.solana.web2.jupiter.client.http.request;

import software.sava.core.accounts.PublicKey;
import software.sava.solana.web2.jupiter.client.http.response.SwapMode;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.US_ASCII;

public interface JupiterQuoteRequest {

  static Builder buildRequest() {
    return new JupiterQuoteRequestRecord.JupiterQuoteRequestBuilder();
  }

  BigInteger amount();

  SwapMode swapMode();

  PublicKey inputTokenMint();

  PublicKey outputTokenMint();

  int slippageBps();

  Collection<String> excludeDexes();

  Collection<String> allowDexes();

  boolean onlyDirectRoutes();

  boolean asLegacyTransaction();

  int platformFeeBps();

  int maxAccounts();

  default String serialize() {
    final var builder = new StringBuilder(256);
    builder.append("inputMint=").append(inputTokenMint().toBase58());
    builder.append("&outputMint=").append(outputTokenMint().toBase58());
    final var amount = amount();
    if (amount != null && amount.signum() > 0) {
      builder.append("&amount=").append(amount);
    }
    if (slippageBps() > 0) {
      builder.append("&slippageBps=").append(slippageBps());
    }
    if (swapMode() != null) {
      builder.append("&swapMode=").append(swapMode().name());
    }
    final var excludeDexes = excludeDexes();
    if (excludeDexes != null && !excludeDexes.isEmpty()) {
      builder.append("&excludeDexes=").append(URLEncoder.encode(String.join(",", excludeDexes), US_ASCII));
    }
    final var allowDexes = allowDexes();
    if (allowDexes != null && !allowDexes.isEmpty()) {
      builder.append("&dexes=").append(URLEncoder.encode(String.join(",", allowDexes), US_ASCII));
    }
    if (onlyDirectRoutes()) {
      builder.append("&onlyDirectRoutes=true");
    }
    if (asLegacyTransaction()) {
      builder.append("&asLegacyTransaction=true");
    }
    if (platformFeeBps() > 0) {
      builder.append("&platformFeeBps=").append(platformFeeBps());
    }
    if (maxAccounts() > 0) {
      builder.append("&maxAccounts=").append(maxAccounts());
    }
    return builder.toString();
  }

  interface Builder extends JupiterQuoteRequest {

    JupiterQuoteRequest create();

    default Builder amount(final long amount) {
      return amount(BigInteger.valueOf(amount));
    }

    Builder amount(final BigInteger inAmount);

    Builder swapMode(final SwapMode swapMode);

    Builder inputTokenMint(final PublicKey inputTokenMint);

    Builder outputTokenMint(final PublicKey outputTokenMint);

    Builder slippageBps(final int slippageBps);

    Builder excludeDexes(final Collection<String> excludeDexes);

    Builder allowDexes(final Collection<String> allowDexes);

    Builder onlyDirectRoutes(final boolean onlyDirectRoutes);

    Builder asLegacyTransaction(final boolean asLegacyTransaction);

    Builder platformFeeBps(final int platformFeeBps);

    Builder maxAccounts(final int maxAccounts);
  }
}
