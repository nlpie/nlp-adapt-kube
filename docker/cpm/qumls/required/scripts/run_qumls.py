import glob, os, sys
import pandas as pd
import time
from client import get_quickumls_client
from quickumls import QuickUMLS
from sys import argv

s=sys.argv[1]
o=sys.argv[2]
b=sys.argv[3]

start = time.time()

if b == 'true':
    best_match = True
else:
    best_match = False

# directory of notes to process
directory_to_parse = '/data/data_in/'

# QuickUMLS data directory
quickumls_fp = '/data/UMLS/'
os.chdir(directory_to_parse)

matcher = QuickUMLS(quickumls_fp, o, 0.7, 5, s)
test = pd.DataFrame()
fn = pd.concat
gn = matcher.match
df = pd.DataFrame

for fname in glob.glob(directory_to_parse + '*.txt'):
    t = os.path.basename(fname)
    u = t.split('.')[0]
    with open(directory_to_parse + u + '.txt') as f:
        f1 = f.read()

        print(u)
        out = gn(f1, best_match=best_match, ignore_syntax=False)

        for i in out:
            i[0]['note_id'] = u
            test = fn([ test, df(i[0], index = [0]) ], ignore_index=True)

test['system'] = 'quick_umls'
test['similarity_name'] = s
test['overlap'] = o
test['type'] = 'concept'
test['best_match'] = best_match
test['corpus'] = 'clinical_trial'

temp = test.rename(columns={'start': 'begin'}).copy()

temp.to_csv('/data/qumls_out.csv', mode='a')

elapsed = (time.time() - start)
print("time elapsed:", elapsed)
