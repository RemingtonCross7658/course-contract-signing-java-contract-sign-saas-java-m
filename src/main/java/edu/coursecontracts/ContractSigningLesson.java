package edu.coursecontracts;

import edu.coursecontracts.config.ContractServiceConfig;
import edu.coursecontracts.domain.ContractSigningService;
import edu.coursecontracts.domain.ContractSigningService.ContractRequest;
import edu.coursecontracts.domain.ContractSigningService.SigningReceipt;
import edu.coursecontracts.domain.TenantAccount;
import edu.coursecontracts.infrai.InfraiPdfClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;

public final class ContractSigningLesson {
    private ContractSigningLesson() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: ContractSigningLesson <contract.pdf> <signer.p12> <alias> <tenant-id>");
            System.exit(2);
        }
        char[] password = requiredEnvironment("SIGNING_KEY_PASSWORD").toCharArray();
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(Path.of(args[1]))) { store.load(input, password); }
        PrivateKey key = (PrivateKey) store.getKey(args[2], password);

        TenantAccount tenant = new TenantAccount(args[3]);
        tenant.activate();
        ContractRequest contract = new ContractRequest(
                "annual-2026", tenant.tenantId(), "Faculty Learning Cloud", 12, Files.readAllBytes(Path.of(args[0])));
        var service = new ContractSigningService(new InfraiPdfClient(ContractServiceConfig.fromEnvironment()));
        SigningReceipt receipt = service.sign(contract, tenant, key);
        Path signaturePath = Path.of(args[0] + ".sig");
        Files.writeString(signaturePath, receipt.detachedSignature());
        System.out.println("SIGNED contract=" + receipt.contractId() + " tenant=" + tenant.tenantId()
                + " state=" + receipt.tenantState() + " signature=" + signaturePath);
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Set " + name);
        return value;
    }
}
