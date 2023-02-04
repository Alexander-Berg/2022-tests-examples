package tests

import (
	"encoding/json"
	"fmt"
	"github.com/YandexClassifieds/goLB"
	"github.com/YandexClassifieds/goLB/consumer"
	"github.com/YandexClassifieds/goLB/producer"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"log"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

var (
	nameF    = "Test %s Thread %d Num %d"
	testName = "ci"
)

func runWithSession(t *testing.T, p producer.Producer, c consumer.Consumer) {
	t.Log("Init producer")
	_, err := p.Init()
	require.NoError(t, err)

	t.Log("Init consumer")
	err = c.Init()
	require.NoError(t, err)

	prefix := uuid.New().String()
	counter := NewCounter()
	go processPrint(t, counter)
	registry := NewRegistry()
	count := 1000
	threads := 20

	log.Printf("TEST: push first messages %d", count)
	for i := 1; i <= count; i++ {
		r := request{
			Num:    prefix + "_" + uuid.New().String(),
			Thread: 0,
			Name:   fmt.Sprintf(nameF, testName, 0, i),
		}
		registry.req(r)
		push(t, counter, p, r)
	}

	go func() {
		log.Print("TEST: start consumer")
		defer log.Print("TEST: end consumer")
		for {
			var b *consumer.Batch
			select {
			case <-c.CloseSign():
				return
			case b = <-c.C():
			}
			if b == nil {
				continue
			}
			for _, d := range b.Data {

				r := &request{}
				err := json.Unmarshal(d.Body, r)
				assert.NoError(t, err)

				d.OnSuccess = func() {
					registry.unReq(*r)
					atomic.AddInt64(counter.consumeDataSuccess, 1)
				}
				d.OnFail = func() { atomic.AddInt64(counter.consumeDataFail, 1) }
			}
			b.OnSuccess = func() { atomic.AddInt64(counter.consumeBatchSuccess, 1) }
			b.OnFail = func(err error) { atomic.AddInt64(counter.consumeBatchFail, 1) }
			c.Commit(b)
		}
	}()

	log.Printf("TEST: push second messages %d", count*threads)

	end := make(chan int)
	for thread := 1; thread <= threads; thread++ {
		go func(thread int) {
			for i := 1; i <= count; i++ {
				r := request{
					Num:    prefix + "_" + uuid.New().String(),
					Thread: thread,
					Name:   fmt.Sprintf(nameF, testName, 0, i),
				}
				registry.req(r)
				push(t, counter, p, r)
			}
			end <- 1
		}(thread)
	}
	log.Printf("TEST: wait pushing")
	for thread := 1; thread <= threads; thread++ {
		<-end
	}
	log.Printf("TEST: slow push")
	//  batchHandler не имеет пуша по tiker и рассчитывает на постоянную нагрузку, поэтому мы подождем
	// 1 секунду и бросим одно сообщения для срабатывания по времени. Это принимо только к батчам, но полезно будет и
	// для остальных как часть проверки
	time.Sleep(1 * time.Second)
	push(t, counter, p, request{
		Num:    prefix + "_" + uuid.New().String(),
		Thread: threads + 1,
		Name:   fmt.Sprintf(nameF, testName, 0, count*threads+1),
	})
	log.Printf("TEST: assert")
	now := time.Now()
	for !registry.check() && time.Now().Sub(now).Seconds() <= 45 {

		time.Sleep(3 * time.Second)
	}

	p.Close()
	c.Close()
	onCloseP(t, p)
	onCloseC(t, c)
	assert.True(t, registry.check())
	counter.print()
}

func processPrint(t *testing.T, counter *Counter) {
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()
	timer := time.NewTimer(180 * time.Second)
	defer timer.Stop()
	for {
		select {
		case <-timer.C:
			return
		case <-ticker.C:
		}
		counter.print()
	}
}

func push(t *testing.T, counter *Counter, p producer.Producer, r request) {
	bytes, err := json.Marshal(r)
	assert.NoError(t, err)

	p.Push(&goLB.Data{
		Body:      bytes,
		OnSuccess: func() { atomic.AddInt64(counter.pushSuccess, 1) },
		OnFail:    func() { atomic.AddInt64(counter.pushFail, 1) },
	})
}

func onCloseP(t *testing.T, p producer.Producer) {
	<-p.CloseSign()
	backup := p.Backup()
	log.Printf("Miss producer messages : %d", len(backup))
}

func onCloseC(t *testing.T, c consumer.Consumer) {
	<-c.CloseSign()
	backup := c.Backup()
	log.Printf("Miss consumer messages : %d", len(backup))
}

type request struct {
	Num    string `json:"num"`
	Thread int    `json:"thread"`
	Name   string `json:"name"`
}

type Counter struct {
	pushSuccess         *int64
	pushFail            *int64
	consumeDataSuccess  *int64
	consumeDataFail     *int64
	consumeBatchSuccess *int64
	consumeBatchFail    *int64
}

func NewCounter() *Counter {
	return &Counter{
		pushSuccess:         NewIntPointer(),
		pushFail:            NewIntPointer(),
		consumeDataSuccess:  NewIntPointer(),
		consumeDataFail:     NewIntPointer(),
		consumeBatchSuccess: NewIntPointer(),
		consumeBatchFail:    NewIntPointer(),
	}
}

func NewIntPointer() *int64 {
	var result int64
	return &result
}

func (c *Counter) print() {
	log.Printf("Counter[pushSuccess=%d; pushFail=%d; consumeDataSuccess=%d; "+
		"consumeDataFail=%d; consumeBatchSuccess=%d; consumeBatchFail=%d;]",
		atomic.LoadInt64(c.pushSuccess),
		atomic.LoadInt64(c.pushFail),
		atomic.LoadInt64(c.consumeDataSuccess),
		atomic.LoadInt64(c.consumeDataFail),
		atomic.LoadInt64(c.consumeBatchSuccess),
		atomic.LoadInt64(c.consumeBatchFail))
}

type Registry struct {
	point map[string]request
	rw    sync.RWMutex
}

func NewRegistry() *Registry {
	return &Registry{
		point: map[string]request{},
		rw:    sync.RWMutex{},
	}
}

func (r *Registry) req(req request) {
	r.rw.Lock()
	defer r.rw.Unlock()
	r.point[req.Num] = req
}

func (r *Registry) unReq(req request) {
	r.rw.Lock()
	defer r.rw.Unlock()
	_, ok := r.point[req.Num]
	if !ok {
		fmt.Printf("miss message by %v", req)
	}
	delete(r.point, req.Num)
}

func (r *Registry) check() bool {
	var fail []request
	r.rw.Lock()
	defer r.rw.Unlock()
	for _, v := range r.point {
		fail = append(fail, v)
	}
	log.Printf("Not process number: %v", fail)
	return len(fail) == 0
}
