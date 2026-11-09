# CSA09-192425250-Assignment5
Student: Yashaswini, Register No 192425250

## Java version
java version "27" 2026-09-15

## MySQL version
MySQL Community Server 8.0.46

## Setup
1. Start the local MySQL server on port 3307 using the project copy of MySQL.
2. Create the database and populate schema:
   mysql -uroot --protocol=TCP -P3307 -e "CREATE DATABASE IF NOT EXISTS pass_test;"
   mysql -uroot --protocol=TCP -P3307 pass_test < PartB/B3/schema.sql
3. Copy db.properties.example to db.properties and adjust credentials if needed.

## Tasks
- PartA/A1: record layout, pseudocode, file-store design.
- PartA/A2: buffered I/O and complexity notes.
- PartA/A3: DAO design and maintenance comparison.
- PartA/A4: serialization and compatibility notes.
- PartA/A5: transaction and injection safety notes.
- PartB/B1: bus-pass file store and benchmark driver.
- PartB/B2: adversarial input demo.
- PartB/B3: JDBC statement/batch benchmark and loader.
- PartB/B4: migration and metadata validation.

## Result notes
- B1, B2, B3, and B4 all include run outputs, measurement CSVs, and explanation notes.
- All outputs are saved under the task folders in output.txt and results.csv where applicable.
