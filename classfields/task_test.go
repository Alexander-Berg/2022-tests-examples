package task

import (
	"fmt"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/approve"
	"github.com/YandexClassifieds/shiva/cmd/shiva/bulk_deployment"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/include_links"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/mocks"
	dModel "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/status"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/override"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scaler"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/storage"
	serviceMap "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/bulk"
	"github.com/YandexClassifieds/shiva/pkg/include"
	inc_data "github.com/YandexClassifieds/shiva/pkg/include/model/data"
	"github.com/YandexClassifieds/shiva/pkg/include/model/link"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/tracker/model/issue"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/mock/election"
	mqMock "github.com/YandexClassifieds/shiva/test/mock/mq"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	v1 "github.com/prometheus/client_golang/api/prometheus/v1"
	"github.com/prometheus/common/model"
	tMock "github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

var (
	mapFile = `
name: %s
description: Deployment system
is_external: false
owners: [https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra]
src: https://github.com/YandexClassifieds/shiva
%s
`
	manFile = `
name: %s
image: %s
general:
  config: { files: [test.yml] }
  datacenters:
    sas: { count: 1 }
  upgrade: { parallel: 1 }
  resources:
    cpu: 500
    memory: 256
    %s
`

	sMap = &serviceMap.ServiceMap{
		Name: "service-test",
		Type: serviceMap.ServiceType_service,
	}
)

type vScaleCpu struct {
	allocatedCpu          int
	unallocatedCpu        int
	calculatedCpuService1 int
	calculatedCpuService2 int
}

func TestTask_enabledServices(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	db.GormDb.DisableForeignKeyConstraintWhenMigrating = true

	log := test.NewLogger(t)
	mapS := service_map.NewService(db, log, service_change.NewNotificationMock())
	approve.NewService(db, log, nil) // table create
	issue.NewStorage(db, log)        // table create
	includeSvc := include.NewService(db, log)
	manSvc := manifest.NewService(db, log, parser.NewService(log, nil), includeSvc)
	statusSrv := status.NewService(db, log, mapS)
	dataService := data.NewService(db, log)
	overrideSvc := override.NewService(db, log, includeSvc, manSvc)
	writeManifestAndMap(t, db, "test-service", "", "auto_cpu: true")
	writeDeploymentAndStatus(t, db, "test-service", "", common.Prod, nil)
	writeManifestAndMap(t, db, "service-without-auto-cpu", "", "")
	writeDeploymentAndStatus(t, db, "service-without-auto-cpu", "", common.Prod, nil)
	writeManifestAndMap(t, db, "batch", "type: batch", "auto_cpu: true")
	writeDeploymentAndStatus(t, db, "batch", "", common.Prod, nil)

	// TODO: это для того чтобы сымитировать удаление инклуда https://st.yandex-team.ru/VOID-1764
	db.GormDb.Delete(&link.Link{}, "service_name=?", "test-service")
	db.GormDb.Delete(&inc_data.Data{}, "path=?", "test.yml")

	task, err := RunTask(mapS, manSvc, nil, nil, statusSrv, dataService, overrideSvc, nil, NewConf(), log, nil)
	require.NoError(t, err)

	list, err := task.enabledServices(common.Prod)
	require.NoError(t, err)
	require.Len(t, list, 1)
	require.Equal(t, list[0].manifest.Name, "test-service")
}

func TestTask_VScale_ScalingInCpuBoundary(t *testing.T) {
	test.InitTestEnv()

	db, cleanup, task, promApiMock := vscalePrepare(t, vScaleCpu{
		allocatedCpu:          1000,
		unallocatedCpu:        1000,
		calculatedCpuService1: 600,
		calculatedCpuService2: 600,
	})
	defer cleanup()

	err := task.VScale(common.Prod, false)
	require.NoError(t, err)

	var count int64
	err = db.GormDb.Model(bulk_deployment.BulkDeployment{}).Where("type=?", common.Update).Count(&count).Error
	require.NoError(t, err)
	require.Equal(t, int64(2), count)

	bdStatus := []*bulk_deployment.Status{}
	err = db.GormDb.Find(&bdStatus).Error
	require.NoError(t, err)
	var names []string
	for _, status := range bdStatus {
		names = append(names, status.Name)
	}
	require.ElementsMatch(t, names, []string{"test-service", "test-service2"})

	// wait BR for end
	require.Eventually(t, func() bool {
		count := int64(0)
		err = db.GormDb.Model(bulk_deployment.BulkDeployment{}).Where("state=?", bulk.State_Success).Count(&count).Error
		require.NoError(t, err)
		return count == 2
	}, 5*time.Second, time.Second/10)
	promApiMock.AssertExpectations(t)
}

func TestTask_VScale_DryRun(t *testing.T) {
	test.InitTestEnv()

	db, cleanup, task, promApiMock := vscalePrepare(t, vScaleCpu{
		allocatedCpu:          1000,
		unallocatedCpu:        1000,
		calculatedCpuService1: 600,
		calculatedCpuService2: 600,
	})
	defer cleanup()

	err := task.VScale(common.Prod, true)
	require.NoError(t, err)

	count := int64(0)
	err = db.GormDb.Model(bulk_deployment.BulkDeployment{}).Where("type=?", common.Update).Count(&count).Error
	require.NoError(t, err)
	require.Equal(t, int64(0), count)

	require.NotEmpty(t, task.VScaleLog())
	promApiMock.AssertExpectations(t)
}

func TestTask_VScale_AlwaysScaleDown(t *testing.T) {
	test.InitTestEnv()

	db, cleanup, task, promApiMock := vscalePrepare(t, vScaleCpu{
		allocatedCpu:          1000,
		unallocatedCpu:        1000,
		calculatedCpuService1: 400,
		calculatedCpuService2: 2000,
	})
	defer cleanup()

	err := task.VScale(common.Prod, false)
	require.NoError(t, err)

	count := int64(0)
	err = db.GormDb.Model(bulk_deployment.BulkDeployment{}).Where("type=?", common.Update).Count(&count).Error
	require.NoError(t, err)
	require.Equal(t, int64(1), count)

	brStatus := bulk_deployment.Status{}
	err = db.GormDb.First(&brStatus).Error
	require.NoError(t, err)
	require.Equal(t, "test-service", brStatus.Name)

	// wait BR for end
	require.Eventually(t, func() bool {
		br := bulk_deployment.BulkDeployment{}
		db.GormDb.First(&br)
		return br.State == bulk.State_Success
	}, 5*time.Second, time.Second/10)
	promApiMock.AssertExpectations(t)
}

func TestTask_VScale_ScalingDisallowed(t *testing.T) {
	test.InitTestEnv()

	db, cleanup, task, promApiMock := vscalePrepare(t, vScaleCpu{
		allocatedCpu:          1000,
		unallocatedCpu:        1000,
		calculatedCpuService1: 2000,
		calculatedCpuService2: 2000,
	})
	defer cleanup()

	err := task.VScale(common.Prod, false)
	require.NoError(t, err)

	count := int64(0)
	err = db.GormDb.Model(bulk_deployment.BulkDeployment{}).Where("type=?", common.Update).Count(&count).Error
	require.NoError(t, err)
	require.Equal(t, int64(0), count)

	promApiMock.AssertExpectations(t)
}

func TestTask_VScaleServiceWithBranch(t *testing.T) {
	test.InitTestEnv()

	testCases := []struct {
		name  string
		layer common.Layer
	}{
		{
			name:  "Skip branches at prod",
			layer: common.Prod,
		},
		{
			name:  "Skip branches at test",
			layer: common.Test,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			db := test_db.NewSeparatedDb(t)
			db.GormDb.DisableForeignKeyConstraintWhenMigrating = true

			promApiMock := &mock.PrometheusApi{}
			promApiMock.On("Query", tMock.Anything, tMock.Anything, tMock.Anything).
				Return(&model.Scalar{Value: model.SampleValue(10000)}, v1.Warnings{}, nil)
			promApiMock.On("QueryRange", tMock.Anything, tMock.Anything, tMock.Anything).
				Return(promApiMock.GenerateMatrix(float64(600)), v1.Warnings{}, nil)

			task, cleanup := prepareTask(t, db, promApiMock)
			defer cleanup()

			writeManifestAndMap(t, db, "svc-1", "", "auto_cpu: true")
			writeManifestAndMap(t, db, "svc-2", "", "auto_cpu: true")
			writeDeploymentAndStatus(t, db, "svc-1", "", tc.layer, &dModel.Overrides{CPU: 500})
			writeDeploymentAndStatus(t, db, "svc-1", "some-branch", tc.layer, &dModel.Overrides{CPU: 500})
			writeDeploymentAndStatus(t, db, "svc-2", "", tc.layer, &dModel.Overrides{CPU: 500})

			err := task.VScale(tc.layer, false)
			require.NoError(t, err)

			var brStatuses []*bulk_deployment.Status
			require.NoError(t, db.GormDb.Find(&brStatuses).Error)

			require.Len(t, brStatuses, 2)
			require.False(t, containsBranch(brStatuses))
		})
	}

}

func containsBranch(statuses []*bulk_deployment.Status) bool {
	for _, st := range statuses {
		if st.Branch != "" {
			return true
		}
	}
	return false
}

func TestTask_ProcessHistoricalData(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)

	promApiMock := &mock.PrometheusApi{}
	promApiMock.On("QueryRange", tMock.Anything, tMock.Anything, tMock.Anything).
		Return(promApiMock.GenerateMatrix(199), v1.Warnings{}, nil).Times(4)

	log := test.NewLogger(t)
	scalerStorage := scaler.NewStorage(db, log)
	scalerSrv := scaler.NewService(nil, nil, promApiMock, nil, scaler.NewConf(), db, log)

	conf := NewConf()
	conf.CooldownDays = 3
	task, err := RunTask(nil, nil, nil, scalerSrv, nil, nil, nil, nil, conf, log, nil)
	require.NoError(t, err)

	date := time.Date(2011, 1, 13, 0, 0, 0, 0, time.Local)
	err = task.ProcessHistoricalData(common.Prod, "test-service", date)
	require.NoError(t, err)
	// ensure no processing will occur
	err = task.ProcessHistoricalData(common.Prod, "test-service", date)
	require.NoError(t, err)

	cnt, err := scalerStorage.Count(common.Prod, "test-service")
	require.NoError(t, err)
	require.Equal(t, int64(4), cnt)

	entries, err := scalerStorage.List("")
	require.NoError(t, err)
	var dates []int64
	for _, entry := range entries {
		dates = append(dates, entry.At.Unix())
	}
	expectedDates := []int64{
		date.Unix(),
		date.AddDate(0, 0, -1).Unix(),
		date.AddDate(0, 0, -2).Unix(),
		date.AddDate(0, 0, -3).Unix(),
	}
	require.ElementsMatch(t, dates, expectedDates)

	promApiMock.AssertExpectations(t)
}

func TestTask_processHistoricalData_noPrometheusData(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)

	promApiMock := &mock.PrometheusApi{}
	promApiMock.On("QueryRange", tMock.Anything, tMock.Anything, tMock.Anything).
		Return(promApiMock.GenerateMatrix(), v1.Warnings{}, nil).Times(4)

	log := test.NewLogger(t)
	scalerStorage := scaler.NewStorage(db, log)
	scalerSrv := scaler.NewService(nil, nil, promApiMock, nil, scaler.NewConf(), db, log)

	conf := NewConf()
	conf.CooldownDays = 3
	task, err := RunTask(nil, nil, nil, scalerSrv, nil, nil, nil, nil, conf, log, nil)
	require.NoError(t, err)

	date := time.Date(2011, 1, 13, 0, 0, 0, 0, time.Local)
	err = task.ProcessHistoricalData(common.Prod, "test-service", date)
	require.NoError(t, err)

	cnt, err := scalerStorage.Count(common.Prod, "test-service")
	require.NoError(t, err)
	require.Equal(t, int64(0), cnt)

	promApiMock.AssertExpectations(t)
}

func writeManifestAndMap(
	t *testing.T,
	db *storage.Database,
	serviceName, mapExt, manExt string,
) {
	log := test.NewLogger(t)
	mapS := service_map.NewService(db, log, service_change.NewNotificationMock())
	includeSrv := include.NewService(db, log)
	manS := manifest.NewService(db, log, parser.NewService(log, nil), includeSrv)

	bytes := []byte(fmt.Sprintf(mapFile, serviceName, mapExt))
	err := mapS.ReadAndSave(bytes, 10, fmt.Sprintf("maps/%s.yml", serviceName))
	require.NoError(t, err)

	incFile := &inc_data.Data{Path: "test.yml"}
	db.GormDb.Create(incFile)

	bytes = []byte(fmt.Sprintf(manFile, serviceName, serviceName, manExt))
	err = manS.ReadAndSave(bytes, 10, fmt.Sprintf("deploy/%s.yml", serviceName))
	require.NoError(t, err)
}

func writeDeploymentAndStatus(t *testing.T, db *storage.Database, serviceName string, branch string, layer common.Layer, overrides *dModel.Overrides) {
	log := test.NewLogger(t)
	dStorage := dModel.NewStorage(db, log)
	mapS := service_map.NewService(db, log, service_change.NewNotificationMock())
	includeSrv := include.NewService(db, log)
	manS := manifest.NewService(db, log, parser.NewService(log, nil), includeSrv)
	statusStorage := status.NewStorage(db, log)

	_, manifestId, err := manS.GetByNameWithId(layer, serviceName)
	require.NoError(t, err)
	_, mapId, err := mapS.GetByFullPath(fmt.Sprintf("maps/%s.yml", serviceName))
	require.NoError(t, err)
	deployment := dModel.Deployment{
		Layer:            layer,
		DeployManifestID: manifestId,
		ServiceMapsID:    mapId,
		Overrides:        overrides,
		Branch:           branch,
	}
	err = dStorage.Save(&deployment)
	require.NoError(t, err)

	// TODO: deployment and internals should be saved through deploy factory
	db.GormDb.AutoMigrate(&include_links.DeploymentIncludes{})
	inc := &inc_data.Data{}
	db.GormDb.Unscoped().First(inc, "path=?", "test.yml")
	deployInc := &include_links.DeploymentIncludes{DeploymentId: deployment.ID, IncludeId: inc.ID}
	require.NoError(t, db.GormDb.Create(deployInc).Error)

	err = statusStorage.Save(
		&status.Status{
			State:        status.StateRunning,
			Layer:        layer,
			Name:         serviceName,
			Branch:       branch,
			Version:      "v42",
			DeploymentID: deployment.ID,
		},
	)
	require.NoError(t, err)
}

func vscalePrepare(t *testing.T, cpu vScaleCpu) (*storage.Database, func(), *Task, *mock.PrometheusApi) {
	db := test_db.NewSeparatedDb(t)
	db.GormDb.DisableForeignKeyConstraintWhenMigrating = true

	allocatedCpuQuery := fmt.Sprintf(scaler.ClusterCPUReq, "", "sas")
	unallocatedCpuQuery := fmt.Sprintf(scaler.ClusterCPUReq, "un", "sas")
	calculateQuery := fmt.Sprintf(scaler.CalculateCPUReq, "test-service")
	calculateQuery2 := fmt.Sprintf(scaler.CalculateCPUReq, "test-service2")
	promApiMock := &mock.PrometheusApi{}
	promApiMock.On("Query", tMock.Anything, allocatedCpuQuery, tMock.Anything).
		Return(&model.Scalar{Value: model.SampleValue(cpu.allocatedCpu)}, v1.Warnings{}, nil).Once()
	promApiMock.On("Query", tMock.Anything, unallocatedCpuQuery, tMock.Anything).
		Return(&model.Scalar{Value: model.SampleValue(cpu.unallocatedCpu)}, v1.Warnings{}, nil).Once()
	promApiMock.On("QueryRange", tMock.Anything, calculateQuery, tMock.Anything).
		Return(promApiMock.GenerateMatrix(float64(cpu.calculatedCpuService1)), v1.Warnings{}, nil).Once()
	promApiMock.On("QueryRange", tMock.Anything, calculateQuery2, tMock.Anything).
		Return(promApiMock.GenerateMatrix(float64(cpu.calculatedCpuService2)), v1.Warnings{}, nil).Once()

	task, cleanup := prepareTask(t, db, promApiMock)

	services := []string{"test-service", "test-service2"}
	for _, serviceName := range services {
		writeManifestAndMap(t, db, serviceName, "", "auto_cpu: true")
		writeDeploymentAndStatus(t, db, serviceName, "", common.Prod, &dModel.Overrides{CPU: 500})
	}

	return db, cleanup, task, promApiMock
}

func prepareTask(t *testing.T, db *storage.Database, promApi *mock.PrometheusApi) (*Task, func()) {
	log := test.NewLogger(t)
	approve.NewService(db, log, nil) // table create
	issue.NewStorage(db, log)        // table create
	mapSrv := service_map.NewService(db, log, service_change.NewNotificationMock())
	includeSvc := include.NewService(db, log)
	manSvc := manifest.NewService(db, log, parser.NewService(log, nil), includeSvc)
	statusSrv := status.NewService(db, log, mapSrv)
	scalerSrv := scaler.NewService(nil, nil, promApi, promApi, scaler.NewConf(), db, log)
	electionStub := election.NewElectionStub()
	dataService := data.NewService(db, log)
	overrideSvc := override.NewService(db, log, includeSvc, manSvc)

	conf := bulk_deployment.NewConf()
	conf.ServiceLimit = 0
	silentLocker := mock.NewMockSilentLocker()
	brSvc := (&bulk_deployment.Service{
		Conf:      conf,
		DB:        db,
		Log:       log,
		DeploySvc: &mocks.IService{},
		StatusSvc: status.NewService(db, log, mapSrv),
		Locker:    silentLocker,
		Producer:  mqMock.NewProducerMock(),
	}).Init()

	newConf := NewConf()
	newConf.CooldownDays = 0
	task, err := RunTask(mapSrv, manSvc, brSvc, scalerSrv, statusSrv, dataService, overrideSvc, electionStub, newConf, log, nil)
	require.NoError(t, err)

	return task, func() {
		task.Close()
		electionStub.Stop()
		silentLocker.Stop()
	}
}

func TestTask_extractChange(t *testing.T) {
	test.InitTestEnv()

	testCases := []struct {
		name     string
		override *dModel.Overrides
		want     *Change
	}{
		{name: "nil override", override: nil, want: &Change{
			Service:    "test-service",
			CurrentCpu: 500,
			NewCpu:     500,
			Dc:         map[string]int{"sas": 1},
		}},
		{name: "no overrides", override: &dModel.Overrides{}, want: &Change{
			Service:    "test-service",
			CurrentCpu: 500,
			NewCpu:     500,
			Dc:         map[string]int{"sas": 1},
		}},
		{name: "cpu override", override: &dModel.Overrides{CPU: 300}, want: &Change{
			Service:    "test-service",
			CurrentCpu: 300,
			NewCpu:     500,
			Dc:         map[string]int{"sas": 1},
		}},
		{name: "dc override", override: &dModel.Overrides{DC: map[string]int{"vla": 1}}, want: &Change{
			Service:    "test-service",
			CurrentCpu: 500,
			NewCpu:     500,
			Dc:         map[string]int{"vla": 1},
		}},
	}

	for _, testCase := range testCases {
		t.Run(testCase.name, func(t *testing.T) {
			tt := testCase

			db := test_db.NewSeparatedDb(t)

			log := test.NewLogger(t)
			includeService := include.NewService(db, log)
			manS := manifest.NewService(db, log, parser.NewService(log, nil), includeService)
			scalerSrv := scaler.NewService(nil, nil, nil, nil, scaler.NewConf(), db, log)
			writeManifestAndMap(t, db, "test-service", "", "auto_cpu: true")

			task, err := RunTask(nil, nil, nil, scalerSrv, nil, nil, nil, nil, NewConf(), log, nil)
			require.NoError(t, err)

			manifest, _, err := manS.GetByNameWithId(common.Prod, "test-service")
			require.NoError(t, err)

			change, err := task.ExtractChange(
				common.Prod, manifest, tt.override, sMap,
			)
			require.NoError(t, err)
			require.Equal(t, tt.want, change)
		})
	}
}

func TestTask_filterForChangedCpu(t *testing.T) {
	test.InitTestEnv()
	log := test.NewLogger(t)

	task, err := RunTask(nil, nil, nil, nil, nil, nil, nil, nil, NewConf(), log, nil)
	require.NoError(t, err)

	input := []*Change{
		{
			Service:    "1",
			CurrentCpu: 100,
			NewCpu:     200,
		},
		{
			Service:    "1",
			CurrentCpu: 200,
			NewCpu:     200,
		},
	}

	filtered, err := task.filterForChangedCpu(common.Prod, input)
	require.NoError(t, err)
	require.Equal(t, []*Change{
		{
			Service:    "1",
			CurrentCpu: 100,
			NewCpu:     200,
		},
	}, filtered)
}

func TestTask_calculateResourcesAndSave(t *testing.T) {
	test.InitTestEnv()
	log := test.NewLogger(t)
	db := test_db.NewSeparatedDb(t)

	promApiMock := &mock.PrometheusApi{}
	promApiMock.On("QueryRange", tMock.Anything, tMock.Anything, tMock.Anything).
		Return(promApiMock.GenerateMatrix(1.2, 199), v1.Warnings{}, nil).Once()

	scalerStorage := scaler.NewStorage(db, log)
	scalerSrv := scaler.NewService(nil, nil, promApiMock, nil, scaler.NewConf(), db, log)

	task, err := RunTask(nil, nil, nil, scalerSrv, nil, nil, nil, nil, NewConf(), log, nil)
	require.NoError(t, err)

	err = task.calculateResourcesAndSave(common.Prod, "srv", time.Now())
	require.NoError(t, err)

	entries, err := scalerStorage.List("")
	require.NoError(t, err)
	require.Len(t, entries, 1)

	promApiMock.AssertExpectations(t)
}

func TestTask_calculateResourcesAndSave_NoData(t *testing.T) {
	test.InitTestEnv()
	log := test.NewLogger(t)
	db := test_db.NewSeparatedDb(t)

	promApiMock := &mock.PrometheusApi{}
	promApiMock.On("QueryRange", tMock.Anything, tMock.Anything, tMock.Anything).
		Return(promApiMock.GenerateMatrix(), v1.Warnings{}, nil).Once()

	scalerStorage := scaler.NewStorage(db, log)
	scalerSrv := scaler.NewService(nil, nil, promApiMock, nil, scaler.NewConf(), db, log)

	task, err := RunTask(nil, nil, nil, scalerSrv, nil, nil, nil, nil, NewConf(), log, nil)
	require.NoError(t, err)

	err = task.calculateResourcesAndSave(common.Prod, "srv", time.Now())
	require.NoError(t, err)

	entries, err := scalerStorage.List("")
	require.NoError(t, err)
	require.Len(t, entries, 0)
}
