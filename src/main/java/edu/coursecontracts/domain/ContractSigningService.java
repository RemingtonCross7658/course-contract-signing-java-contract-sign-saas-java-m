package edu.coursecontracts.domain;

import edu.coursecontracts.infrai.InfraiPdfClient;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Objects;

public final class ContractSigningService {
    private final InfraiPdfClient pdfClient;

    public ContractSigningService(InfraiPdfClient pdfClient) { this.pdfClient = pdfClient; }

    public SigningReceipt sign(ContractRequest request, TenantAccount tenant, PrivateKey key)
            throws IOException, InterruptedException, GeneralSecurityException {
        if (!tenant.maySignContracts()) {
            throw new IllegalStateException("Only ACTIVE tenants may sign contracts");
        }
        if (!tenant.tenantId().equals(request.tenantId())) {
            throw new IllegalArgumentException("Contract tenant does not match account");
        }

        String markdown = "# Course platform agreement\n\nTenant: " + request.tenantId()
                + "\n\nPlan: " + request.coursePlan() + "\n\nTerm: " + request.termMonths() + " months\n";
        Object generatedPdf = pdfClient.generateContract(markdown, "contract-" + request.contractId());

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key);
        signer.update(request.finalPdf());
        String detachedSignature = Base64.getEncoder().encodeToString(signer.sign());
        return new SigningReceipt(request.contractId(), tenant.state(), detachedSignature, generatedPdf);
    }

    public record ContractRequest(String contractId, String tenantId, String coursePlan,
                                  int termMonths, byte[] finalPdf) {
        public ContractRequest {
            Objects.requireNonNull(contractId);
            Objects.requireNonNull(tenantId);
            Objects.requireNonNull(coursePlan);
            finalPdf = finalPdf.clone();
            if (termMonths < 1 || finalPdf.length == 0) throw new IllegalArgumentException("Contract must be complete");
        }
        @Override public byte[] finalPdf() { return finalPdf.clone(); }
    }

    public record SigningReceipt(String contractId, TenantAccount.State tenantState,
                                 String detachedSignature, Object generatedPdf) {}
}
