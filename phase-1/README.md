# FNF Karting Arena

Simple Java console application for Phase 1 karting bookings and race
scheduling.

## Run

Open PowerShell in the `phase-1` folder and run:

```powershell
New-Item -ItemType Directory -Force out
javac -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object FullName)
java -cp out console.Main
```

Admin login:

```text
Username: admin
Password: admin
```

The application supports customer registration, bookings, race batches,
cancellation, race history, and earnings.
