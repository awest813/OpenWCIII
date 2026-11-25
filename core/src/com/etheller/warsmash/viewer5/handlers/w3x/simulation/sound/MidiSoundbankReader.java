package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;

final class MidiSoundbankReader {
	private static final int RIFF_HEADER_SIZE = 12;
	private static final int CHUNK_HEADER_SIZE = 8;

	private MidiSoundbankReader() {
	}

	static Soundbank read(final InputStream stream) throws IOException, InvalidMidiDataException {
		final byte[] bytes = stream.readAllBytes();
		try {
			return MidiSystem.getSoundbank(new ByteArrayInputStream(bytes));
		}
		catch (final IOException | InvalidMidiDataException error) {
			// Some classic DLS banks carry INFO fields the Java RIFF reader cannot parse.
			// INFO is descriptive metadata. Keep every instrument and wave chunk intact.
			final byte[] stripped = withoutRiffInfo(bytes);
			if (stripped == null) {
				throw error;
			}
			try {
				return MidiSystem.getSoundbank(new ByteArrayInputStream(stripped));
			}
			catch (final IOException | InvalidMidiDataException retry) {
				error.addSuppressed(retry);
				throw error;
			}
		}
	}

	private static byte[] withoutRiffInfo(final byte[] bytes) {
		if (bytes.length < RIFF_HEADER_SIZE || !fourCC(bytes, 0).equals("RIFF")
				|| !fourCC(bytes, CHUNK_HEADER_SIZE).equals("DLS ")) {
			return null;
		}
		final ByteBuffer riff = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		final long end = CHUNK_HEADER_SIZE + Integer.toUnsignedLong(riff.getInt(4));
		if (end < RIFF_HEADER_SIZE || end > bytes.length) {
			return null;
		}
		final ByteArrayOutputStream result = new ByteArrayOutputStream(bytes.length);
		result.write(bytes, 0, RIFF_HEADER_SIZE);
		int offset = RIFF_HEADER_SIZE;
		boolean removedInfo = false;
		while (offset < end) {
			if (end - offset < CHUNK_HEADER_SIZE) {
				return null;
			}
			final long size = Integer.toUnsignedLong(riff.getInt(offset + 4));
			final long next = offset + CHUNK_HEADER_SIZE + size + (size & 1);
			final boolean info = size >= 4 && end - offset >= RIFF_HEADER_SIZE
					&& fourCC(bytes, offset).equals("LIST")
					&& fourCC(bytes, offset + CHUNK_HEADER_SIZE).equals("INFO");
			// A classic bank's RIFF length can end inside its final INFO list.
			if (next > end && !(info && next == bytes.length)) {
				return null;
			}
			if (info) {
				removedInfo = true;
			}
			else {
				result.write(bytes, offset, (int) next - offset);
			}
			offset = (int) next;
		}
		if (!removedInfo) {
			return null;
		}
		final byte[] stripped = result.toByteArray();
		ByteBuffer.wrap(stripped).order(ByteOrder.LITTLE_ENDIAN).putInt(4, stripped.length - CHUNK_HEADER_SIZE);
		return stripped;
	}

	private static String fourCC(final byte[] bytes, final int offset) {
		return new String(bytes, offset, 4, StandardCharsets.US_ASCII);
	}
}
