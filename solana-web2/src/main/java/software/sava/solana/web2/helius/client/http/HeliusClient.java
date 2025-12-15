package software.sava.solana.web2.helius.client.http;

import software.sava.core.accounts.PublicKey;
import software.sava.core.rpc.Filter;
import software.sava.core.tx.Transaction;
import software.sava.rpc.json.http.request.Commitment;
import software.sava.rpc.json.http.response.AccountInfo;
import software.sava.solana.web2.helius.client.http.request.Encoding;
import software.sava.solana.web2.helius.client.http.response.PriorityFeesEstimates;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static software.sava.solana.web2.helius.client.http.HeliusJsonRpcClient.DEFAULT_REQUEST_TIMEOUT;

public interface HeliusClient {

  static HeliusClient createHttpClient(final URI endpoint,
                                       final HttpClient httpClient,
                                       final Duration requestTimeout,
                                       final UnaryOperator<HttpRequest.Builder> extendRequest,
                                       final Predicate<HttpResponse<byte[]>> applyResponse) {
    return new HeliusJsonRpcClient(endpoint, httpClient, requestTimeout, extendRequest, applyResponse);
  }

  static HeliusClient createHttpClient(final URI endpoint,
                                       final HttpClient httpClient,
                                       final UnaryOperator<HttpRequest.Builder> extendRequest,
                                       final Predicate<HttpResponse<byte[]>> applyResponse) {
    return createHttpClient(endpoint, httpClient, DEFAULT_REQUEST_TIMEOUT, extendRequest, applyResponse);
  }

  static HeliusClient createHttpClient(final URI endpoint,
                                       final HttpClient httpClient,
                                       final Predicate<HttpResponse<byte[]>> applyResponse) {
    return createHttpClient(endpoint, httpClient, DEFAULT_REQUEST_TIMEOUT, null, applyResponse);
  }

  static HeliusClient createHttpClient(final URI endpoint,
                                       final HttpClient httpClient,
                                       final Duration requestTimeout) {
    return new HeliusJsonRpcClient(endpoint, httpClient, requestTimeout, null, null);
  }

  static HeliusClient createHttpClient(final URI endpoint, final HttpClient httpClient) {
    return createHttpClient(endpoint, httpClient, DEFAULT_REQUEST_TIMEOUT);
  }

  URI endpoint();

  Commitment defaultCommitment();

  CompletableFuture<PriorityFeesEstimates> getPriorityFeeEstimate(final String params);

  CompletableFuture<PriorityFeesEstimates> getPriorityFeeEstimate(final List<String> accountKeys);

  CompletableFuture<PriorityFeesEstimates> getPriorityFeeEstimate(final List<String> accountKeys,
                                                                  final int lookBackSlots);

  CompletableFuture<PriorityFeesEstimates> getTransactionPriorityFeeEstimate(final String transaction);

  CompletableFuture<PriorityFeesEstimates> getTransactionPriorityFeeEstimate(final String transaction,
                                                                             final Encoding transactionEncoding);

  CompletableFuture<PriorityFeesEstimates> getTransactionPriorityFeeEstimate(final String transaction,
                                                                             final Encoding transactionEncoding,
                                                                             final int lookBackSlots);

  CompletableFuture<PriorityFeesEstimates> getTransactionPriorityFeeEstimate(final String transaction,
                                                                             final int lookBackSlots);

  default CompletableFuture<PriorityFeesEstimates> getTransactionPriorityFeeEstimate(final Transaction transaction) {
    return getTransactionPriorityFeeEstimate(transaction.base64EncodeToString(), Encoding.base64);
  }

  default CompletableFuture<PriorityFeesEstimates> getTransactionPriorityFeeEstimate(final Transaction transaction,
                                                                                     final int lookBackSlots) {
    return getTransactionPriorityFeeEstimate(transaction.base64EncodeToString(), Encoding.base64, lookBackSlots);
  }

  CompletableFuture<BigDecimal> getRecommendedPriorityFeeEstimate(final String params);

  CompletableFuture<BigDecimal> getRecommendedPriorityFeeEstimate(final List<String> accountKeys);

  CompletableFuture<BigDecimal> getRecommendedTransactionPriorityFeeEstimate(final String transaction);

  CompletableFuture<BigDecimal> getRecommendedTransactionPriorityFeeEstimate(final String transaction,
                                                                             final Encoding transactionEncoding);

  <T> CompletableFuture<List<AccountInfo<T>>> getProgramAccounts(final Duration requestTimeout,
                                                                 final PublicKey programId,
                                                                 final Commitment commitment,
                                                                 final BigInteger minContextSlot,
                                                                 final Collection<Filter> filters,
                                                                 final int length,
                                                                 final int offset,
                                                                 final String paginationKey,
                                                                 final int limit,
                                                                 final BigInteger changedSinceSlot,
                                                                 final BiFunction<PublicKey, byte[], T> factory);
}
