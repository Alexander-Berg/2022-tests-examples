const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const AudioPlayer = require('./AudioPlayer');

it('должен загрузить новое аудио, если поменялся ресурс', () => {
    const tree = shallow(<AudioPlayer
        isPlaying={ true }
        onError={ _.noop }
        onPlay={ _.noop }
        onPause={ _.noop }
        onStop={ _.noop }
    />);

    tree.instance().audioRef = {
        current: {
            pause: jest.fn(),
            load: jest.fn(),
        },
    };

    tree.setProps({ source: 'newSource' });
    expect(tree.instance().audioRef.current.pause).toHaveBeenCalled();
    expect(tree.instance().audioRef.current.load).toHaveBeenCalled();
});

it('должен воспроизвести аудио, если isPlaying = true', () => {
    const tree = shallow(<AudioPlayer
        isPlaying={ false }
        onError={ _.noop }
        onPlay={ _.noop }
        onPause={ _.noop }
        onStop={ _.noop }
    />);

    tree.instance().audioRef = {
        current: {
            play: jest.fn(),
            pause: jest.fn(),
            load: jest.fn(),
        },
    };

    tree.setProps({ isPlaying: true });
    expect(tree.instance().audioRef.current.play).toHaveBeenCalled();
});

it('должен остановить аудио, если isPlaying = false', () => {
    const tree = shallow(<AudioPlayer
        isPlaying={ true }
        onError={ _.noop }
        onPlay={ _.noop }
        onPause={ _.noop }
        onStop={ _.noop }
    />);

    tree.instance().audioRef = {
        current: {
            play: jest.fn(),
            pause: jest.fn(),
            load: jest.fn(),
        },
    };

    tree.setProps({ isPlaying: false });
    expect(tree.instance().audioRef.current.pause).toHaveBeenCalled();
});

it('должен перемотать запись, если поменялось recordStartTime', () => {
    const tree = shallow(<AudioPlayer
        recordStartTime={ 0 }
        isPlaying={ false }
        onError={ _.noop }
        onPlay={ _.noop }
        onPause={ _.noop }
        onStop={ _.noop }
    />);

    tree.instance().audioRef = {
        current: {
            currentTime: 0,
            play: jest.fn(),
            pause: jest.fn(),
            load: jest.fn(),
        },
    };

    tree.setProps({ recordStartTime: 500 });
    expect(tree.instance().audioRef.current.currentTime).toBe(500);
});
