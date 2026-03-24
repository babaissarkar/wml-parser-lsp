package com.babai.wml;

import static com.babai.wml.experimental.ParseUtils.csvEscape;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import com.babai.wml.core.Config;
import com.babai.wml.core.ConfigAttributeBase;
import com.babai.wml.core.Definition;
import com.babai.wml.utils.Table;

public class DataExtractor {
	
	public static void writeUnitTypeData(HashSet<Config> unitTypeData, Path unitTypeOutPath) {
		final String[] UNIT_TYPE_COLUMNS = {
			"id",
			"race",
			"gender",
			"hitpoints",
			"movement_type",
			"movement",
			"experience",
			"level",
			"alignment",
			"advances_to",
			"cost",
			"usage",
			"name",
			"image",
			"profile",
			"description"
		};

		try (BufferedWriter writer = Files.newBufferedWriter(unitTypeOutPath)) {

			// Header
			writer.write(String.join(",", UNIT_TYPE_COLUMNS));
			writer.newLine();

			for (Config cfg : unitTypeData) {
				StringBuilder row = new StringBuilder();

				for (int i = 0; i < UNIT_TYPE_COLUMNS.length; i++) {
					if (i > 0) row.append(',');

					String key = UNIT_TYPE_COLUMNS[i];
					ConfigAttributeBase attr = cfg.getAttr(key);

					String value = (attr == null) ? "" : attr.stringValue();
					row.append(csvEscape(value));
				}

				writer.write(row.toString());
				writer.newLine();
			}

		} catch (IOException e) {
			throw new UncheckedIOException("Failed to write unit type CSV", e);
		}
	}

	public static void generateMacroRef(Path macroRefPath, Table defines) {
		try (BufferedWriter writer = Files.newBufferedWriter(macroRefPath)) {
			HashSet<String> uriList = new HashSet<>();
			// URI column contains duplicates, suppress them.
			// we only need unique values for getRows() below.
			for (var uriObj : defines.getColumn("URI")) {
				uriList.add((String) uriObj);
			}
			
			for (String uri : uriList) {
				writer.write("URI: " + uri);
				writer.newLine();
				for (var row : defines.getRows("URI", uri)) {
					String macroName = (String) row.getColumn("Name").getValue();
					if (macroName.startsWith("INTERNAL:")) continue;
					Definition def = (Definition) row.getColumn("Definition").getValue();
					writer.write("\tMacro: " + macroName);
					String docs = def.getDocs();
					if (!docs.isEmpty()) {
						writer.write("\t" + docs);
					} else {
						writer.write("\t No documentation found");
					}
					writer.newLine();
				}
				writer.newLine();
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to write macro reference html", e);
		}
	}
}
