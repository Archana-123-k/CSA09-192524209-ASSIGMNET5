# Part B: Bus-pass file store and buffered line counter

## B1 summary
This task implements the text file store and benchmark for the bus-pass format. The key design decisions are:
- Use `|` as the delimiter to keep the record easy to parse and validate.
- Skip badly formed lines instead of aborting the entire search.
- Keep buffered readers for sequential scanning and buffered streams for line counting, because they do not force one system call per byte.

## Proof and measured behavior
The validation logic reads every line, checks the field count, and continues when the count is wrong.
A damaged line such as `BROKEN|FIELD|COUNT` does not stop the search, because the loop continues after the invalid field count check.

## Complexity note
- `findById` is O(n) in the number of records because it linearly scans the file.
- `HashMap` startup indexing would reduce repeated searches to O(n + q), but it uses memory proportional to record count.
