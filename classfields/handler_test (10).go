package stream

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/telegram/bot"
	"github.com/YandexClassifieds/shiva/cmd/telegram/message"
	"github.com/YandexClassifieds/shiva/pb/shiva/api/deploy2"
	dpb "github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	dtypePb "github.com/YandexClassifieds/shiva/pb/shiva/types/dtype"
	infoPb "github.com/YandexClassifieds/shiva/pb/shiva/types/info"
	layerPb "github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	statePb "github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/test"
	smock "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/mock/election"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const (
	testChatID = int64(42)
)

func TestRestoreGeneralHandler_Duplicate(t *testing.T) {
	test.InitTestEnv()
	cfg := NewConf(time.Second*5, time.Second/10)
	log := test.NewLogger(t)
	dkv := smock.NewKVMock()
	elect := election.NewElectionStub()
	msgSvc := message.NewService(log, smock.NewNDAStub(), message.NewMarkdown())

	// prepare bot state
	mockBot := smock.NewTelegramMock(t)
	lastMsgId := mockBot.AddMessage(testChatID, &smock.MsgInfo{
		Text: "Command: Run\nService: `test-svc`\nLayer: Test\nVersion: `v1`\nInitiator: [alexander-s](https://staff.yandex-team.ru/alexander-s)\n\nState: *progress*",
		Buttons: []*bot.Button{
			{Message: "cancel", Callback: "/cancel -id 12345"},
		},
	})

	msgState := &MessageState{
		Login:     "alexander-s",
		ID:        "1337",
		ChatID:    testChatID,
		LastMsgID: lastMsgId,
		State:     statePb.DeploymentState_IN_PROGRESS,
		InitState: statePb.DeploymentState_IN_PROGRESS,
	}

	// prepare sender
	sh := &stateHelper{
		C:            make(chan *deploy2.StateResponse),
		DeploymentID: "12345",
		Layer:        layerPb.Layer_TEST,
		Service:      "test-svc",
		Version:      "v1",
		User:         "alexander-s",
		Type:         dtypePb.DeploymentType_RUN,
	}
	go func() {
		sh.Push(statePb.DeploymentState_IN_PROGRESS) // err not modified
		sh.Push(statePb.DeploymentState_IN_PROGRESS) // should be skipped
		sh.Push(statePb.DeploymentState_SUCCESS)     // final msg
		sh.Done()
	}()

	handlerDone := make(chan struct{})
	go func() {
		RestoreGeneralHandler(cfg, log, mockBot, msgSvc, elect, dkv, msgState, sh.C).Start()
		close(handlerDone)
	}()
	select {
	case <-handlerDone:
	case <-time.After(time.Second * 10):
		t.Fatalf("general handler wait timeout")
	}

	// verify state
	history := mockBot.History(testChatID)
	require.Len(t, history, 2)
	assert.Contains(t, history[0].Text, "State: *success*")
	assert.Contains(t, history[1].Text, "Run *success*")
	assert.Equal(t, 1, mockBot.NumDuplicateErrors)
}

type stateHelper struct {
	C            chan *deploy2.StateResponse
	DeploymentID string
	Layer        layerPb.Layer
	Service      string
	Version      string
	User         string
	Type         dtypePb.DeploymentType
}

func (s *stateHelper) Push(state statePb.DeploymentState) {
	s.C <- &deploy2.StateResponse{
		Service: &infoPb.DeploymentInfo{
			Deployment: &dpb.Deployment{
				Id:          s.DeploymentID,
				Layer:       s.Layer,
				ServiceName: s.Service,
				Version:     s.Version,
				User:        s.User,
				Type:        s.Type,
				State:       state,
			},
		},
	}
}

func (s *stateHelper) Done() {
	close(s.C)
}
