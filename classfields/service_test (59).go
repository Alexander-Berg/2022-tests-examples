package parser_test

import (
	"errors"
	"io/ioutil"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/storage"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/writer"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/user_error"
	envTypes "github.com/YandexClassifieds/shiva/pb/shiva/types/env"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/manifest/model/canary"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/param"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestParser(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	includeGetter := include.NewService(test_db.NewDb(t), test.NewLogger(t))
	service := parser.NewService(test.NewLogger(t), nil)

	data, err := ioutil.ReadFile("example.yml")
	require.NoError(t, err)
	assert.True(t, string(data) != "")
	m, _, err := service.ParseByte(common.Prod, data, includeGetter)
	require.NoError(t, err)

	// assert name and image
	assert.Equal(t, "yandex_vertis_example_service", m.Name)
	assert.Equal(t, "yandex_vertis_example_image", m.Image)
	assert.Equal(t, canary.Manual, m.Canary.Promote)
	assert.Equal(t, map[string]int{"sas": 10, "myt": 5, "vla": 10}, m.DC)
	assert.Equal(t, 100, m.Resources.CPU)
	assert.Equal(t, 256, m.Resources.Memory)
	assert.Equal(t, 4, m.Upgrade.Parallel)
	assert.Equal(t, 8, m.GeobaseVersion)
	assert.True(t, m.Resources.CpuHardLimit)
	assert.True(t, m.Resources.AutoCpu)

	// assert env
	prodEnvs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	mTest, _, err := service.ParseByte(common.Test, data, includeGetter)
	require.NoError(t, err)

	testEnvs, err := mTest.Config.GetEnvs()
	require.NoError(t, err)

	for _, env := range []map[string]string{prodEnvs, testEnvs} {
		assert.Equal(t, env["STRING2"], "txt2")
		assert.Equal(t, env["DURATION_SECOND"], "5s")
		assert.Equal(t, env["DURATION_MINUTE"], "2m")
		assert.Equal(t, env["DURATION_HOURS"], "1h")
		assert.Equal(t, env["ADDRESS_PORT"], ":80")
		assert.Equal(t, env["LONG_STRING"], "simple text for test")
		assert.Equal(t, env["STRING_WITH_POINT"], "text1.text2.text3")
		assert.Equal(t, env["STRING_WITH_UPPERSANT"], "text1_text2_text3")
		assert.Equal(t, env["STRING_WITH_HYPHEN"], "text1-text2--text3")
		assert.Equal(t, env["INTEGER"], "60")
		assert.Equal(t, env["LONG_1"], "60.56")
		assert.Equal(t, env["LONG_2"], "60,56")
		assert.Equal(t, env["ZERO"], "0")
		assert.Equal(t, env["NULL"], "")
		assert.Equal(t, env["NIL"], "nil")
		assert.Equal(t, env["TRUE"], "")
		assert.Equal(t, env["FALSE"], "")
		assert.Equal(t, env["TRUE_PARAM"], "true")
		assert.Equal(t, env["FALSE_PARAM"], "false")
		assert.Equal(t, env["YES_PARAM"], "true")
		assert.Equal(t, env["NO_PARAM"], "false")
		assert.Equal(t, env["YES_STRING_PARAM"], "yes")
		assert.Equal(t, env["NO_STRING_PARAM"], "no")
		assert.Equal(t, env["PLACEHOLDER_SERVICE"], "${service:tvmtool}")
		assert.Equal(t, env["PLACEHOLDER_SECRET"], "${secret:tvmtool}")
		assert.Equal(t, env["PLACEHOLDER_AUTO"], "${DC}")
	}

	assert.Equal(t, prodEnvs["ADDRESS"], "1.1.1.1:8080")
	assert.Equal(t, prodEnvs["STRING"], "txt_prod")

	assert.False(t, mTest.Resources.AutoCpu)

	assert.Equal(t, param.Layer, m.Place[param.Parallel.Path])
	assert.Equal(t, param.Overridden, m.Place[param.Sas.Path])
	assert.Equal(t, param.General, m.Place[param.Memory.Path])
}

func TestParserWithoutCanary(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	includeGetter := include.NewService(test_db.NewDb(t), test.NewLogger(t))
	svc := parser.NewService(test.NewLogger(t), nil)

	m, _, err := svc.ParseByte(common.Prod, []byte(manifestYamlWithSecrets), includeGetter)
	require.NoError(t, err)
	assert.True(t, m.Canary == nil)
}

func TestParserNotYaml(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	includeGetter := include.NewService(test_db.NewDb(t), test.NewLogger(t))
	service := parser.NewService(test.NewLogger(t), nil)

	_, _, err := service.ParseByte(common.Prod, []byte("asdfasdfasd"), includeGetter)
	assert.NotNil(t, err)
	uErr := &user_error.UserError{}
	assert.True(t, errors.As(err, &uErr))
}

func TestParser_Batch(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	includeGetter := include.NewService(test_db.NewDb(t), test.NewLogger(t))
	service := parser.NewService(test.NewLogger(t), nil)

	m, _, err := service.ParseByte(common.Prod, []byte(manifestBatch), includeGetter)
	require.NoError(t, err)

	assert.Empty(t, m.DC, "dc map should be empty")
	assert.Equal(t, "xyz", m.Periodic)
}

func TestSecretsPlaceholder(t *testing.T) {
	test.RunUp(t)
	includeGetter := include.NewService(test_db.NewDb(t), test.NewLogger(t))
	service := parser.NewService(test.NewLogger(t), nil)
	m, _, err := service.ParseByte(common.Prod, []byte(manifestYamlWithSecrets), includeGetter)
	require.NoError(t, err)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	assert.Len(t, envs, 4)
	assert.Equal(t, "1", envs["X"])
	assert.Equal(t, "${sec-one:ver-abc2def:some_key}", envs["SECRET_VAR"])
	assert.Equal(t, "${sec-one:ver-abc2def:k2}", envs["OTHER_SECRET_VAR"])
	assert.Equal(t, "${sec-42:ver-42123:kk}", envs["SECRET_VER"])
}

func TestTemplatesPlaceholder(t *testing.T) {
	const (
		urlTemplate  = "${url:shiva-tg:ci}"
		hostTemplate = "${hOsT:shiva:ci}"
		portTemplate = "${port:shiva-tg--main:ci}"
		tvmTemplate  = "${tvm-id:shiva}"
	)

	test.RunUp(t)

	includeGetter := include.NewService(test_db.NewDb(t), test.NewLogger(t))
	parserSvc := parser.NewService(test.NewLogger(t), nil)

	manifest, _, err := parserSvc.ParseByte(common.Prod, []byte(manifestYamlWithTemplates), includeGetter)
	require.NoError(t, err)

	envs, err := manifest.Config.GetEnvs()
	require.NoError(t, err)

	assert.Len(t, envs, 4)
	assert.Equal(t, urlTemplate, envs["SHIVA_URL"])
	assert.Equal(t, hostTemplate, envs["SHIVA_HOST"])
	assert.Equal(t, portTemplate, envs["SHIVA_PORT"])
	assert.Equal(t, tvmTemplate, envs["SHIVA_TVM"])
}

// DEPRECATED: temporary solution, see https://st.yandex-team.ru/VERTISADMIN-22609
func TestSecretsFromEnv(t *testing.T) {

	test.RunUp(t)
	includeGetter := include.NewService(test_db.NewDb(t), test.NewLogger(t))
	service := parser.NewService(test.NewLogger(t), nil)
	m, _, err := service.ParseByte(common.Prod, []byte(secretYaml), includeGetter)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, "foo", m.Name)
	assert.Equal(t, 1, m.DC["myt"])
	assert.True(t, m.SecretsFromEnv)
}

func TestConf(t *testing.T) {

	test.RunUp(t)
	confSrv := include.NewService(test_db.NewDb(t), test.NewLogger(t))
	require.NoError(t, confSrv.ReadAndSave([]byte(commonConf1), 10, "path/common1.yml"))
	require.NoError(t, confSrv.ReadAndSave([]byte(commonConf2), 10, "path/common2.yml"))
	require.NoError(t, confSrv.ReadAndSave([]byte(prodConf), 10, "path/prod.yml"))

	service := parser.NewService(test.NewLogger(t), nil)
	m, _, err := service.ParseByte(common.Prod, []byte(manifestWithConf), confSrv)
	require.NoError(t, err)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	assert.Equal(t, 5, len(envs))
	assert.Equal(t, "${sec-42:ver-42123:kkk}", envs["SECRET_VER"])
	assert.Equal(t, envs["PARAM_2"], "param 2")
	assert.Equal(t, envs["PARAM_3"], "param 3")
	assert.Equal(t, envs["PARAM_4"], "param 4")
	assert.Equal(t, envs["PARAM_5"], "prod param 5")

	m, _, err = service.ParseByte(common.Test, []byte(manifestWithConf), confSrv)
	require.NoError(t, err)
	envs, err = m.Config.GetEnvs()
	require.NoError(t, err)
	require.NoError(t, err)
	assert.Equal(t, 1, len(envs))
	assert.Equal(t, envs["PARAM_1"], "param 1")
	assert.Equal(t, m.GeobaseVersion, 0)
}

func TestConfig(t *testing.T) {

	test.RunUp(t)
	confSrv := include.NewService(test_db.NewDb(t), test.NewLogger(t))
	require.NoError(t, confSrv.ReadAndSave([]byte(commonConf1), 10, "path/common1.yml"))
	require.NoError(t, confSrv.ReadAndSave([]byte(commonConf2), 10, "path/common2.yml"))
	require.NoError(t, confSrv.ReadAndSave([]byte(commonConf3), 10, "path/common3.yml"))
	require.NoError(t, confSrv.ReadAndSave([]byte(prodConf), 10, "path/prod.yml"))

	service := parser.NewService(test.NewLogger(t), nil)
	m, _, err := service.ParseByte(common.Prod, []byte(manifestConfigAndCommon), confSrv)
	require.NoError(t, err)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	assert.Len(t, envs, 8)
	assert.Equal(t, "${sec-42:ver-42123:kkk}", envs["SECRET_VER"])
	// common_params
	assert.Equal(t, envs["CP1"], "VALUE_CP1")
	assert.Equal(t, envs["CP2"], "VALUE_CP2")
	// prod.config.files
	assert.Equal(t, envs["PARAM_3"], "param 3")
	assert.Equal(t, envs["PARAM_4"], "param 4")
	// params
	assert.Equal(t, envs["PARAM_2"], "param 2")
	assert.Equal(t, envs["PARAM_5"], "prod.config.params.PARAM_5")
	assert.Equal(t, envs["ADDRESS"], "1.1.1.1:8080")

	m, _, err = service.ParseByte(common.Test, []byte(manifestConfigAndCommon), confSrv)
	require.NoError(t, err)
	envs, err = m.Config.GetEnvs()
	require.NoError(t, err)
	assert.Equal(t, 4, len(envs))
	// common_params
	assert.Equal(t, envs["CP1"], "VALUE_CP1")
	assert.Equal(t, envs["CP2"], "VALUE_CP2")
	// test.config.files
	assert.Equal(t, envs["PARAM_2"], "param 2")
	// general.config.params
	assert.Equal(t, envs["STRING"], "general")
	assert.Equal(t, m.GeobaseVersion, 0)
}

func TestErrConfEnvTogether(t *testing.T) {

	test.RunUp(t)
	confSrv := include.NewService(test_db.NewDb(t), test.NewLogger(t))
	require.NoError(t, confSrv.ReadAndSave([]byte(prodConf), 10, "path/prod.yml"))

	service := parser.NewService(test.NewLogger(t), nil)
	_, _, err := service.ParseByte(common.Prod, []byte(manifestWithConfAndEnv), confSrv)
	assert.Equal(t, parser.ErrConfEnvTogether, err)
}

func TestDefault(t *testing.T) {
	test.RunUp(t)
	confSrv := include.NewService(test_db.NewDb(t), test.NewLogger(t))
	require.NoError(t, confSrv.ReadAndSave([]byte(prodConf), 10, "path/prod.yml"))

	service := parser.NewService(test.NewLogger(t), nil)
	m, uErr, err := service.ParseByte(common.Prod, []byte(clearYaml), confSrv)
	require.NoError(t, err)
	require.Equal(t, 0, uErr.Len())
	assert.False(t, m.Resources.CpuHardLimit)
}

func TestEnvGetter(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	includeGetter := include.NewService(db, log)
	externalEnvReader := reader.NewService(db, log)
	externalEnvWriter := writer.NewService(db, log, nil, nil, nil)
	service := parser.NewService(log, externalEnvReader)
	require.NoError(t, externalEnvWriter.New(&storage.ExternalEnv{
		Service: "yandex_vertis_example_service",
		Layer:   common.Unknown,
		Type:    envTypes.EnvType_TYPE_UNKNOWN,
		Key:     "externalEnv_key0",
		Value:   "value0",
	}))
	require.NoError(t, externalEnvWriter.New(&storage.ExternalEnv{
		Service: "no_service",
		Layer:   common.Prod,
		Type:    envTypes.EnvType_GENERATED_TVM_ID,
		Key:     "externalEnv_key1",
		Value:   "value1",
	}))
	require.NoError(t, externalEnvWriter.New(&storage.ExternalEnv{
		Service: "yandex_vertis_example_service",
		Layer:   common.Test,
		Type:    envTypes.EnvType_GENERATED_TVM_ID,
		Key:     "externalEnv_key2",
		Value:   "value2",
	}))
	require.NoError(t, externalEnvWriter.New(&storage.ExternalEnv{
		Service: "yandex_vertis_example_service",
		Layer:   common.Prod,
		Type:    envTypes.EnvType_GENERATED_TVM_ID,
		Key:     "externalEnv_key3",
		Value:   "value3",
	}))
	require.NoError(t, externalEnvWriter.New(&storage.ExternalEnv{
		Service: "yandex_vertis_example_service",
		Layer:   common.Prod,
		Type:    envTypes.EnvType_GENERATED_TVM_SECRET,
		Key:     "externalEnv_secret",
		Value:   "${sec-123:ver-321:value4}",
	}))

	data, err := ioutil.ReadFile("example.yml")
	require.NoError(t, err)
	assert.True(t, string(data) != "")
	m, _, err := service.ParseByte(common.Prod, data, includeGetter)
	require.NoError(t, err)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)
	assert.Equal(t, "value0", envs["externalEnv_key0"])
	assert.Equal(t, "value3", envs["externalEnv_key3"])
	assert.Equal(t, "${sec-123:ver-321:value4}", envs["externalEnv_secret"])
}

func TestAnyDC(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	includeGetter := include.NewService(test_db.NewDb(t), test.NewLogger(t))
	service := parser.NewService(test.NewLogger(t), nil)

	m, _, err := service.ParseByte(common.Prod, []byte(manifestAnyDC), includeGetter)
	require.NoError(t, err)

	assert.Equal(t, 1, m.DC["any"])
}

func TestWithEnvGetter(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	includeGetter := include.NewService(test_db.NewDb(t), test.NewLogger(t))
	service := parser.NewService(test.NewLogger(t), nil)

	getter := &mock.EnvGetter{}
	getter.On("GetEnvs").
		Return(map[string]string{"k1": "${sec-1:ver-1:k1}", "SECRET_VAR": "${sec-1:ver-1:SECRET_VAR"}, nil)
	m, _, err := service.ParseByte(common.Prod, []byte(manifestYamlWithSecrets), includeGetter, getter)
	require.NoError(t, err)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	require.NoError(t, err)

	require.Equal(t, "${sec-1:ver-1:k1}", envs["k1"])
	assert.Equal(t, "${sec-one:ver-abc2def:some_key}", envs["SECRET_VAR"])
}

var clearYaml = `
name: foo
general:
 datacenters:
  myt:
   count: 1
`

var secretYaml = `
name: foo
secrets_from_env: true
general:
 datacenters:
  myt:
   count: 1
`

var manifestAnyDC = `
name: foo
general:
 datacenters:
  any:
   count: 1
`

var manifestYamlWithSecrets = `
name: my-secret-service
general: {datacenters: {myt: {count: 1}}}
prod:
  env:
    - X: 1
    - SECRET_VAR: "${sec-one:ver-abc2def:some_key}"
    - OTHER_SECRET_VAR: "${sec-one:ver-abc2def:k2}"
    - SECRET_VER: "${sec-42:ver-42123:kk}"
`

var manifestYamlWithTemplates = `
name: my_service
general: {datacenters: {myt: {count: 1}}}
prod:
  env:
    - SHIVA_URL: "${url:shiva-tg:ci}"
    - SHIVA_PORT: "${port:shiva-tg--main:ci}"
    - SHIVA_HOST: "${hOsT:shiva:ci}"
    - SHIVA_TVM: "${tvm-id:shiva}"
`

var manifestBatch = `
name: some-batch-svc
prod:
  periodic: "xyz"
`

const (
	manifestWithConfAndEnv = `
name: my_service
general:
  datacenters:
    myt:
      count: 1
prod:
  env:
    - PARAM_ENV_1: param 1
  conf:
    - path/prod.yml
`

	manifestWithConf = `
name: my_service
general:
  datacenters:
    myt:
      count: 1
  conf:
    - path/common1.yml
prod:
  conf:
    - path/common2.yml
    - path/prod.yml
`

	manifestConfigAndCommon = `
name: my_service

common_params:
  CP1: VALUE_CP1
  CP2: VALUE_CP2

general:
  datacenters:
    myt:
      count: 1
  config:
    files:
      - path/common1.yml
    params:
      STRING: general
prod:
  config:
    files:
      - path/common3.yml
      - path/prod.yml
    params:
      ADDRESS: 1.1.1.1:8080
      PARAM_5: prod.config.params.PARAM_5
test:
  config:
    files:
      - path/common2.yml
`

	commonConf1 = `
PARAM_1: param 1
`
	commonConf2 = `
PARAM_2: param 2
`
	commonConf3 = `
PARAM_2: param 2
PARAM_5: common param 5
`
	prodConf = `
PARAM_3: param 3
SECRET_VER: "${sec-42:ver-42123:kkk}"
PARAM_4: param 4
PARAM_5: prod param 5
`
)
