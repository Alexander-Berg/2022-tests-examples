package testing

import (
	"testing"

	"a.yandex-team.ru/infra/hostctl/internal/template"
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"a.yandex-team.ru/library/go/test/assertpb"
	"github.com/stretchr/testify/assert"
)

// TestMetaParserFunc checks if MetaParserFunc produces materializable MetaTemplate
func TestMetaParserFunc(t *testing.T, f template.MetaParserFunc) {
	specBuf := []byte(`meta:
  name: mock
  version: "{ver}"
  annotations:
    stage: "{stage}"
`)
	doc := template.NewDocument(nil, specBuf, "mock", "/mock.yaml")
	ctx := template.NewMaterializedCtx(map[string]string{"ver": "ver-mock", "stage": "stage-mock"})
	mt, err := f(doc)
	assert.NoError(t, err)
	assert.NotNil(t, mt)
	mm, err := mt.Materialize(ctx)
	assert.NoError(t, err)
	assert.NotNil(t, mm)
	expected := &template.MaterializedMeta{
		Name:        "mock",
		Version:     "ver-mock",
		Annotations: map[string]string{"stage": "stage-mock", "filename": "/mock.yaml", "repo": "mock"},
	}
	assertpb.Equal(t, (*pb.ObjectMeta)(expected), (*pb.ObjectMeta)(mm))
}
