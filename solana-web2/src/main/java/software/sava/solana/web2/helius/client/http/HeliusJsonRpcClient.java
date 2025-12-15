package software.sava.solana.web2.helius.client.http;

import software.sava.core.accounts.PublicKey;
import software.sava.core.rpc.Filter;
import software.sava.rpc.json.http.client.BaseSolanaJsonRpcClient;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNullElse;
import static software.sava.core.rpc.Filter.MAX_MEM_COMP_LENGTH;
import static software.sava.solana.web2.helius.client.http.request.PriorityFeeRequest.serializeParams;
import static software.sava.solana.web2.helius.client.http.request.PriorityFeeRequest.serializeRecommendedParams;

final class HeliusJsonRpcClient extends BaseSolanaJsonRpcClient implements HeliusClient {

  static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(13);
  static final Duration PROGRAM_ACCOUNTS_TIMEOUT = Duration.ofSeconds(120);

  private static final Function<HttpResponse<?>, PriorityFeesEstimates> PRIORITY_FEES = applyGenericResponseResult(PriorityFeesEstimates::parseLevels);
  private static final Function<HttpResponse<?>, BigDecimal> PRIORITY_FEE = applyGenericResponseResult(ji -> ji.skipUntil("priorityFeeEstimate").readBigDecimalDropZeroes());

  private final AtomicLong id;

  HeliusJsonRpcClient(final URI endpoint,
                      final HttpClient httpClient,
                      final Duration requestTimeout,
                      final UnaryOperator<HttpRequest.Builder> extendRequest,
                      final Predicate<HttpResponse<byte[]>> applyResponse) {
    super(endpoint, httpClient, requestTimeout, extendRequest, applyResponse, null, Commitment.CONFIRMED);
    this.id = new AtomicLong(System.currentTimeMillis());
  }

  @Override
  public CompletableFuture<PriorityFeesEstimates> getPriorityFeeEstimate(final String params) {
    return sendPostRequest(PRIORITY_FEES, String.format("""
                {"jsonrpc":"2.0","id":%d,"method":"getPriorityFeeEstimate","params":[{%s}]}""",
            id.incrementAndGet(), params
        )
    );
  }

  @Override
  public CompletableFuture<PriorityFeesEstimates> getPriorityFeeEstimate(final List<String> accountKeys) {
    return getPriorityFeeEstimate(serializeParams(accountKeys));
  }

  @Override
  public CompletableFuture<PriorityFeesEstimates> getPriorityFeeEstimate(final List<String> accountKeys,
                                                                         final int lookBackSlots) {
    return getPriorityFeeEstimate(serializeParams(accountKeys, lookBackSlots));
  }

  @Override
  public CompletableFuture<PriorityFeesEstimates> getTransactionPriorityFeeEstimate(final String transaction) {
    return getPriorityFeeEstimate(serializeParams(transaction));
  }

  @Override
  public CompletableFuture<PriorityFeesEstimates> getTransactionPriorityFeeEstimate(final String transaction,
                                                                                    final Encoding transactionEncoding) {
    return getPriorityFeeEstimate(serializeParams(transaction, transactionEncoding));
  }

  @Override
  public CompletableFuture<PriorityFeesEstimates> getTransactionPriorityFeeEstimate(final String transaction,
                                                                                    final Encoding transactionEncoding,
                                                                                    final int lookBackSlots) {
    return getPriorityFeeEstimate(serializeParams(transaction, transactionEncoding, lookBackSlots));
  }

  @Override
  public CompletableFuture<PriorityFeesEstimates> getTransactionPriorityFeeEstimate(final String transaction,
                                                                                    final int lookBackSlots) {
    return getPriorityFeeEstimate(serializeParams(transaction, lookBackSlots));
  }

  @Override
  public CompletableFuture<BigDecimal> getRecommendedPriorityFeeEstimate(final String params) {
    final var body = String.format("""
            {"jsonrpc":"2.0","id":%d,"method":"getPriorityFeeEstimate","params":[{%s}]}""",
        id.incrementAndGet(), params
    );
    return sendPostRequest(PRIORITY_FEE, body);
  }

  @Override
  public CompletableFuture<BigDecimal> getRecommendedPriorityFeeEstimate(final List<String> accountKeys) {
    return getRecommendedPriorityFeeEstimate(serializeRecommendedParams(accountKeys));
  }

  @Override
  public CompletableFuture<BigDecimal> getRecommendedTransactionPriorityFeeEstimate(final String transaction) {
    return getRecommendedPriorityFeeEstimate(serializeRecommendedParams(transaction));
  }

  @Override
  public CompletableFuture<BigDecimal> getRecommendedTransactionPriorityFeeEstimate(final String transaction,
                                                                                    final Encoding transactionEncoding) {
    return getRecommendedPriorityFeeEstimate(serializeRecommendedParams(transaction, transactionEncoding));
  }

  @Override
  public <T> CompletableFuture<List<AccountInfo<T>>> getProgramAccounts(final Duration requestTimeout,
                                                                        final PublicKey programId,
                                                                        final Commitment commitment,
                                                                        final BigInteger minContextSlot,
                                                                        final Collection<Filter> filters,
                                                                        final int length,
                                                                        final int offset,
                                                                        final String paginationKey,
                                                                        final int limit,
                                                                        final BigInteger changedSinceSlot,
                                                                        final BiFunction<PublicKey, byte[], T> factory) {
    final int numFilters = filters == null ? 0 : filters.size();
    final var builder = new StringBuilder(256 + (numFilters * MAX_MEM_COMP_LENGTH));

    builder.append("""
        {"jsonrpc":"2.0","id":""");
    builder.append(id.incrementAndGet());

    builder.append("""
        ,"method":"getProgramAccountsV2","params":[\"""");
    builder.append(programId.toBase58());

    builder.append("""
        ",{"withContext":true,"encoding":"base64","commitment":\"""");
    builder.append(commitment.getValue());

    if (paginationKey != null && !paginationKey.isBlank()) {
      builder.append("""
          ","paginationKey":\"""");
      builder.append(paginationKey);
    }
    builder.append('"');

    builder.append("""
        ,"limit":""");
    builder.append(limit);

    if (minContextSlot != null) {
      builder.append("""
          ,"minContextSlot":""");
      builder.append(minContextSlot);
    }

    if (changedSinceSlot != null) {
      builder.append("""
          ,"changedSinceSlot":""");
      builder.append(changedSinceSlot);
    }

    if (length != 0) {
      builder.append("""
          ,"dataSlice":{"length":""");
      builder.append(length);
      builder.append("""
          ,"offset":""");
      builder.append(offset);
      builder.append('}');
    }

    if (numFilters == 0) {
      builder.append("}]}");
    } else {
      builder.append("""
          ,"filters":[""");
      final var iterator = filters.iterator();
      for (Filter filter; ; ) {
        filter = iterator.next();
        builder.append(filter.toJson());
        if (iterator.hasNext()) {
          builder.append(',');
        } else {
          break;
        }
      }
      builder.append("]}]}");
    }

    final var body = builder.toString();
    return sendPostRequest(
        applyGenericResponseValue((ji, context) -> AccountInfo.parseAccounts(ji, context, factory)),
        requireNonNullElse(requestTimeout, this.requestTimeout),
        body
    );
  }
}
