# Security Policy

This project handles legal secretaries operating workflows, including
privileged and confidential case files. Treat vulnerabilities as potentially
high impact even when the demo data is synthetic.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real client, case or operator data exposure
- authorization bypass
- Legal Secretary Governor bypass
- audit-ledger tampering
- any path that could finalize disclosure of privileged/confidential case
  information without governor gating
- over-disclosure in reports or exports
- unsafe robot action dispatch

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on client/case data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real client/case/operator data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
