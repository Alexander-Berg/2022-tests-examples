package core

import (
	"context"
	"os"
	"testing"

	"github.com/heetch/confita"
	"github.com/stretchr/testify/require"
	"gopkg.in/yaml.v2"

	"a.yandex-team.ru/library/go/core/resource"
)

func TestParseAllowedTVM(t *testing.T) {
	require.Nil(t, parseAllowedTVM(""))

	res := parseAllowedTVM(`[12, 23]`)
	require.Equal(t, 2, len(res))
	require.EqualValues(t, 12, res[0])
	require.EqualValues(t, 23, res[1])

	require.Nil(t, parseAllowedTVM("[:]"))
}

// Resource определяем confita.Backend чтобы конфиг из ресурсов брать
type Resource struct {
	path string
}

func NewResourceBackend(path string) *Resource {
	return &Resource{
		path: path,
	}
}

// Get is not implemented.
func (b *Resource) Get(ctx context.Context, key string) ([]byte, error) {
	return nil, nil
}

// Name returns the type of the file.
func (b *Resource) Name() string {
	return "ArcadiaResource"
}

// Unmarshal takes a struct pointer and unmarshals the file into it,
// using either json or yaml based on the file extention.
func (b *Resource) Unmarshal(ctx context.Context, to any) error {
	yamlFile := resource.Get(b.path)
	return yaml.Unmarshal(yamlFile, to)
}

func TestParseConfigBaseEnv(t *testing.T) {
	loader := confita.NewLoader(NewResourceBackend("/prod_conf"))

	require.NoError(t, os.Setenv("PAYOUT_TVM_SRC", "111"))
	require.NoError(t, os.Setenv("PAYOUT_TVM_ALLOWED", "[222]"))
	require.NoError(t, os.Setenv("PAYOUT_ACCOUNTS_URL", "333"))
	require.NoError(t, os.Setenv("PAYOUT_ACCOUNTS_TVM", "444"))

	require.NoError(t, os.Setenv("PAYOUT_LB_OEBS_BASE_PATH", "/base/oebs"))
	require.NoError(t, os.Setenv("PAYOUT_LB_NOTIFIER_BASE_PATH", "/base/notify"))
	require.NoError(t, os.Setenv("PAYOUT_LB_OEBS_ERRORS_BASE_PATH", "/base"))
	require.NoError(t, os.Setenv("PAYOUT_LB_CPF_BASE_PATH", "/base"))

	ctx := context.Background()

	conf, err := ParseConfig(ctx, loader)
	require.NoError(t, err)
	require.NotNil(t, conf)

	require.Equal(t, "111", conf.TVM.SRC)
	require.EqualValues(t, 222, conf.TVM.Allowed[0])
	require.Equal(t, "333", conf.Clients.Accounts.Transport.BaseURL)
	require.Equal(t, "444", conf.Clients.Accounts.Transport.TVMDst)
	require.Greater(t, conf.Monitorings.MvPayoutRetrotime, 0)
	require.Equal(t, 10, conf.Monitorings.PayoutMaxAge)

	require.Equal(t, "/base/oebs/new-payout", conf.OEBSGate["taxi"].Providers.NewPayouts.Topic)
	require.Equal(t, "/base/oebs/new-payout-dry", conf.OEBSGate["taxi"].Providers.NewPayoutsDry.Topic)
	require.Equal(t, "/base/oebs/payout-status-ard", conf.OEBSGate["taxi"].Consumers.PayoutsResponses.Topic)
	require.Equal(t, "/base/oebs/payout-status-ard-dry", conf.OEBSGate["taxi"].Consumers.PayoutsResponsesDry.Topic)
	require.Equal(t, "/base/oebs/payout-status-ard", conf.OEBSGate["taxi"].Providers.ZeroPayoutsResponses.Topic)
	require.Equal(t, "/base/oebs/payout-status-ard-dry", conf.OEBSGate["taxi"].Providers.ZeroPayoutsResponsesDry.Topic)
	require.Equal(t, "/base/oebs/billing-reader", conf.OEBSGate["taxi"].Consumers.PayoutsResponses.Consumer)
	require.Equal(t, "/base/oebs/billing-reader", conf.OEBSGate["taxi"].Consumers.PayoutsResponsesDry.Consumer)
	require.Equal(t, "/base/oebs/payout-status-oebs", conf.OEBSGate["taxi"].Consumers.BatchResponses.Topic)
	require.Equal(t, "/base/oebs/payout-status-oebs-dry", conf.OEBSGate["taxi"].Consumers.BatchResponsesDry.Topic)
	require.Equal(t, "/base/oebs/billing-reader", conf.OEBSGate["taxi"].Consumers.BatchResponses.Consumer)
	require.Equal(t, "/base/oebs/billing-reader", conf.OEBSGate["taxi"].Consumers.BatchResponsesDry.Consumer)
	require.Equal(t, "/base/notify/errors-%s", conf.Notifier.LogBroker.TopicTemplate)
	require.Equal(t, "/base/oebs-errors", conf.OebsErrors.Topic)
	require.Equal(t, "/base/cpf", conf.Clients.Cpf.Producer.Topic)
	require.Equal(t, "/base/cpf-dry", conf.Clients.Cpf.ProducerDryRun.Topic)
}
