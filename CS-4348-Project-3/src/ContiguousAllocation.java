/**
 * The ContiguousAllocation class represents a contiguous allocation strategy in a file system.
 * This class manages the allocation of disk space for files in contiguous blocks.
 * It works in conjunction with the DiskDrive and FileSystem classes to simulate file storage.
 * 
 * Author: Danny Amezquita 
 * Class: CS 4348.001
 */
import java.util.Arrays;

public class ContiguousAllocation {
    // Maximum file size in blocks that can be handled by this allocation strategy
    public static final int MAX_FILE_SIZE = 10; 
    // DiskDrive instance representing the disk on which files are stored
    protected DiskDrive diskDrive;
    // FileSystem instance for managing files
    protected FileSystem fileSystem;
    // Bitmap to track free and occupied blocks on the disk
    protected boolean[] bitmap;

    /**
     * Constructor for ContiguousAllocation.
     * Initializes the allocation with a DiskDrive and a FileSystem.
     * 
     * @param diskDrive  The DiskDrive instance to interact with the disk.
     * @param fileSystem The FileSystem instance for file management.
     */
    public ContiguousAllocation(DiskDrive diskDrive, FileSystem fileSystem) {
        this.diskDrive = diskDrive;
        this.fileSystem = fileSystem;
        this.bitmap = new boolean[DiskDrive.DISK_SIZE];
        // Initially, all blocks are free except for the first two (FAT and bitmap)
        for (int i = 2; i < DiskDrive.DISK_SIZE; i++) {
            this.bitmap[i] = true;
        }
        // The first two blocks are reserved and hence not free
        this.bitmap[0] = this.bitmap[1] = false;
    }

    /**
     * Copies a file to the simulated disk, applying the contiguous allocation strategy.
     * 
     * @param fileData     The data of the file to be copied.
     * @param simFileName  The name of the file in the simulation.
     * @param realFilePath The actual file path of the file.
     */
    public void copyFileToSimulation(byte[] fileData, String simFileName, String realFilePath) {
        // Check if the file is too large for the maximum allowed file size
        if (fileData.length > MAX_FILE_SIZE * DiskDrive.BLOCK_SIZE) {
            System.out.println("File is too large.");
            System.out.println("Maximum file size is " + MAX_FILE_SIZE + " blocks.");
            System.out.println("Each block is " + DiskDrive.BLOCK_SIZE + " bytes.");
            System.out.println("So Maximum file size is " + MAX_FILE_SIZE * DiskDrive.BLOCK_SIZE + " bytes.");
            return;
        }
        // Find contiguous blocks for the file
        int startingBlock = findContiguousBlocks(fileData.length);
        if (startingBlock == -1) {
            System.out.println("Not enough contiguous space on disk.");
            return;
        }
        // Calculate the number of blocks needed to store the file
        int fileLengthInBlocks = (int) Math.ceil((double) fileData.length / DiskDrive.BLOCK_SIZE);
        // Write the file to the disk
        writeBlocks(startingBlock, fileData);
        // Update bitmap for the allocated blocks
        allocateBlocks(startingBlock, fileLengthInBlocks);
        // Add the file to the file table
        fileSystem.addFileToFAT(simFileName, startingBlock, fileLengthInBlocks, -1);
        System.out.println("File " + realFilePath + " copied to simulation.");
        System.out.println("Starting block: " + startingBlock);
    }

    /**
     * Deletes a file from the disk by freeing up its allocated blocks.
     * This method updates the bitmap to reflect the newly freed blocks.
     * 
     * @param fileEntry The FATEntry object representing the file to be deleted.
     */
    public void deleteFile(FileSystem.FATEntry fileEntry){
        // Free up the disk space
        int blocksToFree = fileEntry.fileLength;
        int startingBlock = fileEntry.startingBlock;
        for (int i = 0; i < blocksToFree; i++) {
            freeBlock(startingBlock + i);  // Free each block
        }
        freeBlocks(fileEntry.startingBlock, fileEntry.fileLength);
    }

    /**
     * Writes data into the disk starting from a specified block number.
     * The data is divided into full and partial blocks as needed, 
     * and each block is written sequentially on the disk.
     * This method also marks the used blocks as occupied in the bitmap.
     * 
     * @param startingBlock The starting block number to write the data.
     * @param data          The data to be written to the disk.
     */
    public void writeBlocks(int startingBlock, byte[] data) {
        // Calculate the number of full and partial blocks needed for the data
        int fullBlocks = data.length / DiskDrive.BLOCK_SIZE;
        int remainingData = data.length % DiskDrive.BLOCK_SIZE;
        
        // Write full blocks
        for (int i = 0; i < fullBlocks; i++) {
            byte[] blockData = Arrays.copyOfRange(data, i * DiskDrive.BLOCK_SIZE, (i + 1) * DiskDrive.BLOCK_SIZE);
            diskDrive.writeBlock(startingBlock + i, blockData);
            setBlockUsed(startingBlock + i);
        }
        
        // Write the remaining partial block, if any
        if (remainingData > 0) {
            byte[] blockData = new byte[DiskDrive.BLOCK_SIZE];
            System.arraycopy(data, fullBlocks * DiskDrive.BLOCK_SIZE, blockData, 0, remainingData);
            diskDrive.writeBlock(startingBlock + fullBlocks, blockData);
            setBlockUsed(startingBlock + fullBlocks);
        }
    }

    /**
     * Marks a specific block on the disk as used.
     * This method updates the bitmap to indicate that the block is no longer available.
     * 
     * @param blockNumber The block number to mark as used.
     */
    public void setBlockUsed(int blockNumber) {
        bitmap[blockNumber] = false;
    }

    ////////////////////////////////////////////////////////////////
    // Private helper methods

    /**
     * Finds a sequence of contiguous free blocks on the disk sufficient to store a file of a given size.
     * 
     * @param sizeInBytes The size of the file in bytes.
     * @return The starting block number of the contiguous free space, or -1 if insufficient space is available.
     */
    private int findContiguousBlocks(int sizeInBytes) {
        int requiredBlocks = (int) Math.ceil((double) sizeInBytes / DiskDrive.BLOCK_SIZE);
        for (int i = 0; i < bitmap.length - requiredBlocks + 1; i++) {
            // Check if there is a sequence of 'requiredBlocks' free blocks starting from 'i'
            boolean sufficientSpace = true;
            for (int j = i; j < i + requiredBlocks; j++) {
                if (!bitmap[j]) { // If the block is not free
                    sufficientSpace = false;
                    break;
                }
            }
            if (sufficientSpace) {
                return i; // Starting block of the contiguous free space
            }
        }
        return -1; // Indicates not enough contiguous space is available
    }

    /**
     * Frees up a block on the disk, marking it as available in the bitmap and optionally clearing its data.
     * 
     * @param blockNumber The block number to be freed.
     */
    private void freeBlock(int blockNumber) {
        if (blockNumber < 0 || blockNumber >= DiskDrive.DISK_SIZE) {
            System.out.println("Block number is out of bounds.");
            return;
        }
        // Mark the block as free in the bitmap
        bitmap[blockNumber] = true;
        // Optionally clear the block data
        byte[] emptyData = new byte[DiskDrive.BLOCK_SIZE]; // An array of zeros
        diskDrive.writeBlock(blockNumber, emptyData);
    }

    /**
     * Marks a sequence of blocks as used, starting from a specific block.
     * This method updates the disk's bitmap to reflect the allocation.
     * 
     * @param startingBlock The starting block number of the sequence.
     * @param length        The number of blocks to allocate.
     */
    private void allocateBlocks(int startingBlock, int length) {
        for (int i = 0; i < length; i++) {
            diskDrive.setBitmapBit(startingBlock + i, true); // Mark as used
        }
    }

    /**
     * Frees a sequence of blocks, starting from a specific block.
     * This method updates the disk's bitmap to reflect the deallocation.
     * 
     * @param startingBlock The starting block number of the sequence.
     * @param length        The number of blocks to free.
     */
    private void freeBlocks(int startingBlock, int length) {
        for (int i = 0; i < length; i++) {
            diskDrive.setBitmapBit(startingBlock + i, false); // Mark as free
        }
    }
}
