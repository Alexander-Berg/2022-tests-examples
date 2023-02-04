def equalizer_enabled(config):
    if 'equalizer' not in config:
        return False

    eq_config = config['equalizer']
    if 'enabled' not in eq_config or not eq_config['enabled']:
        return False

    if 'bands' not in eq_config or len(eq_config['bands']) == 0:
        return False

    return sum(band['gain'] for band in eq_config['bands']) != 0


def wait_equalizer(connector, device, expected_band_count, expected_band_gain=0):
    def equalizer_matcher(quasar_message):
        if not quasar_message.HasField('io_control') or not quasar_message.io_control.HasField('equalizer_config'):
            return False
        eq_config = quasar_message.io_control.equalizer_config
        if len(eq_config.bands) != expected_band_count:
            return False
        for band in eq_config.bands:
            if band.gain != expected_band_gain:
                return False
        return True

    device.wait_for_message(
        connector,
        equalizer_matcher,
        "Failed to receive equalizer config",
    )
