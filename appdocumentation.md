# CLEAN SHIELD — END-TO-END PRODUCT DOCUMENTATION

## 1. Product Overview

Clean Shield is a native Android privacy-focused communication application.

The public-facing experience must resemble a legitimate phone optimisation/security utility. The actual communication functionality is revealed only after the user completes the secure access flow.

Core authenticated functionality:
- Private messaging
- Friends
- Username-based discovery
- Audio calls
- Video calls
- One-Shot and normal media
- Private photo/video Vault
- Blocking and reporting
- Privacy/security controls

The application must be production-ready. No dummy data, mock APIs, temporary implementations, placeholder security, or half-completed functionality.

---

# 2. CORE DESIGN SYSTEM

## Brand

App name: Clean Shield

Primary gradient:
- #5DE0E6
- #0078A6

Gradient:
- Circular/radial
- Center: 50% 50%

UI principles:
- Modern native Android design
- Clean white content areas
- Cyan/teal brand gradient
- Smooth transitions
- Mobile responsive
- Clear typography
- Consistent icons
- No unnecessary UI

Every secondary page must provide a Back icon where applicable.

---

# 3. PUBLIC / PRE-AUTHENTICATION PRIVACY LAYER

Until successful secure access, the application must NOT reveal:
- Chat
- Messenger
- Couples
- Friends
- Calls
- Private Vault
- Social features
- Messaging terminology

The launch experience is a phone-manager/security-style interface.

## Launch sequence

Splash
→ Optimise Scanning
→ Optimise Complete
→ Go
→ Access PIN
→ Authentication/setup
→ Dashboard

Every application launch must begin with the Splash/Optimise experience instead of reopening directly to the messaging dashboard.

---

# 4. SCREEN INVENTORY

Main screens:

1. Splash Screen
2. Optimise Scanning
3. Optimise Complete
4. Create 6-Digit PIN
5. Confirm 6-Digit PIN
6. Enter 6-Digit PIN
7. Sign Up
8. Email OTP Verification
9. Sign In
10. Forgot Password
11. Dashboard
12. Profile
13. Edit Profile
14. Friends
15. Blocked Users
16. Search Users
17. Search Result / User Profile
18. Notifications
19. Inbox / Messenger
20. Chat
21. Secure Chat State / Settings
22. Audio Call
23. Video Call
24. Create Vault PIN
25. Enter Vault PIN
26. Private Vault
27. Vault Media Viewer

Supporting UI states:
- Loading
- Skeleton
- Empty
- Error
- Retry
- Success
- Upload progress
- Permission handling
- Confirmation dialogs
- Connection states
- Call states
- Message states

---

# 5. SPLASH SCREEN

Purpose:
- Initial application startup.

Requirements:
- Clean Shield branding.
- Use #5DE0E6 → #0078A6 radial gradient.
- Minimal production-ready Android splash UI.
- No messaging/social information.

Navigation:
Splash → Optimise Scanning.

---

# 6. OPTIMISE SCANNING

Purpose:
Present the application as a phone optimisation/security utility.

Requirements:
- Animated scan.
- Progress from 0% to 100%.
- Security/optimisation-style categories.
- Example categories:
  - Cleanup
  - Privacy
  - Security Protection
  - General
- Smooth progress updates.
- Loading state throughout scan.
- No messaging terminology.

Important:
The app must not falsely claim to perform system/antivirus operations that it cannot actually perform.

Navigation:
Optimise Scanning → Optimise Complete.

---

# 7. OPTIMISE COMPLETE

Display:
- Optimal status
- 100 pts
- Completed optimisation state
- Recommended actions
- Go buttons
- Done button

Rules:
- Done closes the application.
- Every Go button enters the secure access flow.
- No messaging functionality is exposed.

Navigation:
Go → Access Check
Done → Close app.

---

# 8. ACCESS PIN

## First-time user

Go
→ Create 6-Digit PIN
→ Confirm 6-Digit PIN
→ Sign Up
→ Email OTP
→ Dashboard

Rules:
- Exactly 6 numeric digits.
- Confirmation required.
- PIN must never be stored plaintext.
- Use secure Android credential/key-storage mechanisms.
- Invalid/mismatched PIN must show validation.
- Save setup state securely.

## Returning user

Go
→ Enter 6-Digit PIN
→ Session/authentication check
→ Dashboard OR Sign In

Rules:
- Correct PIN is required.
- Incorrect PIN remains on the PIN screen.
- Never bypass the PIN.

---

# 9. SIGN UP

Fields:
- Username
- Email
- Password
- Confirm Password

Username requirements:
- Instagram-style username format.
- Globally unique.
- One username can belong to only one account.
- Duplicate usernames must be impossible.
- Validate availability.
- Enforce uniqueness server-side/database-side.

Password:
- Never store plaintext.
- Use secure authentication provider/backend implementation.

Navigation:
Sign Up → Email OTP Verification.

---

# 10. EMAIL OTP VERIFICATION

Requirements:
- Send 6-digit OTP to registered email.
- 6-digit input.
- Verification.
- Resend OTP.
- Resend cooldown.
- Expiration handling.
- Invalid OTP handling.
- Loading state.
- Error state.
- Success state.

Account must not be marked verified until OTP succeeds.

Navigation:
Successful verification → Dashboard.

---

# 11. SIGN IN

Fields:
- Username or Email
- Password

Features:
- Sign In
- Forgot Password
- Loading state
- Invalid credentials state
- Network error/retry

Successful authentication:
→ Dashboard.

---

# 12. DASHBOARD

The dashboard must contain ONLY the specified navigation structure.

Fixed Header:
- Left: Logout icon
- Right: Messenger icon
- Right: Notification icon

Fixed Mobile Navigation:
- Profile
- Friends
- Private Vault
- Search

Do not add:
- Unrequested cards
- Extra dashboard features
- Unrequested shortcuts
- Unrequested bottom-navigation items

Use:
- #5DE0E6
- #0078A6

---

# 13. PROFILE

Display/edit:
- Circular profile image
- 300 × 300 px profile image
- Username
- Name
- Bio

All are editable.

Username:
- Instagram-style format.
- Globally unique.
- Same username cannot belong to two accounts.
- Real-time availability validation.
- Server-side uniqueness enforcement.

Profile image:
- Upload/change.
- Validate supported format and size according to implementation requirements.
- Upload progress.
- Error/retry states.

Save:
- Persist changes.
- Loading state.
- Success/error state.

---

# 14. FRIENDS

Requirements:
- Search box.
- List all accepted friends.
- Profile image.
- Username/name.
- Chat button.
- 3-dot menu.

3-dot options:
- Unfriend
- Block
- Report

Sensitive/destructive actions:
- Confirmation required.

Blocked Users button:
→ Blocked Users screen.

Loading:
- Skeleton state.

Empty:
- No friends state.

Search:
- Smooth.
- No UI freezing.

---

# 15. BLOCKED USERS

Display:
- Blocked account
- Profile image
- Username/name
- Unblock button

Unblock:
- Confirmation required.
- Server-side enforcement.
- Refresh blocked list after success.

---

# 16. SEARCH USERS

Search only by username.

Do NOT search by:
- Name
- Email
- Phone
- Other personal fields

Results:
If exact username exists:
- Profile image
- Username
- Name
- Request button
- Block button

States:
- Search loading
- User found
- User not found
- Request Sent
- Already Friends
- Blocked
- Error

Rules:
- Duplicate requests prevented.
- Block requires confirmation.
- Server-side authorization required.

---

# 17. FRIEND REQUEST LOGIC

Search user
→ Request
→ Recipient receives notification
→ Recipient Accepts or Rejects

If accepted:
- Friendship created for both users.
- Chat becomes available.

If rejected:
- Request rejected.
- Chat remains unavailable.

Before acceptance:
- Users cannot chat.

---

# 18. NOTIFICATIONS

Notification types:
- New friend request
- Friend request accepted
- Friend request rejected

Friend request item:
- Profile image
- Username/name
- Timestamp
- Accept
- Reject

Accept:
- Confirmation.
- Create friendship.
- Enable chat.
- Update notification.

Reject:
- Confirmation.
- Reject request.
- Update/remove notification.

Notification system:
- Read/unread states.
- Unread badge.
- Loading/skeleton.
- Empty state.
- Error/retry.
- Real-time updates.
- Prevent duplicate actions.

---

# 19. INBOX / MESSENGER

Style:
- Instagram-style private inbox.

Only accepted friends can appear as active conversations.

Conversation row:
- Profile image
- Username/name
- Last message/media indicator
- Time
- Unread count
- Sent/seen state where applicable

States:
- Loading
- Empty
- Error
- Retry

Navigation:
Inbox → Chat.

Back icon required.

---

# 20. CHAT

Header:
- Back icon
- Profile image
- Username/name
- Audio call
- Video call
- 3-dot menu

Composer:
- Text input
- Gallery/media button
- Send button

Media:
- Images
- Videos
- Normal sending
- One-Shot sending
- Gallery selection
- Upload progress

Message states:
- Sending
- Sent
- Seen
- Failed

Requirements:
- Real-time messages.
- Smooth scrolling.
- Keyboard handling.
- Network retry.
- Duplicate-send prevention.

Only accepted friends may communicate.

---

# 21. ONE-SHOT MEDIA

One-Shot media:
- Clearly identified before sending.
- Temporary/private behavior.
- Must follow server-side authorization and lifecycle rules.
- Must not be treated as ordinary permanent media.

Media deletion/access must be securely enforced.

---

# 22. CHAT 3-DOT MENU

Options:

1. Secure My Chat
2. Clear Chat
3. Block User

No unnecessary options.

---

# 23. SECURE MY CHAT

When enabled:
- Use Android secure-screen capabilities.
- Prevent screenshots where supported.
- Prevent screen recording/capture where supported.
- Protect content in recent-app previews where applicable.
- Restrict message/media forwarding through application/server rules.
- Apply protection to the protected chat screen.

Important:
Do not claim that Android can prevent every possible external recording method.

Security must not rely only on UI hiding.

---

# 24. CLEAR CHAT

Options:

## Clear for me
Remove conversation/messages from current user's view according to backend deletion rules.

## Clear for both sides
Remove messages for both participants according to server-side deletion rules.

Every deletion:
- Confirmation dialog.
- Cancel.
- Confirm.
- Loading state.
- Success/error handling.

---

# 25. BLOCK USER FROM CHAT

Block requires confirmation.

After block:
- Messaging disabled.
- Calls disabled.
- New friend interaction restricted.
- Search/Friends/Inbox states updated.
- Server-side block enforcement.

---

# 26. AUDIO CALL

States:
- Calling
- Ringing
- Connected
- Declined
- Ended
- Failed

Controls:
- End call
- Microphone mute/unmute
- Call duration

Permission:
- Microphone only when required.

Handle:
- Network interruption
- Permission denial
- Call failure

---

# 27. VIDEO CALL

States:
- Calling
- Ringing
- Connected
- Declined
- Ended
- Failed

Controls:
- End call
- Microphone mute/unmute
- Camera enable/disable
- Front/back camera switch
- Call duration

Permissions:
- Camera
- Microphone

Handle:
- Permission denial
- Network failure
- Call failure

---

# 28. PRIVATE VAULT

Purpose:
Private photo/video storage.

First access:
→ Create 4-Digit Vault PIN
→ Confirm PIN
→ Vault

Future access:
→ Enter 4-Digit Vault PIN
→ Vault

Rules:
- Exactly 4 numeric digits.
- Secure storage.
- Never plaintext.
- Incorrect PIN remains on PIN screen.
- Auto-lock when leaving/backgrounding where appropriate.

Vault supports:
- Images
- Videos
- Upload progress
- Private media grid
- Media viewer
- Delete

Delete:
- Confirmation required.

Private media:
- Must not be publicly accessible.
- Must require authenticated authorization.
- Do not expose unrestricted public media URLs.

---

# 29. ANDROID PERMISSIONS

Request permissions only when the related feature is used.

Potential permissions/features:
- Photo/video access or Android system Photo Picker
- Camera
- Microphone
- Notifications

Rules:
- Runtime permission handling.
- Explain permission context where appropriate.
- Handle denied.
- Handle permanently denied.
- Handle revoked permissions.
- Do not request unnecessary permissions.
- Optional permission denial must not unnecessarily block unrelated features.

---

# 30. CONFIRMATION DIALOGS

Confirmation is mandatory for:

- Unfriend
- Block
- Report where appropriate
- Unblock
- Clear chat
- Clear for both
- Delete message
- Delete media
- Vault media deletion
- Logout
- Account deletion
- Other irreversible/sensitive actions

Every dialog:
- Clearly describe consequence.
- Cancel.
- Confirm.
- Prevent accidental action.

---

# 31. GLOBAL UI STATES

Every network/database operation must support:

Loading:
- Skeleton/progress.

Empty:
- Clear explanation.
- No misleading content.

Error:
- Human-readable message.
- Retry where applicable.

Success:
- Clear state update.
- Avoid unnecessary blocking dialogs.

Offline/network:
- Graceful failure.
- Retry.

Duplicate action:
- Disable button while processing.
- Prevent duplicate API requests.

---

# 32. NAVIGATION RULES

Required main flow:

Splash
→ Optimise Scanning
→ Optimise Complete
→ Go
→ 6-Digit PIN
→ Authentication
→ Dashboard

Dashboard navigation:
- Profile
- Friends
- Vault
- Search

Header navigation:
- Logout
- Messenger
- Notifications

Secondary navigation:
- Back icon required.

Do not break existing navigation state when moving between screens.

Do not expose dashboard functionality before authentication.

---

# 33. SECURITY REQUIREMENTS

Security must be implemented at both client and server levels.

Required:
- Secure authentication.
- Server-side authorization.
- Secure PIN storage.
- No plaintext passwords.
- No plaintext PINs.
- Protected media access.
- Friend-only chat authorization.
- Server-side blocking.
- Server-side request validation.
- Input validation.
- Rate limiting where appropriate.
- Secure session handling.
- Secure API key handling.
- No sensitive secrets embedded in the Android client.

Client UI must never be considered the only security layer.

---

# 34. DATA MODEL REQUIREMENTS

Core entities:

## users
- id
- username
- normalized_username
- email
- profile_image
- name
- bio
- email_verified
- created_at
- updated_at

Unique constraint:
- normalized_username UNIQUE

## friend_requests
- id
- sender_id
- receiver_id
- status
- created_at
- updated_at

Statuses:
- pending
- accepted
- rejected

## friendships
- id
- user_id
- friend_id
- created_at

Must prevent duplicate friendship relationships.

## blocked_users
- id
- blocker_id
- blocked_id
- created_at

Must prevent duplicate blocks.

## notifications
- id
- user_id
- type
- related_user_id
- related_request_id
- read
- created_at

## conversations
- id
- created_at
- updated_at

## conversation_members
- conversation_id
- user_id

## messages
- id
- conversation_id
- sender_id
- message_type
- content
- media_reference
- one_shot
- sent_at
- seen_at
- deleted_for_sender
- deleted_for_receiver

## vault_media
- id
- user_id
- media_type
- secure_storage_reference
- created_at

Never expose Vault storage publicly.

---

# 35. REAL-TIME REQUIREMENTS

Real-time updates are required for:
- Messages
- Seen status
- Friend requests
- Notifications
- Friend status changes
- Call signaling where applicable

Handle:
- Reconnection
- Duplicate events
- Out-of-order events
- Offline recovery
- Session expiration

---

# 36. MEDIA REQUIREMENTS

Media upload:
- Gallery/system picker.
- Image/video validation.
- Upload progress.
- Cancel/error handling.
- Retry.
- Secure storage.
- Authenticated download/access.

Do not expose private media through unrestricted public URLs.

---

# 37. ACCESSIBILITY & MOBILE UX

The application must:
- Work across common Android screen sizes.
- Handle keyboard correctly.
- Support scrolling.
- Avoid content behind system bars.
- Maintain touch target usability.
- Provide readable text.
- Provide visual feedback for actions.
- Avoid accidental destructive taps.

---

# 38. PRODUCTION QUALITY RULE

Never implement:
- Dummy APIs
- Mock authentication
- Fake calls
- Fake security
- Placeholder databases
- Temporary navigation
- Hardcoded user data
- Client-only authorization
- Fake upload progress
- UI-only friendship/blocking logic

All functionality must be production-ready.

---

# 39. FINAL ACCEPTANCE CRITERIA

The application is considered complete only when:

- All 27 main screens exist as required.
- Every required navigation path works.
- Authentication works.
- Email OTP works.
- Username uniqueness is enforced.
- 6-digit app PIN works.
- 4-digit Vault PIN works.
- Friends work.
- Blocking works.
- Requests work.
- Notifications work.
- Inbox works.
- Real-time chat works.
- Sent/Seen states work.
- Image/video upload works.
- One-Shot media works.
- Audio calls work.
- Video calls work.
- Secure Chat works within Android platform limitations.
- Vault securely stores private media.
- Permissions are correctly handled.
- Confirmation dialogs protect destructive actions.
- Loading/error/empty states exist.
- Server-side authorization exists.
- No dummy or temporary implementation remains.
- Existing functionality is not broken.
- Android UI is responsive and production-ready.

---

# 40. END-TO-END USER FLOWS

## New User

Open app
→ Splash
→ Optimise Scanning
→ Optimise Complete
→ Go
→ Create 6-Digit PIN
→ Confirm PIN
→ Sign Up
→ Email OTP
→ Verify
→ Dashboard
→ Profile/Friends/Search/Vault/Messenger

## Returning User

Open app
→ Splash
→ Optimise Scanning
→ Optimise Complete
→ Go
→ Enter 6-Digit PIN
→ Session Check
→ Dashboard

If session is unavailable:
→ Sign In
→ Dashboard

## Friend Flow

Search username
→ User result
→ Request
→ Recipient Notification
→ Accept
→ Friendship created
→ Inbox/Chat enabled

## Vault Flow

Dashboard
→ Vault
→ First-time: Create 4-digit PIN
→ Confirm
→ Vault

Returning:
Vault
→ Enter PIN
→ Vault
→ Upload/view/delete private media

## Chat Flow

Friends
→ Chat
→ Text/media
→ Sending
→ Sent
→ Seen

Chat:
→ Audio Call / Video Call
OR
→ Secure My Chat
OR
→ Clear Chat
OR
→ Block

---

# 41. DEVELOPMENT PRINCIPLE

Build Clean Shield as a real secure communication product, not a visual prototype.

Every screen must connect to real application state.

Every sensitive permission must be enforced by the backend.

Every security feature must be implemented using actual platform capabilities.

Every destructive action must require confirmation.

Every page must have appropriate loading, empty, error and success states.

Never break an already working feature while implementing another feature.

Never replace production functionality with temporary code.
