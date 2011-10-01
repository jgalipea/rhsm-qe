package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.LogMessageUtil;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.InstalledProduct;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.ProductNamespace;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * Note: This scribe depends on register with --autosubscribe working properly
 */


@Test(groups={"ComplianceTests","AcceptanceTests"})
public class ComplianceTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************
	
	@Test(	description="subscription-manager: verify the system.compliant fact is False when some installed products are subscribable",
			groups={"configureProductCertDirForSomeProductsSubscribable","cli.tests"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenSomeProductsAreSubscribable_Test() {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,(String)null,Boolean.TRUE,null, null, null);
		List<InstalledProduct> installdProducts = clienttasks.getCurrentlyInstalledProducts();
		Assert.assertFalse(installdProducts.isEmpty(),
				"Products are currently installed for which the compliance of only SOME are covered by currently available subscription pools.");
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.FALSE.toString(),
				"Before attempting to subscribe and become compliant for all the currently installed products, the system should be incompliant (see value for fact '"+factNameForSystemCompliance+"').");
		clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.listInstalledProducts();
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.FALSE.toString(),
				"When a system has products installed for which only SOME are covered by available subscription pools, the system should NOT become compliant (see value for fact '"+factNameForSystemCompliance+"') even after having subscribed to every available subscription pool.");
	}
	
	@Test(	description="rhsm-complianced: verify rhsm-complianced -d -s reports an incompliant status when some installed products are subscribable",
			groups={"blockedbyBug-723336","blockedbyBug-691480","cli.tests"},
			dependsOnMethods={"VerifySystemCompliantFactWhenSomeProductsAreSubscribable_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRhsmCompliancedWhenSomeProductsAreSubscribable_Test() {
		String command = clienttasks.rhsmComplianceD+" -s -d";
		RemoteFileTasks.runCommandAndWait(client, "echo 'Testing "+command+"' >> "+clienttasks.varLogMessagesFile, LogMessageUtil.action());

		// verify the stdout message
		RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0), rhsmComplianceDStdoutMessageWhenIncompliant, null);
		
		// also verify the /var/syslog/messages
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.varLogMessagesFile, null, rhsmComplianceDSyslogMessageWhenIncompliant, null);
	}
	
	
	
	@Test(	description="subscription-manager: verify the system.compliant fact is True when all installed products are subscribable",
			groups={"configureProductCertDirForAllProductsSubscribable","cli.tests"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenAllProductsAreSubscribable_Test() {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,(String)null,Boolean.TRUE,null, null, null);
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		Assert.assertFalse(installedProducts.isEmpty(),
				"Products are currently installed for which the compliance of ALL are covered by currently available subscription pools.");
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.FALSE.toString(),
				"Before attempting to subscribe and become compliant for all the currently installed products, the system should be incompliant (see value for fact '"+factNameForSystemCompliance+"').");
		clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.listInstalledProducts();
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.TRUE.toString(),
				"When a system has products installed for which ALL are covered by available subscription pools, the system should become compliant (see value for fact '"+factNameForSystemCompliance+"') after having subscribed to every available subscription pool.");
	}
	
	@Test(	description="rhsm-complianced: verify rhsm-complianced -d -s reports a compliant status when all installed products are subscribable",
			groups={"blockedbyBug-723336","cli.tests"},
			dependsOnMethods={"VerifySystemCompliantFactWhenAllProductsAreSubscribable_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRhsmCompliancedWhenAllProductsAreSubscribable_Test() {
		String command = clienttasks.rhsmComplianceD+" -s -d";

		// verify the stdout message
		RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0), rhsmComplianceDStdoutMessageWhenCompliant, null);
	}
	
	
	
	@Test(	description="subscription-manager: verify the system.compliant fact is False when no installed products are subscribable",
			groups={"configureProductCertDirForNoProductsSubscribable","cli.tests", "blockedByBug-737762"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenNoProductsAreSubscribable_Test() {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,(String)null,Boolean.TRUE,null, null, null);
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		Assert.assertFalse(installedProducts.isEmpty(),
				"Products are currently installed for which the compliance of NONE are covered by currently available subscription pools.");
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.FALSE.toString(),
				"Before attempting to subscribe and become compliant for all the currently installed products, the system should be incompliant (see value for fact '"+factNameForSystemCompliance+"').");
		clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.listInstalledProducts();
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.FALSE.toString(),
				"When a system has products installed for which NONE are covered by available subscription pools, the system should NOT become compliant (see value for fact '"+factNameForSystemCompliance+"') after having subscribed to every available subscription pool.");
	}
	
	@Test(	description="rhsm-complianced: verify rhsm-complianced -d -s reports an incompliant status when no installed products are subscribable",
			groups={"blockedbyBug-723336","blockedbyBug-691480","cli.tests"},
			dependsOnMethods={"VerifySystemCompliantFactWhenNoProductsAreSubscribable_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRhsmCompliancedWhenNoProductsAreSubscribable_Test() {
		String command = clienttasks.rhsmComplianceD+" -s -d";
		RemoteFileTasks.runCommandAndWait(client, "echo 'Testing "+command+"' >> "+clienttasks.varLogMessagesFile, LogMessageUtil.action());

		// verify the stdout message
		RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0), rhsmComplianceDStdoutMessageWhenIncompliant, null);
		
		// also verify the /var/syslog/messages
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.varLogMessagesFile, null, rhsmComplianceDSyslogMessageWhenIncompliant, null);
	}

	
	
	@Test(	description="subscription-manager: verify the system.compliant fact is True when no products are installed",
			groups={"configureProductCertDirForNoProductsInstalled","cli.tests"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenNoProductsAreInstalled_Test() {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,(String)null,Boolean.TRUE,null, null, null);
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		Assert.assertTrue(installedProducts.isEmpty(),
				"No products are currently installed.");
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.TRUE.toString(),
				"Because no products are currently installed, the system should inherently be compliant (see value for fact '"+factNameForSystemCompliance+"') even without subscribing to any subscription pools.");
		clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.listInstalledProducts();
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.TRUE.toString(),
				"Even after subscribing to all the available subscription pools, a system with no products installed should remain compliant (see value for fact '"+factNameForSystemCompliance+"').");
	}
	
	@Test(	description="rhsm-complianced: verify rhsm-complianced -d -s reports a compliant status when no products are installed",
			groups={"blockedbyBug-723336","cli.tests"},
			dependsOnMethods={"VerifySystemCompliantFactWhenNoProductsAreInstalled_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRhsmCompliancedWhenNoProductsAreInstalled_Test() {
		String command = clienttasks.rhsmComplianceD+" -s -d";

		// verify the stdout message
		RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0), rhsmComplianceDStdoutMessageWhenCompliant, null);
	}
	
	
	
	
	
	
	@Test(	description="subscription-manager: verify the system.compliant fact when system is already registered to RHN Classic",
			groups={"blockedByBug-742027","RHNClassicTests","cli.tests"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenRegisteredToRHNClassic_Test() {
		
		// pre-test check for installed products
		clienttasks.unregister(null,null,null);
		configureProductCertDirAfterClass();
		if (clienttasks.getCurrentlyInstalledProducts().isEmpty()) throw new SkipException("This test requires that at least one product cert is installed.");


		// first assert that we are not compliant since we have not yet registered to RHN Classic
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.FALSE.toString(),
				"While at least one product cert is installed and we are NOT registered to RHN Classic, the system should NOT be compliant (see value for fact '"+factNameForSystemCompliance+"').");

		// simulate registration to RHN Classic by creating a /etc/sysconfig/rhn/systemid
		log.info("Simulating registration to RHN Classic by creating an empty systemid file '"+clienttasks.rhnSystemIdFile+"'...");
		RemoteFileTasks.runCommandAndWait(client, "touch "+clienttasks.rhnSystemIdFile, LogMessageUtil.action());
		Assert.assertTrue(RemoteFileTasks.testFileExists(client, clienttasks.rhnSystemIdFile)==1, "RHN Classic systemid file '"+clienttasks.rhnSystemIdFile+"' is in place.");

		// now assert compliance
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.TRUE.toString(),
				"By definition, being registered to RHN Classic implies the system IS compliant no matter what products are installed (see value for fact '"+factNameForSystemCompliance+"').");
	}
	
	@Test(	description="rhsm-complianced: verify rhsm-complianced -d -s reports a compliant status when registered to RHN Classic",
			groups={"RHNClassicTests","cli.tests"},
			dependsOnMethods={"VerifySystemCompliantFactWhenRegisteredToRHNClassic_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRhsmCompliancedWhenRegisteredToRHNClassic_Test() {
		String command = clienttasks.rhsmComplianceD+" -s -d";

		// verify the stdout message
		RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0), "System is already registered to another entitlement system", null);
	}
	
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 649068 - Certs with entitlement start date in the future are treated as Expired
	// TODO Bug 737553 - should not be compliant for a future subscription
	// TODO Bug 727967 - Compliance Assistant Valid Until Date Detection Not Working
	
	
	
	// Protected Class Variables ***********************************************************************
	
	protected final String productCertDirForSomeProductsSubscribable = "/tmp/sm-someProductsSubscribable";
	protected final String productCertDirForAllProductsSubscribable = "/tmp/sm-allProductsSubscribable";
	protected final String productCertDirForNoProductsSubscribable = "/tmp/sm-noProductsSubscribable";
	protected final String productCertDirForNoProductsinstalled = "/tmp/sm-noProductsInstalled";
	protected String productCertDir = null;
	protected final String factNameForSystemCompliance = "system.entitlements_valid"; // "system.compliant"; // changed with the removal of the word "compliance" 3/30/2011
	protected final String rhsmComplianceDStdoutMessageWhenIncompliant = "System has one or more certificates that are not valid";
	protected final String rhsmComplianceDStdoutMessageWhenCompliant = "System entitlements appear valid";
	protected final String rhsmComplianceDSyslogMessageWhenIncompliant = "This system is missing one or more valid entitlement certificates. Please run subscription-manager for more information.";
	
	
	// Protected Methods ***********************************************************************
	
	
	
	
	// Configuration Methods ***********************************************************************

	@AfterGroups(groups={"setup"},value="RHNClassicTests")
	public void removeRHNSystemIdFile() {
		client.runCommandAndWait("rm -rf "+clienttasks.rhnSystemIdFile);;
	}
	
	@BeforeClass(groups={"setup"})
	public void setupProductCertDirsBeforeClass() {
		
		// clean out the productCertDirs
		for (String productCertDir : new String[]{productCertDirForSomeProductsSubscribable,productCertDirForAllProductsSubscribable,productCertDirForNoProductsSubscribable,productCertDirForNoProductsinstalled}) {
			RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+productCertDir, 0);
			RemoteFileTasks.runCommandAndAssert(client, "mkdir "+productCertDir, 0);
		}
		
		// autosubscribe
//		clienttasks.unregister(null,null,null);	// avoid Bug 733525 - [Errno 2] No such file or directory: '/etc/pki/entitlement'
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, (String)null, true, null, null, null);
		
//		// distribute a copy of the product certs amongst the productCertDirs
//		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
//		for (File productCertFile : clienttasks.getCurrentProductCertFiles()) {
//			ProductCert productCert = clienttasks.getProductCertFromProductCertFile(productCertFile);
//			// TODO WORKAROUND NEEDED FOR Bug 733805 - the name in the subscription-manager installed product listing is changing after a valid subscribe is performed (https://bugzilla.redhat.com/show_bug.cgi?id=733805)
//			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", productCert.productName, installedProducts);
//			if (installedProduct.status.equalsIgnoreCase("Not Subscribed")) {
//				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForNoProductsSubscribable, 0);
//				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForSomeProductsSubscribable, 0);
//			} else if (installedProduct.status.equalsIgnoreCase("Subscribed")) {
//				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForAllProductsSubscribable, 0);
//				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForSomeProductsSubscribable, 0);
//			}
//		}
		
		// distribute a copy of the product certs amongst the productCertDirs
		List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
		for (File productCertFile : clienttasks.getCurrentProductCertFiles()) {
			ProductCert productCert = clienttasks.getProductCertFromProductCertFile(productCertFile);
			
			// WORKAROUND NEEDED FOR Bug 733805 - the name in the subscription-manager installed product listing is changing after a valid subscribe is performed (https://bugzilla.redhat.com/show_bug.cgi?id=733805)
			List<EntitlementCert> correspondingEntitlementCerts = clienttasks.getEntitlementCertsCorrespondingToProductCert(productCert);
			
			if (correspondingEntitlementCerts.isEmpty()) {
				// "Not Subscribed" case...
				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForNoProductsSubscribable, 0);
				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForSomeProductsSubscribable, 0);
			} else {
				// "Subscribed" case...
				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForAllProductsSubscribable, 0);
				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForSomeProductsSubscribable, 0);
			}
			// TODO "Partially Subscribed" case
			//InstalledProduct installedProduct = clienttasks.getInstalledProductCorrespondingToEntitlementCert(correspondingEntitlementCert);
		}
		
		this.productCertDir = clienttasks.productCertDir;
	}
	
	@AfterClass(groups={"setup"},alwaysRun=true)
	public void configureProductCertDirAfterClass() {
		if (clienttasks==null) return;
		if (this.productCertDir!=null) clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", this.productCertDir);
	}
	
	
	@BeforeGroups(groups={"setup"},value="configureProductCertDirForSomeProductsSubscribable")
	protected void configureProductCertDirForSomeProductsSubscribable() {
		clienttasks.unregister(null, null, null);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",productCertDirForSomeProductsSubscribable);
		SSHCommandResult r0 = client.runCommandAndWait("ls -1 "+productCertDirForSomeProductsSubscribable+" | wc -l");
		SSHCommandResult r1 = client.runCommandAndWait("ls -1 "+productCertDirForAllProductsSubscribable+" | wc -l");
		SSHCommandResult r2 = client.runCommandAndWait("ls -1 "+productCertDirForNoProductsSubscribable+" | wc -l");
		if (Integer.valueOf(r1.getStdout().trim())==0) throw new SkipException("Could not find any installed product certs that are subscribable based on the currently available subscriptions.");
		if (Integer.valueOf(r2.getStdout().trim())==0) throw new SkipException("Could not find any installed product certs that are non-subscribable based on the currently available subscriptions.");
		Assert.assertTrue(Integer.valueOf(r0.getStdout().trim())>0 && Integer.valueOf(r1.getStdout().trim())>0 && Integer.valueOf(r2.getStdout().trim())>0,
				"The "+clienttasks.rhsmConfFile+" file is currently configured with a productCertDir that contains some subscribable products based on the currently available subscriptions.");
	}
	@BeforeGroups(groups={"setup"},value="configureProductCertDirForAllProductsSubscribable")
	protected void configureProductCertDirForAllProductsSubscribable() {
		clienttasks.unregister(null, null, null);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",productCertDirForAllProductsSubscribable);	
		SSHCommandResult r = client.runCommandAndWait("ls -1 "+productCertDirForAllProductsSubscribable+" | wc -l");
		if (Integer.valueOf(r.getStdout().trim())==0) throw new SkipException("Could not find any installed product certs that are subscribable based on the currently available subscriptions.");
		Assert.assertTrue(Integer.valueOf(r.getStdout().trim())>0,
				"The "+clienttasks.rhsmConfFile+" file is currently configured with a productCertDir that contains all subscribable products based on the currently available subscriptions.");
	}
	@BeforeGroups(groups={"setup"},value="configureProductCertDirForNoProductsSubscribable")
	protected void configureProductCertDirForNoProductsSubscribable() {
		clienttasks.unregister(null, null, null);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",productCertDirForNoProductsSubscribable);
		SSHCommandResult r = client.runCommandAndWait("ls -1 "+productCertDirForNoProductsSubscribable+" | wc -l");
		if (Integer.valueOf(r.getStdout().trim())==0) throw new SkipException("Could not find any installed product certs that are non-subscribable based on the currently available subscriptions.");
		Assert.assertTrue(Integer.valueOf(r.getStdout().trim())>0,
				"The "+clienttasks.rhsmConfFile+" file is currently configured with a productCertDir that contains all non-subscribable products based on the currently available subscriptions.");
	}
	@BeforeGroups(groups={"setup"},value="configureProductCertDirForNoProductsInstalled")
	protected void configureProductCertDirForNoProductsInstalled() {
		clienttasks.unregister(null, null, null);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",productCertDirForNoProductsinstalled);
		SSHCommandResult r = client.runCommandAndWait("ls -1 "+productCertDirForNoProductsinstalled+" | wc -l");
		Assert.assertEquals(Integer.valueOf(r.getStdout().trim()),Integer.valueOf(0),
				"The "+clienttasks.rhsmConfFile+" file is currently configured with a productCertDir that contains no products.");
	}
	
	// Data Providers ***********************************************************************

	

}

