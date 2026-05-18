# File System Watcher Project

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
  
   
