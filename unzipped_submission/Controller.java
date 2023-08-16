import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Controller {
    // Track the number of removals performed
    private static int removalsAccomplished = 0;
    // Stores PrintWriter objects for saving to a file
    private static final Map<String, PrintWriter> fileSaver = new HashMap<>();
    // Stores the index of files and their states
    static final Index counter = new Index(new ArrayList<>(), new HashMap<>());
    // Stores the timeout value
    static int timeout;
    // Monitor current files and their associated sockets
    static final Map<String, ArrayList<Integer>> currentFiles = new HashMap<>();
    // Track the number of storings performed
    private static int savesAccomplished = 0;
    // Stores the replication factor value
    private static int replicationFactor = 0;
    // Stores current Dstore sockets
    static final ArrayList<Integer> currentDstores = new ArrayList<>();
    // Lock object for synchronization
    private static final Lock lock = new ReentrantLock();
    // List of substitutes
    private static final ArrayList<Integer> placeholderList = new ArrayList<>();
    // Stores the time to rebalance the value
    static int timeForRebalancing;
    // Stores PrintWriter objects for removal from the file
    private static final Map<String, PrintWriter> fileRemovers = new HashMap<>();
    // Saves the port number for the controller
    static int portController;
    // Stores the number of ports
    private static int numberOfPort = 0;

    // Constructor for the Controller class
    public Controller(int portController, int replicationFactor, int timeout, int timeForRebalancing) {
    // Initiating the re-balance time variable
        Controller.timeForRebalancing = timeForRebalancing;
    // Initiating the replication factor variable
        Controller.replicationFactor = replicationFactor;
    // Initializing the portController variable
        Controller.portController = portController;
    // Initiating the timeout variable
        Controller.timeout = timeout;}

    // Getter and Setter methods

    //Accessor for the replication factor
    public static int getReplicationFactor() {
        return replicationFactor;}
    //Mutator for the replication factor
    public static void setReplicationFactor(int replicationFactor) {
        Controller.replicationFactor = replicationFactor;}
    //Accessor for the successfully saved commands
    public static int getSavesAccomplished() {
        return savesAccomplished;}
    //Mutator for the successfully saved commands
    public static void setSavesAccomplished(int savesAccomplished) {
        Controller.savesAccomplished = savesAccomplished;}
    //Accessor for the number of a particular port
    public static int getNumberOfPort() {
        return numberOfPort;}
    //Mutator for the number of a particular port
    public static void setNumberOfPort(int numberOfPort) {
        Controller.numberOfPort = numberOfPort;}
    //Accessor for the successfully removed commands
    public static int getRemovalsAccomplished() {
        return removalsAccomplished;}
    //Mutator for the successfully removed commands
    public static void setRemovalsAccomplished(int removalsAccomplished) {
        Controller.removalsAccomplished = removalsAccomplished;}

    // This method is used to send a signal to all Dstores to list the files they have.
    private synchronized static void signalList(PrintWriter contentsToWrite){
    // Adds a new variable to carry the filename
        StringBuilder nameOfFile = new StringBuilder();
    // If there are filenames in the list, they are added to the variable
        if(0 < counter.getNameOfFile().size()) {
            for(String doc : counter.getNameOfFile()){
                nameOfFile.append(" ").append(doc);}}
    // Print a message indicating that an attempt is being made to log the operation
        System.out.println("Trying to list " + nameOfFile);
    // Send an alert to all D-repositories to list their files
        contentsToWrite.println("LIST" + nameOfFile);
    // Print a message indicating that the enumeration operation is complete
        System.out.println("Listing accomplished.");}

    // This method is used to check whether the number of D repositories is sufficient for replication.
    synchronized static boolean sufficientNumDstores(PrintWriter printWriter){
    // If the replication factor is greater than the current number of Dstores, an error message is displayed and a value of false is returned
        if(!(replicationFactor <= currentDstores.size())){
            printWriter.println("ERROR_NOT_ENOUGH_DSTORES");
            return false;}
    // Otherwise returns true
        return true;}

    // this method is used to send an alert to a specified Dstore to remove a file
    private synchronized static void signalRemove(Integer socket, String nameOfFile){
        try {
    // Create a socket to connect to the specified Dstore
            Socket socketForDstore = new Socket(InetAddress.getLoopbackAddress(), socket);
    // Create a recorder to send the signal to the Dstore
            PrintWriter writerForDstore = new PrintWriter(socketForDstore.getOutputStream(), true);
            writerForDstore.println("REMOVE " + nameOfFile);
        } catch (IOException ioException) {
            ioException.printStackTrace();}}

    // This method is used to send a signal to load a file from a specific Dstore
    synchronized static void signalLoad(PrintWriter contentsAdder, String nameOfFile, int numLoadedSockets){
    // Iterating over the set of keys in the `currentFiles` map
        for(String identifier : currentFiles.keySet()){
    // Check that the identifier matches the name of the file being loaded and that `counter` contains the identifier
            if(identifier.equals(nameOfFile) && counter.nameOfFile.contains(identifier)){
    // Get the socket number and file size from the `currentFiles` map
                int socket = currentFiles.get(nameOfFile).get(numLoadedSockets);
                int sizeOfFile = currentFiles.get(nameOfFile).get(0);
    // Send a message to load the file from the specified socket
                contentsAdder.println("LOAD_FROM " + socket + " " + sizeOfFile);
    // Print a message in the console indicating that a file is being loaded from a socket
                System.out.println("Trying to load file from socket: " + socket + " with filesize of: " + sizeOfFile);}}}

    // Add a Dstore to the list of current Dstore
    synchronized static void dStoreSaving(String socket) {
    // Converting a socket string to an integer
        int dstoreSocket = Integer.parseInt(socket);
    // Check if the current Dstores list already contains the socket
        if (!currentDstores.contains(dstoreSocket)) {
    // If not, add the socket to the list and display a message on the console
            System.out.println("Trying to add a Dstore.");
            currentDstores.add(dstoreSocket);
            System.out.println("Successful addition of a Dstore.");}}

    // Handle the successful storing of a file
    private synchronized static void accomplishedStoring(int socket, PrintWriter contentsAdder, int successfulSaves, String nameOfFile) {
    // Check if all replicas of the file have been stored successfully
        if(getReplicationFactor() == successfulSaves) {
            System.out.println("Trying to add a file named: " + nameOfFile + " to the index file.");
    // Add the file to the counter map
            counter.nameOfFile.add(nameOfFile);
    // Reset the number of entries made
            setSavesAccomplished(0);
    // Add the socket to the socket list of the file if it is not already present
            if(!currentFiles.get(nameOfFile).contains(socket)) {
                currentFiles.get(nameOfFile).add(socket);
                currentFiles.put(nameOfFile, currentFiles.get(nameOfFile));}
    // Remove file from current index counter
            counter.stateOfFile.remove(nameOfFile);
    // Index updated to "store complete"
            counter.stateOfFile.put(nameOfFile, 1000);
    // Extract Println for successful index update
            System.out.println(" \"Store complete\" is the updated index");
    // writer prints STORE_COMPLETE signal
            contentsAdder.println("STORE_COMPLETE");
    // print statement to confirm successful file saving
            System.out.println("Successful saving of file");
        } else {
    // Add the socket to the socket list of the file if it is not already present
            setSavesAccomplished(getSavesAccomplished()+1);
    // Add the socket to the socket list of the file if it is not already present
            if(!currentFiles.get(nameOfFile).contains(socket)) {
                currentFiles.get(nameOfFile).add(socket);
                currentFiles.put(nameOfFile, currentFiles.get(nameOfFile));}}}

    // a method for dealing with a problem occurring on a particular Dstore socket
    private synchronized static void probDStoreHandle(Integer socket) {
    // If socket is 0, return (no action required)
        if(0 == socket) return;
    // Remove the socket from the list of active Dstores
        currentDstores.remove(socket);
    // going through the current file list and searching for a specific String identifier
        for(String identifier : currentFiles.keySet()){
    // if the current file list already owns the port, remove the port from this array list
            if(currentFiles.get(identifier).contains(socket)){
    // Remove the socket from the list of sockets in the file
                currentFiles.get(identifier).remove(socket);
    // If only one copy of the file remains, remove the file record
                if(1 == currentFiles.get(identifier).size()){
                    currentFiles.remove(identifier);
                    counter.stateOfFile.remove(identifier);
                    counter.nameOfFile.remove(identifier);
                    }}}}


    // This method is used to send an alert to store a file in the system
    private synchronized static void signalStore(PrintWriter contentsAdder, int replicationFactor, String nameOfFile, int sizeOfFile) {
    // Check if the file is no longer stored on the system
        if (!counter.stateOfFile.containsKey(nameOfFile)) {
    // To do this, add its size to the list that serves as a placeholder.
            placeholderList.add(sizeOfFile);
    // adding the list, which acts as a placeholder with its name, to the list of current files
            currentFiles.put(nameOfFile, placeholderList);
    // index updated to "store in progress"
            counter.stateOfFile.put(nameOfFile, 1001);
    // println declaration to successfully save the file and update the index file
            System.out.println(" \"Store in progress\" is the updated index.");
    // represents all ports as StringBuilder type
            // StringBuilder object for sockets
            StringBuilder sockets = new StringBuilder();
    // Check if the replicationFactor is the expected value
            if (replicationFactor != 3) {
    // Statement println on problem with insufficient number of Dstores
                System.out.println("Invalid replicationFactor. Expected: 3, Actual: " + replicationFactor);
    // return of the exception to an insufficient number of Dstores
                contentsAdder.println("ERROR_NOT_ENOUGH_DSTORES");
    // Exit the method
                return;
            }
    // Adding Dstore sockets to StringBuilder
            for (int j = 0; j < replicationFactor; j++) {
                sockets.append(" ").append(currentDstores.get(j));}
    // println statement for sent file store message
            System.out.println("Sending a message for storing the file" + nameOfFile + " in the Controller.");
    // a message to the writer specifying the explicit socket on which the file will be stored
            contentsAdder.println("STORE_TO" + sockets);
    // println statement that informs about the successful sending of the message
            System.out.println("Message for storing file" + nameOfFile + " has been successfully sent.");
        } else {
    // an else statement with a println statement indicating that the file is now in the system
            System.out.println("The file you try to add is already present in the system.");
    // a message to the writer to indicate the availability of the file
            contentsAdder.println("ERROR_FILE_ALREADY_EXISTS");}}

    // This method is used to send an alert to remove a file from the system.
    private synchronized static void signalRemove(PrintWriter contentsAdder, String nameOfFile){
    // Check if the file is in the system
        if(counter.nameOfFile.contains(nameOfFile)) {
    // if so, remove the file from the list of current files
            counter.stateOfFile.remove(nameOfFile);
    // removal in progress
            counter.stateOfFile.put(nameOfFile, 2001);
    // println to update the index after removing the file
            System.out.println("\"Remove in progress\" is the updated index.");
    // checks whether the file the system is working with is present in the list of current files
            if (currentFiles.containsKey(nameOfFile)) {
    // if so, add the Dstore of the file the system is working with to the list of available files
                // Keeps the Dstore sockets
                ArrayList<Integer> dStores = currentFiles.get(nameOfFile);
    // iteration through the file list, searching for the specific file to remove its signal
                for (Integer dStore : dStores) {
                    signalRemove(dStore, nameOfFile);
                }
            }
        } else {
    // else statement for a non-existent file we are trying to access
            System.out.println("File " + nameOfFile + " which you are trying to remove doesn't exist.");
    // a writer who adds an error for a non-existent file
            contentsAdder.println("ERROR_FILE_DOES_NOT_EXIST");
            }}

    // This method is called when file removal is complete
    private synchronized static void accomplishedRemove(int socket, PrintWriter contentsAdder, int removalsAccomplished, String nameOfFile) {
    // Check if all replicas of the file have been removed
        if (currentFiles.get(nameOfFile).size() - 1 == removalsAccomplished) {
    // remove the file along with its name from the current index file
            counter.nameOfFile.remove(nameOfFile);
    // indicate that the removal has not yet been successfully established
            setRemovalsAccomplished(0);
    // get rid of the file in the current file list
            currentFiles.remove(nameOfFile);
    // removing the file in the counter object
            counter.stateOfFile.remove(nameOfFile);
    // index has been updated to "remove complete"
            counter.stateOfFile.put(nameOfFile, 2000);
    // a printed declaration of successful removal together with the updated index file
            System.out.println("\"Remove complete\" is the updated index.");
    // print a successful file removal statement along with the file name
            System.out.println("Successful removal of file " + nameOfFile);
    // writer who adds the performance of removing
            contentsAdder.println("REMOVE_COMPLETE");
        } else {
    // use of a mutator and accessory to increase the number of removals performed in the system
            setRemovalsAccomplished(getRemovalsAccomplished() + 1);
    // transfer the file from the list of current files along with its name and port to the list of removed files
            currentFiles.get(nameOfFile).remove(socket);}}

    // This method is for making changes to the port and name of a particular file
    private synchronized static void changeFile(Integer socket, ArrayList<String> nameOfFile) {
    // Checks if the size of currentFiles is other than zero
        if (currentFiles.size() != 0) {
    // if so, scroll through each of the characters in the filename
            for (String string : nameOfFile) {
    // Checks if currentFiles contains the file with the given string
                if(currentFiles.containsKey(string)) {
    // Adds the socket to the list of sockets in the file
                    currentFiles.get(string).add(socket);}
    // Prints the updated HashMap with the file name
        System.out.println("The updated HashMap contains file " + nameOfFile);}}}

    // Basic method for initializing the four parameters and creating a new Controller object
    public static void main(String[] args) {
    // Develops the command line arguments and initializes the Controller object
    // initializing the rebalanced time argument
        timeForRebalancing = Integer.parseInt(args[3]);
    // initializing the repetition factor argument
        final int replicationFactor = Integer.parseInt(args[1]);
    // initializing the timeout argument
        timeout = Integer.parseInt(args[2]);
    // initializing the control port argument
        portController = Integer.parseInt(args[0]);
    // create a new controller object that accepts all parameters initialized so far
        Controller control = new Controller(portController,replicationFactor,timeout,timeForRebalancing);
    // start the execution of this object using the start command
        control.start();}

    // method for combining all methods created so far and implementing the starting point of the Cotntroller class
    public void start(){
    // Clear currentDstores and counter maps
        currentDstores.clear();
    // clears the current counter and its value
        counter.clear();
    // Listening on the specified port number for incoming connections
        try {
    // displays the port number being listened to
            System.out.println("Port number: " + portController + " is being listened by the Controller");
    // creates a new ServerSocket object that captures the port
            ServerSocket portServer = new ServerSocket(portController);
    // goes through all possible variants of the cycle
            for (;;) {
    // Accepts incoming connections
                try {
    // prints the connection attempt
                    System.out.println("Trying to establish a connection.");
    // creates a new socket called user that accepts the successful port number
                    Socket user = portServer.accept();
    // printing the successful connection
                    System.out.println("Connection established successfully.");
    // adding a new thread to execute the protocol processes
                    new Thread(() -> {
    // adding a local variable to identify the port number used
                        int numberOfPort = 0;
                        try {
    // adding a local variable for the already scanned part of the characters
                            String scannedChar;
    // adding a local variable reader that scans the input character stream
                            BufferedReader reader = new BufferedReader(new InputStreamReader(user.getInputStream()));
    // adding a local boolean variable to determine if the object is a Dstore or not
                            boolean booleanDstore = false;
    // adding a local record variable to transfer the output steam to text output
                            PrintWriter writer = new PrintWriter(user.getOutputStream(), true);
    // looping only through the scanned input string that is other than null, or simply looping until information is available
                            while (null != (scannedChar = reader.readLine())) {
    // Acquire a lock to ensure exclusive access to critical sections
                                lock.lock();
                                try {
    // adding a local String list variable to split the scanned string when necessary and appropriate
                                    String[] actionStep = scannedChar.split(" ");
    // adding a local String variable that takes the first possible element from the list and scans it
                                    String action = actionStep[0];
    // pprintln statement to return the command that is red
                                    System.out.println("The command which is being read is: " + action);
    // using switch and cases to view all possible values that the command can take according to the protocol class, and explicitly trying to match them one by one
                                    switch (action) {
    // if the scanned variable matches the JOIN action
                                        case "JOIN":
    // setting the boolean variable for Dstore to true
                                            booleanDstore = true;
    // retrieve the port number by parsing its value in a locally defined variable
                                            numberOfPort = Integer.parseInt(actionStep[1]);
    // retrieve the storage method and add the action to be performed by it
                                            dStoreSaving(actionStep[1]);
    // moving on to the next case
                                            break;
    // if the scanned variable matches the RELOAD action
                                        case "RELOAD":
    // verify that the port number corresponds to the alleged action to be taken, reducing the amount by 1
                                            if (currentFiles.get(actionStep[1]).size() - 1 == getNumberOfPort()) {
    // if so, the recorder adds an ERROR LOAD message because it could not be successfully reloaded.
                                                writer.println("ERROR_LOAD");
    // checks whether the writer has the required amount of Dstores
                                            } else if (sufficientNumDstores(writer)) {
    // checks if the next possible identification action is written in the counter file name
                                                if (counter.nameOfFile.contains(actionStep[1])) {
    // if so, it uses the mutator and accessor functions to be able to increment the port number used.
                                                    setNumberOfPort(getNumberOfPort() + 1);
    // the method also informs with the writer object about its action and the number of ports in which the action was performed
                                                    signalLoad(writer, actionStep[1], getNumberOfPort());
                                                } else {
    // otherwise the recorder encounters a problem related to a non-existent file and informs the user via the system
                                                    writer.println("ERROR_FILE_DOES_NOT_EXIST");
                                                }}
    // moving on to the next case
                                            break;
    // if the scanned variable corresponds to the LOAD action
                                        case "LOAD":
    // checks whether the writer has the required amount of Dstores
                                            if (sufficientNumDstores(writer)) {
    // checks if the next possible identification action is written in the counter file name
                                                if (counter.nameOfFile.contains(actionStep[1])) {
    // if so, the system sets this port number to match the command set as 1
                                                    setNumberOfPort(1);
    // the method also informs with the writer object about its action and the number of ports in which the action was performed
                                                    signalLoad(writer, actionStep[1], getNumberOfPort());
                                                } else {
    // otherwise the recorder encounters a problem related to a non-existent file and informs the user via the system
                                                    writer.println("ERROR_FILE_DOES_NOT_EXIST");
                                                }}
    // moving on to the next case
                                            break;

    // if the scanned variable corresponds to the STORE action
                                        case "STORE":
    // checks whether the writer has the required amount of Dstores
                                            if (sufficientNumDstores(writer)) {
    // checks if the next possible identification action is written in the counter file name
                                                if (!counter.stateOfFile.containsKey(actionStep[1])) {
    // writes the file by comparing the string with the content the writer currently contains
                                                    fileSaver.put(actionStep[1], writer);
                                                }
    // informs the system about the addition of the writer, together with its contents, the replication factor, the returned values of the first step and the modified from String to int contents of the second step
                                                signalStore(writer, replicationFactor, actionStep[1], Integer.parseInt(actionStep[2]));
                                            }
    // moving on to the next case
                                            break;

    // if the scanned variable matches the REMOVE ACKNOWLEDGEMENT action
                                        case "REMOVE_ACK":
    // checks whether the writer has the required amount of Dstors
                                            if (sufficientNumDstores(writer)) {
    // informs about the successful removal of the file and identifies its port, the potential initial action it responds to, also increments the value of the counter of successful removals and the final content of the string combining all these
                                                accomplishedRemove(numberOfPort, fileRemovers.get(actionStep[1]), getRemovalsAccomplished() + 1, actionStep[1]);
                                            }
    // moving on to the next case
                                            break;

    // if the scanned variable matches the STORE ACKNOWLEDGEMENT action
                                        case "STORE_ACK":
    // verifies that the current step content matches port 1001
                                            if (1001 == counter.stateOfFile.get(actionStep[1])) {
    // if so, the port indicated by its port number successfully detects a write, writes the contents of the file, increments the write accessor by 1, and takes the next step
                                                accomplishedStoring(numberOfPort, fileSaver.get(actionStep[1]), getSavesAccomplished() + 1, actionStep[1]);
                                            }
    // moving on to the next case
                                            break;

    // if the scanned variable matches the STORE ACKNOWLEDGEMENT action
                                        case "REMOVE":
    // checks whether the writer has the required amount of Dstores
                                            if (sufficientNumDstores(writer)) {
    // checks if the next possible identification action is written in the counter file name
                                                if (counter.nameOfFile.contains(actionStep[1])) {
    // removes the file by matching the string against the content the writer currently contains
                                                    fileRemovers.put(actionStep[1], writer);
                                                }
    // informs the system about the removal of the writer, along with its contents, the replication factor, the returned values from the first step and the contents modified from String to int from the second step
                                                signalRemove(writer, actionStep[1]);
                                            }
    // moving on to the next case
                                            break;

    // if the scanned variable matches the LIST action
                                        case "LIST":
    // checks if the boolean variable that defines the authenticity of dstore is still at the same stage as it was when it was declared
                                            if (!booleanDstore) {
    // if so, it is checked whether the writer has the required amount of Dstors
                                                if (sufficientNumDstores(writer)) {
    // printing the LIST command execution process from the client
                                                    System.out.println("LIST executed by Client");
    // writers add the LIST command execution to the system
                                                    signalList(writer);}
                                            } else {
    // otherwise it is not the client that executes the LIST command, but Dstore.
                                                System.out.println("LIST executed by Dstore");
    // adds a local list that contains the names of the files used
                                                // iterates each of the possible symbols so that they all match the size of the action
                                                ArrayList<String> nameOfFile = new ArrayList<>(Arrays.asList(actionStep).subList(1, actionStep.length));
    // it uses the method to change the port number and file name
                                                changeFile(numberOfPort, nameOfFile);
                                            }
    // moving on to the next case
                                            break;
    // Specify the default case that occurs if none of the previous commands meet the requirements.
                                        default:
    // returns that the scanned message is not valid and cannot be recognized.
                                            System.out.println(scannedChar + " is a non-valid message.");
    // moving on to the next case
                                            break;}
    // when all possible scenarios have been checked, you can proceed to the last step.
                                } finally {
    // Release lock after critical section is complete
                                    lock.unlock();
                                }}
    // The connection to the customer is suspended
                            user.close();
    // catch possible exception socketException
                        } catch (SocketException socketException) {
    // execute the probDStoreHandle method, which will identify the port number causing the exception
                            probDStoreHandle(numberOfPort);
    // print the port number that identifies the exception
                            System.out.println(socketException + " is the encountered Socket exception.");
    // catching a possible IOException
                        } catch (IOException ioException) {
    // print the nature and reason for the exception
                            ioException.printStackTrace();
    // command to start the method
                        }}).start();
    // Capture a Possible Exception
                } catch (Exception exception) {
    // printing of the exception, its nature and information about it
                    System.out.println(exception + " is the encountered exception.");
    // Capture a Possible Exception
                }}} catch (Exception exception) {
    // printing of the exception, its nature and information about it
            System.out.println(exception + " is the encountered exception.");
        }}}
