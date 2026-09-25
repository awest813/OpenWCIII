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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hiveworkshop.rms.parsers.mdlx.MdlxModel;

import mpq.ArchivedFile;
import mpq.ArchivedFileExtractor;
import mpq.ArchivedFileStream;
import mpq.HashLookup;
import mpq.MPQArchive;

/**
 * Dumps the cinematic asset references retail campaign scripts make
 * ({@code SetSkyModel}, {@code SetCinematicCamera}, {@code PlayModelCinematic},
 * {@code SetIntroShotModel}), checks each file exists in the given archives,
 * and parses referenced MDX headers (sequences / cameras / geosets) so engine
 * work can be validated against real data.
 *
 * <p>Usage:
 *
 * <pre>
 * ./gradlew :desktop:campaignCinematicRefs -Pargs="--mpq F:\WC3Data\war3.mpq --mpq F:\WC3Data\War3x.mpq --mpq F:\WC3Data\War3xlocal.mpq"
 * </pre>
 */
public final class CampaignCinematicRefs {
	private static final Pattern SKY = Pattern.compile("SetSkyModel\\s*\\(");
	private static final Pattern SKY_LITERAL = Pattern.compile("SetSkyModel\\s*\\(\\s*\"([^\"]*)\"");
	private static final Pattern CAMERA = Pattern.compile("SetCinematicCamera\\s*\\(");
	private static final Pattern CAMERA_LITERAL = Pattern.compile("SetCinematicCamera\\s*\\(\\s*\"([^\"]*)\"");
	private static final Pattern MODEL_CINE = Pattern.compile("PlayModelCinematic\\s*\\(\\s*\"([^\"]*)\"");
	private static final Pattern INTRO_MODEL = Pattern.compile("SetIntroShotModel\\s*\\(\\s*\"([^\"]*)\"");

	private CampaignCinematicRefs() {
	}

	public static void main(final String[] args) throws Exception {
		final List<Path> archives = new ArrayList<>();
		String find = null;
		String probe = null;
		String readPath = null;
		for (int i = 0; i < args.length; i++) {
			if ("--mpq".equals(args[i])) {
				archives.add(Paths.get(args[++i]));
			}
			else if ("--find".equals(args[i])) {
				find = args[++i];
			}
			else if ("--probe".equals(args[i])) {
				probe = args[++i];
			}
			else if ("--read".equals(args[i])) {
				readPath = args[++i];
			}
			else {
				throw new IllegalArgumentException("Unknown option: " + args[i]);
			}
		}
		if (archives.isEmpty()) {
			for (final String defaultPath : new String[] { "F:\\WC3Data\\war3.mpq", "F:\\WC3Data\\War3x.mpq", "F:\\WC3Data\\War3xlocal.mpq" }) {
				final Path p = Paths.get(defaultPath);
				if (Files.exists(p)) {
					archives.add(p);
				}
			}
		}
		if (archives.isEmpty()) {
			System.err.println("Pass at least one --mpq <path> holding campaign maps.");
			System.exit(2);
		}

		final Map<String, String> mapScripts = new TreeMap<>();
		final List<OpenArchive> open = new ArrayList<>();
		for (final Path archive : archives) {
			final SeekableByteChannel channel = Files.newByteChannel(archive, StandardOpenOption.READ);
			final MPQArchive mpq = new MPQArchive(channel);
			open.add(new OpenArchive(archive, mpq, channel));
			if (find != null) {
				findInListfile(archive, mpq, channel, find);
			}
			if (probe != null) {
				probePath(archive, mpq, channel, probe);
			}
			if (readPath != null) {
				final byte[] data = read(mpq, channel, new ArchivedFileExtractor(), readPath);
				if (data == null) {
					System.out.println(archive.getFileName() + ": no such file: " + readPath);
				}
				else {
					System.out.println("== " + archive.getFileName() + " :: " + readPath + " ("
							+ data.length + " bytes)");
					System.out.println(new String(data, StandardCharsets.UTF_8));
				}
			}
			collectMapScripts(mpq, channel, new ArchivedFileExtractor(), mapScripts);
		}
		if ((find != null) || (probe != null) || (readPath != null)) {
			for (final OpenArchive o : open) {
				o.channel.close();
			}
			return;
		}
		System.out.println("Maps with scripts: " + mapScripts.size());

		final Map<String, KindRefs> byKind = new LinkedHashMap<>();
		byKind.put("SetSkyModel", new KindRefs());
		byKind.put("SetCinematicCamera", new KindRefs());
		byKind.put("PlayModelCinematic", new KindRefs());
		byKind.put("SetIntroShotModel", new KindRefs());
		for (final Map.Entry<String, String> e : mapScripts.entrySet()) {
			collect(e.getKey(), e.getValue(), SKY_LITERAL, byKind.get("SetSkyModel"));
			collect(e.getKey(), e.getValue(), CAMERA_LITERAL, byKind.get("SetCinematicCamera"));
			collect(e.getKey(), e.getValue(), MODEL_CINE, byKind.get("PlayModelCinematic"));
			collect(e.getKey(), e.getValue(), INTRO_MODEL, byKind.get("SetIntroShotModel"));
		}
		System.out.println("Bare call sites (any arg shape): SetSkyModel=" + countCalls(mapScripts, SKY)
				+ " SetCinematicCamera=" + countCalls(mapScripts, CAMERA));

		int missing = 0;
		for (final Map.Entry<String, KindRefs> kind : byKind.entrySet()) {
			System.out.println("== " + kind.getKey() + " (" + kind.getValue().refs.size() + " distinct paths, "
					+ kind.getValue().hits + " call sites in " + kind.getValue().maps.size() + " maps)");
			for (final Map.Entry<String, TreeSet<String>> ref : kind.getValue().refs.entrySet()) {
				final String path = ref.getKey();
				final byte[] data = readAny(open, path);
				final String status;
				if (path.isEmpty()) {
					status = "EMPTY-ARG";
				}
				else if (data == null) {
					status = "MISSING";
					missing++;
				}
				else {
					status = describeModel(data);
				}
				System.out.println("  [" + status + "] " + (path.isEmpty() ? "(empty)" : path) + " <- "
						+ ref.getValue());
			}
		}
		System.out.println("Missing files: " + missing);
		for (final OpenArchive o : open) {
			o.channel.close();
		}
		if (missing > 0) {
			System.exit(1);
		}
	}

	private static String describeModel(final byte[] data) {
		try {
			final MdlxModel model = new MdlxModel(ByteBuffer.wrap(data));
			return "model seq=" + model.sequences.size() + " cam=" + model.cameras.size() + " geo="
					+ model.geosets.size() + " (" + data.length + " bytes)";
		}
		catch (final Exception e) {
			return "MODEL-PARSE-FAIL: " + e.getMessage();
		}
	}

	private static void collect(final String map, final String script, final Pattern pattern, final KindRefs out) {
		final Matcher m = pattern.matcher(script);
		while (m.find()) {
			out.hits++;
			out.maps.add(map);
			// JASS source escapes backslashes, so unescape to the runtime value.
			out.refs.computeIfAbsent(m.group(1).replace("\\\\", "\\"), k -> new TreeSet<>()).add(map);
		}
	}

	private static byte[] readAny(final List<OpenArchive> open, final String path) {
		if ((path == null) || path.isEmpty()) {
			return null;
		}
		// Later archives shadow earlier ones, mirroring CompoundDataSource.
		byte[] found = null;
		final ArchivedFileExtractor extractor = new ArchivedFileExtractor();
		for (final OpenArchive o : open) {
			final byte[] data = read(o.mpq, o.channel, extractor, path);
			if (data != null) {
				found = data;
			}
			else {
				// Retail scripts reference .mdl source names; archives carry compiled .mdx.
				final String swapped = swapMdxMdl(path);
				if (swapped != null) {
					final byte[] alt = read(o.mpq, o.channel, extractor, swapped);
					if (alt != null) {
						found = alt;
					}
				}
			}
		}
		return found;
	}

	private static String swapMdxMdl(final String path) {
		final String lower = path.toLowerCase();
		if (lower.endsWith(".mdl")) {
			return path.substring(0, path.length() - 4) + ".mdx";
		}
		if (lower.endsWith(".mdx")) {
			return path.substring(0, path.length() - 4) + ".mdl";
		}
		return null;
	}

	private static int countCalls(final Map<String, String> mapScripts, final Pattern pattern) {
		int total = 0;
		for (final String script : mapScripts.values()) {
			final Matcher m = pattern.matcher(script);
			while (m.find()) {
				total++;
			}
		}
		return total;
	}

	private static void probePath(final Path archive, final MPQArchive mpq,
			final SeekableByteChannel channel, final String path) {
		final ArchivedFileExtractor extractor = new ArchivedFileExtractor();
		final String[] variants = new String[] { path, path.replace('\\', '/'), path.toLowerCase(),
				path.toLowerCase().replace('\\', '/'), path.toUpperCase(), };
		for (final String v : variants) {
			final byte[] data = read(mpq, channel, extractor, v);
			System.out.println(archive.getFileName() + " probe '" + v + "': "
					+ (data == null ? "MISS" : "HIT " + data.length + " bytes"));
		}
	}

	private static void findInListfile(final Path archive, final MPQArchive mpq,
			final SeekableByteChannel channel, final String needle) throws IOException {
		final byte[] listBytes = read(mpq, channel, new ArchivedFileExtractor(), "(listfile)");
		if (listBytes == null) {
			System.out.println(archive + ": no (listfile)");
			return;
		}
		final String lower = needle.toLowerCase();
		int count = 0;
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(listBytes), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.toLowerCase().contains(lower)) {
					System.out.println(archive.getFileName() + ": " + line.trim());
					count++;
				}
			}
		}
		System.out.println(archive.getFileName() + ": " + count + " entries match '" + needle + "'");
	}

	private static void collectMapScripts(final MPQArchive mpq, final SeekableByteChannel channel,
			final ArchivedFileExtractor extractor, final Map<String, String> mapScripts) throws IOException {
		final byte[] listBytes = read(mpq, channel, extractor, "(listfile)");
		if (listBytes == null) {
			return;
		}
		final List<String> entries = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(listBytes), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				final String trimmed = line.trim();
				if (!trimmed.isEmpty()) {
					entries.add(trimmed);
				}
			}
		}
		for (final String entry : entries) {
			final String lower = entry.toLowerCase();
			if (!lower.endsWith(".w3m") && !lower.endsWith(".w3x")) {
				continue;
			}
			final byte[] mapBytes = read(mpq, channel, extractor, entry);
			if (mapBytes == null) {
				continue;
			}
			final Path tmp = Files.createTempFile("warsmash-cinerefs", ".w3x");
			try {
				Files.write(tmp, mapBytes);
				try (SeekableByteChannel mapChannel = Files.newByteChannel(tmp, StandardOpenOption.READ)) {
					final String script = readText(new MPQArchive(mapChannel), mapChannel,
							new ArchivedFileExtractor(), "war3map.j");
					if (script != null) {
						mapScripts.put(mapName(entry), script);
					}
				}
				catch (final Exception e) {
					System.err.println("Could not read " + entry + ": " + e);
				}
			}
			finally {
				Files.deleteIfExists(tmp);
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
				int n;
				while ((n = stream.read(buffer)) > 0) {
					bytes.write(buffer.array(), 0, n);
					buffer.clear();
				}
				return bytes.toByteArray();
			}
		}
		catch (final Exception e) {
			return null;
		}
	}

	private static final class KindRefs {
		final Map<String, TreeSet<String>> refs = new TreeMap<>();
		final TreeSet<String> maps = new TreeSet<>();
		int hits;
	}

	private static final class OpenArchive {
		final Path path;
		final MPQArchive mpq;
		final SeekableByteChannel channel;

		OpenArchive(final Path path, final MPQArchive mpq, final SeekableByteChannel channel) {
			this.path = path;
			this.mpq = mpq;
			this.channel = channel;
		}
	}
}
