package com.mazhar.socialconnect.ui.screens.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.mazhar.socialconnect.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onNavigateBack: () -> Unit,
    postId: String? = null,
    viewModel: CreatePostViewModel = viewModel()
) {
    var content by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val loading by viewModel.loading.collectAsState()
    val postSuccess by viewModel.postSuccess.collectAsState()
    val error by viewModel.error.collectAsState()
    val postToEdit by viewModel.postToEdit.collectAsState()
    val currentUser by viewModel.currentUserData.collectAsState()
    
    val context = LocalContext.current

    LaunchedEffect(postId) {
        if (postId != null) {
            viewModel.fetchPostToEdit(postId)
        }
    }

    LaunchedEffect(postToEdit) {
        postToEdit?.let {
            content = it.content
            if (it.imageUrl != null) {
                selectedImageUri = Uri.parse(it.imageUrl)
            }
        }
    }

    LaunchedEffect(postSuccess) {
        if (postSuccess) {
            Toast.makeText(context, if (postId == null) "Post created successfully!" else "Post updated successfully!", Toast.LENGTH_SHORT).show()
            onNavigateBack()
            viewModel.resetState()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.resetState()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .shadow(4.dp, CircleShape)
                        .background(Color.White, CircleShape)
                        .size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                }
                
                Text(
                    text = if (postId == null) "New Story" else "Edit Story",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                
                Button(
                    onClick = { viewModel.createPost(content, selectedImageUri, postId) },
                    enabled = (content.isNotBlank() || selectedImageUri != null) && !loading,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurpleSoft),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (postId == null) "Publish" else "Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Main Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .shadow(10.dp, RoundedCornerShape(32.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // User Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                        ) {
                            if (currentUser?.profilePictureUrl?.isNotEmpty() == true) {
                                Image(
                                    painter = rememberAsyncImagePainter(currentUser!!.profilePictureUrl),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(currentUser?.name ?: "User", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
                            Text("Share with everyone", color = TextGray, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("What's on your mind today?", color = Color.LightGray, fontSize = 18.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = PrimaryPurple
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, color = TextDark)
                    )

                    if (selectedImageUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(24.dp))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedImageUri),
                                contentDescription = "Selected image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove image", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Add Media Button at bottom
            Card(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .height(56.dp)
                    .width(160.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Media", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
