package com.example.telemedicineapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.telemedicineapp.presentation.screens.auth.LoginScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Khởi tạo bộ điều hướng
                    val navController = rememberNavController()

                    // NavHost quản lý danh sách các màn hình
                    NavHost(navController = navController, startDestination = "login_screen") {

                        // 1. Màn hình Đăng nhập
                        composable("login_screen") {
                            LoginScreen(
                                onLoginSuccess = {
                                    // Chuyển sang màn hình bác sĩ và xóa màn hình đăng nhập khỏi bộ nhớ
                                    navController.navigate("doctor_screen") {
                                        popUpTo("login_screen") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. Màn hình Bác sĩ (Tạm thời là 1 dòng Text)
                        composable("doctor_screen") {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🏥 CHÀO MỪNG ĐẾN VỚI MÀN HÌNH BÁC SĨ!")
                            }
                        }

                    }
                }
            }
        }
    }
}
