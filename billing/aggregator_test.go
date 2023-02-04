package eventaggregator

import (
	"context"
	"embed"
	"encoding/json"
	"fmt"
	"io/fs"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"
	"golang.org/x/exp/slices"
	"golang.org/x/sync/errgroup"

	mock2 "a.yandex-team.ru/billing/hot/scheduler/mock"
	"a.yandex-team.ru/billing/hot/scheduler/pkg/core"
	"a.yandex-team.ru/billing/hot/scheduler/pkg/core/actions/impl"
	"a.yandex-team.ru/billing/hot/scheduler/pkg/core/entities"
	"a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/logbroker"
	"a.yandex-team.ru/kikimr/public/sdk/go/persqueue"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
	"a.yandex-team.ru/library/go/core/xerrors"
)

//go:embed testdata/*
var testFS embed.FS

//go:embed configs/*
var configFS embed.FS

type objectSliceMatcher struct {
	objects []entities.Object
}

func equalToObjects(objects []entities.Object) gomock.Matcher {
	sortObjects(objects)
	return &objectSliceMatcher{objects}
}

func sortObjects(objects []entities.Object) {
	slices.SortFunc(objects, func(l, r entities.Object) bool {
		if l.ID != r.ID {
			return l.ID < r.ID
		}
		return l.DataNamespace < r.DataNamespace
	})
}

func (o *objectSliceMatcher) Matches(x any) bool {
	gotObjects, ok := x.([]entities.Object)
	if !ok {
		return false
	}
	if len(gotObjects) != len(o.objects) {
		return false
	}
	sortObjects(gotObjects)

	for i, expectedObject := range o.objects {
		if gotObjects[i].ID != expectedObject.ID || gotObjects[i].DataNamespace != expectedObject.DataNamespace {
			return false
		}
	}
	return true
}

func (o *objectSliceMatcher) String() string {
	return fmt.Sprintf("not equal to %v", o.objects)
}

func (s *AggregatorTestSuite) TestRun() {
	cfg, err := core.ParseAggregatorConfig(configFS, ".")
	s.Require().NoError(err)

	s.Require().NoError(fs.WalkDir(testFS, ".", func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if d.IsDir() || strings.HasSuffix(d.Name(), "expected.json") {
			return nil
		}

		testContent, err := fs.ReadFile(testFS, path)
		if err != nil {
			return err
		}

		var testPayloadsIface [][]any
		if err := json.Unmarshal(testContent, &testPayloadsIface); err != nil {
			return xerrors.Errorf("cannot parse file %s: %w", path, err)
		}
		testPayloads := make([][]string, 0, len(testPayloadsIface))
		for i, arr := range testPayloadsIface {
			testPayloads = append(testPayloads, make([]string, 0, len(arr)))
			for _, iface := range arr {
				bytesIface, err := json.Marshal(iface)
				if err != nil {
					return err
				}
				testPayloads[i] = append(testPayloads[i], string(bytesIface))
			}
		}

		testFileName := filepath.Base(path)
		testExpectedFileName := strings.Replace(testFileName, ".json", "-expected.json", 1)
		expectedFilePath := filepath.Join(filepath.Dir(path), testExpectedFileName)
		testExpectedContent, err := fs.ReadFile(testFS, expectedFilePath)
		if err != nil {
			return xerrors.Errorf("cannot read file %s: %w", testExpectedFileName, err)
		}

		var testExpected [][]entities.Object
		if err := json.Unmarshal(testExpectedContent, &testExpected); err != nil {
			return xerrors.Errorf("cannot parse file %s: %w", testExpectedFileName, err)
		}

		s.Run(testFileName, func() {
			s.internalTestRun(cfg, testPayloads, testExpected)
		})
		return nil
	}))
}

func (s *AggregatorTestSuite) internalTestRun(
	cfg *core.AggregatorConfig, payloads [][]string, expectedBatch [][]entities.Object,
) {
	ctrl := gomock.NewController(s.T())

	consumerMock := mock.NewMockConsumerProtocol(ctrl)
	handlePayload(consumerMock, payloads...)

	mockStorage := mock2.NewMockStorage(ctrl)
	for _, expected := range expectedBatch {
		if len(expected) != 0 {
			mockStorage.EXPECT().AddObjects(gomock.Any(), equalToObjects(expected)).Return(nil)
		}
	}

	actions := impl.NewActions(mockStorage)

	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
	defer cancel()

	registry := solomon.NewRegistry(nil)
	aggregator := NewEventAggregator(consumerMock, registry, actions, cfg, time.Second)

	s.Require().NoError(aggregator.Run(ctx))
}

func handlePayload(consumerMock *mock.MockConsumerProtocol, payloadsGroups ...[]string) {
	consumerMock.EXPECT().Read(gomock.Any(), gomock.Any()).DoAndReturn(
		func(ctx context.Context, handler logbroker.ConsumerMsgHandler) error {
			g, gCtx := errgroup.WithContext(ctx)
			for _, group := range payloadsGroups {
				group := group
				g.Go(func() error {
					msgs := make([]persqueue.ReadMessage, 0, len(group))
					for _, payload := range group {
						msgs = append(msgs, persqueue.ReadMessage{
							Data: []byte(payload),
						})
					}
					return handler(gCtx, persqueue.ReadMessage{}, persqueue.MessageBatch{
						Messages: msgs,
					})
				})
			}
			return g.Wait()
		},
	)
}

type AggregatorTestSuite struct {
	suite.Suite
}

func TestObjectActionTestSuite(t *testing.T) {
	suite.Run(t, new(AggregatorTestSuite))
}
