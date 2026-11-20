package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.repository.AppContainer
import com.example.ui.containers.ContainersScreen
import com.example.ui.containers.ContainersViewModel
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.meal.MealLoggerScreen
import com.example.ui.meal.MealLoggerViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.table.MasterTableScreen
import com.example.ui.table.MasterTableViewModel
import com.example.ui.weight.WeightSimulatorScreen
import com.example.ui.weight.WeightSimulatorViewModel
import kotlinx.serialization.Serializable

@Serializable data object DashboardRoute
@Serializable data class MealLoggerRoute(val logicalDate: String)
@Serializable data object ContainersRoute
@Serializable data object WeightRoute
@Serializable data object TableRoute
@Serializable data object SettingsRoute

private data class TopDestination(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TOP_DESTINATIONS = listOf(
    TopDestination(DashboardRoute, "Hoy", Icons.Filled.Home, Icons.Outlined.Home, "nav_today"),
    TopDestination(ContainersRoute, "Tuppers", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_containers"),
    TopDestination(WeightRoute, "Báscula", Icons.Filled.MonitorWeight, Icons.Outlined.MonitorWeight, "nav_weight"),
    TopDestination(TableRoute, "Tabla", Icons.Filled.TableChart, Icons.Outlined.TableChart, "nav_table"),
    TopDestination(SettingsRoute, "Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun AppNavigation(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevel = currentDestination == null || !currentDestination.hasRoute<MealLoggerRoute>()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (isTopLevel && !useRail) {
                    NavigationBar {
                        TOP_DESTINATIONS.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
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
                if (isTopLevel && useRail) {
                    NavigationRail {
                        TOP_DESTINATIONS.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = DashboardRoute
                    ) {
                        composable<DashboardRoute> {
                            val vm: DashboardViewModel = viewModel(
                                factory = DashboardViewModel.Factory(container.repository)
                            )
                            val state by vm.uiState.collectAsStateWithLifecycle()
                            val context = androidx.compose.ui.platform.LocalContext.current
                            DashboardScreen(
                                uiState = state,
                                onShiftDate = vm::shiftDate,
                                onSelectToday = vm::selectToday,
                                onOpenMealLogger = { date ->
                                    navController.navigate(MealLoggerRoute(date))
                                },
                                onAdjustWater = { delta -> vm.adjustWaterGlasses(context, delta) },
                                onToggleExamMode = vm::toggleExamSurvivalMode,
                                onQuickAcceptHabit = { habit -> vm.quickAcceptRepeatedHabit(context, habit) },
                                onCloseDay = { vm.closeCurrentDay(context) },
                                onReopenDay = vm::reopenCurrentDay,
                                onEditMeal = vm::editMeal,
                                onReassignToYesterday = vm::reassignMealToYesterday,
                                onDeleteMeal = vm::deleteMeal,
                                onQuickAddFavorite = { fav -> vm.quickAddFavorite(context, fav) },
                                onRetryQueue = vm::retryOfflineQueue,
                                onDismissAchievementBanner = vm::dismissAchievementBanner,
                                onClearFeedback = vm::clearFeedback
                            )
                        }

                        composable<MealLoggerRoute> { entry ->
                            val route = entry.toRoute<MealLoggerRoute>()
                            val vm: MealLoggerViewModel = viewModel(
                                factory = MealLoggerViewModel.Factory(container.repository, route.logicalDate)
                            )
                            val state by vm.uiState.collectAsStateWithLifecycle()
                            val context = androidx.compose.ui.platform.LocalContext.current
                            MealLoggerScreen(
                                uiState = state,
                                onModeChange = vm::setWorkbenchMode,
                                onTitleChange = vm::updateTitle,
                                onDescriptionChange = vm::updateDescription,
                                onSlotChange = vm::updateSlot,
                                onContainerChange = vm::updateContainer,
                                onFillPercentChange = vm::updateFillPercent,
                                onBatchServingsChange = vm::updateBatchServings,
                                onCostChange = vm::updateEstimatedCost,
                                onFavoriteChange = vm::updateSaveAsFavorite,
                                onQuickAcceptHabit = vm::quickAcceptHabitForSlot,
                                onBarcodeScanned = vm::onBarcodeDetected,
                                onPhotoPicked = { uri -> vm.attachPickedPhoto(context, uri) },
                                onPhotoCapturedFile = { file -> vm.attachCapturedFile(context, file) },
                                onRemovePhoto = { vm.removePhoto(context) },
                                onSimulate = vm::simulateImpact,
                                onSelectScenario = vm::selectEstimationScenario,
                                onAdjustSimulatedPortion = vm::adjustSimulatedPortion,
                                onDiscardSimulation = vm::discardSimulation,
                                onConfirmSimulatedMeal = vm::confirmSimulatedMeal,
                                onQueueOfflineMeal = vm::queueOfflineMeal,
                                onOptionADescChange = vm::updateOptionADescription,
                                onOptionAPhotoPicked = { uri -> vm.attachOptionAPhoto(context, uri) },
                                onOptionBDescChange = vm::updateOptionBDescription,
                                onOptionBPhotoPicked = { uri -> vm.attachOptionBPhoto(context, uri) },
                                onCompareOptions = vm::compareOptionsAAndB,
                                onAcceptComparisonOption = vm::acceptComparisonOption,
                                onPantryIngredientsChange = vm::updatePantryIngredients,
                                onPantryPhotoPicked = { uri -> vm.attachPantryPhoto(context, uri) },
                                onGeneratePantryRecipes = vm::generatePantryRecipes,
                                onAcceptPantryRecipe = vm::acceptPantryRecipe,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable<ContainersRoute> {
                            val vm: ContainersViewModel = viewModel(
                                factory = ContainersViewModel.Factory(container.repository)
                            )
                            val state by vm.uiState.collectAsStateWithLifecycle()
                            ContainersScreen(
                                uiState = state,
                                onOpenNew = vm::openNewEditor,
                                onOpenEdit = vm::openEditEditor,
                                onCloseEditor = vm::closeEditor,
                                onNameChange = vm::updateName,
                                onShapeChange = vm::updateShape,
                                onRadiusChange = vm::updateRadius,
                                onHeightChange = vm::updateHeight,
                                onLengthChange = vm::updateLength,
                                onWidthChange = vm::updateWidth,
                                onPreviewFillChange = vm::updatePreviewFill,
                                onSaveEditor = vm::saveEditor,
                                onToggleFavorite = vm::toggleFavorite,
                                onDeleteContainer = vm::deleteContainer,
                                onAddStudentPresets = vm::addStudentPresets
                            )
                        }

                        composable<WeightRoute> {
                            val vm: WeightSimulatorViewModel = viewModel(
                                factory = WeightSimulatorViewModel.Factory(container.repository)
                            )
                            val state by vm.uiState.collectAsStateWithLifecycle()
                            val context = androidx.compose.ui.platform.LocalContext.current
                            WeightSimulatorScreen(
                                uiState = state,
                                onSubmitWeight = { w -> vm.submitWeight(context, w) },
                                onConfirmOutlier = { vm.confirmOutlierWeight(context) },
                                onDismissOutlier = vm::dismissOutlierDialog,
                                onDeleteWeight = vm::deleteWeight,
                                onUpdateSimulatedDeficit = vm::updateSimulatedDeficit,
                                onUpdateProjectionWeeks = vm::updateProjectionWeeks,
                                onUpdateCheatExtraKcal = vm::updateCheatExtraKcal,
                                onUpdateCheatSpreadDays = vm::updateCheatSpreadDays,
                                onApplySimulatedDeficit = vm::applySimulatedDeficitToProfile,
                                onApplyCalibration = vm::applyCalibrationSuggestion,
                                onDismissCelebration = vm::dismissCelebration,
                                onClearFeedback = vm::clearFeedback
                            )
                        }

                        composable<TableRoute> {
                            val vm: MasterTableViewModel = viewModel(
                                factory = MasterTableViewModel.Factory(container.repository)
                            )
                            val state by vm.uiState.collectAsStateWithLifecycle()
                            MasterTableScreen(
                                uiState = state,
                                onSelectFilter = vm::selectFilter,
                                onCustomRangeApply = vm::updateCustomRange,
                                onExportExcel = vm::exportExcel,
                                onImportExcel = vm::importExcel,
                                onClearFeedback = vm::clearFeedback
                            )
                        }

                        composable<SettingsRoute> {
                            val vm: SettingsViewModel = viewModel(
                                factory = SettingsViewModel.Factory(container.repository)
                            )
                            val state by vm.uiState.collectAsStateWithLifecycle()
                            SettingsScreen(
                                uiState = state,
                                onAdaptRoutineWithGemini = vm::adaptSpecificRoutineWithGemini,
                                onSaveProfile = vm::saveProfile,
                                onExportExcel = vm::exportExcel,
                                onImportExcel = vm::importExcel,
                                onClearFeedback = vm::clearFeedback
                            )
                        }
                    }
                }
            }
        }
    }
}
