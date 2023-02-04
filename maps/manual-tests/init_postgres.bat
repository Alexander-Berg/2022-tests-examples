SET PROGFILES=%ProgramFiles(x86)%
IF "%PROGFILES%" == "" SET PROGFILES = "%ProgramFiles%
SET PROGFILES=%ProgramFiles(x86)%
IF "%PROGFILES%" == "" SET PROGFILES = "%ProgramFiles%

set postgres_path=%PROGFILES%\PostgreSQL\8.4\bin\

set PGHOST=localhost
set PGDATABASE=autotest
set PGUSER=postgres
set PGPASSWD=1111111
set PGCLIENTENCODING=UTF8
"%postgres_path%\psql.exe" -f ..\sqlscripts\init.sql