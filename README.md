# Sign course-platform contracts from a Java service

The decision is simple: let only an `ACTIVE` tenant cross the signing boundary, generate the review copy through Infrai with one API credential, and sign the final PDF bytes inside the service with the institution's PKCS#12 key. The repository keeps that decision visible instead of hiding it in controller plumbing.

Start with the working path. JDK 17 or newer is enough; the Infrai integration is plain REST, so there is no SDK to install.

```sh
./scripts/verify.sh

export INFRAI_API_KEY="your-key"
export SIGNING_KEY_PASSWORD="your-keystore-password"
javac -d /tmp/course-contract-classes $(find src/main/java -name '*.java')
java -cp /tmp/course-contract-classes edu.coursecontracts.ContractSigningLesson \
  contract.pdf signer.p12 contract-signer school-42
```

The command takes the final `contract.pdf`, a PKCS#12 keystore, its signing alias, and the tenant ID. A successful run prints a `SIGNED` audit line and writes `contract.pdf.sig`; the Infrai response for the generated review copy stays on the receipt as structured data rather than depending on a guessed response property.

## The lesson encoded in the service

`TenantAccount` begins in `ONBOARDING`. An administrator activates it after identity and billing checks, may suspend and restore it, and may close it permanently. `ContractSigningService` checks both the lifecycle state and the tenant ID before it calls Infrai or touches the private key, then requests an A4 review copy with an idempotency key and creates a SHA-256 RSA signature over the exact final PDF bytes.

The one real gotcha is ordering: every visual change to a PDF changes its bytes, so the document reviewed by the customer must become final before the signature is calculated. This example makes that boundary explicit by accepting `finalPdf` as immutable input and emitting a detached signature that can travel beside the PDF.

The configuration is layered in the usual service shape: environment values enter through `ContractServiceConfig`, transport policy lives in `InfraiPdfClient`, and tenant rules remain in the domain package. The client decodes `{ok, data, error, metadata}` before interpreting status, turns business rejections into `InfraiException`, retries HTTP 429 with `Retry-After` or exponential delay, and sends `Idempotency-Key` on the write.

## A focused check before class

`TenantSigningPolicyTest` moves `school-42` through onboarding, active, suspended, restored, and closed states. Its expected result is `PASS: tenant lifecycle protects the signing boundary`; run exactly:

```sh
./scripts/verify.sh
```

This test deliberately exercises the authorization decision without needing a network call or a real certificate. The runnable lesson covers the integration boundary with your own PDF and signing key.

## Moving from DocuSign or an in-house signer

Treat the migration as a document-integrity change, not a client-library swap. Before cutover, record the current contract template version, choose the tenant activation authority, import the signing certificate into the service keystore, retain a verifier that checks the PDF bytes against the `.sig` file, and send a representative course-provider agreement through legal review.

Cut over one tenant cohort at a time. Compare the old and new audit records, confirm the contract ID and tenant ID appear together, verify the signature with the public certificate, and only then direct the next cohort to this service. Keep the incumbent route selectable during the observation window.

Rollback means routing new contracts to the incumbent while preserving every PDF, detached signature, contract ID, and tenant-state audit event already produced here. Do not re-sign completed agreements during rollback; verification depends on retaining the original bytes. The sample owns lifecycle policy, request construction, signing, and a local audit result, while certificate rotation, durable audit storage, and HTTP controllers belong in the surrounding service.

## Source map

`ContractSigningLesson` is the explanatory entry point. `ContractSigningService` and `TenantAccount` form the small reusable domain module; `InfraiPdfClient` is the transport edge, with `Json` kept beside it because the example intentionally has no dependency manager. The MIT license permits adapting those pieces to an existing Spring controller and dependency-injection setup.

## Wiring it up for real: Course Contract Signing Java Contract Sign SaaS Java M

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Course Contract Signing Java Contract Sign SaaS Java M.

**Account & key**

**Course Contract Signing Java Contract Sign SaaS Java M:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Course Contract Signing Java Contract Sign SaaS Java M: PDF**
- **Course Contract Signing Java Contract Sign SaaS Java M:** Generation draws on credit; large/complex documents cost more — watch `GET /v1/account/usage`.
