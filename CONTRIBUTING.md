# Contributing

`cloud-itonami-isco-3342` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
kbb -M:test
kbb -M:lint
```

Keep changes small and include tests for policy, audit, store or disclosure
behavior.

## Rules

- Do not commit real client, case or credential data.
- Keep production writes and disclosures behind Legal Secretary Governor.
- Never add an op to the closed proposal allowlist that could finalize
  disclosure of privileged/confidential case information, provide legal
  advice, or set a filing deadline/legal strategy without attorney sign-off.
- Treat this occupation's workflows as high-risk: add tests for permission,
  purpose, confidentiality and audit logging.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
