package io.draeger.bcrypt;

import jline.console.ConsoleReader;
import org.apache.commons.cli.*;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;

/**
 * Command-line application for testing the hardware-dependent performance of the bcrypt hash-algorithm.
 * <p>
 * It takes the user-input as the log2 of the number of rounds of hashing to apply, and displays the total
 * time consumed by the hash-function afterwards. The work factor and therefore the duration of the hashing
 * increases exponentially (2^x), which is important to know for balancing performance and security.
 *
 * @author Carsten Draeger
 * @since 16.01.15 23:39
 */
public class PerformanceTest {

  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_BLACK = "\u001B[30m";
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_MAGENTA = "\u001B[35m";
  public static final String ANSI_CYAN = "\u001B[36m";
  public static final String ANSI_WHITE = "\u001B[37m";

  public static final String ANSI_BOLD = "\u001B[1m";

  private static final String DEFAULT_PASSWORD = "j77*h&DEDYpLpZs3";

  private static final String OPTION_HELP = "h";

  private static final String OPTION_STRING = "s";
  private static final String OPTION_MILLIS = "m";
  private static final String OPTION_PRINT_HASH = "p";
  private static final String OPTION_COLOR = "c";

  private static final Character MASK_CHAR = '\0';
  private static final String MASK_TRIGGER = OPTION_STRING;

  private static final Options cliOptions = new Options();
  private static final CommandLineParser cliParser = new BasicParser();
  private static final HelpFormatter cliHelpFormatter = new HelpFormatter();

  private final ConsoleReader reader;

  final boolean durationInMillis;
  final boolean printHash;
  final boolean showColor;

  private final String colorSuccess;
  private final String colorInfo;
  private final String colorWarning;
  private final String colorError;
  private final String colorHash;
  private final String colorDuration;
  private final String colorReset;
  private final String colorPrompt;

  private String password;

  public static void main(String[] args) {
    Option helpOption = new Option(OPTION_HELP, "help", false, "shows this help.");
    cliOptions.addOption(helpOption);

    final Option passwordOption = new Option(OPTION_STRING, "string", true,
        "sets the string used for the hash-function" +
            " (optional, by default a hardcoded random 16-character string will be used).");

    final Option millisOption = new Option(OPTION_MILLIS, "millis", false,
        "enables output in milliseconds (optional, default: ISO-8601).");

    final Option printHashOption = new Option(OPTION_PRINT_HASH, "print", false,
        "enables printing of the resulting hash to the console (optional, default: disabled). The hash will" +
            " only be displayed if you also specified a custom string via the '" + OPTION_STRING + "'-option.");

    final Option colorOption = new Option(OPTION_COLOR, "color", false,
        "disables colorized output (optional, default: enabled)");

    cliOptions.addOption(colorOption);
    cliOptions.addOption(printHashOption);
    cliOptions.addOption(millisOption);
    cliOptions.addOption(passwordOption);

    try {
      final CommandLine cmdLine = cliParser.parse(cliOptions, args);

      if (cmdLine.hasOption(OPTION_HELP)) {
        printHelp();
        return;
      }

      final boolean durationInMillis = cmdLine.hasOption(OPTION_MILLIS);
      final boolean showColor = !cmdLine.hasOption(OPTION_COLOR);
      final boolean printHash = cmdLine.hasOption(OPTION_PRINT_HASH) && cmdLine.hasOption(OPTION_STRING);

      final String string = cmdLine.getOptionValue(OPTION_STRING, DEFAULT_PASSWORD);
      new PerformanceTest(string, durationInMillis, printHash, showColor);
    } catch (ParseException e) {
      printHelp();
      System.out.println(String.format("%sParsing error: %s%s", ANSI_RED, e.getMessage(), ANSI_RESET));
    } catch (IOException e) {
      System.out.println(String.format("%sIO-Exception: %s%s", ANSI_RED, e.getMessage(), ANSI_RESET));
    }
  }

  private static void printHelp() {
    cliHelpFormatter.printHelp(PerformanceTest.class.getPackage().getName() + "-performance", cliOptions);
  }

  public PerformanceTest(String password, boolean durationInMillis, boolean printHash, boolean showColor) throws IOException {
    this.password = password;
    this.durationInMillis = durationInMillis;
    this.printHash = printHash;
    this.showColor = showColor;

    colorSuccess = this.showColor ? ANSI_GREEN : "";
    colorInfo = this.showColor ? ANSI_YELLOW : "";
    colorWarning = this.showColor ? ANSI_RED : "";
    colorError = this.showColor ? ANSI_RED : "";
    colorHash = this.showColor ? ANSI_YELLOW : "";
    colorDuration = this.showColor ? ANSI_GREEN : "";
    colorReset = this.showColor ? ANSI_RESET : "";

    final String ansiDescription = this.showColor ? ANSI_BOLD : "";
    final String colorInfo = this.showColor ? ANSI_MAGENTA : "";

    final String title = String.format("%s<<< bcrypt - Performance Testing Tool >>>%s", ANSI_BOLD, ANSI_RESET);

    final String description = String.format(
        "\n%sPlease enter the log2 of the number of rounds of hashing to apply. Start with" +
            "\nlower numbers (e.g. 10), since the work factor and therefore the duration of" +
            "\nthe hashing increases exponentially (2^x).%s",
            ansiDescription, colorReset);

    final String info = String.format(
        "\n%sUse the '-h','--help' command-line option for information about optional parameters.\n\n" +
            "Type\n - 'q', 'quit' or 'exit' to leave this application, " +
            "\n - 'h' or 'help' to show the help at runtime," +
            "\n - '" + OPTION_STRING + "' to dynamically change the string used for hashing at runtime," +
            "\n - 'cls' to clear the screen." +
            "%s\n",
        colorInfo, colorReset);


    System.out.println(title);
    System.out.println(description);
    System.out.println(info);

    this.reader = new ConsoleReader();

    colorPrompt = this.showColor ? ANSI_CYAN : "";
    resetPrompt();

    processInput();
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
      if (line.equalsIgnoreCase("q") || line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
        break;
      }

      // typing the trigger word will mask the next line and set the input as the new string to be hashed
      if (line.equalsIgnoreCase(MASK_TRIGGER)) {
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
      } else if (line.equalsIgnoreCase("h") || line.equalsIgnoreCase("help")) {
        printHelp();
      } else if (line.equalsIgnoreCase("cls")) {
        reader.clearScreen();
      } else { /* Expecting number of rounds here */
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

  private void hashPassword(PrintWriter out, int rounds) {
    final Instant start = Instant.now();
    final String hash;
    try {
      hash = BCrypt.hashpw(password, BCrypt.gensalt(rounds));

      final Instant end = Instant.now();

      final Duration duration = Duration.between(start, end);
      final String durationFormatted = durationInMillis ? duration.toMillis() + "ms" : duration.toString();

      if (printHash) {
        out.println(String.format("Hash: %s%s%s", colorHash, hash, colorReset));
      }
      out.println(String.format("Duration: %s%s%s (2^%d rounds)", colorDuration, durationFormatted, colorReset, rounds));
      out.flush();
    } catch (Exception e) {
      System.out.println(String.format("%sError: %s%s", colorError, e.getMessage(), colorReset));
    }

  }
}
