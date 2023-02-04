#!/usr/bin/python
'''
Script calculating linear models forecast based on vw binary dump.
Requires binary dump and state file from learning

depends on:
yabs-mapreduce-modules
yabs-logger
yabs-ml-engine
yabs-ml-factors
'''

import sys
import os
import re
from copy import deepcopy
from subprocess import check_call
from tempfile import mkstemp
import yaml
from optparse import OptionParser

import yabs.tabtools
#from lmfactor import RSYAFactors, SERPFactors
from yabs.matrixnet.lmfactor import LMFactors, RSYAFactors, SERPFactors


from yabs.logger import info, error
from yabs.tabutils import restore_format_from_stream, get_tsrecord_type, TSFileFormat, TSRecordFormat
from yabs.tabtools import _MapWrapper, _CompositeMapper, _MapReducer
from yabs.tabtools import Mapper, Grep, Cut
from mr_c_functions import MRTSRecord

from yabs.ml.engines.vw import VWHasher
from yabs.ml.engines.vwutil import VWApplyMapper
from yabs.ml.engines.dumptools import VWDumpHandler
#from dumptools import VWDumpHandler
from yabs.logger import set_log_stream


def generate_model_mapper(dump_path, target):
    '''
    Reads vw model from path.
    Requires file dump.bin with binary dump and file 'state' with state.
    '''

    dump_file = os.path.join(dump_path, 'dump.bin')
    state_file = os.path.join(dump_path, 'state') 
    state = yaml.load(open(os.path.join(dump_path, 'state')))
    num_bits = state['num_bits']
    factors = state['factors']
    quadratic = state['quadratic_features']
    borders = state['borders']
    raw_factors = state['raw_factors']
    no_hashing = state.get('no_hashing', False)

    if no_hashing:
        raise ValueError('no_hashing regime not supported')

    handler = VWDumpHandler()
    weights = handler.get_regression_weights_from_dump_file(dump_file, num_bits)

    mapper = VWApplyMapper(
        target, 
        weights,
        VWHasher(num_bits=num_bits),
        factors,
        raw_factors=raw_factors,
        borders=borders,
        quadratic=quadratic
    )

    return mapper

def apply_mappers(mappers, with_meta=False, begin_lines=''):
 
    input_fmt = restore_format_from_stream(sys.stdin)
    wrapper = _MapWrapper(_CompositeMapper(mappers))
    ff = TSRecordFormat([], [], input_fmt.value, input_fmt.inits)

    wrapper.begin = '\n'.join(begin_lines)
    exec wrapper.begin in yabs.tabtools.__dict__
    wrapper.setFormat(ff)
    wrapper = deepcopy(wrapper)

    rt = MRTSRecord
    out_ff = wrapper.deduceOutputFormat()[0]
    output_fmt = TSFileFormat(out_ff.key + out_ff.subkey + out_ff.value, out_ff.inits)

    sys.stdout.write(output_fmt.header(encode_meta=with_meta))
    #fields = out_ff.key + out_ff.subkey + out_ff.value
    output_fmt.write_records_to_stream(
        wrapper(input_fmt.read_records_from_stream(sys.stdin, rt)), 
        sys.stdout
    )

if __name__ == '__main__':

    argparser = OptionParser()
    argparser.add_option('-d', '--dump', dest='dump', help='path with dump', 
        type='str', default=None)
    argparser.add_option('--target', dest='target', help='target field', 
        type='str', default='LM')
    argparser.add_option('-f', '--factors', dest='factors', help='explicit namespaces to compute', 
        type='str', default=None)
    argparser.add_option('--net', dest='net', help='calculate factors for net', 
        action='store_true', default=False)
    argparser.add_option('--search', dest='search', help='calculate factors for net', 
        action='store_true', default=False)

    options, args = argparser.parse_args()

    set_log_stream(sys.stderr)
 
    if not options.net ^ options.search:
        info('either --net or --search should be specified')    
        sys.exit(0)

    model_mapper = generate_model_mapper(options.dump, options.target)
   
    factors_to_compute = None
    if options.factors:
        factors_to_compute = options.factors.split(',') 
    if options.net:
        lm_mapper = RSYAFactors(factors_to_compute=factors_to_compute)
    elif options.search:
        lm_mapper = SERPFactors(factors_to_compute=factors_to_compute)

    mappers = [lm_mapper, model_mapper]
    apply_mappers(mappers)




 
