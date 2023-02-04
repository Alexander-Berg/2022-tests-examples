package main

import (
	"a.yandex-team.ru/library/go/test/recipe"

	"a.yandex-team.ru/infra/diskmanager/tests/recipes/tmp_file/lib"
)

func main() {
	r := lib.Recipe{}
	recipe.Run(r)
}
