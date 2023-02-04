:: usage:
:: cd tests_dir && vars.bat
::
@echo off
if defined renderer_tests_vars_bat (
  echo note: %0 allready called
  goto exit
) else (
  set renderer_tests_vars_bat=1
)
set configuration_name=Debug

set current_dir=%CD%
cd ../../
set root_dir=%CD%
cd %current_dir%

set tilerenderer_test_dir=%root_dir%\tilerenderer-test
set renderer_dir=%root_dir%\renderer-win32\renderer_test_gui
set renderer_bin=%renderer_dir%\%configuration_name%\renderer_test_gui.exe
set lib_dir=%root_dir%\renderer-win32\dll\%configuration_name%
set PATH=%lib_dir%;%PATH%

set PGHOST=localhost
set PGPORT=5432
set PGUSER=postgres
set PGPASSWORD=1111111
set PGDATABASE=autotest

SET PROGFILES=%ProgramFiles(x86)%
IF "%PROGFILES%" == "" SET PROGFILES = "%ProgramFiles%

set postgres_path=%PROGFILES%\PostgreSQL\8.4\bin\
:exit
