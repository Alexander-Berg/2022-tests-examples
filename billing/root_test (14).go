package sandbox

import (
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions/sandbox/entities"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strconv"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type ClientSuite struct {
	btesting.BaseSuite

	client Client

	interactionsMock *mock.MockClientProtocol

	ctx context.Context
}

var (
	resourceRequestTemplate = interactions.Request{
		APIMethod: "/resource",
		Method:    http.MethodGet,
		Name:      "get_resource",
		Params: url.Values{
			"limit": []string{"1"},
		},
	}
	taskRequestTemplate = interactions.Request{
		APIMethod: "/task",
		Method:    http.MethodGet,
		Name:      "get_task",
		Params: url.Values{
			"limit": []string{"1"},
		},
	}
)

func generateResource() *entities.Resource {
	return &entities.Resource{
		ID:    btesting.RandN64(),
		Type:  btesting.RandS(8),
		Md5:   btesting.RandS(16),
		Owner: "BILLING-TESTING-INC-AND-Co.",
	}
}

func generateTask() *entities.Task {
	return &entities.Task{
		ID:     btesting.RandN64(),
		Type:   btesting.RandS(8),
		Status: entities.GroupQueue[btesting.RandN64n(0, int64(len(entities.GroupQueue)-1))],
		Owner:  "BILLING-TESTING-INC-AND-Co.",
	}
}

func generateGetResourceResponse(numberOfItems int) entities.GetResponse[entities.Resource] {
	items := make([]*entities.Resource, 0)

	for i := 0; i < numberOfItems; i++ {
		items = append(items, generateResource())
	}

	return entities.GetResponse[entities.Resource]{
		Items: items,
		Total: numberOfItems,
	}
}

func generateGetTaskResponse(numberOfItems int) entities.GetResponse[entities.Task] {
	items := make([]*entities.Task, 0)

	for i := 0; i < numberOfItems; i++ {
		items = append(items, generateTask())
	}

	return entities.GetResponse[entities.Task]{
		Items: items,
		Total: numberOfItems,
	}
}

func marshal(obj any) []byte {
	marshaled, _ := json.Marshal(obj)
	return marshaled
}

func (s *ClientSuite) SetupTest() {

	s.interactionsMock = mock.NewMockClientProtocol(s.Ctrl())

	s.client = Client{c: s.interactionsMock}
	s.ctx = context.Background()
}

func (s *ClientSuite) TestGetResource() {
	request := resourceRequestTemplate

	getResponse := generateGetResourceResponse(1)

	response := interactions.NewRawResponse(marshal(getResponse), http.StatusOK, nil)

	s.interactionsMock.EXPECT().MakeRequestRaw(s.ctx, request).Return(response)

	resources, err := s.client.Resources(s.ctx)

	s.Require().NoError(err)

	s.Require().Equal(1, len(resources))

	s.Assert().Equal(getResponse.Items[0], resources[0])
}

func (s *ClientSuite) TestGetTask() {
	request := taskRequestTemplate

	getResponse := generateGetTaskResponse(1)

	response := interactions.NewRawResponse(marshal(getResponse), http.StatusOK, nil)

	s.interactionsMock.EXPECT().MakeRequestRaw(s.ctx, request).Return(response)

	resources, err := s.client.Tasks(s.ctx)

	s.Require().NoError(err)

	s.Require().Equal(1, len(resources))

	s.Assert().Equal(getResponse.Items[0], resources[0])
}

func (s *ClientSuite) TestFilters() {
	tests := []struct {
		testname     string
		inputFilters []entities.Filter
		resultParams url.Values
	}{
		{
			"without_filters",
			[]entities.Filter{},
			url.Values{"limit": {"1"}},
		},
		{
			"with_id",
			[]entities.Filter{WithID(123)},
			url.Values{"limit": {"1"}, "id": {"123"}},
		},
		{
			"with_ids",
			[]entities.Filter{WithIDs([]int{123, 321})},
			url.Values{"limit": {"1"}, "id": {"123", "321"}},
		},
		{
			"with_limit",
			[]entities.Filter{WithLimit(11)},
			url.Values{"limit": {"11"}},
		},
		{
			"with_type",
			[]entities.Filter{WithType("BILLING_TEST")},
			url.Values{"limit": {"1"}, "type": {"BILLING_TEST"}},
		},
		{
			"with_types",
			[]entities.Filter{WithTypes([]string{"BILLING_TEST", "BILLING_TEST2"})},
			url.Values{"limit": {"1"}, "type": {"BILLING_TEST", "BILLING_TEST2"}},
		},
		{
			"with_offset",
			[]entities.Filter{WithOffset(100)},
			url.Values{"limit": {"1"}, "offset": {"100"}},
		},
		{
			"with_order",
			[]entities.Filter{WithOrder("+id")},
			url.Values{"limit": {"1"}, "order": {"+id"}},
		},
		{
			"with_attributes",
			[]entities.Filter{WithAttributes(map[string]string{"foo": "bar", "fork": "pork"})},
			url.Values{"limit": {"1"}, "attrs": {string(marshal(map[string]string{"foo": "bar", "fork": "pork"}))}},
		},
		{
			"with_multiple_filters",
			[]entities.Filter{WithID(123), WithType("BILLING_TEST"), WithLimit(22)},
			url.Values{"limit": {"22"}, "id": {"123"}, "type": {"BILLING_TEST"}},
		},
	}

	rawResponse := interactions.NewRawResponse(nil, http.StatusOK, nil)

	for _, test := range tests {
		s.T().Run(fmt.Sprintf("%s_resource", test.testname), func(t *testing.T) {
			request := resourceRequestTemplate
			request.Params = test.resultParams

			s.interactionsMock.EXPECT().MakeRequestRaw(gomock.Any(), request).Return(rawResponse)
			_, err := s.client.Resources(s.ctx, test.inputFilters...)
			s.Require().NoError(err)
		})

		s.T().Run(fmt.Sprintf("%s_task", test.testname), func(t *testing.T) {
			request := taskRequestTemplate
			request.Params = test.resultParams

			s.interactionsMock.EXPECT().MakeRequestRaw(gomock.Any(), request).Return(rawResponse)
			_, err := s.client.Tasks(s.ctx, test.inputFilters...)
			s.Require().NoError(err)
		})
	}
}

func (s *ClientSuite) TestBatch() {
	req := interactions.Request{
		Method:    http.MethodPut,
		APIMethod: "/batch/some_method",
		Name:      "put_batch_some_method",
		Body:      []byte("test"),
	}

	targetResp := []entities.BatchResponse{
		{
			ID:      int(btesting.RandN64()),
			Status:  entities.BatchSuccess,
			Message: btesting.RandS(16),
		},
	}

	rawResponse := interactions.NewRawResponse(marshal(targetResp), http.StatusOK, nil)

	s.interactionsMock.EXPECT().MakeRequestRaw(gomock.Any(), req).Return(rawResponse)

	resp, err := s.client.Batch(s.ctx, http.MethodPut, "some_method", []byte("test"))

	s.Require().NoError(err)

	s.Assert().Equal(targetResp, resp)
}

func (s *ClientSuite) TestTaskManagement() {
	tests := []struct {
		testName     string
		method       func(context.Context, *entities.Task) error
		apiMethod    string
		requestName  string
		returnStatus entities.BatchResponseStatus
	}{
		{
			"enqueue_task",
			s.client.EnqueueTask,
			"/batch/tasks/start",
			"put_batch_tasks_start",
			entities.BatchSuccess,
		},
		{
			"enqueue_task_with_error",
			s.client.EnqueueTask,
			"/batch/tasks/start",
			"put_batch_tasks_start",
			entities.BatchError,
		},
		{
			"stop_task",
			s.client.StopTask,
			"/batch/tasks/stop",
			"put_batch_tasks_stop",
			entities.BatchSuccess,
		},
		{
			"delete_task",
			s.client.DeleteTask,
			"/batch/tasks/delete",
			"put_batch_tasks_delete",
			entities.BatchSuccess,
		},
		{
			"suspend_task",
			s.client.SuspendTask,
			"/batch/tasks/suspend",
			"put_batch_tasks_suspend",
			entities.BatchSuccess,
		},
		{
			"resume_task",
			s.client.ResumeTask,
			"/batch/tasks/resume",
			"put_batch_tasks_resume",
			entities.BatchSuccess,
		},
	}

	for _, test := range tests {
		s.T().Run(test.testName, func(t *testing.T) {
			dummyTask := &entities.Task{
				ID: btesting.RandN64(),
			}

			req := interactions.Request{
				Method:    http.MethodPut,
				APIMethod: test.apiMethod,
				Name:      test.requestName,
				Body:      []byte(strconv.Itoa(int(dummyTask.ID))),
			}

			targerResp := []entities.BatchResponse{
				{
					ID:      int(dummyTask.ID),
					Status:  test.returnStatus,
					Message: btesting.RandS(16),
				},
			}

			rawResponse := interactions.NewRawResponse(marshal(targerResp), http.StatusOK, nil)

			s.interactionsMock.EXPECT().MakeRequestRaw(gomock.Any(), req).Return(rawResponse)

			err := test.method(s.ctx, dummyTask)

			if test.returnStatus != entities.BatchSuccess {
				s.Require().Error(err)
			} else {
				s.Require().NoError(err)
			}
		})
	}
}

func TestClient(t *testing.T) {
	suite.Run(t, new(ClientSuite))
}
