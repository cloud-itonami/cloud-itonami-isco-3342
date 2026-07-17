# cloud-itonami-isco-3342

Open Occupation Blueprint for **ISCO-08 3342**: Legal Secretaries.

This repository designs a forkable OSS business for an independent legal secretarial practice: a document intake and case-file coordination robot manages filing and docketing records under a governor-gated actor, so the practice keeps its own case records instead of renting a closed case-management SaaS.

**Maturity: `:implemented`.** `src/legalsecretary/` implements the
`LegalSecretaryActor` as a `langgraph.graph/state-graph`
(`legalsecretary.actor`) wired to a `Legal Secretary Advisor`
(`legalsecretary.advisor`) and an independent `LegalSecretaryGovernor`
(`legalsecretary.governor`), following the itonami actor pattern
(ADR-2607011000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. HARD invariants (always hold, never overridable):
attorney provenance, no-actuation (`:effect` must be `:propose`), a
closed op allowlist (`:log-document-record`,
`:schedule-court-operation`, `:flag-confidentiality-concern`,
`:coordinate-supply-order` -- no op in it can finalize disclosure of
privileged/confidential case information, provide legal advice, or set
a filing deadline/legal strategy without attorney sign-off), a
registered case basis for any case-scoped proposal, an
attorney-supplied deadline for any scheduling proposal, and a
defense-in-depth scope-exclusion check on proposal text. Always-escalate
ops (human sign-off regardless of confidence, mapping this repo's
Trust Controls in [`docs/business-model.md`](docs/business-model.md)):
`:flag-confidentiality-concern` (always -- a confidentiality/privilege
concern is surfaced to a human, never auto-resolved) and any
`:coordinate-supply-order` above the case's registered
`:max-supply-cost` ceiling.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a document intake and case-file coordination robot performs filing, docketing and physical archival under an actor that proposes
actions and an independent **Legal Secretary Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as flagging a confidentiality concern, or a supply order above the case's registered cost ceiling) require human sign-off. The governor never finalizes disclosure of privileged/confidential case information, never provides legal advice, and never sets a filing deadline or legal strategy without attorney sign-off -- no op that could do so exists in the closed proposal allowlist.

## Core Contract

```text
attorney case load + filing calendar + case-material needs
        |
        v
Legal Secretary Advisor -> Legal Secretary Governor -> log/schedule/order, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
finalize disclosure of privileged/confidential case information,
provide legal advice, set a filing deadline/legal strategy without
attorney sign-off, or suppress an operating record.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3342`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
