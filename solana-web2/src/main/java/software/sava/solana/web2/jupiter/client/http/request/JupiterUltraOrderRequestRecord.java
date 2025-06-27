package software.sava.solana.web2.jupiter.client.http.request;

import software.sava.core.accounts.PublicKey;
import software.sava.rpc.json.PublicKeyEncoding;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record JupiterUltraOrderRequestRecord(PublicKey inputMint,
                                             PublicKey outputMint,
                                             BigInteger amount,
                                             PublicKey taker,
                                             PublicKey referralAccount,
                                             int referralFeeBps,
                                             Set<String> excludeRouters,
                                             Set<String> excludeDexes) implements JupiterUltraOrderRequest {

  static final class Parser implements FieldBufferPredicate {

    private final JupiterUltraOrderRequest.Builder builder;

    Parser(final JupiterUltraOrderRequest prototype) {
      this.builder = JupiterUltraOrderRequest.buildRequest(prototype);
    }

    JupiterUltraOrderRequest createRequest() {
      return builder.create();
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("amount", buf, offset, len)) {
        builder.amount(ji.readBigInteger());
      } else if (fieldEquals("inputMint", buf, offset, len)) {
        builder.inputMint(PublicKeyEncoding.parseBase58Encoded(ji));
      } else if (fieldEquals("outputMint", buf, offset, len)) {
        builder.outputMint(PublicKeyEncoding.parseBase58Encoded(ji));
      } else if (fieldEquals("taker", buf, offset, len)) {
        builder.taker(PublicKeyEncoding.parseBase58Encoded(ji));
      } else if (fieldEquals("referralAccount", buf, offset, len)) {
        builder.referralAccount(PublicKeyEncoding.parseBase58Encoded(ji));
      } else if (fieldEquals("referralFeeBps", buf, offset, len)) {
        builder.referralFeeBps(ji.readInt());
      } else {
        ji.skip();
      }
      return true;
    }
  }

  static final class BuilderImpl implements JupiterUltraOrderRequest.Builder {

    private PublicKey inputMint;
    private PublicKey outputMint;
    private BigInteger amount;
    private PublicKey taker;
    private PublicKey referralAccount;
    private int referralFeeBps;
    private Set<String> excludeRouters;
    private Set<String> excludeDexes;

    BuilderImpl() {
    }

    BuilderImpl(final JupiterUltraOrderRequest prototype) {
      this.amount = prototype.amount();
      this.inputMint = prototype.inputMint();
      this.outputMint = prototype.outputMint();
      this.taker = prototype.taker();
      this.referralAccount = prototype.referralAccount();
      this.referralFeeBps = prototype.referralFeeBps();
    }

    @Override
    public JupiterUltraOrderRequest create() {
      return new JupiterUltraOrderRequestRecord(
          inputMint,
          outputMint,
          amount,
          taker,
          referralAccount,
          referralFeeBps,
          excludeRouters,
          excludeDexes
      );
    }

    @Override
    public Builder inputMint(final PublicKey inputMint) {
      this.inputMint = inputMint;
      return this;
    }

    @Override
    public Builder referralAccount(final PublicKey referralAccount) {
      this.referralAccount = referralAccount;
      return this;
    }

    @Override
    public Builder taker(final PublicKey taker) {
      this.taker = taker;
      return this;
    }

    @Override
    public Builder outputMint(final PublicKey outputMint) {
      this.outputMint = outputMint;
      return this;
    }

    @Override
    public Builder amount(final BigInteger amount) {
      this.amount = amount;
      return this;
    }

    @Override
    public Builder referralFeeBps(final int referralFeeBps) {
      this.referralFeeBps = referralFeeBps;
      return this;
    }

    @Override
    public Builder excludeRouters(final Set<String> excludeRouters) {
      this.excludeRouters = excludeRouters;
      return this;
    }

    @Override
    public Builder excludeRouter(final String excludeRouter) {
      if (this.excludeRouters == null) {
        this.excludeRouters = new HashSet<>();
      }
      this.excludeRouters.add(excludeRouter);
      return this;
    }

    @Override
    public Builder excludeDexes(final Set<String> excludeDexes) {
      this.excludeDexes = excludeDexes;
      return this;
    }

    @Override
    public Builder excludeDex(final String excludeDex) {
      if (this.excludeDexes == null) {
        this.excludeDexes = new HashSet<>();
      }
      this.excludeDexes.add(excludeDex);
      return this;
    }

    @Override
    public PublicKey inputMint() {
      return inputMint;
    }

    @Override
    public PublicKey outputMint() {
      return outputMint;
    }

    @Override
    public BigInteger amount() {
      return amount;
    }

    @Override
    public PublicKey taker() {
      return taker;
    }

    @Override
    public PublicKey referralAccount() {
      return referralAccount;
    }

    @Override
    public int referralFeeBps() {
      return referralFeeBps;
    }

    @Override
    public Set<String> excludeRouters() {
      return excludeRouters;
    }

    @Override
    public Set<String> excludeDexes() {
      return excludeDexes;
    }
  }
}
