package software.sava.solana.web2.sanctum.client.http;

import software.sava.rpc.json.http.client.JsonHttpClient;
import software.sava.solana.web2.sanctum.client.http.request.SwapMode;
import software.sava.solana.web2.sanctum.client.http.response.SanctumQuote;
import software.sava.solana.web2.sanctum.client.http.response.StakePoolContext;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static software.sava.core.util.LamportDecimal.LAMPORT_DIGITS;

final class SanctumHttpClient extends JsonHttpClient implements SanctumClient {

  static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(13);

  private static final Function<HttpResponse<?>, Map<String, BigDecimal>> SOL_VALUE_PARSER = applyGenericResponse(ji -> {
    final var prices = new HashMap<String, BigDecimal>();
    ji.skipUntil("solValues");
    for (String symbolOrMint; (symbolOrMint = ji.readObjField()) != null; ) {
      prices.put(symbolOrMint, ji.readBigDecimal().movePointLeft(9).stripTrailingZeros());
    }
    return prices;
  });
  private static final Function<HttpResponse<?>, Map<String, BigDecimal>> PRICE_PARSER = applyGenericResponse(ji -> {
    final var prices = new HashMap<String, BigDecimal>();
    String mint;
    BigDecimal price;
    ji.skipUntil("prices");
    for (int mark; ji.readArray(); ) {
      mark = ji.mark();
      mint = ji.skipUntil("mint").readString();
      if (ji.skipUntil("amount") == null) {
        ji.reset(mark).skipUntil("amount");
      }
      price = ji.readBigDecimal().movePointLeft(LAMPORT_DIGITS).stripTrailingZeros();
      prices.put(mint, price);
      ji.skipRestOfObject();
    }
    return prices;
  });
  private static final Function<HttpResponse<?>, SanctumQuote> QUOTE_PARSER = applyGenericResponse(SanctumQuote::parse);
  private static final Function<HttpResponse<?>, byte[]> SWAP_PARSER = applyGenericResponse(ji -> ji.skipUntil("tx").decodeBase64String());

  private final URI swapURI;
  private final URI extraApiEndpoint;

  SanctumHttpClient(final URI apiEndpoint,
                    final URI extraApiEndpoint,
                    final HttpClient httpClient,
                    final Duration requestTimeout,
                    final UnaryOperator<HttpRequest.Builder> extendRequest,
                    final Predicate<HttpResponse<byte[]>> applyResponse) {
    super(apiEndpoint, httpClient, requestTimeout, extendRequest, applyResponse, null);
    this.swapURI = apiEndpoint.resolve("/v1/swap");
    this.extraApiEndpoint = extraApiEndpoint;
  }

  @Override
  public CompletableFuture<Map<String, BigDecimal>> price(final Collection<String> tokenMints) {
    final var pathQuery = tokenMints.stream().collect(Collectors.joining("&input=", "/v1/price?input=", ""));
    return sendGetRequest(PRICE_PARSER, pathQuery);
  }

  @Override
  public CompletableFuture<SanctumQuote> quote(final String inputMint,
                                               final String outputMint,
                                               final BigInteger amount,
                                               final SwapMode swapMode) {
    return sendGetRequest(QUOTE_PARSER, String.format(
            "/v1/swap/quote?input=%s&outputLstMint=%s&amount=%s&mode=%s",
            inputMint, outputMint, amount, swapMode
        )
    );
  }

  @Override
  public CompletableFuture<byte[]> swap(final String postBody) {
    return sendPostRequest(swapURI, SWAP_PARSER, postBody);
  }

  @Override
  public CompletableFuture<Map<String, BigDecimal>> solValue(final Collection<String> tokenMints) {
    final var pathQuery = tokenMints.stream().collect(Collectors.joining("&lst=", "/v1/sol-value/current?lst=", ""));
    return sendGetRequest(extraApiEndpoint.resolve(pathQuery), SOL_VALUE_PARSER);
  }

  @Override
  public CompletableFuture<List<StakePoolContext>> fetchSanctumLstList() {
    final var request = HttpRequest.newBuilder(URI.create("https://raw.githubusercontent.com/igneous-labs/sanctum-lst-list/master/sanctum-lst-list.toml")).build();
    final var fetchFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    return fetchFuture.thenApply(response -> StakePoolContext.parse(response.body()));
  }
}
