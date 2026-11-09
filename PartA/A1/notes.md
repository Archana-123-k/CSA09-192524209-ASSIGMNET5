# Part A: Bus-pass file format

## A1.1 Record layout
### Delimited text layout
Record fields in order: `passId, regNo, name, routeNo, boardingStop, validityDate, amountPaid`.

- Delimiter: pipe `|` is chosen because route numbers and names are likely to include commas in names or addresses; quoting is still allowed for embedded delimiters.
- Name handling: `"O'Brien"` is stored as `"O''Brien"` or as CSV-style quoted field `"O'Brien"` with doubled embedded quotes. CSV quoting is more standard in Java and easy to validate.
- Example row: `P1001|192311001|"O'Brien"|7|MG Road|2026-12-31|3800.0`

### Binary layout
Binary record format using `DataOutputStream`:
1. `magic` header (4 bytes int, e.g. 0x42505301)
2. `recordVersion` (short/int)
3. `passId` as UTF (writeUTF)
4. `regNo` as UTF or fixed-length char array if a fixed-size layout is required
5. `name` as UTF
6. `routeNo` as int
7. `boardingStop` as UTF
8. `validityDate` as UTF or long millis
9. `amountPaid` as double
10. `recordLength` or checksum for validation

A fixed-length binary layout improves random access, but variable-length UTF fields complicate exact byte offsets. A magic header and checksum let us detect corruption after a crash or partial write.

## A1.2 Pseudocode: text design
### Add
START
  INPUT file, pass
  IF file does not exist THEN
    CREATE file
  ENDIF
  OPEN file with BufferedWriter over FileWriter in append mode
  WRITE pass fields joined with `|`
  FLUSH and CLOSE
  RETURN success
END

### Find by passId
START
  INPUT file, passId
  OPEN file with BufferedReader over FileReader
  WHILE line = readLine() != null DO
    SPLIT line by `|`
    IF field count is valid AND first field equals passId THEN
      RETURN record
    ENDIF
  ENDWHILE
  RETURN null
END

### List by route
START
  INPUT file, routeNo
  OPEN file with BufferedReader over FileReader
  WHILE line = readLine() != null DO
    SPLIT line by `|`
    IF field count is valid AND route field equals routeNo THEN
      PRINT record
    ENDIF
  ENDWHILE
END

Text files are chosen because the data is human-readable, easy to inspect, and the workload is append-heavy rather than random byte-level access. BufferedWriter and BufferedReader are the best fit for sequential reads and writes.

## A1.3 Damage handling and crash safety
- Damaged line: validate field count, parse numeric value, skip malformed rows and log them. Example: if a line has 4 fields or an invalid amount, mark it rejected.
- Missing file: catch `FileNotFoundException` or `NoSuchFileException`; create the file on first add.
- Power failure: write data to a temporary file, flush and fsync, and then atomically rename it over the target. Add a terminator or checksum and detect a truncated final line after a crash.

## Alternative: RandomAccessFile
Fixed-length records allow random access by computing a file offset: `seek(i * recordSize)`. This yields O(1) access by index with a sequential ID. However, it wastes space if IDs are not dense and requires padding for variable-length strings. If IDs are not sequential, an index map is required.

For a record with 7 fields and fixed field widths, the stored bytes are roughly:
- passId 12 bytes
- regNo 12 bytes
- name 30 bytes
- routeNo 4 bytes
- stop 20 bytes
- date 10 bytes
- amount 8 bytes
- padding 24 bytes
Total ≈ 120 bytes per record.

A linear scan (O(n)) is simpler and avoids padding complexity.
