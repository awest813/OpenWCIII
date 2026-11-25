package com.etheller.warsmash.viewer5.handlers.w3x.simulation.save;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.UUID;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.ProfileGameCacheStore;

/** Mission save slots belong to a profile, including the Quick Save slot. */
public final class MissionSaveStore {
	private MissionSaveStore() { }
	public static File defaultRoot() {
		return new File(new File(System.getProperty("user.home"), ".warsmash"), "saves");
	}
	public static File currentDirectory() {
		return ProfileGameCacheStore.profileDirectory(defaultRoot(), CampaignProgressStore.get().getProfileName());
	}

	public static File namedSlot(final String enteredName) {
		String name = enteredName.strip();
		if (name.toLowerCase(Locale.ROOT).endsWith(".w3s")) name = name.substring(0, name.length() - 4).strip();
		if (!validComponent(name)) {
			throw new IllegalArgumentException("Enter a save name without path characters.");
		}
		return new File(currentDirectory(), name + ".w3s");
	}

	public static File scriptFile(final File root, final String name) throws IOException {
		return scriptPath(root, name, ".w3s").toFile();
	}

	public static File findScriptFile(final File root, final String name) throws IOException {
		final File file = scriptFile(root, name);
		if (file.isFile()) return file;
		final String previousName = new File(name).getName();
		final Path previousFile = root.toPath().toAbsolutePath().normalize().resolve(previousName + ".w3s");
		if (previousFile.getParent().equals(root.toPath().toAbsolutePath().normalize())
				&& Files.isRegularFile(previousFile, LinkOption.NOFOLLOW_LINKS)) return previousFile.toFile();
		return file;
	}

	public static Path scriptDirectory(final File root, final String name) throws IOException {
		return scriptPath(root, name, "");
	}

	public static boolean removeScriptDirectory(final File root, final String name) throws IOException {
		final Path directory = scriptDirectory(root, name);
		if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return false;
		final Path realDirectory = directory.toRealPath();
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(final Path current, final BasicFileAttributes attributes) throws IOException {
				if (!current.toRealPath().startsWith(realDirectory)) throw new IOException("Mission save directory contains a linked directory");
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attributes) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(final Path current, final IOException error) throws IOException {
				if (error != null) throw error;
				Files.delete(current);
				return FileVisitResult.CONTINUE;
			}
		});
		return true;
	}

	private static Path scriptPath(final File root, final String name, final String suffix) throws IOException {
		final Path saveRoot = root.toPath().toAbsolutePath().normalize();
		final Path realRoot = Files.isDirectory(saveRoot) ? saveRoot.toRealPath() : saveRoot;
		final String[] parts = name.replace('\\', '/').split("/", -1);
		Path path = saveRoot;
		for (int i = 0; i < parts.length; i++) {
			final String part = parts[i];
			if (!validComponent(part) || !part.equals(part.strip())) throw new IOException("Invalid mission save path");
			path = path.resolve(i == parts.length - 1 ? part + suffix : part);
			if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !path.toRealPath().startsWith(realRoot)) {
				throw new IOException("Mission save path leaves the profile directory");
			}
		}
		return path;
	}

	private static boolean validComponent(final String name) {
		return !name.isEmpty() && !name.endsWith(".") && !name.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?")
				&& name.chars().noneMatch(character -> Character.isISOControl(character)
						|| "<>:\"/\\|?*".indexOf(character) >= 0);
	}
	/** Keep deleted profiles' files recoverable without giving them to a recreated profile. */
	public static void retireProfile(final File root, final String profile) throws IOException {
		final File directory = ProfileGameCacheStore.profileDirectory(root, profile);
		if (!directory.exists()) return;
		final File retired = new File(root, "retired");
		Files.createDirectories(retired.toPath());
		Files.move(directory.toPath(), new File(retired, directory.getName() + "-" + UUID.randomUUID()).toPath());
	}
}
