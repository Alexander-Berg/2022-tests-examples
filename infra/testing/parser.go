package testing

import (
	"testing"

	"a.yandex-team.ru/infra/hostctl/internal/template"
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"github.com/stretchr/testify/assert"
)

func TestCtxParserParseDocument(t *testing.T, f template.CtxParserFunc) {
	doc := template.NewDocument(nil, nil, "mock", "/mock.yaml")
	def, err := f(doc)
	assert.NoError(t, err)
	assert.NotNil(t, def)
	assert.NotNil(t, def.Vars)
	ctxBuf := []byte(`format_values: True
vars:
  - name: v1
    match:
      - exp: "default()"
        val: "default"
`)
	doc = template.NewDocument(ctxBuf, nil, "mock", "/mock.yaml")
	def, err = f(doc)
	assert.NoError(t, err)
	expected := &template.CtxDefinition{
		Vars: []*pb.Context_Var{
			{
				Name: "v1",
				Match: []*pb.Context_Match{
					{
						Exp: "default()",
						Val: "default",
					},
				},
			},
		},
		FormatValues: "True",
	}
	assert.Equal(t, expected, def)

	ctxBuf = []byte("{malformed yaml")
	doc = template.NewDocument(ctxBuf, nil, "mock", "/mock.yaml")
	def, err = f(doc)
	assert.Error(t, err)
	assert.Nil(t, def)
}
