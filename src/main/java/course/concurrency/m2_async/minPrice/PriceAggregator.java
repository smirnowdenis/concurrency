package course.concurrency.m2_async.minPrice;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PriceAggregator {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        var completableFutureList = shopIds.stream().parallel()
                .map(shopId -> CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executorService)
                        .completeOnTimeout(Double.NaN, 2750, TimeUnit.MILLISECONDS)
                        .exceptionally(ex -> Double.NaN))
                .toList();

        CompletableFuture.allOf(completableFutureList.toArray(CompletableFuture[]::new)).join();

        return completableFutureList.stream()
                .map(CompletableFuture::join)
                .filter(d -> !d.isNaN())
                .min(Comparator.naturalOrder())
                .orElse(Double.NaN);
    }
}
