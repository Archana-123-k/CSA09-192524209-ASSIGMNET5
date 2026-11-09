# Part A: Correctness and safety

## A5.1 Wallet transfer pseudocode with transaction safety
START
  INPUT fromAccount, toAccount, amount
  CONNECTION = openDBConnection()
  SET autoCommit = false
  TRY
    IF fromAccount.balance < amount THEN
      RAISE error "insufficient funds"
    ENDIF
    UPDATE fromAccount SET balance = balance - amount
    UPDATE toAccount SET balance = balance + amount
    COMMIT
    RETURN success
  CATCH error
    ROLLBACK
    RETURN failure
  FINALLY
    SET autoCommit = true
    CLOSE connection
  ENDTRY
END

The critical part is that debit and credit happen in a single transaction. If any step fails, the database is rolled back to maintain integrity.

## A5.2 Injection example
Unsafe SQL:
`SELECT * FROM users WHERE id='" + id + "' AND pwd='" + pwd + "'"

Example attacker input:
- `id = admin' --`
- `pwd = ' OR '1'='1`

This yields a query equivalent to:
`SELECT * FROM users WHERE id='admin' --' AND pwd='...'`

The `--` comments out the rest of the predicate, allowing login bypass.

### Safe version
START
  PREPARE statement: SELECT * FROM users WHERE id = ? AND pwd = ?
  SET parameter 1 = id
  SET parameter 2 = pwd
  EXECUTE
  IF row found THEN
    RETURN authenticated
  ELSE
    RETURN rejected
  ENDIF
END

Prepared statements separate data from SQL syntax and prevent injection.

## Alternative: two auto-commit updates
Two separate updates without a transaction can leave the system in a broken state. Example: debit succeeds, credit fails, and the total money disappears. A transaction is the correct design for money movement because it preserves the invariant of correctness.
