/**
 * The ChainedAllocation class represents a file allocation strategy using chained (or linked) allocation.
 * In this strategy, each file is a chain of blocks linked together, 
 * with each block containing a pointer to the next block in the file.
 * This class is responsible for managing the allocation and deallocation of disk blocks 
 * for files in a disk drive simulated by the DiskDrive class, in conjunction with the FileSystem class.
 * 
 * Author: Danny Amezquita
 * Class: CS 4348.001
 */

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChainedAllocation {
    // DiskDrive instance representing the disk on which files are stored
    protected DiskDrive diskDrive;
    // FileSystem instance for managing files
    protected FileSystem fileSystem;
    // Bitmap to track free and occupied blocks on the disk
    protected boolean[] bitmap;
    // Maximum file size in blocks that can be handled by this allocation strategy
    public static final int MAX_FILE_SIZE = 10;

    /**
     * Constructor for ChainedAllocation.
     * Initializes the allocation with a DiskDrive and a FileSystem.
     * Marks the first two blocks as reserved and assumes all other blocks are initially free.
     * 
     * @param diskDrive  The DiskDrive instance to interact with the disk.
     * @param fileSystem The FileSystem instance for file management.
     */
    public ChainedAllocation(DiskDrive diskDrive, FileSystem fileSystem) {
        this.diskDrive = diskDrive;
        this.fileSystem = fileSystem;
        this.bitmap = new boolean[DiskDrive.DISK_SIZE];
        // Initialization logic specific to the allocation strategy
        Arrays.fill(this.bitmap, true); // Assume all blocks are free initially
        this.bitmap[0] = this.bitmap[1] = false; // The first two blocks are reserved
    }

    /**
     * Copies a file into the simulation using chained allocation.
     * This method checks if the file is too large, finds free blocks,
     * splits the file data into these blocks, and links them together.
     * It also updates the FAT (File Allocation Table) with the file's starting block and size.
     * 
     * @param fileData     The data of the file to be copied.
     * @param simFileName  The name of the file in the simulation.
     * @param realFilePath The actual file path of the file.
     */
    public void copyFileToSimulation(byte[] fileData, String simFileName, String realFilePath) {
        // Check if the file is too large
        if (fileData.length > MAX_FILE_SIZE * DiskDrive.BLOCK_SIZE) {
            System.out.println("File is too large.");
            System.out.println("Maximum file size is " + MAX_FILE_SIZE + " blocks.");
            System.out.println("Each block is " + DiskDrive.BLOCK_SIZE + " bytes.");
            System.out.println("So Maximum file size is " + MAX_FILE_SIZE * DiskDrive.BLOCK_SIZE + " bytes.");
            return;
        }
        // Split the file data into blocks, and link them
        List<Integer> blockNumbers = new ArrayList<>();
        int currentBlockIndex = 0;
        while (currentBlockIndex * DiskDrive.BLOCK_SIZE < fileData.length) {
            int nextBlockIndex = findFreeBlock();
            if (nextBlockIndex == -1) {
                System.out.println("Disk is full or not enough free blocks available.");
                return; // Not enough space
            }
            blockNumbers.add(nextBlockIndex);
            setBlockUsed(nextBlockIndex);
            currentBlockIndex++;
        }
        // Now we have a list of block numbers to be used; let's copy the data
        for (int i = 0; i < blockNumbers.size(); i++) {
            byte[] blockData = new byte[DiskDrive.BLOCK_SIZE];
            int start = i * DiskDrive.BLOCK_SIZE;
            int end = Math.min((i + 1) * DiskDrive.BLOCK_SIZE, fileData.length);
            // Copy only the relevant portion of fileData to blockData
            System.arraycopy(fileData, start, blockData, 0, end - start);
            // Write the block data
            diskDrive.writeBlock(blockNumbers.get(i), blockData);
            
            // Update the FAT to set the next block pointer
            if (i < blockNumbers.size() - 1) {
                // Not the last block, so set the pointer to the next block
                setNextBlockPointer(blockNumbers.get(i), blockNumbers.get(i + 1));
            } else {
                // Last block, so set the end-of-file marker
                setNextBlockPointer(blockNumbers.get(i), -1); // Assuming -1 indicates EOF
            }
        }
        for (int blockNumber : blockNumbers) {
            setBlockUsed(blockNumber);
            diskDrive.setBitmapBit(blockNumber, true); // Mark as used in the DiskDrive bitmap
        }
        // Add the file to the FAT
        fileSystem.addFileToFAT(simFileName, blockNumbers.get(0), blockNumbers.size(), -1);
        System.out.println("File " + simFileName + " copied to simulation.");
        System.out.println("Starting block: " + blockNumbers.get(0));
    }

    /**
     * Deletes a file from the disk by traversing and freeing its chain of blocks.
     * This method works by iterating through the chained blocks using their pointers,
     * freeing each block in the process. The FAT entry is removed separately in FileSystem.deleteFile.
     * 
     * @param fileEntry The FATEntry object representing the file to be deleted.
     */
    public void deleteFile(FileSystem.FATEntry fileEntry) {
        int currentBlock = fileEntry.startingBlock;
        while (currentBlock != -1) {
            // Get the pointer to the next block before freeing the current block
            int nextBlock = getNextBlock(currentBlock);
            // Free the current block
            freeBlock(currentBlock);
            // Move to the next block in the chain
            currentBlock = nextBlock;
        }
        // Note: The file entry is removed from the FAT in FileSystem.deleteFile
    }

    /**
     * Retrieves the pointer to the next block in the chain for a given block.
     * This public method serves as an interface to the private getNextBlockPointer method,
     * allowing other classes to access the next block pointer.
     * 
     * @param currentBlock The block number whose next block pointer is to be retrieved.
     * @return The block number of the next block in the chain, or an end-of-file marker if it's the last block.
     */
    public int getNextBlock(int currentBlock) {
        return getNextBlockPointer(currentBlock); // Calls the private method
    }

/////////////////////////////////////////////////////////////////
// Private Helper Methods
/////////////////////////////////////////////////////////////////

    /**
     * Sets the pointer in a block to point to the next block in the chain.
     * This method writes the pointer data to the end of the block's data on the disk.
     * 
     * @param currentBlock The block number where the pointer is to be set.
     * @param nextBlock    The block number to be set as the next block.
     */
    private void setNextBlockPointer(int currentBlock, int nextBlock) {
        byte[] blockData = diskDrive.readBlock(currentBlock);
        ByteBuffer buffer = ByteBuffer.allocate(4); // Allocate 4 bytes for an integer
        buffer.putInt(nextBlock); // Put the next block number into the buffer
        byte[] pointerData = buffer.array(); // Convert to byte array
        // Assume the last 4 bytes of the block are reserved for the pointer
        System.arraycopy(pointerData, 0, blockData, DiskDrive.BLOCK_SIZE - 4, 4);
        diskDrive.writeBlock(currentBlock, blockData); // Write back to the disk
    }

    /**
     * Retrieves the next block pointer from a block's data.
     * This method reads the pointer data from the end of the block's data on the disk.
     * 
     * @param blockNumber The block number whose next block pointer is to be retrieved.
     * @return The block number of the next block in the chain.
     */
    private int getNextBlockPointer(int blockNumber) {
        byte[] blockData = diskDrive.readBlock(blockNumber);
        ByteBuffer buffer = ByteBuffer.wrap(blockData, DiskDrive.BLOCK_SIZE - 4, 4);
        return buffer.getInt(); // Read the integer value of the next block pointer
    }

    /**
     * Finds a free block on the disk.
     * This method searches through the disk blocks in a shuffled order
     * to find a block that is free (not in use).
     * 
     * @return The block number of a free block, or -1 if no free block is found.
     */
    private int findFreeBlock() {
        int size = DiskDrive.DISK_SIZE;
        // Create a list of block indices starting from after the reserved blocks
        List<Integer> blockIndices = IntStream.range(2, size)
                                            .boxed()
                                            .collect(Collectors.toList());
        // Shuffle the list to introduce randomness
        Collections.shuffle(blockIndices);
        // Iterate through the shuffled list to find a free block
        for (int index : blockIndices) {
            if (isBlockFree(index)) {
                return index;
            }
        }
        return -1; // No free block found
    }

    /**
     * Frees a block on the disk, marking it as available in the bitmap and clearing its data.
     * 
     * @param blockNumber The block number to be freed.
     */
    private void freeBlock(int blockNumber) {
        // Mark the block as free in the bitmap
        setBlockFree(blockNumber);
        // Optionally clear the block data
        diskDrive.writeBlock(blockNumber, new byte[DiskDrive.BLOCK_SIZE]);
        // Update the bitmap in the disk drive
        diskDrive.setBitmapBit(blockNumber, false);
    }

    /**
     * Checks if a specific block on the disk is free.
     * 
     * @param blockNumber The block number to check.
     * @return True if the block is free, false otherwise.
     */
    protected boolean isBlockFree(int blockNumber) {
        return bitmap[blockNumber];
    }

    /**
     * Marks a specific block on the disk as used or free.
     * 
     * @param blockNumber The block number to be marked.
     */
    protected void setBlockUsed(int blockNumber) {
        bitmap[blockNumber] = false;
    }

    protected void setBlockFree(int blockNumber) {
        if (blockNumber < 0 || blockNumber >= DiskDrive.DISK_SIZE) {
            throw new IllegalArgumentException("Block number is out of bounds");
        }
        bitmap[blockNumber] = true;
    }
}
