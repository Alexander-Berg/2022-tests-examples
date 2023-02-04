package tests

import (
	"testing"

	"a.yandex-team.ru/infra/hostctl/internal/template"
	ttesting "a.yandex-team.ru/infra/hostctl/internal/template/testing"
)

func TestDefaultSpecParser(t *testing.T) {
	ttesting.TestSpecParser(t, template.NewDefaultSpecParser())
}
