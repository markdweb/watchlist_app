package com.cheese.watchlist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.ui.components.AppLogo
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Outlined.Movie,
        title = "Track every movie & series",
        description = "Keep tabs on what you're watching, want to watch, and have already finished."
    ),
    OnboardingPage(
        icon = Icons.Outlined.Lock,
        title = "Your data stays with you",
        description = "Everything is stored locally on this device. No cloud, no account required."
    ),
    OnboardingPage(
        icon = Icons.Outlined.Sell,
        title = "Organize your way",
        description = "Custom genres, sources, favorites, and episode tracking — all built in."
    ),
    OnboardingPage(
        icon = Icons.Outlined.SaveAlt,
        title = "Back up anytime",
        description = "Export your whole watchlist to a file, and import it back whenever you need to."
    )
)

@Composable
fun OnboardingScreen(onComplete: (name: String) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size + 1 })
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < onboardingPages.size) {
                    TextButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(onboardingPages.size) }
                    }) {
                        Text("Skip", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                if (page < onboardingPages.size) {
                    OnboardingPageContent(onboardingPages[page])
                } else {
                    NameCaptureContent(name = name, onNameChange = { name = it })
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(onboardingPages.size + 1) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                    }
                }

                val isLastPage = pagerState.currentPage == onboardingPages.size
                Button(
                    onClick = {
                        if (isLastPage) {
                            onComplete(name.trim())
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isLastPage) "Get Started" else "Next")
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(28.dp))
        Text(
            page.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
        Text(
            page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NameCaptureContent(name: String, onNameChange: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppLogo(size = 84.dp)
        androidx.compose.foundation.layout.Spacer(Modifier.size(28.dp))
        Text(
            "What should we call you?",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        Text(
            "We'll use this to greet you — totally optional.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("Your name") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
