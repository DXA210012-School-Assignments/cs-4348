===========================
DISK ALLOCATION SIMULATOR
===========================

COMPILATION:
------------
To compile the project, navigate to the project directory in your terminal and run:
javac *.java
This command compiles all the .java files in the current directory.

RUNNING THE PROGRAM:
--------------------
To run the program, use the following command:
java Project3 [contiguous|chained|indexed]
Replace [contiguous|chained|indexed] with the desired file allocation strategy.

TEST FILES: MUST BE IN SAME DIRECTORY AS .java FILES
-----------
- 0empty.txt: An empty file for copy operations
- 1block.txt: Contains 3 characters, fitting in one block
- 2block.txt: First block 'a', second block 'b'
- 5block.txt: Blocks from 'a' to 'e'
- 9block.txt: Blocks from 'a' to 'i'
- 10block.txt: Maximum file size, blocks 'a' to 'j'
- 11block.txt: Exceeds maximum file size, should throw an error

ADDITIONAL NOTES:
-----------------
- The simulation handles file sizes up to 10 blocks. Files larger than this will trigger an error.
- The file system does not persist between runs; all data is reset when the program restarts.
- Error handling is implemented for common scenarios like file not found, disk full, and invalid block numbers.
- When copying files to the simulation, ensure the file names match the test files provided or are correctly specified.
