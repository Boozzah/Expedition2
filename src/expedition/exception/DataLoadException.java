package expedition.exception;

/*
 * DataLoadException
 * 
 * Thrown when a data file fails to parse. Should always carry the
 * file name and line number of the offending line — forces callers
 * to handle a bad file rather than silently discovering it at runtime
*/ 

public class DataLoadException extends Exception {
    
    private final String fileName;
    private final int lineNumber;
   
    public DataLoadException(String fileName, int lineNumber, String reason) {
        super(fileName + " line " + lineNumber + ": " + reason);
        
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
}
