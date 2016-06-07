/**
 * Main entry point for snake server.
 *
 * Eugene Marinelli
 * 12/15/07
 */

package supplelabs.multisnake;

import jargs.gnu.CmdLineParser;

public class Main {
  public static boolean DEBUG_MODE;

  private static void printUsage() {
    System.out.println("Usage: java -jar dist/SnakeServer.jar [--server/-s server hostname]\n\t" +
      "[--max_rows/-r max_rows] [--max_cols/-c max_cols]\n\t[--ai_players/-a ai_players] [--help/-h]\n\t" +
      "[--map/-m map_filename] [--mapdir/-f maps_directory] [--limit_ip/-l] [--disable_notifications/-n]\n\t" +
      "[--intermission_duration/-i (duration in sec)] [--round_duration/-r (duration in sec)]\n\t" +
      "[--debug_mode/-d debug mode]");
    System.out.println("  Both max_rows and max_cols must be specified to override the default values.");
  }

  public static void main(String[] args) {
    CmdLineParser parser = new CmdLineParser();
    CmdLineParser.Option hostname_op = parser.addStringOption('s', "server");
    CmdLineParser.Option mapfile_op = parser.addStringOption('m', "map");
    CmdLineParser.Option mapdir_op = parser.addStringOption('f', "mapdir");
    CmdLineParser.Option max_rows_op = parser.addIntegerOption('r', "max_rows");
    CmdLineParser.Option max_cols_op = parser.addIntegerOption('c', "max_cols");
    CmdLineParser.Option num_ai_players_op = parser.addIntegerOption('a', "ai_players");
    CmdLineParser.Option help_op = parser.addBooleanOption('h', "help");
    CmdLineParser.Option limit_ip_op = parser.addBooleanOption('l', "limit_ip");
    CmdLineParser.Option disable_notif_op = parser.addBooleanOption('n', "disable_notifications");
    CmdLineParser.Option intermission_duration_op = parser.addIntegerOption('i', "intermission_duration");
    CmdLineParser.Option round_duration_op = parser.addIntegerOption('r', "round_duration");
    CmdLineParser.Option debug_mode_op = parser.addBooleanOption('d', "debug_mode");

    try {
      parser.parse(args);
    } catch (CmdLineParser.OptionException e) {
      System.out.println("Invalid input pattern.");
      printUsage();
      System.exit(1);
    }

    if (parser.getOptionValue(help_op) != null) { // Display help and exit.
      printUsage();
      System.exit(1);
    }

    SnakeServerOptions options = new SnakeServerOptions();
    options.max_rows = (Integer)parser.getOptionValue(max_rows_op);
    options.max_cols = (Integer)parser.getOptionValue(max_cols_op);
    options.num_ai = (Integer)parser.getOptionValue(num_ai_players_op, 0);
    options.hostname = (String)parser.getOptionValue(hostname_op);
    options.map_filename = (String)parser.getOptionValue(mapfile_op);
    options.map_directory = (String)parser.getOptionValue(mapdir_op);
    options.limit_connections_per_ip = (Boolean)parser.getOptionValue(limit_ip_op, false);
    options.disable_notifs = (Boolean)parser.getOptionValue(disable_notif_op, false);
    options.intermission_duration = (Integer)parser.getOptionValue(intermission_duration_op,
      SnakeServer.DEFAULT_INTERMISSION_DURATION);
    options.round_duration = (Integer)parser.getOptionValue(round_duration_op,
      SnakeServer.DEFAULT_ROUND_DURATION);
    options.debug_mode = (Boolean)parser.getOptionValue(debug_mode_op, false);

    if ((options.max_rows != null && options.max_rows <= 0) 
      || (options.max_cols != null && options.max_cols <= 0)) {
      System.out.println("Invalid max_rows or max_cols.");
      System.exit(1);
    }
    if (options.num_ai < 0) {
      System.out.println("Invalid number of AI players.");
      System.exit(1);
    }

    MasterPolicyServer ps = new MasterPolicyServer();
    Thread policy_server = new Thread(ps);
    policy_server.setPriority(Thread.MIN_PRIORITY);
    policy_server.start();

    SnakeServer ss = new SnakeServer(options);
    System.out.println("Using " + options.num_ai + " bots.");
    if (options.limit_connections_per_ip) {
      System.out.println("Limiting connections per IP.");
    } else {
      System.out.println("Not limiting connections per IP.");
    }
    if (options.disable_notifs) {
      System.out.println("Notifications disabled.");
    } else {
      System.out.println("Notifications enabled.");
    }
    if (options.max_rows != null && options.max_cols != null) {
      System.out.println("Using max_rows=" + options.max_rows + ", max_cols=" + options.max_cols);
    } else {
      System.out.println("Using default max_rows and max_cols.");
    }
    if (options.map_filename != null) {
      System.out.println("Using map file: " + options.map_filename);
    } else {
      System.out.println("Using default map file.");
    }
    if (options.hostname == null) {
      System.out.println("Not reporting a hostname.");
    } else {
      System.out.println("Reporting hostname: " + options.hostname);

      InfoServer is = new InfoServer(ss, options.hostname);
      Thread info_server = new Thread(is);
      info_server.setPriority(Thread.MIN_PRIORITY);
      info_server.start();
    }

    System.out.println("Running snake server...");
    ss.run(); // Snake server runs in the current thread.
  }
}
