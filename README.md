# File System Watcher

TCSS 360 — University of Washington Tacoma
Authors: Mariam Hussein & Nasra Hussein

## Overview
A desktop application that monitors a directory for file system events
(create, modify, delete), logs them to a SQLite database, lets the user
query the log by extension / date range / activity / path, export results
to CSV, and email the CSV report via Gmail.

## Requirements
- Java JDK 21 or newer (developed on OpenJDK 25)
- IntelliJ IDEA (project includes .iml files)
- The following libraries on the classpath (in /lib or via your IDE):
  - sqlite-jdbc (SQLite JDBC driver)
  - jakarta.mail and its dependencies (for the email feature)

## How to Run (IntelliJ)
1. Open the project folder in IntelliJ IDEA.
2. Confirm the SQLite JDBC and Jakarta Mail JARs are added as libraries
   (File > Project Structure > Libraries). They are in the /lib folder.
3. Set the project SDK to JDK 21+ (File > Project Structure > Project).
4. Locate Main.java (package filewatcher) and run its main() method.
5. The File System Watcher window opens.

## How to Use
1. Enter a directory path to monitor and select a file extension
   (or "All Files").
2. Click Start to begin monitoring. File events appear in the list in
   real time.
3. Click Write to DB to save the captured events to the database.
4. Click Stop to stop monitoring.
5. Open Database > Query to search the log. Run any of the four query
   types, view results in the table, Export to CSV, or Email the CSV.

## Email Setup (required for the email feature only)
The email feature reads credentials from a config.properties file in the
project working directory. Create config.properties with:

    email=youraddress@gmail.com
    password=your-16-char-gmail-app-password

Use a Gmail App Password (Google Account > Security > App Passwords), not
your normal account password. The file is excluded from version control.

## How to Run the Tests

The project uses JUnit 5.

1. In IntelliJ, right-click the test source folder and choose

   "Run All Tests", OR run each test class individually:
   - DatabaseHandlerTest (24 tests)
   - QueryEngineTest (30 tests)
   - QueryFormTest (5 tests)
   - FileEventTest (7 tests)
   - FileWatcherTest(4 tests)
   - ReportGeneratorTest(11 tests)
   - EmailServiceTest

2. All model/controller tests should pass.
Note: When tests run, the SQLite driver prints JDK warnings about
"restricted method" / "native access". These are harmless and do not
affect test results.


## Project Structure
- Main.java — application entry point
- MainForm.java / QueryForm.java / AboutDialog.java — Swing GUI (View)
- QueryEngine.java — service/controller layer
- FileWatcher.java — directory monitoring
- DatabaseHandler.java — SQLite persistence
- ReportGenerator.java — CSV formatting/export
- EmailService.java — Gmail SMTP delivery
- FileEvent.java — data model
- *Test.java — JUnit 5 test suites




--First Iteration--

Team Members:
- Mariam Hussein (mah01@uw.edu)
- Nasra Hussein (nasraali@uw.edu)

Nasra Hussein: For this first iteration, my biggest challenge was setting up the GitHub repository and getting everyone connected 
as collaborators also took some time. Once the environment was ready, 
I focused on implementing the FileEvent data model class. Understanding 
why the timestamp field needed to be LocalDateTime instead of a String 
was a good learning moment it matters for database queries and date 
comparisons later. Writing JUnit tests for FileEvent was new to me but 
helped me catch issues like checking the wrong expected values in 
assertEquals.

Mariam Hussein: For this iteration, I was responsible for setting up the 
YouTrack project for our team. I created the project, added team members, 
configured the agile board, and created the initial user stories for our 
File System Watcher. I shared what I learned about YouTrack with my 
teammate so we could both use it effectively. I also implemented the filewatcher class and ReportGenarator class. 




--Second Iteration--
Team Members:
- Mariam Hussein (mah01@uw.edu)
- Nasra Hussein (nasraali@uw.edu)

  IDE Used: IntelliJ IDEA


Nasra Hussein: This week marked significant progress on the File System Watcher project, with 22.5 hours invested in mastering SQL and implementing the DatabaseHandler component. Starting with SQL fundamentals through W3Schools tutorials, Nasra learned SELECT, INSERT, UPDATE, DELETE, WHERE, BETWEEN, and LIKE queries, then set up SQLite in IntelliJ to practice hands-on. The core achievement was completing DatabaseHandler (FSW-4), a full-featured SQLite repository class with 11 methods: openConnection(), closeConnection(), createTableIfNotExists(), saveEvent(), saveEvents(), and five query methods (fetchAll, fetchByExtension, fetchByDateRange, fetchByActivity, fetchByPath). Each method uses proper JDBC patterns, Statement vs PreparedStatement, try catch error handling, try-with-resources, and ResultSet iteration, and includes full Javadoc comments. To deepen understanding. All code implements SRS requirements FR-2.1 through FR-2.7 and is ready for GitHub commit. Next steps include fixing one minor bug in fetchByPath, writing JUnit tests, and implementing the QueryEngine orchestration layer.


Mariam Hussein: During this iteration,I implemented MainForm.java: built the main application window including layout, menu structure, and navigation components. I also  implemented AboutDialog.java: created the modal dialog window that displays
application information, with proper open/close behavior tied to the menu.
I learned and began integrating SQL: studied database fundamentals and set up
initial database connectivity to support future data-driven features. No major blocking issues were encountered with the UI components.







--Third Iteration--

Team Members:
- Mariam Hussein (mah01@uw.edu)
- Nasra Hussein (nasraali@uw.edu)

  IDE Used: IntelliJ IDEA


  Nasra Hussein: QueryEngine: Implemented the QueryEngine orchestration layer that coordinates DatabaseHandler, ReportGenerator, and EmailService. Added four query methods: queryByExtension(), queryByDateRange(), queryByActivity(), and queryByPath(), each caching results and metadata for subsequent export or email operations. Implemented input validation with IllegalArgumentException for null or blank parameters, date range validation ensuring start date is before end date, and automatic wrapping of path search terms in SQL LIKE wildcards. Added saveResultsToCsv() for CSV export with automatic .csv extension appending, emailResults() for sending reports, and clearDatabase() which also resets the cache. Implements FR-3.1 through FR-3.8, FR-4.1 through FR-4.8, and FR-5.1 through FR-5.8.
JUnit Tests:
Wrote comprehensive JUnit 5 tests for DatabaseHandler (22 tests) and QueryEngine (27 tests). DatabaseHandler tests cover connection management, table creation called twice without error, single and batch event saving with field verification, all four fetch methods with match and no-match scenarios, clear database, and edge cases including null extensions and special characters in file names. QueryEngine tests cover all four query methods with match and no-match cases, input validation for null, blank, and invalid date ranges, result caching behavior across multiple queries, CSV export including file creation and automatic extension appending, database clearing with cache reset, and email error handling when EmailService is null. Each test uses a fresh temporary database deleted after completion to ensure isolation.


  Mariam Hussein:This iteration I focused on writing JUnit 5 unit tests for two classes  ReportGenerator and FileWatcher.
  The ReportGenerator class has three methods: buildHeader, formatToTable, and exportToCsv. For buildHeader, four tests were written to       verify that the query type, query parameter, and export date label all appear in the output, and that the method still works correctly      when empty strings are passed in as an edge case. For formatToTable, three tests were written to confirm that an empty list returns an      empty string, one event produces one line, and multiple events produce the correct number of lines. For exportToCsv, four tests were        written to verify that the file is actually created on disk, the first line is the correct CSV header, one event produces a header plus     one data row, and an empty list produces only the header. This gave a total of 11 tests for ReportGenerator.


  The FileWatcher class has four public methods: getPendingEvents, hasUnsavedEvents, stopMonitoring, and startMonitoring. A test was          written for each method. For getPendingEvents and hasUnsavedEvents, the tests confirm that a brand new FileWatcher starts with an empty     list and returns false for unsaved events. For stopMonitoring, the test verifies that calling it does not throw an exception. For           startMonitoring, the test uses a real temporary directory, starts the watcher on a background thread, creates a .txt file, and confirms     that the event was detected and added to the pending events list. This gave a total of 4 tests for FileWatcher.



  
--Fourth Iteration--

Team Members:
- Mariam Hussein (mah01@uw.edu)
- Nasra Hussein (nasraali@uw.edu)

  IDE Used: IntelliJ IDEA

  Nasra Hussein:
* Built QueryForm.java: the database query GUI window using Java Swing with a JTabbedPane layout containing four tabs for querying by extension, date range, activity type, and path. Includes a results JTable with non-editable model, and action buttons for Export to CSV, Email Results, Clear Database, and Return to Main. All event handlers validate user input and show error dialogs. Full Javadoc on every class, field, and method with SRS traceability references (FR-3.1 through FR-5.x, Section 4.1.2).
* Wrote QueryFormTest.java: JUnit 5 test class with 5 tests covering constructor null rejection, valid construction, window title verification, visibility after construction, and safe dispose. Uses @BeforeEach with a real DatabaseHandler and test data, and @AfterEach with window disposal and temp database cleanup.



  Mariam Hussein: Created EmailService.java : handles Gmail SMTP authentication and email delivery, including CSV file attachment, subject/body generation, and graceful error handling for missing credentials or network failures.
Created EmailServiceTest.java : JUnit 5 test class covering constructor behavior, input validation (null/empty/blank recipient, null/missing attachment), and graceful failure when no credentials are configured.
Updated MainForm.java — added email send button and wired it to EmailService
Updated Main.java — added SQLite driver loading on startup

  
   --Fifth Iteration--

Team Members:
- Mariam Hussein (mah01@uw.edu)
- Nasra Hussein (nasraali@uw.edu)

  IDE Used: IntelliJ IDEA
  Nasra Hussein:
This week I focused on improving the usability of the QueryForm interface. I added descriptive tooltips to every control; combo boxes, spinners, text fields, and buttons, using setToolTipText() so users can understand each control's purpose at a glance. I implemented keyboard shortcuts on all action buttons (Alt+R to Run Query, Alt+E to Export, Alt+M to Email, Alt+C to Clear, and Alt+B to go Back), along with tab-switching shortcuts (Alt+1 through Alt+4) so the form can be operated quickly without a mouse. To make actions more recognizable, I added Unicode icons to the buttons (▶ Run Query, 📄 Export, ✉ Email, 🗑 Clear, ↩ Return). I also enabled column sorting so that clicking any column header sorts the results table (setAutoCreateRowSorter(true)), and added a status bar at the bottom of the form that displays the result count (e.g. "Found 12 result(s)") to give users immediate feedback instead of relying solely on a popup dialog. Note that opening QueryForm depends on MainForm being wired up to call openQueryForm(), which is handled separately by the MainForm owner.



  Mariam Hussein:
  1. BUG FIX: FileWatcher.java — CPU Busy-Wait.
The startMonitoring() loop was calling watcher.poll() with no delay, causing the background thread to spin at 100% CPU while waiting for file events. Fixed by replacing poll() with poll(500, TimeUnit.MILLISECONDS) so the thread sleeps betweenchecks. Also added InterruptedException handling to allow the thread to shut down cleanly when interrupted. Added a new clearPendingEvents() method to properly encapsulate clearing the pending events list rather than exposing the list directly.
 
  2. BUG FIX: ReportGenerator.java — FileWriter Resource Leak.The exportToCsv() method created a FileWriter but did not use try-with resources, meaning the file handle could be left open if an exception occurred during writing. Fixed by wrapping theFileWriter in a try-with-resources block so it always closes correctly regardless of whether an exception is thrown.
 
  3. BUG FIX: EmailService.java — Null Input Validation.
The sendEmail() method was missing input validation before the try block. If a null or blank recipient, or a null/missing file was passed in, the method would throw an exception instead of returning false cleanly. Fixed by adding early return checks at the top of the method before any sending logic is attempted. This ensures all EmailServiceTest validation tests pass correctly.
 
4. ENHANCEMENT: MainForm.java — Integration and Polish
Updated MainForm to initialize a QueryEngine instance at startup using the existing DatabaseHandler, ReportGenerator, and EmailService. Wired the Database > Query menu item to open Nasra's QueryForm window (previously showed a "coming soon" placeholder). Replaced the direct call to fileWatcher.getPendingEvents().clear() with the new fileWatcher.clearPendingEvents() method for better encapsulation.


 --Sixth Iteration--

Team Members:
- Mariam Hussein (mah01@uw.edu)
- Nasra Hussein (nasraali@uw.edu)

IDE Used: IntelliJ IDEA

Nasra Hussein:
This week I focused on final test verification and the UML class diagram in
preparation for the project presentation.

Test Verification:
I ran the full JUnit 5 test suite for the components I am responsible for and
confirmed all tests pass with no failures or errors:
- DatabaseHandlerTest — 24 tests passed (543 ms). Covers saving single and batch
  events, fetch-by-extension, fetch-by-date-range, fetch-by-activity,
  fetch-by-path (match and no-match cases), clearing the database, connection
  open/close, special-character handling, and empty-database queries.
- QueryEngineTest — 30 tests passed (451 ms). Covers all four query types,
  result/metadata caching, input validation (null/blank/empty), CSV export,
  email-result validation, and clear-database behavior.
- QueryFormTest — 5 tests passed (1.878 s). Covers form construction, window
  title, visibility after construction, null-engine rejection, and clean
  disposal.
- FileEventTest — 7 tests passed (14 ms). Covers all getters, toString(), and
  getAsCsvRow().
Total: 66 tests passing across the four suites. I also confirmed that the red
"restricted method / native access" messages in the run output are benign JDK
warnings emitted when the SQLite JDBC driver loads, and do not affect test
results. Screenshots of each green run were captured for the presentation slides.

UML Class Diagram:
I authored the final UML class diagram in PlantUML, reflecting the final code
base. It includes all classes (Main, MainForm, QueryForm, AboutDialog,
QueryEngine, FileWatcher, DatabaseHandler, ReportGenerator, EmailService,
FileEvent) grouped into the View, Controller/Service, and Model layers to show
the MVC structure. Relationships use formal notation — composition for owned
objects (MainForm–FileWatcher, FileWatcher–FileEvent), aggregation for the
injected services (QueryEngine–DatabaseHandler/ReportGenerator/EmailService),
and dependencies for transient uses. Both team members' names appear in the
diagram title and footer. The diagram was rendered and exported for inclusion
in the slide deck.
  
