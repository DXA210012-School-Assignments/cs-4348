/**
 * The IndexedAllocation class represents a file allocation strategy using indexed allocation.
 * In this strategy, each file has an index block that contains pointers to all the other blocks in the file.
 * This class manages the allocation and deallocation of disk blocks for files,
 * working with the DiskDrive and FileSystem classes to simulate file storage.
 * 
 * Author: Danny Amezquita
 * Class: CS 4348.001
 */
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IndexedAllocation {
    // DiskDrive instance representing the disk on which files are stored
    protected DiskDrive diskDrive;
    // FileSystem instance for managing files
    protected FileSystem fileSystem;
    // Bitmap to track free and occupied blocks on the disk
    protected boolean[] bitmap;
    // Maximum file size in blocks that can be handled by this allocation strategy
    public static final int MAX_FILE_SIZE = 10;

    /**
     * Constructor for IndexedAllocation.
     * Initializes the allocation with a DiskDrive and a FileSystem,
     * and marks the first two blocks as reserved, assuming all other blocks are initially free.
     * 
     * @param diskDrive  The DiskDrive instance to interact with the disk.
     * @param fileSystem The FileSystem instance for file management.
     */
    public IndexedAllocation(DiskDrive diskDrive, FileSystem fileSystem) {
        this.diskDrive = diskDrive;
        this.fileSystem = fileSystem;
        this.bitmap = new boolean[DiskDrive.DISK_SIZE];
        // Initialization logic specific to the allocation strategy
        Arrays.fill(this.bitmap, true); // Assume all blocks are free initially
        this.bitmap[0] = this.bitmap[1] = false; // The first two blocks are reserved
    }

    /**
     * Copies a file into the simulation using indexed allocation.
     * This method first checks if the file is too large, then finds a block for the index,
     * splits the file data into blocks, writes each block to disk, and records their numbers in the index block.
     * It also updates the FAT (File Allocation Table) with information about the index block.
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
        int numberOfBlocks = (int) Math.ceil((double) fileData.length / DiskDrive.BLOCK_SIZE);
        int indexBlock = findFreeBlock(); // Find a block for the index
        if (indexBlock == -1 || numberOfBlocks >= DiskDrive.DISK_SIZE - 1) {
            System.out.println("Insufficient disk space.");
            return;
        }
        setBlockUsed(indexBlock);
        int[] indexBlockData = new int[numberOfBlocks];
        for (int i = 0; i < numberOfBlocks; i++) {
            int dataBlock = findFreeBlock();
            if (dataBlock == -1) {
                System.out.println("Insufficient disk space for data blocks.");
                return;
            }
            byte[] blockData = new byte[DiskDrive.BLOCK_SIZE]; // Initialize a new block data array
            int start = i * DiskDrive.BLOCK_SIZE;
            int end = Math.min(start + DiskDrive.BLOCK_SIZE, fileData.length);
            System.arraycopy(fileData, start, blockData, 0, end - start); // Copy data to blockData
            diskDrive.writeBlock(dataBlock, blockData); // Write the block to disk
            setBlockUsed(dataBlock);
            indexBlockData[i] = dataBlock; // Add the block number to the index block data
        }
        // Convert index block data to bytes and write to disk
        ByteBuffer buffer = ByteBuffer.allocate(DiskDrive.BLOCK_SIZE); // Allocate buffer of BLOCK_SIZE
        for (int block : indexBlockData) {
            buffer.putInt(block);
        }
        // Fill the rest of the buffer with zeros if it's not full
        while (buffer.position() < DiskDrive.BLOCK_SIZE) {
            buffer.put((byte) 0);
        }
        diskDrive.writeBlock(indexBlock, buffer.array());
        // Add the file to the FAT with index block info
        fileSystem.addFileToFAT(simFileName, -1, -1, indexBlock);
        System.out.println("File " + simFileName + " copied to simulation.");
        System.out.println("Index block: " + indexBlock);
    }

    /**
     * Deletes a file from the disk by freeing its blocks using the index block information.
     * This method reads the index block to get the numbers of the file's blocks and frees them.
     * Finally, it frees the index block itself. The FAT entry is removed separately in FileSystem.deleteFile.
     * 
     * @param fileEntry The FATEntry object representing the file to be deleted.
     */
    public void deleteFile(FileSystem.FATEntry fileEntry) {
        // First, get the index block number from the FAT entry
        int indexBlockNumber = fileEntry.indexBlock;
        // Read the index block data
        byte[] indexBlockData = diskDrive.readBlock(indexBlockNumber);
        ByteBuffer buffer = ByteBuffer.wrap(indexBlockData);
        // Loop through the block numbers stored in the index block
        while (buffer.hasRemaining()) {
            int dataBlockNumber = buffer.getInt();
            if (dataBlockNumber == 0) break; // Assuming 0 indicates the end of the block numbers
            // Free each data block
            setBlockFree(dataBlockNumber);
            diskDrive.writeBlock(dataBlockNumber, new byte[DiskDrive.BLOCK_SIZE]); // Optionally clear the block data
            diskDrive.setBitmapBit(dataBlockNumber, false); // Update the bitmap in the disk drive
        }
        // Finally, free the index block itself
        setBlockFree(indexBlockNumber);
        diskDrive.writeBlock(indexBlockNumber, new byte[DiskDrive.BLOCK_SIZE]); // Optionally clear the index block data
        diskDrive.setBitmapBit(indexBlockNumber, false); // Update the bitmap in the disk drive
    }

////////////////////////////////////////////////////////////////
// Private Helper Methods
////////////////////////////////////////////////////////////////
    /**
     * Searches for a free block on the disk.
     * The method creates a list of potential block indices (excluding reserved blocks),
     * shuffles them to introduce randomness, and iterates through the list to find a free block.
     * 
     * @return The index of a free block, or -1 if no free block is found.
     */
    private int findFreeBlock() {
        int size = DiskDrive.DISK_SIZE;
        // Create a list of block indices starting from after the reserved blocks
        List<Integer> blockIndices = IntStream.range(2, size)
                                            .boxed()
                                            .collect(Collectors.toList());
        Collections.shuffle(blockIndices); // Shuffle to introduce randomness
        // Iterate through the shuffled list to find a free block
        for (int index : blockIndices) {
            if (isBlockFree(index)) {
                return index;
            }
        }
        return -1; // No free block found
    }

    /**
     * Checks if a specific block on the disk is free.
     * 
     * @param blockNumber The block number to be checked.
     * @return True if the block is free, false otherwise.
     */
    protected boolean isBlockFree(int blockNumber) {
        return bitmap[blockNumber];
    }

    /**
     * Marks a specific block on the disk as used.
     * This method updates both the local bitmap and the bitmap in the DiskDrive.
     * 
     * @param blockNumber The block number to be marked as used.
     * @throws IllegalArgumentException if the block number is out of bounds.
     */
    protected void setBlockUsed(int blockNumber) {
        if (blockNumber < 0 || blockNumber >= DiskDrive.DISK_SIZE) {
            throw new IllegalArgumentException("Block number is out of bounds");
        }
        bitmap[blockNumber] = false;
        diskDrive.setBitmapBit(blockNumber, true); // Mark as used in the DiskDrive bitmap
    }

    /**
     * Marks a specific block on the disk as free.
     * 
     * @param blockNumber The block number to be marked as free.
     * @throws IllegalArgumentException if the block number is out of bounds.
     */
    protected void setBlockFree(int blockNumber) {
        if (blockNumber < 0 || blockNumber >= DiskDrive.DISK_SIZE) {
            throw new IllegalArgumentException("Block number is out of bounds");
        }
        bitmap[blockNumber] = true;
    }
}
