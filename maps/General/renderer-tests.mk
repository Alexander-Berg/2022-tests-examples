empty :=
space :=$(empty) $(empty)

ifneq (,$(YMR_NO_POSTGRES_TESTS))
NO_POSTGRES_TESTS = no
else
NO_POSTGRES_TESTS =
endif

AUXBIN += manual-test-checker

$(LIB): TESTS = \
    post_build_test=post_build_test.cpp \
    boost-tests=$(BOOST_TESTS_SRCS)

ifndef NO_POSTGRES_TESTS
$(LIB): TESTS += \
    boost-postgres-tests=$(BOOST_POSTGRES_TESTS_SRCS)
endif

ifdef YMR_BUILD_LOCAL
    $(LIB): TEST-INCLUDES = ../libs/geojson/include
    $(LIB): TEST-LIBS += ::../libs/geojson/libyandex-maps-renderer-geojson.so
    $(LIB): TEST-LDFLAGS = -L ../libs/geojson
endif

$(LIB): TEST-INCLUDES += tests/boost-tests/include
$(LIB): TEST-LIBS += $(LOCAL_LIBS) $(COMMON_LIBS) yandex-maps-hotspots-base5 gmock yandex-maps-renderer-geojson
$(LIB): TEST-CXXFLAGS += -DBOOST_TEST_DYN_LINK
$(LIB): TEST-LDFLAGS += $(COMMON_LDFLAGS)

test:: manual-tests-run check-headers-run

#set tests order
boost-tests-run: post_build_test-run
ifndef NO_POSTGRES_TESTS
boost-postgres-tests-run: boost-tests-run
manual-tests-run:: boost-postgres-tests-run
else
manual-tests-run:: boost-tests-run
endif
check-headers-run:: manual-tests-run

clean-test:
	rm -f ./post_build_test;
	find ./tests/manual-tests/ -iname "screenshot_new.png" -exec rm {} \;
	find ./tests/manual-tests/ -iname "screenshot_new.svg" -exec rm {} \;
	find ./tests/manual-tests/ -iname "screenshot_diff.png" -exec rm {} \;
	find ./tests/manual-tests/ -iname "screenshot_svg-png_diff.png" -exec rm {} \;
	find ./tests/manual-tests/ -iname "screenshot_svg_diff.png" -exec rm {} \;
	find ./tests/manual-tests/ -iname "screenshot_svg_new.png" -exec rm {} \;
	rm -rf $(CHECK_HEADERS_DIR)


###############################################################################
#  Manual tests
manual-test-checker: SRCS = tests/manual-test-checker/manual-test-checker.cpp
manual-test-checker: CXXFLAGS += $(COMMON_CXXFLAGS)
manual-test-checker: INCLUDES = $(COMMON_INCLUDES)
manual-test-checker: LIBS = :$(LIB).so $(LOCAL_LIBS) $(COMMON_LIBS) boost_program_options
manual-test-checker: LDFLAGS = $(COMMON_LDFLAGS)

MANUAL_TESTS_FOLDERS = "labeler" "rasterizer"
ifndef NO_POSTGRES_TESTS
MANUAL_TESTS_FOLDERS += "postgres"
endif

manual-tests-run:: manual-test-checker
	r=0; \
	for d in $(MANUAL_TESTS_FOLDERS); \
	do \
		tests/run_manual_tests.sh tests/manual-tests/$$d; \
		if [ $$? -ne 0 ]; then r=1; fi; \
	done; \
	exit $$r;
###############################################################################


###############################################################################
#  Check headers
PUBLIC_HEADERS := $(shell find include/yandex/maps/renderer5/ *.h -type f | grep -v "_inl.h" | sort -n)

CHECK_HEADERS_DIR := check_headers
CHECK_HEADERS_SRCS = $(PUBLIC_HEADERS:%.h=$(CHECK_HEADERS_DIR)/%.h.cpp)
CHECK_HEADERS_ASMS = $(PUBLIC_HEADERS:%.h=$(CHECK_HEADERS_DIR)/%.h.s)

$(CHECK_HEADERS_DIR)/%.h.cpp:
	@mkdir -p $(dir $@); \
	echo "#include \"$(basename $(subst $(CHECK_HEADERS_DIR)/,$(empty),$@))\"" > $@;

$(CHECK_HEADERS_DIR)/%.h.s: $(CHECK_HEADERS_DIR)/%.h.cpp
	@echo check header: $(basename $(subst $(CHECK_HEADERS_DIR)/,$(empty),$@)) && \
	$(CXX) -c $(basename $@).cpp -o $@ -S -I../ -I../include $(CXXFLAGS) $(COMMON_CXXFLAGS) -DYANDEX_MAPS_BUILD $(addprefix -I, $(COMMON_INCLUDES));

check-headers-run:: $(CHECK_HEADERS_SRCS) $(CHECK_HEADERS_ASMS)
###############################################################################

BOOST_TESTS_VCPROJ = tests/boost-tests/boost-tests.vcxproj
BOOST_TESTS_SRCS = $(subst $(space),:,$(shell $(SCRIPTS_DIR)/extract_sources.sh $(BOOST_TESTS_VCPROJ) $(shell dirname $(BOOST_TESTS_VCPROJ))))

BOOST_POSTGRES_TESTS_VCPROJ = tests/boost-tests/boost-tests-postgres.vcxproj
BOOST_POSTGRES_TESTS_SRCS = $(subst $(space),:,$(shell $(SCRIPTS_DIR)/extract_sources.sh $(BOOST_POSTGRES_TESTS_VCPROJ) $(shell dirname $(BOOST_POSTGRES_TESTS_VCPROJ))))
###############################################################################
