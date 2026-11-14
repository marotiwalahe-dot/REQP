const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";
const ADMIN_UID = "admin_user_1";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated caller cannot read PDF documents, categories, or payloads", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("pdf_documents").where("isPublished", "==", true).get());
  await assertFails(unauthDb.collection("categories").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("pdf_payloads").doc("doc_1").get());
});

test("Student cannot self-assign admin role or active status manipulation on profile create/update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true }).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@test.com",
      displayName: "Alice",
      role: "admin",
      status: "active",
      biometricLockEnabled: true,
      inactivityTimeoutSec: 60,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@test.com",
      displayName: "Alice",
      role: "student",
      status: "active",
      biometricLockEnabled: true,
      inactivityTimeoutSec: 60,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Ghost field / privilege escalation update must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(),
    })
  );
});

test("Student cannot read premium PDF payload without active server-verified subscription", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("pdf_documents").doc("free_doc").set({
      title: "Free Intro",
      description: "Free study note",
      categoryId: "cat_1",
      categoryName: "DSA",
      accessTier: "free",
      isPublished: true,
      isFeatured: false,
      pageCount: 2,
      fileSizeLabel: "120 KB",
      thumbnailColorHex: "#FFD600",
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("pdf_payloads").doc("free_doc").set({
      documentId: "free_doc",
      accessTier: "free",
      pdfBase64: "JVBERi0xLjQ=",
      pageTexts: ["Page 1"],
      watermarkRequired: true,
      updatedAt: now,
    });
    await db.collection("pdf_documents").doc("prem_doc").set({
      title: "Premium System Design",
      description: "Protected PDF",
      categoryId: "cat_1",
      categoryName: "DSA",
      accessTier: "premium",
      isPublished: true,
      isFeatured: true,
      pageCount: 5,
      fileSizeLabel: "450 KB",
      thumbnailColorHex: "#FFC400",
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("pdf_payloads").doc("prem_doc").set({
      documentId: "prem_doc",
      accessTier: "premium",
      pdfBase64: "JVBERi0xLjQ=",
      pageTexts: ["Secret Page 1"],
      watermarkRequired: true,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true }).firestore();
  await assertSucceeds(aliceDb.collection("pdf_payloads").doc("free_doc").get());
  await assertFails(aliceDb.collection("pdf_payloads").doc("prem_doc").get());

  // Student cannot self-activate subscription
  const future = new Date(Date.now() + 86400000);
  await assertFails(
    aliceDb.collection("subscriptions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      planId: "plan_monthly",
      planName: "Monthly Pro",
      productId: "secure_study_monthly",
      status: "active",
      source: "google_play",
      purchaseToken: "fake_token_1234",
      autoRenewing: true,
      startedAt: now,
      expiresAt: future,
      updatedAt: now,
    })
  );

  // Once admin activates subscription, Alice can read premium PDF payload; Bob still cannot
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("subscriptions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      planId: "plan_monthly",
      planName: "Monthly Pro",
      productId: "secure_study_monthly",
      status: "active",
      source: "google_play",
      purchaseToken: "verified_token_1234",
      autoRenewing: true,
      startedAt: now,
      expiresAt: future,
      updatedAt: now,
    });
  });

  await assertSucceeds(aliceDb.collection("pdf_payloads").doc("prem_doc").get());
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@test.com", email_verified: true }).firestore();
  await assertFails(bobDb.collection("pdf_payloads").doc("prem_doc").get());
});

test("Cross-user isolation on purchases and subscriptions; admin can verify purchases", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: "admin@test.com",
      grantedBy: "system",
      createdAt: now,
    });
    await db.collection("subscription_plans").doc("plan_monthly").set({
      name: "Monthly Vault",
      billingPeriod: "monthly",
      googlePlayProductId: "secure_study_monthly",
      displayPriceInr: 199,
      durationDays: 30,
      badge: "Popular",
      features: ["All PDFs"],
      isAvailable: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Alice cannot submit an already-verified purchase directly
  await assertFails(
    aliceDb.collection("purchases").doc("pur_fake").set({
      userId: ALICE_UID,
      userEmail: "alice@test.com",
      planId: "plan_monthly",
      planName: "Monthly Vault",
      paymentMethod: "phonepe_upi",
      amountInr: 199,
      purchaseToken: "UTR9988776655",
      status: "verified",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice can submit pending_verification purchase
  await assertSucceeds(
    aliceDb.collection("purchases").doc("pur_1").set({
      userId: ALICE_UID,
      userEmail: "alice@test.com",
      planId: "plan_monthly",
      planName: "Monthly Vault",
      paymentMethod: "phonepe_upi",
      amountInr: 199,
      purchaseToken: "UTR9988776655",
      status: "pending_verification",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read Alice's purchase or list unfiltered purchases
  await assertFails(bobDb.collection("purchases").doc("pur_1").get());
  await assertFails(bobDb.collection("purchases").get());
  await assertSucceeds(aliceDb.collection("purchases").where("userId", "==", ALICE_UID).get());

  // Admin can verify Alice's purchase
  await assertSucceeds(
    adminDb.collection("purchases").doc("pur_1").update({
      status: "verified",
      updatedAt: new Date(),
    })
  );
});

test("Email spoofing test: unverified bootstrapped email cannot act as admin", async () => {
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "marotiwalahe@gmail.com",
      email_verified: false,
    })
    .firestore();

  await assertFails(spoofedAdminDb.collection("audit_logs").get());
});
