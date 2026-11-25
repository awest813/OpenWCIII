package com.etheller.warsmash.units.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.util.War3ID;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

class War3ObjectDataChangesetTest {
	private static final War3ID BASE = War3ID.fromString("LTlt");
	private static final War3ID CUSTOM = War3ID.fromString("D001");
	private static final War3ID FIELD = War3ID.fromString("dvr2");

	@Test
	void readsClassicAndExtendedDoodadFields() throws IOException {
		final War3ObjectDataChangeset classic = new War3ObjectDataChangeset('d');
		assertTrue(load(classic, doodads(1, 100, false)));
		assertFalse(classic.extended());
		assertEquals(100, change(classic.getOriginal(), BASE).getLongval());
		assertEquals(101, change(classic.getCustom(), CUSTOM).getLongval());

		final War3ObjectDataChangeset extended = new War3ObjectDataChangeset('d');
		assertTrue(load(extended, doodads(2, 200, false)));
		assertTrue(extended.extended());
		assertEquals(3, change(extended.getOriginal(), BASE).getLevel());
		assertEquals(201, change(extended.getCustom(), CUSTOM).getLongval());
	}

	@Test
	void truncatedOverlayPreservesBothTablesAndTheirLayout() throws IOException {
		final War3ObjectDataChangeset changes = new War3ObjectDataChangeset('d');
		assertTrue(load(changes, doodads(2, 100, false)));
		assertFalse(load(changes, doodads(1, 200, true)));
		assertTrue(changes.extended());
		assertEquals(100, change(changes.getOriginal(), BASE).getLongval());
		assertEquals(101, change(changes.getCustom(), CUSTOM).getLongval());

		assertFalse(load(changes, doodads(2, 300, true)));
		assertEquals(100, change(changes.getOriginal(), BASE).getLongval());
		assertEquals(101, change(changes.getCustom(), CUSTOM).getLongval());
		assertTrue(load(changes, doodads(2, 400, false)));
		assertEquals(400, change(changes.getOriginal(), BASE).getLongval());
		assertEquals(401, change(changes.getCustom(), CUSTOM).getLongval());
	}

	private static Change change(final ObjectMap table, final War3ID id) {
		return table.get(id).getChanges().get(FIELD).get(0);
	}

	private static boolean load(final War3ObjectDataChangeset changes, final byte[] bytes) throws IOException {
		try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream(new ByteArrayInputStream(bytes))) {
			final boolean loaded = changes.load(stream, WTS.DO_NOTHING, false);
			if (loaded) {
				assertEquals(-1, stream.read());
			}
			return loaded;
		}
	}

	private static byte[] doodads(final int version, final int value, final boolean truncated) throws IOException {
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(bytes)) {
			stream.writeInt(version);
			stream.writeInt(1);
			writeDoodad(stream, version, "\0\0\0\0", value);
			stream.writeInt(truncated ? 2 : 1);
			writeDoodad(stream, version, CUSTOM.asStringValue(), value + 1);
			if (truncated) {
				stream.write("LTltD002".getBytes(StandardCharsets.US_ASCII));
				stream.writeInt(1);
				stream.write("dnam".getBytes(StandardCharsets.US_ASCII));
				stream.writeInt(War3ObjectDataChangeset.VAR_TYPE_STRING);
				if (version == 2) {
					stream.writeInt(0);
					stream.writeInt(0);
				}
				stream.write("unterminated".getBytes(StandardCharsets.US_ASCII));
			}
		}
		return bytes.toByteArray();
	}

	private static void writeDoodad(final LittleEndianDataOutputStream stream, final int version, final String id,
			final int value) throws IOException {
		stream.write(BASE.asStringValue().getBytes(StandardCharsets.US_ASCII));
		stream.write(id.getBytes(StandardCharsets.US_ASCII));
		stream.writeInt(1);
		stream.write(FIELD.asStringValue().getBytes(StandardCharsets.US_ASCII));
		stream.writeInt(War3ObjectDataChangeset.VAR_TYPE_INT);
		if (version == 2) {
			stream.writeInt(3);
			stream.writeInt(0);
		}
		stream.writeInt(value);
		stream.writeInt(0);
	}
}
