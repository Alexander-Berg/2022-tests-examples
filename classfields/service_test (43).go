package scaler

import (
	"fmt"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/approve"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	dModel "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/common"
	serviceMap "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	mModel "github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/prometheus/client_golang/api"
	v1 "github.com/prometheus/client_golang/api/prometheus/v1"
	"github.com/prometheus/common/model"
	"github.com/stretchr/testify/assert"
	tMock "github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

var (
	date = time.Date(2011, 1, 1, 0, 0, 0, 0, time.Local)

	sMap = &serviceMap.ServiceMap{
		Name: "service-test",
		Type: serviceMap.ServiceType_service,
	}
)

func TestService_calculateCpu(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	serviceName := "test-service"
	serviceName2 := "test-service2"
	noDataService := "no-data"
	noDataService2 := "no-data2"
	promApiMock := &mock.PrometheusApi{}
	r := v1.Range{
		Start: date.Add(-24 * time.Hour),
		End:   date,
		Step:  30 * time.Second,
	}
	promApiMock.On("QueryRange", tMock.Anything, fmt.Sprintf(CalculateCPUReq, serviceName), r).
		Return(promApiMock.GenerateMatrix(1.2, 199), v1.Warnings{}, nil).Once()
	promApiMock.On("QueryRange", tMock.Anything, fmt.Sprintf(CalculateCPUReq, serviceName2), r).
		Return(promApiMock.GenerateMatrix(1.2, 499), v1.Warnings{"abc"}, nil).Once()
	promApiMock.On("QueryRange", tMock.Anything, fmt.Sprintf(CalculateCPUReq, noDataService), r).
		Return(model.Matrix{}, v1.Warnings{"abc"}, nil).Once()
	promApiMock.On("QueryRange", tMock.Anything, fmt.Sprintf(CalculateCPUReq, noDataService2), r).
		Return(promApiMock.GenerateMatrix(), v1.Warnings{}, nil).Once()
	defer promApiMock.AssertExpectations(t)

	log := test.NewLogger(t)
	service := NewService(nil, nil, promApiMock, promApiMock, NewConf(), db, log)

	cpu, err := service.calculateCpu(common.Prod, serviceName, date)
	require.NoError(t, err)
	require.Equal(t, 200, cpu)

	cpu, err = service.calculateCpu(common.Test, serviceName2, date)
	require.NoError(t, err)
	require.Equal(t, 500, cpu)

	cpu, err = service.calculateCpu(common.Prod, noDataService, date)
	require.NoError(t, err)
	require.Zero(t, cpu)

	cpu, err = service.calculateCpu(common.Prod, noDataService2, date)
	require.NoError(t, err)
	require.Zero(t, cpu)
}

func TestService_calculateCpu_Error(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	promApiMock := &mock.PrometheusApi{}
	log := test.NewLogger(t)
	service := NewService(nil, nil, promApiMock, nil, NewConf(), db, log)
	promApiMock.On("QueryRange", tMock.Anything, fmt.Sprintf(CalculateCPUReq, "wrong-type"), tMock.Anything).
		Return(&model.Scalar{}, v1.Warnings{}, nil)
	promApiMock.On("QueryRange", tMock.Anything, fmt.Sprintf(CalculateCPUReq, "req-failed"), tMock.Anything).
		Return(nil, nil, fmt.Errorf("prom error"))

	_, err := service.calculateCpu(common.Prod, "wrong-type", time.Now())
	require.Error(t, err)
	_, err = service.calculateCpu(common.Prod, "req-failed", time.Now())
	require.Error(t, err)
}

func TestTask_round(t *testing.T) {
	tests := []struct {
		cpu  float64
		want int
	}{
		{0, 100},
		{1, 100},
		{100, 100},
		{101, 200},
		{500, 500},
		{501, 600},
		{1000, 1000},
		{1001, 1500},
		{1500, 1500},
		{1501, 2000},
		{10000, 10000},
		{10001, 11000},
		{11000, 11000},
		{11001, 12000},
	}
	for _, tt := range tests {
		t.Run(
			fmt.Sprintf("%v", tt), func(t *testing.T) {
				s := &Service{}
				if got := s.round(tt.cpu); got != tt.want {
					t.Errorf("round() = %v, want %v", got, tt.want)
				}
			},
		)
	}
}

func TestService_GetOverridedCpu(t *testing.T) {
	test.InitTestEnv()

	var man = func(autoCPU bool, CPU int) *mModel.Manifest {
		return &mModel.Manifest{
			Name: "test-service",
			Resources: mModel.Resources{
				AutoCpu: autoCPU,
				CPU:     CPU,
			},
		}
	}
	var calc = func(cpu int) *CalculatedResource {
		return &CalculatedResource{
			Layer:   common.Prod,
			Service: "test-service",
			CPU:     cpu,
			At:      time.Now(),
		}
	}

	testCases := []struct {
		name       string
		layer      common.Layer
		manifest   *mModel.Manifest
		calculated *CalculatedResource
		wantCPU    int
		warn       error
	}{
		{
			name:       "auto-cpu",
			layer:      common.Prod,
			manifest:   man(true, 300),
			calculated: calc(200),
			wantCPU:    200,
			warn:       nil,
		},
		{
			name:       "no auto-cpu",
			layer:      common.Prod,
			manifest:   man(false, 200),
			calculated: calc(300),
			wantCPU:    0,
			warn:       nil,
		},
		{
			name:       "cpu in manifest fallback",
			layer:      common.Test,
			manifest:   man(true, 200),
			calculated: calc(300),
			wantCPU:    200,
			warn:       ErrCpuNoDataUseManualCPU,
		},
		{
			name:       "max cpu limited",
			layer:      common.Prod,
			manifest:   man(true, 200),
			calculated: calc(1300),
			wantCPU:    500,
			warn:       ErrCpuLimited,
		},
		{
			name:       "max cpu fallback: missing calculate",
			layer:      common.Prod,
			manifest:   man(true, 0),
			calculated: nil,
			wantCPU:    500,
			warn:       ErrCpuNoDataUseMaxCPU,
		},
		{
			name:     "max cpu fallback: old entry",
			layer:    common.Prod,
			manifest: man(true, 0),
			calculated: &CalculatedResource{Service: "test-service", CPU: 400, Layer: common.Prod,
				At: time.Now().AddDate(0, 0, -8)},
			wantCPU: 500,
			warn:    ErrCpuNoDataUseMaxCPU,
		},
		{
			name:       "max cpu fallback: wrong service name",
			layer:      common.Prod,
			manifest:   man(true, 0),
			calculated: &CalculatedResource{Service: "abcd", CPU: 400, Layer: common.Prod, At: time.Now()},
			wantCPU:    500,
			warn:       ErrCpuNoDataUseMaxCPU,
		},
		{
			name:       "max cpu fallback: wrong layer",
			layer:      common.Prod,
			manifest:   man(true, 0),
			calculated: &CalculatedResource{Service: "test-service", CPU: 400, Layer: common.Test, At: time.Now()},
			wantCPU:    500,
			warn:       ErrCpuNoDataUseMaxCPU,
		},
	}

	log := test.NewLogger(t)

	for _, tt := range testCases {
		t.Run(tt.name, func(t *testing.T) {

			db := test_db.NewSeparatedDb(t)
			s := NewStorage(db, log)
			conf := NewConf()
			conf.MaxCpu = 500
			svc := NewService(nil, nil, nil, nil, conf, db, log)
			if tt.calculated != nil {
				err := s.Save(tt.calculated)
				require.NoError(t, err)
			}

			cpu, warn, err := svc.GetOverridedCpu(tt.layer, tt.manifest, sMap)
			require.NoError(t, err)
			require.Equal(t, tt.warn, warn)
			require.Equal(t, tt.wantCPU, cpu)
		})
	}
}

func TestGetOverridedCpu(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)
	require.NoError(t, db.GormDb.AutoMigrate(staff.User{}, dModel.Deployment{}))
	includeService := include.NewService(db, log)
	manSrv := manifest.NewService(db, log, parser.NewService(log, nil), includeService)
	dataSvc := data.NewService(db, log)
	service := NewService(dataSvc, manSrv, nil, nil, NewConf(), db, log)

	m := &mModel.Manifest{Name: "aglomerat-scheduler"}
	sMap := &serviceMap.ServiceMap{Name: "aglomerat-scheduler", Type: serviceMap.ServiceType_service}

	cpu, warn, err := service.GetOverridedCpu(common.Test, m, sMap)
	assert.Nil(t, err)
	assert.Equal(t, ErrCpuNoDataUseMaxCPU, warn)
	assert.Equal(t, service.conf.MaxCpu, cpu)

	require.NoError(t, service.scalerStorage.Save(&CalculatedResource{
		Layer:   common.Test,
		Service: "aglomerat-scheduler",
		CPU:     100,
		At:      time.Now().Add(-12 * time.Hour),
	}))
	cpu, warn, err = service.GetOverridedCpu(common.Test, m, sMap)
	assert.Nil(t, err)
	assert.Nil(t, warn)
	assert.Equal(t, 100, cpu)
	require.NoError(t, service.scalerStorage.Save(&CalculatedResource{
		Layer:   common.Test,
		Service: "aglomerat-scheduler",
		CPU:     500,
		At:      time.Now().Add(-24 * time.Hour),
	}))
	require.NoError(t, service.scalerStorage.Save(&CalculatedResource{
		Layer:   common.Test,
		Service: "aglomerat-scheduler",
		CPU:     700,
		At:      time.Now().Add(-48 * time.Hour),
	}))
	require.NoError(t, service.scalerStorage.Save(&CalculatedResource{
		Layer:   common.Test,
		Service: "aglomerat-scheduler",
		CPU:     600,
		At:      time.Now().Add(-72 * time.Hour),
	}))
	cpu, warn, err = service.GetOverridedCpu(common.Test, m, sMap)
	assert.Nil(t, err)
	assert.Nil(t, warn)
	assert.Equal(t, 700, cpu)
}

func TestService_InstancesChanged(t *testing.T) {
	test.InitTestEnv()

	testCases := []struct {
		name    string
		dc1     string
		dc2     string
		changed bool
	}{
		{
			name:    "dc change",
			dc1:     "{sas: {count: 1}}",
			dc2:     "{myt: {count: 1}}",
			changed: false,
		},
		{
			name:    "dc split",
			dc1:     "{sas: {count: 2}}",
			dc2:     "{myt: {count: 1}, sas: {count: 1}}",
			changed: false,
		},
		{
			name:    "dc add",
			dc1:     "{sas: {count: 1}}",
			dc2:     "{myt: {count: 1}, sas: {count: 1}}",
			changed: true,
		},
		{
			name:    "count add",
			dc1:     "{sas: {count: 1}}",
			dc2:     "{sas: {count: 2}}",
			changed: true,
		},
		{
			name:    "dc change + count add",
			dc1:     "{sas: {count: 1}}",
			dc2:     "{myt: {count: 2}}",
			changed: true,
		},
	}

	getMan := func(dc string) []byte {
		man := `
name: test-service
image: test-service
general:
  datacenters: %s
  upgrade: { parallel: 1 }
  resources:
    cpu: 200
    memory: 256`
		return []byte(fmt.Sprintf(man, dc))
	}
	getDeployment := func(deployManifestId int64, startDate time.Time) *dModel.Deployment {
		return &dModel.Deployment{
			DeployManifestID: deployManifestId,
			Type:             common.Run,
			Layer:            common.Prod,
			Name:             "test-service",
			StartDate:        startDate,
		}
	}

	for _, tt := range testCases {
		t.Run(tt.name, func(t *testing.T) {
			// region prepare
			tt := tt

			db := test_db.NewSeparatedDb(t)
			db.GormDb.DisableForeignKeyConstraintWhenMigrating = true

			log := test.NewLogger(t)
			includeService := include.NewService(db, log)
			manSrv := manifest.NewService(db, log, parser.NewService(log, nil), includeService)
			dStorageSrv := dModel.NewStorage(db, log)
			dataSvc := data.NewService(db, log)
			service := NewService(dataSvc, manSrv, nil, nil, NewConf(), db, log)
			staffApi := staffapi.NewApi(staffapi.NewConf(), log)
			staffService := staff.NewService(db, staffApi, log)
			approve.NewService(db, log, staffService)
			// endregion

			require.NoError(t, manSrv.ReadAndSave(getMan(tt.dc1), test.AtomicNextUint(), "deploy/test-service.yml"))
			_, man1ID, err := manSrv.GetByNameWithId(common.Prod, "test-service")
			require.NoError(t, err)

			deployment := getDeployment(man1ID, time.Now().Add(-time.Hour))
			require.NoError(t, dStorageSrv.Save(deployment))

			require.NoError(t, manSrv.ReadAndSave(getMan(tt.dc2), test.AtomicNextUint(), "deploy/test-service.yml"))
			man2, man2ID, err := manSrv.GetByNameWithId(common.Prod, "test-service")

			changed, err := service.InstancesChanged(common.Prod, "test-service", man2)
			require.NoError(t, err)
			require.Equal(t, tt.changed, changed)

			require.NoError(t, err)
			deployment2 := getDeployment(man2ID, time.Now())
			require.NoError(t, dStorageSrv.Save(deployment2))

			changed2, err := service.InstancesChanged(common.Prod, "test-service", nil)
			require.NoError(t, err)
			require.Equal(t, tt.changed, changed2)
		})
	}
}

func TestService_CalculateResources(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	log := test.NewLogger(t)
	promApiMock := &mock.PrometheusApi{}
	promApiMock.On("QueryRange", tMock.Anything, tMock.Anything, tMock.Anything).
		Return(promApiMock.GenerateMatrix(1.2, 199), v1.Warnings{}, nil)

	service := NewService(nil, nil, promApiMock, nil, NewConf(), db, log)
	resource, err := service.CalculateResources(common.Prod, "test-service", time.Now())
	require.NoError(t, err)
	require.Equal(t, 200, resource.CPU)
}

func TestService_CalculateResources_NoData(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	log := test.NewLogger(t)
	promApiMock := &mock.PrometheusApi{}
	promApiMock.On("QueryRange", tMock.Anything, tMock.Anything, tMock.Anything).
		Return(promApiMock.GenerateMatrix(), v1.Warnings{}, nil)

	service := NewService(nil, nil, promApiMock, nil, NewConf(), db, log)
	resource, err := service.CalculateResources(common.Prod, "test-service", time.Now())
	require.NoError(t, err)
	require.Nil(t, resource)
}

func TestService_ClusterCPU(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	log := test.NewLogger(t)
	promClient, _ := api.NewClient(api.Config{
		Address: "https://prometheus.vertis.yandex.net",
	})
	promApiProd := v1.NewAPI(promClient)
	service := NewService(nil, nil, promApiProd, nil, NewConf(), db, log)
	service.ClusterCPU(common.Prod, "sas", AllocatedCPU)
	service.ClusterCPU(common.Prod, "sas", UnallocatedCPU)
}
