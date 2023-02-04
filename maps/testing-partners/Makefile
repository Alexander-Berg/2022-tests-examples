package-name := yandex-maps-analyzer-environment-testing-partners
PKG = $(package-name)
SRCPKG = $(package-name)

BUILD_DEPENDS := yandex-maps-build

$(package-name): ARCH = all
$(package-name): DESC = Analyzer environment name setter
$(package-name): DEPENDS = yandex-environment

env_path = /etc/yandex/

$(package-name): FILES = $(addprefix $(env_path), $(wildcard environment.name*))

include /usr/share/yandex/maps/build/build.mk
