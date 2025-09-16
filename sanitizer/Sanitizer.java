package sanitizer;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class Sanitizer {
    private static final String[] directives = {
        "ifdef",
        "enddef",
        "define",
        "undef",
        "ifhave",
        "ifver",
        "arg",
        "endarg",
        "textdomain"
    };
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Sanitizer <file-or-directory>");
            System.exit(1);
        }

        Path startPath = Paths.get(args[0]);

        try {
            if (Files.isDirectory(startPath)) {
                // Recursively process all files in directory, following symlinks
                Files.walk(startPath, FileVisitOption.FOLLOW_LINKS)
                     .filter(Files::isRegularFile)
                     .forEach(Sanitizer::processFile);
            } else {
                // Single file
                processFile(startPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processFile(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            List<String> modified = new ArrayList<>();
            boolean changed = false;
            boolean skipLine = false;

            for (String line : lines) {
                String trimmed = line.stripLeading();
                if (trimmed.startsWith("#")) {
                    for (var directive : directives) {
                        if (trimmed.startsWith("#" + directive)) {
                            skipLine = true;
                        }
                    }
                    
                    if (skipLine) {
                        skipLine = false;
                        modified.add(line);
                        continue;
                    }
                    
                    int hashIndex = line.indexOf('#');
                    if (hashIndex != -1) {
                        if (line.length() == hashIndex + 1
                            || (line.charAt(hashIndex + 1) != ' '
                                && line.charAt(hashIndex + 1) != '\t'
                                && line.charAt(hashIndex + 1) != '#'))
                        {
                            // Insert a space after '#'
                            line = line.substring(0, hashIndex + 1) + " " + line.substring(hashIndex + 1);
                            changed = true;
                        }
                    }
                }
                modified.add(line);
            }

            if (changed) {
                Files.write(filePath, modified);
                System.out.println("Changed: " + filePath);
            }
        } catch (IOException e) {
            System.err.println("Error processing file: " + filePath);
            e.printStackTrace();
        }
    }
}
