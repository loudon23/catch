package com.loudon23.acatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.loudon23.acatch.ui.theme.CatchTheme
import com.loudon23.acatch.ui.video.VideoDetailScreen
import com.loudon23.acatch.ui.video.VideoListScreen
import com.loudon23.acatch.ui.video.VideoViewModel
import android.net.Uri // Uri import
import android.util.Log // Log import 추가

// 내비게이션 경로 정의
object NavRoutes {
    const val VIDEO_LIST = "video_list"
    const val VIDEO_DETAIL = "video_detail"
    const val VIDEO_URI_KEY = "videoUri"
    const val VIDEO_INDEX_KEY = "videoIndex" // 새로운 인덱스 키 추가
    // 인자를 포함한 전체 경로 (videoIndex는 선택적 쿼리 파라미터로 변경)
    const val VIDEO_DETAIL_WITH_ARG = "${VIDEO_DETAIL}/{${VIDEO_URI_KEY}}?${VIDEO_INDEX_KEY}={${VIDEO_INDEX_KEY}}"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                onNavigateToDetail = { videoUri, videoIndex -> // videoIndex 인자 추가
                                    // 내비게이션 시도 시 로그 출력
                                    Log.d("MainActivity", "Attempting to navigate to detail for URI: $videoUri, index: $videoIndex")
                                    val encodedUri = Uri.encode(videoUri)
                                    // videoIndex를 쿼리 파라미터로 전달
                                    navController.navigate("${NavRoutes.VIDEO_DETAIL}/$encodedUri?${NavRoutes.VIDEO_INDEX_KEY}=$videoIndex")
                                }
                            )
                        }
                        composable(
                            route = NavRoutes.VIDEO_DETAIL_WITH_ARG, // 상수로 정의된 경로 사용
                            arguments = listOf(
                                navArgument(NavRoutes.VIDEO_URI_KEY) { type = NavType.StringType; nullable = true },
                                navArgument(NavRoutes.VIDEO_INDEX_KEY) { type = NavType.IntType; defaultValue = -1 } // IntType으로 변경 및 기본값 설정
                            )
                        ) { backStackEntry ->
                            val videoUri = backStackEntry.arguments?.getString(NavRoutes.VIDEO_URI_KEY)
                            val videoIndex = backStackEntry.arguments?.getInt(NavRoutes.VIDEO_INDEX_KEY) ?: -1 // IntType으로 받아옴

                            VideoDetailScreen(
                                videoUri = videoUri,
                                videoIndex = videoIndex, // videoIndex 전달
                                videoViewModel = videoViewModel,
                                onDeleteVideo = { videoItem ->
                                    videoViewModel.deleteVideo(videoItem)
                                    navController.popBackStack()
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                                // onNavigateToNextVideo 콜백은 VerticalPager가 처리하므로 더 이상 필요 없습니다. 이전에 제거되었습니다.
                            )
                        }
                    }
                }
            }
        }
    }
}