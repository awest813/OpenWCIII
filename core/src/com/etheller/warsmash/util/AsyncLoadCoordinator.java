package com.etheller.warsmash.util;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Coordinates a two-stage loading pipeline:
 * <ol>
 * <li>background prefetch work represented by a {@link Future}</li>
 * <li>main-thread staged work represented by {@link MainThreadStage}</li>
 * </ol>
 *
 * <p>
 * The {@link #process()} call is designed for frame-loop usage and does not
 * block while prefetch is still running.
 * </p>
 */
public final class AsyncLoadCoordinator<T> implements AutoCloseable {

	@FunctionalInterface
	public interface MainThreadStageFactory<T> {
		MainThreadStage create(T prefetchResult) throws IOException;
	}

	public interface MainThreadStage extends AutoCloseable {
		boolean process() throws IOException;

		float getCompletionRatio();

		@Override
		default void close() throws IOException {
		}
	}

	private final Future<T> prefetchFuture;
	private final Supplier<Float> prefetchProgressSupplier;
	private final float prefetchWeight;
	private final MainThreadStageFactory<T> mainThreadStageFactory;

	private MainThreadStage mainThreadStage;
	private boolean complete;
	private IOException failure;

	public AsyncLoadCoordinator(final Future<T> prefetchFuture, final Supplier<Float> prefetchProgressSupplier,
			final float prefetchWeight, final MainThreadStageFactory<T> mainThreadStageFactory) {
		this.prefetchFuture = prefetchFuture;
		this.prefetchProgressSupplier = prefetchProgressSupplier;
		this.prefetchWeight = clamp01(prefetchWeight);
		this.mainThreadStageFactory = mainThreadStageFactory;
	}

	public boolean process() throws IOException {
		if (this.failure != null) {
			throw this.failure;
		}
		if (this.complete) {
			return true;
		}
		if (this.mainThreadStage == null) {
			if (!this.prefetchFuture.isDone()) {
				return false;
			}
			try {
				final T prefetchResult = this.prefetchFuture.get();
				this.mainThreadStage = this.mainThreadStageFactory.create(prefetchResult);
			}
			catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				this.failure = new IOException("Interrupted while waiting for async prefetch stage", e);
				throw this.failure;
			}
			catch (final ExecutionException e) {
				this.failure = wrapFailure("Async prefetch stage failed", e.getCause() != null ? e.getCause() : e);
				throw this.failure;
			}
		}
		this.complete = this.mainThreadStage.process();
		return this.complete;
	}

	public float getCompletionRatio() {
		if (this.complete) {
			return 1.0f;
		}
		final float prefetchProgress = clamp01(this.prefetchProgressSupplier.get());
		if (this.mainThreadStage == null) {
			return this.prefetchWeight * prefetchProgress;
		}
		return this.prefetchWeight + ((1.0f - this.prefetchWeight) * clamp01(this.mainThreadStage.getCompletionRatio()));
	}

	@Override
	public void close() throws IOException {
		if ((this.prefetchFuture != null) && !this.prefetchFuture.isDone()) {
			this.prefetchFuture.cancel(true);
		}
		if (this.mainThreadStage != null) {
			this.mainThreadStage.close();
			this.mainThreadStage = null;
		}
	}

	private static IOException wrapFailure(final String message, final Throwable cause) {
		if (cause instanceof IOException) {
			return (IOException) cause;
		}
		return new IOException(message, cause);
	}

	private static float clamp01(final float value) {
		return Math.max(0.0f, Math.min(1.0f, value));
	}
}
