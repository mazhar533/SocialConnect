# SocialConnect (VibeConnect) 📱

SocialConnect is a premium, real-time social media application designed for seamless interaction. Built with modern Android technologies, it provides a smooth and engaging user experience with a focus on aesthetics, premium features, and performance.

---

## ✨ Core Features & Capabilities

### 1. 💬 Real-Time Chat & DM Rooms
Connect with your friends instantly through personal chat channels.
- **Three-Dot Option Menu**: Easy control over chats directly from the header options.
- **Selective Clear Chat**: Wipe chat messages for yourself to tidy up your screen, while the messages safely remain visible to the other participant.
- **Silent Blocking & Unblocking**:
  - Block a user silently. They can continue writing messages (so they don't know they are blocked), but their messages are filtered out and not received by you.
  - Easily unblock any blocked user anytime from the chat header.

### 2. 🛡️ Profile & Real-Time Email Verification Badge
- Displays a premium, color-coded badge under your username:
  - 🟢 **Verified Account** (for verified emails).
  - 🔴 **Unverified Account** (for unverified emails).
- Clicking the unverified badge sends a Firebase Auth verification email link instantly.
- The app automatically detects when you return from your email client and updates the verification status in real time.

### 3. 🔒 Privacy & Private Account Workflow
- **Private Account Toggle**: Switch your profile to private from the Settings screen.
- **Locked Content**: When a user's account is private, non-followers cannot view their posts, follower lists, or full profile details.
- **Follow Request System**: Non-followers must send a "Follow Request" which generates a notification for the target user to either **Accept** or **Decline**.

### 4. ⚙️ Secure Account Control & Settings
- **Change Email**: Seamlessly request an email change link with built-in Firebase security rules and state flows.
- **Irreversible Account Deletion**:
  - Permanently delete your profile and auth credentials.
  - Automatically sweeps and deletes all associated user document records, post uploads, comment replies, and notification lists in Firestore.
  - Clears associated profile pictures and post images from Firebase Storage.
- **Collapsible FAQs & Privacy Policy**: Elegant expandable text cards embedded inside the Settings screen for quick reference.

### 5. ⚡ Optimized Caching Architecture
- Powered by centralized singletons: `UserRepository` and `NotificationRepository`.
- Connects active Firestore Snapshot Listeners that stream real-time updates and cache them in memory.
- Avoids redundant Firestore read calls and network delays when navigating between Home, Notifications, and Profile screens.
- Automatically handles listener cleanup and cache invalidation upon signing out.

### 6. 📸 Visual Experience & UI
- **Real-Time Feed**: View, like, comment on, and edit posts instantly.
- **Premium Animations**: Smooth compose transitions and glassmorphism elements.
- **Shimmer Loaders**: Premium loading placeholders while database streams are fetching.
- **Dark Mode Support**: Adapts beautifully to system-wide theme changes.

### 7. 🔍 Search Functionality
- **User-Only Search**: An integrated search bar is placed inside the `HomeHeader` of the Home Screen, above the user's greeting and profile details/image.
- **Real-Time Prefix Matching**: Queries the Firebase Firestore `users` collection dynamically by checking if the name starts with the search query (prefix search). Clicking a search result navigates to that user's profile.

---

## 🗺️ Navigation & Screen Routes

All navigation flows are managed in `AppNavigation.kt` using Jetpack Compose Navigation. The application defines the following 12 distinct routes:

| Route Pattern | Screen Component | Description & Parameters |
|:---|:---|:---|
| `login` | `LoginScreen` | Direct email & password entry with login validation and navigation to Signup or Password Recovery. |
| `signup` | `SignUpScreen` | Account registration including custom profile creation. |
| `recovery` | `RecoveryScreen` | Triggers a Firebase password reset email flow. |
| `home` | `HomeScreen` | Core feed displaying posts, integrated Search bar in the header to query users by name, and links to Profile, Chats, Notifications, and Post Details. |
| `notifications` | `NotificationScreen` | Central hub for likes, comments, follow alerts, follow requests (with Accept/Decline action buttons), and message routing. |
| `create_post?postId={postId}` | `CreatePostScreen` | Handles both new post creation and editing existing posts (via optional `postId`). |
| `profile?userId={userId}&showRequests={showRequests}` | `ProfileScreen` | Displays posts, follower counts, verification badge, follow request options, and private/public profile locking. |
| `profile_edit` | `ProfileEditScreen` | Modifies nickname, bio, and profile picture avatar. |
| `settings` | `SettingsScreen` | Privacy options (Private Account switch), security settings (Change Email, Delete Account), and interactive FAQ cards. |
| `chats` | `ChatListScreen` | Lists active conversations sorted by the latest message timestamp. |
| `chat_detail?roomId={roomId}&userName={userName}&profileImage={profileImage}` | `ChatDetailScreen` | Real-time chat messages room with inline typing indicators, silent block actions, and selective clear options. |
| `post_detail?postId={postId}&commentId={commentId}` | `PostDetailScreen` | View individual post details, likes, list of comments, and allows adding new comments. |

---

## 🛠️ Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (100% Declarative UI)
- **Backend & Services**: [Firebase](https://firebase.google.com/)
  - **Authentication**: Email/Password Sign-In & Verification Links.
  - **Firestore**: Real-Time NoSQL database for posts, rooms, messages, notifications, and user documents.
  - **Cloud Storage**: Secure media storage for posts and profile images.
  - **FCM (Firebase Cloud Messaging)**: Secure push notifications delivery.
- **Image Loader**: [Coil](https://coil-kt.github.io/coil/)
- **Architecture**: MVVM (Model-View-ViewModel) + Centralized Repositories (Repository Pattern)
- **Programming Language**: Kotlin

---

## 🚀 Getting Started

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/mazhar533/SocialConnect.git
   ```
2. **Open in Android Studio**:
   Import the project and wait for the Gradle sync to complete.
3. **Firebase Configuration**:
   - Create a Firebase Project.
   - Place your `google-services.json` file inside the `app/` directory.
   - Enable Email/Password Auth, Cloud Firestore, and Cloud Storage.
4. **Deploy & Run**:
   Click the **Run** button in Android Studio to launch on your physical device or emulator.

---

Developed with ❤️ by **Mazhar**
