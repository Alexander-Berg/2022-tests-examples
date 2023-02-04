package subscribe

import (
	"fmt"
	"math/rand"
	"testing"

	apiSm "github.com/YandexClassifieds/shiva/pb/shiva/api/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/batch_task"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/test"
	smMocks "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	mock2 "github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

func TestSubscribeUnsubscribe(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	chatID := chat(t)
	s := NewSubscribe(name(t), chatID, layer.Layer_TEST, false, All, true)
	smMock := &smMocks.ServiceMapsClient{}
	smMock.On("Get", mock2.Anything, mock2.Anything).Return(
		&apiSm.ServiceData{Service: &service_map.ServiceMap{Type: service_map.ServiceType_service}}, nil)
	service := NewService(test_db.NewDb(t), smMock, test.NewLogger(t))
	require.NoError(t, service.Subscribe(s.Name, s.ChatID, layer.FromCommonLayer(s.Layer), s.Branch, All, true))
	subscriptions, err := service.Subscriptions(chatID)
	require.NoError(t, err)
	assertContains(t, subscriptions, s)
	require.NoError(t, service.Unsubscribe(s.Name, s.ChatID, layer.FromCommonLayer(s.Layer)))
	subscriptions, err = service.Subscriptions(chatID)
	require.NoError(t, err)
	assertNotContains(t, subscriptions, s)
}

func TestBatchSubscribeWithState(t *testing.T) {
	type TestCase struct {
		name   string
		stGr   StateGroup
		state  batch_task.State
		notify bool
	}
	cases := []TestCase{
		{
			stGr:   All,
			state:  batch_task.State_Process,
			notify: true,
		},
		{
			stGr:   All,
			state:  batch_task.State_Success,
			notify: true,
		},
		{
			stGr:   All,
			state:  batch_task.State_Failed,
			notify: true,
		},
		{
			stGr:   All,
			state:  batch_task.State_Skipped,
			notify: true,
		},
		{
			stGr:   All,
			state:  batch_task.State_Canceling,
			notify: true,
		},
		{
			stGr:   All,
			state:  batch_task.State_Canceled,
			notify: true,
		},

		{
			stGr:   End,
			state:  batch_task.State_Process,
			notify: false,
		},
		{
			stGr:   End,
			state:  batch_task.State_Success,
			notify: true,
		},
		{
			stGr:   End,
			state:  batch_task.State_Failed,
			notify: true,
		},
		{
			stGr:   End,
			state:  batch_task.State_Skipped,
			notify: true,
		},
		{
			stGr:   End,
			state:  batch_task.State_Canceling,
			notify: false,
		},
		{
			stGr:   End,
			state:  batch_task.State_Canceled,
			notify: true,
		},

		{
			stGr:   Fail,
			state:  batch_task.State_Process,
			notify: false,
		},
		{
			stGr:   Fail,
			state:  batch_task.State_Success,
			notify: false,
		},
		{
			stGr:   Fail,
			state:  batch_task.State_Failed,
			notify: true,
		},
		{
			stGr:   Fail,
			state:  batch_task.State_Skipped,
			notify: true,
		},
		{
			stGr:   Fail,
			state:  batch_task.State_Canceling,
			notify: false,
		},
		{
			stGr:   Fail,
			state:  batch_task.State_Canceled,
			notify: true,
		},
	}
	for _, c := range cases {
		t.Run(fmt.Sprintf("%s-%s", c.stGr.String(), c.state.String()), func(t *testing.T) {
			test.RunUp(t)
			defer test.Down(t)
			db := test_db.NewSeparatedDb(t)

			chatID := chat(t)
			smMock := &smMocks.ServiceMapsClient{}
			smMock.On("Get", mock2.Anything, mock2.Anything).Return(
				&apiSm.ServiceData{Service: &service_map.ServiceMap{Type: service_map.ServiceType_batch}}, nil)
			service := NewService(db, smMock, test.NewLogger(t))
			require.NoError(t, service.Subscribe(t.Name(), chatID, layer.Layer_TEST, false, c.stGr, true))
			notifications, err := service.BatchTaskNotifications(t.Name(), layer.Layer_TEST, c.state)
			require.NoError(t, err)
			assert.True(t, len(notifications) < 2)
			notify := len(notifications) > 0
			assert.Equal(t, notify, c.notify)
		})
	}
}

func TestSubscribesToOneChannel(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	smMock := &smMocks.ServiceMapsClient{}
	smMock.On("Get", mock2.Anything, mock2.Anything).Return(
		&apiSm.ServiceData{Service: &service_map.ServiceMap{Type: service_map.ServiceType_service}}, nil)
	service := NewService(test_db.NewDb(t), smMock, test.NewLogger(t))
	chatID := chat(t)
	name := name(t)
	newSubscribes := []*Subscribe{
		NewSubscribe(name+"a", chatID, layer.Layer_TEST, false, All, true),
		NewSubscribe(name+"a", chatID, layer.Layer_PROD, false, All, true),
		NewSubscribe(name+"b", chatID, layer.Layer_PROD, false, All, true),
		NewSubscribe(name+"c", chatID, layer.Layer_PROD, false, All, true),
		NewSubscribe(name+"d", chatID, layer.Layer_TEST, true, All, true),
		NewSubscribe(name+"e", chatID, layer.Layer_PROD, true, All, true),
		NewSubscribe(name+"f", chatID, layer.Layer_TEST, true, All, true),
		NewSubscribe(name+"g", chatID, layer.Layer_PROD, true, All, true),
	}
	for _, s := range newSubscribes {

		require.NoError(t, service.Subscribe(s.Name, s.ChatID, layer.FromCommonLayer(s.Layer), s.Branch, All, true))
	}
	subscriptions, err := service.Subscriptions(chatID)
	require.NoError(t, err)
	for _, s := range newSubscribes {
		assertContains(t, subscriptions, s)
	}
}

func TestSubscribesOneService(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	smMock := &smMocks.ServiceMapsClient{}
	smMock.On("Get", mock2.Anything, mock2.Anything).Return(
		&apiSm.ServiceData{Service: &service_map.ServiceMap{Type: service_map.ServiceType_service}}, nil)
	service := NewService(test_db.NewDb(t), smMock, test.NewLogger(t))
	chatID := chat(t)
	name := name(t)
	newSubscriptions := []*Subscribe{
		NewSubscribe(name, chatID, layer.Layer_TEST, false, All, true),
		NewSubscribe(name, chatID, layer.Layer_PROD, false, All, true),
		NewSubscribe(name, chatID+2, layer.Layer_TEST, false, All, true),
		NewSubscribe(name, chatID+3, layer.Layer_PROD, false, All, true),
		NewSubscribe(name, chatID+4, layer.Layer_TEST, true, All, true),
		NewSubscribe(name, chatID+5, layer.Layer_PROD, true, All, true),
		NewSubscribe(name, chatID+6, layer.Layer_TEST, true, All, true),
		NewSubscribe(name, chatID+7, layer.Layer_PROD, true, All, true),
	}
	for _, s := range newSubscriptions {

		require.NoError(t, service.Subscribe(s.Name, s.ChatID, layer.FromCommonLayer(s.Layer), s.Branch, All, true))
	}

	for _, s := range newSubscriptions {
		subscriptions, err := service.Subscriptions(s.ChatID)
		require.NoError(t, err)
		assertContains(t, subscriptions, s)
	}
}

func TestUpdateSubscribe(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)

	chatID := chat(t)
	name := name(t)
	s := NewSubscribe(name, chatID, layer.Layer_TEST, false, All, true)
	sb := NewSubscribe(name, chatID, layer.Layer_TEST, true, All, true)

	smMock := &smMocks.ServiceMapsClient{}
	smMock.On("Get", mock2.Anything, mock2.Anything).Return(
		&apiSm.ServiceData{Service: &service_map.ServiceMap{Type: service_map.ServiceType_service}}, nil)
	service := NewService(test_db.NewDb(t), smMock, test.NewLogger(t))
	require.NoError(t, service.Subscribe(s.Name, s.ChatID, layer.FromCommonLayer(s.Layer), s.Branch, All, true))
	subscriptions, err := service.Subscriptions(chatID)
	require.NoError(t, err)
	assertContains(t, subscriptions, s)

	require.NoError(t, service.Subscribe(sb.Name, sb.ChatID, layer.FromCommonLayer(sb.Layer), sb.Branch, All, true))
	subscriptions, err = service.Subscriptions(chatID)
	require.NoError(t, err)
	assertContains(t, subscriptions, sb)
	assertNotContains(t, subscriptions, s)
}

func TestMigrate(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	smMock := &smMocks.ServiceMapsClient{}
	smMock.On("Get", mock2.Anything, mock2.Anything).Return(
		&apiSm.ServiceData{Service: &service_map.ServiceMap{Type: service_map.ServiceType_service}}, nil)
	service := NewService(db, smMock, test.NewLogger(t))

	name := name(t)
	chatID := chat(t)
	oldChatID := chat(t)
	newChatID := chat(t)

	subs := []*Subscribe{
		NewSubscribe(name, oldChatID, layer.Layer_TEST, false, All, true),
		NewSubscribe(name, oldChatID, layer.Layer_PROD, false, All, true),
		NewSubscribe(name, chatID, layer.Layer_TEST, false, All, true),
		NewSubscribe(name, chatID, layer.Layer_PROD, false, All, true),
	}
	for _, s := range subs {
		require.NoError(t, service.Subscribe(s.Name, s.ChatID, layer.FromCommonLayer(s.Layer), s.Branch, All, true))
	}

	require.NoError(t, service.Migrate(oldChatID, newChatID))

	for _, l := range []layer.Layer{layer.Layer_TEST, layer.Layer_PROD} {
		subscriptions, err := service.storage.GetAllByService(name, l)
		require.NoError(t, err)
		assert.Len(t, subscriptions, 2)
		assert.Equal(t, chatID, subscriptions[0].ChatID)
		assert.Equal(t, newChatID, subscriptions[1].ChatID)
	}
}

func name(t *testing.T) string {

	return "test_svc_" + t.Name()
}

func chat(t *testing.T) int64 {

	return rand.Int63()
}

func assertNotContains(t *testing.T, subscriptions []*Subscribe, expected *Subscribe) {

	result := find(subscriptions, expected)
	if result != nil {
		assert.FailNow(t, "subscribe found", expected)
	}
}

func assertContains(t *testing.T, subscriptions []*Subscribe, expected *Subscribe) {

	result := find(subscriptions, expected)
	if result == nil {
		assert.FailNow(t, "subscribe not found", expected)
	}
}

func find(subscriptions []*Subscribe, expected *Subscribe) *Subscribe {
	for _, s := range subscriptions {

		if compare(s, expected) {
			return s
		}
	}
	return nil
}

func compare(s1, s2 *Subscribe) bool {

	return s1.Name == s2.Name &&
		s1.Layer == s2.Layer &&
		s1.ChatID == s2.ChatID &&
		s1.Branch == s2.Branch
}
