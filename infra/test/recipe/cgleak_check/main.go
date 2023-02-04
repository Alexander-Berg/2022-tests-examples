package main

import (
	"fmt"

	"a.yandex-team.ru/library/go/test/recipe"

	"a.yandex-team.ru/infra/kernel/test/recipe/cgleak_check/lib"
)

func main() {
	if lib.RemoteFileExists(lib.DisableCgroupsChecksPath) {
		lib.EnableCgroupsChecks = false
	}
	if lib.RemoteFileExists(lib.DisableCgroupsExcessCheckPath) {
		lib.EnableCgroupsExcessCheck = false
	}
	if lib.RemoteFileExists(lib.DisableCgroupsShortageCheckPath) {
		lib.EnableCgroupsShortageCheck = false
	}

	if lib.EnableCgroupsChecks {
		r := lib.Recipe{}
		recipe.Run(r)
	} else {
		fmt.Println("CGLEAK_CHECK: Cgroups checks are disabled")
	}
}
