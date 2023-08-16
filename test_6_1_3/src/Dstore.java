import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public class Dstore {
    // Variable to store the name of the transferred document
    static String transferedDoc;
    // Variable to store the size of the transferred file
    static int sizeOfFile;
    // Variable to store the scanned character
    static String scannedChar;
    // File object for document submission
    static File doc;
    // Socket object for communication
    static Socket socket;
    // Port number
    static int port;
    // Document storage directory
    static String directoryForDoc;
    // ServerSocket object for accepting connections
    static ServerSocket ss;
    // Controller port number
    static int portController;
    // Timeout value for socket connections
    static int timeout;
    // File name
    static String nameOfFile;

    // Constructor for the Dstore class that initializes all parameters
    public Dstore(int port, int portController, int timeout, String directoryForDoc) {
    // initializing the variable representing the gate
        Dstore.port = port;
    // initialization of the variable representing the port controller
        Dstore.portController = portController;
    // initialization of the variable representing the waiting period
        Dstore.timeout = timeout;
    // initialize the variable representing the file folder
        Dstore.directoryForDoc = directoryForDoc;
    }

    // main method in the Dstore class that evaluates the parameters from the constructor and creates a new instance of the Dstore class
    public static void main(String[] args)  {
    // evaluation of the file folder variable
        String directoryForDoc = args[3];
    // estimation of the port controller variable
        portController = Integer.parseInt(args[1]);
    // evaluation of the timeout variable
        timeout = Integer.parseInt(args[2]);
    // estimation of the port variable
        port = Integer.parseInt(args[0]);
    // create a new instance of the Dstore class including the values of the components evaluated above
        Dstore dstore = new Dstore(port,portController,timeout,directoryForDoc);
    // performing the initial process of starting the dstore and running all actions
        dstore.start();
    // println statement ensuring the Dstore port is connected and listening to the port that is returned as a response
        System.out.println(port + " is the listening port.");
    }

    // the start method, which is the starting point for starting all events in the class
    public void start(){
    // Signal the document directory to the controller
        signalDocDirectory(directoryForDoc);
    // Signal to the controller that the folder is empty
        signalEmptyFolder(directoryForDoc);
    // try statement
        try {
    // Creating a ServerSocket object
            ss = new ServerSocket(port);
    // catch IOException statement
        } catch (IOException ioException) {
    //if a RuntimeException exception occurs at any point in the processes' execution, it is handled by this interception statement
            throw new RuntimeException(ioException);
        }
    // try statement
        try {
    // adding a new local variable for the socket object to communicate with the controller
            socket = new Socket(InetAddress.getLoopbackAddress(), portController);
    // catch IOException statement
        } catch (IOException ioException) {
    // if a RuntimeException occurs at some point in the execution of the processes, it is handled by this interception statement
            throw new RuntimeException(ioException);}
    // adding a local variable that initializes a specific port writer with the value null
        PrintWriter writerForSocket = null;
    // try statement
        try {
    // adding a new local printWriter object for writing to the socket
            writerForSocket = new PrintWriter(socket.getOutputStream(), true);
    // catch IOException statement
        } catch (IOException ioException) {
    // if a RuntimeException occurs at some point in the execution of the processes, it is handled by this interception statement
            throw new RuntimeException(ioException);}
    // Sending a JOIN message to the controller with the port number
        writerForSocket.println("JOIN " + port);
    // Create a synchronization lock object
        Lock lock = new ReentrantLock();
    // loop to continue the processes until the components become false in a Boolean manner
        while (true) {
    // adding a new local variable for the client and initializing it with the value null
            Socket user = null;
    // try statement
            try {
    // Accepting inbound links from customers
                user = ss.accept();
    // catch IOException statement
            } catch (IOException ioException) {
    // if a RuntimeException occurs at any point in the execution of the processes, it is handled by this interception statement
                throw new RuntimeException(ioException);}
    // if a RuntimeException exception occurs at any point in the processes' execution, it is handled by this interception statement
            System.out.println("Client " + user + " successfully established a connection with the server.");
    // adding a local variable to finalize the end user
            Socket finalUser = user;
    // adding a local writer variable to a port
            PrintWriter finalWriterForSocket = writerForSocket;
    // adding a new thread that contains all the matches needed to execute the processes
            new Thread(() -> {
    // try statement
                try {
    // adding a local variable for a reader that accepts the end client's input stream and scans it
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(finalUser.getInputStream()));
    // adding a local variable that represents the scanned output of the client
                    OutputStream userInfo = finalUser.getOutputStream();
    // adding a local variable to determine the (recorder) of the scanned client information and adding it to the Dstore
                    PrintWriter inputStream = new PrintWriter(userInfo, true);
    // add a declaration that ensures that the method will not continue if the value of the scanned characters matches null
                    while (null != (scannedChar = bufferedReader.readLine())) {
    // adding a local variable to the list of characters to be split under blank space
                        String[] chars = scannedChar.split(" ");
    // Use locking to ensure exclusive access to critical sections.
                        lock.lock();
    // try statement
                        try {
    // starting with cases to identify different results from the scanned strings depending on the characters in them
                            switch (chars[0]) {
    // if it coincides with an encounter event with a LOAD DATA process
                                case "LOAD_DATA":
    // Data loading signal to the controller
                                    signalLoad(chars[1], userInfo);
    // printing the returned content of the command and informing about its successful addition to the system
                                    System.out.println("Loading command with contents" + userInfo + " was successfully received by the server");
    // moving on to the next case
                                    break;

    // if it coincides with an encounter with STORE process
                                case "STORE":
    // Signal to the controller to store a document
                                    signalStoringAck(inputStream, chars[1], Integer.parseInt(chars[2]));
    // set the duration of the specified users to the current value of the timeout variable
                                    finalUser.setSoTimeout(timeout);
    // Signal the receipt of a document to the controller
                                    signalDocReceiver(finalWriterForSocket, finalUser.getInputStream(), sizeOfFile, true);
    // println to inform the user of the successful receipt of the storage command from the server
                                    System.out.println("Store command was successfully received by the server");
    // moving on to the next case
                                    break;

    // if it coincides with an encounter with STORE process
                                case "ACK":
    // Signal to send document information to the controller
                                    signalDocInfoSent(userInfo);
    // println to inform the user of the successful receipt of the confirmation command from the server
                                    System.out.println("Acknowledgement command was successfully received by the server");
    // moving on to the next case
                                    break;

    // if it coincides with an encounter with a LIST process
                                case "LIST":
    // Signalling the controller about the document list
                                    signalDocList(finalWriterForSocket);
    // println to inform the user of the successful receipt of the list command from the server
                                    System.out.println("List command was successfully received by the server");
    // moving on to the next case
                                    break;

    // if it coincides with an encounter with a LIST process
                                case "REMOVE":
    // Signalling the controller about the document list
                                    signalDocRemoval(finalWriterForSocket, chars[1]);
    // println to inform the user of the successful receipt of the remove command from the server
                                    System.out.println("Removal command was successfully received by the server");
    // moving on to the next case
                                    break;
    // Specify the default case that occurs if none of the previous commands meet the requirements
                                default:
    // returns that the scanned message is an unknown command to the system and therefore cannot be recognized.
                                    System.out.println("Non-known command executed.");
    // when all possible scenarios have been checked, you can proceed to the last step.
                            }} finally {
    // Release lock after critical section is complete
                            lock.unlock();
                        }}
    // The customer relationship is closed
                    finalUser.close();
    // catch IOException statement
                } catch (IOException ioException) {
    // println statement to inform the user of the encountered exception and its contents
                    System.out.println("Encountered exception " + ioException);
    // print the nature and reason for the exception
                    ioException.printStackTrace();
    // command to start the method
                }}).start();}}

    // method to remove a specific file from the system
    private synchronized static void signalDocRemoval(PrintWriter contentsAdder, String nameOfFile){
    // Creates a File object representing the document to be removed
        File doc = new File(directoryForDoc + File.separator + nameOfFile);
    // Checks if the document exists
        if(doc.exists()) {
    // Deletes the document and checks if the deletion was successful
            if(doc.delete()) {
    // informs the client about the successful removal of the corresponding file and also gives its name.
                System.out.println("Successful removal of file: " + nameOfFile);
    // Sending a "REMOVE_ACK" message to contentAdder
                contentsAdder.println("REMOVE_ACK " + nameOfFile);
    // if the file cannot be deleted
            } else {
    // prints and informs about failed file removal and gives the file name
                System.out.println("Unsuccessful removal of file: " + nameOfFile);}
    // if the file does not exist
        } else {
    // println and informs the user of the nonexistent nature of the file and its name
            System.out.println("The file: " + nameOfFile + " which you are trying to remove doesn't exist.");
    // Sends message "ERROR_FILE_DOES_NOT_EXIST" to contentsAdder
            contentsAdder.println("ERROR_FILE_DOES_NOT_EXIST " + nameOfFile);
            }}

    // a method for specifying a received file as an alert along with its contents
    private synchronized static void signalDocReceiver(PrintWriter contentsAdder, InputStream addedInfo, int sizeOfFile, boolean notifyStoreACK) {
    // try statement
        try {
    // Reading the contents of the document from the InputStream into an array of bytes
            byte[] info = new byte[sizeOfFile];
    // Creates a File object representing the document to be retrieved
            doc = new File(directoryForDoc + File.separator + nameOfFile);
    // Creates a FileOutputStream to write the resulting content to the document file
            FileOutputStream contents = new FileOutputStream(doc);
    // Reads the specified number of bytes from the addedInfo stream into the info array
            addedInfo.readNBytes(info, 0, sizeOfFile);
    // Writes the contents of the information array to the document file
            contents.write(info);
    // closes the output stream file as there is nothing more to add to it
            contents.close();
    // catch FileNotFoundException statement
        } catch (FileNotFoundException fileNotFoundException) {
    // print the nature and reason for the exception
            fileNotFoundException.printStackTrace();
    // catch IOException statement
        } catch (IOException ioException) {
    // print the nature and reason for the exception
            ioException.printStackTrace();}
    // checks whether the Boolean number representing a successful notification is true or false
        if(notifyStoreACK) {
    // Sending a "STORE_ACK" message to contentsAdder
            contentsAdder.println("STORE_ACK " + nameOfFile);}
    // prints and informs about the successful saving of the file and its name
        System.out.println("Successful saving of document: " + nameOfFile);}

    // method for identifying an empty folder and its address
    private static void signalEmptyFolder(String docDirectory) {
    // Creates a File object representing the document directory
        File folder = new File(docDirectory);
    // Checks if there is such a directory in the system
        if(!folder.exists()){
    // Creates the document directory if it does not exist
            folder.mkdir();}
    // Checks if the document directory is empty
        if(!isEmpty(docDirectory)){
    // Prints and informs the client of the removal of this empty directory
            System.out.println("Successful removal of an empty folder.");
    // Removes all files from the document directory
            emptyFolder(folder);
            }
     else {
        System.out.println("Folder is not empty. Deleting all files in the folder.");
        emptyFolder(folder);
    }}

    // Method for setting the folder of the corresponding file
    public static void signalDocDirectory(String docDirectory) {
    // Sets the directoryForDoc variable to the specified document directory
        Dstore.directoryForDoc = docDirectory;
    // prints the address of the folder and informs about its successful assignment as a folder of the corresponding file
        System.out.println(docDirectory + " successfully set as a file directory.");}

    // A method of informing the system of the saving of the relevant file, together with its name and number, by sending an alert to the system
    private synchronized static void signalStoringAck(PrintWriter contentsAdder, String nameOfFile, int sizeOfFile) {
    // try statement
        try {
    // Updates the sizeOfFile field with the specified value
            Dstore.sizeOfFile = sizeOfFile;
    // Updates the nameOfFile field with the specified value
            Dstore.nameOfFile = nameOfFile;
    // println statement to inform about successful sending of acknowledgement signal
            System.out.println("Sending an Acknowledgement signal.");
    // Sending an "ACK" message to contentsAdder
            contentsAdder.println("ACK");
    // catch Exception statement
        } catch (Exception exception) {
    // print the nature and reason for the exception
            exception.printStackTrace();
    // println and inform the client of the occurrence of the corresponding exception along with its contents.
            System.out.println("Exception: " + exception + " was encountered.");}}

    // method of loading a file into the system, along with its name and contents
    private synchronized static void signalLoad(String nameOfFile, OutputStream finalVersion){
    // try statement
        try {
     // Creates a File object representing the document to be loaded
            doc = new File(directoryForDoc + File.separator + nameOfFile);
    // Reading the contents of the document into a byte array
            byte[] info = Files.readAllBytes(doc.toPath());
    // Writes the contents of the document to the finalVersion OutputStream
            finalVersion.write(info);
    // print the successfully loaded file as well as its name
            System.out.println(nameOfFile + " is being loaded.");
    // catch IOException statement
        } catch (IOException ioException) {
    // print the nature and reason for the exception
            ioException.printStackTrace();
    // println and inform the client of the occurrence of the corresponding exception along with its contents.
            System.out.println(ioException + " has been encountered as an exception.");}}

    // A method for deleting the entire contents of a directory and making it empty
    private static void emptyFolder(File folder){
    // Goes through the files in the specific folder and continues until it scans all the files.
        for(File document : Objects.requireNonNull(folder.listFiles())){
    // Deletes all files in the specified folder
            document.delete();
    // println statement that informs about the successful removal of the desired file from the corresponding folder
            System.out.println("Document: " + nameOfFile + " has been successfully deleted from folder:" + folder);}}

    // Boolean method for checking whether the corresponding directory is empty and setting it only as true or false final value
    private static boolean isEmpty(String filePath) {
    // try, which adds a local field to specify the path to the desired directory
        try (Stream<Path> fields = Files.list(Path.of(filePath))) {
    // Checks if the specified directory is empty by trying to find the first file
            return fields.findFirst().isEmpty();
    // catch IOException statement
        } catch (IOException ioException) {
    // print the nature and reason for the exception
            ioException.printStackTrace();
    // println and inform the client of the failure to determine whether the folder is empty or not and return the exception
            System.out.println(ioException + " has been encountered as an exception.");}
    // if it did not continue and did not return any file path as response, then the method automatically returns false
        return false;}

    // A method of ensuring that the contents of a file are sent and delivered and not lost during transfer
    private synchronized static void signalDocInfoSent(OutputStream contentsAdder){
    // Creates a File object representing the transferred document
        doc = new File(directoryForDoc + File.separator + transferedDoc);
    // println for the content of a file and its successful sending to the system
        System.out.println( doc + " 's contents have been sent");
    // try statement
        try {
    // Reading the contents of the document into an array of bytes
            byte[] info = Files.readAllBytes(doc.toPath());
    // Writes the contents of the document to the contentsAdder OutputStream
            contentsAdder.write(info);
    // catch IOException statement
        } catch (IOException ioException) {
    // print the nature and reason for the exception
            ioException.printStackTrace();
    // prints an exception and its contents for not sending the contents of the file
            System.out.println(ioException + " has been encountered as an exception.");}}

    // A method for identifying the LIST command received from the controller and informing the client
    private synchronized static void signalDocList(PrintWriter contentsAdder){
    // Creates a File object representing the document directory
        File directory = new File(directoryForDoc);
    // Stores the names of all documents in the directory
        StringBuilder docs = new StringBuilder();
    // going through all the files and into the appropriate folder
        for(String doc : Objects.requireNonNull(directory.list())){
    // adding the names of all documents to the document list
            docs.append(" ").append(doc);}
    // Sends a "LIST" message with the list of documents to contentAdder
        contentsAdder.println("LIST" + docs);
    // prints the list containing all files and informs the client of its receipt by the controller
        System.out.println("List: " + docs + " was successfully received by the Controller");}}