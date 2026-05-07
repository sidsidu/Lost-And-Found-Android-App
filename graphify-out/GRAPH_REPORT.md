# Graph Report - .  (2026-05-07)

## Corpus Check
- Corpus is ~6,355 words - fits in a single context window. You may not need a graph.

## Summary
- 114 nodes · 131 edges · 19 communities (6 shown, 13 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 1 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]

## God Nodes (most connected - your core abstractions)
1. `MainActivity` - 10 edges
2. `ReportFoundActivity` - 9 edges
3. `ReportLostActivity` - 9 edges
4. `ItemViewModel` - 9 edges
5. `SpotlightRippleCard` - 8 edges
6. `FirebaseRepository` - 6 edges
7. `AIEngine` - 5 edges
8. `ImagePicker` - 5 edges
9. `save_item_and_check_matches()` - 4 edges
10. `SpotlightDemoActivity` - 4 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities (19 total, 13 thin omitted)

### Community 4 - "Community 4"
Cohesion: 0.0
Nodes (3): ItemAdapter, ItemDiffCallback, ItemViewHolder

### Community 6 - "Community 6"
Cohesion: 0.0
Nodes (3): AIEngine, Generates an embedding vector for the text to allow for fast cosine similarity s, Uses an external llama.cpp server to extract description, category, colors, and

### Community 8 - "Community 8"
Cohesion: 0.0
Nodes (5): cosine_similarity(), Saves the new item to Firestore and searches for potential matches., Sends an FCM notification to the user of the matched item., save_item_and_check_matches(), send_match_notification()

## Knowledge Gaps
- **7 isolated node(s):** `Uses an external llama.cpp server to extract description, category, colors, and`, `Generates an embedding vector for the text to allow for fast cosine similarity s`, `Saves the new item to Firestore and searches for potential matches.`, `Sends an FCM notification to the user of the matched item.`, `ItemModel` (+2 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **13 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.