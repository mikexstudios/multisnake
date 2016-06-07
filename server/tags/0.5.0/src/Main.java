/**
 * Main entry point for snake server.
 *
 * Eugene Marinelli
 * 12/15/07
 */

import jargs.gnu.CmdLineParser;
import Snake.MasterPolicyServer;
import Snake.SnakeServer;
import Snake.InfoServer;

public class Main {
  private static void printUsage() {
    System.out.println("Usage: java -jar dist/SnakeServer.jar [--server/-s server hostname]\n\t" +
      "[--max_rows/-r max_rows] [--max_cols/-c max_cols]\n\t[--ai_players/-a ai_players] [--help/-h]\n\t" +
      "[--map/-m map_filename]");
    System.out.println("  Both max_rows and max_cols must be specified to override the default values.");
  }

  public static void main(String[] args) {
    CmdLineParser parser = new CmdLineParser();
    CmdLineParser.Option hostname_op = parser.addStringOption('s', "server");
    CmdLineParser.Option mapfile_op = parser.addStringOption('m', "map");
    CmdLineParser.Option max_rows_op = parser.addIntegerOption('r', "max_rows");
    CmdLineParser.Option max_cols_op = parser.addIntegerOption('c', "max_cols");
    CmdLineParser.Option num_ai_players_op = parser.addIntegerOption('a', "ai_players");
    CmdLineParser.Option help_op = parser.addIntegerOption('h', "help");

    try {
      parser.parse(args);
    } catch (CmdLineParser.OptionException e) {
      printUsage();
      System.exit(1);
    }

    if (parser.getOptionValue(help_op) != null) { // Display help and exit.
      printUsage();
      System.exit(1);
    }

    Integer max_rows = (Integer)parser.getOptionValue(max_rows_op);
    Integer max_cols = (Integer)parser.getOptionValue(max_cols_op);
    Integer num_ai = (Integer)parser.getOptionValue(num_ai_players_op, 0);
    String hostname = (String)parser.getOptionValue(hostname_op);
    String map_filename = (String)parser.getOptionValue(mapfile_op);

    MasterPolicyServer ps = new MasterPolicyServer();
    Thread policy_server = new Thread(ps);
    policy_server.setPriority(Thread.MIN_PRIORITY);
    policy_server.start();

    System.out.println("Using " + num_ai + " bots.");

    SnakeServer ss;
    if (max_rows != null && max_cols != null) {
      System.out.println("Using max_rows=" + max_rows + ", max_cols=" + max_cols);

      if (map_filename != null) {
        System.out.println("Using map file: " + map_filename);
        ss = new SnakeServer(max_rows.intValue(), max_cols.intValue(), num_ai.intValue(), map_filename);
      } else {
        System.out.println("Using default map file.");
        ss = new SnakeServer(max_rows.intValue(), max_cols.intValue(), num_ai.intValue());
      }
    } else {
      System.out.println("Using default max_rows and max_cols.");
      if (map_filename != null) {
        System.out.println("Using map file: " + map_filename);
        ss = new SnakeServer(num_ai.intValue(), map_filename);
      } else {
        System.out.println("Using default map file.");
        ss = new SnakeServer(num_ai.intValue());
      }
    }

    if (hostname == null) {
      System.out.println("Not reporting a hostname.");
    } else {
      System.out.println("Reporting hostname: " + hostname);

      InfoServer is = new InfoServer(ss, hostname);
      Thread info_server = new Thread(is);
      info_server.setPriority(Thread.MIN_PRIORITY);
      info_server.start();
    }

    System.out.println("Running snake server...");
    ss.run(); // Snake server runs in the current thread.
  }
}
