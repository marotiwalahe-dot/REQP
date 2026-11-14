package com.example.data.remote

import com.example.security.PdfGenerator

object CatalogSeeder {

    suspend fun seedDefaultCatalogIfEmpty(
        repository: SecureStudyRepository,
        existingCategoryCount: Int,
        existingPlanCount: Int,
        existingDocCount: Int
    ): Result<String> = runCatching {
        var createdCount = 0

        if (existingCategoryCount == 0) {
            val categories = listOf(
                Triple("cat_dsa", "Algorithms & DSA", "Data structures, dynamic programming, and graph theory blueprints"),
                Triple("cat_sysdesign", "System Design", "Distributed architecture, caching, sharding, and consistency models"),
                Triple("cat_security", "App Security", "Cryptography, Android Keystore, OAuth2, and zero-trust architecture"),
                Triple("cat_android", "Android Internals", "Jetpack Compose, Coroutines, Binder IPC, and performance profiling"),
                Triple("cat_examprep", "Interview Vault", "Curated technical interview handbooks and solved problem sets")
            )
            categories.forEachIndexed { index, (id, name, desc) ->
                val icon = when (index) {
                    0 -> "code"
                    1 -> "hub"
                    2 -> "shield"
                    3 -> "smartphone"
                    else -> "school"
                }
                repository.saveCategory(
                    categoryId = id,
                    name = name,
                    description = desc,
                    iconName = icon,
                    sortOrder = (index + 1).toLong(),
                    isActive = true
                ).getOrThrow()
                createdCount++
            }
        }

        if (existingPlanCount == 0) {
            repository.saveSubscriptionPlan(
                planId = "plan_monthly",
                name = "Monthly Pass",
                billingPeriod = "monthly",
                googlePlayProductId = "secure_study_monthly",
                displayPriceInr = 199L,
                durationDays = 30L,
                badge = "FLEXIBLE",
                features = listOf(
                    "Full access to all Premium PDF Vault materials",
                    "Dynamic watermarked offline-safe reader",
                    "Biometric vault lock & reading progress sync",
                    "Cancel anytime via Google Play"
                ),
                isAvailable = true
            ).getOrThrow()

            repository.saveSubscriptionPlan(
                planId = "plan_quarterly",
                name = "Quarterly Pro",
                billingPeriod = "quarterly",
                googlePlayProductId = "secure_study_quarterly",
                displayPriceInr = 499L,
                durationDays = 90L,
                badge = "POPULAR • SAVE 16%",
                features = listOf(
                    "90 days uninterrupted Premium PDF access",
                    "Priority access to newly published study guides",
                    "In-document search & encrypted temp cache",
                    "Google Play Billing & PhonePe UPI supported"
                ),
                isAvailable = true
            ).getOrThrow()

            repository.saveSubscriptionPlan(
                planId = "plan_yearly",
                name = "Annual Vault",
                billingPeriod = "yearly",
                googlePlayProductId = "secure_study_yearly",
                displayPriceInr = 1499L,
                durationDays = 365L,
                badge = "BEST VALUE • SAVE 37%",
                features = listOf(
                    "365 days unlimited access to entire library",
                    "All upcoming Exam Prep & System Design releases",
                    "Zero-trust device biometric protection",
                    "Dedicated priority support"
                ),
                isAvailable = true
            ).getOrThrow()
            createdCount += 3
        }

        if (existingDocCount == 0) {
            val seedDocs = listOf(
                SeedDocSpec(
                    id = "doc_free_dsa_starter",
                    title = "Big-O & Core Data Structures Primer",
                    description = "Essential time/space complexity charts, arrays, stacks, queues, and hash table internals for fast revision.",
                    categoryId = "cat_dsa",
                    categoryName = "Algorithms & DSA",
                    accessTier = "free",
                    isFeatured = true,
                    colorHex = "#FFD600",
                    sections = listOf(
                        "Asymptotic Complexity Foundations" to "Big-O notation bounds worst-case growth rates as input size N scales toward infinity.\n\nO(1) Constant: HashMap lookup, array index access.\nO(log N) Logarithmic: Binary search on sorted arrays, balanced BST height.\nO(N) Linear: Single pass two-pointer scans.\nO(N log N) Linearithmic: Merge Sort, QuickSort average, Heap Sort.\n\nAlways evaluate both time complexity and auxiliary stack/heap space.",
                        "Hash Tables & Collision Resolution" to "A well-tuned hash table achieves O(1) expected lookup by mapping keys via a deterministic hash function into buckets.\n\nChaining stores linked buckets per slot; open addressing probes linearly or quadratically. Resize when load factor exceeds 0.75 to avoid clustering degradation.",
                        "Stacks, Queues & Monotonic Patterns" to "Monotonic stacks solve Next Greater Element, Largest Rectangle in Histogram, and daily temperature span problems in O(N) amortized time because each element is pushed and popped at most once."
                    )
                ),
                SeedDocSpec(
                    id = "doc_free_security_basics",
                    title = "Android App Hardening Checklist",
                    description = "Free starter guide covering FLAG_SECURE, Keystore AES-GCM encryption, and BiometricPrompt best practices.",
                    categoryId = "cat_security",
                    categoryName = "App Security",
                    accessTier = "free",
                    isFeatured = false,
                    colorHex = "#FFC400",
                    sections = listOf(
                        "Window Protection & FLAG_SECURE" to "Setting WindowManager.LayoutParams.FLAG_SECURE prevents standard system screenshots, screen recording capture, and recent-apps thumbnail leaks.\n\nWhile FLAG_SECURE raises a strong platform barrier, defense-in-depth combines it with dynamic user-bound watermarks and short-lived encrypted temp caches.",
                        "Android Keystore & BiometricPrompt" to "Hardware-backed Keystore keys never leave the Secure Element or TEE. Combining BiometricPrompt with BIOMETRIC_STRONG or DEVICE_CREDENTIAL ensures biometric templates stay strictly on device and are never transmitted to remote servers."
                    )
                ),
                SeedDocSpec(
                    id = "doc_prem_system_design",
                    title = "Distributed Systems & Sharding Playbook",
                    description = "Deep dive into consistent hashing, Raft consensus, read replicas, write-ahead logs, and backpressure control.",
                    categoryId = "cat_sysdesign",
                    categoryName = "System Design",
                    accessTier = "premium",
                    isFeatured = true,
                    colorHex = "#FFD600",
                    sections = listOf(
                        "Consistent Hashing & Virtual Nodes" to "Traditional modulo hashing remaps nearly all keys when a cache shard joins or leaves. Consistent hashing maps nodes and keys onto a 2^32 ring, limiting key movement to K/N keys.\n\nVirtual nodes (vnodes) assign multiple ring positions per physical server, eliminating hot-spot skew across heterogeneous hardware.",
                        "Replication, Quorums & CAP Trade-offs" to "Leader-follower replication scales read throughput while maintaining linearizable reads on the leader. Quorum systems set W + R > N so at least one replica in the read set holds the latest write.\n\nUnder network partitions, systems choose between Consistency (CP, e.g., Raft/Paxos) and Availability (AP, e.g., Dynamo-style eventual consistency).",
                        "Rate Limiting & Backpressure" to "Token bucket and sliding window log algorithms protect downstream databases from burst traffic. Pair distributed Redis Lua rate limiters with exponential backoff and jitter on client retries to prevent thundering herd failures."
                    )
                ),
                SeedDocSpec(
                    id = "doc_prem_dp_mastery",
                    title = "Dynamic Programming Patterns Mastery",
                    description = "State transition blueprints for Knapsack, Interval DP, Bitmask DP, and Tree DP with complexity proofs.",
                    categoryId = "cat_dsa",
                    categoryName = "Algorithms & DSA",
                    accessTier = "premium",
                    isFeatured = true,
                    colorHex = "#FFAB00",
                    sections = listOf(
                        "Recognizing Optimal Substructure" to "Dynamic programming applies whenever a problem exhibits overlapping subproblems and optimal substructure.\n\nStart by defining the minimal state dp[i][j] clearly in plain English, write the recurrence relation, verify base cases, and determine iteration order before optimizing space.",
                        "0/1 vs Unbounded Knapsack" to "0/1 Knapsack iterates capacity backwards (from W down to weight[i]) in 1D space optimization so each item is used at most once.\n\nUnbounded Knapsack iterates capacity forwards (from weight[i] up to W), allowing unlimited reuse of each item.",
                        "Bitmask & Tree DP" to "When N <= 20, represent subsets as integer bitmasks (0 until 1 shl N). For Tree DP, compute subtree aggregates bottom-up via post-order DFS and apply rerooting when answers depend on every node as root."
                    )
                ),
                SeedDocSpec(
                    id = "doc_prem_compose_internals",
                    title = "Jetpack Compose Runtime & Stability",
                    description = "Slot table internals, snapshot state system, skippable composables, and 60fps Canvas rendering optimization.",
                    categoryId = "cat_android",
                    categoryName = "Android Internals",
                    accessTier = "premium",
                    isFeatured = false,
                    colorHex = "#FFD600",
                    sections = listOf(
                        "Compose Phases: Composition, Layout, Draw" to "Every frame passes through three phases: Composition (what UI to show), Layout (where to place it), and Draw (rendering pixels).\n\nDefer state reads as late as possible—reading animated offsets inside lambda modifiers like Modifier.offset { } or Canvas drawBehind skips Composition and Layout completely for 60 FPS animations.",
                        "Snapshot System & Strong Skipping" to "Compose tracks state reads automatically via Snapshot. MutableState writes invalidate only the narrowest RecomposeScope that read that property."
                    )
                ),
                SeedDocSpec(
                    id = "doc_prem_zero_trust",
                    title = "Zero-Trust Subscription & Billing Security",
                    description = "Server-side Google Play purchase token verification, RTDN webhooks, idempotency keys, and Firestore RLS.",
                    categoryId = "cat_examprep",
                    categoryName = "Interview Vault",
                    accessTier = "premium",
                    isFeatured = true,
                    colorHex = "#FFC400",
                    sections = listOf(
                        "Never Trust Client Entitlements" to "Client applications can be inspected or tampered with on rooted devices. Never unlock premium resources using a local SharedPreferences boolean.\n\nAlways send the Google Play purchaseToken to a trusted backend verifier that queries the Google Play Developer API, validates packageName, productId, and purchaseState, and writes an authoritative entitlement record.",
                        "Idempotency & Real-Time Developer Notifications" to "Store purchaseToken as a unique transaction key to prevent replay attacks. Subscribe to Google Play RTDN (Pub/Sub) to handle SUBSCRIPTION_RENEWED, SUBSCRIPTION_CANCELED, SUBSCRIPTION_REVOKED, and SUBSCRIPTION_EXPIRED events automatically."
                    )
                )
            )

            seedDocs.forEach { spec ->
                val generated = PdfGenerator.createStudyPdf(
                    title = spec.title,
                    subject = spec.categoryName,
                    sections = spec.sections
                )
                repository.uploadPdfDocumentAndPayload(
                    documentId = spec.id,
                    title = spec.title,
                    description = spec.description,
                    categoryId = spec.categoryId,
                    categoryName = spec.categoryName,
                    accessTier = spec.accessTier,
                    isPublished = true,
                    isFeatured = spec.isFeatured,
                    pageCount = generated.pageCount.toLong(),
                    fileSizeLabel = generated.fileSizeLabel,
                    thumbnailColorHex = spec.colorHex,
                    pdfBase64 = generated.pdfBase64,
                    pageTexts = generated.pageTexts
                ).getOrThrow()
                createdCount++
            }
        }

        "Seeded $createdCount items into Secure Study Firestore."
    }

    private data class SeedDocSpec(
        val id: String,
        val title: String,
        val description: String,
        val categoryId: String,
        val categoryName: String,
        val accessTier: String,
        val isFeatured: Boolean,
        val colorHex: String,
        val sections: List<Pair<String, String>>
    )
}
