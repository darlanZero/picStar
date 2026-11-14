package com.picstar.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picstar.app.data.PicStarViewModel
import com.picstar.app.ui.theme.LocalPicStarColors

/**
 * Full-screen Authentication Gate.
 * Blocks access to the rest of the app until the user authenticates with Pixiv.
 */
@Composable
fun LoginScreen(
    viewModel: PicStarViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalPicStarColors.current
    var refreshTokenInput by remember { mutableStateOf("") }
    var showTokenInput by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.ink,
                        colors.navy,
                        colors.ink
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Emblem
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(colors.coral.copy(alpha = 0.12f))
                    .border(1.5.dp, colors.coral.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✦",
                    color = colors.coral,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brand Name
            Text(
                text = buildAnnotatedString {
                    append("pic")
                    withStyle(SpanStyle(color = colors.coral)) {
                        append("star")
                    }
                },
                color = colors.text,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "ILLUSTRATION OBSERVATORY",
                color = colors.muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.panel.copy(alpha = 0.85f))
                    .border(1.dp, colors.line, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sign in to your Pixiv universe",
                    color = colors.text,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Connect your Pixiv account to load your personalized recommendations, followed artists, rankings and bookmarks directly from the Pixiv API.",
                    color = colors.muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (viewModel.isAuthenticating) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = colors.coral,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Authenticating with Pixiv OAuth…",
                            color = colors.text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    // Primary OAuth2 PKCE WebView Login Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.coral)
                            .clickable { viewModel.startPixivOAuthWebView(createAccount = false) }
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "✦",
                                color = Color(0xFF071423),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign in with Pixiv (WebView OAuth)",
                                color = Color(0xFF071423),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Create Account Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.panel2.copy(alpha = 0.7f))
                            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
                            .clickable { viewModel.startPixivOAuthWebView(createAccount = true) }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create a new Pixiv account",
                            color = colors.text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = colors.line)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Toggle Refresh Token login
                    Text(
                        text = if (showTokenInput) "Hide Refresh Token login ▲" else "Or sign in with a Pixiv Refresh Token ▼",
                        color = colors.coral2,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { showTokenInput = !showTokenInput }
                            .padding(vertical = 4.dp)
                    )

                    AnimatedVisibility(visible = showTokenInput) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text(
                                text = "Pixiv OAuth Refresh Token",
                                color = colors.muted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(colors.ink.copy(alpha = 0.65f))
                                    .border(1.dp, colors.line, RoundedCornerShape(11.dp))
                                    .padding(horizontal = 13.dp, vertical = 12.dp)
                            ) {
                                if (refreshTokenInput.isEmpty()) {
                                    Text(
                                        text = "Paste your refresh_token here…",
                                        color = colors.muted.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                                BasicTextField(
                                    value = refreshTokenInput,
                                    onValueChange = { refreshTokenInput = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = colors.text,
                                        fontSize = 12.sp
                                    ),
                                    cursorBrush = SolidColor(colors.coral),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(colors.coral.copy(alpha = 0.18f))
                                    .border(1.dp, colors.coral, RoundedCornerShape(11.dp))
                                    .clickable {
                                        viewModel.connectWithRefreshToken(refreshTokenInput)
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Connect with Refresh Token",
                                    color = colors.coral2,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Error Message
                val errorMsg = viewModel.authErrorMessage
                if (!errorMsg.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.coral.copy(alpha = 0.14f))
                            .border(1.dp, colors.coral.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = errorMsg,
                            color = colors.coral2,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Fullscreen OAuth2 PKCE WebView Modal when open
        val pkce = viewModel.pkceSession
        if (viewModel.isOAuthWebViewOpen && pkce != null) {
            val targetUrl = if (viewModel.isOAuthCreateAccountMode) {
                pkce.createAccountUrl
            } else {
                pkce.loginUrl
            }
            PixivOAuthWebViewModal(
                loginUrl = targetUrl,
                onCodeIntercepted = { code ->
                    viewModel.handlePixivOAuthCallbackCode(code)
                },
                onDismiss = { viewModel.isOAuthWebViewOpen = false }
            )
        }
    }
}
