# Check cgroup leakage (excess or shortage).
#
# Best used in conjunction with 'qemu_kvm' recipe.
#
# Basically reads "/proc/cgroups" before and after all tests execution and
# compares numbers of each cgroup controller. If the difference is more than
# 'AllowedCgroupsExcess' or 'AllowedCgroupsShortage' (default is 100), recipe
# reports error with details about the leakage/shortage.
#
# Recipe is configurable via creation of files in "/home/qemu" (tests' default
# working directory, when using 'qemu_kvm' recipe) from the test itself:
#
# To disable all or excess/shortage checks, create:
# * "cgroups_checks.disable"
# * "cgroups_excess_check.disable"
# * "cgroups_shortage_check.disable"
#
# To configure 'AllowedCgroupsExcess' or 'AllowedCgroupsShortage', write desired
# number to:
# * "allowed_cgroups_excess.conf"
# * "allowed_cgroups_shortage.conf"

DEPENDS(infra/kernel/test/recipe/cgleak_check)
USE_RECIPE(infra/kernel/test/recipe/cgleak_check/cgleak_check)
