# FNF Karting Arena - Phase 2

This is the isolated application for Phase 2.

## Structure

- `src/` - Phase 2 application source code
- `test/` - Phase 2 tests

Phase 2 keeps the Phase 1 booking rules and adds safe activity from several
threads at the same time.

## Run

Open PowerShell in the repository root:

```powershell
New-Item -ItemType Directory -Force phase-2\out
javac -d phase-2\out (Get-ChildItem -Recurse -Filter *.java phase-2\src | ForEach-Object FullName)
java -cp phase-2\out console.Main
```

The application includes concurrent booking, background batch scheduling,
automatic race starts, waiting-token expiry, race locking, asynchronous
notifications, and developer test tools.

Admin login:

```text
Username: admin
Password: admin
```

## Concurrency test

```powershell
javac -cp phase-2\out -d phase-2\out (Get-ChildItem -Recurse -Filter *.java phase-2\test | ForEach-Object FullName)
java -cp phase-2\out ConcurrencyTest
```
