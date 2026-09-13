package com.etheller.warsmash.desktop.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mpq.ArchivedFile;
import mpq.ArchivedFileExtractor;
import mpq.ArchivedFileStream;
import mpq.HashLookup;
import mpq.MPQArchive;

/**
 * Measures how much of the Warcraft III native API the retail single-player
 * campaigns actually need, and which of those natives this engine implements.
 *
 * <p>
 * For every campaign map in the given archives it pulls out war3map.j, walks
 * the call graph through Blizzard.j, and collects the common.j natives the
 * script can reach. Those are compared against the natives registered in the
 * engine source, and the difference is written out as a markdown report.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * ./gradlew :desktop:campaignNativeAudit -Pargs="--out docs/CAMPAIGN_NATIVE_COVERAGE.md \
 *     --mpq C:\WC3\war3.mpq --mpq C:\WC3\War3x.mpq --mpq C:\WC3\War3xlocal.mpq"
 * </pre>
 *
 * The archives are the ones named in your warsmash.ini [DataSources]. Reign of
 * Chaos maps live in war3.mpq and Frozen Throne maps in War3xlocal.mpq, so pass
 * both to audit both campaigns.
 */
public final class CampaignNativeAudit {
	private static final Pattern NATIVE_DECLARATION = Pattern
			.compile("^\\s*(?:constant\\s+)?native\\s+([A-Za-z_]\\w*)\\s+takes", Pattern.MULTILINE);
	private static final Pattern FUNCTION_DEFINITION = Pattern.compile(
			"^\\s*(?:constant\\s+)?function\\s+([A-Za-z_]\\w*)\\s+takes(.*?)^\\s*endfunction",
			Pattern.MULTILINE | Pattern.DOTALL);
	private static final Pattern CALL = Pattern.compile("\\b([A-Za-z_]\\w*)\\s*\\(");
	private static final Pattern FUNCTION_REFERENCE = Pattern.compile("\\bfunction\\s+([A-Za-z_]\\w*)");
	private static final Pattern LINE_COMMENT = Pattern.compile("//[^\n]*");
	private static final Pattern REGISTERED_NATIVE = Pattern.compile("createNative\\(\\s*\"([A-Za-z0-9_]+)\"");

	private static final String[] ENGINE_SOURCES = {
			"core/src/com/etheller/warsmash/parsers/jass/Jass2.java",
			"core/src/com/etheller/warsmash/parsers/jass/JassAIEnvironment.java" };

	private CampaignNativeAudit() {
	}

	public static void main(final String[] args) throws Exception {
		Path repo = Paths.get(".");
		Path out = Paths.get("docs/CAMPAIGN_NATIVE_COVERAGE.md");
		final List<Path> archives = new ArrayList<>();
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--repo":
				repo = Paths.get(args[++i]);
				break;
			case "--out":
				out = Paths.get(args[++i]);
				break;
			case "--mpq":
				archives.add(Paths.get(args[++i]));
				break;
			default:
				throw new IllegalArgumentException("Unknown option: " + args[i]);
			}
		}
		if (archives.isEmpty()) {
			System.err.println("Pass at least one --mpq <path> holding campaign maps and Scripts\\common.j.");
			System.exit(2);
		}

		String commonJass = null;
		String blizzardJass = null;
		final Map<String, String> mapScripts = new TreeMap<>();
		for (final Path archive : archives) {
			try (SeekableByteChannel channel = Files.newByteChannel(archive, StandardOpenOption.READ)) {
				final MPQArchive mpq = new MPQArchive(channel);
				final ArchivedFileExtractor extractor = new ArchivedFileExtractor();
				// Later archives win, the way CompoundDataSource resolves them, so
				// the expansion's common.j shadows the Reign of Chaos one.
				final String archiveCommon = readText(mpq, channel, extractor, "Scripts\\common.j");
				if (archiveCommon != null) {
					commonJass = archiveCommon;
				}
				final String archiveBlizzard = readText(mpq, channel, extractor, "Scripts\\Blizzard.j");
				if (archiveBlizzard != null) {
					blizzardJass = archiveBlizzard;
				}
				collectMapScripts(archive, mpq, channel, extractor, mapScripts);
			}
		}
		if (commonJass == null) {
			System.err.println("None of the given archives contains Scripts\\common.j.");
			System.exit(2);
		}

		final Set<String> declaredNatives = matchAll(NATIVE_DECLARATION, commonJass);
		final Map<String, String> blizzardFunctions = functionBodies(
				blizzardJass == null ? "" : stripComments(blizzardJass));
		final Set<String> implementedNatives = new HashSet<>();
		for (final String source : ENGINE_SOURCES) {
			implementedNatives.addAll(
					matchAll(REGISTERED_NATIVE, new String(Files.readAllBytes(repo.resolve(source)), "UTF-8")));
		}

		final Map<String, Set<String>> missingByNative = new TreeMap<>();
		final Map<String, Integer> missingCountByMap = new TreeMap<>();
		for (final Map.Entry<String, String> entry : mapScripts.entrySet()) {
			final Set<String> reached = reachableNatives(stripComments(entry.getValue()), declaredNatives,
					blizzardFunctions);
			int missingHere = 0;
			for (final String nativeName : reached) {
				if (!implementedNatives.contains(nativeName)) {
					missingByNative.computeIfAbsent(nativeName, key -> new TreeSet<>()).add(entry.getKey());
					missingHere++;
				}
			}
			missingCountByMap.put(entry.getKey(), missingHere);
		}

		Files.createDirectories(out.toAbsolutePath().getParent());
		Files.write(out, report(mapScripts.keySet().size(), declaredNatives.size(), implementedNatives.size(),
				missingByNative, missingCountByMap).getBytes(StandardCharsets.UTF_8));
		System.out.println("maps audited:          " + mapScripts.size());
		System.out.println("natives in common.j:   " + declaredNatives.size());
		System.out.println("natives implemented:   " + implementedNatives.size());
		System.out.println("missing but reachable: " + missingByNative.size());
		System.out.println("report written to:     " + out);
	}

	private static String report(final int mapCount, final int declaredCount, final int implementedCount,
			final Map<String, Set<String>> missingByNative, final Map<String, Integer> missingCountByMap) {
		final StringBuilder text = new StringBuilder();
		text.append("# Campaign native coverage\n\n");
		text.append("Generated by `CampaignNativeAudit` from retail campaign scripts. ");
		text.append("A native counts as reachable when a map script can get to it, ");
		text.append("directly or through a Blizzard.j wrapper.\n\n");
		text.append("| Measure | Count |\n|---|---|\n");
		text.append("| Campaign maps audited | ").append(mapCount).append(" |\n");
		text.append("| Natives declared in common.j | ").append(declaredCount).append(" |\n");
		text.append("| Natives registered by the engine | ").append(implementedCount).append(" |\n");
		text.append("| Reachable natives with no implementation | ").append(missingByNative.size()).append(" |\n\n");

		text.append("## Missing natives, by how many maps reach them\n\n");
		text.append("| Native | Maps | Example maps |\n|---|---|---|\n");
		final List<Map.Entry<String, Set<String>>> ranked = new ArrayList<>(missingByNative.entrySet());
		ranked.sort(Comparator.<Map.Entry<String, Set<String>>>comparingInt(e -> -e.getValue().size())
				.thenComparing(Map.Entry::getKey));
		for (final Map.Entry<String, Set<String>> entry : ranked) {
			final List<String> examples = new ArrayList<>(entry.getValue());
			text.append("| `").append(entry.getKey()).append("` | ").append(entry.getValue().size()).append(" | ")
					.append(String.join(", ", examples.subList(0, Math.min(3, examples.size()))));
			if (examples.size() > 3) {
				text.append(", ...");
			}
			text.append(" |\n");
		}

		text.append("\n## Maps with the most missing natives\n\n");
		text.append("| Map | Missing |\n|---|---|\n");
		final List<Map.Entry<String, Integer>> byMap = new ArrayList<>(missingCountByMap.entrySet());
		byMap.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(e -> -e.getValue())
				.thenComparing(Map.Entry::getKey));
		for (final Map.Entry<String, Integer> entry : byMap.subList(0, Math.min(20, byMap.size()))) {
			text.append("| ").append(entry.getKey()).append(" | ").append(entry.getValue()).append(" |\n");
		}
		return text.toString();
	}

	private static Set<String> reachableNatives(final String mapScript, final Set<String> declaredNatives,
			final Map<String, String> blizzardFunctions) {
		final Map<String, String> localFunctions = functionBodies(mapScript);
		final Set<String> reached = new TreeSet<>();
		final Set<String> seen = new HashSet<>();
		final Deque<String> pending = new ArrayDeque<>(callees(mapScript));
		while (!pending.isEmpty()) {
			final String name = pending.pop();
			if (!seen.add(name)) {
				continue;
			}
			if (declaredNatives.contains(name)) {
				reached.add(name);
				continue;
			}
			final String body = localFunctions.containsKey(name) ? localFunctions.get(name)
					: blizzardFunctions.get(name);
			if (body != null) {
				pending.addAll(callees(body));
			}
		}
		return reached;
	}

	private static Set<String> callees(final String body) {
		final Set<String> names = matchAll(CALL, body);
		names.addAll(matchAll(FUNCTION_REFERENCE, body));
		return names;
	}

	private static Map<String, String> functionBodies(final String script) {
		final Map<String, String> bodies = new HashMap<>();
		final Matcher matcher = FUNCTION_DEFINITION.matcher(script);
		while (matcher.find()) {
			bodies.put(matcher.group(1), matcher.group(2));
		}
		return bodies;
	}

	private static Set<String> matchAll(final Pattern pattern, final String text) {
		final Set<String> found = new HashSet<>();
		final Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			found.add(matcher.group(1));
		}
		return found;
	}

	private static String stripComments(final String script) {
		return LINE_COMMENT.matcher(script).replaceAll("");
	}

	private static void collectMapScripts(final Path archive, final MPQArchive mpq, final SeekableByteChannel channel,
			final ArchivedFileExtractor extractor, final Map<String, String> mapScripts) throws IOException {
		final List<String> listfile = listfile(mpq, channel, extractor);
		if (listfile == null) {
			System.err.println("No (listfile) in " + archive + "; skipping its maps.");
			return;
		}
		for (final String entry : listfile) {
			final String lower = entry.toLowerCase();
			if (!lower.endsWith(".w3m") && !lower.endsWith(".w3x")) {
				continue;
			}
			final byte[] mapBytes = read(mpq, channel, extractor, entry);
			if (mapBytes == null) {
				continue;
			}
			final Path temporaryMap = Files.createTempFile("warsmash-audit", lower.substring(lower.length() - 4));
			try {
				Files.write(temporaryMap, mapBytes);
				try (SeekableByteChannel mapChannel = Files.newByteChannel(temporaryMap, StandardOpenOption.READ)) {
					final String script = readText(new MPQArchive(mapChannel), mapChannel,
							new ArchivedFileExtractor(), "war3map.j");
					if (script != null) {
						mapScripts.put(mapName(entry), script);
					}
				}
				catch (final Exception malformedMap) {
					System.err.println("Could not read " + entry + ": " + malformedMap);
				}
			}
			finally {
				Files.deleteIfExists(temporaryMap);
			}
		}
	}

	private static String mapName(final String archivePath) {
		final String fileName = archivePath.substring(archivePath.lastIndexOf('\\') + 1);
		return fileName.substring(0, fileName.lastIndexOf('.'));
	}

	private static String readText(final MPQArchive mpq, final SeekableByteChannel channel,
			final ArchivedFileExtractor extractor, final String path) {
		final byte[] data = read(mpq, channel, extractor, path);
		return data == null ? null : new String(data, StandardCharsets.UTF_8);
	}

	private static byte[] read(final MPQArchive mpq, final SeekableByteChannel channel,
			final ArchivedFileExtractor extractor, final String path) {
		try {
			final ArchivedFile file = mpq.lookupHash2(new HashLookup(path));
			try (ArchivedFileStream stream = new ArchivedFileStream(channel, extractor, file)) {
				final ByteArrayOutputStream bytes = new ByteArrayOutputStream((int) stream.size());
				final ByteBuffer buffer = ByteBuffer.allocate(1 << 16);
				int read;
				while ((read = stream.read(buffer)) > 0) {
					bytes.write(buffer.array(), 0, read);
					buffer.clear();
				}
				return bytes.toByteArray();
			}
		}
		catch (final Exception notInArchive) {
			return null;
		}
	}

	private static List<String> listfile(final MPQArchive mpq, final SeekableByteChannel channel,
			final ArchivedFileExtractor extractor) throws IOException {
		final byte[] data = read(mpq, channel, extractor, "(listfile)");
		if (data == null) {
			return null;
		}
		final List<String> entries = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				final String trimmed = line.trim();
				if (!trimmed.isEmpty()) {
					entries.add(trimmed);
				}
			}
		}
		return entries;
	}
}
