package com.etheller.warsmash.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class AsyncLoadCoordinatorTest {

	@Test
	void prefetchProgressIsWeightedBeforeMainThreadStageStarts() {
		final CompletableFuture<Integer> pendingFuture = new CompletableFuture<>();
		final AsyncLoadCoordinator<Integer> coordinator = new AsyncLoadCoordinator<>(pendingFuture, () -> 0.5f, 0.4f,
				value -> new NoopMainThreadStage());

		assertEquals(0.2f, coordinator.getCompletionRatio(), 0.0001f);
	}

	@Test
	void processHandsOffOnceAndCompletesAfterMainStage() throws IOException {
		final CompletableFuture<Integer> doneFuture = CompletableFuture.completedFuture(7);
		final AtomicInteger factoryCalls = new AtomicInteger(0);
		final AtomicInteger processCalls = new AtomicInteger(0);
		final AtomicReference<Float> stageRatio = new AtomicReference<>(0.0f);

		final AsyncLoadCoordinator<Integer> coordinator = new AsyncLoadCoordinator<>(doneFuture, () -> 1.0f, 0.4f,
				value -> {
					factoryCalls.incrementAndGet();
					return new AsyncLoadCoordinator.MainThreadStage() {
						@Override
						public boolean process() {
							final int calls = processCalls.incrementAndGet();
							if (calls == 1) {
								stageRatio.set(0.25f);
								return false;
							}
							stageRatio.set(1.0f);
							return true;
						}

						@Override
						public float getCompletionRatio() {
							return stageRatio.get();
						}
					};
				});

		assertFalse(coordinator.process());
		assertEquals(0.4f + (0.6f * 0.25f), coordinator.getCompletionRatio(), 0.0001f);
		assertTrue(coordinator.process());
		assertEquals(1.0f, coordinator.getCompletionRatio(), 0.0001f);
		assertEquals(1, factoryCalls.get());
	}

	@Test
	void processDoesNotBlockWhenPrefetchFutureIsPending() throws IOException {
		final CompletableFuture<Integer> pendingFuture = new CompletableFuture<>();
		final AsyncLoadCoordinator<Integer> coordinator = new AsyncLoadCoordinator<>(pendingFuture, () -> 0.0f, 0.4f,
				value -> new NoopMainThreadStage());

		final long startNanos = System.nanoTime();
		final boolean done = coordinator.process();
		final long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;

		assertFalse(done);
		assertTrue(elapsedMs < 50, "process() should return quickly when prefetch is still running");
	}

	@Test
	void prefetchFailureIsSurfacedAsIoException() {
		final CompletableFuture<Integer> failedFuture = new CompletableFuture<>();
		failedFuture.completeExceptionally(new IllegalStateException("boom"));
		final AsyncLoadCoordinator<Integer> coordinator = new AsyncLoadCoordinator<>(failedFuture, () -> 1.0f, 0.5f,
				value -> new NoopMainThreadStage());

		final IOException thrown = assertThrows(IOException.class, coordinator::process);
		assertTrue(thrown.getMessage().contains("Async prefetch stage failed"));
	}

	@Test
	void closeCancelsPendingFutureAndClosesMainThreadStage() throws IOException {
		final CompletableFuture<Integer> pendingFuture = new CompletableFuture<>();
		final AtomicInteger closeCalls = new AtomicInteger(0);
		final AsyncLoadCoordinator<Integer> pendingCoordinator = new AsyncLoadCoordinator<>(pendingFuture, () -> 0.0f,
				0.5f, value -> new NoopMainThreadStage());
		pendingCoordinator.close();
		assertTrue(pendingFuture.isCancelled());

		final CompletableFuture<Integer> doneFuture = CompletableFuture.completedFuture(1);
		final AsyncLoadCoordinator<Integer> finishedCoordinator = new AsyncLoadCoordinator<>(doneFuture, () -> 1.0f,
				0.5f, value -> new AsyncLoadCoordinator.MainThreadStage() {
					@Override
					public boolean process() {
						return false;
					}

					@Override
					public float getCompletionRatio() {
						return 0;
					}

					@Override
					public void close() {
						closeCalls.incrementAndGet();
					}
				});
		assertFalse(finishedCoordinator.process());
		finishedCoordinator.close();
		assertEquals(1, closeCalls.get());
	}

	private static final class NoopMainThreadStage implements AsyncLoadCoordinator.MainThreadStage {
		@Override
		public boolean process() {
			return true;
		}

		@Override
		public float getCompletionRatio() {
			return 1.0f;
		}
	}
}
