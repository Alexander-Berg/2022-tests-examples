import { resolve } from '../../../../../common/xpromise-support'
import { int64 } from '../../../../../common/ys'
import { ArrayJSONItem } from '../../../../common/code/json/json-types'
import { getVoid, Result } from '../../../../common/code/result/result'
import {
  JSONItemFromJSONString,
  JSONItemToJSONString,
  transformJSONItemToSerializable,
} from '../../../../common/__tests__/__helpers__/json-helpers'
import { File } from '../../../../common/code/file-system/file'
import { Encoding } from '../../../../common/code/file-system/file-system-types'
import { TaskType } from '../../../code/service/task'
import {
  DeserializingError,
  TaskSerializer,
  TaskSerializerError,
  UnsupportedTypeError,
  BasicError,
} from '../../../code/service/task-serializer'
import { MockHighPrecisionTimer } from '../../__helpers__/mock-patches'
import { MockModels } from '../../__helpers__/models'
import { rejected } from '../../__helpers__/test-failure'
import { TestTask } from '../../__helpers__/test-task'
import { MockFileSystem, MockJSONSerializer } from '../../../../common/__tests__/__helpers__/mock-patches'

describe(TaskSerializer, () => {
  describe('serializing', () => {
    it('should serialize tasks', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(testTask.serialize())

      const deleteMock = jest.fn().mockReturnValueOnce(resolve(getVoid())).mockReturnValueOnce(resolve(getVoid()))
      const ensureFolderExistsMock = jest.fn().mockReturnValueOnce(resolve(getVoid()))
      const existsMock = jest.fn().mockReturnValueOnce(resolve(false))
      const writeAsStringMock = jest.fn().mockReturnValueOnce(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValueOnce(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        ensureFolderExists: ensureFolderExistsMock,
        exists: existsMock,
        writeAsString: writeAsStringMock,
        readAsString: readAsStringMock,
      })
      const serializeMock = jest.fn().mockReturnValue(new Result(contents, null))
      const deserializeMock = jest.fn().mockImplementation((json) => new Result(JSONItemFromJSONString(json), null))
      const jsonSerializer = MockJSONSerializer({
        serialize: serializeMock,
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(() => int64(12345)),
        jsonSerializer,
        models,
      )
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer').mockReturnValue(() => resolve(testTask))
      serializer.serialize(testTask).then((res) => {
        expect(ensureFolderExistsMock).toBeCalledTimes(1)
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(existsMock).toBeCalledTimes(1)
        expect(existsMock).toBeCalledWith('commands/12345_1_2')
        expect(writeAsStringMock).toBeCalledWith('commands/12345_1_2', contents, Encoding.Utf8, false)
        expect(readAsStringMock).toBeCalledWith('commands/12345_1_2', Encoding.Utf8)
        expect(deserializeMock).toBeCalledWith(contents)
        expect(tasksMaterializerSpy).toBeCalledWith(testTask.getType())
        expect(deleteMock).not.toBeCalled()
        expect(res).toStrictEqual(new File('commands/12345_1_2'))
        done()
      })
    })
    it('should attempt more if the output file already exists', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(testTask.serialize())

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const ensureFolderExistsMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const existsMock = jest
        .fn()
        .mockReturnValueOnce(resolve(true))
        .mockReturnValueOnce(resolve(true))
        .mockReturnValueOnce(resolve(true))
        .mockReturnValueOnce(resolve(false))
      const writeAsStringMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        ensureFolderExists: ensureFolderExistsMock,
        exists: existsMock,
        writeAsString: writeAsStringMock,
        readAsString: readAsStringMock,
      })
      const serializeMock = jest.fn().mockReturnValue(new Result(contents, null))
      const deserializeMock = jest
        .fn()
        .mockImplementation((jsonItem) => new Result(JSONItemFromJSONString(jsonItem), null))
      const jsonSerializer = MockJSONSerializer({
        serialize: serializeMock,
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(() => int64(12345)),
        jsonSerializer,
        models,
      )
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer').mockReturnValue(() => resolve(testTask))
      serializer.serialize(testTask).then((res) => {
        expect(ensureFolderExistsMock).toBeCalledTimes(4)
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(existsMock).toBeCalledTimes(4)
        expect(existsMock).toBeCalledWith('commands/12345_1_2')
        expect(existsMock).toBeCalledWith('commands/12345_2_2')
        expect(existsMock).toBeCalledWith('commands/12345_3_2')
        expect(existsMock).toBeCalledWith('commands/12345_4_2')
        expect(writeAsStringMock).toBeCalledWith('commands/12345_4_2', contents, Encoding.Utf8, false)
        expect(readAsStringMock).toBeCalledWith('commands/12345_4_2', Encoding.Utf8)
        expect(deserializeMock).toBeCalledWith(contents)
        expect(tasksMaterializerSpy).toBeCalledWith(testTask.getType())
        expect(deleteMock).not.toBeCalled()
        expect(res).toStrictEqual(new File('commands/12345_4_2'))
        done()
      })
    })
    it('should fail if all attempts fail', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(testTask.serialize())

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const ensureFolderExistsMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const existsMock = jest.fn().mockReturnValue(resolve(true))
      const writeAsStringMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        ensureFolderExists: ensureFolderExistsMock,
        exists: existsMock,
        writeAsString: writeAsStringMock,
        readAsString: readAsStringMock,
      })
      const serializeMock = jest.fn().mockReturnValue(new Result(contents, null))
      const deserializeMock = jest
        .fn()
        .mockImplementation((jsonItem) => new Result(JSONItemFromJSONString(jsonItem), null))
      const jsonSerializer = MockJSONSerializer({
        serialize: serializeMock,
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(() => int64(12345)),
        jsonSerializer,
        models,
      )
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer')
      serializer.serialize(testTask).failed((err) => {
        expect(ensureFolderExistsMock).toBeCalledTimes(5)
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(existsMock).toBeCalledTimes(5)
        expect(existsMock).toBeCalledWith('commands/12345_1_2')
        expect(existsMock).toBeCalledWith('commands/12345_2_2')
        expect(existsMock).toBeCalledWith('commands/12345_3_2')
        expect(existsMock).toBeCalledWith('commands/12345_4_2')
        expect(existsMock).toBeCalledWith('commands/12345_5_2')
        expect(writeAsStringMock).not.toBeCalled()
        expect(readAsStringMock).not.toBeCalledWith()
        expect(deserializeMock).not.toBeCalledWith()
        expect(tasksMaterializerSpy).not.toBeCalled()
        expect(deleteMock).not.toBeCalled()
        expect(err.message).toBe(`Failed to serialize task ${TaskType.delete}`)
        done()
      })
    })
    it('should delete the file if error occured during file write', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(testTask.serialize())

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const ensureFolderExistsMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const existsMock = jest.fn().mockReturnValue(resolve(false))
      const writeAsStringMock = jest.fn().mockReturnValueOnce(rejected('FAILED')).mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        ensureFolderExists: ensureFolderExistsMock,
        exists: existsMock,
        writeAsString: writeAsStringMock,
        readAsString: readAsStringMock,
      })
      const serializeMock = jest.fn().mockReturnValue(new Result(contents, null))
      const deserializeMock = jest.fn().mockImplementation((jsonItem) => {
        const res = JSONItemFromJSONString(jsonItem)
        return res !== null ? new Result(res, null) : new Result(null, new Error('Unable to parse JSONItem'))
      })

      const jsonSerializer = MockJSONSerializer({
        serialize: serializeMock,
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(() => int64(12345)),
        jsonSerializer,
        models,
      )
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer').mockReturnValue(() => resolve(testTask))
      serializer.serialize(testTask).then((res) => {
        expect(ensureFolderExistsMock).toBeCalledTimes(2)
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(existsMock).toBeCalledTimes(2)
        expect(existsMock).toBeCalledWith('commands/12345_1_2')
        expect(existsMock).toBeCalledWith('commands/12345_2_2')
        expect(writeAsStringMock).toBeCalledTimes(2)
        expect(writeAsStringMock).toBeCalledWith('commands/12345_1_2', contents, Encoding.Utf8, false)
        expect(writeAsStringMock).toBeCalledWith('commands/12345_2_2', contents, Encoding.Utf8, false)
        expect(readAsStringMock).toBeCalledTimes(1)
        expect(readAsStringMock).toBeCalledWith('commands/12345_2_2', Encoding.Utf8)
        expect(deserializeMock).toBeCalledTimes(1)
        expect(deserializeMock).toBeCalledWith(contents)
        expect(tasksMaterializerSpy).toBeCalledWith(testTask.getType())
        expect(deleteMock).toBeCalledTimes(1)
        expect(deleteMock).toBeCalledWith('commands/12345_1_2', true)
        expect(res).toStrictEqual(new File('commands/12345_2_2'))
        done()
      })
    })
    it('should stop attempts if serializer failed serializing', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(testTask.serialize())

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const ensureFolderExistsMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const existsMock = jest.fn().mockReturnValue(resolve(false))
      const writeAsStringMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        ensureFolderExists: ensureFolderExistsMock,
        exists: existsMock,
        writeAsString: writeAsStringMock,
        readAsString: readAsStringMock,
      })
      const serializeMock = jest
        .fn()
        .mockReturnValueOnce(new Result(null, new Error('FAILED')))
        .mockReturnValue(new Result(contents, null))
      const deserializeMock = jest.fn().mockImplementation((jsonItem, materializer) => {
        const res = JSONItemFromJSONString(jsonItem)
        return res !== null ? materializer(res) : new Result(null, new Error('Unable to parse JSONItem'))
      })

      const jsonSerializer = MockJSONSerializer({
        serialize: serializeMock,
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(() => int64(12345)),
        jsonSerializer,
        models,
      )
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer')
      serializer.serialize(testTask).failed((err) => {
        expect(ensureFolderExistsMock).toBeCalledTimes(1)
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(existsMock).toBeCalledTimes(1)
        expect(existsMock).toBeCalledWith('commands/12345_1_2')
        expect(writeAsStringMock).not.toBeCalled()
        expect(readAsStringMock).not.toBeCalled()
        expect(deserializeMock).not.toBeCalled()
        expect(tasksMaterializerSpy).not.toBeCalled()
        expect(deleteMock).not.toBeCalled()
        expect(err.message).toBe('FAILED')
        done()
      })
    })
    it('should fail if command file has unknown type', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(testTask.serialize())

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const ensureFolderExistsMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const existsMock = jest.fn().mockReturnValue(resolve(false))
      const writeAsStringMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        ensureFolderExists: ensureFolderExistsMock,
        exists: existsMock,
        writeAsString: writeAsStringMock,
        readAsString: readAsStringMock,
      })
      const serializeMock = jest.fn().mockReturnValue(new Result(contents, null))
      const deserializeMock = jest.fn().mockImplementation((jsonItem, materializer) => {
        const res = JSONItemFromJSONString(jsonItem)
        return res !== null ? materializer(res) : new Result(null, new Error('Unable to parse JSONItem'))
      })

      const jsonSerializer = MockJSONSerializer({
        serialize: serializeMock,
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(() => int64(12345)),
        jsonSerializer,
        models,
      )
      const parseTaskSpy = jest.spyOn(serializer as any, 'parseTaskTypeFromName').mockReturnValue(null)
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer')
      serializer.serialize(testTask).failed((err) => {
        expect(ensureFolderExistsMock).toBeCalledTimes(1)
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(existsMock).toBeCalledTimes(1)
        expect(existsMock).toBeCalledWith('commands/12345_1_2')
        expect(writeAsStringMock).toBeCalledTimes(1)
        expect(writeAsStringMock).toBeCalledWith('commands/12345_1_2', contents, Encoding.Utf8, false)
        expect(readAsStringMock).toBeCalledTimes(1)
        expect(readAsStringMock).toBeCalledWith('commands/12345_1_2', Encoding.Utf8)
        expect(deserializeMock).not.toBeCalled()
        expect(tasksMaterializerSpy).not.toBeCalled()
        expect(deleteMock).toBeCalledTimes(1)
        expect(deleteMock).toBeCalledWith('commands/12345_1_2', true)
        expect(err).toBeInstanceOf(UnsupportedTypeError)
        expect(err).toStrictEqual(
          new UnsupportedTypeError("Unknown type of command file 'commands/12345_1_2'", 'commands/12345_1_2'),
        )
        parseTaskSpy.mockRestore()
        done()
      })
    })
    it('should fail if task deserialization fails', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(testTask.serialize())

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const ensureFolderExistsMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const existsMock = jest.fn().mockReturnValue(resolve(false))
      const writeAsStringMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        ensureFolderExists: ensureFolderExistsMock,
        exists: existsMock,
        writeAsString: writeAsStringMock,
        readAsString: readAsStringMock,
      })
      const serializeMock = jest.fn().mockReturnValue(new Result(contents, null))
      const deserializeMock = jest.fn().mockReturnValue(new Result(resolve(null), { message: 'FAILED' }))
      const jsonSerializer = MockJSONSerializer({
        serialize: serializeMock,
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(() => int64(12345)),
        jsonSerializer,
        models,
      )
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer')
      serializer.serialize(testTask).failed((err) => {
        expect(ensureFolderExistsMock).toBeCalledTimes(1)
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(existsMock).toBeCalledTimes(1)
        expect(existsMock).toBeCalledWith('commands/12345_1_2')
        expect(writeAsStringMock).toBeCalledTimes(1)
        expect(writeAsStringMock).toBeCalledWith('commands/12345_1_2', contents, Encoding.Utf8, false)
        expect(readAsStringMock).toBeCalledTimes(1)
        expect(readAsStringMock).toBeCalledWith('commands/12345_1_2', Encoding.Utf8)
        expect(deserializeMock).toBeCalledTimes(1)
        expect(deserializeMock).toBeCalledWith(contents)
        expect(deleteMock).toBeCalledTimes(1)
        expect(deleteMock).toBeCalledWith('commands/12345_1_2', true)
        expect(tasksMaterializerSpy).not.toBeCalled()
        expect(err).toBeInstanceOf(DeserializingError)
        expect(err).toStrictEqual(
          new DeserializingError(
            "Task deserialization error from file: 'commands/12345_1_2'. Error: FAILED",
            'commands/12345_1_2',
            { message: 'FAILED' },
            null,
          ),
        )
        done()
      })
    })
    it('should fail if task is of unknown type', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(testTask.serialize())

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const ensureFolderExistsMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const existsMock = jest.fn().mockReturnValue(resolve(false))
      const writeAsStringMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        ensureFolderExists: ensureFolderExistsMock,
        exists: existsMock,
        writeAsString: writeAsStringMock,
        readAsString: readAsStringMock,
      })
      const serializeMock = jest.fn().mockReturnValue(new Result(contents, null))
      const deserializeMock = jest.fn().mockImplementation((jsonItem) => {
        const res = JSONItemFromJSONString(jsonItem)
        return res !== null ? new Result(res, null) : new Result(resolve(null), new Error('Unable to parse JSONItem'))
      })

      const jsonSerializer = MockJSONSerializer({
        serialize: serializeMock,
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer(
        'commands',
        fs,
        MockHighPrecisionTimer(() => int64(12345)),
        jsonSerializer,
        models,
      )
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer').mockReturnValue(null)
      serializer.serialize(testTask).failed((err) => {
        expect(ensureFolderExistsMock).toBeCalledTimes(1)
        expect(ensureFolderExistsMock).toBeCalledWith('commands')
        expect(existsMock).toBeCalledTimes(1)
        expect(existsMock).toBeCalledWith('commands/12345_1_2')
        expect(writeAsStringMock).toBeCalledTimes(1)
        expect(writeAsStringMock).toBeCalledWith('commands/12345_1_2', contents, Encoding.Utf8, false)
        expect(readAsStringMock).toBeCalledTimes(1)
        expect(readAsStringMock).toBeCalledWith('commands/12345_1_2', Encoding.Utf8)
        expect(deserializeMock).toBeCalledTimes(1)
        expect(deserializeMock).toBeCalledWith(contents)
        expect(deleteMock).toBeCalledTimes(1)
        expect(deleteMock).toBeCalledWith('commands/12345_1_2', true)
        expect(tasksMaterializerSpy).toBeCalledWith(testTask.getType())
        expect(err).toBeInstanceOf(TaskSerializerError)
        expect(err).toStrictEqual(
          new TaskSerializerError(`Unknown type of Task ${testTask.getType()}`, testTask.getType()),
        )
        done()
      })
    })
  })
  describe('deserializing', () => {
    it('should deserialize tasks', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(testTask.serialize())

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        readAsString: readAsStringMock,
      })
      const deserializeMock = jest
        .fn()
        .mockImplementation((jsonItem) => new Result(JSONItemFromJSONString(jsonItem), null))
      const jsonSerializer = MockJSONSerializer({
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer('commands', fs, MockHighPrecisionTimer(), jsonSerializer, models)
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer').mockReturnValue(() => resolve(testTask))
      serializer.retriableReadTaskFromFile('12345_1_2').then((res) => {
        expect(readAsStringMock).toBeCalledWith('12345_1_2', Encoding.Utf8)
        expect(deserializeMock).toBeCalledWith(contents)
        expect(tasksMaterializerSpy).toBeCalledWith(testTask.getType())
        expect(deleteMock).not.toBeCalled()
        expect(res).toBe(testTask)
        done()
      })
    })
    it('should try several times deserializing if errors', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(testTask.serialize())

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest
        .fn()
        .mockReturnValueOnce(rejected('FAILED 1'))
        .mockReturnValueOnce(rejected('FAILED 2'))
        .mockReturnValueOnce(rejected('FAILED 3'))
        .mockReturnValueOnce(rejected('FAILED 4'))
        .mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        readAsString: readAsStringMock,
      })
      const deserializeMock = jest
        .fn()
        .mockImplementation((jsonItem) => new Result(JSONItemFromJSONString(jsonItem), null))
      const jsonSerializer = MockJSONSerializer({
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer('commands', fs, MockHighPrecisionTimer(), jsonSerializer, models)
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer').mockReturnValue(() => resolve(testTask))
      serializer.retriableReadTaskFromFile('12345_1_2').then((res) => {
        expect(readAsStringMock).toBeCalledTimes(5)
        expect(readAsStringMock).toBeCalledWith('12345_1_2', Encoding.Utf8)
        expect(readAsStringMock).toBeCalledWith('12345_1_2', Encoding.Utf8)
        expect(readAsStringMock).toBeCalledWith('12345_1_2', Encoding.Utf8)
        expect(readAsStringMock).toBeCalledWith('12345_1_2', Encoding.Utf8)
        expect(readAsStringMock).toBeCalledWith('12345_1_2', Encoding.Utf8)
        expect(deserializeMock).toBeCalledWith(contents)
        expect(tasksMaterializerSpy).toBeCalledWith(testTask.getType())
        expect(deleteMock).not.toBeCalled()
        expect(res).toBe(testTask)
        done()
      })
    })
    it('should fail after certain number of attempts', (done) => {
      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(rejected('FAILED'))
      const fs = MockFileSystem({
        delete: deleteMock,
        readAsString: readAsStringMock,
      })
      const deserializeMock = jest
        .fn()
        .mockImplementation((jsonItem) => new Result(JSONItemFromJSONString(jsonItem), null))
      const jsonSerializer = MockJSONSerializer({
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer('commands', fs, MockHighPrecisionTimer(), jsonSerializer, MockModels())
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer')
      serializer.retriableReadTaskFromFile('12345_1_2').failed((err) => {
        expect(readAsStringMock).toBeCalledTimes(5)
        expect(readAsStringMock).toBeCalledWith('12345_1_2', Encoding.Utf8)
        expect(readAsStringMock).toBeCalledWith('12345_1_2', Encoding.Utf8)
        expect(readAsStringMock).toBeCalledWith('12345_1_2', Encoding.Utf8)
        expect(readAsStringMock).toBeCalledWith('12345_1_2', Encoding.Utf8)
        expect(readAsStringMock).toBeCalledWith('12345_1_2', Encoding.Utf8)
        expect(deserializeMock).not.toBeCalled()
        expect(tasksMaterializerSpy).not.toBeCalled()
        expect(deleteMock).toBeCalledWith('12345_1_2', true)
        expect(err.message).toBe("Failed to deserialize task from file '12345_1_2'")
        done()
      })
    })
    it('should not try to deserialize if unknown type of task', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(testTask.serialize())

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        readAsString: readAsStringMock,
      })
      const deserializeMock = jest
        .fn()
        .mockImplementation((jsonItem) => new Result(JSONItemFromJSONString(jsonItem), null))
      const jsonSerializer = MockJSONSerializer({
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer('commands', fs, MockHighPrecisionTimer(), jsonSerializer, models)
      const parseTaskTypeFromNameSpy = jest.spyOn(serializer as any, 'parseTaskTypeFromName').mockReturnValue(null)
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer')
      serializer.retriableReadTaskFromFile('12345_1_2').failed((err) => {
        expect(readAsStringMock).toBeCalledTimes(1)
        expect(deserializeMock).not.toBeCalled()
        expect(tasksMaterializerSpy).not.toBeCalled()
        expect(deleteMock).toBeCalledWith('12345_1_2', true)
        expect(err).toBeInstanceOf(UnsupportedTypeError)
        expect(err).toStrictEqual(new UnsupportedTypeError("Unknown type of command file '12345_1_2'", '12345_1_2'))
        parseTaskTypeFromNameSpy.mockRestore()
        done()
      })
    })
    it('should not try to deserialize if deserialization of JSON failed', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(testTask.serialize())

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        readAsString: readAsStringMock,
      })
      const deserializeMock = jest.fn().mockReturnValue(new Result(resolve(null), { message: 'FAILED' }))
      const jsonSerializer = MockJSONSerializer({
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer('commands', fs, MockHighPrecisionTimer(), jsonSerializer, models)
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer')
      serializer.retriableReadTaskFromFile('12345_1_2').failed((err) => {
        expect(readAsStringMock).toBeCalledTimes(1)
        expect(deserializeMock).toBeCalledWith(contents)
        expect(deleteMock).toBeCalledWith('12345_1_2', true)
        expect(tasksMaterializerSpy).not.toBeCalled()
        expect(err).toBeInstanceOf(DeserializingError)
        expect(err).toStrictEqual(
          new DeserializingError(
            "Task deserialization error from file: '12345_1_2'. Error: FAILED",
            '12345_1_2',
            { message: 'FAILED' },
            null,
          ),
        )
        done()
      })
    })
    it('should allow deletion failure if attempts are exempt', (done) => {
      const deleteMock = jest.fn().mockReturnValue(rejected('DELETION FAILED'))
      const readAsStringMock = jest.fn().mockReturnValue(rejected('READ FAILED'))
      const fs = MockFileSystem({
        delete: deleteMock,
        readAsString: readAsStringMock,
      })
      const deserializeMock = jest.fn().mockReturnValue(new Result(null, new Error('FAILED')))
      const jsonSerializer = MockJSONSerializer({
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer('commands', fs, MockHighPrecisionTimer(), jsonSerializer, MockModels())
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer')
      serializer.retriableReadTaskFromFile('12345_1_2').failed((err) => {
        expect(readAsStringMock).toBeCalledTimes(5)
        expect(readAsStringMock).toBeCalledWith('12345_1_2', Encoding.Utf8)
        expect(deserializeMock).not.toBeCalled()
        expect(tasksMaterializerSpy).not.toBeCalled()
        expect(deleteMock).toBeCalledWith('12345_1_2', true)
        expect(err.message).toBe("Failed to deserialize task from file '12345_1_2'")
        done()
      })
    })
    it('should fail if error occurs during materialization', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(testTask.serialize())

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        readAsString: readAsStringMock,
      })
      const deserializeMock = jest.fn().mockImplementation((cts) => new Result(JSONItemFromJSONString(cts), null))
      const jsonSerializer = MockJSONSerializer({
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer('commands', fs, MockHighPrecisionTimer(), jsonSerializer, models)
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer')
      serializer.retriableReadTaskFromFile('12345_1_2').failed((err) => {
        expect(readAsStringMock).toBeCalledTimes(1)
        expect(readAsStringMock).toBeCalledWith('12345_1_2', Encoding.Utf8)
        expect(deserializeMock).toBeCalledTimes(1)
        expect(deserializeMock).toBeCalledWith(contents)
        expect(deleteMock).toBeCalledWith('12345_1_2', true)
        expect(tasksMaterializerSpy).toBeCalledWith(testTask.getType())
        expect(err).toBeInstanceOf(DeserializingError)
        expect(err).toStrictEqual(
          new DeserializingError(
            "Task deserialization error from file: '12345_1_2'",
            '12345_1_2',
            new BasicError(),
            transformJSONItemToSerializable(testTask.serialize()),
          ),
        )
        done()
      })
    })
    it('should fail if filename is malformed and delete it', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(new ArrayJSONItem().add(testTask.serialize()))

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        readAsString: readAsStringMock,
      })
      const deserializeMock = jest.fn().mockImplementation((cts) => new Result(JSONItemFromJSONString(cts), null))
      const jsonSerializer = MockJSONSerializer({
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer('commands', fs, MockHighPrecisionTimer(), jsonSerializer, models)
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer')
      serializer.retriableReadTaskFromFile('12345_1').failed((err) => {
        expect(readAsStringMock).toBeCalledTimes(1)
        expect(readAsStringMock).toBeCalledWith('12345_1', Encoding.Utf8)
        expect(deserializeMock).not.toBeCalled()
        expect(tasksMaterializerSpy).not.toBeCalled()
        expect(deleteMock).toBeCalledWith('12345_1', true)
        expect(err).toBeInstanceOf(UnsupportedTypeError)
        expect(err).toStrictEqual(new UnsupportedTypeError("Unknown type of command file '12345_1'", '12345_1'))
        done()
      })
    })
    it('should fail if filename specifies unsupported type', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(new ArrayJSONItem().add(testTask.serialize()))

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        readAsString: readAsStringMock,
      })
      const deserializeMock = jest.fn().mockImplementation((cts) => new Result(JSONItemFromJSONString(cts), null))
      const jsonSerializer = MockJSONSerializer({
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer('commands', fs, MockHighPrecisionTimer(), jsonSerializer, models)
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer')
      serializer.retriableReadTaskFromFile('12345_1_100').failed((err) => {
        expect(readAsStringMock).toBeCalledTimes(1)
        expect(readAsStringMock).toBeCalledWith('12345_1_100', Encoding.Utf8)
        expect(deserializeMock).not.toBeCalled()
        expect(tasksMaterializerSpy).not.toBeCalled()
        expect(deleteMock).toBeCalledWith('12345_1_100', true)
        expect(err).toBeInstanceOf(UnsupportedTypeError)
        expect(err).toStrictEqual(new UnsupportedTypeError("Unknown type of command file '12345_1_100'", '12345_1_100'))
        done()
      })
    })
    it('should fail if task materialization fails', (done) => {
      const models = MockModels()
      const testTask = new TestTask(1, int64(54321), models)
      const contents = JSONItemToJSONString(testTask.serialize())

      const deleteMock = jest.fn().mockReturnValue(resolve(getVoid()))
      const readAsStringMock = jest.fn().mockReturnValue(resolve(contents))
      const fs = MockFileSystem({
        delete: deleteMock,
        readAsString: readAsStringMock,
      })
      const deserializeMock = jest.fn().mockImplementation((cts) => new Result(JSONItemFromJSONString(cts), null))
      const jsonSerializer = MockJSONSerializer({
        deserialize: deserializeMock,
      })
      const serializer = new TaskSerializer('commands', fs, MockHighPrecisionTimer(), jsonSerializer, models)
      const tasksMaterializerSpy = jest.spyOn(serializer, 'tasksMaterializer').mockReturnValue(() => resolve(null))
      serializer.retriableReadTaskFromFile('12345_1_2').failed((err) => {
        expect(readAsStringMock).toBeCalledTimes(1)
        expect(readAsStringMock).toBeCalledWith('12345_1_2', Encoding.Utf8)
        expect(deserializeMock).toBeCalledTimes(1)
        expect(deserializeMock).toBeCalledWith(contents)
        expect(tasksMaterializerSpy).toBeCalledWith(testTask.getType())
        expect(deleteMock).toBeCalledWith('12345_1_2', true)
        expect(err).toBeInstanceOf(DeserializingError)
        expect(err).toStrictEqual(
          new DeserializingError(
            "Task deserialization error from file: '12345_1_2'",
            '12345_1_2',
            new BasicError(),
            transformJSONItemToSerializable(testTask.serialize()),
          ),
        )
        done()
      })
    })
  })
  describe('Materializing', () => {
    it('should return materializer for task type', () => {
      const serializer = new TaskSerializer(
        'commands',
        MockFileSystem(),
        MockHighPrecisionTimer(),
        MockJSONSerializer(),
        MockModels(),
      )
      expect(serializer.tasksMaterializer(TaskType.delete)).toEqual(expect.any(Function))
    })
  })
})
