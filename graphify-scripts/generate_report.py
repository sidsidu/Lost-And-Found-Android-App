import json
from graphify.build import build_from_json
from graphify.cluster import cluster
from graphify.analyze import god_nodes, surprising_connections
from graphify.report import generate
from graphify.export import to_html
from pathlib import Path

# Generate plain-language report
extraction = json.loads(Path('graphify-out/.graphify_extract.json').read_text())
analysis = json.loads(Path('graphify-out/.graphify_analysis.json').read_text())

G = build_from_json(extraction)
communities = {int(k): v for k, v in analysis['communities'].items()}
gods = god_nodes(G)
surprises = surprising_connections(G, communities)

# Pass detection result (not extraction) to the report generator
detect_result = json.loads(Path('graphify-out/.graphify_detect.json').read_text())
token_cost = {'input': 0, 'output': 0}
report = generate(G, communities, {}, {}, gods, surprises, detect_result, token_cost, '.')
Path('graphify-out/GRAPH_REPORT.md').write_text(report, encoding='utf-8')
print('GRAPH_REPORT.md written')

# Try to render HTML visualization
G2 = build_from_json(extraction)
communities2 = cluster(G2)
try:
    to_html(G2, communities2, 'graphify-out/graph.html')
    print('graph.html written')
except Exception as e:
    print(f'Visualization skipped: {e}')
