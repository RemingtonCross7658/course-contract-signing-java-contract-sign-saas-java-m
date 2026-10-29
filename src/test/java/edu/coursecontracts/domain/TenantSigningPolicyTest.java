package edu.coursecontracts.domain;

public final class TenantSigningPolicyTest {
    public static void main(String[] args) {
        TenantAccount tenant = new TenantAccount("school-42");
        require(!tenant.maySignContracts(), "onboarding must not sign");
        tenant.activate();
        require(tenant.maySignContracts(), "active tenant should sign");
        tenant.suspend();
        require(!tenant.maySignContracts(), "suspended tenant must not sign");
        tenant.restore();
        require(tenant.maySignContracts(), "restored tenant should sign");
        tenant.close();
        require(!tenant.maySignContracts(), "closed tenant must not sign");
        System.out.println("PASS: tenant lifecycle protects the signing boundary");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
