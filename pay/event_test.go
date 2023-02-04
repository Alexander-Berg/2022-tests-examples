package actions

import (
	"context"
	"encoding/json"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/payplatform/fes/collector/pkg/interactions/fes"
	"a.yandex-team.ru/payplatform/fes/collector/pkg/interactions/fes/mock"
)

type EventActionTestSuite struct {
	ActionTestSuite
}

func (s *EventActionTestSuite) TestSaveEventsAction() {
	ctx := context.Background()

	ctrl := gomock.NewController(s.T())
	defer ctrl.Finish()

	mockClient := mock.NewMockFiscalEventServiceClient(ctrl)

	item := fes.FiscalEventBatchItem{EventID: "12345", PaymentID: "23456", Event: make(map[string]interface{})}
	itemString, err := json.Marshal(item)
	s.Require().NoError(err)

	expected := []fes.FiscalEventBatchItem{item}
	sent := [][]byte{[]byte("Bad event to skip"), itemString}

	mockClient.EXPECT().SaveEvents(ctx, expected)

	s.Require().NoError(SaveMessages(ctx, sent, mockClient))
}

func (s *EventActionTestSuite) TestProceedWithEventsAction() {
	ctx := context.Background()

	ctrl := gomock.NewController(s.T())
	defer ctrl.Finish()

	mockClient := mock.NewMockFiscalEventServiceClient(ctrl)

	items := []fes.FiscalEventProceedBatchItem{
		{EventID: "12345", PaymentID: "23456", Action: "approve"},
		{EventID: "23451", PaymentID: "34562", Action: "discard"},
		{EventID: "34512", PaymentID: "45623", Action: "no idea"},
	}
	var itemStrings [][]byte
	for _, item := range items {
		itemString, err := json.Marshal(item)
		s.Require().NoError(err)
		itemStrings = append(itemStrings, itemString)
	}

	sent := [][]byte{[]byte("Bad event to skip")}
	sent = append(sent, itemStrings...)

	mockClient.EXPECT().ProceedWithEvents(ctx, items).Times(1).Return(nil)

	s.Require().NoError(ProceedWithEvents(ctx, sent, mockClient))
}

func TestEventActionTestSuite(t *testing.T) {
	suite.Run(t, new(EventActionTestSuite))
}
