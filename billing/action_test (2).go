package manifest

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/processor/pkg/interactions/common/mocks"
	bmocks "a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/common"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/extracontext"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

func TestActions_StageGroups(t *testing.T) {
	tests := []struct {
		testname  string
		actions   Actions
		want      map[string][]Action
		withError bool
	}{
		{
			testname: "Stage",
			actions: Actions{
				{
					Reference: "1",
					Stage:     "before_lock",
					Method:    "BeforeLock_1",
				},
				{
					Reference: "2",
					Stage:     "before_lock",
					Method:    "BeforeLock_2",
				},
				{
					Reference: "3",
					Stage:     "lock",
					Method:    "Lock",
				},
				{
					Reference: "4",
					Stage:     "after_calc",
					Method:    "AfterCalc",
				},
			},
			want: map[string][]Action{
				"before_lock": {
					{
						Reference: "1",
						Stage:     "before_lock",
						Method:    "BeforeLock_1",
					},
					{
						Reference: "2",
						Stage:     "before_lock",
						Method:    "BeforeLock_2",
					},
				},
				"lock": {
					{
						Reference: "3",
						Stage:     "lock",
						Method:    "Lock",
					},
				},
				"after_calc": {
					{
						Reference: "4",
						Stage:     "after_calc",
						Method:    "AfterCalc",
					},
				},
			},
			withError: false,
		},
		{
			testname: "Error if no stage",
			actions: Actions{
				{
					Reference: "1",
					Method:    "BeforeLock_1",
				},
			},
			withError: true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			actual, err := test.actions.StageGroups()
			if test.withError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, actual)
			}
		})
	}
}

func TestAction_prepareArgs(t *testing.T) {
	data := common.NewSyncStringMapWithData(map[string]any{
		"a": common.NewSyncStringMapWithData(map[string]any{
			"b": 100,
			"c": "test_c",
			"d": []int{0, 100, 200},
		}),
	})

	tests := []struct {
		testname  string
		data      *common.SyncStringMap
		action    Action
		want      []any
		withError bool
	}{
		{
			testname: "get args",
			data:     data,
			action: Action{
				Params: []Param{
					{
						Action: "get",
						Key:    "a.b",
					},
					{
						Action: "const",
						Value:  "test",
					},
				},
			},
			want:      []any{100, "test"},
			withError: false,
		},
		{
			testname: "get args from array",
			data:     data,
			action: Action{
				Params: []Param{
					{
						Action: "get",
						Key:    "a.d.1",
					},
					{
						Action: "const",
						Value:  "test",
					},
				},
			},
			want:      []any{100, "test"},
			withError: false,
		},
		{
			testname: "get error if wrong param",
			data:     data,
			action: Action{
				Params: []Param{
					{
						Action: "get",
						Key:    "z",
					},
				},
			},
			withError: true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			actual, err := test.action.prepareArgs(test.data)
			if test.withError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, actual)
			}
		})
	}
}

type testActionCallSuite struct {
	btesting.BaseSuite
}

func (s *testActionCallSuite) TestActionSuccessCall() {
	ctx := extracontext.NewWithParent(context.Background())
	ctrl := gomock.NewController(s.T())

	// тут лучше подменить составляющие адаптера на моки и трекать истрорию вызовов
	// но сейчас не хватило времени, оставляю комметарий что бы не забыть
	accounter := bmocks.NewMockAccounter(ctrl)
	clientGetter := mocks.NewMockClientGetter(ctrl)
	adaptor := Create(nil, accounter, nil, nil, clientGetter)

	action := Action{
		Method: "Accounter.NilLock",
	}

	actual, err := action.Call(ctx, adaptor, nil)
	require.NoError(s.T(), err)
	assert.ElementsMatch(s.T(), &NilLock{}, actual)
}

func (s *testActionCallSuite) TestActionSuccessCallWithForeach() {
	ctx := extracontext.NewWithParent(context.Background())
	ctrl := gomock.NewController(s.T())

	// тут лучше подменить составляющие адаптера на моки и трекать истрорию вызовов
	// но сейчас не хватило времени, оставляю комметарий что бы не забыть
	accounter := bmocks.NewMockAccounter(ctrl)
	clientGetter := mocks.NewMockClientGetter(ctrl)
	adaptor := Create(nil, accounter, nil, nil, clientGetter)

	action := Action{
		Method: "Accounter.NilLock",
		Foreach: &Param{
			Action: "get",
			Key:    "a.d",
		},
	}

	actual, err := action.Call(ctx, adaptor, common.NewSyncStringMapWithData(map[string]any{
		"a": map[string]any{
			"d": []int{1, 2, 3},
		},
	}))
	require.NoError(s.T(), err)
	assert.Len(s.T(), actual, 3)
	assert.ElementsMatch(s.T(), []any{&NilLock{}, &NilLock{}, &NilLock{}}, actual)
}

func (s *testActionCallSuite) TestActionWrongMethodErrorCall() {
	ctx := extracontext.NewWithParent(context.Background())
	ctrl := gomock.NewController(s.T())

	// тут лучше подменить составляющие адаптера на моки и трекать истрорию вызовов
	// но сейчас не хватило времени, оставляю комметарий что бы не забыть
	accounter := bmocks.NewMockAccounter(ctrl)
	clientGetter := mocks.NewMockClientGetter(ctrl)
	adaptor := Create(nil, accounter, nil, nil, clientGetter)

	tests := []struct {
		testname string
		method   string
	}{
		{
			"unknown first part of method path",
			"Unknown.NilLock",
		},
		{
			"unknown last part of method path",
			"Accounter.Unknown",
		},
	}

	for _, test := range tests {
		s.T().Run(test.testname, func(t *testing.T) {
			action := Action{
				Method: test.method,
			}
			_, err := action.Call(ctx, adaptor, nil)
			require.Error(s.T(), err)
			assert.ErrorIs(s.T(), err, ErrorUnknownMethod)
		})
	}
}

func (s *testActionCallSuite) TestWrongParamCall() {
	ctx := extracontext.NewWithParent(context.Background())
	ctrl := gomock.NewController(s.T())

	// тут лучше подменить составляющие адаптера на моки и трекать истрорию вызовов
	// но сейчас не хватило времени, оставляю комметарий что бы не забыть
	accounter := bmocks.NewMockAccounter(ctrl)
	clientGetter := mocks.NewMockClientGetter(ctrl)
	adaptor := Create(nil, accounter, nil, nil, clientGetter)

	action := Action{
		// NilLock - требует 0 параметров - передаем 1
		Method: "Accounter.NilLock",
		Params: []Param{
			{
				Action: "wrong",
			},
		},
	}

	_, err := action.Call(ctx, adaptor, nil)
	require.Error(s.T(), err)
	assert.ErrorIs(s.T(), err, ErrorUnknownParamAction)
}

func (s *testActionCallSuite) TestWrongArgumentCountErrorCall() {
	ctx := extracontext.NewWithParent(context.Background())
	ctrl := gomock.NewController(s.T())

	// тут лучше подменить составляющие адаптера на моки и трекать истрорию вызовов
	// но сейчас не хватило времени, оставляю комметарий что бы не забыть
	accounter := bmocks.NewMockAccounter(ctrl)
	clientGetter := mocks.NewMockClientGetter(ctrl)
	adaptor := Create(nil, accounter, nil, nil, clientGetter)

	action := Action{
		// NilLock - требует 0 параметров - передаем 1
		Method: "Accounter.NilLock",
		Params: []Param{
			{
				Action: "const",
				Value:  "1",
			},
		},
	}

	_, err := action.Call(ctx, adaptor, nil)
	require.Error(s.T(), err)
	assert.Contains(s.T(), err.Error(), "must have 1 params. Have 2")
}

func TestActionCall(t *testing.T) {
	suite.Run(t, &testActionCallSuite{})
}
