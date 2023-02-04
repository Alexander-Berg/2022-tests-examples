PKG := yandex-maps-sh-test-harness
SRCPKG := $(PKG)

DESTDIR := /usr/lib/yandex/maps/sh-test-harness

FILES = $(DESTDIR)/run $(DESTDIR)/test-helpers

BUILD_DEPENDS += grep, sed, realpath
DEPENDS       += python, python-termcolor

DESC = A test harness for sh scripts.

include /usr/share/yandex/maps/build/build.mk
