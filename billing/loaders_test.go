package structures

import (
	"testing"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/manificenta/pkg/core"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type LoaderTestSuite struct {
	btesting.BaseSuite
}

func (s *LoaderTestSuite) TestManifestsLoaderWithoutDefaults() {
	actual, err := LoadManifest(core.Manifest{
		Name: "bnpl", File: "/test/file", Data: map[string]any{
			"envs": map[any]any{
				"test": map[any]any{
					"faas": map[string]any{
						"tenants": []string{"common", "uncommon"},
					},
					"endpoints": map[string]any{
						"revenue": map[string]any{
							"faas": map[string]any{
								"function": "super.long.endpoint.function",
								"peerdir":  "super/log/peerdir",
								"instance": "default-1",
							},
						},
					},
				},
			},
		},
	}, "test")
	s.Require().NoError(err)

	expected := ManifestData{
		Faas: FaasSection{
			Tenants: []string{"common", "uncommon"},
		},
		Endpoints: map[string]Endpoint{
			"revenue": {
				Faas: FaasSection{
					Function: "super.long.endpoint.function",
					Peerdir:  "super/log/peerdir",
					Instance: "default-1",
				},
			},
		},
	}

	s.Assert().Equal(&expected, actual)
}

func (s *LoaderTestSuite) TestManifestsLoaderWithtDefaults() {
	actual, err := LoadManifest(core.Manifest{
		Name: "bnpl", File: "/test/file", Data: map[string]any{
			"envs": map[any]any{
				"test": map[any]any{
					"endpoints": map[string]any{
						"revenue": map[string]any{
							"faas": map[string]any{
								"function": "super.long.endpoint.function",
								"peerdir":  "super/log/peerdir",
							},
						},
					},
				},
			},
		},
	}, "test")
	s.Require().NoError(err)

	expected := ManifestData{
		Faas: FaasSection{
			Tenants: []string{"bnpl"},
		},
		Endpoints: map[string]Endpoint{
			"revenue": {
				Faas: FaasSection{
					Function: "super.long.endpoint.function",
					Peerdir:  "super/log/peerdir",
					Instance: "default",
				},
			},
		},
	}

	s.Assert().Equal(&expected, actual)
}

func (s *LoaderTestSuite) TestFaasSettingsLoader() {
	actual, err := LoadFaasSettings(core.FaasSettings{
		"tenants": []map[string]any{
			{
				"name": "test_tenant",
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
	})
	s.Require().NoError(err)

	expected := FaasSettings{
		Tenants: []Tenant{
			{
				Name: "test_tenant",
				Instances: []Instance{
					{
						Name: "default",
						DCs: []DCSettings{
							{Name: "sas", Amount: 2},
							{Name: "vla", Amount: 1},
						},
					},
				},
			},
		},
	}

	s.Assert().Equal(&expected, actual)
}

func TestLoaderTestSuite(t *testing.T) {
	suite.Run(t, new(LoaderTestSuite))
}
