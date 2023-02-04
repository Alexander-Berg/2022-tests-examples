import Animator from '../../src/vector_render_engine/util/animation/animator';
import AnimatorFunction from '../../src/vector_render_engine/util/animation/animator_function';
import LinearNumberAnimatorFunction from '../../src/vector_render_engine/util/animation/linear_number_animator_function';

/**
 * Simple animator suitable for tests where animators take place.
 */
export default class TestAnimator extends Animator<number> {
    constructor(startValue: number = 10, speed: number = 0.1) {
        super();
        this._startValue = startValue;
        this._speed = speed;
        this._isFinished = false;
    }

    setFinish(): void {
        this._isFinished = true;
    }

    onUpdate(): void {}

    protected isFinished(): boolean {
        return this._isFinished;
    }

    protected getAnimatorFunction(startTime: number): AnimatorFunction<number> {
        return new LinearNumberAnimatorFunction(startTime, this._startValue, this._speed);
    }

    private readonly _startValue: number;
    private readonly _speed: number;
    private _isFinished: boolean;
}
