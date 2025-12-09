package com.jonathan.arberlin.ui.navigation

import androidx.annotation.DrawableRes
import com.jonathan.arberlin.R

enum class Screen(val route: String, val title: String, @DrawableRes val activeIcon: Int, @DrawableRes val inactiveIcon: Int)
{
    Map("map", "Map", R.drawable.map_active, R.drawable.map_passive),
    AR("ar", "AR Camera", R.drawable.camera_active, R.drawable.camera_passive),
    Collections("collections", "Collections", R.drawable.collections_active, R.drawable.collections_passive)
}