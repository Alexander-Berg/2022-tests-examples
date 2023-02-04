package goLB

import (
	"log"
	"testing"
	"time"
)

func TestMassSend(t *testing.T) {

	buffer := NewBuffer()
	count := 1000001

	for i := 1; i < count; i++ {
		j := i
		buffer.Push(&j)
	}

	for i := 1; i < count; i++ {
		v := buffer.Pop().(*int)
		if *v != i {
			log.Panicf("Miss %d vs %d", i, *v)
		}
	}
}

func TestMix(t *testing.T) {

	buffer := NewBuffer()
	count := 100001

	lastWrite := 0
	go func() {
		for i := 1; i < count; i++ {
			j := i
			lastWrite = j
			buffer.Push(&j)
		}
	}()

	lastSuccess := 0
	go func() {
		for i := 1; i < count; i++ {
			v := buffer.Pop()
			lw := lastWrite
			if v == nil {
				log.Printf("repeat, last success %d last write %d", lastSuccess, lw)
				i--
				continue
			}
			vInt := *v.(*int)
			if vInt != i {
				log.Panicf("Miss %d vs %d, last success %d last write %d", i, vInt, lastSuccess, lw)
			}
			lastSuccess = i
		}
	}()

	time.Sleep(1 * time.Second)
}
