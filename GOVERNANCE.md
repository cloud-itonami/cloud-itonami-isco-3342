# Governance

`cloud-itonami-isco-3342` is an OSS open-occupation blueprint. Governance covers
both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Advisor cannot directly dispatch robot actions, finalize disclosure of
  privileged/confidential case information, provide legal advice, or set a
  filing deadline/legal strategy without attorney sign-off.
- Legal Secretary Governor remains independent of the advisor.
- hard policy violations cannot be overridden by human approval.
- `:flag-confidentiality-concern` always escalates and is never added to the
  auto-commit op set.
- every commit, hold and approval path is auditable.
- real client/case/operator data stays outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or license
should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit, support and data-flow
review.

Certified operators can lose certification for:

- bypassing policy checks
- mishandling client/case/operator data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
