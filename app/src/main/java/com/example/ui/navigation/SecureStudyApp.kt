package com.example.ui.navigation

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.billing.PlayBillingManager
import com.example.data.model.UiState
import com.example.data.remote.SecureStudyRepository
import com.example.security.BiometricAuthManager
import com.example.security.ConnectivityObserver
import com.example.security.SecurityPreferences
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.auth.EmailVerificationScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.SuspendedAccountScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.dashboard.HomeDashboardScreen
import com.example.ui.dashboard.PdfCategoriesScreen
import com.example.ui.dashboard.PdfDetailsScreen
import com.example.ui.dashboard.StudyLibraryScreen
import com.example.ui.onboarding.BiometricLockScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.onboarding.SplashScreen
import com.example.ui.profile.AppSecuritySettingsScreen
import com.example.ui.profile.HelpAndSupportScreen
import com.example.ui.profile.UserProfileScreen
import com.example.ui.reader.SecurePdfReaderScreen
import com.example.ui.subscription.PaymentStatusScreen
import com.example.ui.subscription.PurchaseHistoryScreen
import com.example.ui.subscription.SubscriptionManagementScreen
import com.example.ui.subscription.SubscriptionPlansScreen
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.LiquidMorphBackground
import com.example.ui.theme.LiquidYellow
import com.example.ui.theme.MutedZinc
import com.example.ui.theme.ObsidianSurface
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.SecureStudyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object RouteDashboard
@Serializable data object RouteCategories
@Serializable data object RouteLibrary
@Serializable data class RoutePdfDetails(val docId: String)
@Serializable data object RouteSubscriptionPlans
@Serializable data object RoutePaymentStatus
@Serializable data class RouteSecureReader(val docId: String)
@Serializable data object RouteProfile
@Serializable data object RouteSubscriptionManagement
@Serializable data object RoutePurchaseHistory
@Serializable data object RouteSecuritySettings
@Serializable data object RouteHelpSupport
@Serializable data object RouteAdminConsole

private enum class TopNavTab(val label: String, val icon: ImageVector, val route: Any) {
    HOME("Home", Icons.Rounded.Home, RouteDashboard),
    CATEGORIES("Subjects", Icons.Rounded.Category, RouteCategories),
    LIBRARY("Library", Icons.Rounded.MenuBook, RouteLibrary),
    PROFILE("Profile", Icons.Rounded.Person, RouteProfile)
}

@Composable
fun SecureStudyAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val securityPreferences = remember(context) { SecurityPreferences(context) }
    val connectivityObserver = remember(context) { ConnectivityObserver(context) }
    val onboardingDone by securityPreferences.onboardingCompleted.collectAsStateWithLifecycle(initialValue = true)
    val isOnline by connectivityObserver.observeIsOnline().collectAsStateWithLifecycle(initialValue = true)

    var showSplash by remember { mutableStateOf(true) }
    var forceOnboarding by remember { mutableStateOf(false) }

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    if (showSplash) {
        SplashScreen(
            onFinished = {
                showSplash = false
                if (!onboardingDone) {
                    forceOnboarding = true
                }
            }
        )
        return
    }

    if (forceOnboarding || !onboardingDone) {
        OnboardingScreen(
            onComplete = {
                forceOnboarding = false
                scope.launch { securityPreferences.setOnboardingCompleted(true) }
            }
        )
        return
    }

    val user = currentUser
    if (user == null) {
        LoginScreen(
            isOnline = isOnline,
            onAuthSuccess = { /* AuthStateListener transitions automatically */ }
        )
    } else {
        key(user.uid) {
            AuthenticatedSecureStudyShell(
                user = user,
                securityPreferences = securityPreferences,
                connectivityObserver = connectivityObserver
            )
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun AuthenticatedSecureStudyShell(
    user: FirebaseUser,
    securityPreferences: SecurityPreferences,
    connectivityObserver: ConnectivityObserver
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val fragmentActivity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: SecureStudyViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SecureStudyViewModel(
                    repository = SecureStudyRepository(db),
                    securityPreferences = securityPreferences,
                    connectivityObserver = connectivityObserver,
                    billingManager = PlayBillingManager(app),
                    currentUserId = user.uid,
                    userEmail = user.email ?: "",
                    displayName = user.displayName ?: "Student",
                    photoUrl = user.photoUrl?.toString(),
                    isEmailVerified = user.isEmailVerified
                )
            }
        }
    )

    // Observe app foreground/background lifecycle for inactivity auto-lock
    DisposableEffect(viewModel) {
        val processLifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                Lifecycle.Event.ON_START -> viewModel.onAppForegrounded()
                else -> Unit
            }
        }
        processLifecycle.addObserver(observer)
        onDispose { processLifecycle.removeObserver(observer) }
    }

    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val categoriesState by viewModel.categories.collectAsStateWithLifecycle()
    val documentsState by viewModel.documents.collectAsStateWithLifecycle()
    val plansState by viewModel.plans.collectAsStateWithLifecycle()
    val userSubscription by viewModel.userSubscription.collectAsStateWithLifecycle()
    val userPurchasesState by viewModel.userPurchases.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()
    val inactivityTimeoutSec by viewModel.inactivityTimeoutSec.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val isVaultLocked by viewModel.isVaultLocked.collectAsStateWithLifecycle()
    val paymentStatus by viewModel.paymentStatusState.collectAsStateWithLifecycle()
    val readerPayloadState by viewModel.readerPayloadState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    var emailVerifiedConfirmed by remember(user.uid) { mutableStateOf(user.isEmailVerified) }
    var biometricPromptError by remember { mutableStateOf<String?>(null) }

    val userProfile = (userProfileState as? UiState.Success)?.data
    if (userProfile?.status == "suspended") {
        SuspendedAccountScreen(
            email = user.email ?: "",
            onSignOut = { signOutUser(credentialManager, scope) }
        )
        return
    }

    if (!emailVerifiedConfirmed) {
        EmailVerificationScreen(
            email = user.email ?: "",
            isEmailVerified = user.isEmailVerified,
            onContinueVerified = { emailVerifiedConfirmed = true },
            onSignOut = { signOutUser(credentialManager, scope) }
        )
        return
    }

    if (isVaultLocked) {
        LaunchedEffect(Unit) {
            if (fragmentActivity != null) {
                BiometricAuthManager.authenticate(
                    activity = fragmentActivity,
                    onSuccess = {
                        biometricPromptError = null
                        viewModel.unlockVault()
                    },
                    onError = { err -> biometricPromptError = err },
                    onCancelled = { biometricPromptError = "Authentication cancelled. Tap below to unlock." }
                )
            }
        }
        BiometricLockScreen(
            errorMessage = biometricPromptError,
            onUnlockRequested = {
                if (fragmentActivity != null) {
                    BiometricAuthManager.authenticate(
                        activity = fragmentActivity,
                        onSuccess = {
                            biometricPromptError = null
                            viewModel.unlockVault()
                        },
                        onError = { err -> biometricPromptError = err },
                        onCancelled = { biometricPromptError = "Authentication cancelled." }
                    )
                }
            },
            onSignOutRequested = {
                viewModel.unlockVault()
                signOutUser(credentialManager, scope)
            }
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val isReaderActive = currentDest?.hasRoute<RouteSecureReader>() == true
    val showTopLevelNav = currentDest?.hasRoute<RouteDashboard>() == true ||
        currentDest?.hasRoute<RouteCategories>() == true ||
        currentDest?.hasRoute<RouteLibrary>() == true ||
        currentDest?.hasRoute<RouteProfile>() == true

    val widthSizeClass = activity?.let { calculateWindowSizeClass(it).widthSizeClass } ?: WindowWidthSizeClass.Compact
    val useNavRail = widthSizeClass != WindowWidthSizeClass.Compact && showTopLevelNav

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        if (!statusBanner.isNullOrBlank()) {
            snackbarHostState.showSnackbar(statusBanner!!)
            viewModel.clearStatusBanner()
        }
    }

    val hasEntitlement = viewModel.hasEntitlement()
    val documents = (documentsState as? UiState.Success)?.data.orEmpty()

    LiquidMorphBackground(intensity = if (isReaderActive) 0.25f else 0.9f) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showTopLevelNav && !useNavRail) {
                    NavigationBar(containerColor = ObsidianSurface) {
                        TopNavTab.entries.forEach { tab ->
                            val selected = when (tab) {
                                TopNavTab.HOME -> currentDest?.hasRoute<RouteDashboard>() == true
                                TopNavTab.CATEGORIES -> currentDest?.hasRoute<RouteCategories>() == true
                                TopNavTab.LIBRARY -> currentDest?.hasRoute<RouteLibrary>() == true
                                TopNavTab.PROFILE -> currentDest?.hasRoute<RouteProfile>() == true
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo<RouteDashboard> { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepBlack,
                                    selectedTextColor = LiquidYellow,
                                    indicatorColor = LiquidYellow,
                                    unselectedIconColor = MutedZinc,
                                    unselectedTextColor = MutedZinc
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(containerColor = ObsidianSurface) {
                        TopNavTab.entries.forEach { tab ->
                            val selected = when (tab) {
                                TopNavTab.HOME -> currentDest?.hasRoute<RouteDashboard>() == true
                                TopNavTab.CATEGORIES -> currentDest?.hasRoute<RouteCategories>() == true
                                TopNavTab.LIBRARY -> currentDest?.hasRoute<RouteLibrary>() == true
                                TopNavTab.PROFILE -> currentDest?.hasRoute<RouteProfile>() == true
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo<RouteDashboard> { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = DeepBlack,
                                    selectedTextColor = LiquidYellow,
                                    indicatorColor = LiquidYellow,
                                    unselectedIconColor = MutedZinc,
                                    unselectedTextColor = MutedZinc
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = RouteDashboard
                    ) {
                        composable<RouteDashboard> {
                            HomeDashboardScreen(
                                displayName = user.displayName ?: "Student",
                                isAdmin = isAdmin,
                                isOnline = isOnline,
                                subscription = userSubscription,
                                hasEntitlement = hasEntitlement,
                                categoriesState = categoriesState,
                                documentsState = documentsState,
                                searchQuery = searchQuery,
                                onSearchChange = viewModel::updateSearchQuery,
                                onNavigateToCategories = { navController.navigate(RouteCategories) },
                                onNavigateToLibrary = { catId ->
                                    viewModel.selectCategory(catId)
                                    navController.navigate(RouteLibrary)
                                },
                                onNavigateToPlans = { navController.navigate(RouteSubscriptionPlans) },
                                onOpenDocument = { doc ->
                                    if (doc.accessTier == "premium" && !hasEntitlement) {
                                        viewModel.logReaderEvent(doc, "denied_no_subscription")
                                        navController.navigate(RouteSubscriptionPlans)
                                    } else {
                                        navController.navigate(RoutePdfDetails(doc.id))
                                    }
                                },
                                onSeedCatalog = { viewModel.initializeCatalogAsAdmin() },
                                onOpenAdmin = { navController.navigate(RouteAdminConsole) }
                            )
                        }

                        composable<RouteCategories> {
                            PdfCategoriesScreen(
                                categoriesState = categoriesState,
                                documentsState = documentsState,
                                onCategorySelected = { catId ->
                                    viewModel.selectCategory(catId)
                                    navController.navigate(RouteLibrary)
                                }
                            )
                        }

                        composable<RouteLibrary> {
                            StudyLibraryScreen(
                                categoriesState = categoriesState,
                                documentsState = documentsState,
                                hasEntitlement = hasEntitlement,
                                searchQuery = searchQuery,
                                selectedCategoryId = selectedCategoryId,
                                onSearchChange = viewModel::updateSearchQuery,
                                onCategoryFilterChange = viewModel::selectCategory,
                                onDocumentClick = { doc ->
                                    if (doc.accessTier == "premium" && !hasEntitlement) {
                                        viewModel.logReaderEvent(doc, "denied_no_subscription")
                                        navController.navigate(RouteSubscriptionPlans)
                                    } else {
                                        navController.navigate(RoutePdfDetails(doc.id))
                                    }
                                }
                            )
                        }

                        composable<RoutePdfDetails> { entry ->
                            val args = entry.toRoute<RoutePdfDetails>()
                            val doc = documents.firstOrNull { it.id == args.docId }
                            val lastPage by viewModel.observeReadingProgress(args.docId)
                                .collectAsStateWithLifecycle(initialValue = 0)
                            PdfDetailsScreen(
                                document = doc,
                                hasEntitlement = hasEntitlement,
                                lastReadPage = lastPage,
                                onBack = { navController.popBackStack() },
                                onOpenReader = { targetDoc ->
                                    viewModel.openDocumentPayload(targetDoc)
                                    navController.navigate(RouteSecureReader(targetDoc.id))
                                },
                                onUnlockSubscription = { navController.navigate(RouteSubscriptionPlans) }
                            )
                        }

                        composable<RouteSecureReader> { entry ->
                            val args = entry.toRoute<RouteSecureReader>()
                            val doc = documents.firstOrNull { it.id == args.docId }
                            val initialPage by viewModel.observeReadingProgress(args.docId)
                                .collectAsStateWithLifecycle(initialValue = 0)
                            SecurePdfReaderScreen(
                                document = doc,
                                payloadState = readerPayloadState,
                                userEmail = user.email ?: user.uid,
                                userId = user.uid,
                                initialPage = initialPage,
                                onSaveProgress = { p -> viewModel.saveReadingProgress(args.docId, p) },
                                onRetryLoad = { if (doc != null) viewModel.openDocumentPayload(doc) },
                                onLogEvent = { action -> if (doc != null) viewModel.logReaderEvent(doc, action) },
                                onBackgroundAutoLock = {
                                    if (biometricEnabled) {
                                        viewModel.lockVaultNow()
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<RouteSubscriptionPlans> {
                            SubscriptionPlansScreen(
                                plansState = plansState,
                                billingManager = viewModel.billingManager,
                                userSubscription = userSubscription,
                                onBack = { navController.popBackStack() },
                                onSubmitPhonePeUtr = { plan, utr ->
                                    viewModel.submitPurchaseRecord(plan, "phonepe_upi", utr)
                                },
                                onNavigateToPaymentStatus = { navController.navigate(RoutePaymentStatus) },
                                onRestorePurchases = {
                                    viewModel.billingManager.queryExistingPurchases()
                                    navController.navigate(RoutePaymentStatus)
                                }
                            )
                        }

                        composable<RoutePaymentStatus> {
                            PaymentStatusScreen(
                                paymentStatus = paymentStatus,
                                userSubscription = userSubscription,
                                onBackToPlans = {
                                    viewModel.clearPaymentStatus()
                                    navController.popBackStack()
                                },
                                onDone = {
                                    viewModel.clearPaymentStatus()
                                    navController.navigate(RouteDashboard) {
                                        popUpTo<RouteDashboard> { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable<RouteProfile> {
                            UserProfileScreen(
                                displayName = user.displayName ?: "Student",
                                email = user.email ?: "",
                                isEmailVerified = user.isEmailVerified,
                                isAdmin = isAdmin,
                                subscription = userSubscription,
                                biometricEnabled = biometricEnabled,
                                onNavigateToSubscriptionManagement = { navController.navigate(RouteSubscriptionManagement) },
                                onNavigateToPurchaseHistory = { navController.navigate(RoutePurchaseHistory) },
                                onNavigateToSecuritySettings = { navController.navigate(RouteSecuritySettings) },
                                onNavigateToHelpSupport = { navController.navigate(RouteHelpSupport) },
                                onNavigateToAdmin = { navController.navigate(RouteAdminConsole) },
                                onSignOut = { signOutUser(credentialManager, scope) }
                            )
                        }

                        composable<RouteSubscriptionManagement> {
                            SubscriptionManagementScreen(
                                subscription = userSubscription,
                                isAdmin = isAdmin,
                                onBack = { navController.popBackStack() },
                                onUpgrade = { navController.navigate(RouteSubscriptionPlans) },
                                onRestorePurchases = {
                                    viewModel.billingManager.queryExistingPurchases()
                                    navController.navigate(RoutePaymentStatus)
                                }
                            )
                        }

                        composable<RoutePurchaseHistory> {
                            PurchaseHistoryScreen(
                                purchasesState = userPurchasesState,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<RouteSecuritySettings> {
                            AppSecuritySettingsScreen(
                                biometricEnabled = biometricEnabled,
                                inactivityTimeoutSec = inactivityTimeoutSec,
                                onToggleBiometric = viewModel::setBiometricLock,
                                onTimeoutChange = viewModel::setInactivityTimeout,
                                onLockVaultNow = viewModel::lockVaultNow,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<RouteHelpSupport> {
                            HelpAndSupportScreen(onBack = { navController.popBackStack() })
                        }

                        composable<RouteAdminConsole> {
                            if (isAdmin) {
                                val adminViewModel: AdminViewModel = viewModel(
                                    key = "admin_${user.uid}",
                                    factory = viewModelFactory {
                                        initializer {
                                            val app = checkNotNull(this[APPLICATION_KEY])
                                            val databaseId = app.getString(R.string.firestore_database_id)
                                            AdminViewModel(SecureStudyRepository(FirebaseFirestore.getInstance(databaseId)))
                                        }
                                    }
                                )
                                val usersState by adminViewModel.users.collectAsStateWithLifecycle()
                                val subsState by adminViewModel.subscriptions.collectAsStateWithLifecycle()
                                val allPurchasesState by adminViewModel.purchases.collectAsStateWithLifecycle()
                                val accessLogsState by adminViewModel.accessLogs.collectAsStateWithLifecycle()
                                val auditLogsState by adminViewModel.auditLogs.collectAsStateWithLifecycle()
                                val actionMsg by adminViewModel.actionMessage.collectAsStateWithLifecycle()
                                val working by adminViewModel.isWorking.collectAsStateWithLifecycle()

                                AdminDashboardScreen(
                                    isAdmin = true,
                                    categoriesState = categoriesState,
                                    documentsState = documentsState,
                                    plansState = plansState,
                                    usersState = usersState,
                                    subscriptionsState = subsState,
                                    purchasesState = allPurchasesState,
                                    accessLogsState = accessLogsState,
                                    auditLogsState = auditLogsState,
                                    actionMessage = actionMsg,
                                    isWorking = working,
                                    onApprovePurchase = adminViewModel::approvePurchase,
                                    onUpdatePurchaseStatus = adminViewModel::updatePurchaseStatus,
                                    onToggleUserSuspension = adminViewModel::toggleUserSuspension,
                                    onToggleAdminRole = adminViewModel::toggleAdminRole,
                                    onUploadPdf = adminViewModel::uploadPdf,
                                    onTogglePdfPublish = adminViewModel::togglePdfPublish,
                                    onTogglePdfTier = adminViewModel::togglePdfTier,
                                    onSavePlan = adminViewModel::savePlan,
                                    onSeedCatalog = adminViewModel::seedCatalog,
                                    onClearMessage = adminViewModel::clearMessage,
                                    onBack = { navController.popBackStack() }
                                )
                            } else {
                                AdminDashboardScreen(
                                    isAdmin = false,
                                    categoriesState = UiState.Loading,
                                    documentsState = UiState.Loading,
                                    plansState = UiState.Loading,
                                    usersState = UiState.Loading,
                                    subscriptionsState = UiState.Loading,
                                    purchasesState = UiState.Loading,
                                    accessLogsState = UiState.Loading,
                                    auditLogsState = UiState.Loading,
                                    actionMessage = null,
                                    isWorking = false,
                                    onApprovePurchase = { _, _ -> },
                                    onUpdatePurchaseStatus = { _, _, _ -> },
                                    onToggleUserSuspension = {},
                                    onToggleAdminRole = {},
                                    onUploadPdf = { _, _, _, _, _, _, _, _, _, _ -> },
                                    onTogglePdfPublish = {},
                                    onTogglePdfTier = {},
                                    onSavePlan = {},
                                    onSeedCatalog = { _, _, _ -> },
                                    onClearMessage = {},
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
