# Part B: JDBC batch and measurement notes

## B3 summary
The JDBC benchmark compares three approaches:
- `Statement`: simplest but vulnerable to SQL injection and slowest for large imports.
- `PreparedStatement`: safer and better for repetitive inserts.
- `PreparedStatement` with `addBatch` and `executeBatch`: fastest for large imports when used with `rewriteBatchedStatements=true`.

The project stores the raw benchmark results in `PartB/B3/results.csv` and reproduces the expected transaction rollback behavior when an invalid amount appears mid-batch.
