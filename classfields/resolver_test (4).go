package tvm

import (
	_ "embed"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/storage"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/user_error"
	spb "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/env"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	tmp_errors "github.com/YandexClassifieds/shiva/pkg/template/errors"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	//go:embed test_data/test-service.yml
	testSvcMap string
)

func Test_Match(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	db := test_db.NewDb(t)
	log := test.NewLogger(t)

	mapSvc := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())
	envSvc := reader.NewService(db, log)

	svc := NewResolver(mapSvc, envSvc)

	cases := []struct {
		env     string
		matched bool
	}{
		{
			env:     "${tVm-Id:shiva}",
			matched: true,
		},
		{
			env:     "{tvm-id:shiva}",
			matched: false,
		},
		{
			env:     "${tvm-id:shiva:api}",
			matched: true,
		},
		{
			env:     "${tvm_id:shiva}",
			matched: false,
		},
		{
			env:     "${url:shiva}",
			matched: false,
		},
	}

	for _, c := range cases {
		t.Run(c.env, func(t *testing.T) {
			assert.Equal(t, c.matched, svc.Match(c.env))
		})
	}
}

func Test_Validate(t *testing.T) {
	const (
		env = "TEST_SERVICE_TVM"
	)

	test.RunUp(t)
	defer test.Down(t)

	db := test_db.NewDb(t)
	log := test.NewLogger(t)

	mapSvc := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())
	envSvc := reader.NewService(db, log)

	svc := NewResolver(mapSvc, envSvc)

	require.NoError(t, mapSvc.ReadAndSave([]byte(testSvcMap), 10, spb.ToFullPath("test-service")))

	cases := []struct {
		template    string
		expectedErr *user_error.UserError
	}{
		{
			template:    "${tvm-id:test-service}",
			expectedErr: nil,
		},
		{
			template:    "${tvm-id:shiva}",
			expectedErr: tmp_errors.NewErrInvalidTvmID(env),
		},
		{
			template:    "{tvm-id:shiva}",
			expectedErr: tmp_errors.NewErrParseTemplate("{tvm-id:shiva}"),
		},
		{
			template:    "${tvm-id:shiva:api}",
			expectedErr: tmp_errors.NewErrInvalidTvmIDTemplate("${tvm-id:shiva:api}"),
		},
		{
			template:    "${tvm-id:shiva",
			expectedErr: tmp_errors.NewErrInvalidTvmIDTemplate("${tvm-id:shiva"),
		},
	}

	for _, c := range cases {
		t.Run(c.template, func(t *testing.T) {
			err := svc.Validate("shiva", env, c.template)

			assert.Equal(t, c.expectedErr, err)
		})
	}
}

func Test_ResolveSuccess(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	db := test_db.NewDb(t)
	log := test.NewLogger(t)

	mapSvc := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())
	envSvc := reader.NewService(db, log)
	envStorage := storage.NewStorage(db, log)

	svc := NewResolver(mapSvc, envSvc)

	require.NoError(t, mapSvc.ReadAndSave([]byte(testSvcMap), 10, spb.ToFullPath("test-service")))

	cases := []struct {
		layer common.Layer
		tvmID string
	}{
		{layer: common.Test, tvmID: "10"},
		{layer: common.Prod, tvmID: "20"},
	}

	for _, c := range cases {
		t.Run(c.layer.String(), func(t *testing.T) {
			require.NoError(t, envStorage.Save(&storage.ExternalEnv{
				Service: "test-service",
				Layer:   c.layer,
				Type:    env.EnvType_GENERATED_TVM_ID,
				Key:     "tvm-id",
				Value:   c.tvmID,
			}))

			tvmID, err := svc.Resolve("shiva", "tvm", "${tvm-id:test-service}", c.layer)
			require.Nil(t, err)

			assert.Equal(t, c.tvmID, tvmID)
		})
	}
}

func Test_ResolveUndefinedService(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	db := test_db.NewDb(t)
	log := test.NewLogger(t)

	mapSvc := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())
	envSvc := reader.NewService(db, log)

	svc := NewResolver(mapSvc, envSvc)

	tvmID, err := svc.Resolve("shiva", "tvm", "${tvm-id:test-service}", common.Prod)

	assert.Equal(t, tmp_errors.NewErrUnknownService("tvm", "test-service"), err)
	assert.Equal(t, "", tvmID)
}
