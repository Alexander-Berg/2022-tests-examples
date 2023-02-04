package handler

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/generator/task"
	events "github.com/YandexClassifieds/shiva/pb/shiva/events/change_conf"
	"github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pkg/mq"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/golang/protobuf/proto"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

const (
	srvName = "shiva-test-test"
)

func TestCheckTypes(t *testing.T) {
	for index, name := range service_map.ServiceType_name {
		t.Run(name, func(t *testing.T) {
			test.RunUp(t)
			defer test.Down(t)
			sType := service_map.ServiceType(index)
			_, want := allowTypes[sType]
			base := NewBase(nil, test.NewLogger(t), nil, nil)
			assert.Equal(t, want, !base.skip(&service_map.ServiceMap{
				Name: name,
				Type: sType,
				Path: name,
			}))
		})
	}
}

func TestSkipOnTestLayer(t *testing.T) {
	// prepare
	test.RunUp(t)
	defer test.Down(t)
	c := newConsumer(t)
	// test
	_, msg := event(t, "admin-www", "", events.ChangeType_NEW, service_map.ServiceType_service)
	require.NoError(t, c.process(msg))
	// assert
	assert.Nil(t, c.handler.(*EmptyHandler).Get())
}

func TestSkipByHandlerField(t *testing.T) {
	// prepare
	test.RunUp(t)
	defer test.Down(t)
	c := newConsumer(t)
	// test
	_, msg := event(t, srvName, "No", events.ChangeType_NEW, service_map.ServiceType_service)
	require.NoError(t, c.process(msg))
	// assert
	assert.Nil(t, c.handler.(*EmptyHandler).Get())
}

func TestHandlerField(t *testing.T) {
	// prepare
	test.RunUp(t)
	defer test.Down(t)
	c := newConsumer(t)
	c.handler.(*EmptyHandler).On("OnNew", mock.Anything, mock.Anything).Return(nil)
	// test
	e, msg := event(t, srvName, c.handler.Name(), events.ChangeType_NEW, service_map.ServiceType_service)
	require.NoError(t, c.process(msg))
	// assert
	result := c.handler.(*EmptyHandler).Get()
	assert.Equal(t, e.UUID, result.UUID)
}

func TestSkipEndState(t *testing.T) {
	// prepare
	test.RunUp(t)
	defer test.Down(t)
	c := newConsumer(t)
	// test
	e, msg := event(t, srvName, "", events.ChangeType_NEW, service_map.ServiceType_service)
	newTask := &task.Task{
		UUID:       e.UUID,
		Service:    e.New.Name,
		ChangeType: e.ChangeType,
		Handler:    c.handler.Name(),
		State:      task.Success,
	}
	require.NoError(t, c.taskS.Save(newTask))
	require.NoError(t, c.process(msg))
	// assert
	assert.Nil(t, c.handler.(*EmptyHandler).Get())
}

func TestRetry(t *testing.T) {
	// prepare
	test.RunUp(t)
	defer test.Down(t)
	c := newConsumer(t)
	c.handler.(*EmptyHandler).On("OnNew", mock.Anything, mock.Anything).Return(nil)
	// test
	e, msg := event(t, srvName, "", events.ChangeType_NEW, service_map.ServiceType_service)
	newTask := &task.Task{
		UUID:       e.UUID,
		Service:    e.New.Name,
		ChangeType: e.ChangeType,
		Handler:    c.handler.Name(),
		State:      task.New,
	}
	require.NoError(t, c.taskS.Save(newTask))
	require.NoError(t, c.process(msg))
	// assert
	result := c.handler.(*EmptyHandler).Get()
	assert.Equal(t, e.UUID, result.UUID)
	assert.Equal(t, newTask.ID, result.ID)
}

func TestNewMessage(t *testing.T) {
	// prepare
	test.RunUp(t)
	defer test.Down(t)
	c := newConsumer(t)
	c.handler.(*EmptyHandler).On("OnNew", mock.Anything, mock.Anything).Return(nil)
	// test
	e, msg := event(t, srvName, "", events.ChangeType_NEW, service_map.ServiceType_service)
	require.NoError(t, c.process(msg))
	// assert
	result := c.handler.(*EmptyHandler).Get()
	assert.Equal(t, e.UUID, result.UUID)
}

func TestUpdateMessage(t *testing.T) {
	// prepare
	test.RunUp(t)
	defer test.Down(t)
	c := newConsumer(t)
	c.handler.(*EmptyHandler).On("OnUpdate", mock.Anything, mock.Anything, mock.Anything).Return(nil)
	// test
	e, msg := event(t, srvName, "", events.ChangeType_UPDATE, service_map.ServiceType_service)
	require.NoError(t, c.process(msg))
	// assert
	result := c.handler.(*EmptyHandler).Get()
	assert.Equal(t, e.UUID, result.UUID)
}

func TestDeleteMessage(t *testing.T) {
	// prepare
	test.RunUp(t)
	defer test.Down(t)
	c := newConsumer(t)
	c.handler.(*EmptyHandler).On("OnDelete", mock.Anything, mock.Anything).Return(nil)
	// test
	e, msg := event(t, srvName, "", events.ChangeType_DELETE, service_map.ServiceType_service)
	require.NoError(t, c.process(msg))
	// assert
	result := c.handler.(*EmptyHandler).Get()
	assert.Equal(t, e.UUID, result.UUID)
}

func TestForceUpdateMessage(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	c := newConsumer(t)
	c.handler.(*EmptyHandler).On("OnForceUpdate", mock.Anything, mock.Anything).Return(nil)

	e, msg := event(t, srvName, "", events.ChangeType_FORCE, service_map.ServiceType_service)
	require.NoError(t, c.process(msg))

	result := c.handler.(*EmptyHandler).Get()
	assert.Equal(t, e.UUID, result.UUID)
}

func TestEnvUpdated(t *testing.T) {
	t.Skip("turn on after https://st.yandex-team.ru/PASSP-27423")

	test.RunUp(t)
	defer test.Down(t)
	c := newConsumer(t)
	c.handler.(*EmptyHandler).On("Updated", mock.Anything, mock.Anything).Return(nil)

	e, msg := event(t, srvName, "", events.ChangeType_ENV_UPDATED, service_map.ServiceType_service)
	require.NoError(t, c.process(msg))

	result := c.handler.(*EmptyHandler).Get()
	assert.Equal(t, e.UUID, result.UUID)
}

func TestSecretProdDontSkip(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	c := newConsumer(t)
	c.conf.layer = layer.Layer_PROD

	testCases := []struct {
		name        string
		handlerName string
	}{
		{
			name:        "dont skip test",
			handlerName: "secret_TEST",
		},
		{
			name:        "dont skip prod",
			handlerName: "secret_PROD",
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			c.handler.(*EmptyHandler).name = tc.handlerName
			c.handler.(*EmptyHandler).On("OnNew", mock.Anything, mock.Anything).Return(nil).Once()
			// test
			e, msg := event(t, srvName, "", events.ChangeType_NEW, service_map.ServiceType_service)
			require.NoError(t, c.process(msg))

			result := c.handler.(*EmptyHandler).Get()
			assert.Equal(t, e.UUID, result.UUID)
			c.handler.(*EmptyHandler).AssertExpectations(t)
		})
	}
}

func newConsumer(t *testing.T) *Consumer {
	log := test.NewLogger(t)
	taskS := task.NewService(log, test_db.NewDb(t))
	handler := NewEmptyHandler()

	return NewConsumer(log, handler, taskS)
}

func event(t *testing.T, name, handler string, changeType events.ChangeType, sType service_map.ServiceType) (*events.ServiceChange, *mq.Message) {
	UUID := uuid.New().String()
	var event = &events.ServiceChange{
		UUID:       UUID,
		ChangeType: changeType,
		Handler:    handler,
		New: &service_map.ServiceMap{
			Name: name,
			Type: sType,
			Path: name,
		}}
	b, err := proto.Marshal(event)
	require.NoError(t, err)
	return event, mq.NewMessage(srvName, b, nil)
}

type EmptyHandler struct {
	mock.Mock
	T    chan *task.Task
	name string
}

func NewEmptyHandler() *EmptyHandler {
	return &EmptyHandler{
		T:    make(chan *task.Task, 10),
		name: "EmptyHandler",
	}
}

func (h *EmptyHandler) Name() string {
	return h.name
}

func (h *EmptyHandler) OnUpdate(t *task.Task, new, old *service_map.ServiceMap) error {
	args := h.Mock.Called(t, new, old)
	h.T <- t
	return args.Error(0)
}

func (h *EmptyHandler) OnDelete(t *task.Task, sMap *service_map.ServiceMap) error {
	args := h.Mock.Called(t, sMap)
	h.T <- t
	return args.Error(0)
}

func (h *EmptyHandler) OnNew(t *task.Task, sMap *service_map.ServiceMap) error {
	args := h.Mock.Called(t, sMap)
	h.T <- t
	return args.Error(0)
}

func (h *EmptyHandler) OnForceUpdate(t *task.Task, sMap *service_map.ServiceMap) error {
	args := h.Mock.Called(t, sMap)
	h.T <- t
	return args.Error(0)
}

func (h *EmptyHandler) Updated(t *task.Task, sMap *service_map.ServiceMap) error {
	args := h.Mock.Called(t, sMap)
	h.T <- t
	return args.Error(0)
}

func (h *EmptyHandler) Get() *task.Task {
	select {
	case <-time.NewTimer(100 * time.Millisecond).C:
		return nil
	case result := <-h.T:
		return result
	}
}
