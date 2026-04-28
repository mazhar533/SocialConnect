package com.mazhar.socialconnect.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.mazhar.socialconnect.ui.components.CustomButton
import com.mazhar.socialconnect.ui.components.CustomTextField
import com.mazhar.socialconnect.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID


@Composable
fun ProfileEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val userData by viewModel.userData.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val loading by viewModel.loading.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current

    // Prefill data when userData is loaded
    LaunchedEffect(userData) {
        userData?.let {
            name = it.name
            username = "@${it.name.lowercase().replace(" ", "")}"
            bio = it.bio
        }
    }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
            if (it == "Profile updated successfully") {
                onNavigateBack()
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { profileImageUri = it }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.background(BorderColor, CircleShape).size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryPurple)
                }
                Text(
                    text = "Edit Profile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                IconButton(
                    onClick = { viewModel.saveProfile(name, bio, profileImageUri) },
                    modifier = Modifier.background(PrimaryPurple, CircleShape).size(40.dp),
                    enabled = !loading
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                    }
                }
            }
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(Modifier, DividerDefaults.Thickness, color = BorderColor)
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                ) {
                    if (profileImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(profileImageUri),
                            contentDescription = "Profile Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (userData?.profilePictureUrl?.isNotEmpty() == true) {
                        Image(
                            painter = rememberAsyncImagePainter(userData?.profilePictureUrl),
                            contentDescription = "Profile Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 4.dp, bottom = 4.dp)
                        .size(28.dp)
                        .background(OrangeAccent, CircleShape)
                        .border(2.dp, BackgroundLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { launcher.launch("image/*") },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurple),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("UPDATE PHOTO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text("DISPLAY NAME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryPurpleSoft, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TextFieldBg, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("USERNAME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryPurpleSoft, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = username,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TextFieldBg, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextGray, fontSize = 14.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("USERNAMES CANNOT BE CHANGED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OrangeAccent, letterSpacing = 0.5.sp)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("BIO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryPurpleSoft, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(TextFieldBg, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontSize = 14.sp)
                    )
                }
            }
        }
    }
}
