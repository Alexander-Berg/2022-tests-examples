TEST_SOURCES = \
	renderertest.cpp

LIBDIRS += ../
# The next line prevents from using flags for tests from the global makefile
CXXFLAGS =

LD_LIBRARY_PATH = ../

LIBS += boost_unit_test_framework \
        yandex-maps-common \
        yandex-maps-renderer\
        yandex-maps-tilerenderer\
	yandex-maps-xml3\
	yandex-maps-pgpool2\
	yandex-maps-cgiutils2\
	pqxx \
	yandex-maps-wiki-renderer\
	xml2

INCLUDES += /usr/include/pqxx /usr/include/libxml2 /usr/include/agg2

OPTFLAGS = -O2 

include /usr/share/yandex/maps/makefiles/test/makefile.rules
