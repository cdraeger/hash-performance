package io.draeger.hash;

import jline.console.ConsoleReader;
import org.apache.commons.cli.*;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * Command-line application for testing the hardware-dependent performance of an hash-algorithm (currently
 * only bcrypt is supported, this might change in the future).
 * <p>
 * It takes the user-input as the log2 of the number of rounds of hashing to apply, and displays the total
 * time consumed by the hash-function afterwards. The work factor and therefore the duration of the hashing
 * increases exponentially (2^x), which is important to know for balancing performance and security.
 *
 * @author Carsten Draeger
 * @since 16.01.15 23:39
 */
public class PerformanceTest {

  static final String ANSI_RESET = "\u001B[0m";
  static final String ANSI_BLACK = "\u001B[30m";
  static final String ANSI_RED = "\u001B[31m";
  static final String ANSI_GREEN = "\u001B[32m";
  static final String ANSI_YELLOW = "\u001B[33m";
  static final String ANSI_BLUE = "\u001B[34m";
  static final String ANSI_MAGENTA = "\u001B[35m";
  static final String ANSI_CYAN = "\u001B[36m";
  static final String ANSI_WHITE = "\u001B[37m";

  static final String ANSI_BOLD = "\u001B[1m";

  static final String DEFAULT_PASSWORD = "j77*h&DEDYpLpZs3";

  static final String[] OPTION_HELP = new String[] {"h", "help"};

  static final String[] OPTION_STRING = new String[] {"s", "string"};
  static final String[] OPTION_MILLIS = new String[] {"m", "millis"};
  static final String[] OPTION_PRINT_HASH = new String[] {"p", "print"};
  static final String[] OPTION_COLOR = new String[] {"c", "color"};

  static final String[] OPTION_QUIT = new String[] {"q", "quit", "exit"};
  static final String OPTION_CLEAR_SCREEN = "cls";

  static final Character MASK_CHAR = '\0';

  static final Options cliOptions = new Options();
  static final CommandLineParser cliParser = new BasicParser();
  static final HelpFormatter cliHelpFormatter = new HelpFormatter();

  private final ConsoleReader reader;

  private final boolean showColor;
  private final String colorSuccess;

  private final String colorInfo;
  private final String colorInfo2;
  private final String colorWarning;
  private final String colorError;
  private final String colorHash;
  private final String colorDuration;
  private final String colorReset;
  private final String colorPrompt;

  private boolean durationInMillis;
  private boolean printHash;
  private String password;

  public static void main(String[] args) {
    addOptions();

    try {
      final CommandLine cmdLine = cliParser.parse(cliOptions, args);

      if (cmdLine.hasOption(OPTION_HELP[0])) {
        printHelp();
        return;
      }

      final boolean durationInMillis = cmdLine.hasOption(OPTION_MILLIS[0]);
      final boolean showColor = !cmdLine.hasOption(OPTION_COLOR[0]);
      final boolean printHash = cmdLine.hasOption(OPTION_PRINT_HASH[0]);
      final String password = cmdLine.getOptionValue(OPTION_STRING[0], DEFAULT_PASSWORD);

      new PerformanceTest(password, durationInMillis, printHash, showColor);
    } catch (ParseException e) {
      printHelp();
      System.out.println(String.format("%sParsing error: %s%s", ANSI_RED, e.getMessage(), ANSI_RESET));
    } catch (IOException e) {
      System.out.println(String.format("%sIO-Exception: %s%s", ANSI_RED, e.getMessage(), ANSI_RESET));
    }
  }

  private static void addOptions() {
    final Option helpOption = new Option(OPTION_HELP[0], OPTION_HELP[1], false, "shows this help.");

    final Option passwordOption = new Option(OPTION_STRING[0], OPTION_STRING[1], true,
        "sets the string used for the hash-function" +
            " (optional, by default a hardcoded random 16-character string will be used).");

    final Option millisOption = new Option(OPTION_MILLIS[0], OPTION_MILLIS[1], false,
        "enables output in milliseconds (optional, default: ISO-8601).");

    final Option printHashOption = new Option(OPTION_PRINT_HASH[0], OPTION_PRINT_HASH[1], false,
        "enables printing of the resulting hash to the console (optional, default: disabled).");

    final Option colorOption = new Option(OPTION_COLOR[0], OPTION_COLOR[1], false,
        "disables colorized output (optional, default: enabled)");

    cliOptions.addOption(helpOption);
    cliOptions.addOption(passwordOption);
    cliOptions.addOption(millisOption);
    cliOptions.addOption(printHashOption);
    cliOptions.addOption(colorOption);
  }

  private static void printHelp() {
    cliHelpFormatter.printHelp("java -jar hash-performance.jar [options]", cliOptions);
  }

  public PerformanceTest(String password, boolean durationInMillis, boolean printHash, boolean showColor) throws IOException {
    this.password = password;
    this.durationInMillis = durationInMillis;
    this.printHash = printHash;
    this.showColor = showColor;

    colorSuccess = this.showColor ? ANSI_GREEN : "";
    colorInfo = this.showColor ? ANSI_YELLOW : "";
    colorInfo2 = this.showColor ? ANSI_MAGENTA : "";
    colorWarning = this.showColor ? ANSI_RED : "";
    colorError = this.showColor ? ANSI_RED : "";
    colorHash = this.showColor ? ANSI_YELLOW : "";
    colorDuration = this.showColor ? ANSI_GREEN : "";
    colorReset = this.showColor ? ANSI_RESET : "";

    final String ansiDescription = this.showColor ? ANSI_BOLD : "";

    final String title = String.format("%s<<< Hash Performance Testing Tool (bcrypt) >>>%s", ANSI_BOLD, ANSI_RESET);

    final String description = String.format(
        "\n%sPlease enter the log2 of the number of rounds of hashing to apply. Start with" +
            "\nlower numbers (e.g. 10), since the work factor and therefore the duration of" +
            "\nthe hashing increases exponentially (2^x).%s",
            ansiDescription, colorReset);


    System.out.println(title);
    System.out.println(description);

    printRuntimeHelp(true);

    this.reader = new ConsoleReader();

    colorPrompt = this.showColor ? ANSI_CYAN : "";
    resetPrompt();

    processInput();
  }

  private void printRuntimeHelp(boolean includeCommandLineHelpOptionInfo) {
    final String cliHelpOptionInfo = includeCommandLineHelpOptionInfo ?
        String.format("Use the '-%s','--%s' command-line option for information about optional parameters.\n\n",
        OPTION_HELP[0], OPTION_HELP[1])
        : "";

    final String info = String.format(
        "\n%s%s" +
            "Type%s\n - '%s', '%s' or '%s' to leave this application, " +
            "\n - '%s' or '%s' to show this help for parameters at runtime," +
            "\n - '%s' to switch between duration output in milliseconds or ISO-8601 representation," +
            "\n - '%s' to enable or disable printing of the resulting hash to the console," +
            "\n - '%s' to change the string used for hashing," +
            "\n - '%s' to clear the screen." +
            "%s\n",
        colorInfo2,
        cliHelpOptionInfo,
        includeCommandLineHelpOptionInfo ? " (at runtime)" : "",
        OPTION_QUIT[0], OPTION_QUIT[1], OPTION_QUIT[2],
        OPTION_HELP[0], OPTION_HELP[1],
        OPTION_MILLIS[0],
        OPTION_PRINT_HASH[0],
        OPTION_STRING[0],
        OPTION_CLEAR_SCREEN,
        colorReset);
    System.out.println(info);
  }

  private void resetPrompt() {
    final String prompt = String.format("%slog2>%s ", colorPrompt, colorReset);
    reader.setPrompt(prompt);
  }

  private void processInput() throws IOException {
    final int minRounds = 4, maxRounds = 30;
    final String validRangeInfo = String.format("Please enter a valid integer value [%d-%d]", minRounds, maxRounds);

    String line;
    PrintWriter out = new PrintWriter(reader.getOutput());
    while ((line = reader.readLine()) != null) {
      if (Arrays.asList(OPTION_QUIT).contains(line)) {
        break;
      }

      // typing the 'string'-option will mask the next line (for entering
      // a password). The input will be used as the string to be hashed
      if (line.equalsIgnoreCase(OPTION_STRING[0])) {
        final String info = String.format(
            "%sPlease enter the new password to be used for the hash-function (input is masked)." +
                "\nNo input will reset to the default string.%s",
            colorInfo, colorReset);
        out.println(info);
        out.flush();

        final String prompt = String.format("%spassword>%s ", colorPrompt, colorReset);
        final String newPassword = reader.readLine(prompt, MASK_CHAR);
        final String confirm;
        if (newPassword != null && newPassword.length() > 0) {
          this.password = newPassword;
          confirm = String.format(
              "%sNew password-string set (length: %d). Please continue...%s",
              colorSuccess, this.password.length(), colorReset);
        } else {
          this.password = DEFAULT_PASSWORD;
          confirm = String.format(
              "%sPassword-string set to default (length: %d).%s",
              colorSuccess, this.password.length(), colorReset);
        }
        out.println(confirm);
        out.flush();

        resetPrompt();
      } else if (line.equalsIgnoreCase(OPTION_HELP[0]) || line.equalsIgnoreCase(OPTION_HELP[1])) {
          printRuntimeHelp(false); // runtime help makes more sense at this point than help for command-line options
      } else if (line.equalsIgnoreCase(OPTION_MILLIS[0]) || line.equalsIgnoreCase(OPTION_MILLIS[1])) {
        durationInMillis = !durationInMillis;
        final String message = String.format(
            "%sDuration output format changed to %s%s", colorSuccess, durationInMillis ? "milliseconds" : "ISO-8601", colorReset);
        out.println(message);
        out.flush();
      } else if (line.equalsIgnoreCase(OPTION_PRINT_HASH[0]) || line.equalsIgnoreCase(OPTION_PRINT_HASH[1])) {
        printHash = !printHash;
        final String message = String.format(
            "%sPrinting of the resulting hash %s%s", colorSuccess, printHash ? "activated" : "deactivated", colorReset);
        out.println(message);
        out.flush();
      } else if (line.equalsIgnoreCase(OPTION_CLEAR_SCREEN)) {
        reader.clearScreen();
      } else { // expecting number of rounds here
        try {
          final int rounds = Integer.parseInt(line);
          if (rounds < minRounds || rounds > maxRounds) {
            out.println(String.format("%s%s%s", colorInfo, validRangeInfo, colorReset));
            out.flush();
          } else {
            hashPassword(out, rounds);
          }
        } catch (NumberFormatException e) {
          final String nanError;
          if (line.length() > 0) {
            nanError = String.format("'%s' is not a number. %s.", line, validRangeInfo);
          } else {
            nanError = validRangeInfo;
          }
          out.println(String.format("%s%s%s", line.length() > 0 ? colorWarning : colorInfo, nanError, colorReset));
          out.flush();
        }
      }
    }
  }

  /**
   * To maintain Java 1.6/1.7 backwards compatibility, Joda Time is used. This
   * adds overhead and significantly increases the file size, but spared me manual
   * ISO 8601 formatting while not limiting compatibility to Java 1.8 and above.
   *
   * If only millisecond display is required, {@link System#currentTimeMillis()}
   * would suffice of course. If Java 8 support only is fine, use
   * - java.time.Instant.now()
   * - java.time.Duration.between(start, end)
   */
  private void hashPassword(PrintWriter out, int rounds) {
    try {
      final Instant startJT = Instant.now();

      // start hashing
      final String hash = BCrypt.hashpw(password, BCrypt.gensalt(rounds));
      final Instant endJT = Instant.now();
      final Interval interval = new Interval(startJT, endJT);
      final Duration duration = interval.toDuration();

      if (printHash) {
        out.println(String.format("Hash: %s%s%s", colorHash, hash, colorReset));
      }

      final String durationFormatted = durationInMillis ? duration.getMillis() + "ms" : duration.toString();
      out.println(String.format("Duration: %s%s%s (2^%d rounds)", colorDuration, durationFormatted, colorReset, rounds));
      out.flush();
    } catch (Exception e) {
      System.out.println(String.format("%sError: %s%s", colorError, e.getMessage(), colorReset));
    }
  }
}
