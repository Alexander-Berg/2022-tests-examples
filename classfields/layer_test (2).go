package deploy

import (
	"context"
	"testing"

	gCommon "github.com/YandexClassifieds/shiva/cmd/shiva/api/grpc/common"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/issue_link"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/mocks"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	dModel "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	ds "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/status"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/common/logger"
	"github.com/YandexClassifieds/shiva/common/storage"
	dpb "github.com/YandexClassifieds/shiva/pb/shiva/api/deploy"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/pkg/tracker"
	stAPI "github.com/YandexClassifieds/shiva/pkg/tracker/api"
	"github.com/YandexClassifieds/shiva/test"
	smock "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/mock/mq"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

func TestLegacyLayer_WrongLayer_AsyncRun(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	//goland:noinspection GoDeprecation
	request := &dpb.RunRequest{Layer: "abc"}

	deployServer, _, _, _ := prepare(t)

	_, err := deployServer.AsyncRun(context.Background(), request)
	require.Equal(t, gCommon.ErrWrongLayer, err)
}

func TestLegacyLayer_WrongLayer_Run(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	//goland:noinspection GoDeprecation
	request := &dpb.RunRequest{
		Layer:        "abc",
		Login:        "danevge",
		Name:         sName,
		Version:      sVersion,
		Issues:       []string{"VOID-216"},
		Branch:       "",
		TrafficShare: false,
	}

	mockStream := newMock()
	deployServer, _, _, _ := prepare(t)
	err := deployServer.Run(request, mockStream)
	require.Equal(t, gCommon.ErrWrongLayer, err)
}

func TestLegacyLayer_WrongLayer_Stop(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	//goland:noinspection GoDeprecation
	request := &dpb.StopRequest{
		Layer:  "abc",
		Login:  "danevge",
		Name:   sName,
		Branch: "",
	}

	mockStream := newMock()
	deployServer, _, _, _ := prepare(t)
	err := deployServer.Stop(request, mockStream)
	require.Equal(t, gCommon.ErrWrongLayer, err)
}

func TestLegacyLayer_WrongLayer_Restart(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	//goland:noinspection GoDeprecation
	request := &dpb.RestartRequest{
		Layer:  "abc",
		Login:  "danevge",
		Name:   sName,
		Branch: "",
	}

	mockStream := newMock()
	deployServer, _, _, _ := prepare(t)
	err := deployServer.Restart(request, mockStream)
	require.Equal(t, gCommon.ErrWrongLayer, err)
}

func TestDefaultLayer_AsyncRun(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	deployMock := &mocks.IService{}
	// check that empty layer in request resolves to test
	deployMock.On("Run", mock.Anything, mock.AnythingOfType("RunParams")).Run(func(args mock.Arguments) {
		p := args.Get(1).(model.RunParams)
		require.Equal(t, model.RunParams{
			UUID:    p.UUID, // to skip uuid check
			Layer:   common.Test,
			Name:    sName,
			Version: sVersion,
			Login:   "danevge",
			Source:  config.OAuthSource,
		}, p)
	}).Return(nil, nil, storage.ErrStorage)

	deployServer, _, _, _ := prepareWithDeployMock(t, deployMock)

	request := &dpb.RunRequest{
		Login:   "danevge",
		Name:    sName,
		Version: sVersion,
	}
	_, _ = deployServer.AsyncRun(context.Background(), request)
}

func TestDefaultLayer_Run(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	// check deploy is called with proper layer
	deployMock := &mocks.IService{}
	deployMock.On("Run", mock.Anything, mock.AnythingOfType("RunParams")).Run(func(args mock.Arguments) {
		p := args.Get(1).(model.RunParams)
		require.Equal(t, model.RunParams{
			UUID:    p.UUID, // to skip uuid check
			Layer:   common.Test,
			Name:    sName,
			Version: sVersion,
			Login:   "danevge",
			Source:  config.OAuthSource,
		}, p)
	}).Return(nil, nil, storage.ErrStorage)

	deployServer, _, _, _ := prepareWithDeployMock(t, deployMock)

	mockStream := newMock()
	mockStream.On("Send", mock.Anything).Return(nil)

	request := &dpb.RunRequest{
		Login:   "danevge",
		Name:    sName,
		Version: sVersion,
	}
	_ = deployServer.Run(request, mockStream)
}

func TestDefaultLayer_Stop(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	deployMock := &mocks.IService{}
	deployMock.On("Stop", mock.Anything, mock.AnythingOfType("StopParams")).Run(func(args mock.Arguments) {
		p := args.Get(1).(dModel.StopParams)
		assert.Equal(t, common.Test, p.Layer)
		assert.Equal(t, sName, p.Name)
		assert.Equal(t, "danevge", p.Login)
	}).Return(nil, nil, storage.ErrStorage)

	deployServer, _, _, _ := prepareWithDeployMock(t, deployMock)

	mockStream := newMock()
	mockStream.On("Send", mock.Anything).Return(nil)

	request := &dpb.StopRequest{
		Login: "danevge",
		Name:  sName,
	}
	_ = deployServer.Stop(request, mockStream)
}

func TestDefaultLayer_Restart(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	request := &dpb.RestartRequest{
		Login: "danevge",
		Name:  sName,
	}

	deployMock := &mocks.IService{}
	deployMock.On("Restart", mock.Anything, mock.AnythingOfType("RestartParams")).Run(func(args mock.Arguments) {
		p := args.Get(1).(dModel.RestartParams)
		assert.Equal(t, common.Test, p.Layer)
		assert.Equal(t, sName, p.Name)
		assert.Equal(t, "danevge", p.Login)
	}).Return(nil, nil, storage.ErrStorage)

	deployServer, _, _, _ := prepareWithDeployMock(t, deployMock)

	mockStream := newMock()
	mockStream.On("Send", mock.Anything).Return(nil)

	_ = deployServer.Restart(request, mockStream)
}

func prepareWithDeployMock(t *testing.T, deploy deployment.IService) (*Handler, *storage.Database, *smock.Scheduler, logger.Logger) {
	db := test_db.NewDb(t)
	mScheduler := smock.NewMockScheduler()
	log := test.NewLogger(t)
	mapService := service_map.NewService(db, log, service_change.NewNotificationMock())
	statusSvc := ds.NewService(db, log, mapService)
	dataSvc := data.NewService(db, log)
	featureFlagsService := feature_flags.NewService(db, mq.NewProducerMock(), log)
	issueLinkS := issue_link.NewService(db, log, tracker.NewService(db, log, stAPI.NewApi(stAPI.NewConf(), log), dataSvc))
	extEnv := reader.NewService(db, log)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	deployServer := NewHandler(
		extEnv,
		deploy,
		dataSvc,
		statusSvc,
		featureFlagsService, mapService,
		staffService, issueLinkS, log)

	return deployServer, db, mScheduler, log
}
