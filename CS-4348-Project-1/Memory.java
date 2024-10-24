import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Memory {

    // Declare constants and variables
    private static final int MEMORY_SIZE = 2000;
    private static int[] memory;

    // Main function
    public static void main(String[] args) {
        // Validate the input arguments
        validateInputArguments(args);
        
        // Get the file path from the arguments
        String filePath = args[0];
        
        // Initialize memory from file
        try {
            initializeMemory(filePath);
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + filePath);
            System.exit(0);
        }
        
        // Handle memory read and write operations
        handleMemoryOperations();
    }

    // Function to validate input arguments
    private static void validateInputArguments(String[] args) {
        if (args.length < 1) {
            System.err.println("Please provide the input file path.");
            System.exit(0);
        }
    }

    // Initialize memory from file
    private static void initializeMemory(String filePath) throws FileNotFoundException {
        memory = new int[MEMORY_SIZE];
        
        // Use a try-with-resources block to auto-close the Scanner
        try (Scanner scanner = new Scanner(new File(filePath))) {
            int addr = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                
                // Skip empty lines
                if (line.isEmpty()){
                    continue;
                }
                
                // Detect and set address label
                if (line.startsWith(".")) {
                    addr = Integer.parseInt(line.substring(1).split("\\s+")[0]);
                    continue;
                }
                
                // Parse and store memory data
                if (Character.isDigit(line.charAt(0))) {
                    memory[addr++] = Integer.parseInt(line.split("\\s+")[0]);
                }
            }
        }
    }

    // Function to handle memory operations
    private static void handleMemoryOperations() {
        try (Scanner inputScanner = new Scanner(System.in)) {
            while (inputScanner.hasNextLine()) {
                String line = inputScanner.nextLine();
                String instruction = line.substring(0, 1);
                int addr, val;
                
                // Switch based on the operation type
                switch (instruction) {
                    case "R": // Read operation
                        addr = Integer.parseInt(line.substring(1));
                        if (isValidAddress(addr)) {
                            System.out.println(readMemory(addr));
                        }
                        break;
                    case "W": // Write operation
                        String[] params = line.substring(1).split(",");
                        addr = Integer.parseInt(params[0]);
                        val = Integer.parseInt(params[1]);
                        if (isValidAddress(addr)) {
                            writeMemory(addr, val);
                        }
                        break; 
                }
            }
        }
    }

    // Function to check if an address is valid
    private static boolean isValidAddress(int address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            System.err.println("Invalid memory address.");
            return false;
        }
        return true;
    }

    // Function to read from a memory address
    private static int readMemory(int address) {
        return memory[address];
    }

    // Function to write to a memory address
    private static void writeMemory(int address, int data) {
        memory[address] = data;
    }
}
