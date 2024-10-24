/**
 * Disk Allocation Simulator
 * -------------------------
 * Author: Danny
 * Class: CS 4348.001
 * Project: Disk Allocation Simulator
 *
 * This program simulates different disk allocation strategies including
 * contiguous, chained, and indexed allocation. It allows users to interact with
 * a simulated file system through a console menu.
 */
import java.util.Scanner;

public class Project3 {

    private FileSystem fileSystem;

    public Project3(String allocationMethod) {
        // Initialize the file system with the specified allocation method
        this.fileSystem = new FileSystem(allocationMethod);
    }

    public void displayMenu() {
        Scanner scanner = new Scanner(System.in);
        int choice;
        
        do {
            System.out.println("\nDisk Allocation Menu:");
            System.out.println("1) Display a file");
            System.out.println("2) Display the file table");
            System.out.println("3) Display the free space bitmap");
            System.out.println("4) Display a disk block");
            System.out.println("5) Copy a file from the simulation to a file on the real system");
            System.out.println("6) Copy a file from the real system to a file in the simulation");
            System.out.println("7) Delete a file");
            System.out.println("8) Exit");
            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();
            System.out.println();
            System.out.println("Choice: " + choice);  // Print the chosen option
            scanner.nextLine(); // Consume the newline character
            switch (choice) {
                case 1:
                    // Display a file
                    System.out.print("Enter the name of the file to display: ");
                    String fileName = scanner.nextLine();
                    fileSystem.displayFile(fileName);
                    break;
                case 2:
                    // Display the file table
                    fileSystem.displayFAT(0);
                    break;
                case 3:
                    // Display the free space bitmap
                    fileSystem.displayFreeSpaceBitmap(1);
                    break;
                case 4:
                    // Display a disk block
                    System.out.print("Enter the block number to display: ");
                    int blockNumber = scanner.nextInt();
                    fileSystem.displayDiskBlock(blockNumber);
                    break;
                case 5:
                    // Copy a file from the simulation to a file on the real system
                    System.out.print("Copy from: ");
                    String simFileName = scanner.nextLine();
                    System.out.print("Copy to: ");
                    String realFilePath = scanner.nextLine();
                    fileSystem.copyFileToRealSystem(simFileName, realFilePath);
                    break;
                case 6:
                    // Copy a file from the real system to a file in the simulation
                    System.out.print("Copy from: ");
                    String realPath = scanner.nextLine();
                    System.out.print("Copy to: ");
                    String simFile = scanner.nextLine();
                    fileSystem.copyFileToSimulation(realPath, simFile);
                    break;
                case 7:
                    // Delete a file
                    System.out.print("File name to delete: ");
                    String fileToDelete = scanner.nextLine();
                    fileSystem.deleteFile(fileToDelete);
                    break;
                case 8:
                    // Exit the program
                    System.out.println("Exiting...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 8);
        
        scanner.close();
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Project3 [contiguous|chained|indexed]");
            return;
        }
        String allocationMethod = args[0].toLowerCase();
        if (!allocationMethod.equals("contiguous") && !allocationMethod.equals("chained") && !allocationMethod.equals("indexed")) {
            System.out.println("Invalid allocation method. Please choose from: contiguous, chained, indexed.");
            return;
        }
        Project3 ui = new Project3(allocationMethod);
        ui.displayMenu();
    }
}
