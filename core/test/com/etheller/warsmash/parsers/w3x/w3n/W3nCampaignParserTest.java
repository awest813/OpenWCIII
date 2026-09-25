package com.etheller.warsmash.parsers.w3x.w3n;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.common.io.LittleEndianDataOutputStream;

class W3nCampaignParserTest {

	@Test
	void testRoundtripBinarySerialization() throws IOException {
		final W3nCampaign original = new W3nCampaign();
		original.setVersion(1);
		original.setCampaignVersion(5);
		original.setEditorVersion(6035);
		original.setName("The Founding of Durotar");
		original.setDifficulty("Normal");
		original.setAuthor("Blizzard Entertainment");
		original.setDescription("Follow Rexxar as he explores Kalimdor");
		original.setVariableDifficulty(1);
		original.setCampaignBackground(3);
		original.setBackgroundScreenModel("UI\\Glues\\SinglePlayer\\OrcCampaign\\OrcCampaign.mdx");
		original.setMinimapPath("Textures\\Minimap.blp");
		original.setAmbientSound(1);
		original.setCustomAmbientSoundPath("Sound\\Ambient\\OrcTheme.mp3");
		original.setTerrainFog(2);
		original.getFogHeight()[0] = 1000f;
		original.getFogHeight()[1] = 8000f;
		original.setFogDensity(0.005f);
		original.getFogColor()[0] = 255;
		original.getFogColor()[1] = 128;
		original.getFogColor()[2] = 64;
		original.getFogColor()[3] = 255;
		original.setUiRace(1);

		original.getMapButtons().add(new W3nCampaign.MapButton(1, "Chapter One: To Tame a Land", "Act I",
				"Maps\\Campaign\\OrcX01.w3x"));
		original.getMapButtons().add(new W3nCampaign.MapButton(1, "Chapter Two: Old Hatreds", "Act II",
				"Maps\\Campaign\\OrcX02.w3x"));
		original.getMapButtons().add(new W3nCampaign.MapButton(0, "Chapter Three: A Symphony of Frost and Flame", "Act III",
				"Maps\\Campaign\\OrcX03.w3x"));

		original.getMaps().add(new W3nCampaign.CampaignMap(0, "Maps\\Campaign\\OrcX01.w3x"));
		original.getMaps().add(new W3nCampaign.CampaignMap(0, "Maps\\Campaign\\OrcX02.w3x"));
		original.getMaps().add(new W3nCampaign.CampaignMap(0, "Maps\\Campaign\\OrcX03.w3x"));

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final LittleEndianDataOutputStream dos = new LittleEndianDataOutputStream(baos);
		W3nCampaignParser.save(original, dos);
		dos.flush();

		final byte[] bytes = baos.toByteArray();
		final W3nCampaign parsed = W3nCampaignParser.parse(new ByteArrayInputStream(bytes));

		assertEquals(1, parsed.getVersion());
		assertEquals(5, parsed.getCampaignVersion());
		assertEquals(6035, parsed.getEditorVersion());
		assertEquals("The Founding of Durotar", parsed.getName());
		assertEquals("Normal", parsed.getDifficulty());
		assertEquals("Blizzard Entertainment", parsed.getAuthor());
		assertEquals("Follow Rexxar as he explores Kalimdor", parsed.getDescription());
		assertEquals(1, parsed.getVariableDifficulty());
		assertTrue(parsed.isVariableDifficulty());
		assertEquals(3, parsed.getCampaignBackground());
		assertEquals("UI\\Glues\\SinglePlayer\\OrcCampaign\\OrcCampaign.mdx", parsed.getBackgroundScreenModel());
		assertEquals("Textures\\Minimap.blp", parsed.getMinimapPath());
		assertEquals(1, parsed.getAmbientSound());
		assertEquals("Sound\\Ambient\\OrcTheme.mp3", parsed.getCustomAmbientSoundPath());
		assertEquals(2, parsed.getTerrainFog());
		assertEquals(1000f, parsed.getFogHeight()[0], 0.001f);
		assertEquals(8000f, parsed.getFogHeight()[1], 0.001f);
		assertEquals(0.005f, parsed.getFogDensity(), 0.0001f);
		assertArrayEquals(new short[] { 255, 128, 64, 255 }, parsed.getFogColor());
		assertEquals(1, parsed.getUiRace());

		assertEquals(3, parsed.getMapButtons().size());
		final W3nCampaign.MapButton b0 = parsed.getMapButtons().get(0);
		assertTrue(b0.isVisible());
		assertEquals("Chapter One: To Tame a Land", b0.getChapterTitle());
		assertEquals("Act I", b0.getChapterSubtitle());
		assertEquals("Maps\\Campaign\\OrcX01.w3x", b0.getMapPath());

		final W3nCampaign.MapButton b2 = parsed.getMapButtons().get(2);
		assertFalse(b2.isVisible());
		assertEquals("Chapter Three: A Symphony of Frost and Flame", b2.getChapterTitle());

		assertEquals(3, parsed.getMaps().size());
		assertEquals(0, parsed.getMaps().get(0).getFlags());
		assertEquals("Maps\\Campaign\\OrcX01.w3x", parsed.getMaps().get(0).getMapPath());
	}

	@Test
	void testTrigStrResolutionWithWts() throws IOException {
		final W3nCampaign original = new W3nCampaign();
		original.setName("TRIGSTR_001");
		original.setDifficulty("TRIGSTR_002");
		original.setAuthor("TRIGSTR_003");
		original.setDescription("TRIGSTR_005");
		original.getMapButtons().add(new W3nCampaign.MapButton(1, "TRIGSTR_004", "Act I", "Maps\\OrcX01.w3x"));

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final LittleEndianDataOutputStream dos = new LittleEndianDataOutputStream(baos);
		W3nCampaignParser.save(original, dos);
		dos.flush();

		final String wtsContent = "STRING 1\n" +
				"{\n" +
				"The Founding of Durotar\n" +
				"}\n" +
				"STRING 2\n" +
				"{\n" +
				"Hard\n" +
				"}\n" +
				"STRING 3\n" +
				"{\n" +
				"Blizzard Entertainment\n" +
				"}\n" +
				"STRING 4\n" +
				"{\n" +
				"Chapter One: To Tame a Land\n" +
				"}\n" +
				"STRING 5\n" +
				"{\n" +
				"Rexxar's Kalimdor journey\n" +
				"}\n";

		final ByteArrayInputStream w3fStream = new ByteArrayInputStream(baos.toByteArray());
		final ByteArrayInputStream wtsStream = new ByteArrayInputStream(wtsContent.getBytes(StandardCharsets.UTF_8));

		final W3nCampaign parsed = W3nCampaignParser.parse(w3fStream, wtsStream);

		assertEquals("The Founding of Durotar", parsed.getName());
		assertEquals("Hard", parsed.getDifficulty());
		assertEquals("Blizzard Entertainment", parsed.getAuthor());
		assertEquals("Rexxar's Kalimdor journey", parsed.getDescription());
		assertEquals("Chapter One: To Tame a Land", parsed.getMapButtons().get(0).getChapterTitle());
		assertEquals("Act I", parsed.getMapButtons().get(0).getChapterSubtitle());
	}

	@Test
	void testFallbackWhenWtsNull() throws IOException {
		final W3nCampaign original = new W3nCampaign();
		original.setName("TRIGSTR_001");

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final LittleEndianDataOutputStream dos = new LittleEndianDataOutputStream(baos);
		W3nCampaignParser.save(original, dos);
		dos.flush();

		final ByteArrayInputStream w3fStream = new ByteArrayInputStream(baos.toByteArray());
		final W3nCampaign parsed = W3nCampaignParser.parse(w3fStream);

		assertEquals("TRIGSTR_001", parsed.getName());
	}
}
