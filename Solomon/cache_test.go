package cache

import (
	"fmt"
	"reflect"
	"testing"
	"time"
)

func TestCachePrefetch(t *testing.T) {
	n := 10
	valMap := map[int]struct{}{}
	getter := func(key interface{}) (interface{}, error) {
		var val int

		if i, ok := key.(int); ok {
			if _, ok := valMap[i]; ok {
				val = i + 1
			} else {
				valMap[i] = struct{}{}
				val = i
			}
		}
		return &val, nil
	}
	cache := NewCache(
		"test",
		getter,
		20*time.Millisecond,
		1*time.Millisecond,
		10*time.Millisecond,
		10*time.Millisecond,
		false,
		0,
		n,
	)
	for i := 0; i < n; i++ {
		if val, err := cache.Get(i); err != nil {
			t.Errorf("Cache prefetch err: %v", err)
			return
		} else if v := *(val.(*int)); v != i {
			t.Errorf("Cache prefetch bad value = %d, want = %d ", v, i)
			return
		}
	}
	time.Sleep(18 * time.Millisecond)
	for i := 0; i < n; i++ {
		if val, err := cache.Get(i); err != nil {
			t.Errorf("Cache prefetch err: %v", err)
			return
		} else if v := *(val.(*int)); v != i+1 {
			t.Errorf("Cache prefetch bad value = %d, want = %d ", v, i+1)
			return
		}
	}
	cache.Destroy()
}

func TestCacheStaleServe(t *testing.T) {
	n := 10
	errMap := map[int]struct{}{}
	getter := func(key interface{}) (interface{}, error) {
		var err error
		var val int

		if i, ok := key.(int); ok {
			if _, ok := errMap[i]; ok {
				err = fmt.Errorf("test error")
				val = -1
			} else {
				errMap[i] = struct{}{}
				val = i
			}
		}
		return &val, err
	}
	cache := NewCache(
		"test",
		getter,
		10*time.Microsecond,
		1*time.Microsecond,
		2*time.Second,
		5*time.Second,
		true,
		0,
		n,
	)
	for i := 0; i < n; i++ {
		if _, err := cache.Get(i); err != nil {
			t.Errorf("Cache serve stale err: %v", err)
			return
		}
	}
	// wait for badCacheTime to expire
	time.Sleep(time.Millisecond)
	for i := 0; i < n; i++ {
		val, err := cache.Get(i)
		if err == nil {
			t.Errorf("Cache serve stale no error, want stale error")
			return
		}
		if !err.(*CacheError).IsStale() {
			t.Errorf("Cache serve stale, want stale error, got: %v", err)
			return
		}
		v := *(val.(*int))
		if v != i {
			t.Errorf("Cache serve stale bad value = %d, want = %d ", v, i)
			return
		}
	}
	cache.Destroy()
}

func TestCacheForceUpdate(t *testing.T) {
	n := 10
	accessMap := map[int]struct{}{}
	getter := func(key interface{}) (interface{}, error) {
		var val int

		if i, ok := key.(int); ok {
			if _, ok := accessMap[i]; ok {
				val = -i
			} else {
				accessMap[i] = struct{}{}
				val = i
			}
		}
		return &val, nil
	}
	cache := NewCache(
		"test",
		getter,
		10*time.Second,
		1*time.Second,
		2*time.Second,
		5*time.Second,
		true,
		0,
		n,
	)
	for i := 0; i < n; i++ {
		if _, err := cache.Get(i); err != nil {
			t.Errorf("Cache force update err: %v", err)
			return
		}
	}
	for i := 0; i < n; i++ {
		val, _ := cache.Get(i)
		v := *(val.(*int))
		if v != i {
			t.Errorf("Cache force update first access value = %d, want = %d", v, i)
			return
		}
		val, _ = cache.GetForceUpdate(i)
		v = *(val.(*int))
		if v != -i {
			t.Errorf("Cache force update bad value = %d, want = %d", v, -i)
			return
		}
	}
	cache.Destroy()
}

func TestCacheForceFresh(t *testing.T) {
	n := 10
	accessMap := map[int]struct{}{}
	getter := func(key interface{}) (interface{}, error) {
		var val int

		if i, ok := key.(int); ok {
			if _, ok := accessMap[i]; ok {
				val = -i
			} else {
				accessMap[i] = struct{}{}
				val = i
			}
		}
		return &val, nil
	}
	cache := NewCache(
		"test",
		getter,
		10*time.Second,
		1*time.Second,
		2*time.Second,
		5*time.Second,
		true,
		0,
		n,
	)
	for i := 0; i < n; i++ {
		if _, err := cache.Get(i); err != nil {
			t.Errorf("Cache force fresh err: %v", err)
			return
		}
	}
	for i := 0; i < n; i++ {
		val, _ := cache.Get(i)
		v := *(val.(*int))
		if v != i {
			t.Errorf("Cache force fresh first access value = %d, want = %d", v, i)
			return
		}
	}
	time.Sleep(10 * time.Millisecond)
	minLastUpdate := time.Now().Add(-20 * time.Millisecond)

	for i := 0; i < n; i++ {
		val, _ := cache.GetForceFresh(i, &minLastUpdate)
		v := *(val.(*int))
		if v != i {
			t.Errorf("Cache force fresh bad cached value = %d, want = %d", v, i)
			return
		}
	}

	minLastUpdate = minLastUpdate.Add(15 * time.Millisecond)
	for i := 0; i < n; i++ {
		val, _ := cache.GetForceFresh(i, &minLastUpdate)
		v := *(val.(*int))
		if v != -i {
			t.Errorf("Cache force fresh bad fresh value = %d, want = %d", v, -i)
			return
		}
	}
	cache.Destroy()
}

func TestCache2Q(t *testing.T) {
	n := 10
	checker := func(cache *Cache) string {
		return fmt.Sprintf("keys=%v items=%d EOLGood=%d EOLBad=%d LRU1=%d LRU2=%d\n",
			cache.Keys(),
			len(cache.items),
			cache.listEOLGood.Len(),
			cache.listEOLBad.Len(),
			cache.listLRU1.Len(),
			cache.listLRU2.Len(),
		)
	}
	getter := func(key interface{}) (interface{}, error) {
		var err error
		i, ok := key.(int)
		if ok && i%3 == 0 {
			err = fmt.Errorf("test error")
		}
		return &key, err
	}
	cache := NewCache(
		"test",
		getter,
		10*time.Second,
		1*time.Second,
		2*time.Second,
		5*time.Second,
		true,
		0,
		n,
	)
	for i := 0; i < n; i++ {
		_, _ = cache.Get(i)
	}
	_, _ = cache.Get(1)
	_, _ = cache.Get("a")
	_, _ = cache.Get(3)
	_, _ = cache.Get(3)
	_, _ = cache.Get("b")
	if len(cache.items) != 10 || cache.listEOLGood.Len() != 7 || cache.listEOLBad.Len() != 3 ||
		cache.listLRU1.Len() != 8 || cache.listLRU2.Len() != 2 {

		t.Errorf("Cache 2Q check err: %s", checker(cache))
	}
	cache.Destroy()
}

func TestCacheDumpRestore(t *testing.T) {
	n := 10
	getter := func(key interface{}) (interface{}, error) {
		v := fmt.Sprintf("ValFor=%d", key)
		return &v, nil
	}
	cache := NewCache(
		"test",
		getter,
		10*time.Second,
		1*time.Second,
		2*time.Second,
		5*time.Second,
		true,
		0,
		n,
	)
	for i := 0; i < n; i++ {
		_, _ = cache.Get(i)
	}
	cDump, err := cache.Dump(true)
	if err != nil {
		t.Errorf("Cache dump err: %v", err)
		return
	}
	cn := cache.Len()
	if cn != n {
		t.Errorf("Cache dump err: Len = %d, want %d", cn, n)
		return
	}
	cache.Purge()
	cn = cache.Len()
	if cn != 0 {
		t.Errorf("Cache dump err: Len = %d, want 0", cn)
		return
	}
	if err := cache.Restore(cDump, false, int(0), new(string)); err != nil {
		t.Errorf("Cache restore err: %v", err)
		return
	}
	cn = cache.Len()
	if cn != n {
		t.Errorf("Cache restore err: Len = %d, want %d", cn, n)
		return
	}
	timeNow := time.Now()
	for i := 0; i < n; i++ {
		if !cache.Contains(i) {
			t.Errorf("Cache restore err: cache has no value for %d", i)
			return
		}
		val, err := cache.Peek(i)
		if err != nil {
			t.Errorf("Cache peek err: %v", err)
			return
		}
		if *(val.(*string)) != fmt.Sprintf("ValFor=%d", i) {
			t.Errorf("Cache restore err: val = '%v', want 'ValFor=%d'", *(val.(*string)), i)
			return
		}
		eol := cache.items[i].eol
		timeMax := timeNow.Add(11 * time.Second)
		if eol.Before(timeNow) || eol.After(timeMax) {
			t.Errorf("Cache restore err: eol = '%v', want between '%v' and '%v'", eol, timeNow, timeMax)
			return
		}
	}
	cDump2, err := cache.Dump(true)
	if err != nil {
		t.Errorf("Cache dump x2 err: %v", err)
		return
	}
	cn = cache.Len()
	if cn != n {
		t.Errorf("Cache dump x2 err: Len = %d, want %d", cn, n)
		return
	}
	if !reflect.DeepEqual(cDump, cDump2) {
		t.Errorf("Cache dump x2 err: dump = %v, want %v", string(cDump2), string(cDump))
		return
	}
	cache.Destroy()
}

func TestCacheReqsFilter(t *testing.T) {
	n := 10
	calls := 0
	getter := func(key interface{}) (interface{}, error) {
		calls += 1
		return &key, nil
	}
	cache := NewCache(
		"test",
		getter,
		10*time.Second,
		1*time.Second,
		2*time.Second,
		5*time.Second,
		true,
		0,
		n,
	)
	for i := 0; i < n*10; i++ {
		_, _ = cache.Get(i % n)
	}
	if calls != n {
		t.Errorf("Calls = %d, want %d", calls, n)
	}
	cache.Destroy()
}

func TestCacheReqsQueue(t *testing.T) {
	n := 10
	calls := 0
	getter := func(key interface{}) (interface{}, error) {
		calls += 1
		time.Sleep(10 * time.Millisecond)
		return &key, nil
	}
	cache := NewCache(
		"test",
		getter,
		10*time.Second,
		1*time.Second,
		2*time.Second,
		5*time.Second,
		true,
		0,
		n,
	)
	for i := 0; i < n; i++ {
		_, _ = cache.Get(1)
	}
	if calls != 1 {
		t.Errorf("Calls = %d, want 1", calls)
	}
	cache.Destroy()
}

func BenchmarkCacheGetGetter(b *testing.B) {
	n := 10000
	getter := func(key interface{}) (interface{}, error) {
		return &key, nil
	}
	cache := NewCache(
		"test",
		getter,
		10*time.Second,
		1*time.Second,
		2*time.Second,
		5*time.Second,
		false,
		0,
		n,
	)
	n += 1
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_, _ = cache.Get(i % n)
	}
	b.ReportMetric(float64(cache.Len()), "len")
	b.ReportAllocs()
	cache.Destroy()
}

func BenchmarkCacheGetOnly(b *testing.B) {
	n := 10000
	getter := func(key interface{}) (interface{}, error) {
		return &key, nil
	}
	cache := NewCache(
		"test",
		getter,
		10*time.Second,
		1*time.Second,
		2*time.Second,
		5*time.Second,
		false,
		0,
		n,
	)
	for i := 0; i < n; i++ {
		_, _ = cache.Get(i)
	}
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_, _ = cache.Get(i % n)
	}
	b.ReportMetric(float64(cache.Len()), "len")
	b.ReportAllocs()
	cache.Destroy()
}

func BenchmarkCacheSet(b *testing.B) {
	n := 10000
	getter := func(key interface{}) (interface{}, error) {
		return &key, nil
	}
	cache := NewCache(
		"test",
		getter,
		10*time.Second,
		1*time.Second,
		2*time.Second,
		5*time.Second,
		false,
		0,
		n,
	)

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		v := i % n
		_, _ = cache.Set(i, &v)
	}
	b.ReportMetric(float64(cache.Len()), "len")
	b.ReportAllocs()
	cache.Destroy()
}
