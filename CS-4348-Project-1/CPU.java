import java.io.*;
import java.util.Scanner;
import java.util.Random;

public class CPU {
    // Declare CPU registers and other essential variables


    private int programCounter = 0, stackPointer = 1000, instructionRegister, accumulator, regX, regY, timer = 0, timerFlag;
    private boolean kernelMode;
    private Scanner memIn;
    private PrintWriter memOut;

    // Constructor to initialize CPU with memory I/O and timerFlag
    public CPU(Scanner memIn, PrintWriter memOut, int timerFlag) {
        this.memIn = memIn;
        this.memOut = memOut;
        this.timerFlag = timerFlag;
        kernelMode = false;
    }
    public static void main(String[] args) {
        // Validate command line arguments
        if (args.length < 2) {
            System.err.println("Error: In need of 2 arguments: File name and Time Out value.");
            System.exit(0);
        }

        // Parse the input program and timeout values
        String fileName = args[0];
        int myTimeout = Integer.parseInt(args[1]);

        // Validate timeout value
        try {
            if (myTimeout <= 0) {
                System.err.println("Error: Timeout value must be greater than zero.");
                System.exit(0);
            }
        } catch (NumberFormatException exception) {
            System.err.println("Error: Invalid timeout value.");
            System.exit(0);
        }

        // Create a new Memory process and start executing the program
        try {
            Runtime rt = Runtime.getRuntime(); // exec method is deprecated, use ProcessBuilder instead
            Process memory = rt.exec("java Memory" + fileName); // exec method is deprecated, use ProcessBuilder instead
                // replacement for rt.exec
            // ProcessBuilder rt = new ProcessBuilder("java", "Memory", fileName);
            //Process mem = builder.start();
            Scanner memIn = new Scanner(memory.getInputStream());
            PrintWriter memOut = new PrintWriter(memory.getOutputStream());
            CPU process = new CPU(memIn, memOut, myTimeout);
            process.executeProgram();
        } catch (IOException exception) {
            exception.printStackTrace(); // This will print the stack trace to standard error
            System.err.println("Error: Process creation failed.");
            System.exit(0);
        }

    }

    // Method to push value onto the stack
    private void pushToStack(int val) {
        writeToMem(--stackPointer, val);
    }

    // Method to pop value from the stack
    private int popFromStack() {
        return readFromMem(stackPointer++);
    }

    // Main method to execute the program
    public void executeProgram() {
        boolean running = true;
        while (running) {
            // Fetch the next instruction
            instructionRegister = readFromMem(programCounter++);
            // Execute the fetched instruction
            running = processInstruction();
            timer++; // Increment the timer
            // Check for  timeout
            if (timer >= timerFlag) {
                if (!kernelMode) {
                    timer = 0;
                    kernelMode();
                    programCounter = 1000;
                }
            }
        }
    }

    // Method to handle kernel mode operations
    private void kernelMode() {
        kernelMode = true;
        int tempStackPointer = stackPointer;
        stackPointer = 2000;
        pushToStack(tempStackPointer);
        pushToStack(programCounter);

    }

    // Method to read from memory
    private int readFromMem(int address) {
        if (!kernelMode && address >= 1000) {
            System.err.println("Memory violation: accessing system address " + address + " in user mode");
            System.exit(0);
        }
        memOut.println("R" + address);
        memOut.flush();
        return Integer.parseInt(memIn.nextLine());
    }

    // Method to write to memory
    private void writeToMem(int address, int val) {
        memOut.printf("W%d,%d\n", address, val);
        memOut.flush();
    }

    // Method to process the fetched instruction
    private boolean processInstruction() {
        switch (instructionRegister) {
        case 1: // Load immediate value into accumulator
            instructionRegister = readFromMem(programCounter++);
            accumulator = instructionRegister;
            break;
        case 2:  // Load value at specified address into accumulator
            instructionRegister = readFromMem(programCounter++);
            accumulator = readFromMem(instructionRegister);
            break;
        case 3:  // Load value from the address stored at specified address into accumulator
            instructionRegister = readFromMem(programCounter++);
            accumulator = readFromMem(readFromMem(instructionRegister));
            break;
        case 4:  // Load value from address obtained by adding regX to given address into accumulator
            instructionRegister = readFromMem(programCounter++);
            accumulator = readFromMem(instructionRegister + regX);
            break;
        case 5:  // Load value from address obtained by adding regY to given address into accumulator
            instructionRegister = readFromMem(programCounter++);
            accumulator = readFromMem(instructionRegister + regY);
            break;
        case 6:  // Load value from address obtained by adding stackPointer and regX into accumulator
            accumulator = readFromMem(stackPointer + regX);
            break;
        case 7:  // Store accumulator content to specified address
            instructionRegister = readFromMem(programCounter++);
            writeToMem(instructionRegister, accumulator);
            break;
        case 8:  // Load random integer between 1-100 into accumulator
            Random r = new Random();
            int rand = r.nextInt(100) + 1;
            accumulator = rand;
            break;
        case 9:  // Output accumulator content as int (port=1) or char (port=2)
            instructionRegister = readFromMem(programCounter++);
            if (instructionRegister == 1) {
                System.out.print(accumulator);
            } else if (instructionRegister == 2) {
                System.out.print((char) accumulator);
            }
            break;
        case 10:  // Add the value in regX to accumulator
            accumulator += regX;
            break;
        case 11:  // Add the value in regY to accumulator
            accumulator += regY;
            break;
        case 12:  // Subtract the value in regX from accumulator
            accumulator -= regX;
            break;
        case 13:  // Subtract the value in regY from accumulator
            accumulator -= regY;
            break;
        case 14:  // Copy value from accumulator to regX
            regX = accumulator;
            break;
        case 15:  // Copy value from regX to accumulator
            accumulator = regX;
            break;
        case 16:  // Copy value from accumulator to regY
            regY = accumulator;
            break;
        case 17:  // Copy value from regY to accumulator
            accumulator = regY;
            break;
        case 18:  // Copy value from accumulator to stackPointer
            stackPointer = accumulator;
            break;
            case 19:  // Copy value from stackPointer to accumulator
            accumulator = stackPointer;
            break;
        case 20:  // Jump to the address specified by the next value in memory
            instructionRegister = readFromMem(programCounter++);
            programCounter = instructionRegister;
            break;
        case 21:  // Jump to the address if accumulator is zero
            instructionRegister = readFromMem(programCounter++);
            if (accumulator == 0)
                programCounter = instructionRegister;
            break;
        case 22:  // Jump to the address if accumulator is not zero
            instructionRegister = readFromMem(programCounter++);
            if (accumulator != 0)
                programCounter = instructionRegister;
            break;
        case 23:  // Push the return address to the stack and jump to the given address
            instructionRegister = readFromMem(programCounter++);
            pushToStack(programCounter);
            programCounter = instructionRegister;
            break;
        case 24:  // Pop the return address from the stack and jump back to that address
            programCounter = popFromStack();
            break;
        case 25:  // Increment the value in regX
            regX++;
            break;
        case 26:  // Decrement the value in regX
            regX--;
            break;
        case 27:  // Push the value in the accumulator onto the stack
            pushToStack(accumulator);
            break;
        case 28:  // Pop the value from the stack into the accumulator
            accumulator = popFromStack();
            break;
        case 29: // If not in kernel mode, switch to kernel mode and set programCounter to 1500
            // Disable interrupts during interrupt processing
            if (!kernelMode) {
                kernelMode();
                programCounter = 1500;
            }
            break;
        case 30:  // Pop program counter and stack pointer from the stack, exit kernel mode
            programCounter = popFromStack();
            stackPointer = popFromStack();
            kernelMode = false;
            break;
        case 50: // Terminate the program
            System.exit(0);
            break;
        default: // Handle invalid instruction
            System.err.println("Error: Invalid instruction.");
            System.exit(0);
            break;
        }
        return true;
    }
}
