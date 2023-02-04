package mock

import (
	"fmt"
	"math/rand"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/revert"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

type Scheduler struct {
	RunCtxs      []*scheduler.Context
	ForceRunCtxs []*scheduler.Context
	UpdateCtxs   []*scheduler.Context
	StopCtxs     []*scheduler.Context
	RestartCtxs  []*scheduler.Context
	StateCtxs    []*scheduler.Context
	CancelCtxs   []*scheduler.Context
	Error        error
	resultsMutex sync.Mutex
	results      map[string]chan *scheduler.State
}

func NewMockScheduler() *Scheduler {

	return &Scheduler{
		Error:        nil,
		resultsMutex: sync.Mutex{},
		results:      map[string]chan *scheduler.State{},
	}
}

func (mock *Scheduler) id() int64 {

	return rand.Int63()
}

func (mock *Scheduler) Run(c *scheduler.Context) (int64, chan *scheduler.State, error) {
	mock.RunCtxs = append(mock.RunCtxs, c)
	return mock.id(), mock.result(common.Run, c.ServiceMap.Name, c.BranchName, c.Version), mock.Error
}

func (mock *Scheduler) ForceRun(c *scheduler.Context) (int64, chan *scheduler.State, error) {
	mock.ForceRunCtxs = append(mock.ForceRunCtxs, c)
	return mock.id(), mock.result(common.Run, c.ServiceMap.Name, c.BranchName, c.Version), mock.Error
}

func (mock *Scheduler) Update(c *scheduler.Context) (int64, chan *scheduler.State, error) {
	mock.UpdateCtxs = append(mock.UpdateCtxs, c)
	return mock.id(), mock.result(common.Update, c.ServiceMap.Name, c.BranchName, c.Version), mock.Error
}

func (mock *Scheduler) Stop(c *scheduler.Context) (int64, chan *scheduler.State, error) {
	mock.StopCtxs = append(mock.StopCtxs, c)
	return mock.id(), mock.result(common.Stop, c.ServiceMap.Name, c.BranchName, ""), mock.Error
}

func (mock *Scheduler) Restart(c *scheduler.Context) (int64, chan *scheduler.State, error) {
	mock.RestartCtxs = append(mock.RestartCtxs, c)
	return mock.id(), mock.result(common.Restart, c.ServiceMap.Name, c.BranchName, ""), mock.Error
}

func (mock *Scheduler) Restore(c *scheduler.Context) (chan *scheduler.State, error) {
	mock.StateCtxs = append(mock.StateCtxs, c)
	return mock.result(common.Run, c.ServiceMap.Name, c.BranchName, fmt.Sprintf("id:%d", c.NomadId)), nil
}

func (mock *Scheduler) State(c *scheduler.Context) (chan *scheduler.State, error) {
	mock.StateCtxs = append(mock.StateCtxs, c)
	return mock.result(common.Run, c.ServiceMap.Name, c.BranchName, fmt.Sprintf("id:%d", c.NomadId)), nil
}

func (mock *Scheduler) Cancel(c *scheduler.Context) (int64, chan *scheduler.State, error) {
	mock.CancelCtxs = append(mock.CancelCtxs, c)
	resultID := mock.id()
	return resultID, mock.result(common.Cancel, c.ServiceMap.Name, c.BranchName, fmt.Sprintf("%d/%d", c.NomadId, resultID)), mock.Error
}

func (mock *Scheduler) result(command common.Type, name, branch, version string) chan *scheduler.State {
	c := make(chan *scheduler.State)
	mock.resultsMutex.Lock()
	mock.results[mock.key(command, name, branch, version)] = c
	mock.resultsMutex.Unlock()
	return c
}

func (mock *Scheduler) key(command common.Type, name, branch, version string) string {
	key := strings.Join([]string{command.String(), name, branch, version}, "_")
	return key
}

func (mock *Scheduler) IsEmpty() bool {
	return len(mock.results) == 0
}

func (mock *Scheduler) Success(t *testing.T, command common.Type, name, branch, version string) {
	mock.close(t, command, name, branch, version, revert.RevertType_None, scheduler.Success)
}

func (mock *Scheduler) SuccessStop(t *testing.T, command common.Type, name, branch string) {

	test.Wait(t, func() error {
		if len(mock.StopCtxs) > 0 && len(mock.results) > 0 {
			return nil
		}
		return fmt.Errorf("don't stoped")
	})
	mock.close(t, command, name, branch, "", revert.RevertType_None, scheduler.Success)
}

func (mock *Scheduler) Process(t *testing.T, command common.Type, name, branch, version string) {
	key := mock.key(command, name, branch, version)
	c, err := mock.getChannel(key)
	require.NoError(t, err)
	select {
	case c <- &scheduler.State{StateType: scheduler.Process}:
	case <-time.NewTimer(5 * time.Second).C:
		assert.FailNow(t, "timeout")
	}

}

func (mock *Scheduler) Prepare(t *testing.T, command common.Type, name, branch, version string) {
	key := mock.key(command, name, branch, version)
	c, err := mock.getChannel(key)
	require.NoError(t, err)
	select {
	case c <- &scheduler.State{StateType: scheduler.Prepare, Description: "Deployment hasn't started"}:
	case <-time.NewTimer(5 * time.Second).C:
		assert.FailNow(t, "timeout")
	}
}

func (mock *Scheduler) Terminate(t *testing.T, command common.Type, name, branch, version string) {
	key := mock.key(command, name, branch, version)
	c, err := mock.getChannel(key)
	require.NoError(t, err)
	c <- &scheduler.State{
		Number: []scheduler.Number{
			{
				DC:            "sas",
				Total:         3,
				Placed:        1,
				SuccessPlaced: 0,
			},
			{
				DC:            "myt",
				Total:         3,
				Placed:        1,
				SuccessPlaced: 0,
			},
		},
		StateType:         scheduler.Revert,
		Description:       "revert message",
		RevertType:        revert.RevertType_Terminate,
		FailedAllocations: []string{"111", "222"},
	}
}

func (mock *Scheduler) RevertSuccess(t *testing.T, command common.Type, name, branch, version string) {
	mock.close(t, command, name, branch, version, revert.RevertType_Undefined, scheduler.RevertSuccess)
}

func (mock *Scheduler) Canceled(t *testing.T, command common.Type, name, branch, version string) {
	mock.close(t, command, name, branch, version, revert.RevertType_Undefined, scheduler.RevertSuccess)
}

func (mock *Scheduler) Fail(t *testing.T, command common.Type, name, branch, version string) {
	mock.close(t, command, name, branch, version, revert.RevertType_Unhealthy, scheduler.Fail)
}

func (mock *Scheduler) CancelAndReverted(t *testing.T, command common.Type, name, branch, version string) {
	mock.close(t, command, name, branch, version, revert.RevertType_Undefined, scheduler.Revert, scheduler.RevertSuccess)
}

func (mock *Scheduler) CancelAndCanceled(t *testing.T, command common.Type, name, branch, version string) {
	mock.close(t, command, name, branch, version, revert.RevertType_None, scheduler.Fail)
}

func (mock *Scheduler) close(t *testing.T, command common.Type, name, branch, version string, revertType revert.RevertType, stateType ...scheduler.StateType) {
	key := mock.key(command, name, branch, version)
	c, err := mock.getChannel(key)
	require.NoError(t, err)
	for _, state := range stateType {
		select {
		case c <- &scheduler.State{StateType: state, RevertType: revertType}:
		case <-time.NewTimer(5 * time.Second).C:
			assert.FailNow(t, "timeout")
		}
	}
	close(c)
	mock.resultsMutex.Lock()
	delete(mock.results, key)
	mock.resultsMutex.Unlock()
}

func (mock *Scheduler) getChannel(key string) (chan *scheduler.State, error) {
	ticker := time.NewTicker(25 * time.Millisecond)
	defer ticker.Stop()
	timer := time.NewTimer(5 * time.Second)
	defer timer.Stop()
	for {
		select {
		case <-ticker.C:
			mock.resultsMutex.Lock()
			c, ok := mock.results[key]
			mock.resultsMutex.Unlock()
			if ok {
				return c, nil
			}
		case <-timer.C:
			return nil, fmt.Errorf("timeout by key %s. Data: %v", key, mock.results)
		}
	}
}

func (mock *Scheduler) CheckKey(command common.Type, name, branch, version string) bool {

	_, ok := mock.results[mock.key(command, name, branch, version)]
	return ok
}
