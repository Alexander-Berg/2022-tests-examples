package config

import (
	"context"
	"os"
	"path"
	"path/filepath"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"gopkg.in/yaml.v2"

	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	bt "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/test/yatest"
)

const (
	logicFileName    = "logic.yaml"
	defaultNamespace = "default"
)

func TestLogicValidateOk(t *testing.T) {
	formats := Formats{
		"test_agency":          Format{Type: AgencyAccrual},
		"test_agency_detailed": Format{Type: AgencyAccrual},
		"test_spendable":       Format{Type: SpendableAccrual},
	}

	logic := LogicConfig{
		Namespace: map[string]NamespaceConfig{
			defaultNamespace: {
				Accruals: AccrualsConfig{
					Types: map[string]ActionTypeConfig{
						AgencyFull:     {Events: EventTypes{{FormatName: "test_agency"}}},
						AgencyDetailed: {Events: EventTypes{{FormatName: "test_agency_detailed"}}},
						Spendable:      {Events: EventTypes{{FormatName: "test_spendable"}}}},
				},
			},
		},
	}

	require.NoError(t, logic.ValidateFormats(formats))
}

func TestLogicValidateFail(t *testing.T) {
	formats := Formats{
		"test_agency":          Format{Type: AgencyAccrual},
		"test_agency_detailed": Format{Type: AgencyAccrual},
		"test_spendable":       Format{Type: SpendableAccrual},
	}

	logic := LogicConfig{
		Namespace: map[string]NamespaceConfig{
			defaultNamespace: {
				Accruals: AccrualsConfig{
					Types: map[string]ActionTypeConfig{
						AgencyFull:     {Events: EventTypes{{FormatName: "test_agency"}}},
						AgencyDetailed: {Events: EventTypes{{FormatName: "test_agency_detailed"}}},
						Spendable:      {Events: EventTypes{{FormatName: "test_agency"}}}},
				},
			},
		},
	}
	require.Error(t, logic.ValidateFormats(formats))

	logic = LogicConfig{
		Namespace: map[string]NamespaceConfig{
			defaultNamespace: {
				Accruals: AccrualsConfig{
					Types: map[string]ActionTypeConfig{
						AgencyFull:     {Events: EventTypes{{FormatName: "test_agency"}}},
						AgencyDetailed: {Events: EventTypes{{FormatName: "test_spendable"}}},
						Spendable:      {Events: EventTypes{{FormatName: "test_spendable"}}}},
				},
			},
		},
	}
	require.Error(t, logic.ValidateFormats(formats))

	logic = LogicConfig{
		Namespace: map[string]NamespaceConfig{
			defaultNamespace: {
				Accruals: AccrualsConfig{
					Types: map[string]ActionTypeConfig{
						AgencyFull:     {Events: EventTypes{{FormatName: "test_spendable"}}},
						AgencyDetailed: {Events: EventTypes{{FormatName: "test_agency_detailed"}}},
						Spendable:      {Events: EventTypes{{FormatName: "test_spendable"}}}},
				},
			},
		},
	}
	require.Error(t, logic.ValidateFormats(formats))
}

//TestLogicConfig checks logic config parse from file for all envs (dev, test, prod)
// In additional checks:
//
// - agency acts sign is filled
//
// - acts topics are equal
//
// - namespace doesn't override platform fields path (like id, namespace or invoice)
//
// - paysys_type_cc is filled in accruals (not spendable)
func TestLogicConfig(t *testing.T) {
	rootPath, err := filepath.Abs(yatest.SourcePath("billing/hot/accrualer/config"))
	require.NoError(t, err)

	for _, dir := range []string{"dev", "test", "prod"} {
		t.Run("env "+dir, func(t *testing.T) {
			configLoader, err := bconfig.PrepareLoaderWithEnvPrefix("accrualer", path.Join(rootPath, dir, logicFileName))
			require.NoError(t, err)
			logicConfig, err := ParseLogic(context.Background(), configLoader, xlog.GlobalLogger())
			require.NoError(t, err, "parse logic config %s", dir)

			actTopic := ""

			for _, field := range PlatformFields {
				value, ok := logicConfig.Event.Fields[field]
				assert.Truef(t, ok, "{%s} doesn't contain system field \"%s\"", dir, field)
				assert.NotEmptyf(t, value, "{%s} is empty field \"%s\"", dir, field)
			}
			for namespace, namespaceConfig := range logicConfig.Namespace {
				agencyActs, ok := namespaceConfig.Acts[Agency]
				if !ok {
					continue
				}
				if actTopic == "" {
					actTopic = agencyActs.Topics["default"].Topic
				}
				assert.NotEmptyf(t, actTopic, "{%s} {%s} empty act topic", dir, namespace)
				assert.Equalf(t, actTopic, agencyActs.Topics["default"].Topic, "{%s} {%s} default act topic differs", dir, namespace)
				assert.Equalf(t, actTopic, agencyActs.Topics["dry-run"].Topic, "{%s} {%s} dry-run act topic differs", dir, namespace)

				for _, event := range agencyActs.Events {
					assert.NotEmptyf(t, event.Sign, "{%s} {%s} empty act sign %v", dir, namespace, event)
				}

				for _, field := range PlatformFields {
					_, ok := namespaceConfig.Accruals.EventFields.Fields[field]
					assert.Falsef(t, ok, "{%s} {%s} overrides system field \"%s\"", dir, namespace, field)
				}

				for name, events := range namespaceConfig.Accruals.Types {
					for _, field := range PlatformFields {
						_, ok := events.EventFields.Fields[field]
						assert.Falsef(t, ok, "{%s} {%s} {%s} overrides system field \"%s\"", dir, namespace, name, field)
					}
					if name != Spendable {
						for _, event := range events.Events {
							assert.NotEmptyf(t, event.TypeCC, "{%s} {%s} {%s} empty accrual paysys_type_cc %v", dir, namespace, name, event)
						}
					}
					for title, conf := range events.Topics {
						if strings.Contains(conf.Topic, "common") {
							assert.NotZerof(t, conf.PartitionGroup, "{%s} {%s} {%s} {%s} use common topic without partition group ", dir, namespace, name, title)
						}
					}
				}
			}

			formatsConf, err := ParseFormatsConfig(context.Background(), os.DirFS(path.Join(rootPath, dir, "formats")), ".")
			require.NoError(t, err)
			assert.NoError(t, logicConfig.ValidateFormats(formatsConf))
		})
	}
}

// TestStrictLoadLogic fails if where are unknown config fields
func TestStrictLoadLogic(t *testing.T) {
	rootPath, err := filepath.Abs(yatest.SourcePath("billing/hot/accrualer/config"))
	require.NoError(t, err)

	for _, dir := range []string{"dev", "test", "prod"} {
		t.Run("env "+dir, func(t *testing.T) {
			data, err := os.ReadFile(filepath.Join(rootPath, dir, logicFileName))
			require.NoError(t, err)
			logicConfig := LogicConfig{}
			assert.NoError(t, yaml.UnmarshalStrict(data, &logicConfig), "parse logic config %s", dir)
		})
	}
}

// TestLogicYTToken check getting yt-token from env
func TestLogicYTToken(t *testing.T) {
	rootPath, err := filepath.Abs(yatest.SourcePath("billing/hot/accrualer/config"))
	require.NoError(t, err)

	prevYT := os.Getenv("ACCRUALER_YT_TOKEN")
	token := bt.RandS(30)
	require.NoError(t, os.Setenv("ACCRUALER_YT_TOKEN", token))
	defer func() {
		_ = os.Setenv("ACCRUALER_YT_TOKEN", prevYT)
	}()

	configLoader, err := bconfig.PrepareLoaderWithEnvPrefix("accrualer", path.Join(rootPath, "prod", logicFileName))
	require.NoError(t, err)
	logicConfig, err := ParseLogic(context.Background(), configLoader, xlog.GlobalLogger())
	require.NoError(t, err, "parse logic config %s", "prod")
	for namespace, config := range logicConfig.Namespace {
		assert.Equal(t, token, config.Accruals.MarkedEvents.ytToken, "yt token differs for %s namespace", namespace)
	}
}
