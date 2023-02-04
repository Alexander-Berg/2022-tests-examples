from pathlib import Path
import json
import sys


def patch_fn_file(options):
    fr_root = Path('/FR')

    if 'change_fn' in options:
        hardware_file = fr_root / 'hardware.json'
        if hardware_file.is_file():
            try:
                with hardware_file.open() as f:
                    hardware = json.load(f)
            except Exception as e:
                print('Error opening hardware.json:\n{}'.format(e))
        if hardware and isinstance(hardware, dict):
            hardware['FN']['VFNSerialNumber'] = options[1]
        try:
            with hardware_file.open('w') as f:
                json.dump(hardware, f, indent=4)
        except Exception as e:
            print('Error changing fn:\n{}'.format(e))

    fn_dir = fr_root / 'data' / 'virtualfn'
    fn_files = sorted(fn_dir.glob('state_*.json'))
    for fn in fn_files:
        if fn.is_file():
            try:
                with fn.open() as f:
                    state_fn = json.load(f)
            except Exception as e:
                print("Error loading fn:\n{}".format(e))
        if state_fn and isinstance(state_fn, dict):
            if 'clear_queue' in options:
                state_fn['UnsentCounter'] = 0
                state_fn['FirstUnsent'] = 0
                state_fn['FirstDateTime'] = {
                    'Year': 0,
                    'Month': 0,
                    'Day': 0,
                    'Hour': 0,
                    'Minute': 0,
                }
                state_fn['Queue'] = []

            if 'make_archive' in options:
                state_fn['Phase'] = 15

            try:
                with fn.open('w') as f:
                    json.dump(state_fn, f, indent=4)
            except Exception as e:
                print('Error updating queue:\n{}'.format(e))

        if 'clear_archive' in options:
            num = fn.name[:-5].split('_')[1]
            doc_paths = sorted(fn_dir.glob('archive_{}_*.json'.format(num)))
            for doc_path in doc_paths:
                doc_path.unlink()

        if 'clear_state' in options:
            doc_paths = sorted(fn_dir.glob(fn.name))
            for doc_path in doc_paths:
                doc_path.unlink()


if __name__ == '__main__':
    patch_fn_file(sys.argv[1:])
