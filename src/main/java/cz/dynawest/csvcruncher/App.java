package cz.dynawest.csvcruncher;

import static cz.dynawest.csvcruncher.Options.CombineDirectories.*;
import static cz.dynawest.csvcruncher.Options.CombineInputFiles.CONCAT;
import static cz.dynawest.csvcruncher.Options.CombineInputFiles.EXCEPT;
import static cz.dynawest.csvcruncher.Options.CombineInputFiles.INTERSECT;
import cz.dynawest.logging.LoggingUtils;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

/*
 * This was written long ago and then lost and decompiled from an old .jar of an old version, and refactored a bit.
 * So please be lenient with the code below :)
 */

public class App
{
    static {
        printBanner();
    }

    public static final String STR_USAGE = "Usage: crunch [-in] <inCSV> [-out] <outCSV> [-sql] <SQL>";
    private static final Logger log = Logger.getLogger(App.class.getName());

    public static void main(String[] args) throws Exception
    {
        LoggingUtils.initLogging();

        try {
            Options options = parseArgs(args);
            log.info("Options: \n" + options);
            (new Cruncher(options)).crunch();
        }
        catch (IllegalArgumentException var3) {
            System.out.println("" + var3.getMessage());
            System.exit(1);
        }
        catch (Throwable ex) {
            log.log(Level.SEVERE,"CSV Cruncher failed: " + ex.getMessage(), ex);
            System.exit(127);
        }
    }

    private static Options parseArgs(String[] args)
    {
        Options opt = new Options();
        int relPos = -1;
        App.OptionsNext next = null;

        log.fine(" Parameters: ");
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            //System.out.println(" * " + arg);
            log.fine(" * " + arg);

            // JSON output
            if (arg.startsWith("--" + Options.JsonExportFormat.PARAM_NAME)) {
                if (arg.endsWith("=" + Options.JsonExportFormat.ARRAY.getOptionValue()))
                    opt.jsonExportFormat = Options.JsonExportFormat.ARRAY;
                else
                    opt.jsonExportFormat = Options.JsonExportFormat.ENTRY_PER_LINE;
            }

            // Row numbers
            else if (arg.startsWith("--rowNumbers")) {
                opt.initialRowNumber = -1L;
                if (arg.startsWith("--rowNumbers=")) {
                    String numberStr = StringUtils.removeStart(arg, "--rowNumbers=");
                    try {
                        long number = Long.parseLong(numberStr);
                        opt.initialRowNumber = number;
                    } catch (Exception ex) {
                        throw new RuntimeException("Not a valid number: " + numberStr + ". " + ex.getMessage(), ex);
                    }
                }
            }

            // Combine input files
            else if (arg.startsWith("--" + Options.CombineInputFiles.PARAM_NAME)) {
                if (arg.endsWith("--" + Options.CombineInputFiles.PARAM_NAME) ||
                        arg.endsWith("=" + CONCAT.getOptionValue() ))
                    opt.combineInputFiles = CONCAT;
                else if (arg.endsWith("=" + INTERSECT.getOptionValue()))
                    opt.combineInputFiles = INTERSECT;
                else if (arg.endsWith("=" + EXCEPT.getOptionValue()))
                    opt.combineInputFiles = EXCEPT;
                else
                    throw new IllegalArgumentException(String.format(
                            "Unknown value for %s: %s Try one of %s",
                            Options.CombineInputFiles.PARAM_NAME, arg,
                            EnumUtils.getEnumList(Options.CombineInputFiles.class).stream().map(Options.CombineInputFiles::getOptionValue).collect(Collectors.toList())
                    ));
                // TODO: Do something like this instead:
                //opt.combineInputFiles = Utils.processOptionIfMatches(arg, Options.CombineInputFiles.class, Options.CombineInputFiles.CONCAT);
                // Or move it to the respective enum class.
                // Enum<Options.CombineDirectories> val = Options.CombineDirectories.COMBINE_ALL_FILES;
            }

            // Combine files in input directories
            else if (arg.startsWith("--" + Options.CombineDirectories.PARAM_NAME)) {
                // Sorted from most fine-grained to least.
                if (arg.endsWith("=" + COMBINE_PER_EACH_DIR.getOptionValue()))
                    opt.combineDirs = COMBINE_PER_EACH_DIR;
                else if (arg.endsWith("=" + COMBINE_PER_INPUT_SUBDIR.getOptionValue()))
                    opt.combineDirs = COMBINE_PER_INPUT_SUBDIR;
                else if (arg.endsWith("=" + COMBINE_PER_INPUT_DIR.getOptionValue()))
                    opt.combineDirs = COMBINE_PER_INPUT_DIR;
                else if (arg.endsWith("=" + COMBINE_ALL_FILES.getOptionValue()))
                    opt.combineDirs = COMBINE_ALL_FILES;
                else if (arg.equals("--" + Options.CombineDirectories.PARAM_NAME))
                    opt.combineDirs = COMBINE_ALL_FILES;
                else {
                    throw new IllegalArgumentException(String.format(
                        "Unknown value for %s: %s Try one of %s",
                        Options.CombineDirectories.PARAM_NAME, arg,
                        EnumUtils.getEnumList(Options.CombineInputFiles.class).stream().map(Options.CombineInputFiles::getOptionValue).collect(Collectors.toList())
                    ));
                }
            }

            else if ("-in".equals(arg)) {
                next = App.OptionsNext.IN;
            }
            else if ("-out".equals(arg)) {
                next = App.OptionsNext.OUT;
                relPos = 2;
            }
            else if ("-sql".equals(arg)) {
                next = App.OptionsNext.SQL;
                relPos = 3;
            }
            else if ("-db".equals(arg)) {
                next = App.OptionsNext.DBPATH;
            }

            else if ("-v".equals(arg) || "--version".equals(arg)) {
                String version = Utils.getVersion();
                System.out.println(" CSV Cruncher version " + version);
                System.exit(0);
            }

            else if ("-h".equals(arg) || "--vhelp".equals(arg)) {
                String version = Utils.getVersion();
                System.out.println(" CSV Cruncher version " + version);
                printUsage(System.out);
                System.exit(0);
            }

            else if (arg.startsWith("-")) {
                System.out.println("ERROR: Unknown parameter: " + arg);
                System.exit(1);
            }

            else {
                if (next != null) {
                    switch (next) {
                        case IN:
                            opt.inputPaths.add(arg);
                            relPos = Math.max(relPos, 1);
                            continue;
                        case OUT:
                            opt.outputPathCsv = arg;
                            relPos = Math.max(relPos, 2);
                            continue;
                        case SQL:
                            opt.sql = arg;
                            relPos = Math.max(relPos, 3);
                            continue;
                        case DBPATH:
                            opt.dbPath = arg;
                            continue;
                        default:
                            next = null;
                    }
                }

                ++relPos;
                if (relPos == 0) {
                    opt.inputPaths.add(arg);
                }
                else if (relPos == 1) {
                    opt.outputPathCsv = arg;
                }
                else {
                    if (relPos != 2) {
                        printUsage(System.out);
                        throw new IllegalArgumentException("Wrong arguments. Usage: crunch [-in] <inCSV> [-out] <outCSV> [-sql] <SQL> ...");
                    }

                    opt.sql = arg;
                }
            }
        }

        if (!opt.isFilled()) {
            printUsage(System.out);
            throw new IllegalArgumentException("Not enough arguments. Usage: crunch [-in] <inCSV> [-out] <outCSV> [-sql] <SQL> ...");
        }
        else {
            return opt;
        }
    }

    private static void printBanner() {
        System.out.println(
        "\n" +
        "\n" +
        "   ____________    __   ______                      __             \n" +
        "  / ____/ ___/ |  / /  / ____/______  ______  _____/ /_  ___  _____\n" +
        " / /    \\__ \\| | / /  / /   / ___/ / / / __ \\/ ___/ __ \\/ _ \\/ ___/\n" +
        "/ /___ ___/ /| |/ /  / /___/ /  / /_/ / / / / /__/ / / /  __/ /    \n" +
        "\\____//____/ |___/   \\____/_/   \\__,_/_/ /_/\\___/_/ /_/\\___/_/     \n" +
        "                                                                   \n" +
        "\n"
        );
    }

    private static void printUsage(PrintStream dest)
    {
        dest.println("  Usage:");
        dest.println("    crunch [-in] <inCSV> [<inCSV> ...] [-out] <outCSV> [--<option> --...] [-sql] <SQL>");
    }

    private enum OptionsNext
    {
        IN,
        OUT,
        SQL,
        DBPATH;
    }

}
