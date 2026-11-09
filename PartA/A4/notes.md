# Part A: What should be serialized?

## A4.1 transients and object graphs
Serializable objects can include a graph of related objects. Example: `Hostel -> List<Room> -> List<Resident>`. All reachable objects must also be serializable; otherwise, `NotSerializableException` is thrown.

Transient fields are not written to the stream, so they restore to default values on deserialization (`null`, `0`, `false`). For example, a `Connection` should be marked `transient` because it is a live runtime handle and not a valid persisted value. A password is also a good candidate for `transient`, because storing it directly creates a security and privacy risk.

## A4.2 serialVersionUID compatibility
If a class adds a new field but keeps the same `serialVersionUID`, old serialized streams still load. The new field becomes `null` or `0` because it was absent.

If the class does not declare `serialVersionUID`, Java computes a hash based on class structure. A tiny change such as adding a field causes an `InvalidClassException` during deserialization.

This is exactly why production classes should declare an explicit `serialVersionUID` and treat any compatibility-breaking change as a schema migration. A sample throwaway demo can be kept in notes.md or a small test class to prove the behavior.

## Alternative: CSV or JSON
CSV and JSON are easier to inspect and port across languages. They usually consume more storage than a binary serialization for large homogeneous object graphs, but they are more resilient to human editing and forward/backward compatibility. For n residents, serialization usually has the lowest file size but the least human readability. JSON offers a flexible schema evolution story when new fields are added.
