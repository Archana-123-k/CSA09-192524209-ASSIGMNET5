# Part A: Don't read what you don't need

## A2.1 Prediction before benchmarking
A 50 MB file read one byte at a time means roughly 52,428,800 `read()` calls because 50 * 1024 * 1024 = 52,428,800 bytes.

Each byte read is a system call on many platforms, so the cost is dominated by context-switch and kernel-user transitions. The CPU can spend more time in the OS than in application logic.

## A2.2 Buffered redesign
### Buffered approach
START
  INPUT file
  count = 0
  OPEN file with BufferedInputStream or byte[] buffer
  WHILE read chunk not EOF DO
    FOR each byte in chunk DO
      IF byte == '\n' THEN
        count = count + 1
      ENDIF
    ENDFOR
  ENDWHILE
  RETURN count
END

With an 8 KB buffer, 50 MB / 8 KB ≈ 6,400 reads, which is far fewer than 52 million byte-wise reads.

## A2.3 When buffering helps little
Buffering gives little benefit when:
- the source is already a large block read operation,
- the code reads from a memory buffer already,
- the application performs random access or only one small chunk is read,
- the dataset is tiny enough that the overhead is negligible.

## Alternative: Files.readAllLines()
`Files.readAllLines()` reads the whole file into memory, so memory use is O(file size). It is convenient for small files but expensive for large files. Buffered reading keeps memory use near O(buffer size). In large file workloads, buffered sequential reads are usually the better engineering choice.
