import java.io.*;
import java.lang.*;
import java.net.Socket;
import java.util.Arrays;

public class CSdict {
    static Boolean debugOn = false;

    private static Socket socket = null;
    private static PrintWriter serverInput;
    private static BufferedReader serverResponse;

    static final String FROM_CLIENT_PREFIX = "> ";
    static final String FROM_SERVER_PREFIX = "<-- ";

    private static String currDict;

    public static void main(String [] args) {
        // Verify command line arguments, only [-d] (1 argument) is allowed
        if (args.length == 0) {
            System.out.println("Debugging output disabled.");
        }
        else if (args.length == 1) {
            debugOn = args[0].equals("-d") || args[0].equals("[-d]");
            if (debugOn) {
                System.out.println("Debugging output enabled.");
            } else {
                System.err.println("997 Invalid command line option - Only -d is allowed.");
                return;
            }
        } 
        else if (args.length > 1) {
            System.err.println("996 Too many command line options - Only -d is allowed.");
            return;
        }
        else {
            System.err.println("999 Processing error. Please recheck configurations.");
            return;
        }
        handleCommands();
    }

    /*  Handle each command that is issued by client. */
    public static void handleCommands() {
        while (true) {
            byte cmdString[] = new byte[255];
            String command = "";
            String[] arguments = null;
            System.out.print("csdict> ");
            
            try {
                //Get the input command
                System.in.read(cmdString);
                String inputString = new String(cmdString, "ASCII");
                String[] inputs = inputString.trim().split("( |\t)+");
                command = inputs[0].toLowerCase().trim();
                //Get the input argument(s)
                arguments = Arrays.copyOfRange(inputs, 1, inputs.length);
            } 
            catch (Exception e) {
                System.err.println("998 Input error while reading commands, terminating.");
                System.exit(-1);
            }

            // Ignore empty line or line that starts with #
            if (!(command.isEmpty()) && !(command.substring(0,1).equals("#"))) {
                switch (command) {
                    case "open":
                        if (checkSocket("open") && correctArgs("open", arguments)) {
                            try {
                                openConnection(arguments[0], Integer.parseInt(arguments[1]));
                            } 
                            catch (ArrayIndexOutOfBoundsException e) {
                                System.err.println("901 Incorrect number of arguments.");
                            } 
                            catch (NumberFormatException e) {
                                System.err.println("902 Invalid argument.");
                            }
                        }
                        break;

                    case "dict":
                        if (checkSocket("dict") && correctArgs("dict", arguments)) 
                                dictDatabases();
                        break;

                    case "define":
                        if (checkSocket("define") && correctArgs("define", arguments))
                            defineWord(arguments[0]);
                        break;

                    case "set":
                        if (checkSocket("set") && correctArgs("set", arguments))
                            setDictionary(arguments[0]);
                        break;

                    case "match":
                        if (checkSocket("match") && correctArgs("match", arguments)) 
                            matchWord("match", arguments[0]);
                            break;

                    case "prefixmatch":
                        if (checkSocket("prefixmatch") && correctArgs("prefixmatch", arguments)) 
                            matchWord("prefixmatch", arguments[0]);
                        break;

                    case "close":
                        if (checkSocket("close") && correctArgs("close", arguments)) 
                            closeConnection();
                        break;

                    case "quit":
                        if (correctArgs("quit", arguments))
                            quitSystem();
                        break;
                        
                    default:
                        System.err.println("900 Invalid command");
                        break;
                }
            }
        }
    }

    /*  Check if a particular command can be executed based on the socket connection.
        For example, cannot execute "open" if socket is already established. */
    public static Boolean checkSocket(String cmd) {
        switch(cmd) {
            case "open":
                if (socket != null) {
                    System.err.println("903 Supplied command not expected at this time.");
                    return false;
                }
                return true;
            
            case "dict":
                if (socket == null) {
                    System.err.println("903 Supplied command not expected at this time.");
                    return false;
                }
                return true;

            case "define":
                if (socket == null) {
                    System.err.println("903 Supplied command not expected at this time.");
                    return false;
                }
                return true;

            case "set":
                if (socket == null) {
                    System.err.println("903 Supplied command not expected at this time.");
                    return false;
                }
                return true;

            case "match":
                if (socket == null) {
                    System.err.println("903 Supplied command not expected at this time.");
                    return false;
                }
                return true;

            case "prefixmatch":
                if (socket == null) {
                    System.err.println("903 Supplied command not expected at this time.");
                    return false;
                }
                return true;

            case "close":
                if (socket == null) {
                    System.err.println("903 Supplied command not expected at this time.");
                    return false;
                }
                return true;

            default:
                return false;
        }
    }


    /*  Check if a particular command has correct numbers of arguments. */
        public static Boolean correctArgs(String cmd, String[] arguments) {
            switch(cmd) {
                case "open":
                    if (arguments.length != 2) {
                        System.err.println("901 Incorrect number of arguments.");
                        return false;
                    }
                    return true;
                
                case "dict":
                    if (arguments.length != 0) {
                        System.err.println("901 Incorrect number of arguments.");
                        return false;
                    }
                    return true;
    
                case "define":
                    if (arguments.length != 1) {
                        System.err.println("901 Incorrect number of arguments.");
                        return false;
                    }
                    return true;
    
                case "set":
                    if (arguments.length != 1) {
                        System.err.println("901 Incorrect number of arguments.");
                        return false;
                    }
                    return true;
    
                case "match":
                    if (arguments.length != 1) {
                        System.err.println("901 Incorrect number of arguments.");
                        return false;
                    }
                    return true;
    
                case "prefixmatch":
                    if (arguments.length != 1) {
                        System.err.println("901 Incorrect number of arguments.");
                        return false;
                    }
                    return true;
    
                case "close":
                    if (arguments.length != 0) {
                        System.err.println("901 Incorrect number of arguments.");
                        return false;
                    }
                    return true;

                case "quit":
                    if (arguments.length != 0) {
                        System.err.println("901 Incorrect number of arguments.");
                        return false;
                    }
                    return true;
    
                default:
                    return false;
            }
        }



    /*  Handle open args1 args2 command. */
    public static void openConnection(String args1, int args2) {
        currDict = "*";
        try {
            // Try to establish connection with socket.
            socket = new Socket(args1, args2);
            serverInput = new PrintWriter(socket.getOutputStream(), true);
            serverResponse = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Handle if -d is specified.
            if ((socket != null) && socket.isConnected() && debugOn) {
                try {
                    System.out.println(FROM_SERVER_PREFIX + serverResponse.readLine());
                }
                catch (IOException e) {
                    System.err.println("925 Control connection I/O error, closing control connection.");
                    socket = null;
                }
            }
            else if ((socket != null) && socket.isConnected() && !debugOn) {
                try {
                    serverResponse.readLine();
                }
                catch (IOException e) {
                    System.err.println("925 Control connection I/O error, closing control connection.");
                    socket = null;
                }
            }
        }
        // If connection cannot be (takes too long) established, error 920 is thrown.
        // Or if args for open is valid, but wrong (try open abc 1000).
        catch (Exception e) {
            System.err.println("920 Control connection to " + args1 + " on port " + args2 + " failed to open.");
        }
    }
    /*  Handle dict command. Should list all the databases. */
    public static void dictDatabases() {
        try {
            serverInput.println("SHOW DB");
            // Handle if -d is specified.
            if (debugOn) {
                System.out.println(FROM_CLIENT_PREFIX + "SHOW DB");
                // This should print how many databases found.
                System.out.println(FROM_SERVER_PREFIX + serverResponse.readLine() + ": list follows");
            }
            // Print all the databases.
            String resp = serverResponse.readLine();
            while (!resp.contains("250")) {
                // When debug mode is off, the response code should not be printed
                if (resp.contains("110") && !debugOn)
                    System.out.println(resp.replace("110 ", "") + ": list follow");
                else
                    System.out.println(resp);
                resp = serverResponse.readLine();
            }
            // Handle response 250 when command is completed and -d is specified.
            if (resp.contains("250") && debugOn) {
                System.out.println(FROM_SERVER_PREFIX + "250 Command complete");
            }
        }
        catch (IOException e) {
            System.err.println("925 Control connection I/O error, closing control connection.");
            socket = null;
        }
    }

    /*  Handle define args1 command. */
    public static void defineWord(String args1) {
        try {
            serverInput.println("DEFINE " + currDict + " " + args1);
            String resp = serverResponse.readLine();

            // Handle if -d is specified.
            if (debugOn) {
                if (resp.contains("150")) {
                    System.out.println(FROM_CLIENT_PREFIX + "DEFINE " + currDict + " " + args1);
                    System.out.println(FROM_SERVER_PREFIX + resp.replace("retrieved", "found: list follows"));
                }
                else if (resp.contains("552")) {
                    System.out.println(FROM_CLIENT_PREFIX + "DEFINE " + currDict + " " + args1);
                    System.out.println(FROM_SERVER_PREFIX + resp);
                }
            }

            // Print the rest of the lines.
            while (!resp.contains("250")) {
                // If no definition found, this should be printed instead and call match().
                if (resp.contains("552")) {
                    System.out.println("***No definition found***");
                    matchWord("match_default", args1);
                    break;
                }
                // Some definitions are found.
                resp = serverResponse.readLine();
                if (resp.contains("151") && debugOn) 
                    System.out.println(FROM_SERVER_PREFIX + resp + " : text follows");
                // Handle the syntax of dictionary (e.g @ foldoc blahblahblah)
                System.out.println(resp.replaceAll("(?i)151 " + "\"" + args1 + "\"", "@").trim());
            }
            // Handle response 250 when command is completed and -d is specified.
            if (resp.contains("250") && debugOn)
                System.out.println(FROM_SERVER_PREFIX + "250 Command complete");
        } 
        catch (IOException e) {
            System.err.println("925 Control connection I/O error, closing control connection.");
            socket = null;
        }
    }

    /*  Handle set args1 command. */
    public static void setDictionary(String dict) {
        currDict = dict;
    }

    /*  Handle match and prefixmatch args1 command. */
    public static void matchWord(String cmd, String args1) {
        try {
            String resp = "";
            switch (cmd) {
                // Handle command for exact match.
                case "match":
                    serverInput.println("MATCH " + currDict + " exact " + args1);
                    resp = serverResponse.readLine();
                    // Handle if -d is specified.
                    if (debugOn) {
                        System.out.println(FROM_CLIENT_PREFIX + "MATCH " + currDict + " exact " + args1);
                        System.out.println(FROM_SERVER_PREFIX + resp);
                    }
                    break;

                // Handle command for prefix match.
                case "prefixmatch":
                    serverInput.println("MATCH " + currDict + " prefix " + args1);
                    resp = serverResponse.readLine();
                    // Handle if -d is specified.
                    if (debugOn) {
                        System.out.println(FROM_CLIENT_PREFIX + "MATCH " + currDict + " prefix " + args1);
                        System.out.println(FROM_SERVER_PREFIX + resp);
                    }
                    break;
                // Handle command for server default match (for defineWord() function).
                case "match_default":
                    serverInput.println("MATCH " + currDict + " . " + args1);
                    resp = serverResponse.readLine();
                    // Handle if -d is specified.
                    if (debugOn) {
                        System.out.println(FROM_CLIENT_PREFIX + "MATCH " + currDict + " . " + args1);
                        System.out.println(FROM_SERVER_PREFIX + resp);
                    }
                    break;
            }

            // Print the rest of the lines.
            while (!resp.contains("250")) {
                // No match is found case.
                if (resp.contains("552")) {
                    if (cmd.equals("match_default")) {
                        System.out.println("*****No matches found*****");
                        break;
                    }
                    else if (cmd.equals("match")) {
                        System.out.println("*****No matching word(s) found*****");
                        break;
                    }
                    else {
                        System.out.println("***No matching word(s) found****");
                        break;
                    }
                }
                // Matches are found.
                else if (resp.contains("152")) {
                    if (debugOn) {
                        //do nothing, need to skip line or else it is a clone of previous line.
                    }
                    // Debug mode is off, response code should be removed.
                    else
                        System.out.println(resp.replaceAll("152 ", "") + ": list follow");
                }
                else
                    System.out.println(resp);
                resp = serverResponse.readLine();
            }
            // Handle response 250 when command is completed and -d is specified.
            if (resp.contains("250") && debugOn) {
                System.out.println(FROM_SERVER_PREFIX + "250 Command complete");
            }
        } 
        catch (IOException e) {
            System.err.println("925 Control connection I/O error, closing control connection.");
            socket = null;
        }
    }

    /*  Handle close command. */
    public static void closeConnection() {
        try {
            serverInput.println("QUIT");
            // Handle if -d is specified.
            if (serverResponse.readLine().contains("221") && debugOn) {
                System.out.println(FROM_CLIENT_PREFIX + "QUIT");
                System.out.println(FROM_SERVER_PREFIX + "221 Closing connection");
            }
            socket.close();
            socket = null;
            serverInput = null;
            serverResponse = null;
        } 
        catch (IOException e) {
            System.err.println("925 Control connection I/O error, closing control connection.");
            socket = null;
        }
    }

    /*  Handle quit command. Quit can be issued anytime. */
    public static void quitSystem() {
        // If socket is established, need to close connection first then exit system.
        if (socket != null) {
            closeConnection();
            System.exit(1);
        }
        // Otherwise just exit system.
        else
            System.exit(1);
    }
}