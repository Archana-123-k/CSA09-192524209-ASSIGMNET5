# Assignment 5: Data Persistence Engineering, Files and Databases

Student: Yashaswini
Register No: 192425250

## Environment
- Java: 27 (Temurin/OpenJDK)
- MySQL: 8.0.46 Community Server
- Local DB used: `pass_test` on port 3307

## Part A: Design and pseudocode
Files created under:
- [PartA/A1/notes.md](PartA/A1/notes.md)
- [PartA/A2/notes.md](PartA/A2/notes.md)
- [PartA/A3/notes.md](PartA/A3/notes.md)
- [PartA/A4/notes.md](PartA/A4/notes.md)
- [PartA/A5/notes.md](PartA/A5/notes.md)

These cover:
- bus-pass text and binary record layouts
- SIMATS-style pseudocode for add/find/list operations
- corruption and crash recovery design
- buffered I/O predictions and redesign
- DAO abstraction and mapping
- serialization compatibility and transient fields
- transaction-safe wallet transfer and SQL injection prevention

## Part B: Implementation and experiments
Implemented code under:
- [PartB/B1/source/PassStore.java](PartB/B1/source/PassStore.java)
- [PartB/B2/source/UnsafeLoginDemo.java](PartB/B2/source/UnsafeLoginDemo.java)
- [PartB/B3/source/PassLoader.java](PartB/B3/source/PassLoader.java)
- [PartB/B4/source/MigrationProgram.java](PartB/B4/source/MigrationProgram.java)

Supporting notes:
- [PartB/B1/notes.md](PartB/B1/notes.md)
- [PartB/B2/notes.md](PartB/B2/notes.md)
- [PartB/B3/notes.md](PartB/B3/notes.md)
- [PartB/B4/notes.md](PartB/B4/notes.md)

Outputs and measurements:
- [PartB/B1/output.txt](PartB/B1/output.txt)
- [PartB/B1/results.csv](PartB/B1/results.csv)
- [PartB/B1/find_results.csv](PartB/B1/find_results.csv)
- [PartB/B2/output.txt](PartB/B2/output.txt)
- [PartB/B3/output.txt](PartB/B3/output.txt)
- [PartB/B3/results.csv](PartB/B3/results.csv)
- [PartB/B4/output.txt](PartB/B4/output.txt)
- [PartB/B4/migration_input.txt](PartB/B4/migration_input.txt)

## Predicted vs observed key results
### A2.1 prediction
For a 50 MB file, one-byte reads imply roughly 52,428,800 read calls.
Observed in the program output:
- unbuffered 50 MB count: 52,428,800 reads
- buffered 50 MB count: 6,400 reads
This matches the expected difference between byte-by-byte I/O and 8 KB buffered chunks.

### B1 timing result summary
| Test | Result |
| --- | --- |
| unbuffered 1 MB | ~3130 ms average |
| buffered 1 MB | ~6 ms average |
| unbuffered 10 MB | ~29515 ms average |
| buffered 10 MB | ~20 ms average |
| unbuffered 50 MB | ~557795 ms average |
| buffered 50 MB | ~94 ms average |
| find by ID 1000 records | ~3 ms |
| find by ID 100000 records | ~19 ms |

This confirms the expected O(n) sequential scan trend, with buffered input reducing OS overhead dramatically.

### B3 JDBC benchmark summary
| Approach | 1,000 rows | 10,000 rows | 100,000 rows |
| --- | ---: | ---: | ---: |
| Statement | ~2364 ms | ~23544 ms | ~207342 ms |
| PreparedStatement | ~2161 ms | ~21630 ms | ~221341 ms |
| PreparedStatement + batch | ~42 ms | ~221 ms | ~1336 ms |

Batch insert with transaction rollback on invalid data preserved correctness: the invalid batch caused a rollback and row count stayed at 0.

## Security and validation evidence
- Unsafe SQL injection demonstration was run and captured in [PartB/B2/output.txt](PartB/B2/output.txt).
- The unsafe query accepted an invalid login attempt, while the PreparedStatement version rejected it.
- The validation for damaged passages rejects bad field counts, empty lines, and non-numeric amounts.

## Migration and metadata evidence
The migration program wrote valid rows and rejected invalid rows, then printed database metadata and tables in [PartB/B4/output.txt](PartB/B4/output.txt). This demonstrates compatibility checks before moving the application to another server.

## Run steps
From the repo root:

1. Start MySQL server locally on port 3307.
2. Create the database:
   mysql -uroot --protocol=TCP -P3307 -e "CREATE DATABASE IF NOT EXISTS pass_test;"
3. Compile and run each task:
   javac -cp "lib/mysql-connector-j-9.1.0.jar" PartB/B2/source/UnsafeLoginDemo.java -d PartB/B2
   java -cp "PartB/B2;lib/mysql-connector-j-9.1.0.jar" UnsafeLoginDemo

   javac -cp "lib/mysql-connector-j-9.1.0.jar" PartB/B3/source/PassLoader.java -d PartB/B3
   java -cp "PartB/B3;lib/mysql-connector-j-9.1.0.jar" PassLoader

   javac -cp "lib/mysql-connector-j-9.1.0.jar" PartB/B4/source/MigrationProgram.java -d PartB/B4
   java -cp "PartB/B4;lib/mysql-connector-j-9.1.0.jar" MigrationProgram

   javac PartB/B1/source/PassStore.java -d PartB/B1
   java -cp PartB/B1 PassStore

## Completed work summary
- Part A design and pseudocode completed in all five sections.
- Part B Java implementations completed for B1, B2, B3, and B4.
- SQL schema and validation scripts saved in [PartB/B3/schema.sql](PartB/B3/schema.sql).
- Measurement data saved in CSV files under each task folder.
- Console outputs saved in output.txt files for evidence.
- Database and file store behavior validated with real runs and measured timings.
