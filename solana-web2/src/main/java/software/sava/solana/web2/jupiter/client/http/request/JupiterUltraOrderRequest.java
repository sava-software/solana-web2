package software.sava.solana.web2.jupiter.client.http.request;

import software.sava.core.accounts.PublicKey;
import systems.comodal.jsoniter.JsonIterator;

import java.math.BigInteger;
import java.util.Set;

public interface JupiterUltraOrderRequest {

  static JupiterUltraOrderRequest.Builder buildRequest() {
    return new JupiterUltraOrderRequestRecord.BuilderImpl();
  }

  static JupiterUltraOrderRequest.Builder buildRequest(final JupiterUltraOrderRequest prototype) {
    return prototype == null ? buildRequest() : new JupiterUltraOrderRequestRecord.BuilderImpl(prototype);
  }

  static JupiterUltraOrderRequest parseRequest(final JupiterUltraOrderRequest prototype,
                                               final JsonIterator ji) {
    final var parser = new JupiterUltraOrderRequestRecord.Parser(prototype);
    ji.testObject(parser);
    return parser.createRequest();
  }

  static JupiterUltraOrderRequest parseRequest(final JsonIterator ji) {
    return parseRequest(null, ji);
  }

  PublicKey inputMint();

  PublicKey outputMint();

  BigInteger amount();

  PublicKey taker();

  PublicKey referralAccount();

  int referralFeeBps();

  Set<String> excludeRouters();

  Set<String> excludeDexes();

  default String serialize() {
    final var builder = new StringBuilder(256);
    builder.append("inputMint=").append(inputMint().toBase58());
    builder.append("&outputMint=").append(outputMint().toBase58());
    final var amount = amount();
    if (amount != null && amount.signum() > 0) {
      builder.append("&amount=").append(amount);
    }
    if (taker() != null) {
      builder.append("&taker=").append(taker().toBase58());
    }
    if (referralAccount() != null) {
      builder.append("&referralAccount=").append(referralAccount().toBase58());
    }
    if (referralFeeBps() > 0) {
      builder.append("&referralFeeBps=").append(referralFeeBps());
    }
    final var excludeRouters = excludeRouters();
    if (excludeRouters != null && !excludeRouters.isEmpty()) {
      builder.append("&excludeRouters=").append(String.join(",", excludeRouters));
    }
    final var excludeDexes = excludeDexes();
    if (excludeDexes != null && !excludeDexes.isEmpty()) {
      builder.append("&excludeDexes=").append(String.join(",", excludeDexes));
    }
    return builder.toString();
  }

  interface Builder extends JupiterUltraOrderRequest {

    JupiterUltraOrderRequest create();

    Builder amount(final BigInteger amount);

    Builder inputMint(final PublicKey inputMint);

    Builder outputMint(final PublicKey outputMint);

    Builder taker(final PublicKey taker);

    Builder referralAccount(final PublicKey referralAccount);

    Builder referralFeeBps(final int referralFeeBps);

    Builder excludeRouters(final Set<String> excludeRouters);

    Builder excludeRouter(final String excludeRouter);

    Builder excludeDexes(final Set<String> excludeDexes);

    Builder excludeDex(final String excludeDex);
  }
}
