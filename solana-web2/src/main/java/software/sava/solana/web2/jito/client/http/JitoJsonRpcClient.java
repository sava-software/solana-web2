package software.sava.solana.web2.jito.client.http;

import software.sava.core.encoding.Base58;
import software.sava.rpc.json.http.client.BaseSolanaJsonRpcClient;
import software.sava.rpc.json.http.request.Commitment;
import software.sava.solana.web2.jito.client.http.response.BundleStatus;
import software.sava.solana.web2.jito.client.http.response.SendTxResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static systems.comodal.jsoniter.ValueType.STRING;

final class JitoJsonRpcClient extends BaseSolanaJsonRpcClient implements JitoClient {

  private static final Function<HttpResponse<?>, BundleStatus> BUNDLE_STATUS_PARSER = applyGenericResponseValue(BundleStatus::parseStatus);
  private static final Function<HttpResponse<?>, Map<String, BundleStatus>> BUNDLE_STATUSES_PARSER = applyGenericResponseValue(BundleStatus::parseStatuses);
  private static final Function<HttpResponse<?>, SendTxResult> SEND_TX_RESPONSE_PARSER = applyGenericResponseResult(
      (response, _, ji) -> new SendTxResult(ji.readString(), response.headers().firstValue("x-bundle-id").orElse(null)));
  private static final Function<HttpResponse<?>, String> SEND_BUNDLE_RESPONSE_PARSER = applyGenericResponseResult(
      ji -> ji.whatIsNext() == STRING ? ji.readString() : null);
  private static final Function<HttpResponse<?>, List<String>> TIP_ACCOUNTS_PARSER = applyGenericResponseResult(
      ji -> {
        final var tipAccount = new ArrayList<String>();
        while (ji.readArray()) {
          tipAccount.add(ji.readString());
        }
        return List.copyOf(tipAccount);
      });

  private final URI bundlesURI;
  private final URI transactionsURI;
  private final URI bundlyOnlyTxURI;

  private JitoJsonRpcClient(final URI endpoint,
                            final HttpClient httpClient,
                            final Duration requestTimeout,
                            final UnaryOperator<HttpRequest.Builder> extendRequest,
                            final Predicate<HttpResponse<byte[]>> applyResponse,
                            final Commitment defaultCommitment) {
    super(
        endpoint,
        httpClient,
        requestTimeout,
        extendRequest,
        applyResponse,
        null,
        defaultCommitment
    );
    this.bundlesURI = endpoint.resolve("/api/v1/bundles");
    this.transactionsURI = endpoint.resolve("/api/v1/transactions");
    this.bundlyOnlyTxURI = this.transactionsURI.resolve("/api/v1/transactions?bundleOnly=true");
  }

  static JitoJsonRpcClient createClient(final URI endpoint,
                                        final HttpClient httpClient,
                                        final Duration requestTimeout,
                                        final UnaryOperator<HttpRequest.Builder> extendRequest,
                                        final Predicate<HttpResponse<byte[]>> applyResponse,
                                        final Commitment defaultCommitment,
                                        final String apiAuthKey) {
    final UnaryOperator<HttpRequest.Builder> _extendRequest;
    if (apiAuthKey != null) {
      _extendRequest = r -> extendRequest.apply(r.header("x-jito-auth", apiAuthKey));
    } else {
      _extendRequest = extendRequest;
    }
    return new JitoJsonRpcClient(
        endpoint,
        httpClient,
        requestTimeout,
        _extendRequest,
        applyResponse,
        defaultCommitment
    );
  }

  @Override
  public CompletableFuture<List<String>> getTipAccounts() {
    return sendPostRequest(bundlesURI, TIP_ACCOUNTS_PARSER, format("""
            {"jsonrpc":"2.0","id":%d,"method":"getTipAccounts","params":[]}""", id.incrementAndGet()
        )
    );
  }

  @Override
  public CompletableFuture<BundleStatus> getBundleStatus(final String bundleId) {
    return sendPostRequest(bundlesURI, BUNDLE_STATUS_PARSER, format("""
            {"jsonrpc":"2.0","id":%d,"method":"getBundleStatuses","params":[["%s"]]}""", id.incrementAndGet(), bundleId
        )
    );
  }

  @Override
  public CompletableFuture<Map<String, BundleStatus>> getBundleStatuses(final Collection<String> bundleIds) {
    return sendPostRequest(bundlesURI, BUNDLE_STATUSES_PARSER, format("""
            {"jsonrpc":"2.0","id":%d,"method":"getBundleStatuses","params":[["%s"]]}""", id.incrementAndGet(), String.join("\",\"", bundleIds)
        )
    );
  }

  @Override
  public CompletableFuture<SendTxResult> sendBundleOnly(final Commitment preflightCommitment,
                                                        final String base64SignedTx,
                                                        final int maxRetries) {
    return sendPostRequest(bundlyOnlyTxURI, SEND_TX_RESPONSE_PARSER, format("""
                {"jsonrpc":"2.0","id":%d,"method":"sendTransaction","params":["%s",{"encoding":"base64","skipPreflight":true,"preflightCommitment":"%s","maxRetries":%d}]}""",
            id.incrementAndGet(), base64SignedTx, preflightCommitment.getValue(), maxRetries
        )
    );
  }

  @Override
  public CompletableFuture<SendTxResult> sendTransactionSkipPreflight(final Commitment preflightCommitment,
                                                                      final String base64SignedTx,
                                                                      final int maxRetries) {
    return sendPostRequest(transactionsURI, SEND_TX_RESPONSE_PARSER, format("""
                {"jsonrpc":"2.0","id":%d,"method":"sendTransaction","params":["%s",{"encoding":"base64","skipPreflight":true,"preflightCommitment":"%s","maxRetries":%d}]}""",
            id.incrementAndGet(), base64SignedTx, preflightCommitment.getValue(), maxRetries
        )
    );
  }

  @Override
  public CompletableFuture<SendTxResult> sendTransaction(final Commitment preflightCommitment,
                                                         final String base64SignedTx,
                                                         final int maxRetries) {
    return sendPostRequest(transactionsURI, SEND_TX_RESPONSE_PARSER, format("""
                {"jsonrpc":"2.0","id":%d,"method":"sendTransaction","params":["%s",{"encoding":"base64","preflightCommitment":"%s","maxRetries":%d}]}""",
            id.incrementAndGet(), base64SignedTx, preflightCommitment.getValue(), maxRetries
        )
    );
  }

  @Override
  public CompletableFuture<String> sendBundle(final String base58SignedTransactions) {
    final var body = String.format("""
        {"jsonrpc":"2.0","id":%d,"method":"sendBundle","params":[["%s"]]}""", id.incrementAndGet(), base58SignedTransactions
    );
    return sendPostRequest(transactionsURI, SEND_BUNDLE_RESPONSE_PARSER, body);
  }

  @Override
  public CompletableFuture<String> sendBundle(final byte[] signedTransaction) {
    return sendBundle(Base58.encode(signedTransaction));
  }

  @Override
  public CompletableFuture<String> sendBundle(final Collection<String> base58SignedTransactions) {
    return sendBundle(String.join("\",\"", base58SignedTransactions));
  }

  @Override
  public CompletableFuture<String> sendBundle(final String[] base58SignedTransactions) {
    return sendBundle(String.join("\",\"", base58SignedTransactions));
  }

  @Override
  public CompletableFuture<String> sendBundle(final byte[][] signedTransactions) {
    return sendBundle(Arrays.stream(signedTransactions).map(Base58::encode).collect(Collectors.joining("\",\"")));
  }

  @Override
  public CompletableFuture<String> sendBundleBytes(final Collection<byte[]> signedTransactions) {
    return sendBundle(signedTransactions.stream().map(Base58::encode).collect(Collectors.joining("\",\"")));
  }
}
