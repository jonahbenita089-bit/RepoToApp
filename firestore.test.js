const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write profiles or saved_repos", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      preferredContactType: "GOOGLE",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("saved_repos").get()
  );
});

test("Owner can create and partially update UserProfile with Google, Email, and Phone fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Dev",
      preferredContactType: "EMAIL",
      contactEmail: "alice@example.com",
      phoneNumber: "+15551234567",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredContactType: "PHONE",
      phoneNumber: "+15559876543",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      preferredContactType: "GOOGLE",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user isolation: Bob cannot read or mutate Alice profile or saved_repos", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Dev",
      preferredContactType: "EMAIL",
      contactEmail: "alice@example.com",
      createdAt: created,
      updatedAt: created,
    });
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_repos")
      .doc("flutter_samples")
      .set({
        fullName: "flutter/samples",
        owner: "flutter",
        repo: "samples",
        description: "Flutter samples",
        defaultBranch: "main",
        detectedStack: "FLUTTER",
        avatarUrl: "https://avatars.githubusercontent.com/u/14101776",
        htmlUrl: "https://github.com/flutter/samples",
        supportsApk: true,
        supportsIpa: true,
        createdAt: created,
        updatedAt: created,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved_repos").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_repos")
      .doc("flutter_samples")
      .delete()
  );
});

test("Owner can list, create, update, and delete saved_repos with Kotlin-mirrored payload", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Empty subcollection list must succeed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_repos").get()
  );

  // Create without redundant `id` field (@DocumentId excluded)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_repos")
      .doc("flutter_samples")
      .set({
        fullName: "flutter/samples",
        owner: "flutter",
        repo: "samples",
        description: "Flutter samples",
        defaultBranch: "main",
        detectedStack: "FLUTTER",
        avatarUrl: "https://avatars.githubusercontent.com/u/14101776",
        htmlUrl: "https://github.com/flutter/samples",
        supportsApk: true,
        supportsIpa: true,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject payload containing redundant @DocumentId `id` key
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_repos")
      .doc("android_nowinandroid")
      .set({
        id: "android_nowinandroid",
        fullName: "android/nowinandroid",
        owner: "android",
        repo: "nowinandroid",
        description: "Now in Android",
        defaultBranch: "main",
        detectedStack: "ANDROID_NATIVE",
        avatarUrl: "",
        htmlUrl: "https://github.com/android/nowinandroid",
        supportsApk: true,
        supportsIpa: false,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_repos")
      .doc("flutter_samples")
      .update({
        defaultBranch: "master",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_repos")
      .doc("flutter_samples")
      .delete()
  );
});
