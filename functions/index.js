const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

// Initialize Firebase Admin SDK
admin.initializeApp();

/**
 * Cloud Function triggered on creation of a new document in the "notifications" collection.
 * It reads the notification details, fetches the target user's FCM token and preferences,
 * and sends a push notification using the secure FCM HTTP v1 API.
 */
exports.sendNotificationOnTrigger = onDocumentCreated("notifications/{notificationId}", async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
        console.log("No data associated with the event.");
        return;
    }

    const data = snapshot.data();
    const targetUserId = data.targetUserId;
    const type = data.type;
    const fromUserName = data.fromUserName || "Someone";
    const fromUserImage = data.fromUserProfilePicture || "";
    const postId = data.postId || "";
    const postImage = data.postImage || "";
    const postContent = data.postContent || "";
    const fromUserId = data.fromUserId || "";
    const commentId = data.commentId || "";

    if (!targetUserId) {
        console.log("No targetUserId found in notification document.");
        return;
    }

    try {
        // Retrieve the target user's profile to check FCM token and preferences
        const userDoc = await admin.firestore().collection("users").doc(targetUserId).get();
        if (!userDoc.exists) {
            console.log(`Target user document for UID: ${targetUserId} does not exist.`);
            return;
        }

        const userData = userDoc.data();
        
        // 1. Check if recipient has enabled notifications (default to true if field is missing)
        const notificationsEnabled = userData.notificationsEnabled !== false;
        if (!notificationsEnabled) {
            console.log(`User ${targetUserId} has disabled notifications. Skipping FCM dispatch.`);
            return;
        }

        // 1b. Check specific notification settings (post/chat) and muted contacts list
        if (type === "message") {
            const chatNotificationsEnabled = userData.chatNotificationsEnabled !== false;
            if (!chatNotificationsEnabled) {
                console.log(`User ${targetUserId} has disabled chat notifications. Skipping FCM dispatch.`);
                return;
            }
            const mutedChats = userData.mutedChats || [];
            if (fromUserId && mutedChats.includes(fromUserId)) {
                console.log(`User ${targetUserId} has muted chat notifications from ${fromUserId}. Skipping FCM dispatch.`);
                return;
            }
        } else if (["like", "comment", "share", "follow", "follow_request", "follow_accept"].includes(type)) {
            const postNotificationsEnabled = userData.postNotificationsEnabled !== false;
            if (!postNotificationsEnabled) {
                console.log(`User ${targetUserId} has disabled post notifications. Skipping FCM dispatch.`);
                return;
            }
        }

        // 2. Retrieve FCM Token
        const targetToken = userData.fcmToken;
        if (!targetToken) {
            console.log(`User ${targetUserId} does not have a registered FCM Token.`);
            return;
        }

        // 3. Construct Title and Body based on notification type
        const appTitle = "SocialConnect";
        let body = `${fromUserName} notified you.`;
        
        switch (type) {
            case "like":
                body = `${fromUserName} liked your post.`;
                break;
            case "comment":
                body = `${fromUserName} commented on your post.`;
                break;
            case "follow":
                body = `${fromUserName} started following you.`;
                break;
            case "follow_request":
                body = `${fromUserName} sent you a follow request.`;
                break;
            case "follow_accept":
                body = `${fromUserName} accepted your follow request.`;
                break;
            case "share":
                body = `${fromUserName} shared a post with you.`;
                break;
            case "message":
                body = `${fromUserName} sent you a message.`;
                break;
        }

        // For messages, the title is the sender's name with (Chat) suffix, and the body is the text itself
        let finalTitle = appTitle;
        if (type === "message") {
            finalTitle = `${fromUserName} (Chat)`;
            // Fallback to text/content/message if the app stored it inside notification document
            body = data.content || data.text || data.messageText || data.postContent || body;
        }

        // 4. Build FCM HTTP v1 Message Payload
        const message = {
            token: targetToken,
            notification: {
                title: finalTitle,
                body: body,
            },
            android: {
                priority: "high",
                notification: {
                    channelId: "social_connect_v2",
                }
            },
            data: {
                title: finalTitle,
                body: body,
                postId: postId,
                commentId: commentId,
                userImage: fromUserImage,
                postContent: postContent,
                type: type,
                fromUserId: fromUserId,
            }
        };

        // Attach optional post image if available
        if (postImage) {
            message.notification.image = postImage;
            message.data.image = postImage;
        }

        // 5. Send push notification securely via FCM HTTP v1
        const response = await admin.messaging().send(message);
        console.log(`Successfully dispatched FCM notification via HTTP v1 to token: ${targetToken}. Response ID:`, response);
    } catch (error) {
        console.error("Exception encountered in sendNotificationOnTrigger Cloud Function:", error);
    }
});
