
/**
 * The FileSystem class represents a file system in a disk allocation simulation.
 * It handles file operations and delegates the actual allocation and deallocation 
 * of disk space to specific allocation strategy classes (Contiguous, Indexed, or Chained).
 * 
 * Author: Danny Amezquita
 * Class: CS 4348.001
 */
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileSystem {
    // The allocation method used (e.g., "contiguous", "indexed", "chained")
    private String allocationMethod;
    // The DiskDrive instance representing the simulated disk
    private DiskDrive diskDrive;
    // The File Allocation Table (FAT) storing file metadata
    private List<FATEntry> fileTable;
    // Instances of different allocation strategies
    private ContiguousAllocation contiguous;
    private IndexedAllocation indexed;
    private ChainedAllocation chained;

    /**
     * Constructor for the FileSystem.
     * Initializes the DiskDrive and the File Allocation Table.
     * Also, initializes the specific allocation strategy instances based on the provided method.
     * 
     * @param allocationMethod The method of disk space allocation (e.g., "contiguous", "indexed", "chained").
     */
    public FileSystem(String allocationMethod) {
        this.allocationMethod = allocationMethod;
        this.diskDrive = new DiskDrive(); // Initialize the DiskDrive
        this.fileTable = new ArrayList<>(); // Initialize the file table
        // Initialize allocation method instances
        this.contiguous = new ContiguousAllocation(diskDrive, this); // Initialize the Contiguous Allocation
        this.chained = new ChainedAllocation(diskDrive, this); // Initialize the Chained Allocation
        this.indexed = new IndexedAllocation(diskDrive, this); // Initialize the Indexed Allocation
    }

    /**
     * Displays the content of a file stored in the simulation.
     * This method handles different allocation methods (contiguous, chained, indexed) to retrieve and display the file content.
     * 
     * @param fileName The name of the file to display.
     */
    public void displayFile(String fileName) {
        FATEntry fileEntry = getFileEntry(fileName);
        if (fileEntry == null) {
            System.out.println("File not found: " + fileName);
            return;
        }
        StringBuilder fileContent = new StringBuilder();
        if ("contiguous".equals(allocationMethod)) {
            // Handle contiguous file allocation
            int blocksToRead = fileEntry.fileLength;
            for (int i = 0; i < blocksToRead; i++) {
                int blockNumber = fileEntry.startingBlock + i;
                byte[] blockData = diskDrive.readBlock(blockNumber);
                appendValidCharacters(fileContent, blockData);
                fileContent.append("\n"); // Add newline after each block
            }
        } else if ("chained".equals(allocationMethod)) {
            // Handle chained file allocation
            int currentBlock = fileEntry.startingBlock;
            while (currentBlock != -1) {
                byte[] blockData = diskDrive.readBlock(currentBlock);
                appendValidCharacters(fileContent, blockData);
                fileContent.append("\n"); // Add newline after each block
                currentBlock = chained.getNextBlock(currentBlock);
            }
        } else if ("indexed".equals(allocationMethod)) {
            // Handle indexed file allocation
            byte[] indexBlockData = diskDrive.readBlock(fileEntry.indexBlock);
            ByteBuffer buffer = ByteBuffer.wrap(indexBlockData);
            while (buffer.hasRemaining()) {
                int blockNumber = buffer.getInt();
                if (blockNumber == 0) break; // Assuming 0 indicates the end of the block numbers in the index block
                byte[] blockData = diskDrive.readBlock(blockNumber);
                appendValidCharacters(fileContent, blockData);
                fileContent.append("\n"); // Add newline after each block
            }
        } else {
            // Optionally, handle other allocation methods like indexed
            System.out.println("Unsupported allocation method for display: " + allocationMethod);
        }
        System.out.println("Contents of " + fileName + ":");
        System.out.println(fileContent.toString());
    }

    /**
     * Displays the File Allocation Table (FAT).
     * This method checks the allocation method and calls the respective method to display the FAT.
     * 
     * @param blockNumber The block number of the FAT (usually 0).
     */
    public void displayFAT(int blockNumber) {
        // Check if blockNumber is valid for FAT
        if (blockNumber != 0) {
            System.out.println("Invalid block number for FAT.");
            return;
        }
        //System.out.println("Current FAT entries: " + fileTable);
        // Check if there are any files in the FAT
        if (fileTable.isEmpty()) {
            System.out.println("No files found in File Allocation Table.");
            return;
        }
        // Display the FAT based on the allocation method
        switch (allocationMethod) {
            case "contiguous":
                displayContiguousFAT();
                break;
            case "chained":
                displayChainedFAT();
                break;
            case "indexed":
                displayIndexedFAT();
                break;
            default:
                System.out.println("Invalid allocation method.");
        }
    }

    /**
     * Displays the File Allocation Table for contiguous allocation.
     * Lists each file's name, starting block, and length.
     */
    public void displayContiguousFAT() {
        System.out.println("Displaying contiguous FAT");
        System.out.printf("%-20s %-15s %-10s\n", "File Name", "Starting Block", "Length");
        for (FATEntry entry : fileTable) {
            // Display only if it's a contiguous allocation entry
            if (entry.startingBlock != -1 && entry.fileLength != -1) {
                System.out.printf("%-20s %-15d %-10d\n", 
                                entry.fileName, entry.startingBlock, entry.fileLength);
            }
        }
    }

    /**
     * Displays the File Allocation Table for chained allocation.
     * Lists each file's name, starting block, and the total number of blocks.
     */
    public void displayChainedFAT() {
        System.out.println("Displaying chained FAT");
        System.out.println("File Name\tStart Block\tLength");
        // Iterate over each entry in the FAT
        for (FATEntry entry : fileTable) {
            // Start with the starting block and a length of 0
            int currentBlock = entry.startingBlock;
            int length = 0;
            // Follow the chain of blocks to calculate the total length
            while (currentBlock != -1) {  // Assuming -1 is the end-of-file marker
                length++;
                currentBlock = chained.getNextBlock(currentBlock);
            }
            // Print the file information with the calculated length
            System.out.println(entry.fileName + "\t\t" + entry.startingBlock + "\t\t" + length);
        }
    }

    /**
     * Displays the File Allocation Table for indexed allocation.
     * Lists each file's name and its index block number.
     */
    public void displayIndexedFAT() {
        System.out.println("Indexed File Allocation Table:");
        System.out.println("File Name\tIndex Block");
        for (FATEntry entry : fileTable) {
            // Check if the entry is for indexed allocation
            if (entry.indexBlock != -1) {
                System.out.println(entry.fileName + "\t\t" + entry.indexBlock);
            }
        }
    }


    /**
     * Displays the bitmap representing free and occupied space on the disk.
     * 
     * @param blockNumber The block number of the bitmap (usually 1).
     */
    public void displayFreeSpaceBitmap(int blockNumber) {
        byte[] bitmapBlock = diskDrive.getBitmap(); // Get the bitmap directly
        // Check if blockNumber is valid for bitmap
        if (blockNumber != 1) {
            System.out.println("Invalid block number for bitmap.");
            return;
        }
        StringBuilder bitmapDisplay = new StringBuilder();
        for (byte b : bitmapBlock) {
            String binaryString = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            bitmapDisplay.append(binaryString);
        }
        // Display the bitmap in lines of 32 bits
        for (int i = 0; i < DiskDrive.DISK_SIZE; i += 32) {
            if (i + 32 <= DiskDrive.DISK_SIZE) {
                System.out.println(bitmapDisplay.substring(i, i + 32));
            } else {
                System.out.println(bitmapDisplay.substring(i));
            }
        }
    }

    /**
     * Displays the contents of a specific disk block.
     * This method handles special cases for FAT, bitmap, and indexed allocation's index blocks.
     * 
     * @param blockNumber The block number to display.
     */
    public void displayDiskBlock(int blockNumber) {
        if (blockNumber < 0 || blockNumber >= DiskDrive.DISK_SIZE) {
            System.out.println("Block number is out of bounds");
            System.out.println("Valid block numbers: 0 to " + (DiskDrive.DISK_SIZE - 1));
            return;
        }
        // Special handling for block 0 (FAT) and block 1 (bitmap)
        if (blockNumber == 0) {
            displayFAT(blockNumber);
            return;
        } else if (blockNumber == 1) {
            displayFreeSpaceBitmap(blockNumber);
            return;
        }
        // Read the actual block content
        byte[] blockContent = diskDrive.readBlock(blockNumber);
        StringBuilder displayString = new StringBuilder();
        if ("indexed".equals(allocationMethod)) {
            if (isIndexBlock(blockNumber)) {
                displayIndexBlockContent(blockContent);
            } else {
                // Display as a regular data block
                for (byte b : blockContent) {
                    displayString.append(b != 0 ? (char) b : ' ');
                }
                //System.out.println("Contents of block " + blockNumber + ":");
                //System.out.println(displayString.toString());
            }
        } else if ("chained".equals(allocationMethod)) {
            int pointerSize = Integer.BYTES;
            int contentLength = blockContent.length - pointerSize;
            for (int i = 0; i < contentLength; i++) {
                byte b = blockContent[i];
                displayString.append(b != 0 ? (char) b : ' ');
            }
            int nextBlockPointer = ByteBuffer.wrap(blockContent, contentLength, pointerSize).getInt();
            displayString.append("\nNext Block Pointer: ").append(nextBlockPointer == -1 ? "EOF" : nextBlockPointer);
        } else {
            for (byte b : blockContent) {
                displayString.append(b != 0 ? (char) b : ' ');
            }
        }
        System.out.println("Contents of block " + blockNumber + ":");
        System.out.println(displayString.toString());
    }

    /**
     * Copies a file from the simulation to the real file system.
     * This method reads the file content from the simulation and writes it to a specified path in the real system.
     * 
     * @param simFileName  The name of the file in the simulation.
     * @param realFilePath The path in the real file system where the file will be copied.
     */
    public void copyFileToRealSystem(String simFileName, String realFilePath) {
        FATEntry fileEntry = getFileEntry(simFileName);
        if (fileEntry == null) {
            System.out.println("File not found in simulation: " + simFileName);
            return;
        }
        File realFile = new File(realFilePath);
        if (!realFile.exists()) {
            System.out.println("The real file does not exist: " + realFilePath);
            return;
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if ("contiguous".equals(allocationMethod)) {
                for (int i = 0; i < fileEntry.fileLength; i++) {
                    int blockNumber = fileEntry.startingBlock + i;
                    byte[] blockData = diskDrive.readBlock(blockNumber);
                    outputStream.write(blockData, 0, findValidEnd(blockData));
                    if (i < fileEntry.fileLength - 1) {
                        outputStream.write('\n'); // Add a newline character after each block except the last one
                    }
                }
            } else if ("indexed".equals(allocationMethod)) {
                // Read the index block
                byte[] indexBlockData = diskDrive.readBlock(fileEntry.indexBlock);
                ByteBuffer buffer = ByteBuffer.wrap(indexBlockData);
                // Read each block pointed to by the index block
                while (buffer.hasRemaining()) {
                    int blockNumber = buffer.getInt();
                    if (blockNumber == 0) break; // Assuming 0 indicates the end of the block numbers in the index block
                    byte[] blockData = diskDrive.readBlock(blockNumber);
                    outputStream.write(blockData, 0, findValidEnd(blockData));
                    // Optionally, add newline characters between blocks
                    outputStream.write('\n');
                }
            } else {
                System.out.println("Unsupported allocation method for copying: " + allocationMethod);
                return;
            }
            byte[] fileData = outputStream.toByteArray();
            try (FileOutputStream fos = new FileOutputStream(realFile, false)) {
                fos.write(fileData);
                System.out.println("File " + simFileName + " contents copied to existing real system file: " + realFilePath);
            } catch (IOException e) {
                System.out.println("Error copying to the existing real system file: " + e.getMessage());
            }
        } catch (IOException e) {
            System.out.println("Error processing file data: " + e.getMessage());
        }
    }

    /**
     * Copies a file from the real system to the simulation.
     * This method reads a file from the real file system and stores it in the simulation using the current allocation method.
     * 
     * @param realFilePath The path of the file in the real system.
     * @param simFileName  The name under which the file will be stored in the simulation.
     */
    public void copyFileToSimulation(String realFilePath, String simFileName) {
        // Read the file from the real system
        byte[] fileData = readFileFromRealSystem(realFilePath);
        // Check if the file data could be read
        if (fileData == null) {
            System.out.println("Error reading file from real system.");
            return;
        }
        // Delegate to the appropriate allocation class
        switch (allocationMethod) {
            case "contiguous":
                contiguous.copyFileToSimulation(fileData, simFileName, realFilePath);
                break;
            case "chained":
                chained.copyFileToSimulation(fileData, simFileName, realFilePath);
                break;
            case "indexed":
                indexed.copyFileToSimulation(fileData, simFileName, realFilePath);
                break;
            default:
                System.out.println("Invalid allocation method.");
                return;
        }
    }


    /**
     * Deletes a file from the simulation.
     * This method finds the file in the FAT, delegates its deletion to the appropriate allocation class, and removes it from the FAT.
     * 
     * @param fileToDelete The name of the file to be deleted.
     */
    public void deleteFile(String fileToDelete) {
        // Find the file in the FAT
        FATEntry fileEntry = getFileEntry(fileToDelete);
        if (fileEntry == null) {
            System.out.println("File not found: " + fileToDelete);
            return;
        }
        // Delegate the deletion to the appropriate allocation class
        switch (allocationMethod) {
            case "contiguous":
                contiguous.deleteFile(fileEntry);
                break;
            case "chained":
                chained.deleteFile(fileEntry);
                break;
            case "indexed":
                indexed.deleteFile(fileEntry);
                break;
            default:
                System.out.println("Invalid allocation method.");
                return;
        }
        // Remove the file from the FAT
        fileTable.remove(fileEntry);
        System.out.println("File " + fileToDelete + " deleted.");
    }

    /**
     * Checks if a given block number corresponds to an index block in the indexed allocation.
     * 
     * @param blockNumber The block number to check.
     * @return True if the block is an index block, false otherwise.
     */
    public boolean isIndexBlock(int blockNumber) {
        for (FATEntry entry : fileTable) {
            if (entry.indexBlock != -1 && entry.indexBlock == blockNumber) {
                return true; // The block is an index block for a file
            }
        }
        return false; // The block is not an index block
    }

////////////////////////////////////////////////////////////////
// Private Helper Methods
////////////////////////////////////////////////////////////////
    /**
     * Displays the content of an index block.
     * This method reads block pointers from the given block content and prints them.
     * It assumes that a pointer value of 0 indicates the end of the block content.
     * 
     * @param blockContent The byte array representing the index block's content.
     */
    private void displayIndexBlockContent(byte[] blockContent) {
        ByteBuffer buffer = ByteBuffer.wrap(blockContent);
        System.out.println("Index Block:");
        while (buffer.hasRemaining()) {
            int blockPointer = buffer.getInt();
            if (blockPointer == 0) break; // Assuming 0 indicates no more pointers
            System.out.println("Block #" + blockPointer);
        }
    }

    /**
     * Appends valid ASCII printable characters from a byte array to a StringBuilder.
     * This method filters out non-printable characters from the byte array.
     * 
     * @param builder The StringBuilder to which the characters are appended.
     * @param data    The byte array containing the characters.
     */
    private void appendValidCharacters(StringBuilder builder, byte[] data) {
        for (byte b : data) {
            if (b > 31 && b < 127) { // ASCII printable characters range from 32 to 126
                builder.append((char) b);
            }
        }
    }

    /**
     * Finds the end index of valid ASCII printable characters in a byte array.
     * This method scans the array from the end to find the last printable character.
     * 
     * @param data The byte array to scan.
     * @return The end index of the printable characters in the array.
     */
    private int findValidEnd(byte[] data) {
        int endIndex = data.length;
        for (int i = data.length - 1; i >= 0; i--) {
            if (data[i] > 31 && data[i] < 127) { // Check for printable ASCII characters
                endIndex = i + 1;
                break;
            }
        }
        return endIndex;
    }

    /**
     * Reads a file from the real file system into a byte array.
     * This method checks if the file exists and reads its content.
     * It returns null if the file does not exist or if an error occurs during reading.
     * 
     * @param realFilePath The path to the file in the real file system.
     * @return A byte array containing the file's data, or null if an error occurs.
     */
    private byte[] readFileFromRealSystem(String realFilePath) {
        File file = new File(realFilePath);
        if (!file.exists()) {
            System.out.println("File does not exist: " + realFilePath);
            return null;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] fileData = new byte[(int) file.length()];
            int bytesRead = fis.read(fileData);
            if (bytesRead != fileData.length) {
                System.out.println("Error reading the file.");
                return null;
            }
            return fileData;
        } catch (IOException e) {
            System.out.println("Error reading the file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Adds a file entry to the File Allocation Table (FAT).
     * This method uses different parameters based on the allocation method (contiguous, chained, or indexed).
     * 
     * @param fileName      The name of the file.
     * @param startingBlock The starting block number for contiguous or chained allocations.
     * @param fileLength    The length of the file in blocks for contiguous or chained allocations.
     * @param indexBlock    The index block number for indexed allocation.
     */
    public void addFileToFAT(String fileName, int startingBlock, int fileLength, int indexBlock) {
        switch (allocationMethod) {
            case "contiguous":
            case "chained":
                // For contiguous and chained, use starting block and file length
                fileTable.add(new FATEntry(fileName, startingBlock, fileLength));
                break;
            case "indexed":
                // For indexed, use index block only (assuming startingBlock holds the index block in this case)
                fileTable.add(new FATEntry(fileName, indexBlock));
                break;
            default:
                System.out.println("Invalid allocation method.");
        }
    }

////////////////////////////////////////////////////////////////
// FATEntry Inner Class methods:
////////////////////////////////////////////////////////////////
    /**
     * Retrieves the FATEntry for a given file name.
     * 
     * @param fileName The name of the file whose FATEntry is to be retrieved.
     * @return The FATEntry object if the file is found, null otherwise.
     */
    public FATEntry getFileEntry(String fileName) {
        for (FATEntry entry : fileTable) {
            if (entry.fileName.equals(fileName)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Inner class representing an entry in the File Allocation Table (FAT).
     * This class stores metadata about a file, such as its name, location on disk,
     * and other attributes depending on the allocation method used.
     */
    public class FATEntry {
        // The name of the file
        String fileName;
        // The starting block number; used in contiguous and chained allocations
        int startingBlock;
        // The length of the file in blocks; used in contiguous and chained allocations
        int fileLength;
        // The index block number; used in indexed allocation
        int indexBlock;
        /**
         * Constructor for creating a FATEntry for contiguous and chained allocations.
         * 
         * @param fileName      The name of the file.
         * @param startingBlock The starting block number of the file.
         * @param fileLength    The length of the file in blocks.
         */
        FATEntry(String fileName, int startingBlock, int fileLength) {
            this.fileName = fileName;
            this.startingBlock = startingBlock;
            this.fileLength = fileLength;
            this.indexBlock = -1; // Not used in these allocations
        }
        /**
         * Constructor for creating a FATEntry for indexed allocation.
         * 
         * @param fileName   The name of the file.
         * @param indexBlock The index block number of the file.
         */
        FATEntry(String fileName, Integer indexBlock) {
            this.fileName = fileName;
            this.indexBlock = indexBlock;
            // startingBlock and fileLength are not used in indexed allocation
            this.startingBlock = -1; // Indicating not used
            this.fileLength = -1; // Indicating not used
        }
    }
}
