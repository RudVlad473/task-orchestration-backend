#!/usr/bin/env python3
"""Self-test for scripts/verify-k8s-invariants.py's pure check functions.

Prevents the failure this whole gate exists to catch, one level up -- an edit to the gate that makes an invariant unfireable is
invisible against a tree that already satisfies every invariant, so the gate goes green and stays
green. Each case below builds an in-memory rendered document (or source-file text) engineered to
trip exactly one invariant and asserts the violation is reported; a clean fixture asserts nothing
is reported. No disk access, no kubectl, no working-tree mutation -- this proves the LOGIC fires,
not the wiring against the real k8s/ tree (that is what the gate's own real-tree run proves).
"""

import importlib.util
import os
import sys

_GATE_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "verify-k8s-invariants.py")
_spec = importlib.util.spec_from_file_location("verify_k8s_invariants", _GATE_PATH)
_gate = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_gate)


def clean_app_deployment():
    return {
        "kind": "Deployment",
        "metadata": {"name": "app"},
        "spec": {
            "template": {
                "spec": {
                    "initContainers": [
                        {
                            "name": "register-schemas",
                            "image": "rudenkovladimir/kanban-board-backend-nonprod:main-1-abcdef1",
                            "resources": {
                                "requests": {"memory": "512Mi"},
                                "limits": {"memory": "1Gi"},
                            },
                        }
                    ],
                    "containers": [
                        {
                            "name": "app",
                            "image": "rudenkovladimir/kanban-board-backend-nonprod:main-1-abcdef1",
                            "resources": {
                                "requests": {"memory": "512Mi"},
                                "limits": {"memory": "1Gi"},
                            },
                        }
                    ],
                }
            }
        },
    }


def clean_service():
    return {"kind": "Service", "metadata": {"name": "app"}, "spec": {"type": "ClusterIP"}}


def clean_postgres_statefulset():
    return {
        "kind": "StatefulSet",
        "metadata": {"name": "postgres"},
        "spec": {
            "template": {
                "spec": {
                    "containers": [
                        {
                            "name": "postgres",
                            "image": "postgres:16",
                            "resources": {
                                "requests": {"memory": "128Mi"},
                                "limits": {"memory": "256Mi"},
                            },
                            "command": [
                                "postgres",
                                "-c",
                                "shared_buffers=32MB",
                                "-c",
                                "work_mem=4MB",
                                "-c",
                                "max_connections=25",
                            ],
                        }
                    ]
                }
            }
        },
    }


def run_cases():
    fails = []

    def expect_violation(label, violations, must_contain):
        if not any(must_contain in v for v in violations):
            fails.append(
                f"{label}: expected a violation containing {must_contain!r}, got {violations!r}"
            )

    def expect_clean(label, violations):
        if violations:
            fails.append(f"{label}: expected no violations, got {violations!r}")

    for label, doc in (
        ("clean app Deployment", clean_app_deployment()),
        ("clean Service", clean_service()),
        ("clean postgres StatefulSet", clean_postgres_statefulset()),
    ):
        expect_clean(f"clean/{label}", _gate.check_rendered_doc(doc, "fixture"))

    doc = clean_service()
    doc["spec"]["type"] = "NodePort"
    expect_violation("I1", _gate.check_rendered_doc(doc, "fixture"), "I1:")

    doc = clean_app_deployment()
    doc["spec"]["template"]["spec"]["hostNetwork"] = True
    expect_violation("I2 (hostNetwork)", _gate.check_rendered_doc(doc, "fixture"), "I2:")

    doc = clean_app_deployment()
    doc["spec"]["template"]["spec"]["containers"][0]["ports"] = [{"hostPort": 8080}]
    expect_violation("I2 (hostPort)", _gate.check_rendered_doc(doc, "fixture"), "I2:")

    doc = clean_app_deployment()
    doc["spec"]["template"]["spec"]["volumes"] = [{"name": "x", "hostPath": {"path": "/etc"}}]
    expect_violation("I2 (hostPath)", _gate.check_rendered_doc(doc, "fixture"), "I2:")

    doc = clean_app_deployment()
    del doc["spec"]["template"]["spec"]["containers"][0]["resources"]
    expect_violation("I3 (missing)", _gate.check_rendered_doc(doc, "fixture"), "I3:")

    doc = clean_app_deployment()
    doc["spec"]["template"]["spec"]["containers"][0]["resources"] = {
        "requests": {"memory": "2Gi"},
        "limits": {"memory": "1Gi"},
    }
    expect_violation("I3 (requests > limits)", _gate.check_rendered_doc(doc, "fixture"), "I3:")

    # I4 -- a memory: line with no MEASURED/PROVISIONAL comment in the 25 lines above it.
    unmeasured_text = "\n".join(["# just a comment"] * 30 + ["    memory: 512Mi"])
    expect_violation(
        "I4 (unmeasured)",
        _gate.check_i4_source_file("fixture.yaml", unmeasured_text),
        "I4:",
    )
    measured_text = "# MEASURED (fixture, 2026-01-01)\n    memory: 512Mi"
    expect_clean("I4 (measured, clean)", _gate.check_i4_source_file("fixture.yaml", measured_text))

    # I4 -- --no-provisional: a PROVISIONAL label passes without the flag, fails with it.
    provisional_text = "# PROVISIONAL (fixture, 2026-01-01)\n    memory: 512Mi"
    expect_clean(
        "I4 (provisional, no flag)",
        _gate.check_i4_source_file("fixture.yaml", provisional_text, no_provisional=False),
    )
    expect_violation(
        "I4 (provisional, --no-provisional)",
        _gate.check_i4_source_file("fixture.yaml", provisional_text, no_provisional=True),
        "I4:",
    )
    expect_clean(
        "I4 (measured, --no-provisional)",
        _gate.check_i4_source_file("fixture.yaml", measured_text, no_provisional=True),
    )

    doc = clean_app_deployment()
    doc["spec"]["template"]["spec"]["containers"][0]["image"] = "rudenkovladimir/kanban-board-backend"
    expect_violation("I5 (untagged)", _gate.check_rendered_doc(doc, "fixture"), "I5:")

    doc = clean_app_deployment()
    doc["spec"]["template"]["spec"]["containers"][0]["image"] = "rudenkovladimir/kanban-board-backend:latest"
    expect_violation("I5 (:latest)", _gate.check_rendered_doc(doc, "fixture"), "I5:")

    bad_overlay = (
        "apiVersion: kustomize.config.k8s.io/v1beta1\n"
        "kind: Kustomization\n"
        "images:\n"
        "  - name: rudenkovladimir/kanban-board-backend\n"
        "    newName: rudenkovladimir/kanban-board-backend-nonprod\n"
        "    newTag: not-a-sortable-tag\n"
    )
    expect_violation(
        "I5 (overlay bad tag)",
        _gate.check_i5_overlay_kustomization("fixture/kustomization.yaml", bad_overlay),
        "I5:",
    )
    good_overlay = (
        "apiVersion: kustomize.config.k8s.io/v1beta1\n"
        "kind: Kustomization\n"
        "images:\n"
        "  - name: rudenkovladimir/kanban-board-backend\n"
        "    newName: rudenkovladimir/kanban-board-backend-nonprod\n"
        '    newTag: main-1-abcdef1 # {"$imagepolicy": "flux-system:kanban-board-backend-nonprod:tag"}\n'
    )
    expect_clean(
        "I5 (overlay good tag, clean)",
        _gate.check_i5_overlay_kustomization("fixture/kustomization.yaml", good_overlay),
    )

    # I6 -- shared_buffers exceeds a quarter of the limit.
    doc = clean_postgres_statefulset()
    doc["spec"]["template"]["spec"]["containers"][0]["command"] = [
        "postgres",
        "-c",
        "shared_buffers=128MB",
        "-c",
        "work_mem=4MB",
        "-c",
        "max_connections=25",
    ]
    expect_violation("I6 (shared_buffers)", _gate.check_rendered_doc(doc, "fixture"), "I6:")

    # I6 -- worst-case connection memory exceeds 85% of the limit.
    doc = clean_postgres_statefulset()
    doc["spec"]["template"]["spec"]["containers"][0]["resources"]["limits"]["memory"] = "64Mi"
    doc["spec"]["template"]["spec"]["containers"][0]["command"] = [
        "postgres",
        "-c",
        "shared_buffers=8MB",
        "-c",
        "work_mem=4MB",
        "-c",
        "max_connections=25",
    ]
    expect_violation("I6 (worst-case)", _gate.check_rendered_doc(doc, "fixture"), "I6:")

    expect_violation(
        "I7",
        _gate.find_uncovered_roots(["k8s/base/orphan"], set(), set()),
        "I7:",
    )
    expect_clean(
        "I7 (accounted for, clean)",
        _gate.find_uncovered_roots(["k8s/base/app"], {"k8s/base/app"}, set()),
    )

    doc = {"kind": "Secret", "metadata": {"name": "app-env"}}
    expect_violation("I8", _gate.check_rendered_doc(doc, "fixture"), "I8:")

    # I10 -- a redirect route without the ACME-challenge-path negation must fire.
    redirect_mw = {
        "kind": "Middleware",
        "metadata": {"name": "redirect-https", "namespace": "kanban-prod"},
        "spec": {"redirectScheme": {"scheme": "https"}},
    }
    bad_route_ir = {
        "kind": "IngressRoute",
        "metadata": {"name": "app-http", "namespace": "kanban-prod"},
        "spec": {
            "entryPoints": ["web"],
            "routes": [
                {
                    "match": "Host(`example.com`)",
                    "middlewares": [{"name": "redirect-https"}],
                }
            ],
        },
    }
    expect_violation(
        "I10 (missing negation)",
        _gate.check_i10_redirect_acme_exclusion([redirect_mw, bad_route_ir], "fixture"),
        "I10:",
    )
    good_route_ir = {
        "kind": "IngressRoute",
        "metadata": {"name": "app-http", "namespace": "kanban-prod"},
        "spec": {
            "entryPoints": ["web"],
            "routes": [
                {
                    "match": "Host(`example.com`) && !PathPrefix(`/.well-known/acme-challenge/`)",
                    "middlewares": [{"name": "redirect-https"}],
                }
            ],
        },
    }
    expect_clean(
        "I10 (negation present, clean)",
        _gate.check_i10_redirect_acme_exclusion([redirect_mw, good_route_ir], "fixture"),
    )

    # I10 -- a web-entrypoint route naming a Middleware undefined in its root/namespace fails
    # closed as unclassifiable, even with no redirectScheme Middleware present at all.
    undefined_mw_ir = {
        "kind": "IngressRoute",
        "metadata": {"name": "app-http", "namespace": "kanban-prod"},
        "spec": {
            "entryPoints": ["web"],
            "routes": [
                {
                    "match": "Host(`example.com`)",
                    "middlewares": [{"name": "does-not-exist"}],
                }
            ],
        },
    }
    expect_violation(
        "I10 (undefined Middleware, fail closed)",
        _gate.check_i10_redirect_acme_exclusion([undefined_mw_ir], "fixture"),
        "I10:",
    )

    run_i11_cases(expect_violation, expect_clean)

    return fails


def flux_kustomization(name, path, **spec_extra):
    spec = {
        "path": path,
        "sourceRef": {"kind": "GitRepository", "name": "flux-system"},
    }
    spec.update(spec_extra)
    return {
        "apiVersion": "kustomize.toolkit.fluxcd.io/v1",
        "kind": "Kustomization",
        "metadata": {"name": name, "namespace": "flux-system"},
        "spec": spec,
    }


def rendered_object(api_version, kind, name, namespace="monitoring"):
    metadata = {"name": name}
    if namespace is not None:
        metadata["namespace"] = namespace
    return {"apiVersion": api_version, "kind": kind, "metadata": metadata}


def run_i11_cases(expect_violation, expect_clean):
    edge = flux_kustomization("edge", "./k8s/platform/edge")
    monitoring = flux_kustomization("monitoring", "./k8s/monitoring/configs")
    edge_root = "k8s/platform/edge"
    monitoring_root = "k8s/monitoring/configs"
    route = rendered_object("traefik.io/v1alpha1", "IngressRoute", "grafana")

    # The same object rendered by two Kustomizations: the last applier silently wins.
    expect_violation(
        "I11 (duplicate route)",
        _gate.check_i11_single_flux_owner(
            [edge, monitoring], {edge_root: [route], monitoring_root: [dict(route)]}
        ),
        "I11: IngressRoute/grafana (traefik.io, namespace monitoring) is rendered by 2 Flux "
        "Kustomizations: edge (k8s/platform/edge), monitoring (k8s/monitoring/configs)",
    )

    # Kubernetes stores one object per group, kind, namespace and name, so a version difference
    # does not make two objects.
    expect_violation(
        "I11 (version-only difference)",
        _gate.check_i11_single_flux_owner(
            [edge, monitoring],
            {
                edge_root: [route],
                monitoring_root: [rendered_object("traefik.io/v1", "IngressRoute", "grafana")],
            },
        ),
        "I11: IngressRoute/grafana (traefik.io, namespace monitoring) is rendered by 2",
    )

    expect_clean(
        "I11 (other namespace)",
        _gate.check_i11_single_flux_owner(
            [edge, monitoring],
            {
                edge_root: [route],
                monitoring_root: [
                    rendered_object("traefik.io/v1alpha1", "IngressRoute", "grafana", "other")
                ],
            },
        ),
    )

    expect_clean(
        "I11 (other API group)",
        _gate.check_i11_single_flux_owner(
            [edge, monitoring],
            {
                edge_root: [route],
                monitoring_root: [
                    rendered_object("traefik.containo.us/v1alpha1", "IngressRoute", "grafana")
                ],
            },
        ),
    )

    issuer = rendered_object("cert-manager.io/v1", "ClusterIssuer", "letsencrypt", None)
    expect_violation(
        "I11 (cluster-scoped duplicate)",
        _gate.check_i11_single_flux_owner(
            [edge, monitoring], {edge_root: [issuer], monitoring_root: [dict(issuer)]}
        ),
        "no namespace",
    )

    expect_violation(
        "I11 (path with no rendered root)",
        _gate.check_i11_single_flux_owner(
            [flux_kustomization("ghost", "./k8s/does/not/exist")], {edge_root: [route]}
        ),
        "k8s/does/not/exist",
    )
    expect_violation(
        "I11 (absent spec.path)",
        _gate.check_i11_single_flux_owner(
            [
                {
                    "apiVersion": "kustomize.toolkit.fluxcd.io/v1",
                    "kind": "Kustomization",
                    "metadata": {"name": "nopath", "namespace": "flux-system"},
                    "spec": {"sourceRef": {"kind": "GitRepository", "name": "flux-system"}},
                }
            ],
            {edge_root: [route]},
        ),
        "nopath",
    )

    expect_violation(
        "I11 (targetNamespace)",
        _gate.check_i11_single_flux_owner(
            [flux_kustomization("edge", "./k8s/platform/edge", targetNamespace="x")],
            {edge_root: [route]},
        ),
        "targetNamespace",
    )

    other_source = flux_kustomization("edge", "./k8s/platform/edge")
    other_source["spec"]["sourceRef"] = {"kind": "Bucket", "name": "flux-system"}
    expect_violation(
        "I11 (foreign sourceRef)",
        _gate.check_i11_single_flux_owner([other_source], {edge_root: [route]}),
        "sourceRef",
    )

    expect_clean(
        "I11 (disjoint objects)",
        _gate.check_i11_single_flux_owner(
            [edge, monitoring],
            {
                edge_root: [
                    rendered_object("cert-manager.io/v1", "Certificate", "grafana"),
                    issuer,
                ],
                monitoring_root: [route],
            },
        ),
    )


def main():
    fails = run_cases()
    if fails:
        for line in fails:
            print(f"SELFTEST FAIL: {line}")
        return 1
    print(
        "selftest OK -- I1-I11 each fire on an engineered violation; clean fixtures report nothing"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
