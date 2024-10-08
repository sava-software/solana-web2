package software.sava.solana.web2.jupiter.client.http;

import software.sava.core.accounts.PublicKey;
import software.sava.rpc.json.http.client.JsonHttpClient;
import software.sava.solana.web2.jupiter.client.http.request.JupiterQuoteRequest;
import software.sava.solana.web2.jupiter.client.http.request.JupiterTokenTag;
import software.sava.solana.web2.jupiter.client.http.response.*;

import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.net.http.HttpResponse.BodyHandlers.ofByteArray;
import static software.sava.rpc.json.PublicKeyEncoding.PARSE_BASE58_PUBLIC_KEY;
import static software.sava.rpc.json.http.client.JsonResponseController.checkResponseCode;

final class JupiterHttpClient extends JsonHttpClient implements JupiterClient {

  static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(13);

  private static final Function<HttpResponse<byte[]>, TokenContext> TOKEN = applyResponse(TokenContext::parseToken);
  private static final Function<HttpResponse<byte[]>, Map<PublicKey, TokenContext>> TOKEN_LIST = applyResponse(TokenContext::parseList);
  private static final Function<HttpResponse<byte[]>, JupiterQuote> QUOTE_PARSER = applyResponse(JupiterQuote::parse);
  private static final Function<HttpResponse<byte[]>, JupiterSwapTx> SWAP_TX = applyResponse(JupiterSwapTx::parse);
  private static final Function<HttpResponse<byte[]>, byte[]> SWAP_INSTRUCTIONS_TX = response -> {
    checkResponseCode(response);
    return response.body();
  };
  private static final Function<HttpResponse<byte[]>, Map<String, PublicKey>> PROGRAM_LABEL_PARSER = applyResponse(ji -> {
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


  public static void main(String[] args) {
    final var client = JupiterClient.createClient(HttpClient.newHttpClient());
    final var tokens = client.verifiedTokenMap().join();
    final var sol = tokens.values().stream()
        .filter(tokenContext -> tokenContext.symbol().equals("SOL"))
        .findFirst().orElseThrow();
    final var usdc = tokens.values().stream()
        .filter(tokenContext -> tokenContext.symbol().equals("USDC"))
        .findFirst().orElseThrow();
    final var quoteRequest = JupiterQuoteRequest.buildRequest()
        .swapMode(SwapMode.ExactIn)
        .amount(usdc.fromDecimal(BigDecimal.ONE).toBigInteger())
        .inputTokenMint(usdc.address())
        .outputTokenMint(sol.address())
        .slippageBps(2)
        .allowDexes(List.of(
            "1DEX",
            "Cropper",
            "Meteora",
            "Meteora DLMM",
            "Orca V2",
            "OpenBook V2",
            "Phoenix",
            "Raydium",
            "Raydium CLMM",
            "Raydium CP",
            "Saber",
            "Saber (Decimals)",
            "Sanctum",
            "Sanctum Infinity",
            "Whirlpool"
        ))
        .onlyDirectRoutes(true)
        .create();

    final var quote = client.getQuote(quoteRequest).join();
    System.out.println(quote);
  }

  private static final Function<HttpResponse<byte[]>, List<MarketRecord>> MARKET_CACHE_PARSER = applyResponse(MarketRecord::parse);

  private final URI tokensEndpoint;
  private final URI tokensPath;
  private final URI tokensWithMarketsPath;
  private final String quotePathFormat;
  private final String quotePath;
  private final URI swapURI;
  private final URI swapInstructionsURI;
  private final HttpRequest programLabelsRequest;

  JupiterHttpClient(final URI quoteEndpoint,
                    final URI tokensEndpoint,
                    final HttpClient httpClient,
                    final Duration requestTimeout,
                    final Predicate<HttpResponse<byte[]>> applyResponse) {
    super(quoteEndpoint, httpClient, requestTimeout, applyResponse);
    this.tokensEndpoint = tokensEndpoint;
    this.tokensPath = tokensEndpoint.resolve("/tokens");
    this.tokensWithMarketsPath = tokensPath.resolve("tokens_with_markets");
    try {
      final var inetAddress = InetAddress.getByName(quoteEndpoint.getHost());
      if (inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress()) {
        this.quotePathFormat = "/quote?amount=%s&%s";
        this.quotePath = "/quote?";
        this.swapURI = quoteEndpoint.resolve("/swap");
        this.swapInstructionsURI = quoteEndpoint.resolve("/swap-instructions");
        this.programLabelsRequest = newGetRequest(quoteEndpoint.resolve("/program-id-to-label")).build();
      } else {
        this.quotePathFormat = "/v6/quote?amount=%s&%s";
        this.quotePath = "/v6/quote?";
        this.swapURI = quoteEndpoint.resolve("/v6/swap");
        this.swapInstructionsURI = quoteEndpoint.resolve("/v6/swap-instructions");
        this.programLabelsRequest = newGetRequest(quoteEndpoint.resolve("/v6/program-id-to-label")).build();
      }
    } catch (final UnknownHostException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public CompletableFuture<TokenContext> token(final PublicKey mint) {
    final var url = tokensPath.resolve(mint.toBase58());
    return sendGetRequest(url, TOKEN);
  }

  @Override
  public CompletableFuture<Map<PublicKey, TokenContext>> allTokens() {
    return sendGetRequest(tokensPath, TOKEN_LIST);
  }

  @Override
  public CompletableFuture<Map<PublicKey, TokenContext>> tokenMap(final JupiterTokenTag tag) {
    if (tag == null) {
      return verifiedTokenMap();
    }
    final var url = tokensEndpoint.resolve("/tokens?tags=" + tag.name());
    return sendGetRequest(url, TOKEN_LIST);
  }

  @Override
  public CompletableFuture<Map<PublicKey, TokenContext>> tokenMap(final Collection<JupiterTokenTag> tags) {
    if (tags == null || tags.isEmpty()) {
      return verifiedTokenMap();
    }
    final var url = tokensEndpoint.resolve("/tokens?tags=" + tags.stream()
        .map(JupiterTokenTag::name)
        .collect(Collectors.joining(",")));
    return sendGetRequest(url, TOKEN_LIST);
  }

  @Override
  public CompletableFuture<Map<PublicKey, TokenContext>> tokensWithLiquidMarkets() {
    return sendGetRequest(tokensWithMarketsPath, TOKEN_LIST);
  }

  @Override
  public CompletableFuture<Map<String, PublicKey>> getDexLabelToProgramIdMap() {
    return httpClient.sendAsync(programLabelsRequest, ofByteArray()).thenApply(PROGRAM_LABEL_PARSER);
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
  public CompletableFuture<byte[]> swapInstructions(final StringBuilder jsonBodyBuilder, final JupiterQuote jupiterQuote) {
    return swapInstructions(jsonBodyBuilder, jupiterQuote, requestTimeout);
  }

  @Override
  public CompletableFuture<byte[]> swapInstructions(final StringBuilder jsonBodyBuilder, final byte[] quoteResponseJson) {
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

  private CompletableFuture<JupiterSwapTx> swap(final String jsonBody,
                                                final Duration requestTimeout) {
    return sendPostRequest(swapURI, SWAP_TX, requestTimeout, jsonBody);
  }

  @Override
  public CompletableFuture<byte[]> swapInstructions(final StringBuilder jsonBodyBuilder, final JupiterQuote jupiterQuote, final Duration requestTimeout) {
    return swapInstructions(jsonBodyBuilder, jupiterQuote.quoteResponseJson(), requestTimeout);
  }

  @Override
  public CompletableFuture<byte[]> swapInstructions(final StringBuilder jsonBodyBuilder, final byte[] quoteResponseJson, final Duration requestTimeout) {
    return swapInstructions(jsonBodyBuilder.append(new String(quoteResponseJson)).append('}').toString(), requestTimeout);
  }

  @Override
  public CompletableFuture<byte[]> swapInstructions(final String jsonBodyPrefix, final JupiterQuote jupiterQuote, final Duration requestTimeout) {
    return swapInstructions(jsonBodyPrefix, jupiterQuote.quoteResponseJson(), requestTimeout);
  }

  @Override
  public CompletableFuture<byte[]> swapInstructions(final String jsonBodyPrefix, final byte[] quoteResponseJson, final Duration requestTimeout) {
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
    return this.httpClient.sendAsync(request, ofByteArray()).thenApply(QUOTE_PARSER);
  }

  @Override
  public CompletableFuture<JupiterQuote> getQuote(final String query, final Duration requestTimeout) {
    final var request = newRequest(quotePath + query, requestTimeout).GET().build();
    return this.httpClient.sendAsync(request, ofByteArray()).thenApply(QUOTE_PARSER);
  }

  @Override
  public CompletableFuture<List<MarketRecord>> getMarketCache() {
    final var request = HttpRequest
        .newBuilder(URI.create("https://cache.jup.ag/markets?v=3"))
        .header("Content-Type", "application/json")
        .build();
    return httpClient.sendAsync(request, ofByteArray()).thenApply(MARKET_CACHE_PARSER);
  }
}
