# Part B: Migration and metadata notes

## B4 summary
This task migrates text records into the database, writing a failure report for bad lines. It also prints metadata for the target DB and driver to confirm compatibility before production deployment.

This is the correct data-engineering approach for a small-to-medium migration because it validates each record before insertion, keeps the import atomic, and preserves a human-readable reject log.
