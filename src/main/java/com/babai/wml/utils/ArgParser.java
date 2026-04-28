package com.babai.wml.utils;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.babai.wml.core.Definition;

public class ArgParser {
    public Level logLevel = Level.INFO;

    @Option(names = {"-color", "--color"}, arity="1", description = "Toggle colored log messages.", paramLabel="<'true'|'false'>")
    public boolean enableColors = true;

    public boolean extractUnitTypeData = false;

    @Option(names = {"-server", "-s", "--server"}, description = "Run in LSP server mode.")
    public boolean startLSPServer = false;

    public boolean generateMacroRef = false;

    @Option(names = {"-q", "-query", "--query"}, description = "XPath-style WML query. Any tag/key matching this will be printed.")
    public List<String> queries = new ArrayList<>();

    @Option(names = {"-include", "--include"}, arity = "1", description = "Preprocess file/folder and collect macro definitions")
    public List<Path> includes = new ArrayList<>();

    public Table predefines = Table.ofWithIndices(
            new Class<?>[] { Integer.class, String.class, String.class, Definition.class },
            new String[] { "Line", "URI", "Name", "Definition" },
            1, 2
    );

    @Option(names = {"-datadir", "--datadir"}, description = "Absolute path to Wesnoth's data directory")
    public Path dataPath;

    @Option(names = {"-userdatadir", "--userdatadir"}, description = "Absolute path to Wesnoth's userdata directory")
    public Path userDataPath;

    @Option(names = {"-i", "-input", "--input"}, description = "Preprocess the main input file (default: stdin)")
    public Path inputPath;

    public Path outputPath;
    public Path unitTypeOutPath;
    public Path macroRefPath;
    public PrintStream out = null;

    @Option(names = {"-h", "-help", "--help", "-?"}, usageHelp = true, description = "Print this help")
    private boolean helpRequested;

    // Setters kept only where side-effects are needed beyond field assignment

    @Option(names = {"-o", "-output", "--output"}, description = "Write output to given file (default: stdout)")
    public void setOutputPath(String path) throws Exception {
        outputPath = Path.of(path);
        out = new PrintStream(Files.newOutputStream(outputPath));
    }

    @Option(names = {"-define", "-d", "--define"}, arity = "2", description = "Define macro: -define NAME BODY")
    public void addDefine(String[] nameAndBody) {
        predefines.addRow(0, "predefined", nameAndBody[0], new Definition(nameAndBody[0], nameAndBody[1]));
    }

    @Option(names = {"-log-parse", "-log-p", "--log-parse"}, description = "Print all parser logs (= -log-level debug)")
    public void setLogParse(boolean on) { if (on) logLevel = Level.FINER; }

    @Option(names = {"-warn-parse", "-warn-p", "--warn-parse"}, description = "Print parser warnings only (= -log-level warn)")
    public void setWarnParse(boolean on) { if (on) logLevel = Level.WARNING; }

    @Option(names = {"-log-level", "--log-level"}, description = "Set log level to following values severe|warn|info|debug|off", paramLabel = "<'severe'|'warn'|'info'|'debug'|'off'>")
    public void setLogLevel(String level) {
        logLevel = switch (level) {
            case "severe" -> Level.SEVERE;
            case "warn"   -> Level.WARNING;
            case "info"   -> Level.INFO;
            case "debug"  -> Level.FINER;
            case "off"    -> Level.OFF;
            default       -> Level.INFO;
        };
    }

    @Option(names = {"-extract-unit-type", "-eut", "--extract-unit-type"}, description = "Extract unit type data to CSV at given path", paramLabel = "<outputPath>")
    public void setExtractUnitTypeData(String path) {
        extractUnitTypeData = true;
        unitTypeOutPath = Path.of(path);
    }

    @Option(names = {"-generate-macro-ref", "-gmr", "--generate-macro-ref"}, description = "Generate HTML macro reference file", paramLabel = "<outputPath>")
    public void setGenerateMacroRef(String path) {
        generateMacroRef = true;
        macroRefPath = Path.of(path);
    }

    public void parseArgs(String[] args) {
        CommandLine cmd = new CommandLine(this);
        cmd.setUsageHelpAutoWidth(true);
        try {
            CommandLine.ParseResult result = cmd.parseArgs(args);
            if (result.isUsageHelpRequested()) {
                cmd.usage(System.out);
                System.exit(0);
            }
        } catch (CommandLine.ParameterException ex) {
            System.err.println(ex.getMessage());
            cmd.usage(System.err);
        }
    }
}
