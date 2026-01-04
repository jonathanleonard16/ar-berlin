package com.jonathan.arberlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

import com.jonathan.arberlin.ui.theme.ARBerlinTheme

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.jonathan.arberlin.features.ar.ARRoute
import com.jonathan.arberlin.features.collections.CollectionViewModel
import com.jonathan.arberlin.features.collections.CollectionsScreen
import com.jonathan.arberlin.features.collections.PoiDetailScreen
import com.jonathan.arberlin.ui.navigation.Screen
import com.jonathan.arberlin.features.map.MapRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ARBerlinTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
                ARBerlinAppContent()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ARBerlinTheme {
        Greeting("Android")
    }
}

@Composable
fun ARBerlinAppContent() {
    val navController = rememberNavController()


    val activity = LocalContext.current as ComponentActivity

    val sharedViewModel: CollectionViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = CollectionViewModel.Factory
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Screen.entries.forEach { screen ->

                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(
                                    id = if (isSelected) screen.activeIcon else screen.inactiveIcon
                                ),
                                contentDescription = screen.title
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
        ) {
            composable(Screen.Map.route) {
                Box(modifier = Modifier.padding(innerPadding)) {
                    MapRoute()
                }
            }
            composable(Screen.AR.route) {
                Box(modifier = Modifier.padding(innerPadding)) {
                    ARRoute()
                }
            }
            composable(Screen.Collections.route) {
                val list by sharedViewModel.collectionState.collectAsState()

                CollectionsScreen(
                    discoveredPois = list,
                    onPoiClick = { poiId ->
                        navController.navigate("poi_detail/$poiId")
                    },
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
            }

            composable(
                route = "poi_detail/{poiId}",
                arguments = listOf(navArgument("poiId") { type = NavType.LongType })
            ) { backStackEntry ->

                val poiId = backStackEntry.arguments?.getLong("poiId")

                val poi = poiId?.let { sharedViewModel.getPoiDetail(it) }

                if (poi != null) {
                    PoiDetailScreen(
                        poi = poi,
                        onBackClick = { navController.popBackStack() },
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    )
                }

            }
        }
    }
}