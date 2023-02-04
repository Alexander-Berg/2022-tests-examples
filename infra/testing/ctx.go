package testing

import (
	"testing"

	"a.yandex-team.ru/infra/hostctl/internal/template"
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"github.com/stretchr/testify/assert"
)

func TestCompiledCtxMaterialize(t *testing.T, c template.CompiledCtx, hi *pb.HostInfo, expectedVars map[string]string) {
	mCtx, err := c.Materialize(hi)
	assert.NoError(t, err)
	assert.NotNil(t, mCtx)
	vars := mCtx.Values()
	for k, v := range expectedVars {
		assert.Equal(t, v, vars[k])
	}
}
