# Business Model: Independent Legal Secretarial Practice

## Classification

- Repository: `cloud-itonami-isco-3342`
- ISCO-08: `3342`
- Occupation: Legal Secretaries
- Social impact: access-to-justice, small-firm-support, case-file-integrity

## Customer

- solo attorneys
- small law firms

## Offer

- document intake and filing
- court-operation scheduling from attorney-supplied deadlines
- confidentiality-concern flagging
- office/case-material procurement coordination

## Revenue

- monthly retainer
- per-case fee

## Trust Controls

- no proposal outside the closed op allowlist (`:log-document-record`,
  `:schedule-court-operation`, `:flag-confidentiality-concern`,
  `:coordinate-supply-order`) is ever accepted -- no op can finalize
  disclosure of privileged/confidential case information, provide legal
  advice, or set a filing deadline/legal strategy without attorney sign-off
- a filing deadline is only ever logged when attorney-supplied, never
  determined by the actor itself
- `:flag-confidentiality-concern` always escalates to human sign-off,
  regardless of confidence, and is never auto-commit-eligible
- office/case-material procurement above the case's registered cost ceiling
  requires human sign-off
- filing and scheduling records are auditable, not editable
