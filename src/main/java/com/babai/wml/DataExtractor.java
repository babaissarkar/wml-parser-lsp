package com.babai.wml;

import static com.babai.wml.experimental.ParseUtils.csvEscape;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

	public static void generateMacroRef(Path macroRefPath, Table defines, HashMap<String, String> fileExplanations) {
		try (BufferedWriter writer = Files.newBufferedWriter(macroRefPath)) {
			writer.write("<p class='macro-ref-toc'>Documented files:</p>");
			writer.write("<div class='filelist'><ul>");
			
			// File list at top
			HashMap<String, String> uriList = new LinkedHashMap<>();
			// URI column contains duplicates, suppress them.
			// we only need unique values for getRows() below.
			// List of parsed files at top
			for (var uriObj : defines.getColumn("URI")) {
				String uriStr = (String) uriObj;
				String filename = Path.of(URI.create(uriStr)).getFileName().toString();
				if (!uriList.containsKey(filename)) {
					uriList.put(filename, uriStr);
					writer.write(
						"<li><a href='#file:%s'><code class='noframe'>%s</code></a></li>"
						.formatted(filename, filename));
				}
			}
			
			writeln(writer, "</ul></div>");
			writeln(writer, "<p class='toplink'>[ <a href='#content'>top</a> ]</p>");
			
			// File sections with macros
			for (var entry : uriList.entrySet()) {
				String name = entry.getKey();
				String uriStr = entry.getValue();
				writeln(writer,
					"<h2 id='file:%s' class='file_header'>From file: <code class='noframe'><a href='%s'>%s</a></code></h2>"
					.formatted(name, uriStr, name));
				
				String fileDoc = fileExplanations.get(uriStr);
				writeln(writer, "<p class='file_explanation'>" + fileDoc);
				writeln(writer, "</p>");
				
				writeln(writer, "<dl>");
				writer.newLine();
				
				for (var row : defines.getRows("URI", uriStr)) {
					String macroName = (String) row.getColumn("Name").getValue();
					if (macroName.startsWith("INTERNAL:")) continue;
					Definition def = (Definition) row.getColumn("Definition").getValue();
					
					// Macro name and arguments
					writeln(writer, "<dt id='" + macroName + "'>");
					writer.write("<code class='noframe'><span class='macro-name'>" + macroName + "</span>");
					if (def.getArgCount() > 0) {
						writer.write(" <var class='macro-formals'>" + String.join(" ", def.getArgs()) + "</var>");
					}
					if (def.getDefArgCount() > 0) {
						var defArgList = def.getDefArgs().keySet();
						writer.write(
							" Optional Arguments: <var class='macro-formals'>"
							+ String.join(" ", defArgList) + "</var>");
					}
					writer.newLine();
					writeln(writer, "</code></dt>");
					
					// Macro docstring body
					writeln(writer, "<dd>");
					
					// Deprecation message
					boolean hasDoc = false;
					if (def.isDeprecated()) {
						int deprLevel = def.getDeprecationLevel();
						if (deprLevel == 2 | deprLevel == 3) {
							writeln(writer,
								"<p class='macro-deprecated'><strong>Deprecated macro.</strong> " +
								"<em>Deprecation level: %d. Scheduled for removal in %s.</em></p>"
								.formatted(def.getDeprecationLevel(), def.getDeprecationRemovalVersion()));
						} else if (deprLevel == 1 | deprLevel == 4) {
							writeln(writer,
								"<p class='macro-deprecated'><strong>Deprecated macro.</strong> " +
								"<em>Deprecation level: %d.</em></p>"
								.formatted(def.getDeprecationLevel()));
						}
						
						String msg = def.getDeprecationMessage();
						if (!msg.isEmpty()) {
							hasDoc = true;
							writeln(writer, processDoc(msg));
						}
					}
					
					String docs = def.getDocs().trim();
					hasDoc = hasDoc || !docs.isEmpty();
					if (!hasDoc) {
						writeln(writer, "<p class='macro-missing-docs'><em>No documentation available for this macro.</em></p>");
					} else if (!docs.isEmpty()) {
						writeln(writer, processDoc(docs));
					}
					writeln(writer, "</dd>");
					
					writer.newLine();
				}
				
				writeln(writer, "</dl>");
				writeln(writer, "<p class='toplink'>[ <a href='#content'>top</a> ]</p>");
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to write macro reference html", e);
		}
	}

	private static void writeln(BufferedWriter writer, String str) throws IOException {
		writer.write(str);
		writer.newLine();
	}

	/** Convert docstring lines -> html output */
	private static String processDoc(String docs) {
		var docBuff = new StringBuilder();
		docBuff.append("<p class='macro-explanation'>");
		boolean isCodeBlock = false;
		for (String line : docs.split("\\R")) {
			if (line.startsWith("!")) {
				if (!isCodeBlock) {
					docBuff.append("</p>");
					docBuff.append("\n<pre class='listing'>");
					isCodeBlock = true;
				}
				docBuff.append(line.substring(1, line.length()));
			} else {
				if (isCodeBlock) {
					isCodeBlock = false;
					docBuff.append("\n</pre>");
				}
				docBuff.append(line);
			}
			docBuff.append("\n");
		}
		
		if (isCodeBlock) {
			isCodeBlock = false;
			docBuff.append("</pre>");
		}
		
		if (!docBuff.toString().contains("</p>")) {
			docBuff.append("</p>");
		}
		
		return docBuff.toString();
	}
}
