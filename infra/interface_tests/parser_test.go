package tests

import (
	"testing"

	"a.yandex-team.ru/infra/hostctl/internal/template"
	ttesting "a.yandex-team.ru/infra/hostctl/internal/template/testing"
)

func TestDefaultCtxParserParse(t *testing.T) {
	ttesting.TestCtxParserParseDocument(t, template.ParseCtxDocument)
}
