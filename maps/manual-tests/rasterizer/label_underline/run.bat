set test_dir=%CD%
cd ../../ && call vars.bat && cd %test_dir% || cd %test_dir%
@echo on
::cd %tilerenderer_test_dir%
"%renderer_bin%" "%test_dir%\DynamicMap.xml" "%test_dir%\debug.position"
::cd %test_dir%
