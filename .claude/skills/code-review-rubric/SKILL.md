---
name: code-review-rubric
description: This project's review rubric for a diff that adds or changes Java, tests included. Loaded by the GSD code reviewer through agent_skills in .planning/config.json.
---

# code-review-rubric

The rules live in [`docs/CODE_REVIEW_RUBRIC.md`](../../../docs/CODE_REVIEW_RUBRIC.md). Read it in
full, then apply every rule to every line the diff touches.

Two further lenses come from their own skills, each loaded alongside this one:

- Changed test files: `test-rigor-review`.
- Added or edited comments: `review-comments`, which backs rule 14 of the rubric.

Report each finding with the rule it breaks, the file and line, the deciding test it fails, and
which lens (rubric, test-rigor-review or review-comments) produced it.
