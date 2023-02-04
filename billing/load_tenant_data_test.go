package actions

import (
	"testing"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/manificenta/pkg/core"
	structures "a.yandex-team.ru/billing/hot/manificenta/pkg/core/manifest_structures"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type LoadTenantDataTestSuite struct {
	btesting.BaseSuite
}

func (s *LoadTenantDataTestSuite) TestRunSameTenant() {
	manifests := []core.Manifest{s.getTestManifest("bnpl", "common")}

	faasSettings := s.getTestFaasSettings("common")

	actual, err := LoadTenantData(manifests, faasSettings, "test", []string{"sas", "vla"})
	s.Require().NoError(err)

	expected := structures.TenantParams{
		"common": {
			"default": {"sas", "vla"},
		},
	}

	s.Assert().Equal(expected, actual)
}

func (s *LoadTenantDataTestSuite) TestRunWithNoTenants() {
	manifests := []core.Manifest{s.getTestManifest("bnpl", "common")}

	faasSettings := s.getTestFaasSettings("")

	actual, err := LoadTenantData(manifests, faasSettings, "test", []string{"sas", "vla"})
	s.Require().NoError(err)

	expected := structures.TenantParams{
		"common": {
			"default": {"sas", "vla"},
		},
	}

	s.Assert().Equal(expected, actual)
}

func (s *LoadTenantDataTestSuite) TestRunWithDefaultTenant() {
	manifests := []core.Manifest{s.getTestManifest("bnpl", "")}

	faasSettings := s.getTestFaasSettings("")

	actual, err := LoadTenantData(manifests, faasSettings, "test", []string{"sas", "vla"})
	s.Require().NoError(err)

	expected := structures.TenantParams{
		"bnpl": {
			"default": {"sas", "vla"},
		},
	}

	s.Assert().Equal(expected, actual)
}

func (s *LoadTenantDataTestSuite) TestRunWithDifferentTenants() {
	manifests := []core.Manifest{s.getTestManifest("bnpl", "bnpl-group")}

	faasSettings := s.getTestFaasSettings("common")

	actual, err := LoadTenantData(manifests, faasSettings, "test", []string{"sas", "vla"})
	s.Require().NoError(err)

	expected := structures.TenantParams{
		"bnpl-group": {
			"default": {"sas", "vla"},
		},
	}

	s.Assert().Equal(expected, actual)
}

func (s *LoadTenantDataTestSuite) getTestManifest(name, tenant string) core.Manifest {
	faasMap := map[string]any{
		"function": "super.long.endpoint.function",
		"peerdir":  "super/log/peerdir",
	}

	if tenant != "" {
		faasMap["tenants"] = []string{tenant}
	}

	return core.Manifest{
		Name: name, File: "/test/file",
		Data: map[string]any{
			"envs": map[any]any{
				"test": map[any]any{
					"faas": faasMap,
					"endpoints": map[string]any{
						"revenue": map[string]any{},
					},
				},
			},
		},
	}
}

func (s *LoadTenantDataTestSuite) getTestFaasSettings(tenantName string) core.FaasSettings {
	if tenantName != "" {
		return core.FaasSettings{
			"tenants": []map[string]any{
				{
					"name": tenantName,
					"instances": []map[string]any{
						{
							"name": "default",
							"dcs": []map[string]any{
								{"name": "sas", "amount": 2},
								{"name": "vla", "amount": 1},
							},
						},
					},
				},
			},
		}
	} else {
		return core.FaasSettings{
			"tenants": []map[string]any{},
		}
	}
}

func TestLoadTenantDataTestSuite(t *testing.T) {
	suite.Run(t, new(LoadTenantDataTestSuite))
}
