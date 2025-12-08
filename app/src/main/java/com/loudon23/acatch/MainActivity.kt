package com.loudon23.acatch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.loudon23.acatch.ui.theme.CatchTheme
import com.loudon23.acatch.ui.video.detail.VideoDetailScreen
import com.loudon23.acatch.ui.video.list.FolderListScreen
import com.loudon23.acatch.ui.video.VideoViewModel
import java.util.concurrent.Executors
import androidx.compose.runtime.*

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

class MainActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // 인증 상태를 추적하는 Compose State
    private var isAuthenticated by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        setupBiometricPrompt()
        // authenticateWithBiometrics() // 앱 시작 시 생체 인증 시작

        setContent {
            CatchTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // isAuthenticated가 true일 때만 앱의 실제 콘텐츠를 표시
                    if (isAuthenticated) {
                        val navController = rememberNavController()
                        val videoViewModel: VideoViewModel = viewModel() // ViewModel 인스턴스 생성

                        NavHost(navController = navController, startDestination = NavRoutes.VIDEO_LIST) {
                            composable(NavRoutes.VIDEO_LIST) {
                                FolderListScreen(
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
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupBiometricPrompt() {
        val executor = Executors.newSingleThreadExecutor()

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e("Biometric", "Authentication error: $errString ($errorCode)")
                    runOnUiThread {
                        isAuthenticated = true // 오류 발생 시에도 앱 진입 허용
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d("Biometric", "Authentication succeeded!")
                    runOnUiThread {
                        isAuthenticated = true // 인증 성공 시 앱 진입 허용
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w("Biometric", "Authentication failed.")
                    runOnUiThread {
                        isAuthenticated = true // 인증 실패 시에도 앱 진입 허용 (현재 로직 유지)
                    }
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Catch 앱 잠금 해제")
            .setSubtitle("지문 또는 얼굴 인증을 사용하여 계속하세요")
            // .setNegativeButtonText("취소") // DEVICE_CREDENTIAL과 함께 사용 시 IllegalArgumentException 발생
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
    }

    private fun authenticateWithBiometrics() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d("Biometric", "App can authenticate using biometrics.")
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.e("Biometric", "No biometric features available on this device.")
                isAuthenticated = true // 하드웨어 없으면 바로 앱 진입 허용
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.e("Biometric", "Biometric features are currently unavailable.")
                isAuthenticated = true // 하드웨어 사용 불가면 바로 앱 진입 허용
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.e("Biometric", "No biometric credentials enrolled.")
                // Prompt user to enroll a biometric credential
                val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                }
                startActivity(enrollIntent)
                isAuthenticated = true // 등록 요청 후 앱 진입 허용
            }
            else -> {
                Log.e("Biometric", "Unknown biometric status.")
                isAuthenticated = true // 그 외 알 수 없는 상태는 바로 앱 진입 허용
            }
        }
    }
}
