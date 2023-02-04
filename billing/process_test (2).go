package actions

import (
	"context"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"
	"gopkg.in/yaml.v2"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	diodEntities "a.yandex-team.ru/billing/hot/diod/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/processor/pkg/core"
	procContext "a.yandex-team.ru/billing/hot/processor/pkg/core/context"
	"a.yandex-team.ru/billing/hot/processor/pkg/core/manifest"
	manifestMock "a.yandex-team.ru/billing/hot/processor/pkg/core/manifest/mocks"
	"a.yandex-team.ru/billing/hot/processor/pkg/interactions"
	"a.yandex-team.ru/billing/hot/processor/pkg/interactions/calculator"
	calculatorMock "a.yandex-team.ru/billing/hot/processor/pkg/interactions/calculator/mocks"
	"a.yandex-team.ru/billing/hot/processor/pkg/interactions/diod"
	diodMock "a.yandex-team.ru/billing/hot/processor/pkg/interactions/diod/mocks"
	bmocks "a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/common"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/extracontext"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/library/go/core/xerrors"
)

type testProcessorSuite struct {
	btesting.BaseSuite
	accountsMock         *bmocks.MockAccounter
	calculatorClientMock *calculatorMock.MockClient
	diodClientMock       *diodMock.MockClient
	ctx                  procContext.ProcessorContext
}

const manifestYml = `
- calculator:
    transport:
      baseUrl: http://127.0.0.1:9000/v1/calculator-mock
      debug: true
      tvmDst: ""
  endpoints:
    revenue:
      actions:
        - reference: account_location
          stage: before_lock
          # MakeMap(k string, v any, ...)
          method: Accounter.MakeLocation
          params:
          - action: const
            value: accounts
          - action: get
            key: input.Namespace
          - action: const
            value: taxi_cutoff_dt_lock
          - action: const
            value: client_id
          - action: get
            key: input.Event.client_id
          - action: const
            value: contract_id
          - action: get
            key: input.Event.contract_id
          # CreateExclusiveLock(accountLocation map[string]any, timeout int, tickTime int)
        - method: Accounter.CreateExclusiveLock
          reference: account_lock
          stage: lock
          params:
          - action: get
            key: references.account_location
          - action: const
            value: 6
          - action: const
            value: 1
        - method: Lib.JoinToString
          name: event_type
          reference: event_type
          stage: after_calc
          params:
          - action: get
            keys:
            - input.Namespace
            - input.Endpoint
          - action: const
            value: ':'
        - method: Accounter.WriteCalculatorResponse
          reference: write_batch
          stage: after_calc
          deps:
          - event_type
          params:
            - action: get
              key: references.event_type
            - action: get
              key: calc_result.Data.Event.id
            - action: const
              value: 1617000000
            - action: get
              key: calc_result.Data.Event
            - action: get
              key: calc_result.Data.ClientTransactions.0.transactions
              nullify_missing: true
            - action: get
              key: references.account_location
            - action: get
              key: references.account_lock.object
    error-empty-stage:
      actions:
        - reference: account_location
          stage: ""
          # MakeMap(k string, v any, ...)
          method: Lib.MakeMap
    error-before-lock:
      actions:
        - reference: account_location
          stage: "before_lock"
          method: Lib.MakeMap
          params:
          - action: wrong
            value: type
    error-after-lock:
      actions:
        - reference: account_location
          stage: "after_lock"
          method: Lib.MakeMap
          params:
          - action: wrong
            value: type
    error-before-calc:
      actions:
        - reference: account_location
          stage: "before_calc"
          method: Lib.MakeMap
          params:
          - action: wrong
            value: type
    error-after-calc:
      actions:
        - reference: account_location
          stage: "after_calc"
          method: Lib.MakeMap
          params:
          - action: wrong
            value: type
    error-wrong-calc-reference:
      calc_references:
        - wrong
  namespace: taxi
- calculator:
    transport:
      baseUrl: http://127.0.0.1:9000/v1/calculator-mock
      debug: true
      tvmDst: ""
  endpoints:
    no-diod:
      actions:
        - stage: before_lock
          method: Lib.MakeMap
          params:
          - action: const
            value: key
          - action: const
            value: value
    diod-test:
      actions:
        - reference: diod_test
          stage: before_lock
          method: Diod.GetKeys
          params:
          - action: const
            value: abc
          - action: get
            key: input.Event.keys
            nullify_missing: true
    diod-test-update-keys:
      actions:
        - reference: diod_test
          stage: before_lock
          method: Diod.UpdateKeys
          params:
          - action: get
            key: input.Event.keys
    diod-test-references-get-keys:
      actions:
        - reference: diod_get_keys
          stage: before_lock
          method: Diod.GetKeys
          params:
          - action: const
            value: abc
          - action: get
            key: input.Event.keys
        - reference: make_map
          stage: after_lock
          method: Lib.MakeMap
          params:
          - action: const
            value: data_from_diod
          - action: get
            key: references.diod_get_keys
    diod-test-references-update-keys:
      actions:
        - reference: diod_update_keys
          stage: before_lock
          method: Diod.UpdateKeys
          params:
          - action: get
            key: input.Event.keys
        - reference: make_map
          stage: after_lock
          method: Lib.MakeMap
          params:
          - action: const
            value: data_from_diod
          - action: get
            key: references.diod_update_keys
  namespace: diod
`

func (s *testProcessorSuite) SetupTest() {
	var mans manifest.Manifests
	err := yaml.Unmarshal([]byte(manifestYml), &mans)
	require.NoError(s.T(), err)

	s.calculatorClientMock = calculatorMock.NewMockClient(s.Ctrl())
	for _, m := range mans {
		m.SetClient(s.calculatorClientMock)
	}

	config := &core.Config{Debug: true, Manifests: mans}
	parentCtx := context.Background()

	clients, err := interactions.NewClients(parentCtx, config, s.TVM(), s.Registry())
	require.NoError(s.T(), err)

	s.accountsMock = bmocks.NewMockAccounter(s.Ctrl())
	clients.Accounts = s.accountsMock

	s.ctx = procContext.ProcessorContext{
		Context:      extracontext.NewWithParent(parentCtx),
		Config:       config,
		YtReferences: nil,
		Clients:      clients,
		Registry:     s.SolomonRegistry(),
		TVM:          s.TVM(),
	}
}

func (s *testProcessorSuite) TestSuccessProcess() {
	m := ProcessMessage{
		Namespace: "taxi",
		Endpoint:  "revenue",
		Event: map[string]any{
			"contract_id": 52607,
			"client_id":   666,
		},
	}

	lockUID := "uid-lock-666"

	s.accountsMock.EXPECT().InitLock(gomock.Any(), gomock.Any(), gomock.Eq(6)).Return(&lockUID, nil)
	s.accountsMock.EXPECT().RemoveLock(gomock.Any(), gomock.Any(), gomock.Eq(lockUID)).Return(nil)
	s.accountsMock.EXPECT().PingLock(gomock.Any(), gomock.Any(), gomock.Eq(lockUID), gomock.Eq(6)).Times(1).Return(nil)
	s.accountsMock.EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Return(int64(0), nil)

	calculatorResp := calculator.ProcessResponse{
		Status: "success",
		Data: calculator.ProcessResponseData{
			Event: map[string]any{"id": "999", "contract_id": "52607"},
			ClientTransactions: []any{
				map[string]any{
					"client_id":    1,
					"transactions": []any{map[string]any{"test": 1}},
				},
			},
			States: []any{"OK"},
		},
	}

	s.calculatorClientMock.EXPECT().Process(gomock.Any(), gomock.Any()).Return(calculatorResp, nil)

	_, err := ProcessAction(s.ctx, m)
	require.NoError(s.T(), err)
}

func (s *testProcessorSuite) TestErrors() {
	tests := []struct {
		testname  string
		input     ProcessMessage
		withError error
	}{
		{
			"unknown namespace error",
			ProcessMessage{Namespace: "unknown", Endpoint: "revenue"},
			manifest.ErrorUnknownNamespace,
		},
		{
			"unknown endpoint error",
			ProcessMessage{Namespace: "taxi", Endpoint: "unknown"},
			manifest.ErrorUnknownEndpoint,
		},
		{
			"empty stage error",
			ProcessMessage{Namespace: "taxi", Endpoint: "error-empty-stage"},
			manifest.ErrorEmptyStage,
		},
		{
			"before lock error",
			ProcessMessage{Namespace: "taxi", Endpoint: "error-before-lock"},
			manifest.ErrorUnknownParamAction,
		},
		{
			"after lock error",
			ProcessMessage{Namespace: "taxi", Endpoint: "error-after-lock"},
			manifest.ErrorUnknownParamAction,
		},
		{
			"before calc error",
			ProcessMessage{Namespace: "taxi", Endpoint: "error-before-calc"},
			manifest.ErrorUnknownParamAction,
		},
		{
			"after calc error",
			ProcessMessage{Namespace: "taxi", Endpoint: "error-after-calc"},
			manifest.ErrorUnknownParamAction,
		},
		{
			"wrong calc reference error",
			ProcessMessage{Namespace: "taxi", Endpoint: "error-wrong-calc-reference"},
			manifest.ErrorBadCalcReference,
		},
	}

	s.calculatorClientMock.EXPECT().Process(
		gomock.Any(), gomock.Any(),
	).Return(calculator.ProcessResponse{Status: "success"}, nil)

	for _, test := range tests {
		s.T().Run(test.testname, func(t *testing.T) {
			_, err := ProcessAction(s.ctx, test.input)
			require.Error(t, err)
			assert.ErrorIs(t, err, test.withError)
		})
	}
}

func TestProcessAction(t *testing.T) {
	suite.Run(t, &testProcessorSuite{})
}

type testAccounterLockSuite struct {
	btesting.BaseSuite
	manifestMock *manifestMock.MockManifestInterface
	ctx          *procContext.ProcessorContext
}

func (s *testAccounterLockSuite) SetupTest() {
	parentCtx, cancel := context.WithTimeout(context.Background(), time.Second*15)
	defer cancel()
	s.manifestMock = manifestMock.NewMockManifestInterface(s.Ctrl())

	s.ctx = &procContext.ProcessorContext{
		Context:      extracontext.NewWithParent(parentCtx),
		YtReferences: nil,
		Registry:     s.SolomonRegistry(),
		TVM:          s.TVM(),
	}

}

func (s *testAccounterLockSuite) TestGetLock() {
	clientID := "666"
	loc := &entities.LocationAttributes{Type: "cutoff_lock", Attributes: map[string]*string{"client_id": &clientID}}
	adaptorContext := common.NewSyncStringMapWithData(map[string]any{"loc": loc})

	lockMock := manifestMock.NewMockAccounterLockInterface(s.Ctrl())

	accounterManifestMock := manifestMock.NewMockAccounterManifestInterface(s.Ctrl())
	accounterManifestMock.EXPECT().CreateExclusiveLock(
		s.ctx,
		gomock.Eq(loc),
		gomock.Eq(6),
		gomock.Eq(2),
	).Times(1).Return(lockMock, nil)

	s.manifestMock.EXPECT().Accounter().Return(accounterManifestMock)

	locks, err := getAccounterLocksFor(s.ctx, []manifest.Action{
		{
			Method:    "Accounter.CreateExclusiveLock",
			Reference: "test",
			Stage:     "lock",
			Params: []manifest.Param{
				{
					Action: "get",
					Key:    "loc",
				},
				{
					Action: "const",
					Value:  "6",
				},
				{
					Action: "const",
					Value:  "2",
				},
			},
		},
	}, s.manifestMock, adaptorContext)
	require.NoError(s.T(), err)
	assert.Equal(s.T(), map[string]manifest.AccounterLockInterface{"test": lockMock}, locks)
}

func (s *testAccounterLockSuite) TestGetErrorIfReferenceError() {
	accounterManifestMock := manifestMock.NewMockAccounterManifestInterface(s.Ctrl())
	s.manifestMock.EXPECT().Accounter().Return(accounterManifestMock)
	_, err := getAccounterLocksFor(s.ctx, []manifest.Action{
		{
			Method:    "Accounter.CreateExclusiveLock",
			Reference: "test",
			Stage:     lock,
			Params: []manifest.Param{
				{
					Action: "get",
					// отсутствующий параметр
					Key: "loc",
				},
				{
					Action: "const",
					Value:  "6",
				},
				{
					Action: "const",
					Value:  "2",
				},
				{
					Action: "const",
					Value:  "true",
				},
			},
		},
	}, s.manifestMock, nil)
	require.Error(s.T(), err)
}

func (s *testAccounterLockSuite) TestGetErrorIfCantCastLock() {
	clientID := "666"
	loc := &entities.LocationAttributes{Type: "cutoff_lock", Attributes: map[string]*string{"client_id": &clientID}}
	adaptorContext := common.NewSyncStringMapWithData(map[string]any{"loc": loc})

	accounterManifestMock := manifestMock.NewMockAccounterManifestInterface(s.Ctrl())
	accounterManifestMock.EXPECT().CreateExclusiveLock(
		s.ctx,
		gomock.Eq(loc),
		gomock.Eq(6),
		gomock.Eq(2),
	).Times(1).Return(nil, nil)

	s.manifestMock.EXPECT().Accounter().Return(accounterManifestMock)

	_, err := getAccounterLocksFor(s.ctx, []manifest.Action{
		{
			Method:    "Accounter.CreateExclusiveLock",
			Reference: "test",
			Stage:     lock,
			Params: []manifest.Param{
				{Action: "get", Key: "loc"},
				{Action: "const", Value: "6"},
				{Action: "const", Value: "2"},
			},
		},
	}, s.manifestMock, adaptorContext)

	require.Error(s.T(), err)
}

func (s *testAccounterLockSuite) TestLockChain_Lock() {
	lockA := manifestMock.NewMockAccounterLockInterface(s.Ctrl())
	lockB := manifestMock.NewMockAccounterLockInterface(s.Ctrl())

	lockA.EXPECT().Lock().Times(1)
	lockA.EXPECT().ValidationErrors().Times(1).Return(nil)
	lockB.EXPECT().Lock().Times(1)
	lockB.EXPECT().ValidationErrors().Times(1).Return(nil)

	lockChain := CreateLockChain(
		s.ctx,
		map[string]manifest.AccounterLockInterface{"a": lockA, "b": lockB},
	)

	err := lockChain.Lock()
	require.NoError(s.T(), err)
}

func (s *testAccounterLockSuite) TestLockChain_LockWithValidationError() {
	lockA := manifestMock.NewMockAccounterLockInterface(s.Ctrl())
	lockB := manifestMock.NewMockAccounterLockInterface(s.Ctrl())

	errorChA := make(chan error, 1)
	errorChB := make(chan error, 1)

	lockA.EXPECT().Lock().Times(1)
	lockA.EXPECT().ValidationErrors().Times(1).Return(errorChA)
	lockB.EXPECT().Lock().Times(1)
	lockB.EXPECT().ValidationErrors().Times(1).Return(errorChB)

	lockChain := CreateLockChain(
		s.ctx,
		map[string]manifest.AccounterLockInterface{"a": lockA, "b": lockB},
	)

	err := lockChain.Lock()
	require.NoError(s.T(), err)

	go func() {
		errorChA <- xerrors.New("error A")
		errorChB <- xerrors.New("error B")
	}()

	<-lockChain.validationErrors
	<-lockChain.validationErrors
}

func (s *testAccounterLockSuite) TestLockChain_LockError() {
	lockA := manifestMock.NewMockAccounterLockInterface(s.Ctrl())
	lockChain := CreateLockChain(
		s.ctx,
		map[string]manifest.AccounterLockInterface{"a": lockA},
	)
	lockA.EXPECT().Lock().Times(1).Return(xerrors.Errorf("test"))
	lockA.EXPECT().ValidationErrors().Times(1).Return(nil)
	err := lockChain.Lock()
	require.Error(s.T(), err)
}

func (s *testAccounterLockSuite) TestLockChain_Unlock() {
	lockA := manifestMock.NewMockAccounterLockInterface(s.Ctrl())
	lockB := manifestMock.NewMockAccounterLockInterface(s.Ctrl())
	lockChain := CreateLockChain(
		s.ctx,
		map[string]manifest.AccounterLockInterface{"a": lockA, "b": lockB},
	)
	lockA.EXPECT().Unlock().Times(1)
	lockB.EXPECT().Unlock().Times(1)

	err := lockChain.Unlock()
	require.NoError(s.T(), err)
	_, ok := <-lockChain.validationErrors
	require.False(s.T(), ok, "channel closed")
}

func (s *testAccounterLockSuite) TestLockChain_UnlockError() {
	lockA := manifestMock.NewMockAccounterLockInterface(s.Ctrl())
	lockB := manifestMock.NewMockAccounterLockInterface(s.Ctrl())
	lockChain := CreateLockChain(
		s.ctx,
		map[string]manifest.AccounterLockInterface{"a": lockA, "b": lockB},
	)

	lockA.EXPECT().Unlock().Times(1).Return(xerrors.Errorf("test"))
	lockB.EXPECT().Unlock().Times(1).Return(xerrors.Errorf("test"))
	lockA.EXPECT().UID().Times(1).Return("test-A")
	lockB.EXPECT().UID().Times(1).Return("test-B")

	err := lockChain.Unlock()
	require.Error(s.T(), err)
}

func TestAccounterLockSuite(t *testing.T) {
	suite.Run(t, &testAccounterLockSuite{})
}

func Test_getIncludedReferences(t *testing.T) {
	references := common.NewSyncStringMapWithData(map[string]any{"a": 1, "b": "test_b", "c": nil})

	tests := []struct {
		testname   string
		references any
		included   []string
		want       *common.SyncStringMap
		withError  bool
	}{
		{
			testname:   "get references",
			references: references,
			included:   []string{"b", "c"},
			want:       common.NewSyncStringMapWithData(map[string]any{"b": "test_b", "c": nil}),
			withError:  false,
		},
		{
			testname:   "get error if references is not map",
			references: 120,
			included:   []string{"b", "c"},
			withError:  true,
		},
		{
			testname:   "get error if non-existent reference in included",
			references: references,
			included:   []string{"b", "z"},
			withError:  true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			actual, err := getIncludedReferences(test.references, test.included)
			if test.withError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
				assert.Equal(t, test.want, actual)
			}
		})
	}
}

type testProcessorDiodSuite struct {
	btesting.BaseSuite
	calculatorClientMock *calculatorMock.MockClient
	diodClientMock       *diodMock.MockClient
	ctx                  procContext.ProcessorContext
}

func (s *testProcessorDiodSuite) setupDiodMock() {
	s.diodClientMock = diodMock.NewMockClient(s.Ctrl())
	s.ctx.Clients.Diod = s.diodClientMock
	//for _, m := range s.ctx.Config.Manifests {
	//	m.SetDiodClient(s.diodClientMock)
	//}
}

func (s *testProcessorDiodSuite) SetupTest() {
	var mans manifest.Manifests
	err := yaml.Unmarshal([]byte(manifestYml), &mans)
	require.NoError(s.T(), err)

	s.calculatorClientMock = calculatorMock.NewMockClient(s.Ctrl())
	for _, m := range mans {
		m.SetClient(s.calculatorClientMock)
	}

	config := &core.Config{Debug: true, Manifests: mans}
	parentCtx := context.Background()

	clients, err := interactions.NewClients(parentCtx, config, s.TVM(), s.Registry())
	require.NoError(s.T(), err)

	s.ctx = procContext.ProcessorContext{
		Context:      extracontext.NewWithParent(parentCtx),
		Config:       config,
		YtReferences: nil,
		Clients:      clients,
		Registry:     s.SolomonRegistry(),
		TVM:          s.TVM(),
	}
}

func (s *testProcessorDiodSuite) TestDiodNotSetTest() {

	// if diod is not set - cannot invoke diod client within actions.
	m := ProcessMessage{
		Namespace: "diod",
		Endpoint:  "diod-test",
		Event: map[string]any{
			"contract_id": 52607,
			"client_id":   666,
			"keys":        []string{"abc", "cde"},
		},
	}
	_, err := ProcessAction(s.ctx, m)
	s.Require().EqualError(err, "Error in process action Stage: before_lock, Method: Diod.GetKeys, Reference: diod_test: 'diod client is not set'")

	// if diod is not set - can invoke actions without diod.
	m.Endpoint = "no-diod"
	calculatorResp := calculator.ProcessResponse{
		Status: "success",
		Data: calculator.ProcessResponseData{
			Event: map[string]any{"id": "999", "contract_id": "52607"},
			ClientTransactions: []any{
				map[string]any{
					"client_id":    1,
					"transactions": []any{map[string]any{"test": 1}},
				},
			},
			States: []any{"OK"},
		},
	}

	s.calculatorClientMock.EXPECT().Process(gomock.Any(), gomock.Any()).Return(calculatorResp, nil)

	_, err = ProcessAction(s.ctx, m)
	s.Require().NoError(err)
}

func (s *testProcessorDiodSuite) TestDiodTest() {
	s.setupDiodMock()

	m := ProcessMessage{
		Namespace: "diod",
		Endpoint:  "diod-test",
		Event: map[string]any{
			"contract_id": 52607,
			"client_id":   666,
			"keys":        []string{"abc", "cde"},
		},
	}

	s.diodClientMock.EXPECT().GetKeys(gomock.Any(), gomock.Any()).Times(1).Return(diod.Response{
		Response: nil,
		Status:   "ok",
		Data: struct {
			Items []diodEntities.Data
		}{
			[]diodEntities.Data{
				{
					ServiceID: "accounts",
					Namespace: "namespace",
					Key:       "key",
					Revision:  0,
					Value:     "value",
					CreatedAt: time.Now(),
					UpdatedAt: time.Now(),
					Created:   false,
				},
			},
		},
	}, nil)

	calculatorResp := calculator.ProcessResponse{
		Status: "success",
		Data: calculator.ProcessResponseData{
			Event: map[string]any{"id": "999", "contract_id": "52607"},
			ClientTransactions: []any{
				map[string]any{
					"client_id":    1,
					"transactions": []any{map[string]any{"test": 1}},
				},
			},
			States: []any{"OK"},
		},
	}

	s.calculatorClientMock.EXPECT().Process(gomock.Any(), gomock.Any()).Return(calculatorResp, nil)

	res, err := ProcessAction(s.ctx, m)
	require.NoError(s.T(), err)

	s.Assert().NotEmpty(res)
}

func (s *testProcessorDiodSuite) TestDiodEmptyGetKeys() {
	s.setupDiodMock()

	m := ProcessMessage{
		Namespace: "diod",
		Endpoint:  "diod-test",
		Event: map[string]any{
			"contract_id": 52607,
			"client_id":   666,
			"keys":        []string{},
		},
	}

	s.calculatorClientMock.EXPECT().Process(gomock.Any(), gomock.Any()).Return(calculator.ProcessResponse{}, nil)
	_, err := ProcessAction(s.ctx, m)
	s.Require().NoError(err)

	m = ProcessMessage{
		Namespace: "diod",
		Endpoint:  "diod-test",
		Event: map[string]any{
			"contract_id": 52607,
			"client_id":   666,
		},
	}

	s.calculatorClientMock.EXPECT().Process(gomock.Any(), gomock.Any()).Return(calculator.ProcessResponse{}, nil)
	_, err = ProcessAction(s.ctx, m)
	s.Require().NoError(err)
}

func (s *testProcessorDiodSuite) TestDiodEmptyUpdateKeys() {
	s.setupDiodMock()

	m := ProcessMessage{
		Namespace: "diod",
		Endpoint:  "diod-test-update-keys",
		Event: map[string]any{
			"contract_id": 52607,
			"client_id":   666,
			"keys":        []map[string]any{},
		},
	}

	s.calculatorClientMock.EXPECT().Process(gomock.Any(), gomock.Any()).Return(calculator.ProcessResponse{}, nil)
	s.diodClientMock.EXPECT().UpdateKeys(gomock.Any(), gomock.Any()).Return(diod.Response{}, nil)
	_, err := ProcessAction(s.ctx, m)
	s.Require().NoError(err)
}

func (s *testProcessorDiodSuite) TestDiodReferencesGetKeys() {
	s.setupDiodMock()

	m := ProcessMessage{
		Namespace: "diod",
		Endpoint:  "diod-test-references-get-keys",
		Event: map[string]any{
			"contract_id": 52607,
			"client_id":   666,
			"keys":        []string{"abc", "cde"},
		},
	}

	s.diodClientMock.EXPECT().GetKeys(gomock.Any(), gomock.Any()).Times(1).Return(diod.Response{
		Response: nil,
		Status:   "ok",
		Data: struct {
			Items []diodEntities.Data
		}{
			[]diodEntities.Data{
				{
					ServiceID: "accounts",
					Namespace: "namespace",
					Key:       "key",
					Revision:  0,
					Value:     "value",
					CreatedAt: time.Now(),
					UpdatedAt: time.Now(),
					Created:   false,
				},
			},
		},
	}, nil)

	calculatorResp := calculator.ProcessResponse{
		Status: "success",
		Data: calculator.ProcessResponseData{
			Event: map[string]any{"id": "999", "contract_id": "52607"},
			ClientTransactions: []any{
				map[string]any{
					"client_id":    1,
					"transactions": []any{map[string]any{"test": 1}},
				},
			},
			States: []any{"OK"},
		},
	}

	s.calculatorClientMock.EXPECT().Process(gomock.Any(), gomock.Any()).Return(calculatorResp, nil)

	res, err := ProcessAction(s.ctx, m)
	require.NoError(s.T(), err)

	s.Assert().NotEmpty(res)
}

func (s *testProcessorDiodSuite) TestDiodReferencesUpdateKeys() {
	s.setupDiodMock()

	m := ProcessMessage{
		Namespace: "diod",
		Endpoint:  "diod-test-references-update-keys",
		Event: map[string]any{
			"contract_id": 52607,
			"client_id":   666,
			"keys": []map[string]any{
				{
					"namespace": "namespace",
					"key":       "key",
					"value":     "value",
					"immutable": false,
				},
			},
		},
	}

	s.diodClientMock.EXPECT().UpdateKeys(gomock.Any(), gomock.Any()).Times(1).Return(diod.Response{
		Response: nil,
		Status:   "ok",
		Data: struct {
			Items []diodEntities.Data
		}{
			[]diodEntities.Data{
				{
					ServiceID: "accounts",
					Namespace: "namespace",
					Key:       "key",
					Revision:  0,
					Value:     "value",
					CreatedAt: time.Now(),
					UpdatedAt: time.Now(),
					Created:   false,
				},
			},
		},
	}, nil)

	calculatorResp := calculator.ProcessResponse{
		Status: "success",
		Data: calculator.ProcessResponseData{
			Event: map[string]any{"id": "999", "contract_id": "52607"},
			ClientTransactions: []any{
				map[string]any{
					"client_id":    1,
					"transactions": []any{map[string]any{"test": 1}},
				},
			},
			States: []any{"OK"},
		},
	}

	s.calculatorClientMock.EXPECT().Process(gomock.Any(), gomock.Any()).Return(calculatorResp, nil)

	res, err := ProcessAction(s.ctx, m)
	require.NoError(s.T(), err)

	s.Assert().NotEmpty(res)
}

func TestProcessDiodAction(t *testing.T) {
	suite.Run(t, &testProcessorDiodSuite{})
}
