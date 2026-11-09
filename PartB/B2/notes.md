# Part B: Adversarial inputs

## B2 summary
The unsafe login demo concatenates the user input directly into SQL. Another user can write an input such as `admin' --` so that the rest of the predicate is commented out.

The safe version uses `PreparedStatement` and placeholders to separate data from SQL syntax. That prevents quote tricks and the boolean tautology `OR 1=1` from being accepted.

The bus-pass validation also checks invalid field counts, empty lines, and non-numeric amounts and rejects them early.
