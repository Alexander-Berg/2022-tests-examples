package serviceMap

import (
	"context"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/storage"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/writer"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/logger"
	storage2 "github.com/YandexClassifieds/shiva/common/storage"
	spb "github.com/YandexClassifieds/shiva/pb/shiva/api/service_map"
	smpb "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	envTypes "github.com/YandexClassifieds/shiva/pb/shiva/types/env"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pkg/favorite"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	staff_fixture "github.com/YandexClassifieds/shiva/test/mock/staff"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
)

var _ spb.ServiceMapsServer = &Handler{}

func TestServiceMapHandler_IsOwner(t *testing.T) {
	testCases := []struct {
		name    string
		mapSpec string
		isOwner bool
	}{
		{
			name:    "owner=true",
			mapSpec: `{name: owner-test-svc, owners: [https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra]}`,
			isOwner: true,
		},
		{
			name:    "owner=false",
			mapSpec: `{name: owner-test-svc, owners: [https://staff.yandex-team.ru/departments/yandex_personal_vertserv_auto_autoru_serv]}`,
			isOwner: false,
		},
	}
	test.RunUp(t)
	defer test.Down(t)
	h := newHandler(t, test_db.NewDb(t), test.NewLogger(t))

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			err := h.service.ReadAndSave([]byte(tc.mapSpec), test.AtomicNextUint(), "maps/owner-test-svc.yml")
			require.NoError(t, err)
			response, err := h.IsOwner(context.Background(), &spb.IsOwnerRequest{Service: "owner-test-svc", Login: staff_fixture.Owner})
			require.NoError(t, err)
			assert.Equal(t, tc.isOwner, response.IsOwner)
		})
	}
}

func TestServiceMapHandler_IsOwner_NotExists(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	h := newHandler(t, test_db.NewDb(t), test.NewLogger(t))
	_, err := h.IsOwner(context.Background(), &spb.IsOwnerRequest{Service: "non-existing-svc", Login: staff_fixture.Owner})
	if !assert.Error(t, err) {
		return
	}
	s, ok := status.FromError(err)
	if !assert.True(t, ok) {
		return
	}
	assert.Equal(t, codes.NotFound, s.Code())
}

func TestServiceMapHandler_Get(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	h := newHandler(t, test_db.NewSeparatedDb(t), test.NewLogger(t))

	assert.NoError(t, h.service.ReadAndSave([]byte("{name: service}"), 10, "maps/service.yml"))
	assert.NoError(t, h.service.ReadAndSave([]byte("{name: mysql-cluster, type: mdb_mysql, mdb_cluster: {test_id: mdb000000000, prod_id: mdb111111111}}"), 10, "maps/mysql/mysql-cluster.yml"))
	assert.NoError(t, h.service.ReadAndSave([]byte("{name: postgresql-cluster, type: mdb_postgresql, mdb_cluster: {test_id: mdb000000000, prod_id: mdb111111111}}"), 10, "maps/postgresql/postgresql-cluster.yml"))
	assert.NoError(t, h.service.ReadAndSave([]byte("{name: kafka-cluster, type: kafka, mdb_cluster: {test_id: mdb000000000, prod_id: mdb111111111}}"), 10, "maps/kafka/shared-01.yml"))

	tests := []struct {
		Name                string
		Service             string
		ExpectedServiceName string
		ExpectedServiceType smpb.ServiceType
		ExpectedMDBCluster  *smpb.MDBCluster
	}{
		{
			Name:                "getService",
			Service:             "service",
			ExpectedServiceName: "service",
			ExpectedServiceType: smpb.ServiceType_service,
		},
		{
			Name:                "getMySQL",
			Service:             "mysql/mysql-cluster",
			ExpectedServiceName: "mysql-cluster",
			ExpectedServiceType: smpb.ServiceType_mdb_mysql,
			ExpectedMDBCluster: &smpb.MDBCluster{
				TestId: "mdb000000000",
				ProdId: "mdb111111111",
			},
		},
		{
			Name:                "getPostgreSQL",
			Service:             "postgresql/postgresql-cluster",
			ExpectedServiceName: "postgresql-cluster",
			ExpectedServiceType: smpb.ServiceType_mdb_postgresql,
			ExpectedMDBCluster: &smpb.MDBCluster{
				TestId: "mdb000000000",
				ProdId: "mdb111111111",
			},
		},
		{
			Name:                "getKafka",
			Service:             "kafka/shared-01",
			ExpectedServiceName: "kafka-cluster",
			ExpectedServiceType: smpb.ServiceType_kafka,
			ExpectedMDBCluster: &smpb.MDBCluster{
				TestId: "mdb000000000",
				ProdId: "mdb111111111",
			},
		},
	}

	for _, tc := range tests {
		t.Run(tc.Name, func(t *testing.T) {
			sd, err := h.Get(context.Background(), &spb.GetRequest{
				Service: tc.Service,
			})
			assert.NoError(t, err)
			assert.Equal(t, tc.ExpectedServiceName, sd.Service.Name)
			assert.Equal(t, tc.ExpectedServiceType, sd.Service.Type)
			assert.Equal(t, tc.ExpectedMDBCluster, sd.Service.MdbCluster)

			if tc.ExpectedServiceType == smpb.ServiceType_mdb_mysql {
				assert.Equal(t, tc.ExpectedMDBCluster.TestId, sd.Service.MdbCluster.TestId)
				assert.Equal(t, tc.ExpectedMDBCluster.ProdId, sd.Service.MdbCluster.ProdId)
				assert.Equal(t, tc.ExpectedMDBCluster.TestId, sd.Service.MdbMysql.TestId)
				assert.Equal(t, tc.ExpectedMDBCluster.ProdId, sd.Service.MdbMysql.ProdId)
			}
		})
	}
}

func newHandler(t *testing.T, db *storage2.Database, log logger.Logger) *Handler {
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	favoriteSvc := favorite.NewService(db, log)
	extEnvSvc := reader.NewService(db, log)
	sMapSvc := service_map.NewService(db, log, service_change.NewNotificationMock())
	return NewServiceMapHandler(log, sMapSvc, staffService, favoriteSvc, extEnvSvc)
}

func newEnvWriter(db *storage2.Database, log logger.Logger) *writer.Service {
	return writer.NewService(db, log, service_map.NewService(db, log, service_change.NewNotificationMock()), nil, service_change.NewNotificationMock())
}

func TestHandler_ResolveTvmID(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	h := newHandler(t, db, log)
	envWriter := newEnvWriter(db, log)
	require.NoError(t, envWriter.New(&storage.ExternalEnv{
		Service: "shiva-test",
		Layer:   common.Prod,
		Type:    envTypes.EnvType_GENERATED_TVM_ID,
		Key:     "some_key",
		Value:   "123456789",
	}))
	response, err := h.ResolveTvmID(context.TODO(), &spb.ResolveTvmIDRequest{TvmID: "123456789"})
	require.NoError(t, err)
	assert.Equal(t, "shiva-test", response.Service)
	assert.Equal(t, layer.Layer_PROD, response.Layer)
}

func TestServiceMapHandler_ListAll(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	h := newHandler(t, test_db.NewDb(t), test.NewLogger(t))

	services := []struct {
		spec string
		path string
	}{
		{
			spec: `{name: service1, owners: [https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra]}`,
			path: "maps/service1.yml",
		},
		{
			spec: `{name: service2, type: service, owners: [https://staff.yandex-team.ru/departments/yandex_personal_vertserv_auto_autoru_serv]}`,
			path: "maps/service2.yml",
		},
		{
			spec: `{name: external1, type: external, owners: [https://staff.yandex-team.ru/departments/yandex_personal_vertserv_interface_interfaces2, https://staff.yandex-team.ru/departments/yandex_personal_vertserv_interface_2830]}`,
			path: "maps/external1.yml",
		},
		{
			spec: `{name: mysql1, type: mysql}`,
			path: "maps/mysql/mysql1.yml",
		},
		{
			spec: `{name: mdb-mysql1, type: mdb_mysql, mdb_cluster: {test_id: mdb000000000, prod_id: mdb111111111}}`,
			path: "maps/mysql/mdb-mysql1.yml",
		},
		{
			spec: `{name: mdb-postgresql1, type: mdb_postgresql, mdb_cluster: {test_id: mdb000000000, prod_id: mdb111111111}}`,
			path: "maps/postgresql/mdb-postgresql1.yml",
		},
		{
			spec: `{name: shared-01, type: kafka, mdb_cluster: {test_id: mdb000000000, prod_id: mdb111111111}}`,
			path: "maps/kafka/shared-01.yml",
		},
	}
	for _, service := range services {
		err := h.service.ReadAndSave([]byte(service.spec), 10, service.path)
		assert.NoError(t, err)
	}

	tests := []struct {
		Name             string
		Type             []smpb.ServiceType
		Owner            string
		ExpectedServices []string
	}{
		{
			Name:             "empty",
			ExpectedServices: []string{"service1", "service2", "external1", "mysql1", "mdb-mysql1", "mdb-postgresql1", "shared-01"},
		},
		{
			Name:             "service",
			Type:             []smpb.ServiceType{smpb.ServiceType_service},
			ExpectedServices: []string{"service1", "service2"},
		},
		{
			Name:             "external",
			Type:             []smpb.ServiceType{smpb.ServiceType_external},
			ExpectedServices: []string{"external1"},
		},
		{
			Name:             "mysql",
			Type:             []smpb.ServiceType{smpb.ServiceType_mysql},
			ExpectedServices: []string{"mysql1"},
		},
		{
			Name:             "mdb-mysql",
			Type:             []smpb.ServiceType{smpb.ServiceType_mdb_mysql},
			ExpectedServices: []string{"mdb-mysql1"},
		},
		{
			Name:             "kafka",
			Type:             []smpb.ServiceType{smpb.ServiceType_kafka},
			ExpectedServices: []string{"shared-01"},
		},
		{
			Name:             "mdb-postgresql",
			Type:             []smpb.ServiceType{smpb.ServiceType_mdb_postgresql},
			ExpectedServices: []string{"mdb-postgresql1"},
		},
		{
			Name:             "many",
			Type:             []smpb.ServiceType{smpb.ServiceType_mysql, smpb.ServiceType_mdb_mysql},
			ExpectedServices: []string{"mysql1", "mdb-mysql1"},
		},
		{
			Name:             "owner",
			Owner:            staff_fixture.Owner,
			ExpectedServices: []string{"service1"},
		},
		{
			Name:             "owner+service",
			Type:             []smpb.ServiceType{smpb.ServiceType_service},
			Owner:            staff_fixture.Owner,
			ExpectedServices: []string{"service1"},
		},
		{
			Name:             "multiple match + service with multiple groups",
			Type:             []smpb.ServiceType{smpb.ServiceType_external, smpb.ServiceType_service},
			Owner:            "swapster",
			ExpectedServices: []string{"service1", "external1"},
		},
	}

	for _, tc := range tests {
		t.Run(tc.Name, func(t *testing.T) {
			s := &testStream{}
			err := h.ListAll(&spb.ListRequest{
				Type:       tc.Type,
				OwnerLogin: tc.Owner,
			}, s)
			assert.NoError(t, err)

			services := s.GetServices()
			assert.ElementsMatch(t, services, tc.ExpectedServices)
		})
	}
}

type testStream struct {
	grpc.Stream
	services []*spb.ServiceData
}

func (t *testStream) SetHeader(_ metadata.MD) error {
	return nil
}

func (t *testStream) SendHeader(_ metadata.MD) error {
	return nil
}

func (t *testStream) SetTrailer(_ metadata.MD) {
	return
}

func (t *testStream) Send(data *spb.ServiceData) error {
	t.services = append(t.services, data)
	return nil
}

func (t *testStream) GetServices() []string {
	var result []string
	for _, service := range t.services {
		result = append(result, service.Service.Name)
	}

	return result
}
