# ParkSpot

An Android app (Kotlin) for peer-to-peer parking spot rental. Drivers can browse, book, and navigate to parking spots. Owners can list and manage their spaces. Admins can oversee bookings and resolve disputes.

---

## Prerequisites

- Android Studio 
- JDK 17 (bundled with Android Studio)
- Android SDK — API 26 minimum, API 34 recommended
- Google Maps API Key (from Google Cloud Console)
- Backend REST API server running and reachable

---

## Configuration

Create or open `local.properties` in the project root and add:
```properties
API_BASE_URL=https://comp3078project-production.up.railway.app/
MAPS_API_KEY=AIzaSyCYXNSAI4GkWA1YFbKVkIcJU0bEsnO1ceo
```

> If running on an emulator with a local backend, use `10.0.2.2` instead of `localhost`.

---

## Running the App

1. Clone the repo and open it in Android Studio
2. Let Gradle sync complete
3. Fill in `local.properties` as above
4. Start the backend server — see: `[Backend Repo URL]`
5. Connect a device or launch an AVD
6. Press **Run** (▶) or `Shift + F10`

---

## Login

### Register
Tap **Register** on the launch screen and provide a username, email, and password.

### User Login
| Field    | Value              |
|----------|--------------------|
| Username | `[demo username]`  |
| Password | `[demo password]`  |

### Admin Login
Check the **Admin Login** checkbox on the login screen to reveal the Admin ID field.

| Field    | Value              |
|----------|--------------------|
| Username | `admin` |
| Password | `admin12345!` |
| Admin ID | `admin1234!`       |

> Admin accounts are redirected to the Admin Dashboard. Using admin credentials without the checkbox returns a 403.

---

## Navigation

**Bottom bar:**
- **Home** — map + list of all active listings
- **+** — create listing (Owner Mode) or become an owner
- **Menu** — opens side drawer

**Drawer:**
- Profile, Reserved Listings, My Listings, Create Listing, Payment Methods, Settings, Help & Support, Admin Dashboard (admin only), Logout

**Owner Mode:**  
Users default to Driver Mode. Tap **Become a Spot Owner** to register as an owner, then switch modes freely from the drawer.

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Can't reach server | Check `API_BASE_URL`; use `10.0.2.2` on emulators |
| Map not loading | Verify `MAPS_API_KEY` and that Maps SDK is enabled in Google Cloud Console |
| Gradle sync fails | Confirm both keys exist in `local.properties`, then re-sync |
| Login 403 | Admin accounts must use the Admin Login checkbox |
| Booking 409 | Time slot already taken — pick a different time |