package com.babai.wml;

import static com.babai.wml.experimental.ParseUtils.csvEscape;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

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
			writer.write("<p class='macro-ref-toc'>Documented files:</p>");
			writer.write("<div class='filelist'><ul>");
			
			HashMap<String, String> uriList = new LinkedHashMap<>();
			// URI column contains duplicates, suppress them.
			// we only need unique values for getRows() below.
			// List of parsed files at top
			for (var uriObj : defines.getColumn("URI")) {
				String uriStr = (String) uriObj;
				String name = Path.of(URI.create(uriStr)).getFileName().toString();
				var success = uriList.put(name, uriStr);
				if (success == null) {
					writer.write("<li><a href='" + uriStr + "'><code class='noframe'>" + name + "</code></a></li>");
				}
			}
			
			writer.write("</div></ul>");
			writer.newLine();
			writer.write("<p class='toplink'>[ <a href='#content'>top</a> ]</p>");
			writer.newLine();
			
			// File sections with macros
			for (var entry : uriList.entrySet()) {
				String name = entry.getKey();
				String uriStr = entry.getValue();
				writer.write("<h2 id='" + uriStr + "' class='file_header'>From file: <code class='noframe'>" + name + "</code></h2>");
				writer.newLine();
				writer.write("<dl>");
				
				//TODO top of file docs: <p class="file_explanation"> 
				
				for (var row : defines.getRows("URI", uriStr)) {
					String macroName = (String) row.getColumn("Name").getValue();
					if (macroName.startsWith("INTERNAL:")) continue;
					Definition def = (Definition) row.getColumn("Definition").getValue();
					
					writer.write("<dt id='" + macroName + "'>");
					writer.write("<code class='noframe'><span class='macro-name'>" + macroName + "</code></dt>");
					writer.newLine();
					
					String docs = def.getDocs();
					if (!docs.isEmpty()) {
						writer.write("<dd><p class=\"macro-explanation\">" + docs + "</p></dd>");
					} else {
						writer.write("<dd><p class=\"macro-explanation\"><i>No documentation found</i></p></dd>");
					}
					writer.newLine();
				}
				
				writer.write("</dl>");
				writer.newLine();
				writer.write("<p class='toplink'>[ <a href='#content'>top</a> ]</p>");
				writer.newLine();
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to write macro reference html", e);
		}
	}
}
