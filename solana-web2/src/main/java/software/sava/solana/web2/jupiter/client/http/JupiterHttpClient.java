package software.sava.solana.web2.jupiter.client.http;

import software.sava.core.accounts.PublicKey;
import software.sava.rpc.json.http.client.JsonHttpClient;
import software.sava.solana.web2.jupiter.client.http.response.*;

import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static java.net.http.HttpResponse.BodyHandlers.ofByteArray;
import static software.sava.rpc.json.PublicKeyEncoding.PARSE_BASE58_PUBLIC_KEY;

final class JupiterHttpClient extends JsonHttpClient implements JupiterClient {

  static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(13);
  static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM-yyyy", Locale.ENGLISH);

  private static final Function<HttpResponse<?>, Map<PublicKey, JupiterTokenV2>> TOKENS_V2 = applyGenericResponse(JupiterTokenV2::parseTokens);
  private static final Function<HttpResponse<?>, JupiterQuote> QUOTE_PARSER = applyGenericResponse(JupiterQuote::parse);
  private static final Function<HttpResponse<?>, JupiterSwapTx> SWAP_TX = applyGenericResponse(JupiterSwapTx::parse);
  private static final Function<HttpResponse<?>, byte[]> SWAP_INSTRUCTIONS_TX = response -> {
    final var body = readBody(response);
    final int statusCode = response.statusCode();
    if (statusCode < 200 || statusCode >= 300) {
      throw new UncheckedIOException(new UnknownServiceException(String.format(
          "HTTP request failed with [httpCode:%d], [body=%s]", statusCode, new String(body))));
    } else {
      return body;
    }
  };
  private static final Function<HttpResponse<?>, Map<String, PublicKey>> PROGRAM_LABEL_PARSER = applyGenericResponse(ji -> {
    final var programLabels = new TreeMap<String, PublicKey>(String.CASE_INSENSITIVE_ORDER);
    for (PublicKey program; (program = ji.applyObjField(PARSE_BASE58_PUBLIC_KEY)) != null; ) {
      final var dex = ji.readString();
      final var previousDex = programLabels.put(dex, program);
      if (previousDex != null) {
        throw new IllegalStateException(String.format("Duplicate case insensitive dexes: [%s] [%s]", previousDex, dex));
      }
    }
    return programLabels;
  });
  private static final Function<HttpResponse<?>, List<MarketRecord>> MARKET_CACHE_PARSER = applyGenericResponse(MarketRecord::parse);
  private static final Function<HttpResponse<?>, ClaimAsrProof> ASR_PROOF = applyGenericResponse(ClaimAsrProof::parseProof);

  // Ultra
  private static final Function<HttpResponse<?>, JupiterUltraOrder> ULTRA_ORDER_PARSER = applyGenericResponse(JupiterUltraOrder::parse);
  private static final Function<HttpResponse<?>, JupiterExecuteOrder> EXECUTE_ULTRA_ORDER_PARSER = applyGenericResponse(JupiterExecuteOrder::parse);

  // V2 Token API
  private final URI v2TokenPath;
  private final URI v2RecentTokenPath;

  private final String quotePathFormat;
  private final String quotePath;
  private final URI swapURI;
  private final URI swapInstructionsURI;
  private final URI workerURI;
  private final URI executeUltraOrderURI;
  private final String ultraOrderPathFormat;
  private final HttpRequest programLabelsRequest;
  private final Function<HttpResponse<?>, JupiterQuote> quoteParser;
  private final Function<HttpResponse<?>, JupiterUltraOrder> ultraOrderParser;
  private final Function<HttpResponse<?>, JupiterExecuteOrder> executeUltraOrderParser;

  JupiterHttpClient(final URI quoteEndpoint,
                    final URI tokensEndpoint,
                    final URI workerURI,
                    final HttpClient httpClient,
                    final Duration requestTimeout,
                    final UnaryOperator<HttpRequest.Builder> extendRequest,
                    final Predicate<HttpResponse<byte[]>> applyGenericResponse) {
    super(quoteEndpoint, httpClient, requestTimeout, extendRequest, applyGenericResponse, null);
    this.v2TokenPath = tokensEndpoint.resolve("/tokens/v2/");
    this.v2RecentTokenPath = tokensEndpoint.resolve("/tokens/v2/recent");

    this.workerURI = workerURI;
    try {
      final var inetAddress = InetAddress.getByName(quoteEndpoint.getHost());
      if (inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress()) {
        this.quotePathFormat = "/quote?amount=%s&%s";
        this.quotePath = "/quote?";
        this.swapURI = quoteEndpoint.resolve("/swap");
        this.swapInstructionsURI = quoteEndpoint.resolve("/swap-instructions");
        this.programLabelsRequest = newRequest(quoteEndpoint.resolve("/program-id-to-label")).build();
      } else {
        this.quotePathFormat = "/swap/v1/quote?amount=%s&%s";
        this.quotePath = "/swap/v1/quote?";
        this.swapURI = quoteEndpoint.resolve("/swap/v1/swap");
        this.swapInstructionsURI = quoteEndpoint.resolve("/swap/v1/swap-instructions");
        this.programLabelsRequest = newRequest(quoteEndpoint.resolve("/swap/v1/program-id-to-label")).build();
      }
    } catch (final UnknownHostException e) {
      throw new UncheckedIOException(e);
    }
    this.ultraOrderPathFormat = "/ultra/v1/order?amount=%s&%s";
    this.executeUltraOrderURI = quoteEndpoint.resolve("/ultra/v1/execute");
    this.quoteParser = wrapResponseParser(QUOTE_PARSER);
    this.ultraOrderParser = wrapResponseParser(ULTRA_ORDER_PARSER);
    this.executeUltraOrderParser = wrapResponseParser(EXECUTE_ULTRA_ORDER_PARSER);
  }

  private CompletableFuture<Map<PublicKey, JupiterTokenV2>> queryTokens(final String finalPathSegment,
                                                                        final String query) {
    final var url = v2TokenPath.resolve(finalPathSegment + "?query=" + query);
    return sendGetRequest(url, TOKENS_V2);
  }

  private CompletableFuture<Map<PublicKey, JupiterTokenV2>> queryTokens(final String finalPathSegment,
                                                                        final Collection<String> query) {
    return queryTokens(finalPathSegment, String.join(",", query));
  }

  @Override
  public CompletableFuture<Map<PublicKey, JupiterTokenV2>> queryTokens(final String query) {
    return queryTokens("search", query);
  }

  @Override
  public CompletableFuture<Map<PublicKey, JupiterTokenV2>> queryTokens(final Collection<String> query) {
    return queryTokens("search", query);
  }

  @Override
  public CompletableFuture<Map<PublicKey, JupiterTokenV2>> tokensForTag(final String tag) {
    return queryTokens("tag", tag);
  }

  @Override
  public CompletableFuture<Map<PublicKey, JupiterTokenV2>> tokensForCategory(final String category,
                                                                             final String interval,
                                                                             final int limit) {
    final var url = v2TokenPath.resolve(String.format("%s/%s?limit=%d", category, interval, limit));
    return sendGetRequest(url, TOKENS_V2);
  }

  @Override
  public CompletableFuture<Map<PublicKey, JupiterTokenV2>> recentTokens() {
    return sendGetRequest(v2RecentTokenPath, TOKENS_V2);
  }

  @Override
  public CompletableFuture<Map<String, PublicKey>> getDexLabelToProgramIdMap() {
    return httpClient.sendAsync(programLabelsRequest, ofByteArray()).thenApply(wrapResponseParser(PROGRAM_LABEL_PARSER));
  }

  @Override
  public CompletableFuture<JupiterSwapTx> swap(final StringBuilder jsonBodyBuilder, final JupiterQuote jupiterQuote) {
    return swap(jsonBodyBuilder, jupiterQuote, requestTimeout);
  }

  @Override
  public CompletableFuture<JupiterSwapTx> swap(final StringBuilder jsonBodyBuilder, final byte[] quoteResponseJson) {
    return swap(jsonBodyBuilder, quoteResponseJson, requestTimeout);
  }

  @Override
  public CompletableFuture<JupiterSwapTx> swap(final String jsonBodyPrefix, final JupiterQuote jupiterQuote) {
    return swap(jsonBodyPrefix, jupiterQuote, requestTimeout);
  }

  @Override
  public CompletableFuture<JupiterSwapTx> swap(final String jsonBodyPrefix, final byte[] quoteResponseJson) {
    return swap(jsonBodyPrefix, quoteResponseJson, requestTimeout);
  }

  @Override
  public CompletableFuture<byte[]> swapInstructions(final StringBuilder jsonBodyBuilder,
                                                    final JupiterQuote jupiterQuote) {
    return swapInstructions(jsonBodyBuilder, jupiterQuote, requestTimeout);
  }

  @Override
  public CompletableFuture<byte[]> swapInstructions(final StringBuilder jsonBodyBuilder,
                                                    final byte[] quoteResponseJson) {
    return swapInstructions(jsonBodyBuilder, quoteResponseJson, requestTimeout);
  }

  @Override
  public CompletableFuture<byte[]> swapInstructions(final String jsonBodyPrefix, final JupiterQuote jupiterQuote) {
    return swapInstructions(jsonBodyPrefix, jupiterQuote, requestTimeout);
  }

  @Override
  public CompletableFuture<byte[]> swapInstructions(final String jsonBodyPrefix, final byte[] quoteResponseJson) {
    return swapInstructions(jsonBodyPrefix, quoteResponseJson, requestTimeout);
  }

  @Override
  public CompletableFuture<JupiterSwapTx> swap(final StringBuilder jsonBodyBuilder,
                                               final JupiterQuote jupiterQuote,
                                               final Duration requestTimeout) {
    return swap(jsonBodyBuilder, jupiterQuote.quoteResponseJson(), requestTimeout);
  }

  @Override
  public CompletableFuture<JupiterSwapTx> swap(final StringBuilder jsonBodyBuilder,
                                               final byte[] quoteResponseJson,
                                               final Duration requestTimeout) {
    return swap(jsonBodyBuilder.append(new String(quoteResponseJson)).append('}').toString(), requestTimeout);
  }

  @Override
  public CompletableFuture<JupiterSwapTx> swap(final String jsonBodyPrefix,
                                               final JupiterQuote jupiterQuote,
                                               final Duration requestTimeout) {
    return swap(jsonBodyPrefix, jupiterQuote.quoteResponseJson(), requestTimeout);
  }

  @Override
  public CompletableFuture<JupiterSwapTx> swap(final String jsonBodyPrefix,
                                               final byte[] quoteResponseJson,
                                               final Duration requestTimeout) {
    return swap(jsonBodyPrefix + new String(quoteResponseJson) + '}', requestTimeout);
  }

  private CompletableFuture<JupiterSwapTx> swap(final String jsonBody, final Duration requestTimeout) {
    return sendPostRequest(swapURI, SWAP_TX, requestTimeout, jsonBody);
  }

  @Override
  public CompletableFuture<byte[]> swapInstructions(final StringBuilder jsonBodyBuilder,
                                                    final JupiterQuote jupiterQuote,
                                                    final Duration requestTimeout) {
    return swapInstructions(jsonBodyBuilder, jupiterQuote.quoteResponseJson(), requestTimeout);
  }

  @Override
  public CompletableFuture<byte[]> swapInstructions(final StringBuilder jsonBodyBuilder,
                                                    final byte[] quoteResponseJson,
                                                    final Duration requestTimeout) {
    return swapInstructions(jsonBodyBuilder.append(new String(quoteResponseJson)).append('}').toString(), requestTimeout);
  }

  @Override
  public CompletableFuture<byte[]> swapInstructions(final String jsonBodyPrefix,
                                                    final JupiterQuote jupiterQuote,
                                                    final Duration requestTimeout) {
    return swapInstructions(jsonBodyPrefix, jupiterQuote.quoteResponseJson(), requestTimeout);
  }

  @Override
  public CompletableFuture<byte[]> swapInstructions(final String jsonBodyPrefix,
                                                    final byte[] quoteResponseJson,
                                                    final Duration requestTimeout) {
    return swapInstructions(jsonBodyPrefix + new String(quoteResponseJson) + '}', requestTimeout);
  }

  private CompletableFuture<byte[]> swapInstructions(final String jsonBody, final Duration requestTimeout) {
    return sendPostRequest(swapInstructionsURI, SWAP_INSTRUCTIONS_TX, requestTimeout, jsonBody);
  }

  @Override
  public CompletableFuture<JupiterQuote> getQuote(final BigInteger amount, final String query) {
    return getQuote(amount, query, requestTimeout);
  }

  @Override
  public CompletableFuture<JupiterQuote> getQuote(final String query) {
    return getQuote(query, requestTimeout);
  }

  @Override
  public CompletableFuture<JupiterQuote> getQuote(final BigInteger amount,
                                                  final String query,
                                                  final Duration requestTimeout) {
    final var pathAndQuery = String.format(quotePathFormat, amount, query);
    final var request = newRequest(pathAndQuery, requestTimeout).GET().build();
    return this.httpClient.sendAsync(request, ofByteArray()).thenApply(quoteParser);
  }

  @Override
  public CompletableFuture<JupiterQuote> getQuote(final String query, final Duration requestTimeout) {
    final var request = newRequest(quotePath + query, requestTimeout).GET().build();
    return this.httpClient.sendAsync(request, ofByteArray()).thenApply(quoteParser);
  }

  @Override
  public CompletableFuture<JupiterUltraOrder> ultraOrder(final BigInteger amount,
                                                         final String query,
                                                         final Duration requestTimeout) {
    final var pathAndQuery = String.format(ultraOrderPathFormat, amount, query);
    final var request = newRequest(endpoint.resolve(pathAndQuery)).GET().build();
    return this.httpClient.sendAsync(request, ofByteArray()).thenApply(ultraOrderParser);
  }

  @Override
  public CompletableFuture<JupiterExecuteOrder> executeOrder(final String base64SignedTx, final String requestId) {
    return sendPostRequest(executeUltraOrderURI, executeUltraOrderParser, String.format("""
                {
                 "signedTransaction": "%s",
                 "requestId": "%s"
                }""",
            base64SignedTx, requestId
        )
    );
  }

  @Override
  public CompletableFuture<List<MarketRecord>> getMarketCache() {
    final var request = HttpRequest
        .newBuilder(URI.create("https://cache.jup.ag/markets?v=4"))
        .header("Content-Type", "application/json")
        .build();
    return httpClient.sendAsync(request, ofByteArray()).thenApply(wrapResponseParser(MARKET_CACHE_PARSER));
  }

  @Override
  public CompletableFuture<ClaimAsrProof> claimAsrProof(final PublicKey vault,
                                                        final String account,
                                                        final SequencedCollection<PublicKey> mints) {
    final var body = mints.stream().map(PublicKey::toBase58).collect(Collectors.joining(
        ",", String.format("/asr-claim-proof/%s?asrTimeline=%s&mints=", vault, account), ""
    ));
    final var request = newRequest(workerURI.resolve(body)).build();
    return httpClient.sendAsync(request, ofByteArray()).thenApply(ASR_PROOF);
  }
}
