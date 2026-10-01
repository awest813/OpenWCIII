package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

/** Profile ownership for disk gamecaches; legacy shared files are preserved. */
public final class ProfileGameCacheStore {
	private static final String LEGACY_OWNER = ".legacy-owner";
	private static final String LEGACY_IMPORTED = ".legacy-imported";

	private ProfileGameCacheStore() { }

	public static File defaultRoot() {
		return new File(new File(System.getProperty("user.home"), ".warsmash"), "gamecache");
	}

	private static String profileId(final String profile) {
		// Fixed length, no path characters, and no case-only aliases on Windows filesystems.
		try {
			final byte[] digest = MessageDigest.getInstance("SHA-256").digest(profile.getBytes(StandardCharsets.UTF_8));
			final StringBuilder id = new StringBuilder("p-");
			for (final byte value : digest) id.append(String.format(Locale.ROOT, "%02x", value & 0xff));
			return id.toString();
		}
		catch (final NoSuchAlgorithmException impossible) {
			throw new IllegalStateException(impossible);
		}
	}

	/** Pure path resolution shared by profile-owned persistence stores. */
	public static File profileDirectory(final File root, final String profile) {
		return new File(new File(root, "profiles"), profileId(profile));
	}

	/** Claim before the user can switch/create profiles, assigning old caches only once. */
	public static void claimLegacyOwner(final File root, final String profile) throws IOException {
		Files.createDirectories(root.toPath());
		try {
			Files.writeString(new File(root, LEGACY_OWNER).toPath(), profileId(profile),
					StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
		}
		catch (final FileAlreadyExistsException alreadyClaimed) {
			// A later profile must never take ownership of the shared legacy files.
		}
	}

	public static File prepareProfile(final File root, final String profile) throws IOException {
		claimLegacyOwner(root, profile);
		final File directory = profileDirectory(root, profile);
		Files.createDirectories(directory.toPath());
		final File imported = new File(root, LEGACY_IMPORTED);
		final boolean complete = imported.isFile()
				&& profileId(profile).equals(Files.readString(imported.toPath(), StandardCharsets.UTF_8));
		if (!complete && ownsLegacy(root, profile)) {
			final File[] files = root.listFiles();
			if (files == null) throw new IOException("Cannot list legacy gamecaches: " + root);
			for (final File file : files) {
				if (!file.isFile() || file.getName().equals(LEGACY_OWNER)
						|| file.getName().equals(LEGACY_IMPORTED)) continue;
				final File destination = new File(directory, file.getName());
				if (!destination.exists()) copyLegacyFile(file, destination);
			}
			Files.writeString(imported.toPath(), profileId(profile), StandardCharsets.UTF_8);
		}
		return directory;
	}

	private static void copyLegacyFile(final File source, final File destination) throws IOException {
		final Path temporary = Files.createTempFile(destination.getParentFile().toPath(), "legacy-", ".tmp");
		try {
			Files.copy(source.toPath(), temporary, StandardCopyOption.REPLACE_EXISTING);
			Files.move(temporary, destination.toPath());
		}
		finally {
			Files.deleteIfExists(temporary);
		}
	}

	private static boolean ownsLegacy(final File root, final String profile) throws IOException {
		final File owner = new File(root, LEGACY_OWNER);
		if (!owner.exists()) return false;
		final String id = Files.readString(owner.toPath(), StandardCharsets.UTF_8);
		if (!id.matches("p-[a-f0-9]{64}")) throw new IOException("Invalid legacy gamecache ownership record: " + owner);
		return profileId(profile).equals(id);
	}

	/** Retire caches on profile deletion so recreating the name starts fresh; keep recovery possible. */
	public static void retireProfile(final File root, final String profile) throws IOException {
		if (!root.exists()) return;
		if (ownsLegacy(root, profile)) {
			Files.writeString(new File(root, LEGACY_IMPORTED).toPath(), profileId(profile), StandardCharsets.UTF_8);
		}
		final File directory = profileDirectory(root, profile);
		if (directory.exists()) {
			final File retired = new File(root, "retired");
			Files.createDirectories(retired.toPath());
			Files.move(directory.toPath(), new File(retired, profileId(profile) + "-" + UUID.randomUUID()).toPath());
		}
	}
}
