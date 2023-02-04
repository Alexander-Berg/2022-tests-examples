package service_map

import (
	"errors"
	"fmt"
	"sort"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/common"
	changeConf "github.com/YandexClassifieds/shiva/pb/shiva/events/change_conf"
	sm "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/golang/protobuf/proto"
	"github.com/golang/protobuf/ptypes"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"
)

var yamlFile = `
name: shiva
description: Deployment system
is_external: false
owner: https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
owners:
  - https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
startrek: VOID
design_doc:
src:  https://github.com/YandexClassifieds/shiva
provides:
  - name: deploy
    protocol: grpc
    port: 1337
    description: deploy api
    api_doc: https://wiki.yandex-team.ru/vertis-admin/shiva/
  - name: tcp-thing
    protocol: tcp
    port: 1234
    sla_rps: 20
    sla_timing_mean: 10
    sla_timing_p99: 42ns
    api_doc: https://wiki.yandex-team.ru/vertis-admin/shiva/
  - name: http-api
    protocol: http
    port: 80
  - name: old
    protocol: http
    port: 5000
    description: http api with old address
    api_doc: https://wiki.yandex-team.ru/vertis-admin/shiva/
    old_address_prod: realty-seller-api.vrts-slb.prod.vertis.yandex.net
    old_address_test: realty-seller-api.vrts-slb.test.vertis.yandex.net
depends_on:
  - service_name: other_service
    interface_name: api
    expected_rps: 100
    failure_reaction:
      missing: fatal
      timeout: graceful
      unexpected_result: severe
      errors: fatal
`

func TestNewStorage(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	st := newStorage(db, test.NewLogger(t))
	assert.IsType(t, &storage{}, st)
	assert.NotNil(t, st.base.DB)
	assert.NotNil(t, st.base.Log)
}

func TestStorage_ReadAndSave(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	s := NewService(db, test.NewLogger(t), service_change.NewNotificationMock())

	err := s.ReadAndSave([]byte(yamlFile), 10, "maps/shiva.yml")
	require.NoError(t, err)

	svc, id, err := s.GetByFullPath("maps/shiva.yml")
	require.NoError(t, err)

	assert.True(t, id > 0)
	assert.Equal(t, "shiva", svc.Name)
	assert.Equal(t, "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra", svc.Owner)
	assert.Equal(t, "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra", svc.Owners[0])
	assert.Equal(t, "VOID", svc.Startrek)
	assert.Equal(t, false, svc.IsExternal)
	assert.Equal(t, "https://github.com/YandexClassifieds/shiva", svc.Src)
	assert.Len(t, svc.Provides, 4)
	expectedProvides := []*sm.ServiceProvides{
		{
			Name:        "deploy",
			Description: "deploy api",
			Protocol:    sm.ServiceProvides_grpc,
			Port:        1337,
			ApiDoc:      "https://wiki.yandex-team.ru/vertis-admin/shiva/",
			SlaRps:      0,
		},
		{
			Name:          "tcp-thing",
			Description:   "",
			Protocol:      sm.ServiceProvides_tcp,
			Port:          1234,
			ApiDoc:        "https://wiki.yandex-team.ru/vertis-admin/shiva/",
			SlaRps:        20,
			SlaTimingMean: ptypes.DurationProto(10 * time.Millisecond),
			SlaTimingP99:  ptypes.DurationProto(42 * time.Nanosecond),
		},
		{
			Name:     "http-api",
			Protocol: sm.ServiceProvides_http,
			Port:     80,
		},
		{
			Name:           "old",
			Protocol:       sm.ServiceProvides_http,
			Port:           5000,
			Description:    "http api with old address",
			ApiDoc:         "https://wiki.yandex-team.ru/vertis-admin/shiva/",
			OldAddressProd: "realty-seller-api.vrts-slb.prod.vertis.yandex.net",
			OldAddressTest: "realty-seller-api.vrts-slb.test.vertis.yandex.net",
		},
	}
	assert.Equal(t, expectedProvides, svc.Provides)

	assert.Len(t, svc.DependsOn, 1)
	dep0 := svc.DependsOn[0]
	assert.Equal(t, "other_service", dep0.GetPath())
	assert.Equal(t, "api", dep0.InterfaceName)
	assert.EqualValues(t, 100, dep0.ExpectedRps)
	assert.Equal(t, &sm.FailureReaction{
		Missing:          sm.FailureReaction_Fatal,
		Timeout:          sm.FailureReaction_Graceful,
		UnexpectedResult: sm.FailureReaction_Severe,
		Errors:           sm.FailureReaction_Fatal,
	}, dep0.FailureReaction)
}

func TestService_GetCurrentItems(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	s := NewService(db, test.NewLogger(t), service_change.NewNotificationMock())
	require.NoError(t, s.storage.Create(&Data{Name: "test", Path: "maps/test.yml"}))
	require.NoError(t, s.storage.Create(&Data{Name: "test2", Path: "maps/test2.yml"}))

	paths, err := s.GetCurrentItems()
	require.NoError(t, err)
	assert.Len(t, paths, 2)
	assert.Contains(t, paths, "maps/test.yml")
	assert.Contains(t, paths, "maps/test2.yml")
}

func TestService_Update(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	nMock := service_change.NewNotificationMock()
	s := NewService(db, test.NewLogger(t), nMock)

	// test new
	err := s.ReadAndSave([]byte(`
name: test
`), 10, "maps/test.yml")
	require.NoError(t, err)
	_, err = s.storage.GetByPath("maps/test.yml")
	require.NoError(t, err)
	nMock.AssertSMapCalls(t, 1, 0, 0)

	// test skip update
	err = s.ReadAndSave([]byte(`
name: test
`), 10, "maps/test.yml")
	require.NoError(t, err)
	_, err = s.storage.GetByPath("maps/test.yml")
	require.NoError(t, err)
	nMock.AssertSMapCalls(t, 1, 0, 0)

	// test update
	err = s.ReadAndSave([]byte(`
name: test
description: test
`), 11, "maps/test.yml")
	require.NoError(t, err)
	_, err = s.storage.GetByPath("maps/test.yml")
	require.NoError(t, err)
	nMock.AssertSMapCalls(t, 1, 1, 0)
}

func TestService_DeleteItemsByPath(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	nMock := service_change.NewNotificationMock()
	s := NewService(db, test.NewLogger(t), nMock)
	err := s.ReadAndSave([]byte(`
name: test
`), 10, "maps/test.yml")
	require.NoError(t, err)
	err = s.ReadAndSave([]byte(`
name: test2
`), 10, "maps/test2.yml")
	require.NoError(t, err)
	nMock.AssertSMapCalls(t, 2, 0, 0)
	_, err = s.storage.GetByPath("maps/test.yml")
	require.NoError(t, err)
	_, err = s.storage.GetByPath("maps/test2.yml")
	require.NoError(t, err)

	err = s.DeleteItemByPath("maps/test.yml")
	require.NoError(t, err)

	_, err = s.storage.GetByPath("maps/test.yml")
	assert.True(t, errors.Is(err, common.ErrNotFound))
	_, err = s.storage.GetByPath("maps/test2.yml")
	require.NoError(t, err)
	nMock.AssertSMapCalls(t, 2, 0, 1)
	assert.Equal(t, 1, nMock.DeletedC["test"])
}

func TestStorage_DoubleSave(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	nMock := service_change.NewNotificationMock()
	s := NewService(db, test.NewLogger(t), nMock)
	err := s.ReadAndSave([]byte(`
name: test
`), 10, "maps/test.yml")
	require.NoError(t, err)
	_, err = s.storage.GetByPath("maps/test.yml")
	require.NoError(t, err)

	err = s.ReadAndSave([]byte(`
name: test
description: My description
`), 11, "maps/test.yml")
	require.NoError(t, err)
	result, err := s.storage.GetByPath("maps/test.yml")
	require.NoError(t, err)
	assert.Equal(t, "test", result.Name)
	assert.Equal(t, "My description", result.Proto.Data.Description)
	assert.False(t, result.DeletedAt.Valid)
}

func TestStorage_GetAll(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	s := NewService(db, test.NewLogger(t), service_change.NewNotificationMock())
	// soft delete check
	ts := time.Now()
	dupSvc := Data{
		Name: "service-one",
		DeletedAt: gorm.DeletedAt{
			Time:  ts,
			Valid: true,
		},
		Path: "maps/service-one.yml",
	}
	require.NoError(t, s.storage.Create(&dupSvc))

	expectedServices := []*sm.ServiceMap{
		{
			Name:        "service-one",
			Description: "test svc",
			Startrek:    "VERTISADMIN",
			DependsOn: []*sm.ServiceDependency{
				{Service: "svc2"},
			},
			Path: "maps/service-one.yml",
		},
		{
			Name:        "svc2",
			Description: "service two",
			Path:        "maps/svc2.yml",
		},
		{
			Name:        "c",
			Description: "stub",
			Path:        "maps/c.yml",
		},
		{
			Name: "mysql",
			Type: sm.ServiceType_mdb_mysql,
			MdbCluster: &sm.MDBCluster{
				TestId: "mdb00000000000",
				ProdId: "mdb11111111111",
			},
			Path: "maps/mysql/mysql.yml",
		},
		{
			Name: "postgresql",
			Type: sm.ServiceType_mdb_postgresql,
			MdbCluster: &sm.MDBCluster{
				TestId: "mdb00000000000",
				ProdId: "mdb11111111111",
			},
			Path: "maps/postgresql/postgresql.yml",
		},
		{
			Name: "kafka",
			Type: sm.ServiceType_kafka,
			MdbCluster: &sm.MDBCluster{
				TestId: "mdb00000000000",
				ProdId: "mdb11111111111",
			},
			Path: "maps/kafka/shared-01.yml",
		},
	}
	for _, svc := range expectedServices {
		require.NoError(t, s.storage.Create(&Data{
			Name:  svc.Name,
			Proto: WrappedMap{svc},
			Path:  svc.Path,
		}))
	}

	services, err := s.GetServices(nil)
	if err != nil {
		t.Fatal(err)
	}
	assertServices(t, expectedServices, services)
}

func TestStorage_GetByTypes(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	s := NewService(db, test.NewLogger(t), service_change.NewNotificationMock())

	Services := []*sm.ServiceMap{
		{
			Name:        "service-one",
			Description: "test svc",
			Startrek:    "VERTISADMIN",
			DependsOn: []*sm.ServiceDependency{
				{Service: "svc2"},
			},
			Path: "maps/service-one.yml",
		},
		{
			Name:        "external",
			Description: "external svc",
			Type:        sm.ServiceType_external,
			Path:        "maps/external.yml",
		},
		{
			Name: "mysql",
			Type: sm.ServiceType_mysql,
			Path: "maps/mysql/mysql.yml",
		},
		{
			Name: "mdb-mysql",
			Type: sm.ServiceType_mdb_mysql,
			MdbCluster: &sm.MDBCluster{
				TestId: "mdb00000000000",
				ProdId: "mdb11111111111",
			},
			Path: "maps/mysql/mdb-mysql.yml",
		},
		{
			Name: "kafka",
			Type: sm.ServiceType_kafka,
			MdbCluster: &sm.MDBCluster{
				TestId: "mdb00000000000",
				ProdId: "mdb11111111111",
			},
			Path: "maps/kafka/shared-01.yml",
		},
		{
			Name: "mdb-postgresql",
			Type: sm.ServiceType_mdb_postgresql,
			MdbCluster: &sm.MDBCluster{
				TestId: "mdb00000000000",
				ProdId: "mdb11111111111",
			},
			Path: "maps/postgresql/mdb-postgresql.yml",
		},
	}

	expectedServices := []*sm.ServiceMap{
		{
			Name:        "service-one",
			Description: "test svc",
			Startrek:    "VERTISADMIN",
			DependsOn: []*sm.ServiceDependency{
				{Service: "svc2"},
			},
			Path: "maps/service-one.yml",
		},
		{
			Name:        "external",
			Description: "external svc",
			Type:        sm.ServiceType_external,
			Path:        "maps/external.yml",
		},
		{
			Name: "mdb-mysql",
			Type: sm.ServiceType_mdb_mysql,
			MdbCluster: &sm.MDBCluster{
				TestId: "mdb00000000000",
				ProdId: "mdb11111111111",
			},
			Path: "maps/mysql/mdb-mysql.yml",
		},
	}

	for _, svc := range Services {
		require.NoError(t, s.storage.Create(&Data{
			Name:  svc.Name,
			Type:  svc.Type,
			Proto: WrappedMap{svc},
			Path:  svc.Path,
		}))
	}

	services, err := s.GetServices([]sm.ServiceType{
		sm.ServiceType_service,
		sm.ServiceType_external,
		sm.ServiceType_mdb_mysql,
	})
	if err != nil {
		t.Fatal(err)
	}
	assertServices(t, expectedServices, services)
}

func TestGetByFullPath(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	s := NewService(db, test.NewLogger(t), service_change.NewNotificationMock())

	expectedSvc := &sm.ServiceMap{
		Name:     "test-service",
		Startrek: "VERTISADMIN",
	}
	require.NoError(t, s.storage.Create(&Data{
		Name:  expectedSvc.Name,
		Proto: WrappedMap{expectedSvc},
		Path:  "maps/test-service.yml",
	}))
	svc, id, err := s.GetByFullPath("maps/test-service.yml")
	if err != nil {
		t.Fatal(err)
	}
	assert.True(t, id > 0)
	expectedSvc.Path = svc.Path
	assertServices(t, []*sm.ServiceMap{expectedSvc}, []*sm.ServiceMap{svc})

	svc, id, err = s.GetByFullPath(sm.ToFullPath(expectedSvc.Name))
	if err != nil {
		t.Fatal(err)
	}
	assert.True(t, id > 0)
	expectedSvc.Path = svc.Path
	assertServices(t, []*sm.ServiceMap{expectedSvc}, []*sm.ServiceMap{svc})
}

func TestGetByFullPath_mysql(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	s := NewService(db, test.NewLogger(t), service_change.NewNotificationMock())

	expectedSvc := &sm.ServiceMap{
		Name: "test-mysql",
		Type: sm.ServiceType_mdb_mysql,
		MdbCluster: &sm.MDBCluster{
			TestId: "mdb00000000000",
			ProdId: "mdb11111111111",
		},
	}
	require.NoError(t, s.storage.Create(&Data{
		Name:  expectedSvc.Name,
		Proto: WrappedMap{expectedSvc},
		Path:  "maps/mysql/test-mysql.yml",
	}))

	svc, id, err := s.GetByFullPath("maps/mysql/test-mysql.yml")
	if err != nil {
		t.Fatal(err)
	}
	assert.True(t, id > 0)
	expectedSvc.Path = svc.Path
	assertServices(t, []*sm.ServiceMap{expectedSvc}, []*sm.ServiceMap{svc})

	svc, id, err = s.GetByFullPath(sm.ToFullPath("mysql/" + expectedSvc.Name))
	if err != nil {
		t.Fatal(err)
	}
	assert.True(t, id > 0)
	expectedSvc.Path = svc.Path
	assertServices(t, []*sm.ServiceMap{expectedSvc}, []*sm.ServiceMap{svc})
}

func TestNewNotification(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	nMock := service_change.NewNotificationMock()
	s := NewService(test_db.NewSeparatedDb(t), test.NewLogger(t), nMock)
	makeSimpleMap(t, s, "s_service_1", sm.ServiceType_service.String())
	makeSimpleMap(t, s, "s_service_2", sm.ServiceType_service.String())
	makeSimpleMap(t, s, "s_external", sm.ServiceType_external.String())
	makeSimpleMap(t, s, "s_conductor", sm.ServiceType_conductor.String())
	makeSimpleMap(t, s, "s_mdb_mysql", sm.ServiceType_mdb_mysql.String())
	makeSimpleMap(t, s, "s_jenkins", sm.ServiceType_jenkins.String())
	makeSimpleMap(t, s, "s_kafka", sm.ServiceType_kafka.String())
	nMock = service_change.NewNotificationMock()
	s.notification = nMock

	require.NoError(t, s.NewNotification("sentry", changeConf.ChangeType_NEW, sm.ServiceType_service, nil))

	assert.Len(t, nMock.NewC, 2)
	assert.Len(t, nMock.DeletedC, 0)
	assert.Len(t, nMock.UpdatedC, 0)
	v, ok := nMock.NewC["s_service_1"]
	assert.True(t, ok)
	assert.Equal(t, 1, v)
	v, ok = nMock.NewC["s_service_2"]
	assert.True(t, ok)
	assert.Equal(t, 1, v)
}

func TestNewNotificationWithNames(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	nMock := service_change.NewNotificationMock()
	s := NewService(test_db.NewSeparatedDb(t), test.NewLogger(t), nMock)
	makeSimpleMap(t, s, "s_service_1", sm.ServiceType_service.String())
	makeSimpleMap(t, s, "s_service_2", sm.ServiceType_service.String())
	makeSimpleMap(t, s, "s_service_3", sm.ServiceType_service.String())
	makeSimpleMap(t, s, "s_external", sm.ServiceType_external.String())
	nMock = service_change.NewNotificationMock()
	s.notification = nMock

	require.NoError(t, s.NewNotification("sentry", changeConf.ChangeType_NEW, sm.ServiceType_service, []string{"s_service_2", "s_service_3"}))

	assert.Len(t, nMock.NewC, 2)
	assert.Len(t, nMock.DeletedC, 0)
	assert.Len(t, nMock.UpdatedC, 0)
	v, ok := nMock.NewC["s_service_1"]
	assert.False(t, ok)
	v, ok = nMock.NewC["s_service_2"]
	assert.True(t, ok)
	v, ok = nMock.NewC["s_service_3"]
	assert.True(t, ok)
	assert.Equal(t, 1, v)
}

func TestGetByName(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	s := NewService(test_db.NewSeparatedDb(t), test.NewLogger(t), service_change.NewNotificationMock())
	expectedSvc := &sm.ServiceMap{
		Name:     "test-service",
		Startrek: "VERTISADMIN",
	}
	require.NoError(t, s.storage.Create(&Data{
		Name:  expectedSvc.Name,
		Proto: WrappedMap{expectedSvc},
		Path:  "maps/test-service.yml",
	}))

	svc, err := s.GetByName("test-service")
	if err != nil {
		t.Fatal(err)
	}

	expectedSvc.Path = svc.Path
	assertServices(t, []*sm.ServiceMap{expectedSvc}, []*sm.ServiceMap{svc})
}

func TestGetByNames(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	s := NewService(test_db.NewSeparatedDb(t), test.NewLogger(t), service_change.NewNotificationMock())
	expectedSmap := &sm.ServiceMap{
		Name:     "test-service",
		Startrek: "VERTISADMIN",
		Type:     sm.ServiceType_external,
	}
	require.NoError(t, s.storage.Create(&Data{
		Name:  expectedSmap.Name,
		Proto: WrappedMap{expectedSmap},
		Path:  "maps/test-service.yml",
		Type:  sm.ServiceType_external,
	}))

	sMaps, err := s.GetByNames([]string{"test-service"}, sm.ServiceType_external)
	if err != nil {
		t.Fatal(err)
	}

	require.Equal(t, 1, len(sMaps))

	expectedSmap.Path = sMaps[0].Path
	assertServices(t, []*sm.ServiceMap{expectedSmap}, sMaps)
}

func TestGetByName_mysql(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	s := NewService(test_db.NewDb(t), test.NewLogger(t), service_change.NewNotificationMock())
	expectedSvc := &sm.ServiceMap{
		Name: "test-mysql",
		Type: sm.ServiceType_mdb_mysql,
		MdbCluster: &sm.MDBCluster{
			TestId: "mdb00000000000",
			ProdId: "mdb11111111111",
		},
	}
	require.NoError(t, s.storage.Create(&Data{
		Name:  expectedSvc.Name,
		Proto: WrappedMap{expectedSvc},
		Path:  "maps/mysql/test-mysql.yml",
	}))

	svc, err := s.GetByName("mysql/test-mysql")
	if err != nil {
		t.Fatal(err)
	}

	expectedSvc.Revision = svc.Revision
	expectedSvc.Path = svc.Path
	assertServices(t, []*sm.ServiceMap{expectedSvc}, []*sm.ServiceMap{svc})
}

func TestGetMapsByKeys(t *testing.T) {
	s := prepareService(t)

	testCases := []struct {
		name     string
		mapKeys  []*sm.MapKey
		expected []*sm.ServiceMap
		err      error
	}{
		{
			name: "simple get",
			mapKeys: []*sm.MapKey{
				{
					Service: "service-one",
				},
				{
					Service: "mdb-mysql",
					Type:    sm.ServiceType_mdb_mysql,
				},
			},
			expected: []*sm.ServiceMap{
				{
					Name: "service-one",
					Path: "maps/service-one.yml",
				},
				{
					Name: "mdb-mysql",
					Type: sm.ServiceType_mdb_mysql,
					MdbCluster: &sm.MDBCluster{
						TestId: "mdb00000000000",
						ProdId: "mdb11111111111",
					},
					Path: "maps/mysql/mdb-mysql.yml",
				},
			},
		},
		{
			name: "with duplicates",
			mapKeys: []*sm.MapKey{
				{
					Service: "service-one",
				},
				{
					Service: "mdb-mysql",
					Type:    sm.ServiceType_mdb_mysql,
				},
				{
					Service: "mdb-mysql",
					Type:    sm.ServiceType_mdb_mysql,
				},
				{
					Service: "service-one",
				},
			},
			expected: []*sm.ServiceMap{
				{
					Name: "service-one",
					Path: "maps/service-one.yml",
				},
				{
					Name: "mdb-mysql",
					Type: sm.ServiceType_mdb_mysql,
					MdbCluster: &sm.MDBCluster{
						TestId: "mdb00000000000",
						ProdId: "mdb11111111111",
					},
					Path: "maps/mysql/mdb-mysql.yml",
				},
			},
		},
		{
			name: "not found service",
			mapKeys: []*sm.MapKey{
				{
					Service: "service-one",
				},
				{
					Service: "not-existed-svc",
				},
			},
			err: common.ErrNotFound,
		},
		{
			name:     "empty get",
			mapKeys:  []*sm.MapKey{},
			expected: nil,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			services, err := s.GetMapsByKeys(tc.mapKeys)
			require.ErrorIs(t, err, tc.err)

			assertServices(t, tc.expected, services)
		})
	}
}

func TestGetMapsByPaths(t *testing.T) {
	s := prepareService(t)

	testCases := []struct {
		name     string
		paths    []string
		expected []*sm.ServiceMap
		err      error
	}{
		{
			name:  "simple get",
			paths: []string{"maps/service-one.yml", "maps/mysql/mdb-mysql.yml"},
			expected: []*sm.ServiceMap{
				{
					Name: "service-one",
					Path: "maps/service-one.yml",
				},
				{
					Name: "mdb-mysql",
					Type: sm.ServiceType_mdb_mysql,
					MdbCluster: &sm.MDBCluster{
						TestId: "mdb00000000000",
						ProdId: "mdb11111111111",
					},
					Path: "maps/mysql/mdb-mysql.yml",
				},
			},
		},
		{
			name:  "with duplicates",
			paths: []string{"maps/service-one.yml", "maps/mysql/mdb-mysql.yml", "maps/service-one.yml", "maps/mysql/mdb-mysql.yml"},
			expected: []*sm.ServiceMap{
				{
					Name: "service-one",
					Path: "maps/service-one.yml",
				},
				{
					Name: "mdb-mysql",
					Type: sm.ServiceType_mdb_mysql,
					MdbCluster: &sm.MDBCluster{
						TestId: "mdb00000000000",
						ProdId: "mdb11111111111",
					},
					Path: "maps/mysql/mdb-mysql.yml",
				},
			},
		},
		{
			name:  "not found service",
			paths: []string{"maps/service-one.yml", "maps/mysql/mdb-mysql.yml", "maps/service-one.yml", "maps/mysql/not-exist.yml"},
			err:   common.ErrNotFound,
		},
		{
			name:     "empty get",
			paths:    []string{},
			expected: nil,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			services, err := s.GetMapsByPaths(tc.paths)
			require.ErrorIs(t, err, tc.err)

			assertServices(t, tc.expected, services)
		})
	}
}

func makeSimpleMap(t *testing.T, s *Service, name, sType string) {
	mapStr := fmt.Sprintf(`
name: %s
type: %s`, name, sType)
	path := fmt.Sprintf("maps/%s.yml", name)
	require.NoError(t, s.ReadAndSave([]byte(mapStr), test.AtomicNextUint(), path))
}

func prepareService(t *testing.T) *Service {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	s := NewService(db, test.NewLogger(t), service_change.NewNotificationMock())

	Services := []*sm.ServiceMap{
		{
			Name: "service-one",
			Path: "maps/service-one.yml",
		},
		{
			Name: "external",
			Type: sm.ServiceType_external,
			Path: "maps/external.yml",
		},
		{
			Name: "mysql",
			Type: sm.ServiceType_mysql,
			Path: "maps/mysql/mysql.yml",
		},
		{
			Name: "mdb-mysql",
			Type: sm.ServiceType_mdb_mysql,
			MdbCluster: &sm.MDBCluster{
				TestId: "mdb00000000000",
				ProdId: "mdb11111111111",
			},
			Path: "maps/mysql/mdb-mysql.yml",
		},
		{
			Name: "mdb-postgresql",
			Type: sm.ServiceType_mdb_postgresql,
			MdbCluster: &sm.MDBCluster{
				TestId: "mdb00000000000",
				ProdId: "mdb11111111111",
			},
			Path: "maps/postgresql/mdb-postgresql.yml",
		},
		{
			Name: "kafka",
			Type: sm.ServiceType_kafka,
			MdbCluster: &sm.MDBCluster{
				TestId: "mdb00000000000",
				ProdId: "mdb11111111111",
			},
			Path: "maps/kafka/shared-01.yml",
		},
		{
			Name: "mdb-mysql",
			Type: sm.ServiceType_service,
			Path: "maps/mdb-mysql.yml",
		},
	}

	for _, svc := range Services {
		require.NoError(t, s.storage.Create(&Data{
			Name:  svc.Name,
			Type:  svc.Type,
			Proto: WrappedMap{svc},
			Path:  svc.Path,
		}))
	}

	return s
}

func assertServices(t *testing.T, expected, actual []*sm.ServiceMap) {
	require.Equal(t, len(expected), len(actual))

	sort.Slice(expected, func(i, j int) bool {
		return expected[i].Name < expected[j].Name
	})
	sort.Slice(actual, func(i, j int) bool {
		return actual[i].Name < actual[j].Name
	})

	for i := range actual {
		expected[i].Revision = actual[i].Revision
		assert.True(t, proto.Equal(expected[i], actual[i]), "expected: %v, got: %v", expected[i], actual[i])
	}
}
