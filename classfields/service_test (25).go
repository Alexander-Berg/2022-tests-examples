package deployment

import (
	"fmt"
	"math/rand"
	"strconv"
	"sync"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/hook/hook"
	"github.com/YandexClassifieds/shiva/cmd/hook/hook/model"
	"github.com/YandexClassifieds/shiva/cmd/hook/setting"
	event "github.com/YandexClassifieds/shiva/pb/shiva/events/event2"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/dtype"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/pkg/mq"
	"github.com/YandexClassifieds/shiva/pkg/mq/conf"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/proto"
	"google.golang.org/protobuf/types/known/timestamppb"
)

var (
	r *rand.Rand
)

func init() {
	r = rand.New(rand.NewSource(time.Now().UnixNano()))
}

func TestSendNewHook(t *testing.T) {
	test.RunUp(t)
	mx := sync.Mutex{}
	s := newService(t)
	parentUUID := uuid.New().String()
	actual := prepareSetting(t, s.settingS)
	hookC := mq.Consume(conf.NewConsumerConf("shiva_hook", t.Name()), s.log, nil, func(msg *mq.Message) error {
		hMsg := &hook.Hook{}
		require.NoError(t, proto.Unmarshal(msg.Payload, hMsg))

		h, err := s.hookS.Get(hMsg.Id)
		if err != nil {
			return nil
		}

		// skip another message
		if h.ParentUUID != parentUUID {
			return nil
		}
		_, ok := actual[h.SettingID]
		assert.True(t, ok)
		mx.Lock()
		defer mx.Unlock()
		actual[h.SettingID] = true
		return nil
	})

	deploymentP := mq.NewProducer(conf.NewProducerConf("shiva_deployment"), s.log)
	push(t, parentUUID, deploymentP)

	test.Wait(t, func() error {
		mx.Lock()
		defer mx.Unlock()
		for _, v := range actual {
			if !v {
				return fmt.Errorf("messages not processed")
			}
		}
		return nil
	})

	hookC.Close()
	<-hookC.Closed
}

func TestProcessedHook(t *testing.T) {
	test.RunUp(t)

	eventUuid := uuid.New().String()
	depl := makeDeployment(t)
	consumer := newService(t)
	sett, err := consumer.settingS.NewSetting(&setting.Setting{
		Address:      "",
		Branch:       false,
		States:       []state.DeploymentState{state.DeploymentState_SUCCESS},
		ServiceNames: []string{t.Name()},
		DTypes:       []dtype.DeploymentType{dtype.DeploymentType_RUN},
	})
	require.NoError(t, err)

	h := &model.Hook{
		ParentUUID:   eventUuid,
		SettingID:    sett.ID(),
		DeploymentId: depl.ID(),
		State:        model.InFlight,
	}
	err = consumer.hookS.Save(h)
	require.NoError(t, err)

	err = consumer.handle(&event.Event{
		UUID:       eventUuid,
		Deployment: depl,
	})
	require.NoError(t, err)
}

func push(t *testing.T, parentUUID string, deploymentP *mq.Producer) {
	e := &event.Event{
		UUID:       parentUUID,
		Deployment: makeDeployment(t),
	}
	b, err := proto.Marshal(e)
	require.NoError(t, err)
	require.NoError(t, deploymentP.Push(mq.NewMessage(t.Name(), b, nil)))
}

func prepareSetting(t *testing.T, settingS *setting.Service) map[int64]bool {
	s1, err := settingS.NewSetting(&setting.Setting{
		Address:      "address1",
		ServiceNames: []string{t.Name()},
	})
	require.NoError(t, err)
	s2, err := settingS.NewSetting(&setting.Setting{
		Address:      "address2",
		ServiceNames: []string{t.Name()},
	})
	require.NoError(t, err)
	return map[int64]bool{
		s1.ID(): false,
		s2.ID(): false,
	}
}

func newService(t *testing.T) *Service {
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	settingS := setting.NewService(db, log)
	hookS := hook.NewService(hook.NewConf(), db, log, settingS)
	return RunService(settingS, hookS, log, nil)
}

func makeDeployment(t *testing.T) *deployment.Deployment {
	return &deployment.Deployment{
		Id:          strconv.FormatInt(r.Int63(), 10),
		ServiceName: t.Name(),
		Version:     "0.0.1",
		User:        "login",
		Branch:      "",
		Type:        dtype.DeploymentType_RUN,
		Layer:       layer.Layer_PROD,
		State:       state.DeploymentState_SUCCESS,
		Start:       timestamppb.Now(),
		End:         timestamppb.Now(),
	}
}
