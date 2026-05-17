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


  Nasra Hussein:


  Mariam Hussein:
  

 
