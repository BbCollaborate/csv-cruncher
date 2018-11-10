package cz.dynawest.csvcruncher;

import static cz.dynawest.csvcruncher.Options.CombineDirectories.*;
import static cz.dynawest.csvcruncher.Options.CombineInputFiles.CONCAT;
import static cz.dynawest.csvcruncher.Options.CombineInputFiles.EXCEPT;
import static cz.dynawest.csvcruncher.Options.CombineInputFiles.INTERSECT;
import cz.dynawest.logging.LoggingUtils;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
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

            // Include / exclude file paths
            else if (arg.startsWith("--include")) {
                if (!arg.startsWith("--include=")) {
                    throw new IllegalArgumentException("Option --include has to have a value (regular expression).");
                }
                String regex = StringUtils.removeStart(arg, "--include=");
                try {
                    opt.includePathsRegex = Pattern.compile(regex);
                } catch (Exception ex) {
                    throw new CsvCruncherException("Not a valid regex: " + regex + ". " + ex.getMessage(), ex);
                }
            }
            else if (arg.startsWith("--exclude")) {
                if (!arg.startsWith("--exclude=")) {
                    throw new IllegalArgumentException("Option --exclude has to have a value (regular expression).");
                }
                String regex = StringUtils.removeStart(arg, "--exclude=");
                try {
                    opt.excludePathsRegex = Pattern.compile(regex);
                } catch (Exception ex) {
                    throw new CsvCruncherException("Not a valid regex: " + regex + ". " + ex.getMessage(), ex);
                }
            }

            // Overwrite the output file(s), if they exist.
            else if (arg.startsWith("--overwrite")) {
                opt.overwrite = true;
            }

            // Ignore first N lines
            else if (arg.startsWith("--ignoreFirstLines")) {
                opt.ignoreFirstLines = 1;
                if (arg.startsWith("--ignoreFirstLines=")) {
                    String numberStr = StringUtils.removeStart(arg, "--ignoreFirstLines=");
                    try {
                        int number = Integer.parseInt(numberStr);
                        opt.ignoreFirstLines = number;
                    } catch (Exception ex) {
                        throw new CsvCruncherException("Not a valid number: " + numberStr + ". " + ex.getMessage(), ex);
                    }
                }
            }

            // Ignore lines matching a regex.
            else if (arg.startsWith("--ignoreLinesMatching")) {
                if (!arg.startsWith("--ignoreLinesMatching=")) {
                    throw new IllegalArgumentException("Option --ignoreLinesMatching has to have a value (regular expression).");
                }

                String regex = StringUtils.removeStart(arg, "--ignoreFirstLines=");
                try {
                    opt.ignoreLineRegex = Pattern.compile(regex);
                } catch (Exception ex) {
                    throw new CsvCruncherException("Not a valid regex: " + regex + ". " + ex.getMessage(), ex);
                }
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
                        throw new CsvCruncherException("Not a valid number: " + numberStr + ". " + ex.getMessage(), ex);
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
            else if ("--skipNonReadable".equals(arg)) {
                opt.skipNonReadable = true;
            }
            else if ("--keepWorkFiles".equals(arg)) {
                opt.keepWorkFiles = true;
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

        // HSQLDB bug, see https://stackoverflow.com/questions/52708378/hsqldb-insert-into-select-null-from-leads-to-duplicate-column-name
        if (opt.initialRowNumber != null && opt.sql != null) {

            boolean itsForSure = opt.sql.matches(".*SELECT +\\*.*|.*[^.]\\* +FROM .*");

            if (itsForSure || opt.sql.matches(".*SELECT.*[^.]\\* .*FROM.*")) {
                String msg =
                        "\n    WARNING! It looks like you use --rowNumbers with `SELECT *`." +
                        "\n    Due to a bug in HSQLDB, this causes an error 'duplicate column name in derived table'." +
                        "\n    Use table-qualified way: `SELECT myTable.*`";
                if (itsForSure) {
                    //throw new IllegalArgumentException(msg);
                    log.severe("\n" + msg + "\n\n");
                    System.exit(1);
                } else {
                    String notSure = "\n    (This detection is not reliable so the program will continue, but likely fail.)";
                    log.warning("\n" + msg + notSure + "\n\n");
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
        /*
        dest.println("  Options:");
        dest.println("    --ignoreFirstLines[=<number>]     Ignore first N lines; the first is considered a header with column names.");
        dest.println("    --rowNumbers[=<firstNumber>]      Add an unique incrementing number as a first column.");
        dest.println("    --json[=<firstNumber>]      ");
        TODO: Copy from the README.
        */
    }

    private enum OptionsNext
    {
        IN,
        OUT,
        SQL,
        DBPATH;
    }

}
