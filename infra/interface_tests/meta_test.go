package tests

import (
	"testing"

	"a.yandex-team.ru/infra/hostctl/internal/template"
	ttesting "a.yandex-team.ru/infra/hostctl/internal/template/testing"
)

func TestParseMetaDocument(t *testing.T) {
	ttesting.TestMetaParserFunc(t, template.ParseMetaDocument)
}
