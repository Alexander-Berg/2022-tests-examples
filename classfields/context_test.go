package scheduler

import (
	"github.com/YandexClassifieds/shiva/pkg/secrets"
	"testing"

	"github.com/YandexClassifieds/shiva/common"
	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	manifest "github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/stretchr/testify/assert"
)

var (
	_ JobContext = &Context{}
	_ JobContext = &BatchContext{}
)

func TestMakeContext(t *testing.T) {
	s := &proto.ServiceMap{Name: "svc"}
	m := &manifest.Manifest{
		Name: "svc",
		Resources: manifest.Resources{
			CPU: 999,
		},
	}

	t.Run("BranchTest", func(t *testing.T) {
		ctx := MakeContext("1.0", "branch", m, s, 0, common.Test, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
		assert.Equal(t, minCpu, ctx.GetCpuLimit())
	})
	t.Run("BranchProd", func(t *testing.T) {
		ctx := MakeContext("1.0", "branch", m, s, 0, common.Prod, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
		assert.Equal(t, m.Resources.CPU, ctx.GetCpuLimit())
	})
	t.Run("NotBranchTest", func(t *testing.T) {
		ctx := MakeContext("1.0", "", m, s, 0, common.Test, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
		assert.Equal(t, m.Resources.CPU, ctx.GetCpuLimit())
	})
	t.Run("NotBranchProd", func(t *testing.T) {
		ctx := MakeContext("1.0", "", m, s, 0, common.Prod, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
		assert.Equal(t, m.Resources.CPU, ctx.GetCpuLimit())
	})
	t.Run("OverrideCpu", func(t *testing.T) {
		ctx := MakeContext("1.0", "", m, s, 0, common.Prod, nil, 1337, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
		assert.Equal(t, 1337, ctx.GetCpuLimit())
	})
}

func TestContext_GetDcList(t *testing.T) {
	s := &proto.ServiceMap{Name: "svc"}
	m := &manifest.Manifest{
		Name: "svc",
		DC: map[string]int{
			"sas": 10,
		},
	}

	ctx := MakeContext("1.0", "", m, s, 0, common.Test, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})

	assert.Equal(t, []string{"sas"}, ctx.GetDcList())
}

func TestContext_GetDcList_AnyDC(t *testing.T) {
	s := &proto.ServiceMap{Name: "svc"}
	m := &manifest.Manifest{
		Name: "svc",
		DC: map[string]int{
			"any": 1,
		},
	}

	ctx := MakeContext("1.0", "", m, s, 0, common.Test, nil, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})

	assert.Equal(t, []string{"sas", "vla"}, ctx.GetDcList())
}
