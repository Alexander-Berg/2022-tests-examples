package tests

import (
	"testing"

	"a.yandex-team.ru/infra/hostctl/internal/template"
	ttesting "a.yandex-team.ru/infra/hostctl/internal/template/testing"
)

func TestSimpleCtxCompiler(t *testing.T) {
	c := template.NewSimpleCtxCompiler()
	ttesting.TestCtxCompilerCompile(t, c)
}
