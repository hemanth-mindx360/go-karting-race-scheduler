# FNF Karting Arena - Phase 2

This is the isolated application for Phase 2.

## Structure

- `src/` - Phase 2 application source code
- `test/` - Phase 2 tests

Phase 2 functionality will be developed on this branch and its future
sub-branches.

## Run

Open PowerShell in the repository root:

```powershell
New-Item -ItemType Directory -Force phase-2\out
javac -d phase-2\out (Get-ChildItem -Recurse -Filter *.java phase-2\src | ForEach-Object FullName)
java -cp phase-2\out console.Main
```

The application currently contains only the starting entry point. Feature
branches will add the Phase 2 booking, scheduling, and multithreading work.
