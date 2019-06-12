import sqlite3
import sys

conn = sqlite3.connect('schema.db')
c = conn.cursor()

# Create table
c.execute('''
CREATE TABLE source_note (id INTEGER PRIMARY KEY, service_id INTEGER, note_id INTEGER, version_id INTEGER, rtf2plain TEXT, rtf_pipeline TEXT, b9 TEXT, mm TEXT, clamp TEXT, ctakes TEXT, error TEXT, edited TEXT)
''')
c.execute('''
CREATE TABLE detected_item (id INTEGER PRIMARY KEY, source_note_id INTEGER, engine_id INTEGER, concept_id INTEGER, begin INTEGER, end INTEGER, attributes text)
''')
c.execute('''
CREATE TABLE uima_engine (id INTEGER PRIMARY KEY, name TEXT)
''')
c.execute('''
CREATE TABLE concept (id INTEGER PRIMARY KEY, ui TEXT, description TEXT, is_cui INTEGER, is_abbr INTEGER)
''')

conn.commit()
conn.close()
