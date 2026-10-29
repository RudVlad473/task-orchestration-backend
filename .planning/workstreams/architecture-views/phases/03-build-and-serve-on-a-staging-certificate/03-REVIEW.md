---
phase: 03-build-and-serve-on-a-staging-certificate
reviewed: 2026-10-29T00:00:00Z
depth: standard
files_reviewed: 2
files_reviewed_list:
  - docs/architecture/model/scenarios-platform.c4
  - docs/architecture/model/delivery.c4
findings:
  critical: 0
  warning: 1
  info: 4
  total: 5
status: issues_found
---

# Phase 03: Code Review Report (gap-closure plan 03-08)

**Reviewed:** 2026-10-29
**Depth:** standard
**Files Reviewed:** 2
**Status:** issues_found

## Summary

Scope: `git diff 6386b5f..HEAD -- docs/architecture/model/` (commits d8599d4 and 853bae1), six reworded sentences. I read every workflow under `.github/workflows` that touches the claims: `deploy.yml`, `backup-image.yml`, `architecture-site.yml` in full where relevant, and I searched the other seven by `rg` for `environment:`, `DOCKERHUB_TOKEN`, `push: true`, `docker/build-push-action` and the `on:` triggers.

Verified against the workflow files (WR-05 is closed for the files in scope):

- `environment: production` is set by `architecture-site.yml:96` (publish only; the build job at :50-53 has no `environment`), by `deploy.yml:94,161,307` and by `backup-image.yml:36`. `architecture-site.yml` holds exactly one `environment:` line. So "the only job of this workflow in the production environment" is true.
- `secrets.DOCKERHUB_TOKEN` is read by `architecture-site.yml:112`, `deploy.yml:107,319,391` and `backup-image.yml:46`, and by no other workflow. So "deploy.yml and backup-image.yml also use" the token is true, and "the only job of this workflow that holds the registry token" is true. `deploy.yml:391` is in a job under `environment: staging`, which uses the staging copy of the token, so "also use" holds loosely for that one job.
- `push: true` appears in exactly `deploy.yml:135`, `backup-image.yml:60` and `architecture-site.yml:128`. The `ci` description names those three, and I re-ran the set diff against it (exit 0).
- `backup-image.yml` triggers on a push to main (`:17-22`) and pushes `kanban-postgres-backup`. `deploy.yml` pushes both the production and the nonprod repository (`:135-139`), which the plural "app image repositories" in :14 covers.
- `node docs/architecture/sync.mjs check` exits 0 (`OK (62 elements, ...)`). Quoting is balanced: no apostrophes inside the single-quoted strings, no line breaks inside them, and each changed string stays on its own line.
- Secret leakage: none. The new text says "registry token" and never the secret's identifier. `rg` over `docs/architecture/model` for `DOCKERHUB`, `dckr_pat`, `rudenkovladimir`, IP-shaped strings, `.ssh/`, `svc.cluster.local`, `backblazeb2` and the age-key shape finds nothing. Workflow file names are already public.

One consistency defect remains outside the six edited lines but caused by them, plus four lower-severity accuracy and clarity points.

## Warnings

### WR-01: The delivery view's own description still says "the app and site images", contradicting the reworded edge label in the same view

**File:** `docs/architecture/model/views.c4:81` (affected by `docs/architecture/model/delivery.c4:43`)
**Issue:** The `delivery` deployment view renders the `ci -> dockerhub` relationship, whose title this change rewrote to 'push the app, backup and site images'. The view's description, three lines of the same block, still reads "which pushes the app and site images to Docker Hub". The plan's absence gate searched for the retired phrase "push the app and site images" (without the "es"), so it could not match "pushes the app and site images", and the plan's source search missed `views.c4` because it looked only at the exact retired phrases. The published delivery diagram now carries an edge that names the backup image and a caption that omits it, which is the same class of drift WR-05 reported. Because `sync.mjs check` never reads prose, nothing gates this.
**Fix:** Edit `views.c4:81` to name the backup image, for example:
```
description 'A push to main starts the workflows on a GitHub-hosted runner, which push the app, backup and site images to Docker Hub. Flux scans the tags, commits the bump to the manifests over Git and applies the overlays, and the app and site pods pull the images.'
```
Then add "pushes the app and site images" to the absence search so the near-miss is covered, and rebuild with the workflow's build command.

## Info

### IN-01: The trigger conditions in the `ci` description are incomplete for the newly added workflow

**File:** `docs/architecture/model/delivery.c4:7`
**Issue:** "backup-image.yml, which builds and pushes the image of the nightly backup job when its Dockerfile changes": `backup-image.yml:20-23` also starts on a change to the workflow file itself and on `workflow_dispatch`. The same shortfall applies to the older clauses: `deploy.yml` also ignores `**/*.md` (`:18-22`) and has a manual trigger (`:6`), and `architecture-site.yml` also watches `pnpm-lock.yaml` and its own file (`:27-32`). The scenario view at `scenarios-platform.c4:35` is precise for the site workflow, so :7 is less exact than the model's own detail views. Not false in the sense WR-05 meant, because each clause names its main trigger.
**Fix:** Either say "mainly when" or drop the trigger clauses from :7 and leave them to the scenario views, for example "backup-image.yml, which builds and pushes the image of the nightly backup job".

### IN-02: The Docker Hub description and the edge title now include the backup image, but the model has no consumer for it

**File:** `docs/architecture/model/delivery.c4:14`, `docs/architecture/model/delivery.c4:43`
**Issue:** :14 says the registry holds the backup job image repository and that "the cluster pulls from it". The only pull relationships are `kanban -> dockerhub` and `archsite -> dockerhub`. The backup CronJob (`k8s/data/postgres/backup-cronjob.yaml:58`) pulls `kanban-postgres-backup` by digest, but `model.c4` has the `backup` element with no `backup -> dockerhub` image pull, and the `delivery` view does not include `backup`. So the diagram shows an image going into the registry that nothing visibly takes out. Separately, the `ci` element's `metadata { source }` still names only `deploy.yml` although the description now lists three workflows.
**Fix:** Add `backup -[sync]-> dockerhub 'image pull by digest'` and include the backup element in the view, or note in :14 that the backup image is pinned by digest in the CronJob manifest. Optionally list the other two workflows in a second `source` entry if the schema allows more than one.

### IN-03: The repo-to-ci title lists three workflows as what a push to main starts, while three more also start on that push

**File:** `docs/architecture/model/delivery.c4:42`
**Issue:** `invariant-checks.yml:16`, `secret-scan.yml:20` and `dependency-submission.yml:30` also trigger on a push to main (`dependency-submission.yml:31` lists branches main). They push no image, so they sit outside `ci` as :7 defines it, and the plan chose this scope on purpose. A reader of the title alone, with the element named "GitHub Actions", can still take the list as exhaustive, and the sentence dropped the word "Two" only to become an implicit "these three". This is the residual form of the count problem.
**Fix:** Make the scoping explicit, for example 'push to main starts the image-pushing workflows deploy.yml, architecture-site.yml and backup-image.yml, each for the paths it watches', or rename the element description to say it models only the image-pushing workflows.

### IN-04: Step 4's relative clause is ambiguous and omits the staging copy of the token

**File:** `docs/architecture/model/scenarios-platform.c4:38`
**Issue:** "the only job of this workflow in the production environment, whose registry token deploy.yml and backup-image.yml also use": "whose" can attach to "this workflow" as well as to "the production environment", and "also use" with the verb at the end is hard to parse in a sequence-diagram label. The token is also used by `deploy.yml:380-391` under `environment: staging` (a duplicated copy, per the comment at `deploy.yml:90-91`), so "the production environment's token" is not the whole of what `deploy.yml` uses. Neither point makes the sentence false; both make it easy to misread.
**Fix:** For example 'the publish job, the only job of this workflow in the production environment, builds the image from the same build output and pushes the main, run number, short sha tag; deploy.yml and backup-image.yml use the same registry token in their own jobs'. If the label length is a concern, keep the clause and drop "whose".

---

_Reviewed: 2026-10-29_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
