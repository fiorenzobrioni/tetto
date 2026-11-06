package com.callbackdev.tetto.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp
import com.callbackdev.tetto.core.model.CapIcon

/**
 * The cap palette (PLANNING.md §11 Phase 2): one Material Symbol for each [CapIcon], the curated
 * set and never the whole library, so the caps screen keeps one look. Built once, on first use.
 */
val CapIcon.symbol: ImageVector get() = capSymbols.getValue(this)

/** The symbol's path data on its 960-unit grid ([SymbolPaths]), for a drawing outside Compose. */
val CapIcon.pathData: String get() = pathFor(this)

private val capSymbols: Map<CapIcon, ImageVector> by lazy {
    CapIcon.entries.associateWith { symbol(it.key, pathFor(it)) }
}

private fun pathFor(icon: CapIcon): String = when (icon) {
    CapIcon.GROCERIES -> SymbolPaths.SHOPPING_CART
    CapIcon.EATING_OUT -> SymbolPaths.RESTAURANT
    CapIcon.COFFEE -> SymbolPaths.LOCAL_CAFE
    CapIcon.SHOPPING -> SymbolPaths.SHOPPING_BAG
    CapIcon.FUN -> SymbolPaths.CELEBRATION
    CapIcon.TRANSPORT -> SymbolPaths.DIRECTIONS_BUS
    CapIcon.FUEL -> SymbolPaths.LOCAL_GAS_STATION
    CapIcon.GIFTS -> SymbolPaths.REDEEM
    CapIcon.PERSONAL_CARE -> SymbolPaths.SPA
    CapIcon.PETS -> SymbolPaths.PETS
    CapIcon.TRAVEL -> SymbolPaths.FLIGHT
    CapIcon.HOME -> SymbolPaths.HOME
    CapIcon.BILLS -> SymbolPaths.RECEIPT_LONG
    CapIcon.HEALTH -> SymbolPaths.MEDICAL_SERVICES
    CapIcon.KIDS -> SymbolPaths.CHILD_CARE
    CapIcon.CLOTHES -> SymbolPaths.CHECKROOM
    CapIcon.BOOKS -> SymbolPaths.MENU_BOOK
    CapIcon.SPORT -> SymbolPaths.FITNESS_CENTER
    CapIcon.CAR -> SymbolPaths.DIRECTIONS_CAR
    CapIcon.TECH -> SymbolPaths.DEVICES
    CapIcon.SUBSCRIPTIONS -> SymbolPaths.SUBSCRIPTIONS
    CapIcon.HOBBIES -> SymbolPaths.PALETTE
    CapIcon.EDUCATION -> SymbolPaths.SCHOOL
    CapIcon.TAKEAWAY -> SymbolPaths.TAKEOUT_DINING
    CapIcon.DRINKS -> SymbolPaths.LOCAL_BAR
    CapIcon.GAMES -> SymbolPaths.SPORTS_ESPORTS
    CapIcon.EVENTS -> SymbolPaths.LOCAL_ACTIVITY
    CapIcon.BANK -> SymbolPaths.ACCOUNT_BALANCE
    CapIcon.RENT -> SymbolPaths.APARTMENT
    CapIcon.OTHER -> SymbolPaths.CATEGORY
}

/** Material Symbols' grid: 960 units, y from -960 to 0, so the drawing is moved down by its height. */
private fun symbol(name: String, path: String): ImageVector = ImageVector.Builder(
    name = "cap_$name",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = GRID,
    viewportHeight = GRID,
).apply {
    group(translationY = GRID) {
        addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
    }
}.build()

private const val GRID = 960f
