import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Index {
    // ArrayList to store the names of files
    ArrayList<String> nameOfFile;
    // HashMap to store the state of files (mapping file names to their state)
    HashMap<String, Integer> stateOfFile;
    // Map to store the mapping of file names to a list of integers
    private Map<String, List<Integer>> mapping;
    // Getter method to retrieve the stateOfFile HashMap

    public HashMap<String, Integer> getStateOfFile() {
    // return the state of the file
        return stateOfFile;
    }
    // Getter method to retrieve the nameOfFile ArrayList
    public ArrayList<String> getNameOfFile() {
    // returns the name of the file
        return nameOfFile;
    }
    // Getter method to retrieve the mapping Map
    public Map<String, List<Integer>> getMapping() {
    // returns the mapping of the file
        return mapping;}
    // Setter method to set the stateOfFile HashMap
    public void setStateOfFile(HashMap<String, Integer> stateOfFile) {
    // sets the state of the file
        this.stateOfFile = stateOfFile;
    }
    // Constructor to initialize the Index object with nameOfFile and stateOfFile
    public Index (ArrayList<String> nameOfFile, HashMap<String, Integer> stateOfFile){
    // initialises the name of the file variable
        this.nameOfFile = nameOfFile;
    // initialises the state of the file variable
        this.stateOfFile = stateOfFile;}
    // Setter method to set the nameOfFile ArrayList
    public void setNameOfFile(ArrayList<String> nameOfFile) {
    // sets the name of the file
        this.nameOfFile = nameOfFile;
    }
    // Method to clear the nameOfFile and stateOfFile collections
    public void clear(){
    // clears the name of the file
        this.nameOfFile.clear();
    // clears the state of the file
        this.stateOfFile.clear();}}