package config

import (
	"context"
	"os"
	"path"
	"path/filepath"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/accrualer/internal/transforms"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/configops"
	configopstype "a.yandex-team.ru/billing/library/go/billingo/pkg/configops/type"
	"a.yandex-team.ru/library/go/test/yatest"
)

func TestFormatTypes(t *testing.T) {
	loc, err := time.LoadLocation("Europe/Moscow")
	require.NoError(t, err)

	transforms.RegisterTransforms(loc)

	rootPath, err := filepath.Abs(yatest.SourcePath("billing/hot/accrualer/config"))
	require.NoError(t, err)
	devPath := path.Join(rootPath, "dev", "formats")

	formatsConf, err := ParseFormatsConfig(context.Background(), os.DirFS(devPath), ".")
	require.NoError(t, err)

	agency, ok := formatsConf["base_agency"]
	require.True(t, ok)
	require.Equal(t, AgencyAccrual, agency.Type)

	spendable, ok := formatsConf["base_spendable"]
	require.True(t, ok)
	require.Equal(t, SpendableAccrual, spendable.Type)
}

//TestFormatMessageSystem fails if message_system field is not "billingrew"
func TestFormatMessageSystem(t *testing.T) {
	loc, err := time.LoadLocation("Europe/Moscow")
	require.NoError(t, err)

	transforms.RegisterTransforms(loc)

	rootPath, err := filepath.Abs(yatest.SourcePath("billing/hot/accrualer/config"))
	require.NoError(t, err)
	for _, env := range []string{"dev", "test", "prod"} {
		formats := path.Join(rootPath, env, "formats")

		formatsConf, err := ParseFormatsConfig(context.Background(), os.DirFS(formats), ".")
		require.NoError(t, err)

		for format, conf := range formatsConf {
			if conf.Type == SpendableAccrual {
				continue
			}

			sys, ok := conf.Output["message_system"]
			assert.True(t, ok, "{%s} {%s} no message system output field", env, format)
			value, ok := sys.Transform.(*configops.Value)
			assert.True(t, ok, "{%s} {%s} message system is not Value", env, format)
			assert.False(t, value.IsVariable(), "{%s} {%s} message system is variable", env, format)
			ev, c, err := value.Evaluate(nil)
			require.NoError(t, err, "{%s} {%s}", env, format)
			require.Equal(t, configopstype.ValueTypeString, c, "{%s} {%s}", env, format)
			assert.Equal(t, "billingrew", ev.(string), "{%s} {%s} message system has incorrect value", env, format)
		}
	}
}
