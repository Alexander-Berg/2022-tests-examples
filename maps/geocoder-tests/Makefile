PKG := yandex-maps-geocoder-tests

DESC = Geocoder data tests for release testing by garden.

SRCPKG := $(PKG)

DEPENDS = yandex-maps-user

tests_data_dir=/usr/share/yandex/maps/geocoder/tests

$(PKG): FILES := \
	$(tests_data_dir)/coverage:testsets/1_*.json \
	$(tests_data_dir)/critical:testsets/2_*.json

$(PKG)-postinst::
	chown maps:maps $(tests_data_dir)/*

update-data:
	./create_2_testset.sh
	./create_1_testset.sh

clean::
	rm -rf *.pyc tmp/* debug.config.json testsets.report.log genfiles

include /usr/share/yandex/maps/build/build.mk
