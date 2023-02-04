package election

import "sync"

type Election struct {
	c             chan bool
	closeOnce     sync.Once
	leaderCalls   chan bool
	DefaultLeader bool
}

func NewElectionStub() *Election {
	return &Election{
		c:             make(chan bool, 10),
		closeOnce:     sync.Once{},
		leaderCalls:   make(chan bool, 10),
		DefaultLeader: true,
	}
}

func (e *Election) State() <-chan bool {
	if len(e.c) == 0 {
		e.c <- true
	}
	return e.c
}

func (e *Election) Start() error {
	return nil
}

func (e *Election) Stop() {
	e.closeOnce.Do(func() {
		close(e.c)
	})
}

func (e *Election) IsLeader() bool {
	if len(e.leaderCalls) == 0 {
		return e.DefaultLeader
	}
	return <-e.leaderCalls
}
func (e *Election) SetIsLeaderCalls(values ...bool) *Election {
	for _, v := range values {
		e.leaderCalls <- v
	}
	return e
}

func (e *Election) SetSateChan(values ...bool) *Election {
	for _, v := range values {
		e.c <- v
	}
	return e
}
