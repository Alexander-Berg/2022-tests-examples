package cleaner

import (
	"errors"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/cleaner/branch/store/branch"
	"github.com/YandexClassifieds/shiva/cmd/cleaner/branch/store/issue"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pb/shiva/api/deploy2"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	trackerAPI "github.com/YandexClassifieds/shiva/pkg/tracker/api"
	"github.com/YandexClassifieds/shiva/test"
	mock2 "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/mock/election"
	"github.com/YandexClassifieds/shiva/test/mock/mockery/mocks"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

func TestService_CleanBranch(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	cleanRecord := branch.Model{Layer: common.Test, Name: "test-svc", Branch: "b1"}
	db.GormDb.Create(&cleanRecord)

	shivaClient := new(mocks.DeployServiceClient)
	resp := &deploy2.DeploymentResponse{
		Deployment: &deployment.Deployment{State: state.DeploymentState_SUCCESS},
	}
	shivaClient.On("Stop", mock.Anything, mock.Anything).Return(resp, nil).Once()
	service := (&Service{
		Conf:        NewConf(),
		DB:          db,
		Log:         test.NewLogger(t),
		Election:    election.NewElectionStub(),
		ShivaClient: shivaClient,
	}).Init()

	err := service.cleanBranch(&cleanRecord)
	require.NoError(t, err)
	resultRecord := branch.Model{}
	err = db.GormDb.Find(&resultRecord, branch.Model{Layer: common.Test, Name: "test-svc", Branch: "b1"}).Error
	require.NoError(t, err)
	assert.Equal(t, branch.Finished, cleanRecord.State)
}

func TestService_CleanBranch_Fail(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	cleanRecord := branch.Model{Layer: common.Test, Name: "test-svc", Branch: "b1"}
	db.GormDb.Create(&cleanRecord)

	shivaClient := new(mocks.DeployServiceClient)
	shivaClientError := errors.New("fail")
	shivaClient.On("Stop", mock.Anything, mock.Anything).Return(nil, shivaClientError).Once()
	service := (&Service{
		Conf:        NewConf(),
		DB:          db,
		Log:         test.NewLogger(t),
		Election:    election.NewElectionStub(),
		ShivaClient: shivaClient,
	}).Init()

	err := service.cleanBranch(&cleanRecord)
	assert.Equal(t, err, shivaClientError)
	assert.Equal(t, branch.Started, cleanRecord.State)
}

func TestService_CheckIssues(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(branch.Model{}))

	issue1 := &issue.Model{Key: "VOID-1"}
	issue2 := &issue.Model{Key: "VOID-2"}
	branch1 := &branch.Model{
		Layer:  common.Test,
		Name:   "srv1",
		Branch: "b1",
		Issues: []*issue.Model{
			issue1,
			issue2,
		},
	}

	db.GormDb.Create(branch1)

	trackerClient := &mock2.TrackerAPI{}
	trackerClient.On("FindIssues", mock.Anything).Return(
		[]*trackerAPI.Issue{
			&trackerAPI.Issue{Key: issue1.Key, Status: trackerAPI.IssueStatus{Key: "closed"}},
			&trackerAPI.Issue{Key: issue2.Key, Status: trackerAPI.IssueStatus{Key: "open"}},
		},
		nil,
	)

	service := (&Service{
		Conf:          NewConf(),
		DB:            db,
		Log:           test.NewLogger(t),
		Election:      election.NewElectionStub(),
		ShivaClient:   new(mocks.DeployServiceClient),
		TrackerClient: trackerClient,
	}).Init()

	err := service.checkIssues()
	assert.NoError(t, err)

	res, err := service.issueStorage.GetByKey(issue1.Key)
	assert.NoError(t, err)
	assert.Equal(t, issue.Closed, res.Status)

	res, err = service.issueStorage.GetByKey(issue2.Key)
	assert.NoError(t, err)
	assert.Equal(t, issue.Open, res.Status)
}

func TestService_TrySetTTL(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(branch.Model{}))

	issue1 := &issue.Model{Key: "VOID-1", Status: issue.Closed}
	issue2 := &issue.Model{Key: "VOID-2", Status: issue.Closed}
	branch1 := &branch.Model{
		Layer:  common.Test,
		Name:   "srv1",
		Branch: "b1",
		Issues: []*issue.Model{
			issue1,
			issue2,
		},
	}

	db.GormDb.Create(branch1)

	service := (&Service{
		Conf:          NewConf(),
		DB:            db,
		Log:           test.NewLogger(t),
		Election:      election.NewElectionStub(),
		ShivaClient:   new(mocks.DeployServiceClient),
		TrackerClient: &mock2.TrackerAPI{},
	}).Init()

	err := service.trySetTTL()
	assert.NoError(t, err)

	res, err := service.branchStorage.Get(branch1.Layer, branch1.Name, branch1.Branch)
	assert.NoError(t, err)
	assert.Equal(t, true, res.Expires.Valid)
}
