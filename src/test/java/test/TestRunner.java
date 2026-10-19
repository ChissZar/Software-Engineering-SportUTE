package test;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import vn.edu.ute.sportute.service.InvoiceExportServiceTest;
import vn.edu.ute.sportute.service.ServiceItemServiceTest;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  STARTING AUTOMATED UNIT TESTS (JUNIT 5 / MOCKITO)");
        System.out.println("  - ServiceItemServiceTest (F&B / Sports Gear Inventory & POS)");
        System.out.println("  - InvoiceExportServiceTest (PDF & Excel Invoice Generator)");
        System.out.println("===============================================================");

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        selectClass(ServiceItemServiceTest.class),
                        selectClass(InvoiceExportServiceTest.class)
                )
                .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        System.out.println("\n----------------- TEST EXECUTION SUMMARY -----------------");
        System.out.println("Total Tests Found     : " + summary.getTestsFoundCount());
        System.out.println("Total Tests Started   : " + summary.getTestsStartedCount());
        System.out.println("Total Tests Succeeded : " + summary.getTestsSucceededCount());
        System.out.println("Total Tests Failed    : " + summary.getTestsFailedCount());
        System.out.println("Total Tests Aborted   : " + summary.getTestsAbortedCount());
        System.out.println("Time Finished         : " + summary.getTimeFinished() + " ms");
        System.out.println("----------------------------------------------------------\n");

        if (summary.getFailures().isEmpty()) {
            System.out.println(">>> [PASSED] 100% UNIT TESTS PASSED SUCCESSFULLY! ALL TEST SUITES GREEN! <<<");
        } else {
            System.out.println(">>> [FAILED] TEST FAILURES DETECTED: <<<");
            summary.getFailures().forEach(f -> {
                System.out.println("- " + f.getTestIdentifier().getDisplayName() + ": " + f.getException().getMessage());
                f.getException().printStackTrace();
            });
            System.exit(1);
        }
    }
}