package testing

import (
	"testing"

	"a.yandex-team.ru/infra/hostctl/internal/template"
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"github.com/stretchr/testify/assert"
)

func TestSpecParser(t *testing.T, parser template.SpecParser) {
	specBuf := []byte(`spec:
  files:
    - path: /mock
      content: mock
  packages:
    - name: mock
      version: mock
`)
	doc := template.NewDocument(nil, specBuf, "mock", "mock.yaml")
	mm := &template.MaterializedMeta{Name: "mock", Kind: "PackageSet"}
	st, err := parser.ParseSpec(doc, mm)
	assert.NoError(t, err)
	ctx := template.NewMaterializedCtx(map[string]string{})
	ms, err := st.Materialize(ctx)
	assert.NoError(t, err)
	assert.IsType(t, &pb.PackageSetSpec{}, ms)

	mm = &template.MaterializedMeta{Name: "mock", Kind: "SystemService"}
	st, err = parser.ParseSpec(doc, mm)
	assert.NoError(t, err)
	ms, err = st.Materialize(ctx)
	assert.NoError(t, err)
	assert.IsType(t, &pb.SystemServiceSpec{}, ms)

	mm = &template.MaterializedMeta{Name: "mock", Kind: "TimerJob"}
	st, err = parser.ParseSpec(doc, mm)
	assert.NoError(t, err)
	ms, err = st.Materialize(ctx)
	assert.NoError(t, err)
	assert.IsType(t, &pb.TimerJobSpec{}, ms)

	mm = &template.MaterializedMeta{Name: "mock", Kind: "HostPod"}
	st, err = parser.ParseSpec(doc, mm)
	assert.NoError(t, err)
	ms, err = st.Materialize(ctx)
	assert.NoError(t, err)
	assert.IsType(t, &pb.HostPodSpec{}, ms)

	mm = &template.MaterializedMeta{Name: "mock", Kind: "HostPodFragment"}
	st, err = parser.ParseSpec(doc, mm)
	assert.NoError(t, err)
	ms, err = st.Materialize(ctx)
	assert.NoError(t, err)
	assert.IsType(t, &pb.HostPodSpec{}, ms)

	specBuf = []byte(`spec:
  files:
    - path: /mock
      content: mock
  packages:
    - name: mock
      version: mock
  properties:
    cmd:
      - /dev/null
`)
	doc = template.NewDocument(nil, specBuf, "mock", "mock.yaml")
	mm = &template.MaterializedMeta{Name: "mock", Kind: "PortoDaemon"}
	st, err = parser.ParseSpec(doc, mm)
	assert.NoError(t, err)
	ms, err = st.Materialize(ctx)
	assert.NoError(t, err)
	assert.IsType(t, &pb.PortoDaemon{}, ms)
}
