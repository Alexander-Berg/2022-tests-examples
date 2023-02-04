package csv

import (
	"bufio"
	"bytes"
	"encoding"
	"encoding/hex"
	"fmt"
	"io"
	"reflect"
	"strconv"
	"strings"
)

const (
	Separator = ','
	Wrapper   = "\""
)

type DecodeError struct {
	lineNo  int
	fieldNo int
	hint    string
	reason  error
}

func (e *DecodeError) Error() string {
	if e.fieldNo != 0 {
		return fmt.Sprintf("csv: line %d field %d (%s): %v", e.lineNo, e.fieldNo, e.hint, e.reason)
	} else if e.reason == nil {
		return fmt.Sprintf("csv: line %d: %s", e.lineNo, e.hint)
	}
	return fmt.Sprintf("csv: line %d: %v", e.lineNo, e.reason)
}

type Unmarshaler interface {
	UnmarshalCSV(header, values []string) error
}

type Decoder struct {
	s          *bufio.Scanner
	sep        rune
	lineNo     int
	headerKeys []string
}

func NewDecoder(r io.Reader) *Decoder {
	return &Decoder{
		s:          bufio.NewScanner(r),
		sep:        Separator,
		lineNo:     0,
		headerKeys: make([]string, 0),
	}
}

func (d *Decoder) Buffer(buf []byte) *Decoder {
	d.s.Buffer(buf, cap(buf))
	return d
}

func Unmarshal(data []byte, target interface{}) error {
	decoder := NewDecoder(bytes.NewReader(data))
	return decoder.Decode(target)
}

func (d *Decoder) ReadLine() (string, error) {
	for d.s.Scan() {
		line := d.s.Text()
		d.lineNo++
		if len(line) == 0 {
			continue
		}
		return line, nil
	}
	if err := d.s.Err(); err != nil {
		return "", fmt.Errorf("csv: read failed: %v", err)
	}
	return "", nil
}

func (d *Decoder) Decode(v interface{}) error {
	val := reflect.ValueOf(v)
	if val.Kind() != reflect.Ptr {
		return fmt.Errorf("csv: non-pointer passed to Unmarshal")
	}

	val = reflect.Indirect(val)
	if val.Kind() != reflect.Slice {
		return fmt.Errorf("csv: non-slice passed to Unmarshal")
	}

	for d.s.Scan() {
		line := d.s.Text()
		d.lineNo++
		if len(line) == 0 {
			continue
		}

		if len(d.headerKeys) == 0 {
			if _, err := d.DecodeHeader(line); err != nil {
				return err
			}
			continue
		}

		e := reflect.New(val.Type().Elem())
		if err := d.unmarshal(e.Elem(), line); err != nil {
			return err
		}

		val.Set(reflect.Append(val, e.Elem()))
	}
	if err := d.s.Err(); err != nil {
		return fmt.Errorf("csv: read failed: %v", err)
	}

	return nil
}

func (d *Decoder) DecodeHeader(line string) ([]string, error) {
	d.headerKeys = strings.Split(line, string(d.sep))
	if len(d.headerKeys) == 0 {
		return nil, fmt.Errorf("csv: empty header")
	}
	for i, v := range d.headerKeys {
		d.headerKeys[i] = strings.TrimSpace(v)
	}
	return d.headerKeys, nil
}

func (d *Decoder) DecodeRecord(v interface{}, line string) error {
	val := reflect.ValueOf(v)
	if val.Kind() != reflect.Ptr {
		return fmt.Errorf("csv: non-pointer passed to DecodeRecord")
	}
	return d.unmarshal(val, line)
}

func (d *Decoder) unmarshal(val reflect.Value, line string) error {
	tokens := strings.Split(line, string(d.sep))

	combined := make([]string, 0, len(tokens))
	var merged string
	for _, v := range tokens {
		switch true {
		case len(v) == 1 && strings.HasPrefix(v, Wrapper):
			if merged == "" {
				merged += string(d.sep)
			} else {
				merged += string(d.sep)
				combined = append(combined, merged)
				merged = ""
			}
		case len(v) >= 2 && strings.HasPrefix(v, Wrapper) && strings.HasSuffix(v, Wrapper):
			combined = append(combined, v[1:])
			merged = ""
		case strings.HasPrefix(v, Wrapper):
			merged = v[1:]
		case strings.HasSuffix(v, Wrapper):
			merged = strings.Join([]string{merged, v[:len(v)-1]}, string(d.sep))
			combined = append(combined, merged)
			merged = ""
		default:
			if merged != "" {
				merged = strings.Join([]string{merged, v}, string(d.sep))
			} else {
				combined = append(combined, v)
			}
		}
	}
	tokens = combined

	if len(tokens) != len(d.headerKeys) {
		return &DecodeError{d.lineNo, 0, "number of fields does not match header", nil}
	}

	val = derefValue(val)

	if val.CanInterface() && val.Type().Implements(unmarshalerType) {
		// This is an unmarshaler with a non-pointer receiver,
		// so it's likely to be incorrect, but we do what we're told.
		return val.Interface().(Unmarshaler).UnmarshalCSV(d.headerKeys, tokens)
	}

	if val.CanAddr() {
		pv := val.Addr()
		if pv.CanInterface() && pv.Type().Implements(unmarshalerType) {
			return pv.Interface().(Unmarshaler).UnmarshalCSV(d.headerKeys, tokens)
		}
	}

	// map struct fields
	for i, fName := range d.headerKeys {
		tokens[i] = strings.TrimSpace(tokens[i])
		// remove double quotes
		tokens[i] = strings.Replace(tokens[i], "\"\"", "\"", -1)

		// handle maps
		if val.Kind() == reflect.Map {
			val.SetMapIndex(reflect.ValueOf(fName), reflect.ValueOf(tokens[i]))
			continue
		}

		_, f := d.findStructField(val, fName)
		if !f.IsValid() {
			return &DecodeError{d.lineNo, i + 1, fName, fmt.Errorf("field not found")}
		}

		// try text unmarshalers first
		if f.CanInterface() && f.Type().Implements(textUnmarshalerType) {
			if err := f.Interface().(encoding.TextUnmarshaler).UnmarshalText([]byte(tokens[i])); err != nil {
				return &DecodeError{d.lineNo, i + 1, fName, err}
			}
			continue
		}

		if f.CanAddr() {
			pv := f.Addr()
			if pv.CanInterface() && pv.Type().Implements(textUnmarshalerType) {
				if err := pv.Interface().(encoding.TextUnmarshaler).UnmarshalText([]byte(tokens[i])); err != nil {
					return &DecodeError{d.lineNo, i + 1, fName, err}
				}
				continue
			}
		}

		// otherwise set simple value directly
		if err := setValue(f, tokens[i], fName); err != nil {
			return &DecodeError{d.lineNo, i + 1, fName, err}
		}
	}

	return nil
}

func (d *Decoder) findStructField(val reflect.Value, name string) (*fieldInfo, reflect.Value) {
	typ := val.Type()
	tinfo, err := getTypeInfo(typ)
	if err != nil {
		return nil, reflect.Value{}
	}

	var finfo *fieldInfo
	index := -1
	// pick the correct field based on name and flags
	for i, v := range tinfo.fields {
		// save `any` field in case
		if v.flags&fAny > 0 {
			index = i
		}

		// field name must match
		if v.name != name {
			continue
		}

		finfo = &v
		break
	}

	if finfo == nil && index >= 0 {
		finfo = &tinfo.fields[index]
	}

	// nothing found
	if finfo == nil {
		return nil, reflect.Value{}
	}

	// allocate memory for pointer values in structs
	v := finfo.value(val)
	if v.Type().Kind() == reflect.Ptr && v.IsNil() && v.CanSet() {
		v.Set(reflect.New(v.Type().Elem()))
	}

	return finfo, v
}

func setValue(dst reflect.Value, src, fName string) error {
	if src == "" {
		return nil
	}

	dst0 := dst
	if dst.Kind() == reflect.Ptr {
		if dst.IsNil() {
			dst.Set(reflect.New(dst.Type().Elem()))
		}
		dst = dst.Elem()
	}

	switch dst.Kind() {
	case reflect.Map:
		// map must have map[string]string signature or map value
		// must be an encoding.TextUnmarshaler or a simple value
		t := dst.Type()
		if dst.IsNil() {
			dst.Set(reflect.MakeMap(t))
		}
		switch t.Key().Kind() {
		case reflect.String:
		default:
			return fmt.Errorf("map key type must be string")
		}
		switch t.Elem().Kind() {
		case reflect.String:
			dst.SetMapIndex(reflect.ValueOf(fName), reflect.ValueOf(src).Convert(t.Elem()))
		default:
			// create new map entry and contents if it's pointer type
			val := reflect.New(t.Elem()).Elem()
			if val.Type().Kind() == reflect.Ptr && val.IsNil() && val.CanSet() {
				val.Set(reflect.New(val.Type().Elem()))
			}
			if val.CanInterface() && val.Type().Implements(textUnmarshalerType) {
				if err := val.Interface().(encoding.TextUnmarshaler).UnmarshalText([]byte(src)); err != nil {
					return err
				}
			} else if val.CanAddr() {
				pv := val.Addr()
				if pv.CanInterface() && pv.Type().Implements(textUnmarshalerType) {
					if err := pv.Interface().(encoding.TextUnmarshaler).UnmarshalText([]byte(src)); err != nil {
						return err
					}
				}
			} else {
				if err := setValue(val, src, fName); err != nil {
					return err
				}
			}
			dst.SetMapIndex(reflect.ValueOf(fName), val)
		}

	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64:
		i, err := strconv.ParseInt(src, 10, dst.Type().Bits())
		if err != nil {
			return err
		}
		dst.SetInt(i)
	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64, reflect.Uintptr:
		i, err := strconv.ParseUint(src, 10, dst.Type().Bits())
		if err != nil {
			return err
		}
		dst.SetUint(i)
	case reflect.Float32, reflect.Float64:
		i, err := strconv.ParseFloat(src, dst.Type().Bits())
		if err != nil {
			return err
		}
		dst.SetFloat(i)
	case reflect.Bool:
		i, err := strconv.ParseBool(strings.TrimSpace(src))
		if err != nil {
			return err
		}
		dst.SetBool(i)
	case reflect.String:
		dst.SetString(strings.TrimSpace(src))
	case reflect.Slice:
		// make sure it's a byte slice
		if dst.Type().Elem().Kind() == reflect.Uint8 {
			if buf, err := hex.DecodeString(src); err == nil {
				dst.SetBytes(buf)
			} else {
				dst.SetBytes([]byte(src))
			}
		}
	default:
		return fmt.Errorf("no method for unmarshaling type %s", dst0.Type().String())
	}
	return nil
}
