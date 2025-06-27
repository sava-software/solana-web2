package software.sava.solana.web2.jupiter.client.http;

import software.sava.core.accounts.PublicKey;
import software.sava.solana.web2.jupiter.client.http.request.JupiterQuoteRequest;
import software.sava.solana.web2.jupiter.client.http.request.JupiterTokenTag;
import software.sava.solana.web2.jupiter.client.http.response.*;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static software.sava.solana.web2.jupiter.client.http.JupiterHttpClient.DATE_TIME_FORMATTER;
import static software.sava.solana.web2.jupiter.client.http.JupiterHttpClient.DEFAULT_REQUEST_TIMEOUT;

public interface JupiterClient {

  String PUBLIC_QUOTE_ENDPOINT = "https://lite-api.jup.ag";
  String PUBLIC_TOKEN_LIST_ENDPOINT = "https://lite-api.jup.ag";
  String WORKER_ENDPOINT = "https://worker.jup.ag";

  static JupiterClient createClient(final URI quoteSwapEndpoint,
                                    final URI tokensEndpoint,
                                    final URI workerEndpoint,
                                    final HttpClient httpClient,
                                    final Duration requestTimeout,
                                    final UnaryOperator<HttpRequest.Builder> extendRequest,
                                    final Predicate<HttpResponse<byte[]>> applyResponse) {
    return new JupiterHttpClient(
        quoteSwapEndpoint,
        tokensEndpoint,
        workerEndpoint,
        httpClient,
        requestTimeout,
        extendRequest,
        applyResponse
    );
  }

  static JupiterClient createClient(final URI quoteSwapEndpoint,
                                    final URI tokensEndpoint,
                                    final HttpClient httpClient,
                                    final Duration requestTimeout,
                                    final UnaryOperator<HttpRequest.Builder> extendRequest,
                                    final Predicate<HttpResponse<byte[]>> applyResponse) {
    return new JupiterHttpClient(
        quoteSwapEndpoint,
        tokensEndpoint,
        URI.create(WORKER_ENDPOINT),
        httpClient,
        requestTimeout,
        extendRequest,
        applyResponse
    );
  }

  static JupiterClient createClient(final URI quoteSwapEndpoint,
                                    final URI tokensEndpoint,
                                    final HttpClient httpClient,
                                    final Duration requestTimeout,
                                    final Predicate<HttpResponse<byte[]>> applyResponse) {
    return createClient(
        quoteSwapEndpoint,
        tokensEndpoint,
        httpClient,
        requestTimeout,
        null,
        applyResponse
    );
  }

  static JupiterClient createClient(final URI quoteSwapEndpoint,
                                    final URI tokensEndpoint,
                                    final HttpClient httpClient,
                                    final UnaryOperator<HttpRequest.Builder> extendRequest,
                                    final Predicate<HttpResponse<byte[]>> applyResponse) {
    return createClient(
        quoteSwapEndpoint,
        tokensEndpoint,
        httpClient,
        DEFAULT_REQUEST_TIMEOUT,
        extendRequest,
        applyResponse
    );
  }

  static JupiterClient createClient(final URI quoteSwapEndpoint,
                                    final URI tokensEndpoint,
                                    final HttpClient httpClient,
                                    final Predicate<HttpResponse<byte[]>> applyResponse) {
    return createClient(
        quoteSwapEndpoint,
        tokensEndpoint,
        httpClient,
        DEFAULT_REQUEST_TIMEOUT,
        null,
        applyResponse
    );
  }

  static JupiterClient createClient(final HttpClient httpClient,
                                    final UnaryOperator<HttpRequest.Builder> extendRequest,
                                    final Predicate<HttpResponse<byte[]>> applyResponse) {
    return createClient(
        URI.create(PUBLIC_QUOTE_ENDPOINT),
        URI.create(PUBLIC_TOKEN_LIST_ENDPOINT),
        httpClient,
        extendRequest,
        applyResponse
    );
  }

  static JupiterClient createClient(final URI quoteEndpoint,
                                    final URI tokensEndpoint,
                                    final HttpClient httpClient,
                                    final Duration requestTimeout) {
    return createClient(
        quoteEndpoint,
        tokensEndpoint,
        httpClient,
        requestTimeout,
        null,
        null
    );
  }

  static JupiterClient createClient(final URI quoteEndpoint,
                                    final URI tokensEndpoint,
                                    final HttpClient httpClient) {
    return createClient(
        quoteEndpoint,
        tokensEndpoint,
        httpClient,
        DEFAULT_REQUEST_TIMEOUT,
        null, null
    );
  }

  static JupiterClient createClient(final HttpClient httpClient) {
    return createClient(
        URI.create(PUBLIC_QUOTE_ENDPOINT),
        URI.create(PUBLIC_TOKEN_LIST_ENDPOINT),
        httpClient,
        DEFAULT_REQUEST_TIMEOUT,
        null, null
    );
  }

  static Map<String, TokenContext> reMapBySymbol(final Map<PublicKey, TokenContext> byMintAddress) {
    final var bySymbol = HashMap.<String, TokenContext>newHashMap(byMintAddress.size());
    for (final var tokenContext : byMintAddress.values()) {
      final var previous = bySymbol.put(tokenContext.symbol(), tokenContext);
      if (previous != null) {
        throw new IllegalStateException(String.format("Duplicate token symbol %s:%n  %s%n  %s", tokenContext.symbol(), previous, tokenContext));
      }
    }
    return bySymbol;
  }

  URI endpoint();

  CompletableFuture<Map<PublicKey, JupiterTokenV2>> queryTokens(final String query);

  CompletableFuture<Map<PublicKey, JupiterTokenV2>> queryTokens(final Collection<String> query);

  CompletableFuture<Map<PublicKey, JupiterTokenV2>> tokensForTag(final String tag);

  CompletableFuture<Map<PublicKey, JupiterTokenV2>> tokensForCategory(final String category,
                                                                      final String interval,
                                                                      final int limit);

  CompletableFuture<Map<PublicKey, JupiterTokenV2>> recentTokens();

  @Deprecated
  CompletableFuture<TokenContext> token(final PublicKey mint);

  @Deprecated
  CompletableFuture<Map<PublicKey, TokenContext>> allTokens();

  CompletableFuture<List<PublicKey>> tradableMints();

  @Deprecated
  CompletableFuture<Map<PublicKey, TokenContext>> tokenMap(final JupiterTokenTag tag);

  @Deprecated
  CompletableFuture<Map<PublicKey, TokenContext>> tokenMap(final Collection<JupiterTokenTag> tags);

  @Deprecated
  default CompletableFuture<Map<PublicKey, TokenContext>> verifiedTokenMap() {
    return tokenMap(JupiterTokenTag.verified);
  }

  @Deprecated
  default CompletableFuture<Map<PublicKey, TokenContext>> liquidStakingTokens() {
    return tokenMap(JupiterTokenTag.lst);
  }

  CompletableFuture<Map<String, PublicKey>> getDexLabelToProgramIdMap();

  CompletableFuture<JupiterSwapTx> swap(final StringBuilder jsonBodyBuilder, final JupiterQuote jupiterQuote);

  CompletableFuture<JupiterSwapTx> swap(final StringBuilder jsonBodyBuilder, final byte[] quoteResponseJson);

  CompletableFuture<JupiterSwapTx> swap(final String jsonBodyPrefix, final JupiterQuote jupiterQuote);

  CompletableFuture<JupiterSwapTx> swap(final String jsonBodyPrefix, final byte[] quoteResponseJson);

  CompletableFuture<byte[]> swapInstructions(final StringBuilder jsonBodyBuilder, final JupiterQuote jupiterQuote);

  CompletableFuture<byte[]> swapInstructions(final StringBuilder jsonBodyBuilder, final byte[] quoteResponseJson);

  CompletableFuture<byte[]> swapInstructions(final String jsonBodyPrefix, final JupiterQuote jupiterQuote);

  CompletableFuture<byte[]> swapInstructions(final String jsonBodyPrefix, final byte[] quoteResponseJson);

  default CompletableFuture<JupiterQuote> getQuote(final JupiterQuoteRequest quoteRequest) {
    return getQuote(quoteRequest.serialize());
  }

  CompletableFuture<JupiterQuote> getQuote(final BigInteger amount, final String query);

  CompletableFuture<JupiterQuote> getQuote(final String query);

  default CompletableFuture<JupiterQuote> getQuote(final JupiterQuoteRequest quoteRequest,
                                                   final Duration requestTimeout) {
    return getQuote(quoteRequest.serialize(), requestTimeout);
  }

  CompletableFuture<JupiterQuote> getQuote(final BigInteger amount,
                                           final String query,
                                           final Duration requestTimeout);

  CompletableFuture<JupiterQuote> getQuote(final String query,
                                           final Duration requestTimeout);

  CompletableFuture<JupiterSwapTx> swap(final StringBuilder jsonBodyBuilder,
                                        final JupiterQuote jupiterQuote,
                                        final Duration requestTimeout);

  CompletableFuture<JupiterSwapTx> swap(final StringBuilder jsonBodyBuilder,
                                        final byte[] quoteResponseJson,
                                        final Duration requestTimeout);

  CompletableFuture<JupiterSwapTx> swap(final String jsonBodyPrefix,
                                        final JupiterQuote jupiterQuote,
                                        final Duration requestTimeout);

  CompletableFuture<JupiterSwapTx> swap(final String jsonBodyPrefix,
                                        final byte[] quoteResponseJson,
                                        final Duration requestTimeout);

  CompletableFuture<byte[]> swapInstructions(final StringBuilder jsonBodyBuilder,
                                             final JupiterQuote jupiterQuote,
                                             final Duration requestTimeout);

  CompletableFuture<byte[]> swapInstructions(final StringBuilder jsonBodyBuilder,
                                             final byte[] quoteResponseJson,
                                             final Duration requestTimeout);

  CompletableFuture<byte[]> swapInstructions(final String jsonBodyPrefix,
                                             final JupiterQuote jupiterQuote,
                                             final Duration requestTimeout);

  CompletableFuture<byte[]> swapInstructions(final String jsonBodyPrefix,
                                             final byte[] quoteResponseJson,
                                             final Duration requestTimeout);

  CompletableFuture<JupiterUltraOrder> ultraOrder(final BigInteger amount,
                                                  final String query,
                                                  final Duration requestTimeout);

  CompletableFuture<JupiterExecuteOrder> executeOrder(final String base64SignedTx, final String requestId);

  CompletableFuture<List<MarketRecord>> getMarketCache();

  CompletableFuture<ClaimAsrProof> claimAsrProof(final PublicKey account,
                                                 final String asrTimeline,
                                                 final SequencedCollection<PublicKey> mints);

  default CompletableFuture<ClaimAsrProof> claimAsrProof(final PublicKey account,
                                                         final LocalDate asrTimeline,
                                                         final SequencedCollection<PublicKey> mints) {
    return claimAsrProof(account, DATE_TIME_FORMATTER.format(asrTimeline).toLowerCase(Locale.ENGLISH), mints);
  }
}
