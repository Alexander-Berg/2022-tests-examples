package deploy2

import (
	"testing"

	"github.com/YandexClassifieds/shiva/pb/shiva/types/env"
	"github.com/YandexClassifieds/shiva/pkg/include/domain"
	"github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/require"
)

func TestEnvResolve(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	config := &model.Config{
		CommonParams: map[string]string{"common-override": "A", "generated": "A"},
		Files: []*domain.Include{{
			Path:     "vtail/obsolete.yml",
			Value:    map[string]string{"common-override": "B"},
			Revision: 10,
		}, {
			Path:     "vtail/a.yml",
			Value:    map[string]string{"from-fileA": "A", "common-override": "C"},
			Revision: 1,
		}},
		Params: map[string]string{"common-override": "D"},
		OverrideFiles: []*domain.Include{{
			Path:     "vtail/override.yml",
			Value:    map[string]string{"common-override": "E"},
			Revision: 12,
		}, {
			Path:     "vtail/b.yml",
			Value:    map[string]string{"from-fileB": "B"},
			Revision: 13,
		}},
		OverrideParams:  map[string]string{"common-override": "F"},
		GeneratedParams: map[string]string{"generated": "B"},
		SecretParams:    map[string]string{"k1": "${sec-1:ver-1:k1}"},
	}
	resolve, err := EnvResolve(&model.Manifest{
		Revision: 10,
		Config:   config,
	})
	require.NoError(t, err)
	expected := []*env.Env{
		{Source: env.EnvSource_GENERATED, Key: "generated", Value: "B", Link: "https://docs.yandex-team.ru/classifieds-infra/auto"},
		{Source: env.EnvSource_OVERRIDED_PARAM, Key: "common-override", Value: "F", Overrided: true},
		{Source: env.EnvSource_OVERRIDED_INCLUDE_FILE, Key: "from-fileB", Value: "B",
			Link: "https://a.yandex-team.ru/arc_vcs/classifieds/services/conf/vtail/b.yml?rev=r13"},
		{Source: env.EnvSource_LAYER_INCLUDE_FILE, Key: "from-fileA", Value: "A",
			Link: "https://a.yandex-team.ru/arc_vcs/classifieds/services/conf/vtail/a.yml"},
		{Source: env.EnvSource_SECRETS, Key: "k1", Value: "${sec-1:ver-1:k1}"},
	}
	require.ElementsMatch(t, expected, resolve)
}
