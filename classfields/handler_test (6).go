package deploy2_test

import (
	"context"
	"database/sql"
	_ "embed"
	"fmt"
	"strconv"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/api/grpc/deploy2"
	"github.com/YandexClassifieds/shiva/cmd/shiva/approve"
	"github.com/YandexClassifieds/shiva/cmd/shiva/batch"
	batchStore "github.com/YandexClassifieds/shiva/cmd/shiva/batch/store/batch"
	"github.com/YandexClassifieds/shiva/cmd/shiva/batch/store/task"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment"
	dContext "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/context"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/include_links"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/mocks"
	dModel "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	ds "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/status"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/writer"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/override"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/system"
	"github.com/YandexClassifieds/shiva/cmd/shiva/transformer"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/common/user_error"
	deployV2 "github.com/YandexClassifieds/shiva/pb/shiva/api/deploy2"
	spb "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	batchPb "github.com/YandexClassifieds/shiva/pb/shiva/types/batch"
	btPb "github.com/YandexClassifieds/shiva/pb/shiva/types/batch_task"
	deploymentPb "github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	envPb "github.com/YandexClassifieds/shiva/pb/shiva/types/env"
	error2 "github.com/YandexClassifieds/shiva/pb/shiva/types/error"
	flagsPb "github.com/YandexClassifieds/shiva/pb/shiva/types/flags"
	layerPb "github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	revertPb "github.com/YandexClassifieds/shiva/pb/shiva/types/revert"
	statePb "github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	generatePb "github.com/YandexClassifieds/shiva/pb/ss/secret"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/include/domain"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	mm "github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/secrets/secret"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/pkg/template"
	"github.com/YandexClassifieds/shiva/test"
	mocks2 "github.com/YandexClassifieds/shiva/test/mock/mockery/mocks"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/timestamppb"
)

const (
	sName       = "yandex_vertis_example_service_d"
	sVersion    = "1.0.0"
	includeYml  = `PARAM4: params4`
	includeYml1 = `PARAM4: params4New`
)

var (
	//go:embed testdata/manifest.yml
	testManifest string
	//go:embed testdata/manifest1.yml
	testManifest1 string
	//go:embed testdata/map.yml
	testMap string
)

func TestDeploy_Run(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	deployService := &mocks.IService{}
	deployServer := prepare(t, deployService, nil)

	st := make(chan *deployment.StateChange)
	go func() {
		st <- &deployment.StateChange{
			DState: dModel.Process,
		}
	}()
	dctx := &dContext.Context{
		Meta: &dContext.Meta{
			Deployment: &dModel.Deployment{
				Author: &staff.User{},
			},
		},
	}
	id := uuid.New()
	deployService.On("Run", mock.Anything, dModel.RunParams{
		UUID:    id,
		Layer:   common.Test,
		Name:    sName,
		Version: sVersion,
		Login:   "danevge",
		Source:  "OAUTH",
	}).Return(st, dctx, nil)

	resp, err := deployServer.Run(context.Background(), &deployV2.RunRequest{
		Uuid:    id.String(),
		Login:   "danevge",
		Layer:   layerPb.Layer_TEST,
		Name:    sName,
		Version: sVersion,
	})
	assert.NoError(t, err)
	require.NotNil(t, resp)
}

func TestStop(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	deployService := &mocks.IService{}
	deployServer := prepare(t, deployService, nil)
	st := make(chan *deployment.StateChange)
	go func() {
		st <- &deployment.StateChange{
			DState: dModel.Process,
		}
	}()
	dctx := &dContext.Context{
		Meta: &dContext.Meta{
			Deployment: &dModel.Deployment{
				Author: &staff.User{},
			},
		},
	}

	id := uuid.New()
	deployService.On("Stop", mock.Anything, dModel.StopParams{
		UUID:   id,
		Layer:  common.Test,
		Name:   sName,
		Login:  "danevge",
		Source: config.OAuthSource,
	}).Return(st, dctx, nil)

	resp, err := deployServer.Stop(context.Background(), &deployV2.StopRequest{
		Uuid:  id.String(),
		Login: "danevge",
		Layer: layerPb.Layer_TEST,
		Name:  sName,
	})
	require.NoError(t, err)
	require.NotNil(t, resp)
}

func TestRestart(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	deployService := &mocks.IService{}
	deployServer := prepare(t, deployService, nil)

	st := make(chan *deployment.StateChange)
	go func() {
		st <- &deployment.StateChange{
			DState: dModel.Process,
		}
	}()
	dctx := &dContext.Context{
		Meta: &dContext.Meta{
			Deployment: &dModel.Deployment{
				Author: &staff.User{},
			},
		},
	}
	id := uuid.New()
	deployService.On("Restart", mock.Anything, dModel.RestartParams{
		UUID:   id,
		Layer:  common.Test,
		Name:   sName,
		Login:  "danevge",
		Source: config.OAuthSource,
	}).Return(st, dctx, nil)

	resp, err := deployServer.Restart(context.Background(), &deployV2.RestartRequest{
		Uuid:  id.String(),
		Login: "danevge",
		Layer: layerPb.Layer_TEST,
		Name:  sName,
	})
	require.NoError(t, err)
	require.NotNil(t, resp)
}

func TestRevert(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	deployService := &mocks.IService{}
	deployServer := prepare(t, deployService, nil)

	st := make(chan *deployment.StateChange)
	go func() {
		st <- &deployment.StateChange{
			DState: dModel.Process,
		}
	}()
	dctx := &dContext.Context{
		Meta: &dContext.Meta{
			Deployment: &dModel.Deployment{
				Author: &staff.User{},
			},
		},
	}
	id := uuid.New()
	deployService.On("Revert", mock.Anything, dModel.RevertParams{
		UUID:    id,
		Layer:   common.Test,
		Name:    sName,
		Login:   "danevge",
		Comment: "cmt",
		Source:  config.OAuthSource,
	}).Return(st, dctx, nil)

	resp, err := deployServer.Revert(context.Background(), &deployV2.RevertRequest{
		Uuid:    id.String(),
		Login:   "danevge",
		Layer:   layerPb.Layer_TEST,
		Name:    sName,
		Comment: "cmt",
	})
	require.NoError(t, err)
	require.NotNil(t, resp)
}

func TestCancel(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	deployService := &mocks.IService{}
	deployServer := prepare(t, deployService, nil)

	st := make(chan *deployment.StateChange)
	go func() {
		st <- &deployment.StateChange{
			DState: dModel.Process,
		}
	}()
	dctx := &dContext.Context{
		Meta: &dContext.Meta{
			Deployment: &dModel.Deployment{
				Author: &staff.User{},
			},
		},
	}
	id := uuid.New()
	deployService.On("Cancel", mock.Anything, dModel.CancelParams{
		ID:     42,
		Login:  "danevge",
		Source: config.OAuthSource,
	}).Return(st, dctx, nil)

	resp, err := deployServer.Cancel(context.Background(), &deployV2.CancelRequest{
		Uuid:         id.String(),
		Login:        "danevge",
		DeploymentId: "42",
	})
	require.NoError(t, err)
	require.NotNil(t, resp)
}

func TestRunSox(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	deployService := &mocks.IService{}
	deployServer := prepare(t, deployService, nil)

	dctx := &dContext.Context{
		Meta: &dContext.Meta{
			Deployment: &dModel.Deployment{
				Author: &staff.User{},
				State:  dModel.WaitApprove,
			},
		},
	}

	id := uuid.New()
	deployService.On("Run", mock.Anything, dModel.RunParams{
		UUID:    id,
		Layer:   common.Prod,
		Name:    sName,
		Version: sVersion,
		Login:   "danevge",
		Source:  config.OAuthSource,
	}).Return(nil, dctx, nil)

	resp, err := deployServer.Run(context.Background(), &deployV2.RunRequest{
		Uuid:    id.String(),
		Login:   "danevge",
		Layer:   layerPb.Layer_PROD,
		Name:    sName,
		Version: sVersion,
	})
	assert.NoError(t, err)
	require.NotNil(t, resp)
}

func TestPromoteCanary(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	deployService := &mocks.IService{}
	deployServer := prepare(t, deployService, nil)

	dctx := &dContext.Context{
		Meta: &dContext.Meta{
			Deployment: &dModel.Deployment{
				State:  dModel.CanarySuccess,
				Author: &staff.User{},
			},
		},
	}

	id := uuid.New()
	deployService.On("Promote", mock.Anything, dModel.PromoteParams{
		ID:     42,
		Login:  "danevge",
		Source: config.OAuthSource,
	}).Return(nil, dctx, nil)

	resp, err := deployServer.Promote(context.Background(), &deployV2.PromoteRequest{
		Uuid:         id.String(),
		Login:        "danevge",
		DeploymentId: "42",
	})
	require.NoError(t, err)
	require.NotNil(t, resp)
}

func TestPromote(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	deployService := &mocks.IService{}
	deployServer := prepare(t, deployService, nil)

	st := make(chan *deployment.StateChange)
	go func() {
		st <- &deployment.StateChange{
			DState: dModel.Process,
		}
	}()
	dctx := &dContext.Context{
		Meta: &dContext.Meta{
			Deployment: &dModel.Deployment{
				Author: &staff.User{},
			},
		},
	}

	id := uuid.New()
	deployService.On("Promote", mock.Anything, dModel.PromoteParams{
		ID:     42,
		Login:  "danevge",
		Source: config.OAuthSource,
	}).Return(st, dctx, nil)

	resp, err := deployServer.Promote(context.Background(), &deployV2.PromoteRequest{
		Uuid:         id.String(),
		Login:        "danevge",
		DeploymentId: "42",
	})
	require.NoError(t, err)
	require.NotNil(t, resp)
}

func TestApprove(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	deployService := &mocks.IService{}
	deployServer := prepare(t, deployService, nil)

	st := make(chan *deployment.StateChange)
	go func() {
		st <- &deployment.StateChange{
			DState: dModel.Process,
		}
	}()
	dctx := &dContext.Context{
		Meta: &dContext.Meta{
			Deployment: &dModel.Deployment{
				Author: &staff.User{},
			},
		},
	}

	id := uuid.New()
	deployService.On("Approve", mock.Anything, dModel.ApproveParams{
		ID:     42,
		Login:  "spooner",
		Source: config.OAuthSource,
	}).Return(st, dctx, nil)

	resp, err := deployServer.Approve(context.Background(), &deployV2.ApproveRequest{
		Uuid:         id.String(),
		Login:        "spooner",
		DeploymentId: "42",
	})
	require.NoError(t, err)
	require.NotNil(t, resp)
}

func TestHandler_ApproveList(t *testing.T) {
	test.RunUp(t)

	deployService := &mocks.IService{}
	h := prepare(t, deployService, nil)

	lst := []*dModel.Deployment{
		{
			State:     dModel.WaitApprove,
			StartDate: time.Now(),
			EndDate:   time.Now(),
			Author:    &staff.User{Login: "usr1"},
		},
		{
			State:     dModel.WaitApprove,
			StartDate: time.Now(),
			EndDate:   time.Now(),
			Author:    &staff.User{Login: "usr2"},
		},
	}
	deployService.On("ApproveList2", "test-user").Return(lst, nil)

	result, err := h.ApproveList(context.Background(), &deployV2.ApproveListRequest{Login: "test-user"})
	require.NoError(t, err)
	require.Len(t, result.Deployment, 2)
}

func TestHandler_Settings(t *testing.T) {
	test.RunUp(t)
	deployService := &mocks.IService{}
	h := prepare(t, deployService, nil)

	db := test.NewGorm(t)
	db.Create(&feature_flags.FeatureFlag{
		Flag:   feature_flags.ProdSasOff.String(),
		Value:  true,
		Reason: "drills",
	})
	db.Create(&feature_flags.FeatureFlag{
		Flag:   feature_flags.ProdVlaOff.String(),
		Value:  false,
		Reason: "r2",
	})
	db.Create(&feature_flags.FeatureFlag{
		Flag:   feature_flags.TestSasOff.String(),
		Value:  true,
		Reason: "drills",
	})

	result, err := h.Settings(context.Background(), nil)
	require.NoError(t, err)
	require.Len(t, result.Flags, 2)

	mf := make(map[string]*flagsPb.FeatureFlag)
	for _, v := range result.GetFlags() {
		mf[v.Name] = v
	}
	assert.Equal(t, "drills", mf[feature_flags.ProdSasOff.String()].GetReason())
	assert.Equal(t, "drills", mf[feature_flags.TestSasOff.String()].GetReason())
}

func TestEnvs_BadRequest(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	deployService := &mocks.IService{}
	deployServer := prepare(t, deployService, nil)

	_, err := deployServer.Envs(context.Background(), &deployV2.EnvsRequest{})
	require.Error(t, err)
	require.Equal(t, codes.InvalidArgument, status.Code(err))
}

func TestEnvs_ByServiceLayer(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	makeMap(t, testMap, sName, false, "")
	makeInclude(t, includeYml, "my_service/common.yml")
	makeManifest(t, testManifest, sName)

	ssCliMock := &mocks2.SecretClient{}
	ssCliMock.On("GetSecrets", mock.Anything, &generatePb.GetSecretsRequest{
		ServiceName: sName,
		Layer:       layerPb.Layer_PROD,
	}).Return(&generatePb.GetSecretResponse{SecretEnvs: map[string]string{"k1": "${sec-1:ver-1:k1}"}}, nil)
	ssCliMock.On("GetSecrets", mock.Anything, &generatePb.GetSecretsRequest{
		ServiceName: sName,
		Layer:       layerPb.Layer_TEST,
	}).Return(nil, nil)
	secretSvc := secret.NewService(test.NewLogger(t), ssCliMock)

	deployServer := prepare(t, nil, secretSvc)

	resp, err := deployServer.Envs(context.Background(), &deployV2.EnvsRequest{
		Filter: &deployV2.EnvsRequest_ServiceLayer_{
			ServiceLayer: &deployV2.EnvsRequest_ServiceLayer{
				Service: sName,
				Layer:   layerPb.Layer_PROD,
			}}},
	)
	require.NoError(t, err)
	expected := []*envPb.Env{
		{Source: envPb.EnvSource_COMMON_PARAM, Key: "PARAM1", Value: "common_value_1"},
		{Source: envPb.EnvSource_LAYER_PARAM, Key: "PARAM3", Value: "prod_value_3"},
		{Source: envPb.EnvSource_LAYER_INCLUDE_FILE, Key: "PARAM4", Value: "params4",
			Link: "https://a.yandex-team.ru/arc_vcs/classifieds/services/conf/my_service/common.yml?rev=r10"},
		{Source: envPb.EnvSource_SECRETS, Key: "k1", Value: "${sec-1:ver-1:k1}"},
		{Source: envPb.EnvSource_COMMON_PARAM, Key: "TMP", Value: "http://yandex_vertis_example_service_d-deploy.vrts-slb.prod.vertis.yandex.net:80"},
	}
	require.Subset(t, resp.Envs, expected)

	resp, err = deployServer.Envs(context.Background(), &deployV2.EnvsRequest{
		Filter: &deployV2.EnvsRequest_ServiceLayer_{
			ServiceLayer: &deployV2.EnvsRequest_ServiceLayer{
				Service: sName,
				Layer:   layerPb.Layer_TEST,
			}}},
	)
	require.NoError(t, err)
	expected = []*envPb.Env{
		{Source: envPb.EnvSource_LAYER_PARAM, Key: "PARAM1", Value: "test_value_1"},
		{Source: envPb.EnvSource_LAYER_PARAM, Key: "PARAM6", Value: "test_value_6"},
		{Source: envPb.EnvSource_LAYER_INCLUDE_FILE, Key: "PARAM4", Value: "params4",
			Link: "https://a.yandex-team.ru/arc_vcs/classifieds/services/conf/my_service/common.yml?rev=r10"},
		{Source: envPb.EnvSource_COMMON_PARAM, Key: "TMP", Value: "http://yandex_vertis_example_service_d-deploy.vrts-slb.test.vertis.yandex.net:80"},
	}
	require.Subset(t, resp.Envs, expected)
}

//TODO fix url here when services will transfer to acadia
func TestEnvs_ByDeployment(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	_, smID := makeMap(t, testMap, sName, false, "")
	inc := makeInclude(t, includeYml, "my_service/common.yml")
	_, mID := makeManifest(t, testManifest, sName)
	db := test_db.NewDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(approve.Approve{}))
	log := test.NewLogger(t)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	dStore := dModel.NewStorage(db, log)
	d := &dModel.Deployment{
		State:            dModel.Success,
		Layer:            common.Test,
		Name:             sName,
		Version:          "v42",
		ServiceMapsID:    smID,
		DeployManifestID: mID,
		AuthorID:         user.ID,
		SecretVersion:    "ver-1",
	}
	require.NoError(t, dStore.Save(d))
	includeLinksStore := include_links.NewStorage(db, log)
	err = includeLinksStore.Save([]*include_links.DeploymentIncludes{{
		DeploymentId: d.ID,
		IncludeId:    inc.ID(),
		Override:     false,
	}})
	require.NoError(t, err)

	//update configuration
	makeManifest(t, testManifest1, sName)
	makeInclude(t, includeYml1, "my_service/common.yml")

	ssCliMock := &mocks2.SecretClient{}
	ssCliMock.On("GetSecrets", mock.Anything, &generatePb.GetSecretsRequest{
		ServiceName: sName,
		Layer:       layerPb.Layer_TEST,
		VersionId:   "ver-1",
	}).Return(&generatePb.GetSecretResponse{SecretEnvs: map[string]string{"k1": "${sec-1:ver-1:k1}"}}, nil)
	secretSvc := secret.NewService(log, ssCliMock)
	deployServer := prepare(t, nil, secretSvc)

	resp, err := deployServer.Envs(context.Background(), &deployV2.EnvsRequest{
		Filter: &deployV2.EnvsRequest_DeploymentId{DeploymentId: strconv.FormatInt(d.ID, 10)}},
	)
	require.NoError(t, err)
	expected := []*envPb.Env{
		{Source: envPb.EnvSource_LAYER_PARAM, Key: "PARAM1", Value: "test_value_1"},
		{Source: envPb.EnvSource_LAYER_PARAM, Key: "PARAM6", Value: "test_value_6"},
		{Source: envPb.EnvSource_LAYER_INCLUDE_FILE, Key: "PARAM4", Value: "params4", Link: "https://a.yandex-team.ru/arc_vcs/classifieds/services/conf/my_service/common.yml?rev=r10"},
		{Source: envPb.EnvSource_SECRETS, Key: "k1", Value: "${sec-1:ver-1:k1}"},
		{Source: envPb.EnvSource_COMMON_PARAM, Key: "TMP", Value: "http://yandex_vertis_example_service_d-deploy.vrts-slb.test.vertis.yandex.net:80"},
	}
	require.Subset(t, resp.Envs, expected)
}

func TestHandler_StatusTypeService(t *testing.T) {
	test.RunUp(t)
	db := test.NewGorm(t)

	h := prepare(t, nil, nil)
	makeInclude(t, includeYml, "my_service/common.yml")
	_, svcMapId := makeMap(t, testMap, "some-service", false, "")
	_, manifestId := makeManifest(t, testManifest, "some-service")

	svcDeploy := &dModel.Deployment{
		State:            dModel.Success,
		ServiceMapsID:    svcMapId,
		DeployManifestID: manifestId,
		Author:           &staff.User{Login: "other-guy"},
	}
	require.NoError(t, db.Create(svcDeploy).Error)
	db.Joins("ServiceMap").Joins("Manifest").Find(svcDeploy)
	require.NoError(t, db.Create(&ds.Status{
		State:        ds.StateRunning,
		Version:      "v1",
		Layer:        common.Test,
		Name:         "some-service",
		DeploymentID: svcDeploy.ID,
		EventTime:    time.Now(),
	}).Error)

	result, err := h.Status(context.Background(), &deployV2.StatusRequest{ServiceName: "some-service"})
	require.NoError(t, err)

	data := result.GetServiceInfo().GetEntries()
	require.Len(t, data, 1)
}

func TestHandler_StatusTypeBatch(t *testing.T) {
	test.RunUp(t)
	db := test.NewGorm(t)

	h := prepare(t, nil, nil)
	makeInclude(t, includeYml, "my_service/common.yml")
	_, svcMapId := makeMap(t, testMap, "some-batch", false, "batch")
	_, manifestId := makeManifest(t, testManifest, "some-batch")

	// postgres stores only microseconds
	nextTs := time.Now().Add(time.Hour).Truncate(time.Microsecond)

	b := &batchStore.Batch{
		Layer:    layerPb.Layer_TEST,
		Name:     "some-batch",
		Version:  "v1",
		State:    batchPb.State_Active,
		Periodic: "@daily",
		Next:     sql.NullTime{Time: nextTs, Valid: true},
		Deployment: &dModel.Deployment{
			ServiceMapsID:    svcMapId,
			DeployManifestID: manifestId,
			Author:           &staff.User{Login: "test-usr"},
		},
	}
	require.NoError(t, db.Create(b).Error)
	b2 := &batchStore.Batch{
		Layer:    layerPb.Layer_TEST,
		Name:     "some-batch",
		Branch:   "br1",
		Version:  "v1",
		State:    batchPb.State_Active,
		Periodic: "@daily",
		Next:     sql.NullTime{Time: nextTs, Valid: true},
		Deployment: &dModel.Deployment{
			ServiceMapsID:    svcMapId,
			DeployManifestID: manifestId,
			Author:           &staff.User{Login: "test-usr"},
			Branch:           "br1",
		},
	}
	require.NoError(t, db.Create(b2).Error)

	require.NoError(t, db.Create(&task.Task{
		BatchID:      b.ID,
		State:        btPb.State_Success,
		DeploymentID: b.DeploymentID,
		StartDate:    time.Now(),
		EndDate:      sql.NullTime{Valid: true, Time: time.Now()},
	}).Error)

	result, err := h.Status(context.Background(), &deployV2.StatusRequest{
		ServiceName: "some-batch",
	})
	require.NoError(t, err)

	data := result.GetBatchInfo().GetEntries()
	assert.Len(t, data, 2)
	e := data[0]
	assert.Equal(t, batchPb.State_Active, e.GetBatch().State)
	assert.Equal(t, timestamppb.New(nextTs), e.GetBatch().GetNextRun())
	assert.Equal(t, "@daily", e.GetBatch().GetPeriodic())
	assert.Equal(t, "test-usr", e.GetLast().GetAuthor())
	assert.Equal(t, btPb.State_Success, e.GetLast().GetState())
	assert.Equal(t, "br1", data[1].GetDeployment().GetDeployment().GetBranch())
}

func TestHandler_StatusFirstDeployment(t *testing.T) {
	test.RunUp(t)
	db := test.NewGorm(t)

	h := prepare(t, nil, nil)

	makeInclude(t, includeYml, "my_service/common.yml")
	_, svcMapId := makeMap(t, testMap, t.Name(), false, "")
	_, manifestId := makeManifest(t, testManifest, t.Name())

	svcDeploy := &dModel.Deployment{
		Name:             t.Name(),
		State:            dModel.Process,
		ServiceMapsID:    svcMapId,
		DeployManifestID: manifestId,
		Author:           &staff.User{Login: "some-login"},
	}
	require.NoError(t, db.Create(svcDeploy).Error)
	db.Joins("ServiceMap").Joins("Manifest").Find(svcDeploy)

	result, err := h.Status(context.Background(), &deployV2.StatusRequest{ServiceName: t.Name()})
	require.NoError(t, err)

	data := result.GetServiceInfo().GetEntries()
	require.Len(t, data, 1)
}

func TestHandler_State(t *testing.T) {
	test.RunUp(t)
	db := test.NewGorm(t)

	deploySvc := &mocks.IService{}
	h := prepare(t, deploySvc, nil)

	makeInclude(t, includeYml, "my_service/common.yml")
	_, batchMapId := makeMap(t, testMap, "test-batch", false, "batch")
	_, batchManifestId := makeManifest(t, testManifest, "test-batch")
	_, svcMapId := makeMap(t, testMap, "test-svc", false, "")
	_, svcManifestId := makeManifest(t, testManifest, "test-svc")

	t.Run("base", func(t *testing.T) {
		d := &dModel.Deployment{
			Name:             "test-svc",
			State:            dModel.Process,
			Version:          "v1",
			Author:           &staff.User{Login: "test-user"},
			ServiceMapsID:    svcMapId,
			DeployManifestID: svcManifestId,
		}
		db.Create(d).Joins("ServiceMap").Joins("Manifest").Find(d)
		deploySvc.On("Get", d.ID).Return(d, nil)
		resp, err := h.State(context.Background(), &deployV2.StateRequest{
			Id: strconv.FormatInt(d.ID, 10),
		})
		require.NoError(t, err)

		assert.Equal(t, "test-svc", resp.GetService().GetDeployment().GetServiceName())
		assert.Equal(t, "v1", resp.GetService().GetDeployment().GetVersion())
		assert.Equal(t, statePb.DeploymentState_IN_PROGRESS, resp.GetService().GetDeployment().GetState())
		assert.Equal(t, "test-svc", resp.GetService().GetMap().GetName())
	})
	t.Run("batch_info", func(t *testing.T) {
		d := &dModel.Deployment{
			Layer:            common.Test,
			Name:             "test-batch",
			State:            dModel.Success,
			Version:          "v1",
			Author:           &staff.User{Login: "test-user"},
			ServiceMapsID:    batchMapId,
			DeployManifestID: batchManifestId,
		}

		nextRun := time.Now().Add(time.Hour).Truncate(time.Microsecond)

		db.Create(d).Joins("ServiceMap").Joins("Manifest").Find(d)
		db.Create(&batchStore.Batch{
			Layer:        layerPb.Layer_TEST,
			Name:         "test-batch",
			Version:      "v1",
			State:        batchPb.State_Active,
			Periodic:     "@daily",
			Next:         sql.NullTime{Time: nextRun, Valid: true},
			DeploymentID: d.ID,
		})
		deploySvc.On("Get", d.ID).Return(d, nil)
		resp, err := h.State(context.Background(), &deployV2.StateRequest{
			Id: strconv.FormatInt(d.ID, 10),
		})
		require.NoError(t, err)

		assert.Equal(t, "test-batch", resp.GetService().GetDeployment().GetServiceName())
		assert.Equal(t, "v1", resp.GetService().GetDeployment().GetVersion())
		assert.Equal(t, statePb.DeploymentState_SUCCESS, resp.GetService().GetDeployment().GetState())
		assert.Equal(t, timestamppb.New(nextRun), resp.GetBatch().GetNextRun())
		assert.Equal(t, batchPb.State_Active, resp.GetBatch().GetState())
	})
	t.Run("revert_info", func(t *testing.T) {
		d1 := &dModel.Deployment{
			Type:             common.Run,
			Version:          "v1",
			State:            dModel.Process,
			Author:           &staff.User{Login: "test-usr"},
			ServiceMapsID:    svcMapId,
			DeployManifestID: svcManifestId,
		}
		db.Create(d1)
		d2 := &dModel.Deployment{
			Type:             common.Run,
			Name:             "test-svc",
			Version:          "v2",
			PreviousId:       d1.ID,
			State:            dModel.Process,
			Author:           &staff.User{Login: "test-usr"},
			ServiceMapsID:    svcMapId,
			DeployManifestID: svcManifestId,
			Status: &deploymentPb.Status{
				RevertType: revertPb.RevertType_Unhealthy,
			},
		}
		db.Create(d2).Joins("ServiceMap").Joins("Manifest").Find(d2)

		deploySvc.On("Get", d2.ID).Return(d2, nil)
		deploySvc.On("Get", d2.PreviousId).Return(d1, nil)
		resp, err := h.State(context.Background(), &deployV2.StateRequest{
			Id: strconv.FormatInt(d2.ID, 10),
		})
		require.NoError(t, err)

		assert.Equal(t, statePb.DeploymentState_REVERT, resp.GetService().GetDeployment().GetState())
		assert.Equal(t, "v1", resp.GetRevertVersion())
	})
	t.Run("cancel_info", func(t *testing.T) {
		d := &dModel.Deployment{
			Type:             common.Run,
			Layer:            common.Test,
			Name:             "test-svc",
			State:            dModel.Cancel,
			Version:          "v1",
			Author:           &staff.User{Login: "test-user"},
			ServiceMapsID:    svcMapId,
			DeployManifestID: svcManifestId,
		}
		db.Create(d).Joins("ServiceMap").Joins("Manifest").Find(d)

		db.Create(&dModel.Deployment{
			Type:     common.Cancel,
			Layer:    common.Test,
			Name:     "test-svc",
			ParentId: d.ID,
			Author:   &staff.User{Login: "the-other-guy"},
		})

		deploySvc.On("Get", d.ID).Return(d, nil)
		resp, err := h.State(context.Background(), &deployV2.StateRequest{
			Id: strconv.FormatInt(d.ID, 10),
		})
		require.NoError(t, err)

		assert.Equal(t, "the-other-guy", resp.GetCancelUser())
	})
}

func TestErrorWithDetails(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	deployService := &mocks.IService{}
	deployServer := prepare(t, deployService, nil)

	id := uuid.New()
	resp, err := deployServer.Run(context.Background(), &deployV2.RunRequest{
		Uuid:    id.String(),
		Login:   "danevge",
		Layer:   layerPb.Layer_UNKNOWN,
		Name:    sName,
		Version: sVersion,
	})
	require.Nil(t, resp)

	st := status.Convert(err)

	require.Equal(t, codes.InvalidArgument, st.Code())
	require.Equal(t, user_error.NewUserErrors(deploy2.ErrInvalidLayer).Error(), st.Message())
	for index, detail := range st.Details() {
		switch d := detail.(type) {
		case *error2.UserError:
			require.Equal(t, deploy2.ErrInvalidLayer, user_error.MakeUserError(d))
		default:
			require.FailNow(t, "invalid type of details")
		}
		if index > 0 {
			require.FailNow(t, "invalid count of details")
		}
	}
}

func makeManifest(t *testing.T, yml, name string) (*mm.Manifest, int64) {
	imageName := fmt.Sprintf("%s-image", name)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	service := manifest.NewService(db, log, parser.NewService(log, reader.NewService(db, log)), include.NewService(db, log))
	result := fmt.Sprintf(yml, name, imageName)
	err := service.ReadAndSave([]byte(result), test.AtomicNextUint(), "")
	require.NoError(t, err)
	m, id, err := service.GetByNameWithId(common.Prod, name)
	require.NoError(t, err)
	return m, id
}

func makeInclude(t *testing.T, yml, path string) *domain.Include {
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	service := include.NewService(db, log)
	err := service.ReadAndSave([]byte(yml), 10, path)
	require.NoError(t, err)
	inc, err := service.GetByPath(path)
	require.NoError(t, err)
	return inc
}

func makeMap(t *testing.T, yml, name string, sox bool, svcType string) (*spb.ServiceMap, int64) {
	db := test_db.NewDb(t)
	service := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())
	result := fmt.Sprintf(yml, name, sox, svcType)
	path := spb.ToFullPath(name)
	err := service.ReadAndSave([]byte(result), 10, path)
	require.NoError(t, err)
	m, id, err := service.GetByFullPath(path)
	require.NoError(t, err)
	return m, id
}

func prepare(t *testing.T, deploy deployment.IService, secretSvc *secret.Service) *deploy2.Handler {
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	notificationSvc := service_change.NewNotificationMock()
	mapService := service_map.NewService(db, log, notificationSvc)
	statusSvc := ds.NewService(db, log, mapService)
	externalSvcWriter := writer.NewService(db, log, mapService, nil, service_change.NewNotificationMock())
	externalSvcReader := reader.NewService(db, log)
	parserSrv := parser.NewService(log, externalSvcReader)
	manifestSrv := manifest.NewService(db, log, parserSrv, include.NewService(db, log))
	flagSrv := feature_flags.NewService(db, nil, log)
	dataSvc := data.NewService(db, log)
	protoHelper := transformer.NewHelper(dataSvc, db.GormDb, log)
	includeSvc := include.NewService(db, log)
	overrideSvc := override.NewService(db, log, includeSvc, manifestSrv)
	systemSvc := system.NewService(db, log)
	tmpSvc := template.NewService(mapService, externalSvcReader)
	deployServer := deploy2.NewHandler(
		log,
		statusSvc,
		deploy,
		dataSvc,
		protoHelper,
		mapService,
		manifestSrv,
		batch.NewService(db, log),
		externalSvcWriter,
		flagSrv,
		overrideSvc,
		secretSvc,
		systemSvc,
		tmpSvc,
	)

	return deployServer
}
