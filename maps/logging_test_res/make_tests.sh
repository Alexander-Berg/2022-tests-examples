#!/bin/bash

test_suffix=.test.txt

{
    echo "#include \"logging_test.h\""
    echo "#include \"logging_test_res/res.h\""
    echo
    echo "using namespace yandexs::maps::mapkit::guidance::test;"
    echo
    echo "BOOST_AUTO_TEST_SUITE(LoggingEventsAutoTests)"
    echo
}  > ../logging_test.cpp

> res.h

cd sources

for filename in *.pb; do
    test_name=${filename%.*}
    test_file=$test_name$test_suffix

    if [ ! -f $test_file ]; then
        echo "Corresponding $test_file file was not found for $filename, skipping test."
        continue
    fi

    xxd -i $filename ../${test_name}.pb.h

    expected_var_name=$(echo $test_file | sed -e 's/\./_/g')
    {
         echo "const char * ${expected_var_name} = R\"*8e74fd7f6e64*("
         cat ${test_file}
         echo ")*8e74fd7f6e64*\";"
    } > ../${test_file}.h

    {
       echo "#include \"${test_name}.pb.h\""
       echo "#include \"${test_file}.h\""
    } >> ../res.h

    echo "ADD_TEST($test_name)" >> ../../logging_test.cpp
done

cd ../

{
    echo
    echo "BOOST_AUTO_TEST_SUITE_END()"
} >> ../logging_test.cpp
