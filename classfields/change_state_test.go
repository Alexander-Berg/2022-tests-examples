package notification

import (
	"strconv"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/context"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pb/shiva/events/event2"
	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/traffic"
	manifest "github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/YandexClassifieds/shiva/pkg/mq/event2_mq"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	"github.com/YandexClassifieds/shiva/pkg/tracker/model/issue"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/golang/protobuf/ptypes"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestEvent(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	log := test.NewLogger(t)

	readChan := make(chan struct{}, 2)

	ctx := &context.Context{
		Meta: &context.Meta{
			Deployment: &model.Deployment{
				Name:             "test-svc",
				DeployManifestID: 1,
				ServiceMapsID:    2,
				Layer:            common.Test,
				Version:          "v1",
				Branch:           "b1",
				StartDate:        time.Now(),
				EndDate:          time.Now().Add(time.Second),
				State:            model.Success,
				Type:             common.Run,
				Traffic:          traffic.Traffic_ONE_PERCENT,
				Comment:          "comment",
				Source:           "source",
				Status: &deployment.Status{
					Numbers:         nil,
					Provides:        nil,
					RevertProvides:  nil,
					RevertType:      0,
					Description:     "some desc",
					Error:           nil,
					Warnings:        nil,
					FailAllocations: nil,
				},
			},
			User: &staff.User{
				Login: "test-login",
			},
			Issues:     []*issue.Issue{{Key: "VOID-1"}, {Key: "VOID-2"}},
			ServiceMap: &proto.ServiceMap{Name: "test-svc"},
			Manifest:   &manifest.Manifest{Name: "test-svc"},
		},
	}

	consumer := event2_mq.StartConsumer("new-topic", "producer-test", log, nil,
		func(e *event2.Event) error {
			assertEvent(t, e, ctx)
			readChan <- struct{}{}
			return nil
		})
	defer consumer.Stop()

	producer := NewProducer("new-topic", log)
	err := producer.Notify(ctx)
	require.NoError(t, err)

	timer := time.NewTimer(20 * time.Second)
	defer timer.Stop()
	select {
	case <-readChan:
	case <-timer.C:
		assert.FailNow(t, "message was not read")
	}
}

func TestCanaryEvent(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	log := test.NewLogger(t)

	readChan := make(chan struct{})

	ctx := &context.Context{
		Meta: &context.Meta{
			Deployment: &model.Deployment{
				Name:             "test-svc",
				DeployManifestID: 1,
				ServiceMapsID:    2,
				Layer:            common.Test,
				Version:          "v1",
				Branch:           "b1",
				StartDate:        time.Now(),
				EndDate:          time.Now().Add(time.Second),
				State:            model.CanarySuccess,
				Type:             common.Run,
				Traffic:          traffic.Traffic_ONE_PERCENT,
				Comment:          "comment",
				Source:           "source",
			},
			User: &staff.User{
				Login: "test-login",
			},
			Issues:     []*issue.Issue{{Key: "VOID-1"}, {Key: "VOID-2"}},
			ServiceMap: &proto.ServiceMap{Name: "test-svc"},
			Manifest:   &manifest.Manifest{Name: "test-svc"},
		},
	}

	consumer := event2_mq.StartConsumer("new-topic", "producer-test", log, nil,
		func(e *event2.Event) error {
			assertEvent(t, e, ctx)
			readChan <- struct{}{}
			return nil
		})
	defer consumer.Stop()

	producer := NewProducer("new-topic", log)
	err := producer.Notify(ctx)
	require.NoError(t, err)

	timer := time.NewTimer(20 * time.Second)
	select {
	case <-readChan:
	case <-timer.C:
		assert.FailNow(t, "message was not read")
	}
	timer.Stop()
}

func assertEvent(t *testing.T, e *event2.Event, ctx *context.Context) {

	layer, err := e.Deployment.CommonLayer()
	require.NoError(t, err)
	dtype, err := e.Deployment.CommonType()
	require.NoError(t, err)
	startTime, err := ptypes.Timestamp(e.Deployment.Start)
	require.NoError(t, err)
	endTime, err := ptypes.Timestamp(e.Deployment.End)
	require.NoError(t, err)

	assert.Equal(t, layer, ctx.Deployment.Layer)
	assert.Equal(t, dtype, ctx.Deployment.Type)
	assert.Equal(t, startTime.Unix(), ctx.Deployment.StartDate.Unix())
	assert.Equal(t, endTime.Unix(), ctx.Deployment.EndDate.Unix())
	assert.Equal(t, e.Deployment.ServiceName, ctx.Deployment.Name)
	assert.Equal(t, e.Deployment.Version, ctx.Deployment.Version)
	assert.Equal(t, e.Deployment.Branch, ctx.Deployment.Branch)
	assert.Equal(t, e.Deployment.Traffic, ctx.Deployment.Traffic)
	assert.Equal(t, e.Deployment.Comment, ctx.Deployment.Comment)
	assert.Equal(t, e.Deployment.Source, ctx.Deployment.Source)
	assert.Equal(t, e.Deployment.ManifestId, strconv.FormatInt(ctx.Deployment.DeployManifestID, 10))
	assert.Equal(t, e.Deployment.SmapId, strconv.FormatInt(ctx.Deployment.ServiceMapsID, 10))
	assert.Equal(t, e.Deployment.State, stateMap[ctx.Deployment.State])
	assert.Equal(t, e.Deployment.User, ctx.User.Login)
	assert.Equal(t, e.Deployment.Issue, ctx.IssueKey())
	assert.Equal(t, e.Deployment.Issues, ctx.IssueKeys())
	assert.Equal(t, e.ServiceMap.Name, ctx.ServiceMap.Name)
	assert.Equal(t, e.Manifest.Name, ctx.Manifest.Name)
}
