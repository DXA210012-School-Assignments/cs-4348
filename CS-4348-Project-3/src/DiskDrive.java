/**
 * The DiskDrive class simulates a disk drive for the Disk Allocation Simulation project.
 * It provides basic functionalities to read and write data blocks on the disk.
 * This class is used in conjunction with file system management classes like
 * FileSystem, ContiguousAllocation, ChainedAllocation, and IndexedAllocation.
 * 
 * Author: Danny
 * Class: CS 4348.001
 */
public class DiskDrive {
    // Constants representing the disk size and block size
    public static final int DISK_SIZE = 256; // Total number of blocks on the disk
    public static final int BLOCK_SIZE = 512; // Size of each block in bytes

    // The disk is represented as an array of byte arrays, simulating blocks
    private byte[][] blocks;
    // Bitmap for tracking used and free blocks on the disk
    private byte[] bitmap;

    /**
     * Constructor for DiskDrive.
     * Initializes the disk with empty blocks and sets up the bitmap.
     */
    public DiskDrive() {
        // Initialize the disk with empty blocks
        blocks = new byte[DISK_SIZE][BLOCK_SIZE];
        // Initialize the bitmap with one bit per block, 8 blocks per byte
        bitmap = new byte[DISK_SIZE / 8]; 
        // Initialize the bitmap: mark the first two blocks as used
        bitmap[0] = (byte) (bitmap[0] | 0b11000000); // Mark the first two bits as used
    }

    /**
     * Sets the usage status of a block in the bitmap.
     * 
     * @param blockNumber The block number to be updated in the bitmap.
     * @param isUsed      True if the block is used, false otherwise.
     */
    public void setBitmapBit(int blockNumber, boolean isUsed) {
        if (blockNumber < 0 || blockNumber >= DISK_SIZE) {
            throw new IllegalArgumentException("Block number is out of bounds");
        }
        int byteIndex = blockNumber / 8;
        int bitIndex = blockNumber % 8;
        if (isUsed) {
            // Set the corresponding bit to 1 for used block
            bitmap[byteIndex] = (byte) (bitmap[byteIndex] | (1 << (7 - bitIndex)));
        } else {
            // Set the corresponding bit to 0 for free block
            bitmap[byteIndex] = (byte) (bitmap[byteIndex] & ~(1 << (7 - bitIndex)));
        }
    }

    // Method to get the bitmap block
    public byte[] getBitmap() {
        return bitmap;
    }

    /**
     * Reads a block of data from the disk.
     * 
     * @param blockNumber The block number to read.
     * @return A byte array representing the data in the block.
     */
    public byte[] readBlock(int blockNumber) {
        // Uncomment the line below for debugging: to view the read block data
        // System.out.println("Reading from block " + blockNumber + ": " + Arrays.toString(blocks[blockNumber]));
        if (blockNumber < 0 || blockNumber >= DISK_SIZE) {
            throw new IllegalArgumentException("Block number is out of bounds");
        }
        return blocks[blockNumber];
    }

    /**
     * Writes a block of data to the disk.
     * 
     * @param blockNumber The block number where data is to be written.
     * @param data        The data to write in the block.
     */
    public void writeBlock(int blockNumber, byte[] data) {
        // Uncomment the line below for debugging: to view the written block data
        // System.out.println("Writing to block " + blockNumber + ": " + Arrays.toString(data));
        if (blockNumber < 0 || blockNumber >= DISK_SIZE) {
            throw new IllegalArgumentException("Block number is out of bounds");
        }
        blocks[blockNumber] = data;
    }
}

