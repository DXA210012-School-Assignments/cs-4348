/**
 * CS4348 - Operating Systems Concepts
 * Project 2: HotelSimulator 
 * 
 * The HotelSimulator class serves as the main driver for simulating a hotel environment.
 * It creates and manages various entities such as guests, front desk employees, and bellhops.
 * It also manages the semaphores used for synchronization between the various entities.
 * 
 * @author Danny Amezquita
 */

import java.util.concurrent.Semaphore;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Random;

public class HotelSimulator {
    // Initialize the number of employees and guests
    private static final int NUM_EMPLOYEES = 2;
    private static final int MAX_GUESTS = 25;
    
    // Queues to hold guests and bellhops
    public static Queue<Guest> guestQ; // Queue for guests waiting for service
    public static Queue<Guest> bellhopQ; // Queue for guests needing bellhop assistance
    
    // Mutual Exclusion Semaphores for the threads
    public static Semaphore guestMutex; // Mutex for guest queue
    public static Semaphore frontDeskMutex; // Mutex for front desk operations
    public static Semaphore bellhopMutex; // Mutex for bellhop operations
    
    // Semaphores for various hotel operations
    public static Semaphore guestAvailable; // Signals when a guest is available
    public static Semaphore bellhopRequested; // Signals when a bellhop is requested
    public static Semaphore bellhopEmployee; // Semaphore for available bellhop employee
    public static Semaphore frontDeskEmployee; // Semaphore for available front desk employee
    public static Semaphore leaveFrontDesk; // Signals when to leave the front desk
    public static Semaphore bagsTaken; // Signals when bags are taken by bellhop
    public static Semaphore bagsReceived; // Signals when bags are received by guest
    public static Semaphore roomAssigned; // Signals when a room is assigned
    public static Semaphore allGuestsInRooms; // Signals when all guests are in their rooms
    
    // Arrays for room and guest completion semaphores
    public static Semaphore[] roomReached, guestDone;
    // Arrays for sorting IDs of front desk / bellhop employees
    public static int[] deskIDs, bellhopIDs;
    
    //Constructor to initialize semaphores, queues, and IDs
    public HotelSimulator() { 
        initializeSemaphores();
        initializeQueues();
        initializeIDs();
    }

    // Initialize all semaphores used in the simulation
    private void initializeSemaphores() {
        guestAvailable = new Semaphore(0, true);
        bellhopRequested = new Semaphore(0, true);
        leaveFrontDesk = new Semaphore(0, true);
        bagsTaken = new Semaphore(0, true);
        bagsReceived = new Semaphore(0, true);
        guestMutex = new Semaphore(1, true);
        frontDeskMutex = new Semaphore(1, true);
        bellhopMutex = new Semaphore(1, true);
        roomAssigned = new Semaphore(0, true);
        frontDeskEmployee = new Semaphore(2, true);
        bellhopEmployee = new Semaphore(2, true);
        allGuestsInRooms = new Semaphore(0, true);
        
        guestDone = new Semaphore[MAX_GUESTS];
        roomReached = new Semaphore[MAX_GUESTS];
        
        for (int i = 0; i < MAX_GUESTS; i++) {
            guestDone[i] = new Semaphore(0, true);
            roomReached[i] = new Semaphore(0, true);
        }
    }

    // Initialize all queues used in the simulation
    private void initializeQueues() {
        guestQ = new LinkedList<Guest>();
        bellhopQ = new LinkedList<Guest>();
    }

    // Initialize arrays for storing employee IDs
    private void initializeIDs() {
        deskIDs = new int[MAX_GUESTS];
        bellhopIDs = new int[MAX_GUESTS];
        for (int i = 0; i < MAX_GUESTS; i++) {
            deskIDs[i] = 0;
            bellhopIDs[i] = 0;
        }
    }

    // Main method to run the simulation
    public static void main(String[] args) {
        HotelSimulator hotel = new HotelSimulator();
        System.out.println("Simulation starts"); 
        createEmployees(hotel);
        createGuests(hotel);
        // Wait for all guests to reach their rooms and retire for the evening
        waitForAllGuests(hotel); 
        System.out.println("Simulation ends"); 
        System.exit(0); // Exit the program
    }

    // Method to create hotel employees
    private static void createEmployees(HotelSimulator hotel) {
        // Create front desk employees
        for (int i = 0; i < NUM_EMPLOYEES; i++) { 
            new FrontDesk(i, hotel);
        }
        // Create bellhop employees
        for (int i = 0; i < NUM_EMPLOYEES; i++) { 
            new Bellhop(i, hotel);
        }
    }

    // Method to create hotel guests
    private static void createGuests(HotelSimulator hotel) {
        // Create guests for the hotel
        for (int i = 0; i < MAX_GUESTS; i++) { // create 25 guests 
            new Guest(i, hotel);
        }
    }

    // Method to wait for all guests to retire for the evening
    private static void waitForAllGuests(HotelSimulator hotel) {
        try {
            // Acquire semaphores for each guest, ensuring all are in their rooms and retired for the evening
            for (int i = 0; i < MAX_GUESTS; i++) {  
                HotelSimulator.allGuestsInRooms.acquire();
            }
        } catch (InterruptedException e) {
            e.printStackTrace(); // Handle any interruption exceptions
        }
    }
}

//////////////////////////////////////////////

// Guest class implementing Runnable interface for multi-threading
class Guest implements Runnable {
    // Reference to the HotelSimulator Object
    public HotelSimulator hotel; 
    //Unique identifier for each guest
    public int id; 
    // Room Number assigned to the guest
    public int roomNum; 
    // Number of bags the guest has
    public int numBags; 
    // Thread for each guest
    public Thread guestThread; 

    // Constructor to initialize guest properties
    public Guest(int ID, HotelSimulator hotel) {
        this.id = ID;
        this.numBags = new Random().nextInt(6);
        this.guestThread = new Thread(this);
        guestThread.setDaemon(true);  // Set the thread as a daemon thread
        guestThread.start();
        System.out.println("Guest " + id + " created");
    }

    // Main logic for the guest's actions
    @Override
    public void run() {
        try {
            enterHotel();
            checkIn();
            goToRoom();
            retireForEvening();
        } catch (Exception e) {
            e.printStackTrace();
        }
        joinGuestThread();
    }

    // Guest enters the hotel
    private void enterHotel() {
        System.out.println("Guest " + id + " enters the hotel with " + numBags + " bags");
    }

    // Guest checks in at the front desk
    private void checkIn() throws InterruptedException {
        // Acquire mutex to make sure exclusive access to the guest queue
        HotelSimulator.guestMutex.acquire();
        // Add this guest to the queue
        HotelSimulator.guestQ.add(this);
        // Release mutex to allow other guests to be added to the queue
        HotelSimulator.guestMutex.release();
        // Guest waits for front desk employee to be available
        HotelSimulator.frontDeskEmployee.acquire();
        // Guest signals that they are available for check in
        HotelSimulator.guestAvailable.release();
        // Guest waits for front desk employee to register them and assign a room
        HotelSimulator.guestDone[id].acquire();
        // Guest waits for room to be assigned
        HotelSimulator.roomAssigned.acquire();
        // Guest receives room key from front desk employee
        System.out.println("Guest " + id + " receives room key for room " + roomNum
                + " from front desk employee " + HotelSimulator.deskIDs[id]);
        HotelSimulator.leaveFrontDesk.release();
    }

    // Guest goes to their room
    private void goToRoom() throws InterruptedException {
        if (numBags > 2) {
            requestBellhop();
        } else {
            enterRoom();
        }
    }

    // Guest requests a bellhop for bags if they have more than 2 bags
    private void requestBellhop() throws InterruptedException {
        // Acquire a bellhop to assist this guest
        HotelSimulator.bellhopEmployee.acquire();
        // Log that the guest has requested a bellhop
        System.out.println("Guest " + id + " requests help with bags");
        // Acquire mutex to make sure exclusive access to the bellhop queue
        HotelSimulator.bellhopMutex.acquire();
        // Add this guest to the bellhop queue
        HotelSimulator.bellhopQ.add(this);
        // Release mutex after adding the guest to the queue
        HotelSimulator.bellhopMutex.release();
        // Signal that a bellhop is requested
        HotelSimulator.bellhopRequested.release();
        // Enter the room after the bellhop receives bags from guest
        enterRoom();
        // Wait for the bellhop to deliver bags to guest
        HotelSimulator.bagsReceived.acquire();
        // Guest gives tip to bellhop
        System.out.println("Guest " + id + " receives bags from bellhop "
                + HotelSimulator.bellhopIDs[id] + " and gives tip");
    }

    // Guest enters their room
    private void enterRoom() throws InterruptedException {
        HotelSimulator.roomReached[id].release();
        System.out.println("Guest " + id + " enters room " + roomNum);
    }

    private void retireForEvening() {
        System.out.println("Guest " + id + " retires for the evening");
        HotelSimulator.allGuestsInRooms.release();
    }

    // Join the guest thread to make sure it completes its execution
    private void joinGuestThread() {
        try {
            System.out.println("Guest " + id + " joined");
            guestThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

//////////////////////////////////////////////

// FrontDesk class represents a front desk employee in the hotel
class FrontDesk implements Runnable{
    public int id; // Employee ID
    public static int roomNum = 0; // Room number counter

    // Constructor to initialize a front desk employee
    public FrontDesk(int id, HotelSimulator hotel) {
        this.id = id;
        // Create a new thread for this front desk employee
        Thread frontDeskThread = new Thread(this);
        // Set the thread as a daemon thread
        frontDeskThread.setDaemon(true);  
        // Start the thread
        frontDeskThread.start();
        // Log that the front desk employee has been created
        System.out.println("Front desk employee " + id + " created");
    }

    // Main run method for the front desk employee thread
    @Override
    public void run() {
        try {
            while (true) {
                waitForGuest(); // Wait for a guest to arrive
                registerGuest(); // Register the guest and assign a room
                freeFrontDesk(); // Free the front desk employee for the next guest
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Wait for a guest to arrive and be available for check in
    private void waitForGuest() throws InterruptedException {
        HotelSimulator.guestAvailable.acquire();
    }

    // Register the guest and assign a room
    private void registerGuest() throws InterruptedException {
        // Acquire mutex to make sure exclusive access to the guest queue
        HotelSimulator.frontDeskMutex.acquire();
        // Remove the next guest from the queue
        Guest guest = HotelSimulator.guestQ.remove();
        if (guest != null) {{
            // Assign a room to the guest
            assignRoom(guest);
            // Log the registration of the guest and the room assignment
            System.out.println("Front desk employee " + id +
                " registers guest " + guest.id +
                " and assigns room " + guest.roomNum);
            // Signal that the guest if done with registration
            HotelSimulator.guestDone[guest.id].release();
        }
        // Release the mutex
        HotelSimulator.frontDeskMutex.release();
    }
}

    // Assign a room to the guest
    private void assignRoom(Guest guest) {
        roomNum++; // Increment the room number counter
        guest.roomNum = roomNum; // Assign the room number to the guest
        HotelSimulator.deskIDs[guest.id] = id; // Record which employee assigned the room
        HotelSimulator.roomAssigned.release(); // Signal that the room has been assigned
    }

    // Free the front desk employee for the next guest
    private void freeFrontDesk() throws InterruptedException {
        HotelSimulator.leaveFrontDesk.acquire(); // Wait for the guest to leave the front desk
        HotelSimulator.frontDeskEmployee.release(); // Release the front desk employee semaphore
    }
}

//////////////////////////////////////////////

// Bellhop class represents a bellhop employee in the hotel
class Bellhop implements Runnable {
    public int id; // Bellhop ID
    private Thread bellhopThread; // Thread for this bellhop

    // Constructor to initialize a bellhop employee
    public Bellhop(int id, HotelSimulator hotel) {
        this.id = id;
        // Create a new thread for this bellhop
        this.bellhopThread = new Thread(this);
        // Set the thread as a daemon thread
        bellhopThread.setDaemon(true);
        // Start the thread  
        bellhopThread.start();
        // Log that the bellhop has been created
        System.out.println("Bellhop " + id + " created");
    }

    // Main run method for the bellhop thread
    @Override
    public void run() {
        try {
            while (true) {
                waitForRequest(); // Wait for a guest to request assistance
                assistGuest(); // Assist the guest with their bags
                freeBellhop(); // Free the bellhop employee for the next guest
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Wait for a guest to request bellhop assistance
    private void waitForRequest() throws InterruptedException {
        HotelSimulator.bellhopRequested.acquire();
    }

    // Assist the guest with their bags
    private void assistGuest() throws InterruptedException {
        // Acquire mutex to make sure exclusive access to the bellhop queue
        HotelSimulator.bellhopMutex.acquire();
        // Remove the next guest from the queue
        Guest guest = HotelSimulator.bellhopQ.remove();
        // Record which bellhop assisted the guest
        HotelSimulator.bellhopIDs[guest.id] = id;
        // Release the mutex
        HotelSimulator.bellhopMutex.release();
        if (guest != null) {
            // Log that the bellhop has received bags from the guest
            System.out.println("Bellhop " + id + " receives bags from guest " + guest.id);
        }
        // Signal that the bellhop has received the bags
        HotelSimulator.bagsReceived.release();
        // Deliver the bags to the guest
        deliverBags(guest);
    }

    // Deliver the bags to the guest
    private void deliverBags(Guest guest) throws InterruptedException {
        // Wait for the guest to reach their room
        HotelSimulator.roomReached[guest.id].acquire();
        // Log that the bellhop has delivered the bags to the guest
        System.out.println("Bellhop " + id + " delivers bags to guest " + guest.id);
        // Signal that the bags have been delivered
        HotelSimulator.bagsTaken.release();
    }

    // Free the bellhop employee for the next guest
    private void freeBellhop() {
        // Release the bellhop employee semaphore
        HotelSimulator.bellhopEmployee.release();
    }
}
