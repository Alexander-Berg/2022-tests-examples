package actions

import (
	"context"
	"fmt"
	"testing"
	"time"

	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"github.com/google/go-cmp/cmp"
	"github.com/stretchr/testify/suite"

	cschemas "a.yandex-team.ru/payplatform/fes/core/schemas"
	"a.yandex-team.ru/payplatform/fes/fes/pkg/core/entities"
	"a.yandex-team.ru/payplatform/fes/fes/pkg/server/schemas"
	"a.yandex-team.ru/payplatform/fes/fes/pkg/storage/db"
)

type EventActionTestSuite struct {
	ActionTestSuite
}

func TestEventActionTestSuite(t *testing.T) {
	t.Parallel()
	suite.Run(t, new(EventActionTestSuite))
}

func createItem() schemas.EventBatchItem {
	return schemas.EventBatchItem{
		PaymentID: btesting.RandS(10),
		EventID:   btesting.RandS(10),
		Event: cschemas.Event{
			Firm: cschemas.Firm{INN: btesting.RandSWithCharset(12, "0123456789")},
			Payment: cschemas.Payment{
				FiscalDocType: cschemas.OrdinaryReceipt,
				Rows: []cschemas.Row{{
					Price:   cschemas.NewMoney(10, 0),
					Qty:     cschemas.NewQuantity(10, 0),
					Title:   "Some text",
					TaxType: cschemas.NDS20,
				}},
				TaxationType:      "OSN",
				PaymentMethodType: cschemas.NonCash,
			},
			ServiceID: 124,
			User: cschemas.User{
				EmailOrPhone: fmt.Sprintf("%s@%s.%s", btesting.RandS(3), btesting.RandS(3), btesting.RandS(3)),
			},
		},
	}
}

func createItemWithStatusNew() schemas.EventBatchItemWithDetailedStatus {
	return schemas.EventBatchItemWithDetailedStatus{
		EventBatchItem: createItem(),
		Status:         entities.NewEventStatus,
	}
}

func createItemWithDetailedStatus() schemas.EventBatchItemWithDetailedStatus {
	d := "skipping reason"
	return schemas.EventBatchItemWithDetailedStatus{
		EventBatchItem: createItem(),
		Status:         entities.NoNeedReceiptEventStatus,
		Details:        &d,
	}
}

func createLoadGroup(s *EventActionTestSuite, ctx context.Context, client cschemas.FESClient, item schemas.EventBatchItem) entities.LoadGroup {
	lg, err := CreateLoadGroupAction(ctx, s.repo,
		btesting.RandS(10),
		item.Event.Firm.INN,
		string(db.GroupByClientItem(client, item)),
	)
	s.Require().NoError(err)
	return *lg
}

func (s *EventActionTestSuite) TestGetEventAction() {
	s.Run("Non-existent event", func() {
		ctx := context.Background()

		client := cschemas.Trust
		item := createItemWithStatusNew()

		_, err := GetEventAction(ctx, s.repo, client, item.PaymentID, item.EventID)
		s.Assert().ErrorIs(err, db.ErrEventNotFound)
	})

	s.Run("Regular", func() {
		ctx := context.Background()

		client := cschemas.Trust
		item := createItemWithStatusNew()

		lg := createLoadGroup(s, ctx, client, item.EventBatchItem)

		expected, err := s.repo.Storage.Event.GetOrCreateBatch(ctx, client, []schemas.EventBatchItemWithDetailedStatus{item})
		s.Require().NoError(err)
		s.Assert().Len(expected, 1)

		paymentEvent, err := GetEventAction(ctx, s.repo, client, item.PaymentID, item.EventID)
		s.Require().NoError(err)

		s.Assert().Equal(expected[0], *paymentEvent)

		s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.QueueURL))
		s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.DeadLetterQueueURL))
	})
}

func (s *EventActionTestSuite) TestCreateEventAction() {
	s.Run("Regular", func() {
		ctx := context.Background()

		items := []schemas.EventBatchItemWithDetailedStatus{
			createItemWithStatusNew(),
			createItemWithDetailedStatus(),
		}
		client := cschemas.Trust

		for _, it := range items {
			lg := createLoadGroup(s, ctx, client, it.EventBatchItem)

			createdEvent, err := CreateEventAction(ctx, s.repo, client, it.PaymentID, it.EventID, it.Event, it.Status, it.Details)
			s.Require().NoError(err)

			readEvent, err := s.repo.Storage.Event.Get(ctx, client, it.PaymentID, it.EventID)
			s.Require().NoError(err)

			s.Assert().Equal(string(it.Status), createdEvent.Status)
			s.Assert().Equal(it.Details, createdEvent.Details)
			s.Assert().Equal(it.EventID, createdEvent.EventID)
			s.Assert().Equal(it.PaymentID, createdEvent.PaymentID)

			s.Assert().Equal(createdEvent, readEvent)

			s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.QueueURL))
			s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.DeadLetterQueueURL))
		}
	})

	s.Run("No load group", func() {
		ctx := context.Background()

		items := []schemas.EventBatchItemWithDetailedStatus{
			createItemWithStatusNew(),
			createItemWithDetailedStatus(),
		}
		client := cschemas.Trust

		for _, it := range items {
			_, err := CreateEventAction(ctx, s.repo, client, it.PaymentID, it.EventID, it.Event, it.Status, it.Details)
			s.Assert().ErrorAs(err, new(db.ErrLoadGroupNotFound))

			_, err = s.repo.Storage.Event.Get(ctx, client, it.PaymentID, it.EventID)
			s.Assert().ErrorIs(err, db.ErrEventNotFound)
		}
	})
}

func (s *EventActionTestSuite) TestDeleteEventAction() {
	s.Run("Deletes existing event", func() {
		ctx := context.Background()

		item := createItemWithStatusNew()
		client := cschemas.Trust

		lg := createLoadGroup(s, ctx, client, item.EventBatchItem)
		_, err := CreateEventAction(ctx, s.repo, client, item.PaymentID, item.EventID, item.Event, item.Status, item.Details)
		s.Require().NoError(err)

		err = DeleteEventAction(ctx, s.repo, client, item.PaymentID, item.EventID)
		s.Require().NoError(err)

		_, err = s.repo.Storage.Event.Get(ctx, client, item.PaymentID, item.EventID)
		s.Assert().ErrorIs(err, db.ErrEventNotFound)

		s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.QueueURL))
		s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.DeadLetterQueueURL))
	})

	s.Run("Does nothing for non-existent event", func() {
		ctx := context.Background()
		client := cschemas.Trust
		item := createItemWithStatusNew()

		err := DeleteEventAction(ctx, s.repo, client, item.PaymentID, item.EventID)
		s.Require().NoError(err)

		_, err = s.repo.Storage.Event.Get(ctx, client, item.PaymentID, item.EventID)
		s.Assert().ErrorIs(err, db.ErrEventNotFound)
	})
}

func (s *EventActionTestSuite) TestDeleteBatchEventAction() {
	s.Run("Deletes existing events", func() {
		ctx := context.Background()

		client := cschemas.Trust

		items := []schemas.EventBatchItemWithDetailedStatus{
			createItemWithStatusNew(),
			createItemWithStatusNew(),
		}

		var eventKeys []schemas.EventKey
		var lgs []entities.LoadGroup
		for _, it := range items {
			eventKeys = append(eventKeys, schemas.EventKey{
				PaymentID: it.PaymentID,
				EventID:   it.EventID,
			})
			lgs = append(lgs, createLoadGroup(s, ctx, client, it.EventBatchItem))
		}

		events, err := s.repo.Storage.Event.GetOrCreateBatch(ctx, client, items)
		s.Require().NoError(err)
		s.Require().Len(events, len(items))

		err = DeleteEventBatchAction(ctx, s.repo, client, eventKeys)
		s.Require().NoError(err)

		_, err = s.repo.Storage.Event.GetEventWithQueueBatch(ctx, client, eventKeys)
		s.Assert().ErrorIs(err, db.ErrEventNotFound)

		for _, lg := range lgs {
			s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.QueueURL))
			s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.DeadLetterQueueURL))
		}
	})

	s.Run("Does nothing for non-existent events", func() {
		ctx := context.Background()
		client := cschemas.Trust
		item := createItemWithStatusNew()

		err := DeleteEventBatchAction(ctx, s.repo, client, []schemas.EventKey{{PaymentID: item.PaymentID, EventID: item.EventID}})
		s.Require().NoError(err)

		_, err = s.repo.Storage.Event.Get(ctx, client, item.PaymentID, item.EventID)
		s.Assert().ErrorIs(err, db.ErrEventNotFound)
	})
}

func (s *EventActionTestSuite) TestCreateEventBatchUpsertAction() {
	ctx := context.Background()

	items := []schemas.EventBatchItemWithDetailedStatus{
		createItemWithStatusNew(),
		createItemWithStatusNew(),
	}
	client := cschemas.Trust

	var lgs []entities.LoadGroup
	for _, it := range items {
		lgs = append(lgs, createLoadGroup(s, ctx, client, it.EventBatchItem))
	}
	createdFirstEvent, err := CreateEventAction(ctx, s.repo, client, items[0].PaymentID, items[0].EventID, items[0].Event, items[0].Status, items[0].Details)
	s.Require().NoError(err)

	createdBothEvents, err := CreateEventBatchAction(ctx, s.repo, client, items)
	s.Require().NoError(err)

	// check that first event was not inserted second time

	// they are equal including both timestamps
	s.Assert().Equal(*createdFirstEvent, createdBothEvents[0])
	// check that first and second events were inserted not simultaneously
	s.Assert().NotEqual(createdBothEvents[0].CreatedAt, createdBothEvents[1].CreatedAt)
	s.Assert().NotEqual(createdBothEvents[0].UpdatedAt, createdBothEvents[1].UpdatedAt)

	for _, lg := range lgs {
		s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.QueueURL))
		s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.DeadLetterQueueURL))
	}
}

func (s *EventActionTestSuite) TestUpdateStatusAction() {
	s.Run("Non-existent event", func() {
		ctx := context.Background()

		client := cschemas.Trust
		item := createItemWithStatusNew()
		status := entities.EnqueuedEventStatus

		_, err := UpdateEventStatusAction(ctx, s.repo, client, item.PaymentID, item.EventID, status)
		s.Assert().ErrorIs(err, db.ErrEventNotFound)
	})

	s.Run("Regular", func() {
		ctx := context.Background()

		client := cschemas.Trust
		item := createItemWithStatusNew()
		status := entities.EnqueuedEventStatus

		lg := createLoadGroup(s, ctx, client, item.EventBatchItem)
		es, err := s.repo.Storage.Event.GetOrCreateBatch(ctx, client, []schemas.EventBatchItemWithDetailedStatus{item})
		s.Require().NoError(err)
		s.Require().Len(es, 1)
		createdEvent := es[0]

		updatedEvent, err := UpdateEventStatusAction(ctx, s.repo, client, item.PaymentID, item.EventID, status)
		s.Require().NoError(err)

		s.Assert().Equal(createdEvent.Client, updatedEvent.Client)
		s.Assert().Equal(createdEvent.PaymentID, updatedEvent.PaymentID)
		s.Assert().Equal(createdEvent.EventID, updatedEvent.EventID)
		s.Assert().Equal(createdEvent.LoadGroupID, updatedEvent.LoadGroupID)
		s.Assert().Equal(createdEvent.Payload, updatedEvent.Payload)
		s.Assert().Equal(createdEvent.CreatedAt, updatedEvent.CreatedAt)
		s.Assert().Equal(createdEvent.Details, updatedEvent.Details)

		s.Assert().True(createdEvent.UpdatedAt.Before(updatedEvent.UpdatedAt),
			"updating time = %v should be later then creating time = %v", updatedEvent.UpdatedAt, createdEvent.UpdatedAt)
		s.Assert().Equal(string(status), updatedEvent.Status)

		s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.QueueURL))
		s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.DeadLetterQueueURL))
	})
}

func (s *EventActionTestSuite) TestUpdateStatusAndDetailsAction() {
	s.Run("Non-existent event", func() {
		ctx := context.Background()

		client := cschemas.Trust
		status := entities.GotReceiptEventStatus
		receiptURL := "http://receipt/url"

		item := createItemWithStatusNew()

		_, err := UpdateEventStatusAndDetailsAction(ctx, s.repo, client, item.PaymentID, item.EventID, status, receiptURL)
		s.Assert().ErrorIs(err, db.ErrEventNotFound)
	})

	s.Run("Regular", func() {
		ctx := context.Background()

		client := cschemas.Trust
		status := entities.GotReceiptEventStatus
		receiptURL := "http://receipt/url"

		item := createItemWithStatusNew()

		lg := createLoadGroup(s, ctx, client, item.EventBatchItem)
		es, err := s.repo.Storage.Event.GetOrCreateBatch(ctx, client, []schemas.EventBatchItemWithDetailedStatus{item})
		s.Require().NoError(err)
		s.Require().Len(es, 1)
		createdEvent := es[0]

		updatedEvent, err := UpdateEventStatusAndDetailsAction(ctx, s.repo, client, item.PaymentID, item.EventID, status, receiptURL)
		s.Require().NoError(err)

		s.Assert().Equal(createdEvent.Client, updatedEvent.Client)
		s.Assert().Equal(createdEvent.PaymentID, updatedEvent.PaymentID)
		s.Assert().Equal(createdEvent.EventID, updatedEvent.EventID)
		s.Assert().Equal(createdEvent.LoadGroupID, updatedEvent.LoadGroupID)
		s.Assert().Equal(createdEvent.Payload, updatedEvent.Payload)
		s.Assert().Equal(createdEvent.CreatedAt, updatedEvent.CreatedAt)

		s.Assert().True(createdEvent.UpdatedAt.Before(updatedEvent.UpdatedAt),
			"updating time = %v should be later then creating time = %v", updatedEvent.UpdatedAt, createdEvent.UpdatedAt)
		s.Assert().Equal(string(status), updatedEvent.Status)
		s.Require().NotNil(updatedEvent.Details)
		s.Assert().Equal(receiptURL, *updatedEvent.Details)

		s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.QueueURL))
		s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.DeadLetterQueueURL))
	})
}

func (s *EventActionTestSuite) TestGetEventQueueBatchAction() {
	ctx := context.Background()

	client := cschemas.Trust

	items := []schemas.EventBatchItemWithDetailedStatus{
		createItemWithStatusNew(),
		createItemWithStatusNew(),
	}

	lgs := []entities.LoadGroup{
		createLoadGroup(s, ctx, client, items[0].EventBatchItem),
		createLoadGroup(s, ctx, client, items[1].EventBatchItem),
	}

	events, err := s.repo.Storage.Event.GetOrCreateBatch(ctx, client, items)
	s.Require().NoError(err)
	s.Require().Len(events, len(items))

	expected := []entities.EventWithQueue{
		{
			Client:    client,
			PaymentID: items[0].PaymentID,
			EventID:   items[0].EventID,
			QueueURL:  lgs[0].QueueURL,
			Event:     items[0].Event,
		},
		{
			Client:    client,
			PaymentID: items[1].PaymentID,
			EventID:   items[1].EventID,
			QueueURL:  lgs[1].QueueURL,
			Event:     items[1].Event,
		},
	}
	queues, err := GetEventWithQueueBatchAction(ctx, s.repo, client, []schemas.EventKey{
		{PaymentID: items[0].PaymentID, EventID: items[0].EventID},
		{PaymentID: items[1].PaymentID, EventID: items[1].EventID},
	})
	s.Require().NoError(err)
	s.Require().Len(queues, len(items))

	for i := range items {
		s.Assert().Empty(cmp.Diff(expected[i], queues[i]))

		s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lgs[i].QueueURL))
		s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lgs[i].DeadLetterQueueURL))
	}
}

func (s *EventActionTestSuite) TestEnqueueEventBatchAction() {
	ctx := context.Background()

	client := cschemas.Trust

	var items []schemas.EventBatchItemWithDetailedStatus
	var lgs []entities.LoadGroup
	var eventKeys []schemas.EventKey

	for i := 0; i < 15; i++ {
		item := createItemWithStatusNew()
		items = append(items, item)

		lg := createLoadGroup(s, ctx, client, item.EventBatchItem)
		lgs = append(lgs, lg)

		eventKeys = append(eventKeys, schemas.EventKey{
			PaymentID: item.PaymentID,
			EventID:   item.EventID,
		})
	}
	// https://pkg.go.dev/github.com/aws/aws-sdk-go/service/sqs#SQS.CreateQueue
	// After you create a queue, you must wait at least one second after the queue is created to be able to use the queue.
	time.Sleep(1 * time.Second)

	events, err := s.repo.Storage.Event.GetOrCreateBatch(ctx, client, items)
	s.Require().NoError(err)
	s.Require().Len(events, len(items))

	err = EnqueueEventBatchAction(ctx, s.repo, client, eventKeys)
	s.Assert().NoError(err)

	for _, lg := range lgs {
		s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.QueueURL))
		s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.DeadLetterQueueURL))
	}
}
