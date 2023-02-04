package manifest

import (
	"context"
	"errors"
	"fmt"
	"sync"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/extracontext"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions/accounts"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

var (
	clientID = "666"
	dt       = int64(1618866000)
	dt2      = int64(1619085651)
	location = &entities.LocationAttributes{
		Type: "cutoff_lock",
		Attributes: map[string]*string{
			"client_id": &clientID,
		},
	}
	timeout  = 7
	uid      = "test-uid"
	lockInfo = entities.LockInfo{
		UID:    uid,
		Locked: false,
	}
)

func TestExclusiveAccounterLock_Lock(t *testing.T) {
	ctx := extracontext.NewWithParent(context.Background())
	ctrl := gomock.NewController(t)

	accounter := mock.NewMockAccounter(ctrl)

	lock := ExclusiveAccounterLock{
		AccounterLock: AccounterLock{
			accounter:       accounter,
			ctx:             ctx,
			accountLocation: location,
		},
		m:                &sync.Mutex{},
		timeout:          timeout,
		tickTime:         1,
		tickerAbort:      make(chan struct{}),
		validationErrors: make(chan error),
	}

	accounter.EXPECT().InitLock(gomock.Eq(ctx), gomock.Eq(location), gomock.Eq(timeout)).Times(1).Return(&uid, nil)

	err := lock.Lock()
	require.NoError(t, err)
	assert.Equal(t, true, lock.locked)
	assert.Equal(t, uid, lock.UID())
	_ = lock.Release()
}

func TestExclusiveAccounterLock_LockErrorOnInitLock(t *testing.T) {
	ctx := extracontext.NewWithParent(context.Background())
	ctrl := gomock.NewController(t)
	accounter := mock.NewMockAccounter(ctrl)

	lock := ExclusiveAccounterLock{
		AccounterLock: AccounterLock{
			accounter:       accounter,
			ctx:             ctx,
			accountLocation: location,
		},
		m:       &sync.Mutex{},
		timeout: timeout,
	}

	initErr := errors.New("test-error")
	accounter.EXPECT().InitLock(gomock.Eq(ctx), gomock.Eq(location), gomock.Eq(timeout)).Times(1).Return(nil, initErr)

	err := lock.Lock()
	require.Error(t, err)
	assert.Equal(t, err, initErr)
	assert.Equal(t, false, lock.locked)
	assert.Equal(t, "", lock.UID())
}

func TestExclusiveAccounterLock_Unlock(t *testing.T) {
	tests := []struct {
		testname        string
		removeLockError error
	}{
		{
			"without remove lock",
			nil,
		},
		{
			"with remove lock on already removed",
			errors.New("already removed"),
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(tInner *testing.T) {
			ctx := extracontext.NewWithParent(context.Background())
			removeLockTimes := 1

			accounter := mock.NewMockAccounter(gomock.NewController(tInner))
			accounter.EXPECT().InitLock(gomock.Eq(ctx), gomock.Eq(location), gomock.Eq(timeout)).Return(&uid, nil)
			accounter.EXPECT().RemoveLock(gomock.Eq(ctx), gomock.Eq(location), gomock.Eq(uid)).Times(removeLockTimes).Return(test.removeLockError)

			lock := ExclusiveAccounterLock{
				AccounterLock: AccounterLock{
					accounter:       accounter,
					ctx:             ctx,
					accountLocation: location,
				},
				m:                &sync.Mutex{},
				timeout:          timeout,
				tickTime:         1,
				tickerAbort:      make(chan struct{}),
				validationErrors: make(chan error),
			}

			err := lock.Lock()
			require.NoError(tInner, err)
			err = lock.Unlock()

			if test.removeLockError != nil {
				assert.ErrorIs(tInner, test.removeLockError, err)
			} else {
				require.NoError(tInner, err)
			}

			assert.Equal(tInner, false, lock.locked)
			assert.Equal(tInner, uid, lock.UID())
			_, ok := <-lock.validationErrors
			assert.False(tInner, ok, "error channel closed")
		})
	}
}

func TestExclusiveAccounterLock_ReleaseOnValidateErrors(t *testing.T) {
	ctx := extracontext.NewWithParent(context.Background())
	accounter := mock.NewMockAccounter(gomock.NewController(t))
	accounter.EXPECT().InitLock(gomock.Eq(ctx), gomock.Eq(location), gomock.Eq(timeout)).Return(&uid, nil)
	accounter.EXPECT().PingLock(
		gomock.Eq(ctx),
		locationMatcher{location},
		gomock.Eq(uid),
		gomock.Eq(timeout),
	).Return(accounts.LockUpdateFail)

	lock := ExclusiveAccounterLock{
		AccounterLock: AccounterLock{
			accounter:       accounter,
			ctx:             ctx,
			accountLocation: location,
		},
		m:                &sync.Mutex{},
		timeout:          timeout,
		tickTime:         1,
		tickerAbort:      make(chan struct{}),
		validationErrors: make(chan error),
	}

	err := lock.Lock()
	require.NoError(t, err)
	_, ok := <-lock.validationErrors
	assert.True(t, ok, "got error in channel")
	_, ok = <-lock.validationErrors
	assert.False(t, ok, "close error channel")
	assert.Equal(t, false, lock.locked)
}

func TestSharedAccountLock_Lock(t *testing.T) {
	ctx := extracontext.NewWithParent(context.Background())
	ctrl := gomock.NewController(t)

	accounter := mock.NewMockAccounter(ctrl)
	lock := SharedAccountLock{
		AccounterLock: AccounterLock{
			accounter:       accounter,
			ctx:             ctx,
			accountLocation: location,
		},
	}

	accounter.EXPECT().GetLock(gomock.Eq(ctx), gomock.Eq(location)).Times(1).Times(1).Return(&lockInfo, nil)

	err := lock.Lock()
	require.NoError(t, err)
	assert.Equal(t, uid, lock.UID())
}

func TestSharedAccountLock_LockErrorOnGetLock(t *testing.T) {
	ctx := extracontext.NewWithParent(context.Background())
	ctrl := gomock.NewController(t)

	accounter := mock.NewMockAccounter(ctrl)

	tests := []struct {
		testname  string
		lockInfo  *entities.LockInfo
		lockError error
		wantError string
	}{
		{
			"error on get info",
			nil,
			errors.New("get info error"),
			"get info error",
		},
		{
			"error if location locked",
			&entities.LockInfo{
				UID:    uid,
				Locked: true,
			},
			nil,
			"location locked",
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			lock := SharedAccountLock{
				AccounterLock: AccounterLock{
					accounter:       accounter,
					ctx:             ctx,
					accountLocation: location,
				},
			}

			accounter.EXPECT().GetLock(gomock.Eq(ctx), gomock.Eq(location)).Times(1).Return(test.lockInfo, test.lockError)

			err := lock.Lock()
			require.Error(t, err)
			assert.Equal(t, "", lock.UID())
			assert.Contains(t, err.Error(), test.wantError)
		})
	}
}

type locationMatcher struct {
	x *entities.LocationAttributes
}

func (m locationMatcher) String() string {
	return fmt.Sprintf("is equal to %v", m.x)
}

func (m locationMatcher) Matches(x any) bool {
	x2, ok := x.(*entities.LocationAttributes)
	if !ok || m.x.Type != x2.Type {
		return false
	}
	if len(m.x.Attributes) != len(x2.Attributes) {
		return false
	}

	for k, v := range m.x.Attributes {
		v2, ok := x2.Attributes[k]
		if !ok {
			return false
		}

		if (*v) != (*v2) {
			return false
		}
	}

	return true
}

type testAccounterAdaptorSuite struct {
	btesting.BaseSuite
	ctx       extracontext.Context
	accounter *mock.MockAccounter
	adaptor   AccounterManifestAdaptor
}

func (s *testAccounterAdaptorSuite) SetupTest() {
	ctrl := gomock.NewController(s.T())
	s.ctx = extracontext.NewWithParent(context.Background())
	s.accounter = mock.NewMockAccounter(ctrl)
	s.adaptor = AccounterManifestAdaptor{
		accounts: s.accounter,
	}
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_NilLock() {
	lock, err := s.adaptor.NilLock(s.ctx)
	require.NoError(s.T(), err)
	assert.Equal(s.T(), &NilLock{}, lock)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_CreateSharedLock() {
	lock, err := s.adaptor.CreateSharedLock(s.ctx, location)
	require.NoError(s.T(), err)
	sharedLock, ok := lock.(*SharedAccountLock)
	require.True(s.T(), ok)
	assert.Equal(s.T(), sharedLock.ctx, s.ctx)
	assert.Equal(s.T(), sharedLock.accountLocation, location)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_CreateExclusiveLock() {
	tickTime := 5

	lock, err := s.adaptor.CreateExclusiveLock(s.ctx, location, timeout, tickTime)
	require.NoError(s.T(), err)
	exclusiveLock, ok := lock.(*ExclusiveAccounterLock)
	require.True(s.T(), ok)
	assert.Equal(s.T(), exclusiveLock.ctx, s.ctx)
	assert.Equal(s.T(), exclusiveLock.timeout, timeout)
	assert.Equal(s.T(), exclusiveLock.tickTime, tickTime)
	assert.Equal(s.T(), exclusiveLock.accountLocation, location)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_GetAccountBalance() {
	attributes := []entities.BalanceAttributes{
		{
			Debit: "Debit", Credit: "Credit",
		},
	}
	s.accounter.EXPECT().GetAccountBalance(gomock.Eq(s.ctx), locationMatcher{location}, gomock.Eq(&dt)).Times(1).Return(attributes, nil)
	res, err := s.adaptor.GetAccountBalance(s.ctx, location, dt)
	require.NoError(s.T(), err)
	assert.Equal(s.T(), attributes, res)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_GetAccountTurnover() {
	attributes := []entities.TurnoverAttributes{
		{
			DebitInit: "DebitInit", CreditInit: "CreditInit",
		},
	}
	s.accounter.EXPECT().GetAccountTurnover(gomock.Eq(s.ctx), locationMatcher{location}, gomock.Eq(dt), gomock.Eq(dt2)).Times(1).Return(attributes, nil)
	res, err := s.adaptor.GetAccountTurnover(s.ctx, location, dt, dt2)
	require.NoError(s.T(), err)
	assert.Equal(s.T(), attributes, res)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_GetAccountDetailedTurnover() {
	attributes := []entities.DetailedTurnoverAttributes{
		{
			DebitInit: "DebitInit", CreditInit: "CreditInit",
		},
	}
	s.accounter.EXPECT().GetAccountDetailedTurnover(gomock.Eq(s.ctx), locationMatcher{location}, gomock.Eq(dt), gomock.Eq(dt2)).Times(1).Return(attributes, nil)
	res, err := s.adaptor.GetAccountDetailedTurnover(s.ctx, location, dt, dt2)
	require.NoError(s.T(), err)
	assert.Equal(s.T(), attributes, res)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_GetState() {
	state := []byte("test")
	s.accounter.EXPECT().GetState(gomock.Eq(s.ctx), locationMatcher{location}).Times(1).Return(state, nil)
	res, err := s.adaptor.GetState(s.ctx, location)
	require.NoError(s.T(), err)
	assert.Equal(s.T(), state, res)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_GetLock() {
	s.accounter.EXPECT().GetLock(gomock.Eq(s.ctx), locationMatcher{location}).Times(1).Return(&lockInfo, nil)
	res, err := s.adaptor.GetLock(s.ctx, location)
	require.NoError(s.T(), err)
	assert.Equal(s.T(), &lockInfo, res)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_InitLock() {
	s.accounter.EXPECT().InitLock(gomock.Eq(s.ctx), locationMatcher{location}, gomock.Eq(timeout)).Times(1).Return(&uid, nil)
	res, err := s.adaptor.InitLock(s.ctx, location, timeout)
	require.NoError(s.T(), err)
	assert.Equal(s.T(), &uid, res)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_PingLock() {
	s.accounter.EXPECT().PingLock(gomock.Eq(s.ctx), locationMatcher{location}, gomock.Eq(uid), gomock.Eq(timeout)).Times(1).Return(nil)
	lock := &AccounterLock{uid: uid}
	res, err := s.adaptor.PingLock(s.ctx, location, lock, timeout)
	require.NoError(s.T(), err)
	assert.Nil(s.T(), res)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_RemoveLock() {
	s.accounter.EXPECT().RemoveLock(gomock.Eq(s.ctx), locationMatcher{location}, gomock.Eq(uid)).Times(1).Return(nil)
	lock := &AccounterLock{uid: uid}
	res, err := s.adaptor.RemoveLock(s.ctx, location, lock)
	require.NoError(s.T(), err)
	assert.Nil(s.T(), res)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_WriteBatch() {
	now := time.Now()
	batchWriteReqMap := map[string]any{
		"event_type":  "test",
		"external_id": uid,
		"dt":          now,
	}

	batchWriteReq := entities.BatchWriteRequest{
		EventType:  "test",
		ExternalID: uid,
		Dt:         now,
	}

	var n int64 = 500
	s.accounter.EXPECT().WriteBatch(gomock.Eq(s.ctx), gomock.Eq(&batchWriteReq)).Times(1).Return(n, nil)
	res, err := s.adaptor.WriteBatch(s.ctx, batchWriteReqMap)
	require.NoError(s.T(), err)
	assert.Equal(s.T(), n, res)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_ReadBatch() {
	states := []entities.LocationAttributes{
		{
			Type: "test_type",
			Attributes: map[string]*string{
				"second": &clientID,
			},
		},
	}

	batchReadReqMap := map[string]any{
		"lock_timeout": timeout,
		"states":       states,
	}

	batchReadReq := entities.BatchReadRequest{
		LockTimeout: int64(timeout),
		States:      states,
	}

	batchReadResp := entities.BatchReadResponse{
		Locks: []entities.LockAttributes{{UID: uid}},
	}

	s.accounter.EXPECT().ReadBatch(gomock.Eq(s.ctx), gomock.Eq(&batchReadReq)).Times(1).Return(&batchReadResp, nil)
	res, err := s.adaptor.ReadBatch(s.ctx, batchReadReqMap)
	require.NoError(s.T(), err)
	assert.Equal(s.T(), &batchReadResp, res)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_WriteBatchWithLock() {
	now := time.Now()
	batchWriteReqMap := map[string]any{
		"event_type":  "test",
		"external_id": uid,
		"dt":          now,
	}

	batchWriteReq := entities.BatchWriteRequest{
		EventType:  "test",
		ExternalID: uid,
		Dt:         now,
		Locks: []entities.LockAction{
			{
				UID:  uid,
				Loc:  *location,
				Mode: entities.LockActionMode("add"),
			},
		},
	}

	var n int64 = 500
	s.accounter.EXPECT().WriteBatch(gomock.Eq(s.ctx), gomock.Eq(&batchWriteReq)).Times(1).Return(n, nil)
	lock := &AccounterLock{uid: uid}
	res, err := s.adaptor.WriteBatchWithLock(s.ctx, batchWriteReqMap, location, lock, "add")
	require.NoError(s.T(), err)
	assert.Equal(s.T(), n, res)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_WriteCalculatorResponse() {
	loc, _ := time.LoadLocation("UTC")
	eventType := "test_event_type"
	transactions := []any{map[string]any{"type": "test", "amount": "100.20"}}
	batchWriteReq := entities.BatchWriteRequest{
		EventType:  eventType,
		ExternalID: uid,
		Dt:         time.Unix(dt, 0).In(loc),
		Events: []entities.EventAttributes{
			{Type: "test", Amount: "100.20"},
		},
		Locks: []entities.LockAction{
			{
				UID:  uid,
				Loc:  *location,
				Mode: entities.LockValidateMode,
			},
		},
	}

	var n int64 = 500
	s.accounter.EXPECT().WriteBatch(gomock.Eq(s.ctx), gomock.Eq(&batchWriteReq)).Times(1).Return(n, nil)
	lock := &AccounterLock{uid: uid}
	res, err := s.adaptor.WriteCalculatorResponse(s.ctx, eventType, uid, dt, nil, transactions, location, lock)
	require.NoError(s.T(), err)
	assert.Equal(s.T(), n, res)
}

func (s *testAccounterAdaptorSuite) TestAccounterManifestAdaptor_WriteCalculatorResponseNRemoveLock() {
	loc, _ := time.LoadLocation("UTC")
	eventType := "test_event_type"
	transactions := []any{map[string]any{"type": "test", "amount": "100.20"}}
	states := []any{map[string]any{"state": "test_state"}}
	batchWriteReq := entities.BatchWriteRequest{
		EventType:  eventType,
		ExternalID: uid,
		Dt:         time.Unix(dt, 0).In(loc),
		Events: []entities.EventAttributes{
			{Type: "test", Amount: "100.20"},
		},
		Locks: []entities.LockAction{
			{
				UID:  uid,
				Loc:  *location,
				Mode: entities.LockRemoveMode,
			},
		},
		States: []entities.StateAttributes{
			{State: []byte("\"test_state\"")},
		},
	}

	var n int64 = 500
	s.accounter.EXPECT().WriteBatch(gomock.Eq(s.ctx), gomock.Eq(&batchWriteReq)).Times(1).Return(n, nil)
	lock := &AccounterLock{uid: uid}
	res, err := s.adaptor.WriteCalculatorResponseNRemoveLock(
		s.ctx, eventType, uid, dt, nil, transactions, location, lock, states,
	)
	require.NoError(s.T(), err)
	assert.Equal(s.T(), n, res)
}

func TestGetAccounterLockForSuite(t *testing.T) {
	suite.Run(t, &testAccounterAdaptorSuite{})
}
