package com.example.telemedicineapp.ui.screens

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PaymentWebViewScreen(
    paymentUrl: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true // Bật JS để trang thanh toán hoạt động

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url.toString()

                        // 🌟 BẮT URL THÀNH CÔNG: Khi Stripe/ZaloPay thanh toán xong,
                        // họ thường chuyển hướng về 1 trang có chữ "success"
                        if (url.contains("success")) {
                            onSuccess()
                            return true // Trả về true để chặn WebView load tiếp, chuyển quyền về App
                        }

                        // Nếu user hủy thanh toán
                        if (url.contains("cancel")) {
                            onCancel()
                            return true
                        }

                        return false // Cho phép WebView tiếp tục load các trang bình thường
                    }
                }
                loadUrl(paymentUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}