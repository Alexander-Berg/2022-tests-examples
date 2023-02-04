package tracker

import (
	"errors"
	"net/http"
	"strconv"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/approve"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	deploymentModel "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/cmd/shiva/transformer"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/user_error"
	"github.com/YandexClassifieds/shiva/pb/shiva/events/event2"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/dtype"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	"github.com/YandexClassifieds/shiva/pkg/tracker/api"
	"github.com/YandexClassifieds/shiva/pkg/tracker/model/issue"
	"github.com/YandexClassifieds/shiva/test"
	mock2 "github.com/YandexClassifieds/shiva/test/mock"
	mqMock "github.com/YandexClassifieds/shiva/test/mock/mq"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/golang/protobuf/ptypes"
	"github.com/google/uuid"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"
)

const (
	key              = "TEST-30540"
	key1             = "VOID-102"
	key2             = "VOID-103"
	trackerTestTopic = "tracker_test"
)

func init() {
	test.InitTestEnv()
}

func TestPostRemoteLink(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	m := &mock2.TrackerAPI{}
	m.On("GetIssue", key1).Return(&api.Issue{
		Self:    "",
		Id:      "key:" + key1,
		Key:     key1,
		Version: 0,
		Summary: "issue" + key1,
	}, nil)
	m.On("GetIssue", key2).Return(&api.Issue{
		Self:    "",
		Id:      "key:" + key2,
		Key:     key2,
		Version: 0,
		Summary: "issue" + key2,
	}, nil)
	m.On("PostRemoteLink", key1, mock.Anything).Return(&api.RemoteLinkResponse{}, &common.HttpStatusError{
		Code: http.StatusConflict,
	})
	m.On("PostRemoteLink", key2, mock.Anything).Return(&api.RemoteLinkResponse{Self: "self-uri"}, nil)
	m.On("GetRemoteLinks", mock.Anything, mock.Anything).Return(nil, nil)

	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	featureFlagsService := feature_flags.NewService(db, mqMock.NewProducerMock(), log)
	s := NewService(test_db.NewDb(t), test.NewLogger(t), m, nil)
	issue1, err := s.GetIssueByKey(key1)
	require.NoError(t, err)
	issue2, err := s.GetIssueByKey(key2)
	require.NoError(t, err)
	err = featureFlagsService.Set(
		&feature_flags.FeatureFlag{
			Flag:  feature_flags.TrackerPostDeploymentLink.String(),
			Value: true,
		},
	)
	require.NoError(t, err)

	e := newEvent(dtype.DeploymentType_RUN, state.DeploymentState_SUCCESS, issue1, issue2)
	require.NoError(t, s.handleDeployEvent(e))
}

func TestPostRemoteLinkActualDeployment(t *testing.T) {
	log := test.NewLogger(t)
	db := test_db.NewSeparatedDb(t)
	db.GormDb.DisableForeignKeyConstraintWhenMigrating = true

	db.GormDb.AutoMigrate(deploymentModel.Deployment{}, approve.Approve{})

	const linkId int64 = 100

	d1 := &deploymentModel.Deployment{
		Name: "svc1",
		Issues: []*issue.Issue{{
			Key: key1,
		}},
		Branch: "branch1",
		Type:   common.Run,
		Layer:  common.Test,
		State:  deploymentModel.Success,
	}
	d2 := &deploymentModel.Deployment{
		Name: "svc1",
		Issues: []*issue.Issue{{
			Key: key1,
		}},
		Branch: "branch1",
		Type:   common.Run,
		Layer:  common.Test,
		State:  deploymentModel.Success,
	}

	require.NoError(t, db.GormDb.Create(d1).Error)

	m := &mock2.TrackerAPI{}
	m.On("GetRemoteLinks", key1, originId).Return([]*api.RemoteLinkResponse{
		{
			ID: linkId,
			Link: api.RemoteLink{
				Key:         strconv.Itoa(int(d1.ID)),
				Application: api.RemoteLinkApplication{},
			},
		},
	}, nil).Once()
	m.On("DeleteRemoteLink", key1, linkId).Return(nil).Once()
	m.On("PostRemoteLink", key1, mock.Anything).Return(&api.RemoteLinkResponse{Self: "self-uri"}, nil).Once()

	featureFlagsService := feature_flags.NewService(db, mqMock.NewProducerMock(), log)
	err := featureFlagsService.Set(
		&feature_flags.FeatureFlag{
			Flag:  feature_flags.TrackerPostDeploymentLink.String(),
			Value: true,
		},
	)
	require.NoError(t, err)
	s := NewService(test_db.NewDb(t), test.NewLogger(t), m, data.NewService(db, log))

	e := &event2.Event{UUID: uuid.New().String(), Deployment: transformer.DeploymentByModel(d2)}

	require.NoError(t, s.handleDeployEvent(e))
	m.AssertExpectations(t)
}

func TestSkipParticularDeploymentEvents(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	m := &mock2.TrackerAPI{}
	m.On("GetIssue", key1).Return(&api.Issue{
		Self:    "",
		Id:      "key:" + key1,
		Key:     key1,
		Version: 0,
		Summary: "issue" + key1,
	}, nil)

	db := test_db.NewDb(t)
	log := test.NewLogger(t)

	featureFlagsService := feature_flags.NewService(db, mqMock.NewProducerMock(), log)
	s := NewService(test_db.NewDb(t), test.NewLogger(t), m, data.NewService(db, log))
	issue, err := s.GetIssueByKey(key1)
	require.NoError(t, err)
	err = featureFlagsService.Set(
		&feature_flags.FeatureFlag{
			Flag:  feature_flags.TrackerPostDeploymentLink.String(),
			Value: true,
		},
	)
	require.NoError(t, err)

	eventsToSkip := []*event2.Event{
		newEvent(dtype.DeploymentType_UNKNOWN, state.DeploymentState_SUCCESS, issue),
		newEvent(dtype.DeploymentType_RESTART, state.DeploymentState_SUCCESS, issue),
		newEvent(dtype.DeploymentType_CANCEL, state.DeploymentState_SUCCESS, issue),
		newEvent(dtype.DeploymentType_REVERT, state.DeploymentState_SUCCESS, issue),
		newEvent(dtype.DeploymentType_UPDATE, state.DeploymentState_SUCCESS, issue),
		newEvent(dtype.DeploymentType_RUN, state.DeploymentState_FAILED, issue),
		newEvent(dtype.DeploymentType_RUN, state.DeploymentState_SUCCESS),
	}

	for _, evt := range eventsToSkip {
		require.NoError(t, s.handleDeployEvent(evt))
		m.AssertNotCalled(t, "PostRemoteLink")
	}
}

func TestService_GetTaskByKey(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)

	log := test.NewLogger(t)
	service := NewService(test_db.NewDb(t), log, api.NewApi(api.NewConf(), log), nil)

	i, err := service.GetIssueByKey(key)
	require.NoError(t, err)
	assert.Equal(t, key, i.Key)

	if _, err := service.issueStorage.GetByKey(key); errors.Is(err, gorm.ErrRecordNotFound) {
		t.Fatal(err)
	}

	issue2, err := service.GetIssueByKey(key)
	require.NoError(t, err)
	assert.Equal(t, issue2.ID, i.ID)
}

func TestWrongIssue(t *testing.T) {
	testCases := []string{"a", "VOID-123_hotfix"}

	test.RunUp(t)
	defer test.Down(t)

	log := test.NewLogger(t)
	db := test_db.NewSeparatedDb(t)
	service := NewService(test_db.NewDb(t), log, api.NewApi(api.NewConf(), log), data.NewService(db, log))

	for _, testCase := range testCases {
		_, err := service.GetIssueByKey(testCase)
		require.Error(t, err)
		userError := &user_error.UserError{}
		require.True(t, errors.As(err, &userError))
		require.EqualError(t, err, "issue not found")
	}
}

func newEvent(dType dtype.DeploymentType, dState state.DeploymentState, issues ...*issue.Issue) *event2.Event {

	startTime := ptypes.TimestampNow()
	endTime, _ := ptypes.TimestampProto(time.Now().Add(time.Minute))
	var keys []string
	for _, i := range issues {
		keys = append(keys, i.Key)
	}
	return &event2.Event{
		UUID: uuid.New().String(),
		Deployment: &deployment.Deployment{
			User:        "danevge",
			Branch:      "",
			Issues:      keys,
			ServiceName: "test_service",
			Version:     "0.0.1-8fa14cdd754f91cc6554c9e71929cce7",
			Start:       startTime,
			End:         endTime,
			Layer:       layer.Layer_TEST,
			State:       dState,
			Type:        dType,
		},
	}
}

func deleteOldComments(key string, api *api.API) {

	comments, err := api.GetComments(key)
	if err != nil {
		logrus.WithError(err).Errorf("TEST: Delete old comments fail")
		return
	}
	if len(comments) == 0 {
		return
	}
	for _, c := range comments {
		if time.Now().Sub(c.CreatedAt.Time) > 3*time.Hour {
			err := api.DeleteComment(key, c.ID)
			if err != nil {
				logrus.WithError(err).Infof("TEST: delete comment fail %d %s %v", c.ID, c.Text, c.CreatedAt)
				continue
			}
			logrus.Infof("TEST: delete comment success %d %s %v", c.ID, c.Text, c.CreatedAt)
		}
	}
}
