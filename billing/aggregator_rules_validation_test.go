package scheduler

import (
	"embed"
	"fmt"
	"io/fs"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/scheduler/pkg/core"
	configopstype "a.yandex-team.ru/billing/library/go/billingo/pkg/configops/type"
	configvars "a.yandex-team.ru/billing/library/go/billingo/pkg/configops/vars"
)

//go:embed config/eventaggregator/rules
var configFS embed.FS

const baseDir = "config/eventaggregator/rules"

//go:embed pkg/core/eventaggregator/configs
var aggregatorConfigFS embed.FS

const baseAggregatorDir = "pkg/core/eventaggregator/configs"

func TestValidateConfigTypesDev(t *testing.T) {
	testValidateEnvConfig(t, configFS, baseDir, "dev")
}

func TestValidateConfigTypesTest(t *testing.T) {
	testValidateEnvConfig(t, aggregatorConfigFS, baseAggregatorDir, ".")
}

func TestValidateConfigTypesProd(t *testing.T) {
	testValidateEnvConfig(t, configFS, baseDir, "prod")
}

func testValidateEnvConfig(t *testing.T, cfgFs fs.FS, dir string, env string) {
	t.Helper()

	cfg, err := core.ParseAggregatorConfig(cfgFs, filepath.Join(dir, env))
	require.NoError(t, err)

	fieldsCache := configvars.NewLazyKeyedVars(
		configvars.NewLazyVars(nil, cfg.Source.KeyFields), configvars.LazyVars{},
	)
	for filteredSourceName, aggregateRules := range cfg.FilteredSourceToRules {
		fieldsCache.SetOtherVars(configvars.NewLazyVars(nil, cfg.FilteredSources[filteredSourceName].Fields))
		for _, dataNamespaceRule := range aggregateRules {
			for _, rule := range dataNamespaceRule.Rules {
				objIDType, err := rule.Destination.ObjectID.Validate(fieldsCache)
				require.NoError(t, err, fmt.Sprintf(
					"cannot find object id field %v for data namespace %q and filtered source %q",
					rule.Destination.ObjectID, dataNamespaceRule.DataNamespace, filteredSourceName,
				))
				assert.Equal(t, configopstype.ValueTypeString, objIDType, fmt.Sprintf(
					"object ID must be string type: field %v, data namespace %q, filtered source %q",
					rule.Destination.ObjectID, dataNamespaceRule.DataNamespace, filteredSourceName,
				))
				assert.NoError(t, rule.Filter.Validate(fieldsCache), fmt.Sprintf(
					"failed validation for data namespace %q and filtered source %q",
					dataNamespaceRule.DataNamespace, filteredSourceName,
				))
			}
		}
	}
}
