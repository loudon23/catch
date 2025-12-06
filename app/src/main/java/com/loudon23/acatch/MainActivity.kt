package com.loudon23.acatch

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.loudon23.acatch.ui.theme.CatchTheme
import com.loudon23.acatch.ui.video.VideoDetailScreen
import com.loudon23.acatch.ui.video.VideoListScreen
import com.loudon23.acatch.ui.video.VideoViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// 내비게이션 경로 정의
object NavRoutes {
    const val VIDEO_LIST = "video_list"
    const val VIDEO_DETAIL = "video_detail"
    const val FOLDER_URI_KEY = "folderUri"
    const val VIDEO_URI_KEY = "videoUri"
    const val VIDEO_INDEX_KEY = "videoIndex" // 새로운 인덱스 키 추가
    // 인자를 포함한 전체 경로 (videoIndex는 선택적 쿼리 파라미터로 변경)
    const val VIDEO_DETAIL_WITH_ARG = "${VIDEO_DETAIL}/{${FOLDER_URI_KEY}}/{${VIDEO_URI_KEY}}?${VIDEO_INDEX_KEY}={${VIDEO_INDEX_KEY}}"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        setContent {
            CatchTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val videoViewModel: VideoViewModel = viewModel() // ViewModel 인스턴스 생성

                    NavHost(navController = navController, startDestination = NavRoutes.VIDEO_LIST) {
                        composable(NavRoutes.VIDEO_LIST) {
                            VideoListScreen(
                                videoViewModel = videoViewModel,
                                onNavigateToDetail = { folderUri, videoUri, videoIndex -> // videoIndex 인자 추가
                                    // 내비게이션 시도 시 로그 출력
                                    Log.d("MainActivity", "Attempting to navigate to detail for URI: $videoUri, index: $videoIndex")
                                    val encodedFolderUri = Uri.encode(folderUri)
                                    val encodedVideoUri = Uri.encode(videoUri)
                                    // videoIndex를 쿼리 파라미터로 전달
                                    Log.d("MainActivity", "${NavRoutes.VIDEO_DETAIL}/$encodedFolderUri/$encodedVideoUri?${NavRoutes.VIDEO_INDEX_KEY}=$videoIndex")
                                    navController.navigate("${NavRoutes.VIDEO_DETAIL}/$encodedFolderUri/$encodedVideoUri?${NavRoutes.VIDEO_INDEX_KEY}=$videoIndex")
                                }
                            )
                        }
                        composable(
                            route = NavRoutes.VIDEO_DETAIL_WITH_ARG, // 상수로 정의된 경로 사용
                            arguments = listOf(
                                navArgument(NavRoutes.FOLDER_URI_KEY) { type = NavType.StringType },
                                navArgument(NavRoutes.VIDEO_URI_KEY) { type = NavType.StringType; nullable = true },
                                navArgument(NavRoutes.VIDEO_INDEX_KEY) { type = NavType.IntType; defaultValue = -1 } // IntType으로 변경 및 기본값 설정
                            )
                        ) { backStackEntry ->
                            val folderUri = backStackEntry.arguments?.getString(NavRoutes.FOLDER_URI_KEY) ?: ""
                            val videoUri = backStackEntry.arguments?.getString(NavRoutes.VIDEO_URI_KEY)
                            val videoIndex = backStackEntry.arguments?.getInt(NavRoutes.VIDEO_INDEX_KEY) ?: -1 // IntType으로 받아옴

                            VideoDetailScreen(
                                folderUri = folderUri,
                                videoUri = videoUri,
                                videoIndex = videoIndex, // videoIndex 전달
                                videoViewModel = videoViewModel,
                                onDeleteVideo = { videoItem ->
                                    videoViewModel.deleteVideo(videoItem)
                                    // No need to popBackStack here as pager will handle it
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}