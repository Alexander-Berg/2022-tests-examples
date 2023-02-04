package events

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/github-app/pull_request"
	"github.com/YandexClassifieds/shiva/pb/validator/events"
	validation "github.com/YandexClassifieds/shiva/pb/validator/status"
	"github.com/YandexClassifieds/shiva/pkg/arcanum"
	"github.com/YandexClassifieds/shiva/pkg/mq"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/require"
)

func TestHandlePr(t *testing.T) {
	test.InitTestEnv()
	testCases := []struct {
		name       string
		title      string
		skipPr     []int
		layer      string
		shouldSkip bool
	}{
		{
			name:       "skip by test prefix",
			title:      "test_shiva-gh-app_title",
			layer:      "prod",
			shouldSkip: true,
		},
		{
			name:       "handle with test prefix",
			title:      "test_shiva-gh-app_title",
			layer:      "test",
			shouldSkip: false,
		},
		{
			name:       "skip without test prefix",
			title:      "some-title",
			layer:      "test",
			shouldSkip: true,
		},
		{
			name:       "skip prod by prefix",
			title:      "skip_shiva-gh-app_title",
			layer:      "prod",
			shouldSkip: true,
		},
		{
			name:       "skip by number",
			title:      "some_title",
			skipPr:     []int{100, 101},
			layer:      "prod",
			shouldSkip: true,
		},
		{
			name:       "handle prod pr",
			title:      "some-title",
			layer:      "prod",
			shouldSkip: false,
		},
	}
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			db := test_db.NewSeparatedDb(t)
			log := test.NewLogger(t)
			conf := NewConf()
			conf.Layer = tc.layer
			conf.Skip = tc.skipPr

			arcanumMock := &mock.ArcanumService{}
			prInfo := &arcanum.PrInfo{
				Id:     100,
				Title:  tc.title,
				Author: "user",
			}
			arcanumMock.On("GetPrInfo", uint32(100)).Return(prInfo, nil)
			h := newHandlerMock()
			consumer := NewConsumer(db, log, conf, arcanumMock, h.handle)

			e := &events.ArcadiaPrEvent{Id: 100}
			msg, err := mq.NewProtoMessage("", e, nil)
			require.NoError(t, err)
			require.NoError(t, consumer.handleMessage(msg))

			if tc.shouldSkip {
				require.Len(t, h.events, 0)
			} else {
				events := h.Get(t, 1)
				require.Equal(t, &PrCtx{
					Id:     prInfo.Id,
					Title:  prInfo.Title,
					Author: prInfo.Author,
				}, events[0])
			}
		})
	}
}

func TestSkipNotActualEvents(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)
	conf := NewConf()
	conf.Layer = "prod"

	h := newHandlerMock()
	consumer := NewConsumer(db, log, conf, nil, h.handle)

	prUpdatedAt := time.Now()
	require.NoError(t, db.GormDb.Create(&pull_request.ArcadiaPr{
		PrId:   100,
		Time:   prUpdatedAt,
		Status: validation.Status_SUCCESS,
	}).Error)

	for i := 0; i < 3; i++ {
		prInfo := &arcanum.PrInfo{
			UpdatedAt: prUpdatedAt.Add(-time.Second * time.Duration(i)),
			Id:        100,
			Title:     "some-title",
			Author:    "user",
		}
		arcanumMock := &mock.ArcanumService{}
		arcanumMock.On("GetPrInfo", uint32(100)).Return(prInfo, nil)
		consumer.arcanumService = arcanumMock

		e := &events.ArcadiaPrEvent{Id: 100}
		msg, err := mq.NewProtoMessage("", e, nil)
		require.NoError(t, err)
		require.NoError(t, consumer.handleMessage(msg))
	}

	require.Len(t, h.events, 1)
	require.Equal(t, &PrCtx{
		UpdatedAt: prUpdatedAt,
		Id:        100,
		Title:     "some-title",
		Author:    "user",
	}, h.Get(t, 1)[0])
}

type handlerMock struct {
	events chan *PrCtx
}

func newHandlerMock() *handlerMock {
	return &handlerMock{events: make(chan *PrCtx, 10)}
}

func (m *handlerMock) Get(t *testing.T, count int) []*PrCtx {
	var res []*PrCtx
	for i := 0; i < count; i++ {
		select {
		case e := <-m.events:
			res = append(res, e)
		case <-time.NewTimer(100 * time.Millisecond).C:
			t.Fatalf("fail to get %d events", count)
		}
	}
	return res
}
func (m *handlerMock) handle(prCtx *PrCtx) error {
	m.events <- prCtx
	return nil
}
