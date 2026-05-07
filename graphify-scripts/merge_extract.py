import json
from pathlib import Path

all_nodes, all_edges, all_hyperedges = [], [], []

ast = json.loads(Path('graphify-out/.graphify_ast.json').read_text())
all_nodes.extend(ast.get('nodes', []))
all_edges.extend(ast.get('edges', []))

cached_path = Path('graphify-out/.graphify_cached.json')
if cached_path.exists():
    cached = json.loads(cached_path.read_text())
    all_nodes.extend(cached.get('nodes', []))
    all_edges.extend(cached.get('edges', []))
    all_hyperedges.extend(cached.get('hyperedges', []))

merged = {'nodes': all_nodes, 'edges': all_edges, 'hyperedges': all_hyperedges, 'input_tokens': 0, 'output_tokens': 0}
Path('graphify-out/.graphify_extract.json').write_text(json.dumps(merged, indent=2))
print(f"Merged: {len(all_nodes)} nodes, {len(all_edges)} edges")
