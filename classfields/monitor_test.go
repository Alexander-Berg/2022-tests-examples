package nomad

import (
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler"
	spb "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/revert"
	mm "github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/hashicorp/nomad/api"
	"github.com/stretchr/testify/require"
)

func TestMonitor_ResolveRevertType(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	tests := []struct {
		name     string
		mapping  map[string]revert.RevertType
		expected revert.RevertType
		allocs   []string
	}{
		{"1 undef",
			map[string]revert.RevertType{"abc": revert.RevertType_Undefined},
			revert.RevertType_Undefined,
			[]string{"abc"}},
		{"2 undef",
			map[string]revert.RevertType{"abc": revert.RevertType_Undefined, "abc2": revert.RevertType_Undefined},
			revert.RevertType_Undefined,
			[]string{"abc", "abc2"}},
		{"1 unhealthy",
			map[string]revert.RevertType{"abc": revert.RevertType_Unhealthy},
			revert.RevertType_Unhealthy,
			[]string{"abc"}},
		{"2 unhealthy",
			map[string]revert.RevertType{"abc": revert.RevertType_Unhealthy, "abc2": revert.RevertType_Unhealthy},
			revert.RevertType_Unhealthy,
			[]string{"abc", "abc2"}},
		{"none+unhealthy",
			map[string]revert.RevertType{"abc": revert.RevertType_None, "abc2": revert.RevertType_Unhealthy},
			revert.RevertType_Unhealthy,
			[]string{"abc2"}},
		{"unhealthy+none",
			map[string]revert.RevertType{"abc": revert.RevertType_Unhealthy, "abc2": revert.RevertType_None},
			revert.RevertType_Unhealthy,
			[]string{"abc"}},
		{"none+undef",
			map[string]revert.RevertType{"abc": revert.RevertType_None, "abc2": revert.RevertType_Undefined},
			revert.RevertType_Undefined,
			[]string{"abc2"}},
		{"undef,none",
			map[string]revert.RevertType{"abc": revert.RevertType_Undefined, "abc2": revert.RevertType_None},
			revert.RevertType_Undefined,
			[]string{"abc"}},
		{"undef,term",
			map[string]revert.RevertType{"abc": revert.RevertType_Undefined, "abc2": revert.RevertType_Terminate},
			revert.RevertType_Terminate,
			[]string{"abc", "abc2"}},
		{"unhealth,oom",
			map[string]revert.RevertType{"abc": revert.RevertType_Unhealthy, "abc2": revert.RevertType_OOM},
			revert.RevertType_Undefined,
			[]string{"abc", "abc2"}},
		{"term,undef,term",
			map[string]revert.RevertType{
				"abc":  revert.RevertType_Terminate,
				"abc2": revert.RevertType_Undefined,
				"abc3": revert.RevertType_Terminate,
			},
			revert.RevertType_Terminate,
			[]string{"abc", "abc2", "abc3"}},
		{"term,undef,unhealthy",
			map[string]revert.RevertType{
				"abc":  revert.RevertType_Terminate,
				"abc2": revert.RevertType_Undefined,
				"abc3": revert.RevertType_Unhealthy,
			},
			revert.RevertType_Undefined,
			[]string{"abc", "abc2", "abc3"}},
		{"term,unhealthy,term",
			map[string]revert.RevertType{
				"abc":  revert.RevertType_Terminate,
				"abc2": revert.RevertType_Unhealthy,
				"abc3": revert.RevertType_Terminate,
			},
			revert.RevertType_Undefined,
			[]string{"abc", "abc2", "abc3"}},
		{"undef,unhealthy,unhealthy",
			map[string]revert.RevertType{
				"abc":  revert.RevertType_Undefined,
				"abc2": revert.RevertType_Unhealthy,
				"abc3": revert.RevertType_Unhealthy,
			},
			revert.RevertType_Unhealthy,
			[]string{"abc", "abc2", "abc3"}},
		{"undef,unhealthy,term",
			map[string]revert.RevertType{
				"abc":  revert.RevertType_Undefined,
				"abc2": revert.RevertType_Unhealthy,
				"abc3": revert.RevertType_Terminate,
			},
			revert.RevertType_Undefined,
			[]string{"abc", "abc2", "abc3"}},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			log := test.NewLogger(t)
			sc := &scheduler.Context{
				ServiceMap: &spb.ServiceMap{},
				Manifest:   &mm.Manifest{},
			}
			m := newMonitor(log, nil, nil, sc, "")
			revertType, allocs := m.detectTaskRevertType(&api.Deployment{}, tt.mapping)
			require.Equal(t, tt.expected, revertType, "unexpected revert type")
			require.ElementsMatch(t, tt.allocs, allocs, "unexpected allocations")
		})
	}
}
