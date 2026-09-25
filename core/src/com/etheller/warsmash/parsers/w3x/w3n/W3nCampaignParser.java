package com.etheller.warsmash.parsers.w3x.w3n;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.units.custom.WTS;
import com.etheller.warsmash.units.custom.WTSFile;
import com.etheller.warsmash.util.ParseUtils;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

/**
 * Parser and serializer for war3campaign.w3f and custom campaigns (.w3n).
 */
public final class W3nCampaignParser {
	public static final String W3F_FILE_NAME = "war3campaign.w3f";
	public static final String WTS_FILE_NAME = "war3campaign.wts";

	private W3nCampaignParser() {
	}

	public static W3nCampaign parse(final DataSource dataSource) throws IOException {
		if (!dataSource.has(W3F_FILE_NAME)) {
			throw new IOException("DataSource does not contain " + W3F_FILE_NAME);
		}
		WTS wts = null;
		if (dataSource.has(WTS_FILE_NAME)) {
			try (InputStream wtsStream = dataSource.getResourceAsStream(WTS_FILE_NAME)) {
				wts = new WTSFile(wtsStream);
			}
		}
		try (InputStream w3fStream = dataSource.getResourceAsStream(W3F_FILE_NAME)) {
			return parse(w3fStream, wts);
		}
	}

	public static W3nCampaign parse(final InputStream w3fStream) throws IOException {
		return parse(w3fStream, (WTS) null);
	}

	public static W3nCampaign parse(final InputStream w3fStream, final InputStream wtsStream) throws IOException {
		WTS wts = null;
		if (wtsStream != null) {
			wts = new WTSFile(wtsStream);
		}
		return parse(w3fStream, wts);
	}

	public static W3nCampaign parse(final InputStream w3fStream, final WTS wts) throws IOException {
		final LittleEndianDataInputStream stream = new LittleEndianDataInputStream(w3fStream);
		final W3nCampaign campaign = new W3nCampaign();

		campaign.setVersion(stream.readInt());
		campaign.setCampaignVersion(stream.readInt());
		campaign.setEditorVersion(stream.readInt());

		campaign.setName(resolveString(wts, ParseUtils.readUntilNull(stream)));
		campaign.setDifficulty(resolveString(wts, ParseUtils.readUntilNull(stream)));
		campaign.setAuthor(resolveString(wts, ParseUtils.readUntilNull(stream)));
		campaign.setDescription(resolveString(wts, ParseUtils.readUntilNull(stream)));

		campaign.setVariableDifficulty(stream.readInt());
		campaign.setCampaignBackground(stream.readInt());
		campaign.setBackgroundScreenModel(ParseUtils.readUntilNull(stream));
		campaign.setMinimapPath(ParseUtils.readUntilNull(stream));
		campaign.setAmbientSound(stream.readInt());
		campaign.setCustomAmbientSoundPath(ParseUtils.readUntilNull(stream));

		campaign.setTerrainFog(stream.readInt());
		ParseUtils.readFloatArray(stream, campaign.getFogHeight());
		campaign.setFogDensity(stream.readFloat());
		ParseUtils.readUInt8Array(stream, campaign.getFogColor());

		campaign.setUiRace(stream.readInt());

		final int buttonCount = stream.readInt();
		for (int i = 0; i < buttonCount; i++) {
			final int visible = stream.readInt();
			final String title = resolveString(wts, ParseUtils.readUntilNull(stream));
			final String subtitle = resolveString(wts, ParseUtils.readUntilNull(stream));
			final String mapPath = resolveString(wts, ParseUtils.readUntilNull(stream));
			campaign.getMapButtons().add(new W3nCampaign.MapButton(visible, title, subtitle, mapPath));
		}

		final int mapCount = stream.readInt();
		for (int i = 0; i < mapCount; i++) {
			final int flags = stream.readInt();
			final String mapPath = ParseUtils.readUntilNull(stream);
			campaign.getMaps().add(new W3nCampaign.CampaignMap(flags, mapPath));
		}

		return campaign;
	}

	public static void save(final W3nCampaign campaign, final LittleEndianDataOutputStream stream) throws IOException {
		stream.writeInt(campaign.getVersion());
		stream.writeInt(campaign.getCampaignVersion());
		stream.writeInt(campaign.getEditorVersion());

		ParseUtils.writeWithNullTerminator(stream, campaign.getName());
		ParseUtils.writeWithNullTerminator(stream, campaign.getDifficulty());
		ParseUtils.writeWithNullTerminator(stream, campaign.getAuthor());
		ParseUtils.writeWithNullTerminator(stream, campaign.getDescription());

		stream.writeInt(campaign.getVariableDifficulty());
		stream.writeInt(campaign.getCampaignBackground());
		ParseUtils.writeWithNullTerminator(stream, campaign.getBackgroundScreenModel());
		ParseUtils.writeWithNullTerminator(stream, campaign.getMinimapPath());
		stream.writeInt(campaign.getAmbientSound());
		ParseUtils.writeWithNullTerminator(stream, campaign.getCustomAmbientSoundPath());

		stream.writeInt(campaign.getTerrainFog());
		ParseUtils.writeFloatArray(stream, campaign.getFogHeight());
		stream.writeFloat(campaign.getFogDensity());
		ParseUtils.writeUInt8Array(stream, campaign.getFogColor());

		stream.writeInt(campaign.getUiRace());

		final List<W3nCampaign.MapButton> buttons = campaign.getMapButtons();
		stream.writeInt(buttons.size());
		for (final W3nCampaign.MapButton button : buttons) {
			stream.writeInt(button.getVisible());
			ParseUtils.writeWithNullTerminator(stream, button.getChapterTitle());
			ParseUtils.writeWithNullTerminator(stream, button.getChapterSubtitle());
			ParseUtils.writeWithNullTerminator(stream, button.getMapPath());
		}

		final List<W3nCampaign.CampaignMap> maps = campaign.getMaps();
		stream.writeInt(maps.size());
		for (final W3nCampaign.CampaignMap map : maps) {
			stream.writeInt(map.getFlags());
			ParseUtils.writeWithNullTerminator(stream, map.getMapPath());
		}
	}

	public static String resolveString(final WTS wts, final String string) {
		if (string == null) {
			return "";
		}
		if (string.startsWith("TRIGSTR_") && (wts != null)) {
			try {
				final String resolved = wts.get(Integer.parseInt(string.substring(8)));
				if (resolved != null) {
					return resolved;
				}
			}
			catch (final NumberFormatException e) {
				// retain string as-is
			}
		}
		return string;
	}
}
